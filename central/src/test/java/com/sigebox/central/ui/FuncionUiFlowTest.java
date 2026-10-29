package com.sigebox.central.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.service.FightService.SeatState;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.FuncionService.Estado;
import com.sigebox.central.ui.funcion.FuncionFlow;
import com.sigebox.central.ui.funcion.FuncionQuery;
import com.sigebox.central.ui.funcion.FuncionView;
import com.sigebox.judgesim.SimJudge;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * DSK-021: lo que hacen la pantalla Función y el botón "Siguiente pelea" de Resultado, contra la central
 * real y jueces de judge-sim. {@code App.funciones} es el enlace que avisa a los teléfonos.
 */
class FuncionUiFlowTest {

    private static final int ROUNDS = 2;

    @TempDir
    Path dir;

    @Test
    void theNextFightMovesConnectedJudgesAndOnlyNewSeatsNeedAQr() throws Exception {
        try (var rig = new JudgeSimRig(dir.resolve("sigebox.db"))) {
            var flow = new FuncionFlow(rig.funciones, new FuncionQuery(rig.db));
            assertTrue(flow.funcionId().isEmpty());
            assertTrue(flow.peek().isEmpty());

            var f = rig.funciones.crear(ROUNDS, 2, List.of(new Esquinas("Roja 1", "Azul 1"),
                    new Esquinas("Roja 2", "Azul 2"), new Esquinas("Roja 3", "Azul 3")));
            assertEquals(f.id(), flow.funcionId().orElseThrow());
            rig.funciones.cambiar(f.id(), 2, null, 3);
            rig.funciones.saltar(f.id(), 3);
            assertEquals("Siguiente pelea: Roja 1 vs Azul 1", FuncionView.siguienteTexto(flow.peek().orElseThrow()));

            // Pelea 1: dos jueces hacen JOIN y se aprueban una sola vez.
            var f1 = flow.start().id();
            rig.join(f1, 1, ROUNDS);
            rig.join(f1, 2, ROUNDS);
            JudgeSimRig.await(() -> rig.judges.stream().allMatch(j -> j.phase() == SimJudge.Phase.APPROVED), 20_000,
                    "aprobación");
            rig.play(f1, ROUNDS);
            rig.finish(f1);

            // Resultado ofrece la pelea 2 (la 3 está saltada) con sus 3 jueces.
            var next = flow.peek().orElseThrow();
            assertEquals(2, next.orden());
            assertEquals(3, next.jueces());
            assertEquals("Siguiente pelea: Roja 2 vs Azul 2", FuncionView.siguienteTexto(next));

            var f2 = flow.start().id();
            JudgeSimRig.await(() -> rig.judges.stream().allMatch(j -> j.fightId().equals(f2)
                    && j.phase() == SimJudge.Phase.APPROVED) && rig.hub.connectedSeats(f2).equals(List.of(1, 2)),
                    5_000, "los dos jueces en la pelea 2");

            // Jueces: 1 y 2 ya conectados y aprobados; solo el lugar 3 (nuevo) queda para el QR.
            var seats = rig.fights.seats(f2, rig.hub.connectedSeats(f2));
            assertEquals(List.of(SeatState.CONECTADO, SeatState.CONECTADO, SeatState.SIN_CONECTAR),
                    seats.stream().map(s -> s.state()).toList());

            var estados = rig.funciones.cartelera(f.id()).stream().map(r -> r.estado()).toList();
            assertEquals(List.of(Estado.CERRADA, Estado.EN_CURSO, Estado.SALTADA), estados);
        }
    }
}
