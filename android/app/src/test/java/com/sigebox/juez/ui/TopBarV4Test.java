package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sigebox.juez.R;
import com.sigebox.juez.terms.TermsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v4: barra negra de borde a borde, también bajo la barra de estado, con logo y marca dorada. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class TopBarV4Test {

    private static TermsActivity terms() {
        return Robolectric.buildActivity(TermsActivity.class).setup().get();
    }

    @Test
    public void laBarraLlevaLogoYMarcaDorada() {
        TermsActivity activity = terms();
        ViewGroup bar = activity.findViewById(R.id.top_bar);
        assertTrue("logo", bar.getChildAt(0) instanceof ImageView);
        assertEquals(ContextCompat.getColor(activity, R.color.bar_accent),
                ((TextView) bar.getChildAt(1)).getCurrentTextColor());
    }

    @Test
    public void laBarraCruzaElMargenDeLaPantalla() {
        TermsActivity activity = terms();
        View bar = activity.findViewById(R.id.top_bar);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
        int margin = activity.getResources().getDimensionPixelSize(R.dimen.screen_margin);
        assertEquals(-margin, params.getMarginStart());
        assertEquals(-margin, params.getMarginEnd());
        assertEquals(-margin, params.topMargin);
        assertFalse("no se recorta en el margen", ((ViewGroup) bar.getParent()).getClipToPadding());
    }

    @Test
    public void elNegroSigueBajoLaBarraDeEstado() {
        TermsActivity activity = terms();
        ViewGroup content = activity.findViewById(android.R.id.content);
        WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 48, 0, 32))
                .build();
        ViewCompat.dispatchApplyWindowInsets(content.getChildAt(0), insets);

        View strip = activity.findViewById(R.id.status_strip);
        assertNotNull(strip);
        assertEquals(48, strip.getLayoutParams().height);
        assertFalse("íconos claros sobre negro", WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView()).isAppearanceLightStatusBars());
    }

    @Test
    public void laAyudaSeVeSobreLaBarra() {
        TermsActivity activity = terms();
        View bar = LayoutInflater.from(activity).inflate(R.layout.view_top_bar, null);
        TopBar.bind(bar, true, "Juez 2");
        TextView help = bar.findViewById(R.id.top_help);
        assertEquals(ContextCompat.getColor(activity, R.color.on_bar), help.getCurrentTextColor());
        TopBar.bind(bar, false, "Sin conexión");
        assertEquals(ContextCompat.getColor(activity, R.color.bar), help.getCurrentTextColor());
    }
}
