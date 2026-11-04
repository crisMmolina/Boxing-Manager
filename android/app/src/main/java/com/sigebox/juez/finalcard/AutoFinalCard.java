package com.sigebox.juez.finalcard;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.net.KeystoreSigner;
import com.sigebox.juez.score.OwnCards;
import com.sigebox.juez.score.ScoreUi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Spec 004 §Cierre sin pedir tarjetas: al cerrarse el último round, con {@code FIGHT_ENDED} o con un {@code STATE} de
 * pelea terminada y {@code finalCardSent = false} (el teléfono que estaba desconectado), arma la tarjeta final con los
 * rounds de la bandeja, la firma y la mete a la bandeja de salida, que la reenvía hasta su {@code ACK} (principio 1).
 * Sin pantalla ni acción del juez. Solo para la pelea y el lugar guardados en la sesión, nunca los que diga un
 * {@code STATE}. Espera a que cada round programado tenga su {@code ACK}; una terminación anticipada (faltan rounds)
 * no manda tarjeta. Nunca la manda dos veces. Si la central la rechazó porque la pelea aún no estaba terminada (o el
 * teléfono no estaba aprobado), la vuelve a mandar cuando la pelea termina; con cualquier otro rechazo, no.
 */
public final class AutoFinalCard implements JudgeListener {

    /** Quien entrega el mensaje a la bandeja de salida ({@code JudgeConnection::submit}). */
    public interface Submitter {
        void submit(Message message);
    }

    private static final AtomicBoolean installed = new AtomicBoolean();

    private final Supplier<SupportSQLiteDatabase> db;
    private final Supplier<SessionEntity> session;
    private final FinalCard.Signer signer;
    private final Submitter submitter;
    private final Executor io;

    // Solo en el hilo de la conexión.
    private String fightId;
    private Integer seat;
    private Integer rounds;
    private boolean ended;
    /** La central ya dio la pelea por terminada ({@code FIGHT_ENDED} o {@code STATE}): se puede reintentar. */
    private boolean over;

    /**
     * Pelea → tarjetas finales que debe tener la bandeja tras el último envío de este proceso; solo en {@code io}.
     * Cubre el rato en que {@code submit} aún no la escribe.
     */
    private final Map<String, Integer> submitted = new HashMap<>();

    /**
     * {@code io} debe ser de un solo hilo: así dos avisos seguidos no arman dos tarjetas. Los mensajes llegan en
     * orden, bajo el candado de los listeners de {@code JudgeConnection}.
     */
    public AutoFinalCard(Supplier<SupportSQLiteDatabase> db, Supplier<SessionEntity> session,
            FinalCard.Signer signer, Submitter submitter, Executor io) {
        this.db = db;
        this.session = session;
        this.signer = signer;
        this.submitter = submitter;
        this.io = io;
    }

    /**
     * Lo registra una sola vez en la conexión del proceso y no lo quita nunca: sigue escuchando con la pantalla
     * apagada o en otra pantalla. Lo llama la pantalla de puntuación, a la que vuelve la app tras un cierre forzado;
     * {@code addListener} le repite el último {@code STATE}, así que un registro tardío no pierde el cierre.
     */
    public static void install(JudgeRuntime runtime) {
        if (!installed.compareAndSet(false, true)) {
            return;
        }
        KeystoreSigner[] signer = new KeystoreSigner[1];
        // La llave se abre en el hilo de I/O, no en el principal.
        FinalCard.Signer lazy = text -> {
            if (signer[0] == null) {
                signer[0] = new KeystoreSigner();
            }
            return signer[0].sign(text);
        };
        runtime.connection().addListener(new AutoFinalCard(
                () -> runtime.database().getOpenHelper().getReadableDatabase(), runtime.database().sessionDao()::load,
                lazy, runtime.connection()::submit,
                Executors.newSingleThreadExecutor()));
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof State state) {
            if (!state.fightId().equals(fightId)) {
                ended = false;
                over = false;
            }
            fightId = state.fightId();
            seat = state.seat();
            rounds = state.rounds();
            boolean terminated = state.fightStatus() == EstadoPelea.TERMINADA
                    || state.fightStatus() == EstadoPelea.CERRADA;
            over = over || (terminated && !state.finalCardSent());
            // El STATE que repite la conexión trae el último ROUND_CLOSED ya aplicado (round = rounds, cerrado).
            boolean lastClosed = rounds != null && state.currentRound() != null
                    && state.currentRound().intValue() == rounds.intValue()
                    && state.roundStatus() == EstadoRound.CERRADO;
            ended = ended || ((terminated || lastClosed) && !state.finalCardSent());
        } else if (message instanceof RoundClosed closed) {
            if (closed.fightId().equals(fightId) && rounds != null && closed.round() == (int) rounds) {
                ended = true;
            }
        } else if (message instanceof FightEnded fightEnded) {
            if (fightEnded.fightId().equals(fightId)) {
                ended = true;
                over = true;
            }
        } else if (message instanceof NextFight) {
            fightId = null;
            ended = false;
            over = false;
            return;
        } else if (!(message instanceof Ack)) {
            return;
        }
        // Un ACK puede confirmar el último round que faltaba: se vuelve a revisar.
        if (ended && fightId != null && seat != null && rounds != null) {
            String fight = fightId;
            int judgeSeat = seat;
            int total = rounds;
            boolean retry = over;
            io.execute(() -> trySend(fight, judgeSeat, total, retry));
        }
    }

    private void trySend(String fight, int judgeSeat, int total, boolean retry) {
        SessionEntity paired = session.get();
        if (paired == null || !fight.equals(paired.fightId) || paired.seat != judgeSeat) {
            return; // un STATE de otra pelea u otro lugar no hace firmar nada
        }
        SupportSQLiteDatabase database = db.get();
        FinalCardStatus status = FinalCardStatus.load(database, fight);
        Integer expected = submitted.get(fight);
        if (expected != null && status.count < expected) {
            return; // el envío anterior aún no llega a la bandeja
        }
        if (status.stored && !(retry && status.retryable)) {
            return;
        }
        Map<Integer, OwnCards.Row> all = OwnCards.load(database, fight);
        Map<Integer, OwnCards.Row> rows = new HashMap<>();
        for (int round = 1; round <= total; round++) {
            OwnCards.Row row = all.get(round);
            if (row == null || row.status == ScoreUi.CardStatus.SENDING) {
                return;
            }
            rows.put(round, row);
        }
        SubmitFinalCard card = FinalCard.build(fight, judgeSeat, FinalCard.toOwnCards(rows), signer);
        submitted.put(fight, status.count + 1);
        submitter.submit(card);
    }
}
