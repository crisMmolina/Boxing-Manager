package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.JoinUri;
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

import java.time.Duration;

/** La huella del QR no se pierde, la IP inválida no llega a la red y la búsqueda del lugar no espera para siempre. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JoinLookupGuardTest {

    private static final String PIN = "ab".repeat(32);
    private static final String QR = "sigebox://join?h=192.168.43.10&p=8765&c=482913&f=" + PIN;

    private JoinActivity activity;
    private EditText name;
    private EditText code;
    private EditText host;
    private Button submit;
    private TextView error;

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
        activity = Robolectric.buildActivity(JoinActivity.class).setup().get();
        name = activity.findViewById(R.id.join_name);
        code = activity.findViewById(R.id.join_code);
        host = activity.findViewById(R.id.join_host);
        submit = activity.findViewById(R.id.join_submit);
        error = activity.findViewById(R.id.join_error);
    }

    @After
    public void tearDown() {
        JoinActivity.connections = context -> com.sigebox.juez.net.JudgeRuntime.get(context).connection();
        connection.stop();
        db.close();
    }

    @Test
    public void elQrConHuellaLaConservaAlUnirse() {
        activity.onQrScanned(QR);
        name.setText("Ana");
        assertEquals(PIN, activity.current().request.uri.certFingerprint());
    }

    @Test
    public void escribirElNombreNoTiraLaHuellaPeroCambiarIpOCodigoSi() {
        activity.onQrScanned(QR);
        name.setText("Ana");
        name.setText("Ana Ruiz");
        assertEquals(PIN, activity.current().request.uri.certFingerprint());

        host.setText("192.168.43.11");
        assertNull("otra central: la huella del QR ya no aplica", activity.current().request.uri.certFingerprint());

        activity.onQrScanned(QR);
        code.setText("482914");
        assertNull(activity.current().request.uri.certFingerprint());
    }

    @Test
    public void laIpQueNoSeVolveriaUrlSeRechazaAntes() {
        assertEquals(JoinRequest.Error.HOST_INVALID, JoinRequest.withCode("a:b:c", "482913", "Ana").error);
        assertEquals(JoinRequest.Error.HOST_INVALID, JoinRequest.withCode("[fe80::1]:9000", "482913", "Ana").error);
        assertEquals(JoinRequest.Error.HOST_INVALID, JoinRequest.withCode("]]", "482913", "Ana").error);
        assertNotNull(JoinRequest.withCode("fe80::1", "482913", "Ana").request);
        assertNotNull(JoinRequest.withCode("192.168.43.10:9000", "482913", "Ana").request);
    }

    @Test
    public void elQrViejoConHostInvalidoNoPasa() {
        JoinUri uri = new JoinUri("a:b:c", 8765, "F1", 2, "tok123", null);
        assertEquals(JoinRequest.Error.HOST_INVALID, JoinRequest.fromQr(uri, "Ana").error);
    }

    @Test
    public void siNadieContestaAvisaYDejaReintentar() {
        fillAndSubmit();
        assertFalse(submit.isEnabled());

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(JoinActivity.LOOKUP_TIMEOUT_MS));

        assertEquals(View.VISIBLE, error.getVisibility());
        assertEquals(activity.getString(R.string.join_lookup_timeout), error.getText().toString());
        assertTrue("puede reintentar", submit.isEnabled());

        submit.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(submit.isEnabled());
        assertEquals(activity.getString(R.string.join_looking), error.getText().toString());
    }

    @Test
    public void cambiarLaIpQuitaElAvisoDeTiempo() {
        fillAndSubmit();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(JoinActivity.LOOKUP_TIMEOUT_MS));

        host.setText("192.168.43.11");

        assertEquals(View.GONE, error.getVisibility());
    }

    @Test
    public void laRespuestaATiempoCancelaElAviso() {
        fillAndSubmit();
        ConnectionTestAccess.dispatch(connection, new CodeInfo("482913", "F1", 3, "Roja", "Azul"));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(JoinActivity.LOOKUP_TIMEOUT_MS));

        assertEquals(View.GONE, error.getVisibility());
    }

    private void fillAndSubmit() {
        name.setText("Ana");
        host.setText("192.168.43.10");
        code.setText("482913");
        submit.performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }
}
