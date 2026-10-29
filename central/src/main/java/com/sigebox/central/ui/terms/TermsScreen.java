package com.sigebox.central.ui.terms;

import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Términos de uso, aviso de privacidad y prohibición de apuestas. Sin aceptar no se continúa. */
public final class TermsScreen {

    public static final String ID = "terminos";

    /** Texto vigente ({@link TermsGate#VERSION}). */
    public static final String TEXT = """
            Términos de uso

            SIGEBOX registra las calificaciones de los jueces de una pelea y calcula el resultado. La \
            decisión oficial es responsabilidad del organizador del evento; ante cualquier diferencia con \
            las tarjetas de papel, vale el papel.

            Aviso de privacidad

            Se guardan el nombre de cada juez, la huella de su teléfono, sus calificaciones con la hora y \
            una bitácora de lo que pasa en la central. Los datos se quedan en esta computadora y en la red \
            local del evento: no se envían a internet. El organizador puede borrar la carpeta de datos \
            cuando ya no la necesite.

            Prohibición de apuestas

            Está prohibido usar SIGEBOX, sus resultados parciales o sus datos para apuestas de cualquier \
            tipo. Los totales no se muestran durante la pelea.
            """;

    private TermsScreen() {
    }

    /** Registra la pantalla; {@code onAccepted} lleva a la pantalla siguiente. */
    public static void register(Screens<Node> screens, TermsGate gate, Runnable onAccepted) {
        screens.register(ID, "Términos", () -> build(gate, onAccepted, Platform::exit));
    }

    static Node build(TermsGate gate, Runnable onAccepted, Runnable onDeclined) {
        var text = Ui.body(TEXT);
        text.setWrapText(true);
        text.setMaxWidth(720);
        var scroll = new ScrollPane(text);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);

        var agree = new CheckBox("Leí y acepto los términos, el aviso de privacidad y la prohibición de apuestas");
        var accept = Ui.primary("Aceptar y continuar");
        accept.setDefaultButton(true);
        accept.disableProperty().bind(agree.selectedProperty().not());
        accept.setOnAction(e -> {
            gate.accept();
            onAccepted.run();
        });
        var decline = Ui.secondary("No acepto (salir)");
        decline.setOnAction(e -> {
            gate.decline();
            onDeclined.run();
        });

        var card = Ui.card(scroll, agree, new HBox(12, accept, decline));
        card.setMaxWidth(800);
        var root = new VBox(16, Ui.screenTitle("Antes de empezar"), card);
        return root;
    }
}
