package com.sigebox.judgesim;

import com.google.gson.Gson;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.TarjetaRound;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Guion de una pelea ({@code demo_data/pelea_referencia.json}): qué califica cada juez y cuándo. */
public record Script(Pelea pelea, List<LugarJuez> lugares, List<TarjetaRound> tarjetas) {

    /** Una calificación del guion; {@code late} = se envía después de que el round cierra. */
    public record Plan(int round, int red, int blue, boolean late) {}

    public static Script load(Path file) {
        try {
            var script = new Gson().fromJson(Files.readString(file, StandardCharsets.UTF_8), Script.class);
            if (script == null || script.pelea() == null || script.tarjetas() == null) {
                throw new IllegalArgumentException("guion incompleto: " + file);
            }
            return script;
        } catch (IOException e) {
            throw new IllegalArgumentException("no se pudo leer " + file, e);
        }
    }

    /** Calificaciones del juez {@code seat} por round. */
    public Map<Integer, Plan> plansFor(int seat) {
        var out = new TreeMap<Integer, Plan>();
        for (var t : tarjetas) {
            if (t.juez() == seat) {
                out.put(t.round(), new Plan(t.round(), t.rojo(), t.azul(), t.estado() == EstadoTarjeta.TARDIA));
            }
        }
        return out;
    }

    /** Nombre del juez en el guion, o "Sim N". */
    public String nameFor(int seat) {
        if (lugares != null) {
            for (var l : lugares) {
                if (l.numero() == seat && l.nombre() != null) {
                    return l.nombre();
                }
            }
        }
        return "Sim " + seat;
    }
}
