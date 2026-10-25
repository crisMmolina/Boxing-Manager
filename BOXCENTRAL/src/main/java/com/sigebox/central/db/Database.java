package com.sigebox.central.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Conexión única a SQLite en modo WAL. SQLite admite un solo escritor, así que toda la central
 * comparte esta conexión y los accesos se serializan con {@code synchronized}.
 *
 * <p>{@code synchronous=FULL}: cada commit llega al disco antes de devolver, para que un cierre
 * forzado no pierda una calificación ya confirmada con ACK.
 */
public final class Database implements AutoCloseable {

    /** Migraciones en orden; agregar al final, nunca editar una ya publicada. */
    static final List<String> MIGRATIONS = List.of("V1__init.sql");

    private final Path file;
    private final Connection connection;

    private Database(Path file, Connection connection) {
        this.file = file;
        this.connection = connection;
    }

    /** Abre (o crea) {@code sigebox.db} en la carpeta de datos del usuario. */
    public static Database openDefault() {
        return open(defaultDir().resolve("sigebox.db"));
    }

    /** Abre (o crea) la base en {@code file} y aplica las migraciones pendientes. */
    public static Database open(Path file) {
        try {
            var parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            var connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=FULL");
                st.execute("PRAGMA foreign_keys=ON");
                st.execute("PRAGMA busy_timeout=5000");
            }
            var db = new Database(file, connection);
            db.migrate();
            return db;
        } catch (SQLException | IOException e) {
            throw new DbException("no se pudo abrir " + file, e);
        }
    }

    /** {@code %APPDATA%\SIGEBOX} en Windows; {@code ~/.sigebox} en otros sistemas. */
    public static Path defaultDir() {
        var appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "SIGEBOX");
        }
        return Path.of(System.getProperty("user.home"), ".sigebox");
    }

    public Path file() {
        return file;
    }

    /** Trabajo con la conexión dentro de una transacción. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    /** Ejecuta {@code work} en una transacción: todo o nada. */
    public synchronized <T> T inTransaction(Work<T> work) {
        try {
            if (!connection.getAutoCommit()) {
                return work.run(connection); // transacción anidada: se une a la exterior
            }
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DbException("transacción fallida", e);
        }
    }

    /** Valor de un PRAGMA (para pruebas y diagnóstico). */
    public String pragma(String name) {
        return inTransaction(c -> {
            try (Statement st = c.createStatement(); var rs = st.executeQuery("PRAGMA " + name)) {
                return rs.next() ? rs.getString(1) : null;
            }
        });
    }

    /** Versión de esquema aplicada. */
    public int schemaVersion() {
        return inTransaction(c -> {
            try (Statement st = c.createStatement();
                    var rs = st.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    private void migrate() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS schema_version ("
                    + "version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_ms INTEGER NOT NULL)");
        }
        int current = schemaVersion();
        for (int i = current; i < MIGRATIONS.size(); i++) {
            var name = MIGRATIONS.get(i);
            var sql = resource("/db/" + name);
            int version = i + 1;
            inTransaction(c -> {
                try (Statement st = c.createStatement()) {
                    for (var stmt : statements(sql)) {
                        st.execute(stmt);
                    }
                }
                try (PreparedStatement ps =
                        c.prepareStatement("INSERT INTO schema_version (version, name, applied_ms) VALUES (?, ?, ?)")) {
                    ps.setInt(1, version);
                    ps.setString(2, name);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.executeUpdate();
                }
                return null;
            });
        }
    }

    /** Divide un script en sentencias; los scripts de migración no usan ';' dentro de literales. */
    static List<String> statements(String sql) {
        var withoutComments = sql.lines()
                .map(l -> {
                    int i = l.indexOf("--");
                    return i >= 0 ? l.substring(0, i) : l;
                })
                .reduce("", (a, b) -> a + "\n" + b);
        return java.util.Arrays.stream(withoutComments.split(";"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String resource(String path) {
        try (InputStream in = Database.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new DbException("falta la migración " + path, null);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DbException("no se pudo leer " + path, e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DbException("no se pudo cerrar " + file, e);
        }
    }
}
