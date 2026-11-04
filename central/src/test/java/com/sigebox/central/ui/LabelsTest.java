package com.sigebox.central.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.TipoDecision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Nada de lo que muestra la central queda como código: cada enum y cada tipo de evento tiene etiqueta. */
class LabelsTest {

    @Test
    void cadaEstadoDeTarjetaTieneEtiqueta() {
        assertLabels(EstadoTarjeta.values(), Labels::of);
        assertEquals("Tardía", Labels.of(EstadoTarjeta.TARDIA));
    }

    @Test
    void cadaValorDeLosDemasEnumsTieneEtiqueta() {
        assertLabels(EstadoPelea.values(), Labels::of);
        assertLabels(EstadoRound.values(), Labels::of);
        assertLabels(EstadoConexion.values(), Labels::of);
        assertLabels(TipoDecision.values(), Labels::of);
        assertLabels(MotivoTerminacion.values(), Labels::of);
        assertLabels(Ganador.values(), Labels::of);
        assertLabels(OrigenTarjeta.values(), Labels::of);
        assertEquals("KO técnico", Labels.of(MotivoTerminacion.KOT));
        assertEquals("En curso", Labels.of(EstadoPelea.EN_CURSO));
    }

    /** Recorre el código de la central: todo literal con forma de código de evento debe tener etiqueta. */
    @Test
    void cadaTipoDeEventoQueEscribeLaCentralTieneEtiqueta() throws IOException {
        var codes = new TreeSet<String>();
        var literal = Pattern.compile("\"([A-Z]+(?:_[A-Z]+)+|MALFORMED)\"");
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (var f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var m = literal.matcher(Files.readString(f));
                while (m.find()) {
                    codes.add(m.group(1));
                }
            }
        }
        assertTrue(codes.contains("SUBMIT_ROUND") && codes.contains("TERMS_ACCEPTED"), codes.toString());
        var missing = codes.stream().filter(c -> !Labels.EVENTOS.containsKey(c)).toList();
        assertEquals(List.of(), missing, "tipos sin etiqueta");
        for (var label : Labels.EVENTOS.values()) {
            assertFalse(label.isBlank());
            assertFalse(label.contains("_"), label);
        }
    }

    @Test
    void unTipoDesconocidoSeMuestraComoSuCodigo() {
        assertEquals("APP_START", Labels.evento("APP_START"));
        assertEquals("", Labels.evento(null));
    }

    @Test
    void mensajeDeTarjetaDeRound() {
        assertEquals("Tarjeta de round", Labels.evento("SUBMIT_ROUND"));
        assertEquals("Juez 2 · round 3 · tardía", Labels.mensaje("SUBMIT_ROUND", "lugar 2 round 3 TARDIA"));
        assertEquals("Tarjeta de round · Juez 2 · round 3 · tardía",
                Labels.linea("SUBMIT_ROUND", "lugar 2 round 3 TARDIA"));
    }

    @Test
    void mensajesConLugarRoundYCodigos() {
        assertEquals("Juez 1", Labels.mensaje("JOIN_APPROVED", "lugar 1"));
        assertEquals("Juez 3 · al reconectar", Labels.mensaje("NEXT_FIGHT_SENT", "lugar 3 al reconectar"));
        assertEquals("Juez 2 · round 1 · 10-9 → 9-10", Labels.mensaje("CARD_CORRECTED", "juez 2 round 1 10-9 → 9-10"));
        assertEquals("round 3 · faltantes [1, 2]", Labels.mensaje("ROUND_CLOSED_FORCED", "round 3 faltantes [1, 2]"));
        assertEquals("KO técnico · round 3 · 1:42 · gana roja",
                Labels.mensaje("FIGHT_TERMINATED", "KOT round 3 1:42 gana ROJA"));
        assertEquals("Términos aceptados", Labels.linea("TERMS_ACCEPTED", null));
    }

    @Test
    void elTitularYLosTextosLibresNoSeTocan() {
        var headline = "Gana KO Pérez por KO (round 3, 1:42)";
        assertEquals(headline, Labels.mensaje("FIGHT_CLOSED", headline));
        assertEquals("función f-1, sigue p-2", Labels.mensaje("SIGUIENTE_PELEA", "función f-1, sigue p-2"));
        assertEquals("", Labels.mensaje("FIGHT_ENDED", null));
    }

    private static <E extends Enum<E>> void assertLabels(E[] values, Function<E, String> label) {
        Set<String> seen = new HashSet<>();
        for (var v : values) {
            var text = label.apply(v);
            assertFalse(text == null || text.isBlank(), v.name());
            assertFalse(text.contains("_"), text);
            if (v.name().length() > 3) { // KO y KOT se dicen igual en español
                assertNotEquals(v.name(), text);
            }
            assertTrue(seen.add(text), "etiqueta repetida: " + text);
        }
    }
}
