package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

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

/** AND-037: el código se ve «123 456», se pega con guion o espacio y el sexto dígito busca solo. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JoinCodeGroupTest {

    private JoinActivity activity;
    private EditText name;
    private EditText host;
    private EditText code;
    private TextView error;
    private Button submit;
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
        host = activity.findViewById(R.id.join_host);
        code = activity.findViewById(R.id.join_code);
        error = activity.findViewById(R.id.join_error);
        submit = activity.findViewById(R.id.join_submit);
    }

    @After
    public void tearDown() {
        JoinActivity.connections = context -> com.sigebox.juez.net.JudgeRuntime.get(context).connection();
        connection.stop();
        db.close();
    }

    @Test
    public void elCampoMuestraTresYTresYEntregaLosSeisDigitos() {
        name.setText("Ana");
        host.setText("192.168.43.10");
        code.setText("123456");

        assertEquals("123 456", JoinActivity.visibleCode(code.getText()));
        assertEquals("123456", code.getText().toString());
        assertEquals("123456", activity.current().request.uri.code());
    }

    @Test
    public void conMenosDeCuatroDigitosNoHayHueco() {
        code.setText("123");
        assertEquals("123", JoinActivity.visibleCode(code.getText()));
    }

    @Test
    public void pegarConGuionOEspacioDejaLosSeisDigitos() {
        code.setText("123-456");
        assertEquals("123456", code.getText().toString());
        code.setText("123 456");
        assertEquals("123456", code.getText().toString());
    }

    @Test
    public void conNombreElSextoDigitoBuscaSinTocarUnirme() {
        name.setText("Ana");
        host.setText("192.168.43.10");
        code.setText("48291");
        code.append("3");
        idle();

        assertEquals(View.VISIBLE, error.getVisibility());
        assertEquals(activity.getString(R.string.join_looking), error.getText().toString());
        assertFalse(submit.isEnabled());
    }

    @Test
    public void sinNombreElSextoDigitoPideElNombre() {
        host.setText("192.168.43.10");
        code.setText("482913");
        idle();

        assertEquals(View.VISIBLE, error.getVisibility());
        assertEquals(activity.getString(R.string.join_error_name_empty), error.getText().toString());
        assertTrue(name.hasFocus());
        assertFalse(submit.isEnabled());
    }

    @Test
    public void escribirElNombreQuitaElAvisoYPermiteUnirse() {
        host.setText("192.168.43.10");
        code.setText("482913");
        idle();
        name.setText("Ana");
        idle();

        assertEquals(View.GONE, error.getVisibility());
        assertTrue(submit.isEnabled());
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }
}
