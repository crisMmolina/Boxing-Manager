package com.sigebox.core.scoring;

import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.TipoDecision;

/** Del conteo de jueces por esquina al resultado y tipo de decisión (IDEA §5). */
public final class DecisionRules {

    private DecisionRules() {}

    /** Resultado final y tipo. */
    public record Fallo(Ganador ganador, TipoDecision tipo) {}

    /**
     * @param porRoja jueces con Roja
     * @param porAzul jueces con Azul
     * @param jueces jueces de la pelea (los demás dieron empate)
     */
    public static Fallo fallo(int porRoja, int porAzul, int jueces) {
        if (porRoja == porAzul) {
            return new Fallo(Ganador.EMPATE, porRoja == 0 ? TipoDecision.UNANIME : TipoDecision.DIVIDIDA);
        }
        Ganador esquina = porRoja > porAzul ? Ganador.ROJA : Ganador.AZUL;
        int aFavor = Math.max(porRoja, porAzul);
        if (aFavor == jueces) {
            return new Fallo(esquina, TipoDecision.UNANIME);
        }
        if (Math.min(porRoja, porAzul) == 0) {
            return votosDeUnaEsquinaConEmpates(esquina, aFavor, jueces);
        }
        return new Fallo(esquina, TipoDecision.DIVIDIDA);
    }

    /**
     * DECISIÓN ABIERTA (uak decision en CORE-003): algunos jueces con una esquina y el resto empates. IDEA §5 al pie de
     * la letra: gana esa esquina por decisión mayoritaria. La alternativa del box profesional ("empate mayoritario"
     * cuando los empates son mayoría) se cambia aquí.
     */
    static Fallo votosDeUnaEsquinaConEmpates(Ganador esquina, int aFavor, int jueces) {
        return new Fallo(esquina, TipoDecision.MAYORITARIA);
    }
}
