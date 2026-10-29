package com.sigebox.juez.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.SubmitRound;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class OutboxTest {

    private static final String DB = "outbox-test.db";

    private Context context;
    private SigeboxDatabase db;
    private Outbox outbox;
    private long now = 1_000L;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DB);
        open();
    }

    @After
    public void tearDown() {
        db.close();
        context.deleteDatabase(DB);
    }

    private void open() {
        db = Room.databaseBuilder(context, SigeboxDatabase.class, DB).allowMainThreadQueries().build();
        outbox = new Outbox(db.outboxDao(), () -> now);
    }

    @Test
    public void encolaSubmitRoundYSoloLoMarcaAckedConSuAck() {
        SubmitRound submit = new SubmitRound("F1", 2, 3, 10, 9);
        outbox.enqueue(submit);

        List<OutboxEntity> pending = outbox.pending();
        assertEquals(1, pending.size());
        assertEquals(submit.id(), pending.get(0).messageId);
        assertEquals(Messages.encode(submit), pending.get(0).payload);

        assertFalse("un ACK de otro id no cuenta", outbox.onAck(new Ack(java.util.UUID.randomUUID().toString(), true, EstadoTarjeta.ENVIADA, null)));
        assertEquals(1, outbox.pendingCount());

        assertTrue(outbox.onAck(new Ack(submit.id(), true, EstadoTarjeta.ENVIADA, null)));
        assertEquals(0, outbox.pendingCount());
        OutboxEntity done = db.outboxDao().get(submit.id());
        assertTrue(done.acked);
        assertEquals("ENVIADA", done.ackStatus);
    }

    @Test
    public void unAckConErrorTambienTerminaElReenvioYGuardaElError() {
        SubmitRound submit = new SubmitRound("F1", 2, 3, 10, 9);
        outbox.enqueue(submit);
        assertTrue(outbox.onAck(new Ack(submit.id(), false, null, Ack.Error.ROUND_NOT_OPEN)));
        assertEquals(0, outbox.pendingCount());
        assertEquals("ROUND_NOT_OPEN", db.outboxDao().get(submit.id()).ackError);
    }

    @Test
    public void pendientesEnOrdenDeLlegada() {
        SubmitRound r1 = new SubmitRound("F1", 2, 1, 10, 9);
        outbox.enqueue(r1);
        now = 2_000L;
        SubmitRound r2 = new SubmitRound("F1", 2, 2, 9, 10);
        outbox.enqueue(r2);
        List<OutboxEntity> pending = outbox.pending();
        assertEquals(r1.id(), pending.get(0).messageId);
        assertEquals(r2.id(), pending.get(1).messageId);
    }

    @Test
    public void sobreviveACerrarLaApp() {
        SubmitRound submit = new SubmitRound("F1", 2, 3, 10, 9);
        outbox.enqueue(submit);
        db.close();

        open();
        assertEquals(1, outbox.pendingCount());
        assertEquals(submit.id(), outbox.pending().get(0).messageId);
    }

    @Test
    public void marcaEnviadoParaNoReenviarAntesDeTiempo() {
        SubmitRound submit = new SubmitRound("F1", 2, 3, 10, 9);
        outbox.enqueue(submit);
        outbox.markSent(submit.id());
        now = 3_000L;
        assertEquals(0, outbox.dueForResend(5_000L).size());
        now = 7_000L;
        assertEquals(1, outbox.dueForResend(5_000L).size());
    }
}
