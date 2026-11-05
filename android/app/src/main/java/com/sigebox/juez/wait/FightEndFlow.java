package com.sigebox.juez.wait;

import com.sigebox.core.protocol.EventEnded;
import com.sigebox.core.protocol.FightClosed;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.net.JudgeListener;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Spec 004: {@code FIGHT_CLOSED} de la pelea del juez lleva a "Espera la siguiente pelea"; {@code EVENT_ENDED} borra
 * la sesión del lugar y lleva a "El evento terminó". Con envíos sin {@code ACK} en la bandeja, el cambio espera a que
 * se vacíe (principio 1): sin plazo, porque borrar la sesión antes dejaría esos envíos sin entregar.
 */
public final class FightEndFlow implements JudgeListener {

    /** Las pantallas a las que lleva el cierre. */
    public interface Screens {
        void waitNextFight();

        void eventEnded(String judgeName);
    }

    private final Supplier<SessionEntity> session;
    private final Runnable clearSession;
    private final IntSupplier pending;
    private final Runnable stopConnection;
    private final Screens screens;

    // Solo en el hilo de la conexión.
    private String closedFight;
    private boolean eventEnded;

    public FightEndFlow(Supplier<SessionEntity> session, Runnable clearSession, IntSupplier pending,
            Runnable stopConnection, Screens screens) {
        this.session = session;
        this.clearSession = clearSession;
        this.pending = pending;
        this.stopConnection = stopConnection;
        this.screens = screens;
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof FightClosed closed) {
            SessionEntity current = session.get();
            if (current != null && current.approved && closed.fightId().equals(current.fightId)) {
                closedFight = closed.fightId();
                advance();
            }
        } else if (message instanceof EventEnded) {
            // Sin comparar la pelea: un teléfono sin lugar en la última conserva una anterior en la sesión.
            if (session.get() != null) {
                eventEnded = true;
                advance();
            }
        } else if (message instanceof NextFight) {
            closedFight = null; // la central ya abrió la siguiente: manda el aviso global de NEXT_FIGHT
        }
    }

    @Override
    public void onState(ConnectionState state) {
        if ((closedFight != null || eventEnded) && state.pendingAcks == 0) {
            advance();
        }
    }

    private void advance() {
        if (pending.getAsInt() > 0) {
            return;
        }
        SessionEntity current = session.get();
        boolean ended = eventEnded;
        String closed = closedFight;
        eventEnded = false;
        closedFight = null;
        if (current == null) {
            return;
        }
        if (ended) {
            clearSession.run();
            stopConnection.run();
            screens.eventEnded(current.judgeName);
        } else if (closed.equals(current.fightId)) {
            screens.waitNextFight();
        }
    }
}
