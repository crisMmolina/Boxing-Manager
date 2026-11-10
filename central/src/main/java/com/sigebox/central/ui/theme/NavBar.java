package com.sigebox.central.ui.theme;

import java.util.List;

/** Navegación de la central: la barra superior ({@link StepsBar}) o la lateral v5 ({@link SideNav}). */
public interface NavBar {

    /** Marca el paso o enlace de la pantalla visible. */
    void select(String id);

    /** Dentro de un evento muestra sus pasos; fuera, solo los fijos y {@code keepLink}. */
    void showEvent(boolean inEvent, String keepLink);

    /** Id del paso o enlace activo, o {@code null}. */
    String currentId();

    /** Ids de los pasos en orden. */
    List<String> stepIds();
}
