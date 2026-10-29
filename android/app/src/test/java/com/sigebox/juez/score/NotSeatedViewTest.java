package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-013: sin lugar en la pelea nueva, la puntuación se cubre con "No participas en esta pelea". */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class NotSeatedViewTest {

    private static View cover(Activity activity) {
        return ((ViewGroup) activity.findViewById(android.R.id.content)).findViewWithTag(NotSeatedView.TAG);
    }

    @Test
    public void muestraElTextoYSeOcultaAlVolverARecibirEstado() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        assertNull(cover(activity));

        NotSeatedView.show(activity);
        View cover = cover(activity);
        assertNotNull(cover);
        assertEquals(View.VISIBLE, cover.getVisibility());
        TextView title = (TextView) ((ViewGroup) ((ViewGroup) cover).getChildAt(0)).getChildAt(0);
        assertEquals("No participas en esta pelea", title.getText().toString());

        NotSeatedView.show(activity); // idempotente: no apila otra cubierta
        NotSeatedView.hide(activity);
        assertEquals(View.GONE, cover(activity).getVisibility());
    }

    @Test
    public void losTextosVienenDeRecursos() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NotSeatedView.show(activity);
        ViewGroup box = (ViewGroup) ((ViewGroup) cover(activity)).getChildAt(0);
        assertEquals(activity.getString(com.sigebox.juez.R.string.not_seated_title),
                ((TextView) box.getChildAt(0)).getText().toString());
        assertEquals(activity.getString(com.sigebox.juez.R.string.not_seated_support),
                ((TextView) box.getChildAt(1)).getText().toString());
    }
}
