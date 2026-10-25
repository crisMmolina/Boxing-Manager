package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ApplicationProvider;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.R;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.score.ScoreUi;
import com.sigebox.juez.score.ScoreViewModel;
import com.sigebox.juez.terms.TermsActivity;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v2 en la app del juez: bordes del sistema, − y +, 10–10 inicial y tokens de color. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ScreenDesignTest {

    private final List<Message> submitted = new ArrayList<>();

    private ScoreViewModel openRound() {
        ScoreViewModel vm = new ScoreViewModel(submitted::add);
        vm.setSession("F1", 2, "María López");
        vm.onConnection(new ConnectionState(ConnectionState.Status.CONNECTED, 4, EstadoRound.ABIERTO, 0, null));
        return vm;
    }

    @Test
    public void screensApplySystemBarInsetsToTheirRoot() {
        TermsActivity activity = Robolectric.buildActivity(TermsActivity.class).setup().get();
        ViewGroup content = activity.findViewById(android.R.id.content);
        View root = content.getChildAt(0);
        assertEquals(Boolean.TRUE, root.getTag(R.id.screen_insets_applied));
        int before = root.getPaddingTop();

        WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 48, 0, 32))
                .build();
        ViewCompat.dispatchApplyWindowInsets(root, insets);

        assertEquals("nada bajo la barra de estado", before + 48, root.getPaddingTop());
        assertTrue("nada bajo la barra de navegación", root.getPaddingBottom() >= 32);
    }

    @Test
    public void roundStartsAtTenTenAndPlusMinusStayInRange() {
        ScoreViewModel vm = openRound();
        assertNull(vm.ui().getValue().red);
        assertTrue(vm.applyDefault());
        assertEquals(Integer.valueOf(10), vm.ui().getValue().red);
        assertEquals(Integer.valueOf(10), vm.ui().getValue().blue);
        assertFalse("solo una vez", vm.applyDefault());

        vm.adjust(ScoreUi.Corner.RED, 1);
        assertEquals("el máximo es 10", Integer.valueOf(10), vm.ui().getValue().red);
        vm.adjust(ScoreUi.Corner.BLUE, -1);
        assertEquals(Integer.valueOf(9), vm.ui().getValue().blue);
        for (int i = 0; i < 12; i++) {
            vm.adjust(ScoreUi.Corner.BLUE, -1);
        }
        assertEquals("el mínimo es 0", Integer.valueOf(0), vm.ui().getValue().blue);
        assertTrue(vm.ui().getValue().sendEnabled);
    }

    @Test
    public void sentValuesAreWhatThePlusMinusShowed() {
        ScoreViewModel vm = openRound();
        vm.applyDefault();
        vm.adjust(ScoreUi.Corner.BLUE, -1);
        vm.submit();
        SubmitRound sent = (SubmitRound) submitted.get(0);
        assertEquals(Integer.valueOf(10), sent.red());
        assertEquals(Integer.valueOf(9), sent.blue());
        vm.adjust(ScoreUi.Corner.RED, -1); // ya enviado: no cambia hasta "Corregir"
        assertEquals(Integer.valueOf(10), vm.ui().getValue().card.red);
    }

    @Test
    public void nothingChangesWhileTheRoundIsClosed() {
        ScoreViewModel vm = new ScoreViewModel(submitted::add);
        vm.setSession("F1", 2, "María López");
        vm.onConnection(new ConnectionState(ConnectionState.Status.CONNECTED, 4, EstadoRound.CERRADO, 0, null));
        assertFalse(vm.applyDefault());
        vm.adjust(ScoreUi.Corner.RED, -1);
        assertNull(vm.ui().getValue().red);
    }

    @Test
    public void greenIsOnlyTheConnectionDotAndCornersKeepTheirColors() {
        var ctx = ApplicationProvider.getApplicationContext();
        assertEquals(0xFF1E8E5A, ContextCompat.getColor(ctx, R.color.connected));
        assertEquals(0xFFC62839, ContextCompat.getColor(ctx, R.color.red_corner));
        assertEquals(0xFF1F4E9C, ContextCompat.getColor(ctx, R.color.blue_corner));
        assertEquals(0xFF14284B, ContextCompat.getColor(ctx, R.color.navy));
        // Los estados de la v1 ya no tienen color propio.
        assertEquals(ContextCompat.getColor(ctx, R.color.ink_secondary),
                ContextCompat.getColor(ctx, R.color.status_sent_fg));
    }
}
