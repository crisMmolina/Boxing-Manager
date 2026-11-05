package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.juez.R;
import com.sigebox.juez.net.ConnectionTestAccess;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.terms.TermsStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Unirse con nombre, código de 6 dígitos e IP (spec 004 §Unirse con código y QR, ADR 0004). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JoinCodeTest {

    private JoinActivity activity;
    private EditText name;
    private EditText code;
    private EditText host;
    private Button submit;

    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        connection = ConnectionTestAccess.newConnection(db); // sin llave del equipo ni red real
        JoinActivity.connections = context -> connection;
        new TermsStore(ApplicationProvider.getApplicationContext()).accept();
        activity = Robolectric.buildActivity(JoinActivity.class).setup().get();
        name = activity.findViewById(R.id.join_name);
        code = activity.findViewById(R.id.join_code);
        host = activity.findViewById(R.id.join_host);
        submit = activity.findViewById(R.id.join_submit);
    }

    @After
    public void tearDown() {
        JoinActivity.connections = context -> com.sigebox.juez.net.JudgeRuntime.get(context).connection();
        connection.stop();
        db.close();
    }

    @Test
    public void elCodigoEsDeExactamenteSeisDigitos() {
        assertEquals(JoinRequest.Error.CODE_INVALID, JoinRequest.withCode("192.168.43.10", "48291", "Ana").error);
        assertEquals(JoinRequest.Error.CODE_INVALID, JoinRequest.withCode("192.168.43.10", "48a913", "Ana").error);
        assertEquals(JoinRequest.Error.CODE_INVALID, JoinRequest.withCode("192.168.43.10", "4829130", "Ana").error);
        assertEquals(JoinRequest.Error.HOST_INVALID, JoinRequest.withCode("", "482913", "Ana").error);
        assertEquals(JoinRequest.Error.NAME_EMPTY, JoinRequest.withCode("192.168.43.10", "482913", " ").error);

        JoinRequest ok = JoinRequest.withCode(" 10.0.0.5:9000 ", "482913", "Ana Ruiz").request;
        assertEquals("10.0.0.5", ok.uri.host());
        assertEquals(9000, ok.uri.port());
        assertEquals("482913", ok.uri.code());
        assertNull(ok.uri.fightId());
    }

    @Test
    public void soloPideNombreCodigoEIpYElCodigoUsaTecladoNumerico() {
        assertEquals(View.VISIBLE, name.getVisibility());
        assertEquals(View.VISIBLE, code.getVisibility());
        assertEquals(View.VISIBLE, host.getVisibility());
        assertEquals(InputType.TYPE_CLASS_NUMBER, code.getInputType() & InputType.TYPE_MASK_CLASS);
        int max = -1;
        for (InputFilter f : code.getFilters()) {
            if (f instanceof InputFilter.LengthFilter) {
                max = ((InputFilter.LengthFilter) f).getMax();
            }
        }
        assertEquals(6, max);

        code.setText("12a4");
        assertFalse("solo dígitos", code.getText().toString().contains("a"));

        name.setText("Ana");
        host.setText("192.168.43.10");
        code.setText("48291");
        assertFalse(submit.isEnabled());
        code.setText("482913");
        assertTrue(submit.isEnabled());
    }

    @Test
    public void escanearQrEsLaAccionPrincipal() {
        Button scan = activity.findViewById(R.id.join_scan);
        assertEquals(View.VISIBLE, scan.getVisibility());
        assertEquals("estilo principal (texto on_primary sobre primary)",
                activity.getColor(R.color.on_primary), scan.getCurrentTextColor());
        assertEquals(activity.getString(R.string.join_scan), scan.getText().toString());
        android.view.ViewGroup container = (android.view.ViewGroup) scan.getParent();
        assertTrue("arriba de los campos", container.indexOfChild(scan) < container.indexOfChild(childHolding(container, code)));
    }

    /** El hijo directo de {@code container} que contiene a {@code view}. */
    private static View childHolding(android.view.ViewGroup container, View view) {
        View v = view;
        while (v.getParent() != container) {
            v = (View) v.getParent();
        }
        return v;
    }

    @Test
    public void elQrNuevoLlenaIpYCodigo() {
        activity.onQrScanned("sigebox://join?h=192.168.43.10&p=8765&c=482913");
        assertEquals("192.168.43.10", host.getText().toString());
        assertEquals("482913", code.getText().toString());
        name.setText("Ana");
        assertTrue(submit.isEnabled());
    }

    @Test
    public void elQrNuevoConOtroPuertoLoPoneEnLaIp() {
        activity.onQrScanned("sigebox://join?h=10.0.0.5&p=9000&c=482913");
        assertEquals("10.0.0.5:9000", host.getText().toString());
    }

    @Test
    public void elQrViejoSigueSirviendo() {
        activity.onQrScanned("sigebox://join?h=192.168.43.10&p=8765&s=F1&seat=2&t=tok123");
        assertEquals("192.168.43.10", host.getText().toString());
        assertEquals(activity.getString(R.string.join_seat, 2, "F1"),
                ((TextView) activity.findViewById(R.id.join_seat)).getText().toString());
        name.setText("Ana");
        assertTrue(submit.isEnabled());
    }

    @Test
    public void codigoDesconocidoLoDiceYCodigoBuenoPasaALaEspera() {
        name.setText("Ana");
        host.setText("192.168.43.10");
        code.setText("482913");
        submit.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse("mientras busca no se repite", submit.isEnabled());

        ConnectionTestAccess.dispatch(connection, new CodeUnknown("482913"));
        shadowOf(Looper.getMainLooper()).idle();
        TextView error = activity.findViewById(R.id.join_error);
        assertEquals(View.VISIBLE, error.getVisibility());
        assertEquals("Ese código no es válido. Pídele a la mesa el código de tu lugar.", error.getText().toString());
        assertTrue(submit.isEnabled());

        submit.performClick();
        ConnectionTestAccess.dispatch(connection, new CodeInfo("482913", "F1", 3, "Roja", "Azul"));
        shadowOf(Looper.getMainLooper()).idle();
        Intent next = shadowOf(activity).getNextStartedActivity();
        assertEquals(WaitingActivity.class.getName(), next.getComponent().getClassName());
        assertEquals(3, next.getIntExtra(WaitingActivity.EXTRA_SEAT, 0));
    }
}
