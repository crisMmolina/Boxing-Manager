package com.sigebox.juez.finalcard;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.FinalCardHash;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.juez.score.OwnCards;
import com.sigebox.juez.score.ScoreUi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Arma {@code SUBMIT_FINAL_CARD}: hash de las tarjetas propias y firma del teléfono (no repudio). */
public final class FinalCard {

    /** La llave del teléfono ({@code KeystoreSigner::sign}). */
    public interface Signer {
        String sign(String text);
    }

    private FinalCard() {}

    public static SubmitFinalCard build(String fightId, int seat, List<State.OwnCard> cards, Signer signer) {
        String hash = FinalCardHash.of(fightId, seat, cards);
        String sig = signer.sign(DeviceProof.finalCardText(fightId, seat, cards));
        return new SubmitFinalCard(fightId, seat, hash, sig);
    }

    /** Filas de la pantalla → tarjetas del protocolo, en orden de round. */
    public static List<State.OwnCard> toOwnCards(Map<Integer, OwnCards.Row> rows) {
        List<OwnCards.Row> sorted = new ArrayList<>(rows.values());
        sorted.sort(Comparator.comparingInt(row -> row.round));
        List<State.OwnCard> cards = new ArrayList<>();
        for (OwnCards.Row row : sorted) {
            cards.add(new State.OwnCard(row.round, row.red, row.blue, toEstado(row.status)));
        }
        return cards;
    }

    private static EstadoTarjeta toEstado(ScoreUi.CardStatus status) {
        switch (status) {
            case LATE:
                return EstadoTarjeta.TARDIA;
            case CORRECTED:
                return EstadoTarjeta.CORREGIDA;
            case SENDING:
            case SENT:
            default:
                return EstadoTarjeta.ENVIADA;
        }
    }
}
