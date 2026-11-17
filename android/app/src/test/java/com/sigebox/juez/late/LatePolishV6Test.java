package com.sigebox.juez.late;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.sigebox.juez.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-031 (pulido v6): Round tardío no repite el número del round (el título ya lo dice). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-xxhdpi")
public class LatePolishV6Test {

    @Test
    public void elAnilloNoRepiteElNumeroDelTitulo() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Context themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
        FrameLayout parent = new FrameLayout(themed);
        View root = LayoutInflater.from(themed).inflate(R.layout.activity_late_score, parent, false);
        View number = root.findViewById(R.id.late_ring_number);
        assertTrue("el número del anillo repite el título", number == null || number.getVisibility() == View.GONE);
    }
}
