package com.sigebox.central.ui.inicio;

import com.sigebox.central.db.Database;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Estado;
import com.sigebox.central.ui.funcion.FuncionView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Historial de eventos para Inicio. Ordena igual que el paso Evento ({@code FuncionQuery.latest}): por
 * creación, el más reciente primero; así la primera fila siempre es el evento que abre Evento. El estado de
 * cada pelea sale de {@link FuncionService#cartelera}, el mismo que ve la cartelera.
 */
public final class InicioQuery {

    /** Una fila de Inicio; {@code ultimo} = es el evento en curso (el que edita el paso Evento). */
    public record Evento(String id, String nombre, String fecha, String resumen, boolean ultimo) {}

    /** A dónde lleva abrir un evento. */
    public enum Destino {
        /** Tiene una pelea activa: En vivo. */
        EN_VIVO,
        /** Es el evento en curso sin pelea activa: su cartelera editable. */
        EVENTO,
        /** Un evento anterior: su cartelera, solo para consultar. */
        CARTELERA
    }

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-MX"));

    private final Database db;
    private final FuncionService funciones;

    public InicioQuery(Database db, FuncionService funciones) {
        this.db = db;
        this.funciones = funciones;
    }

    /** Ids del más reciente al más antiguo (mismo orden que {@code FuncionQuery.latest}). */
    List<String> ids() {
        return db.inTransaction(c -> {
            var out = new ArrayList<String>();
            try (var ps = c.prepareStatement("SELECT id FROM funcion ORDER BY created_ms DESC, rowid DESC");
                    var rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
            return out;
        });
    }

    public List<Evento> eventos() {
        var ids = ids();
        var out = new ArrayList<Evento>();
        for (int i = 0; i < ids.size(); i++) {
            var f = funciones.funcion(ids.get(i));
            out.add(new Evento(f.id(), nombre(f), fecha(f.fecha()),
                    FuncionView.resumen(funciones.cartelera(f.id())), i == 0));
        }
        return out;
    }

    public Destino destino(String id) {
        var ids = ids();
        if (ids.isEmpty() || !ids.get(0).equals(id)) {
            return Destino.CARTELERA;
        }
        return funciones.cartelera(id).stream().anyMatch(f -> f.estado() == Estado.EN_CURSO)
                ? Destino.EN_VIVO : Destino.EVENTO;
    }

    /** El nombre guardado o, si no tiene, "Evento 27 oct 2026" (DESIGN v4 §7.1). */
    static String nombre(FuncionService.Funcion f) {
        return f.nombre() == null || f.nombre().isBlank() ? "Evento " + fecha(f.fecha()) : f.nombre();
    }

    /** "3 nov 2026" (sin el punto que Java pone a la abreviatura del mes). */
    static String fecha(String iso) {
        try {
            return FECHA.format(LocalDate.parse(iso)).replace(".", "");
        } catch (DateTimeParseException e) {
            return iso;
        }
    }
}
