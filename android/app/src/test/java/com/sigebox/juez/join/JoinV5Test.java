package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.sigebox.juez.R;
import com.sigebox.juez.ui.ConnectionChip;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Spec 006 · AND-024: Unirse y Espera de aprobación con los componentes v5 (DESIGN v5 §5.17, §6). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JoinV5Test {

    private View inflate(int layout) {
        var ctx = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Sigebox);
        return LayoutInflater.from(ctx).inflate(layout, null);
    }

    @Test
    public void laEsperaDeAprobacionTieneChipDeConexionYEstadoIlustrado() {
        View root = inflate(R.layout.activity_waiting);

        ConnectionChip chip = root.findViewById(R.id.top_pill);
        assertNotNull("chip de conexión en la barra", chip);
        assertNotNull(root.findViewById(R.id.waiting_badge));
        ImageView icon = root.findViewById(R.id.waiting_badge_icon);
        assertEquals("clock", icon.getTag());
    }

    @Test
    public void escanearLlevaIconoYLaIpTambien() {
        View root = inflate(R.layout.activity_join);

        MaterialButton scan = root.findViewById(R.id.join_scan);
        assertNotNull("ícono de QR en Escanear", scan.getIcon());
        TextInputLayout host = root.findViewById(R.id.join_host_layout);
        assertNotNull("ícono en el campo de IP", host.getStartIconDrawable());
    }

    @Test
    public void elNombreNoMuestraTextoDeEjemplo() {
        View root = inflate(R.layout.activity_join);

        TextInputLayout name = (TextInputLayout) root.findViewById(R.id.join_name).getParent().getParent();
        assertNull("AND-025: el Nombre va sin placeholder", name.getPlaceholderText());
        TextInputLayout code = (TextInputLayout) root.findViewById(R.id.join_code).getParent().getParent();
        assertNull("el Código va sin ejemplo", code.getPlaceholderText());
    }
}
