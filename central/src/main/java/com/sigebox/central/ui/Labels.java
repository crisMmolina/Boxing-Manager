package com.sigebox.central.ui;

import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.TipoDecision;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * Etiquetas en español de los códigos que guarda la central (spec 003 §Español en las interfaces).
 * Solo se traduce al mostrar: la base de datos sigue con los códigos.
 */
public final class Labels {

    /** Tipo de evento de {@code event_log} y {@code funcion_log} → etiqueta. */
    public static final Map<String, String> EVENTOS = Map.ofEntries(
            Map.entry("CARD_CAPTURED", "Tarjeta capturada desde papel"),
            Map.entry("CARD_CORRECTED", "Tarjeta corregida"),
            Map.entry("CARTELERA_CAMBIADA", "Cartelera cambiada"),
            Map.entry("CONTROL_REJECTED", "Mensaje de control rechazado"),
            Map.entry("FIGHT_CLOSED", "Pelea cerrada"),
            Map.entry("FIGHT_CREATED", "Pelea creada"),
            Map.entry("FIGHT_ENDED", "Pelea terminada"),
            Map.entry("FIGHT_TERMINATED", "Terminación extraordinaria"),
            Map.entry("FINAL_CARD_REJECTED", "Tarjeta final rechazada"),
            Map.entry("FUNCION_CREADA", "Función creada"),
            Map.entry("INVITATION_ISSUED", "Invitación emitida"),
            Map.entry("JOIN_APPROVED", "Juez aprobado"),
            Map.entry("JOIN_REJECTED", "Juez rechazado"),
            Map.entry("JOIN_REQUESTED", "Solicitud de juez"),
            Map.entry("JUDGE_CONNECTED", "Juez conectado"),
            Map.entry("JUDGE_DISCONNECTED", "Juez desconectado"),
            Map.entry("MALFORMED", "Mensaje mal formado"),
            Map.entry("NEXT_FIGHT_REJECTED", "Siguiente pelea rechazada"),
            Map.entry("NEXT_FIGHT_SENT", "Siguiente pelea enviada"),
            Map.entry("PARTIAL_SUMMARY_VIEWED", "Resumen parcial consultado"),
            Map.entry("PELEA_SALTADA", "Pelea saltada"),
            Map.entry("ROUND_CLOSED", "Round cerrado"),
            Map.entry("ROUND_CLOSED_FORCED", "Round cerrado a la fuerza"),
            Map.entry("ROUND_OPENED", "Round abierto"),
            Map.entry("SIGUIENTE_PELEA", "Siguiente pelea"),
            Map.entry("SUBMIT_FINAL_CARD", "Tarjeta final"),
            Map.entry("SUBMIT_ROUND", "Tarjeta de round"),
            Map.entry("TERMS_ACCEPTED", "Términos aceptados"),
            Map.entry("TERMS_DECLINED", "Términos rechazados"));

    private static final String SEP = " · ";

    private Labels() {
    }

    public static String of(EstadoPelea e) {
        return switch (e) {
            case CREADA -> "Creada";
            case EN_CURSO -> "En curso";
            case TERMINADA -> "Terminada";
            case CERRADA -> "Cerrada";
        };
    }

    public static String of(EstadoRound e) {
        return switch (e) {
            case PENDIENTE -> "Pendiente";
            case ABIERTO -> "Abierto";
            case CERRADO -> "Cerrado";
        };
    }

    public static String of(EstadoTarjeta e) {
        return switch (e) {
            case ENVIADA -> "Enviada";
            case TARDIA -> "Tardía";
            case CAPTURADA -> "Capturada desde papel";
            case CORREGIDA -> "Corregida";
        };
    }

    public static String of(EstadoConexion e) {
        return switch (e) {
            case PENDIENTE -> "Pendiente";
            case CONECTADO -> "Conectado";
            case DESCONECTADO -> "Desconectado";
            case RECHAZADO -> "Rechazado";
        };
    }

    /** Igual que en el titular de {@code ResultService}, con mayúscula inicial. */
    public static String of(TipoDecision e) {
        return switch (e) {
            case UNANIME -> "Unánime";
            case MAYORITARIA -> "Mayoritaria";
            case DIVIDIDA -> "Dividida";
        };
    }

    /** Igual que en el titular de {@code ResultService}, con mayúscula inicial. */
    public static String of(MotivoTerminacion e) {
        return switch (e) {
            case KO -> "KO";
            case KOT -> "KO técnico";
            case MEDICO -> "Detención médica";
            case ABANDONO -> "Abandono";
            case DESCALIFICACION -> "Descalificación";
            case DECISION_TECNICA -> "Decisión técnica";
            case SIN_DECISION -> "Sin decisión";
            case OTRO -> "Otro motivo";
        };
    }

    public static String of(Ganador e) {
        return switch (e) {
            case ROJA -> "Roja";
            case AZUL -> "Azul";
            case EMPATE -> "Empate";
            case SIN_DECISION -> "Sin decisión";
        };
    }

    public static String of(OrigenTarjeta e) {
        return switch (e) {
            case JUEZ -> "Juez";
            case ORGANIZADOR -> "Organizador";
        };
    }

    public static String of(Esquina e) {
        return switch (e) {
            case ROJA -> "Roja";
            case AZUL -> "Azul";
        };
    }

    /** Etiqueta del tipo de evento; un tipo sin etiqueta (filas viejas) se muestra como su código. */
    public static String evento(String tipo) {
        if (tipo == null) {
            return "";
        }
        return EVENTOS.getOrDefault(tipo, tipo);
    }

    /**
     * Detalle del evento en español: "lugar 2 round 3 TARDIA" → "Juez 2 · round 3 · tardía". Solo se
     * leen los formatos que escribe la central; los textos libres (titular, cartelera) no se tocan.
     */
    public static String mensaje(String tipo, String detalle) {
        if (detalle == null) {
            return "";
        }
        Function<String, String> code = switch (tipo == null ? "" : tipo) {
            case "SUBMIT_ROUND" -> t -> lower(enumLabel(EstadoTarjeta.class, t, Labels::of));
            case "FIGHT_TERMINATED" -> t -> enumLabel(MotivoTerminacion.class, t, Labels::of);
            case "FIGHT_CLOSED", "SIGUIENTE_PELEA", "FUNCION_CREADA", "CARTELERA_CAMBIADA", "PELEA_SALTADA" -> null;
            default -> t -> null;
        };
        if (code == null) {
            return detalle;
        }
        var tokens = detalle.split(" ");
        var parts = new ArrayList<String>();
        var rest = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            var t = tokens[i];
            var next = i + 1 < tokens.length ? tokens[i + 1] : "";
            String part = null;
            if ((t.equals("lugar") || t.equals("juez")) && next.matches("\\d+")) {
                part = "Juez " + next;
                i++;
            } else if (t.equals("round") && next.matches("\\d+")) {
                part = "round " + next;
                i++;
            } else if (t.equals("gana") && enumLabel(Esquina.class, next, Labels::of) != null) {
                part = "gana " + lower(enumLabel(Esquina.class, next, Labels::of));
                i++;
            } else {
                part = code.apply(t);
            }
            if (part == null) {
                rest.append(rest.isEmpty() ? "" : " ").append(t);
                continue;
            }
            flush(parts, rest);
            parts.add(part);
        }
        flush(parts, rest);
        return String.join(SEP, parts);
    }

    /** Tipo y detalle en una línea: "Tarjeta de round · Juez 2 · round 3 · tardía". */
    public static String linea(String tipo, String detalle) {
        var m = mensaje(tipo, detalle);
        return m.isEmpty() ? evento(tipo) : evento(tipo) + SEP + m;
    }

    private static void flush(ArrayList<String> parts, StringBuilder rest) {
        if (!rest.isEmpty()) {
            parts.add(rest.toString());
            rest.setLength(0);
        }
    }

    private static <E extends Enum<E>> String enumLabel(Class<E> type, String name, Function<E, String> label) {
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.name().equals(name)).findFirst()
                .map(label).orElse(null);
    }

    /** "Tardía" → "tardía"; "KO técnico" se queda igual. */
    private static String lower(String s) {
        if (s == null || s.length() < 2 || !Character.isLowerCase(s.charAt(1))) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
