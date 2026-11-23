package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sigebox.juez.R;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/** AND-038 · «Mantén para enviar»: 0,8 s mantenido envía, soltar antes no (spec 009). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class HoldToSendTest {

    private Activity activity;
    private final AtomicInteger sent = new AtomicInteger();
    private BottomSheetDialog dialog;
    private HoldToSendButton button;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_Sigebox);
    }

    private void showSheet() {
        dialog = ConfirmSheet.show(activity, 3, 10, 9, sent::incrementAndGet);
        ShadowLooper.idleMainLooper();
        button = dialog.findViewById(R.id.confirm_send);
        assertNotNull(button);
    }

    private void touch(int action) {
        long now = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(now, now, action, 10f, 10f, 0);
        button.dispatchTouchEvent(e);
        e.recycle();
    }

    private static void advance(long ms) {
        ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS);
    }

    @Test
    public void sheetShowsCardSummaryAndHoldLabel() {
        showSheet();
        assertEquals("10", ((TextView) dialog.findViewById(R.id.confirm_red)).getText().toString());
        assertEquals("9", ((TextView) dialog.findViewById(R.id.confirm_blue)).getText().toString());
        assertEquals("Mantén para enviar", button.getText().toString());
    }

    @Test
    public void holding800msSendsButNot799() {
        showSheet();
        touch(MotionEvent.ACTION_DOWN);
        advance(799);
        assertEquals(0, sent.get());
        advance(1);
        assertEquals(1, sent.get());
        assertFalse(dialog.isShowing());
    }

    @Test
    public void releasingAt400msDoesNotSend() {
        showSheet();
        touch(MotionEvent.ACTION_DOWN);
        advance(400);
        touch(MotionEvent.ACTION_UP);
        advance(1000);
        assertEquals(0, sent.get());
        assertTrue(dialog.isShowing());
        assertEquals(0f, button.progress(), 0f);
    }

    @Test
    public void accessibilityActionSendRoundSendsWithoutHolding() {
        showSheet();
        AccessibilityNodeInfo node = button.createAccessibilityNodeInfo();
        boolean labelled = false;
        for (AccessibilityNodeInfo.AccessibilityAction a : node.getActionList()) {
            if (a.getId() == AccessibilityNodeInfo.ACTION_CLICK && a.getLabel() != null
                    && "Enviar round".contentEquals(a.getLabel())) {
                labelled = true;
            }
        }
        assertTrue("ACTION_CLICK etiquetada «Enviar round»", labelled);
        assertTrue(button.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null));
        ShadowLooper.idleMainLooper();
        assertEquals(1, sent.get());
    }

    @Test
    public void ringAnimatesWhenAnimationsOn() {
        Settings.Global.putFloat(activity.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        showSheet();
        touch(MotionEvent.ACTION_DOWN);
        advance(400);
        assertTrue("el anillo avanza a los 0,4 s", button.progress() > 0f);
    }

    @Test
    public void animationsOffRingStaysStillButTimingHolds() {
        Settings.Global.putFloat(activity.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 0f);
        showSheet();
        touch(MotionEvent.ACTION_DOWN);
        advance(400);
        assertEquals(0f, button.progress(), 0f);
        assertEquals(0, sent.get());
        advance(400);
        assertEquals(1, sent.get());
    }
}
