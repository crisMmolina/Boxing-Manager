package com.sigebox.central.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** Ayudas JDBC para columnas que admiten NULL. */
final class Sql {

    private Sql() {
    }

    static void setLong(PreparedStatement ps, int i, Long v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setLong(i, v);
        }
    }

    static void setInt(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setInt(i, v);
        }
    }

    static void setEnum(PreparedStatement ps, int i, Enum<?> v) throws SQLException {
        ps.setString(i, v == null ? null : v.name());
    }

    static Long getLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    static Integer getInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    static <E extends Enum<E>> E getEnum(ResultSet rs, String col, Class<E> type) throws SQLException {
        var v = rs.getString(col);
        return v == null ? null : Enum.valueOf(type, v);
    }
}
