package com.sigebox.juez.finalcard;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.score.OwnCards;
import com.sigebox.juez.score.ScoreUi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Spec 004 §Cierre sin pedir tarjetas: al cerrarse el último round, o con un {@code STATE} de pelea terminada y
 * {@code finalCardSent = false} (el teléfono que estaba desconectado), arma la tarjeta final con los rounds de la
 * bandeja, la firma y la mete a la bandeja de salida, que la reenvía hasta su {@code ACK} (principio 1). Sin pantalla
 * ni acción del juez. Espera a que cada round programado tenga su {@code ACK}; una terminación anticipada (faltan
 * rounds) no manda tarjeta. Nunca la manda dos veces, ni la reintenta si la central la rechazó.
 */
public final class AutoFinalCard implements JudgeListener {

    /** Quien entrega el mensaje a la bandeja de salida ({@code JudgeConnection::submit}). */
    public interface Submitter {
        void submit(Message message);
    }

    private final Supplier<SupportSQLiteDatabase> db;
    private final FinalCard.Signer signer;
    private final Submitter submitter;
    private final Executor io;

    // Solo en el hilo de la conexión.
    private String fightId;
    private Integer seat;
    private Integer rounds;
    private boolean ended;

    /** Peleas cuya tarjeta final ya se metió a la bandeja en este proceso; solo en {@code io}. */
    private final Set<String> submitted = new HashSet<>();

    /** {@code io} debe ser de un solo hilo: así dos avisos seguidos no arman dos tarjetas. */
    public AutoFinalCard(Supplier<SupportSQLiteDatabase> db, FinalCard.Signer signer, Submitter submitter,
            Executor io) {
        this.db = db;
        this.signer = signer;
        this.submitter = submitter;
        this.io = io;
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof State state) {
            if (!state.fightId().equals(fightId)) {
                ended = false;
            }
            fightId = state.fightId();
            seat = state.seat();
            rounds = state.rounds();
            boolean over = state.fightStatus() == EstadoPelea.TERMINADA
                    || state.fightStatus() == EstadoPelea.CERRADA;
            ended = ended || (over && !state.finalCardSent());
        } else if (message instanceof RoundClosed closed) {
            if (closed.fightId().equals(fightId) && rounds != null && closed.round() == (int) rounds) {
                ended = true;
            }
        } else if (message instanceof NextFight) {
            fightId = null;
            ended = false;
            return;
        } else if (!(message instanceof Ack)) {
            return;
        }
        // Un ACK puede confirmar el último round que faltaba: se vuelve a revisar.
        if (ended && fightId != null && seat != null && rounds != null) {
            String fight = fightId;
            int judgeSeat = seat;
            int total = rounds;
            io.execute(() -> trySend(fight, judgeSeat, total));
        }
    }

    private void trySend(String fight, int judgeSeat, int total) {
        if (submitted.contains(fight)) {
            return;
        }
        SupportSQLiteDatabase database = db.get();
        if (FinalCardStatus.load(database, fight).stored) {
            submitted.add(fight);
            return;
        }
        Map<Integer, OwnCards.Row> rows = OwnCards.load(database, fight);
        for (int round = 1; round <= total; round++) {
            OwnCards.Row row = rows.get(round);
            if (row == null || row.status == ScoreUi.CardStatus.SENDING) {
                return;
            }
        }
        SubmitFinalCard card = FinalCard.build(fight, judgeSeat, FinalCard.toOwnCards(rows), signer);
        submitted.add(fight);
        submitter.submit(card);
    }
}
