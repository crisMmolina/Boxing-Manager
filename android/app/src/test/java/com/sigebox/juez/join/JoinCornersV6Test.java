package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.widget.EditText;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.ConnectionTestAccess;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.terms.TermsStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-030: Unirse pasa a Esperando las esquinas que trajo CODE_INFO, para su tarjeta de pelea. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JoinCornersV6Test {

    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        connection = ConnectionTestAccess.newConnection(db);
        JoinActivity.connections = context -> connection;
        new TermsStore(ApplicationProvider.getApplicationContext()).accept();
    }

    @After
    public void tearDown() {
        JoinActivity.connections = context -> com.sigebox.juez.net.JudgeRuntime.get(context).connection();
        connection.stop();
        db.close();
    }

    @Test
    public void elCodigoBuenoLlevaLasEsquinasAEsperando() {
        JoinActivity activity = Robolectric.buildActivity(JoinActivity.class).setup().get();
        ((EditText) activity.findViewById(R.id.join_name)).setText("Ana");
        ((EditText) activity.findViewById(R.id.join_host)).setText("192.168.43.10");
        ((EditText) activity.findViewById(R.id.join_code)).setText("482913");
        activity.findViewById(R.id.join_submit).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ConnectionTestAccess.dispatch(connection, new CodeInfo("482913", "F1", 3, "Pérez", "Gómez"));
        shadowOf(Looper.getMainLooper()).idle();

        Intent next = shadowOf(activity).getNextStartedActivity();
        assertEquals(WaitingActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("Pérez", next.getStringExtra(WaitingActivity.EXTRA_RED));
        assertEquals("Gómez", next.getStringExtra(WaitingActivity.EXTRA_BLUE));
    }
}
