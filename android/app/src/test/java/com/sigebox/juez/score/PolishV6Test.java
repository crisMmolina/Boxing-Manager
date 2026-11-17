package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sigebox.juez.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-031 (pulido v6): notas P2 de design-critic sobre Puntuar. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-xxhdpi")
public class PolishV6Test {

    private Context themed;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
    }

    private View inflate() {
        FrameLayout parent = new FrameLayout(themed);
        View root = LayoutInflater.from(themed).inflate(R.layout.activity_score, parent, false);
        parent.addView(root);
        return root;
    }

    @Test
    public void laFilaOcultaElNombreSiEsIgualALaEtiqueta() {
        View root = inflate();
        View card = root.findViewById(R.id.score_red_card);
        CornerScore red = new CornerScore(card, true, d -> { });
        TextView name = card.findViewById(R.id.corner_name);

        red.setName("Roja");
        assertEquals("sin boxeador no repite la etiqueta", View.GONE, name.getVisibility());
        red.setName("Juan Pérez");
        assertEquals(View.VISIBLE, name.getVisibility());
        assertEquals("Juan Pérez", name.getText().toString());
        red.setName(null);
        assertEquals(View.GONE, name.getVisibility());
    }

    @Test
    public void elChipDeEstadoSobreArenaUsaLosTokensDeArena() {
        TextView status = inflate().findViewById(R.id.score_status);
        assertEquals(ContextCompat.getColor(themed, R.color.on_arena_2), status.getCurrentTextColor());
        assertEquals(ContextCompat.getColor(themed, R.color.arena_2),
                ((GradientDrawable) status.getBackground()).getColor().getDefaultColor());
    }
}
