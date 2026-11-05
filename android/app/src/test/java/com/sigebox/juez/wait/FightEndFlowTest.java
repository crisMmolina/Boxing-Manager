package com.sigebox.juez.wait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.sigebox.core.protocol.EventEnded;
import com.sigebox.core.protocol.FightClosed;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.net.ConnectionState;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FightEndFlowTest {
    private final List<String> calls = new ArrayList<>();
    private SessionEntity session;
    private int pending;
    private FightEndFlow flow;

    @Before
    public void setUp() {
        session = new SessionEntity();
        session.fightId = "F1";
        session.judgeName = "Ana";
        session.approved = true;
        flow = new FightEndFlow(() -> session, () -> {
            session = null;
            calls.add("clear");
        }, () -> pending, () -> calls.add("stop"), new FightEndFlow.Screens() {
            @Override
            public void waitNextFight() {
                calls.add("wait");
            }

            @Override
            public void eventEnded(String judgeName) {
                calls.add("ended:" + judgeName);
            }
        });
    }

    @Test
    public void fightClosedWithNothingPendingOpensTheWaitScreen() {
        flow.onMessage(new FightClosed("F1"));

        assertEquals(List.of("wait"), calls);
    }

    @Test
    public void fightClosedOfAnotherFightOrWithoutApprovalIsIgnored() {
        flow.onMessage(new FightClosed("F2"));
        session.approved = false;
        flow.onMessage(new FightClosed("F1"));

        assertEquals(List.of(), calls);
    }

    @Test
    public void pendingSendsAreDeliveredBeforeTheWaitScreen() {
        pending = 2;
        flow.onMessage(new FightClosed("F1"));
        flow.onState(pendingState(2));
        assertEquals(List.of(), calls);

        pending = 1;
        flow.onState(pendingState(1));
        assertEquals(List.of(), calls);

        pending = 0;
        flow.onState(pendingState(0));
        flow.onState(pendingState(0));
        assertEquals(List.of("wait"), calls);
    }

    @Test
    public void aStateThatDisagreesWithTheOutboxDoesNotReleaseTheScreen() {
        pending = 1;
        flow.onMessage(new FightClosed("F1"));

        flow.onState(pendingState(0));

        assertEquals(List.of(), calls);
    }

    @Test
    public void nextFightDropsTheDeferredWait() {
        pending = 1;
        flow.onMessage(new FightClosed("F1"));
        flow.onMessage(new NextFight("F1", "F2", 2, "Rojo", "Azul", 3));

        pending = 0;
        flow.onState(pendingState(0));

        assertEquals(List.of(), calls);
    }

    @Test
    public void theFightChangedWhileDeferredSoTheWaitIsDropped() {
        pending = 1;
        flow.onMessage(new FightClosed("F1"));
        session.fightId = "F2";

        pending = 0;
        flow.onState(pendingState(0));

        assertEquals(List.of(), calls);
    }

    @Test
    public void eventEndedErasesTheSessionStopsAndKeepsTheName() {
        flow.onMessage(new EventEnded("F1"));

        assertNull(session);
        assertEquals(List.of("clear", "stop", "ended:Ana"), calls);
    }

    /** Un teléfono sin lugar en la última pelea conserva en la sesión una pelea anterior. */
    @Test
    public void eventEndedAppliesEvenIfTheSessionHasAnOlderFight() {
        flow.onMessage(new EventEnded("F7"));

        assertEquals(List.of("clear", "stop", "ended:Ana"), calls);
    }

    @Test
    public void eventEndedWithoutSessionDoesNothing() {
        session = null;

        flow.onMessage(new EventEnded("F1"));

        assertEquals(List.of(), calls);
    }

    @Test
    public void pendingSendsAreDeliveredBeforeErasingTheSession() {
        pending = 1;
        flow.onMessage(new FightClosed("F1"));
        flow.onMessage(new EventEnded("F1"));
        flow.onState(pendingState(1));
        assertNotNull(session);
        assertEquals(List.of(), calls);

        pending = 0;
        flow.onState(pendingState(0));

        assertNull(session);
        assertEquals(List.of("clear", "stop", "ended:Ana"), calls); // el fin del evento gana a la espera
    }

    private static ConnectionState pendingState(int pendingAcks) {
        return new ConnectionState(ConnectionState.Status.DISCONNECTED, null, null, pendingAcks, null);
    }
}
