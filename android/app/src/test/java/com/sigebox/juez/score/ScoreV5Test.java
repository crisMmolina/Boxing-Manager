package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * AND-023 (DESIGN.md v5 §5.16 y §6): segmentos de rounds, franja de la esquina, número que rueda con háptica,
 * "Round N enviado" tras el ACK y movimiento reducido con la escala de animación del sistema en 0.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ScoreV5Test {

    private final List<Message> submitted = new ArrayList<>();
    private Context themed;
    private ScoreViewModel vm;

    @Before
    public void setUp() {
        scale(1f);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
        vm = new ScoreViewModel(submitted::add);
        vm.setSession("F1", 2, "María López");
        vm.onMessage(new State("F1", 2, "María López", "Roja", "Azul", 4,
                EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, Collections.emptyList(), false));
    }

    @After
    public void tearDown() {
        scale(1f);
    }

    private static void scale(float value) {
        Context app = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        Settings.Global.putFloat(app.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, value);
    }

    private View inflate(int layout) {
        FrameLayout parent = new FrameLayout(themed);
        View view = LayoutInflater.from(themed).inflate(layout, parent, false);
        parent.addView(view);
        return view;
    }

    private int color(int id) {
        return ContextCompat.getColor(themed, id);
    }

    private ScoreUi ui() {
        return vm.ui().getValue();
    }

    /** Puntúa las dos esquinas y envía; devuelve el SUBMIT_ROUND que salió. */
    private SubmitRound scoreAndSend() {
        vm.adjust(ScoreUi.Corner.RED, 1);   // 10
        vm.adjust(ScoreUi.Corner.BLUE, -1); // 9
        vm.submit();
        return (SubmitRound) submitted.get(submitted.size() - 1);
    }

    private static int segmentColor(LinearLayout row, int index) {
        GradientDrawable segment = (GradientDrawable) row.getChildAt(index).getBackground();
        return segment.getColor().getDefaultColor();
    }

    private static List<String> texts(View view) {
        List<String> out = new ArrayList<>();
        if (view.getVisibility() != View.VISIBLE) {
            return out;
        }
        if (view instanceof TextView) {
            out.add(((TextView) view).getText().toString());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                out.addAll(texts(group.getChildAt(i)));
            }
        }
        return out;
    }

    // --- Segmentos de rounds (§5.16) ---

    @Test
    public void unSegmentoPorRoundCerradoVerdeActualDoradoFuturoPista() {
        LinearLayout row = inflate(R.layout.activity_score).findViewById(R.id.score_progress);
        RoundSegments.bind(row, 6, 3, false);
        assertEquals(6, row.getChildCount());
        assertEquals(color(R.color.connected), segmentColor(row, 0));
        assertEquals(color(R.color.connected), segmentColor(row, 1));
        assertEquals(color(R.color.accent), segmentColor(row, 2));
        for (int i = 3; i < 6; i++) {
            assertEquals(color(R.color.ring_track), segmentColor(row, i));
        }
        float density = themed.getResources().getDisplayMetrics().density;
        int height = row.getChildAt(0).getLayoutParams().height;
        assertTrue("6–8 dp de alto", height >= Math.round(6 * density) && height <= Math.round(8 * density));
        assertEquals("decorativos", View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                row.getImportantForAccessibility());
    }

    @Test
    public void conElRoundEnviadoElActualYaCuentaComoCerrado() {
        LinearLayout row = inflate(R.layout.activity_score).findViewById(R.id.score_progress);
        RoundSegments.bind(row, 4, 3, true);
        assertEquals(color(R.color.connected), segmentColor(row, 2));
        assertEquals(color(R.color.ring_track), segmentColor(row, 3));
    }

    @Test
    public void elRoundTardioTambienTieneSegmentos() {
        LinearLayout row = inflate(R.layout.activity_late_score).findViewById(R.id.late_progress);
        assertNotNull(row);
        RoundSegments.bind(row, 4, 2, false);
        assertEquals(4, row.getChildCount());
        assertEquals(color(R.color.accent), segmentColor(row, 1));
    }

    // --- Tarjeta de esquina: franja, rodar y háptica ---

    @Test
    public void cadaEsquinaLlevaUnaFranjaIzquierdaDe6dpDeSuColor() {
        View redRoot = inflate(R.layout.view_corner_card);
        View blueRoot = inflate(R.layout.view_corner_card);
        new CornerScore(redRoot, true, d -> { });
        new CornerScore(blueRoot, false, d -> { });
        float density = themed.getResources().getDisplayMetrics().density;
        for (View root : new View[] {redRoot, blueRoot}) {
            View stripe = root.findViewById(R.id.corner_stripe);
            assertNotNull(stripe);
            assertEquals(Math.round(6 * density), stripe.getLayoutParams().width);
        }
        assertEquals(color(R.color.red),
                ((ColorDrawable) redRoot.findViewById(R.id.corner_stripe).getBackground()).getColor());
        assertEquals(color(R.color.blue),
                ((ColorDrawable) blueRoot.findViewById(R.id.corner_stripe).getBackground()).getColor());
    }

    @Test
    public void menosYMasDanHapticaCorta() {
        View root = inflate(R.layout.view_corner_card);
        CornerScore card = new CornerScore(root, true, d -> { });
        card.bind(9, true);
        View plus = root.findViewById(R.id.corner_plus);
        View minus = root.findViewById(R.id.corner_minus);
        plus.performClick();
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, shadowOf(plus).lastHapticFeedbackPerformed());
        minus.performClick();
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, shadowOf(minus).lastHapticFeedbackPerformed());
    }

    @Test
    public void elNumeroRuedaAlNuevoValorEn200msSubiendoOBajando() {
        View root = inflate(R.layout.view_corner_card);
        TextView value = root.findViewById(R.id.corner_value);
        CornerScore card = new CornerScore(root, true, d -> { });
        card.bind(null, true);
        assertNull("abrir el round no rueda", card.lastRoll());

        root.findViewById(R.id.corner_plus).performClick();
        card.bind(10, true);
        Animator up = card.lastRoll();
        assertNotNull("rueda al tocar +", up);
        assertEquals(200, up.getDuration());
        assertEquals("10", value.getText().toString());
        assertTrue("con + entra desde abajo", value.getTranslationY() > 0f);

        root.findViewById(R.id.corner_minus).performClick();
        card.bind(9, true);
        Animator down = card.lastRoll();
        assertNotNull("rueda al tocar −", down);
        assertEquals("9", value.getText().toString());
        assertTrue("con − entra desde arriba", value.getTranslationY() < 0f);
    }

    @Test
    public void unRedibujadoSinToqueNoRueda() {
        View root = inflate(R.layout.view_corner_card);
        CornerScore card = new CornerScore(root, false, d -> { });
        card.bind(null, true);
        card.bind(8, false); // restaurar de la bandeja o cambio de conexión: no es un toque
        assertNull(card.lastRoll());
        assertEquals(0f, ((TextView) root.findViewById(R.id.corner_value)).getTranslationY(), 0f);
    }

    // --- Round N enviado (tras el ACK) ---

    @Test
    public void conElAckSeVeRoundEnviadoConSuTarjetaYCorregir() {
        View root = inflate(R.layout.activity_score);
        SentPanel panel = new SentPanel(root, vm::startCorrection);
        SubmitRound sent = scoreAndSend();

        assertFalse("enviando todavía no", panel.bind(ui(), 4));
        assertEquals(View.GONE, root.findViewById(R.id.score_sent_panel).getVisibility());

        vm.onMessage(new Ack(sent.id(), true, EstadoTarjeta.ENVIADA, null));
        assertTrue(panel.bind(ui(), 4));
        assertEquals(View.VISIBLE, root.findViewById(R.id.score_sent_panel).getVisibility());
        assertEquals("Round 3 enviado",
                ((TextView) root.findViewById(R.id.score_sent_title)).getText().toString());
        assertEquals("La central lo recibió. Espera a que abra el round 4.",
                ((TextView) root.findViewById(R.id.score_sent_support)).getText().toString());
        assertEquals("10", ((TextView) root.findViewById(R.id.score_sent_red)).getText().toString());
        assertEquals("9", ((TextView) root.findViewById(R.id.score_sent_blue)).getText().toString());
        assertEquals(View.GONE, root.findViewById(R.id.score_red_card).getVisibility());
        assertEquals(View.GONE, root.findViewById(R.id.score_blue_card).getVisibility());
        assertEquals(View.GONE, root.findViewById(R.id.score_send).getVisibility());

        TextView correct = root.findViewById(R.id.score_correct);
        assertEquals(View.VISIBLE, correct.getVisibility());
        assertEquals("Corregir esta tarjeta", correct.getText().toString());
    }

    @Test
    public void laPalomitaSeDibujaEn380msUnaSolaVez() {
        View root = inflate(R.layout.activity_score);
        SentPanel panel = new SentPanel(root, vm::startCorrection);
        SubmitRound sent = scoreAndSend();
        vm.onMessage(new Ack(sent.id(), true, EstadoTarjeta.ENVIADA, null));
        panel.bind(ui(), 4);
        SentCheck check = root.findViewById(R.id.score_sent_check);
        assertTrue("se está dibujando", check.isAnimating());
        assertEquals(380, check.animator().getDuration());
        assertTrue("empieza sin trazo completo", check.progress() < 1f);

        Animator first = check.animator();
        panel.bind(ui(), 4); // otro render (conexión, etc.) no la vuelve a dibujar
        assertSame(first, check.animator());
    }

    @Test
    public void corregirEstaTarjetaSigueElFlujoDeCorreccion() {
        View root = inflate(R.layout.activity_score);
        SentPanel panel = new SentPanel(root, vm::startCorrection);
        SubmitRound sent = scoreAndSend();
        vm.onMessage(new Ack(sent.id(), true, EstadoTarjeta.ENVIADA, null));
        panel.bind(ui(), 4);

        root.findViewById(R.id.score_correct).performClick();
        assertTrue("corrigiendo", ui().editing);
        assertEquals(Integer.valueOf(10), ui().red);
        assertFalse(panel.bind(ui(), 4));
        assertEquals(View.GONE, root.findViewById(R.id.score_sent_panel).getVisibility());
        assertEquals(View.VISIBLE, root.findViewById(R.id.score_red_card).getVisibility());
        assertEquals(View.VISIBLE, root.findViewById(R.id.score_send).getVisibility());
    }

    @Test
    public void unAckRechazadoNoMuestraEnviado() {
        View root = inflate(R.layout.activity_score);
        SentPanel panel = new SentPanel(root, vm::startCorrection);
        SubmitRound sent = scoreAndSend();
        vm.onMessage(new Ack(sent.id(), false, null, Ack.Error.ROUND_NOT_OPEN));
        assertFalse(panel.bind(ui(), 4));
        assertEquals(View.GONE, root.findViewById(R.id.score_sent_panel).getVisibility());
    }

    @Test
    public void enElUltimoRoundNoPrometeOtroRound() {
        vm.onMessage(new State("F1", 2, "María López", "Roja", "Azul", 3,
                EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, Collections.emptyList(), false));
        View root = inflate(R.layout.activity_score);
        SentPanel panel = new SentPanel(root, vm::startCorrection);
        SubmitRound sent = scoreAndSend();
        vm.onMessage(new Ack(sent.id(), true, EstadoTarjeta.ENVIADA, null));
        assertTrue(panel.bind(ui(), 3));
        String support = ((TextView) root.findViewById(R.id.score_sent_support)).getText().toString();
        assertFalse(support, support.contains("round 4"));
        assertTrue(support, support.startsWith("La central lo recibió."));
    }

    @Test
    public void principio3EnviadoSoloMuestraLaTarjetaPropiaSinTotal() {
        View root = inflate(R.layout.activity_score);
        SentPanel panel = new SentPanel(root, vm::startCorrection);
        SubmitRound sent = scoreAndSend();
        vm.onMessage(new Ack(sent.id(), true, EstadoTarjeta.ENVIADA, null));
        panel.bind(ui(), 4);
        List<String> shown = texts(root.findViewById(R.id.score_sent_panel));
        for (String text : shown) {
            assertFalse("sin total: " + text, text.contains("19"));
            assertFalse("sin total: " + text, text.toLowerCase(java.util.Locale.ROOT).contains("total"));
            assertFalse("sin ganador: " + text, text.toLowerCase(java.util.Locale.ROOT).contains("gana"));
        }
    }

    // --- Movimiento reducido ---

    @Test
    public void sinAnimacionDelSistemaElNumeroCambiaSinMoverse() {
        scale(0f);
        View root = inflate(R.layout.view_corner_card);
        TextView value = root.findViewById(R.id.corner_value);
        CornerScore card = new CornerScore(root, true, d -> { });
        card.bind(null, true);
        root.findViewById(R.id.corner_plus).performClick();
        card.bind(10, true);
        assertNull("sin rodar", card.lastRoll());
        assertEquals("10", value.getText().toString());
        assertEquals(0f, value.getTranslationY(), 0f);
    }

    @Test
    public void sinAnimacionDelSistemaLaPalomitaApareceCompleta() {
        scale(0f);
        View root = inflate(R.layout.activity_score);
        SentPanel panel = new SentPanel(root, vm::startCorrection);
        SubmitRound sent = scoreAndSend();
        vm.onMessage(new Ack(sent.id(), true, EstadoTarjeta.ENVIADA, null));
        assertTrue(panel.bind(ui(), 4));
        SentCheck check = root.findViewById(R.id.score_sent_check);
        assertFalse("sin dibujar", check.isAnimating());
        assertEquals(1f, check.progress(), 0f);
        assertEquals(1f, check.getScaleX(), 0f);
        assertEquals(1f, check.getScaleY(), 0f);
    }
}
