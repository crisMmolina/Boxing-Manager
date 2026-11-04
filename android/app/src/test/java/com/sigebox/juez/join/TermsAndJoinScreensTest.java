package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.JoinRejected;
import com.sigebox.juez.R;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.terms.TermsActivity;
import com.sigebox.juez.terms.TermsStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class TermsAndJoinScreensTest {

    @Test
    public void sinAceptarTerminosNoSeContinua() {
        TermsActivity activity = Robolectric.buildActivity(TermsActivity.class).setup().get();
        Button accept = activity.findViewById(R.id.terms_accept);
        CheckBox check = activity.findViewById(R.id.terms_check);
        assertFalse(accept.isEnabled());

        check.setChecked(true);
        assertTrue(accept.isEnabled());
        accept.performClick();

        assertTrue(new TermsStore(activity).isAccepted());
        Intent next = shadowOf(activity).getNextStartedActivity();
        assertEquals(JoinActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void unirmeDeshabilitadoHastaTenerLugarYNombre() {
        new TermsStore(ApplicationProvider.getApplicationContext()).accept();
        JoinActivity activity = Robolectric.buildActivity(JoinActivity.class).setup().get();
        Button join = activity.findViewById(R.id.join_submit);
        assertFalse(join.isEnabled());

        activity.onQrScanned("sigebox://join?h=192.168.43.10&p=8765&s=F1&seat=2&t=tok123");
        assertFalse("falta el nombre", join.isEnabled());
        ((android.widget.EditText) activity.findViewById(R.id.join_name)).setText("María López");
        assertTrue(join.isEnabled());
    }

    @Test
    public void textoDeEsperaSegunElEstado() {
        assertEquals(R.string.status_waiting_approval, WaitingText.forState(
                new ConnectionState(ConnectionState.Status.WAITING_APPROVAL, null, null, 0, null)));
        assertEquals(R.string.status_disconnected, WaitingText.forState(
                new ConnectionState(ConnectionState.Status.DISCONNECTED, null, null, 0, null)));
        assertEquals(R.string.join_rejected_token_used, WaitingText.forState(
                new ConnectionState(ConnectionState.Status.REJECTED, null, null, 0, JoinRejected.Reason.TOKEN_USED)));
        assertEquals(R.string.status_connected, WaitingText.forState(
                new ConnectionState(ConnectionState.Status.CONNECTED, 1, EstadoRound.PENDIENTE, 0, null)));
    }
}
