package com.sigebox.core.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.TipoDecision;
import org.junit.jupiter.api.Test;

class DecisionRulesTest {

    /**
     * DECISIÓN ABIERTA (uak decision en CORE-003): votos para una sola esquina y el resto empates. Hoy sigue IDEA §5 al
     * pie de la letra: gana esa esquina por decisión mayoritaria. Si cris/zoe eligen "empate mayoritario", cambia
     * {@link DecisionRules#votosDeUnaEsquinaConEmpates} y este caso.
     */
    @Test
    void decisionAbiertaUnaEsquinaConEmpatesEsMayoritariaSegunIdea5() {
        assertEquals(new DecisionRules.Fallo(Ganador.ROJA, TipoDecision.MAYORITARIA),
                DecisionRules.votosDeUnaEsquinaConEmpates(Ganador.ROJA, 1, 3));
        assertEquals(new DecisionRules.Fallo(Ganador.AZUL, TipoDecision.MAYORITARIA),
                DecisionRules.votosDeUnaEsquinaConEmpates(Ganador.AZUL, 2, 3));
    }

    @Test
    void fallosSinLaReglaAbierta() {
        assertEquals(new DecisionRules.Fallo(Ganador.ROJA, TipoDecision.UNANIME), DecisionRules.fallo(3, 0, 3));
        assertEquals(new DecisionRules.Fallo(Ganador.AZUL, TipoDecision.DIVIDIDA), DecisionRules.fallo(1, 2, 3));
        assertEquals(new DecisionRules.Fallo(Ganador.EMPATE, TipoDecision.UNANIME), DecisionRules.fallo(0, 0, 3));
        assertEquals(new DecisionRules.Fallo(Ganador.EMPATE, TipoDecision.DIVIDIDA), DecisionRules.fallo(1, 1, 3));
    }
}
