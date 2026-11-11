package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sigebox.central.ui.inicio.Nav;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cambiar claro/oscuro con la app abierta: el texto y el logo toman los colores del tema nuevo. */
class ThemeToggleTest {

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @Test
    void togglingRepaintsTextAndLogo() throws Exception {
        List<String> got = FxTest.onFx(() -> {
            Theme.followSystem();
            var nav = new SideNav(Nav.STEPS, Nav.LINKS, "rounds", id -> { });
            var body = new Label("Texto");
            var pageLogo = Logo.node(40, false);
            var root = new BorderPane(new VBox(body, pageLogo));
            root.getStyleClass().add("app-root");
            root.setLeft(nav);
            var scene = new Scene(root, 1280, 800);
            Theme.apply(scene);
            var stage = new Stage();
            stage.setScene(scene);
            stage.show();
            var out = new ArrayList<String>();
            for (int i = 0; i < 4; i++) {
                root.applyCss();
                var navLogo = (Group) nav.lookup(".logo");
                out.add(Theme.mode() + " body=" + body.getTextFill() + " nav=" + outline(navLogo) + " page=" + outline(pageLogo));
                Theme.toggle();
            }
            stage.close();
            return out;
        });
        for (var line : got) {
            boolean dark = line.startsWith("DARK");
            String ink = dark ? "0xf5f3eeff" : "0x111110ff";
            // la barra lateral es negra en los dos temas: su logo siempre marfil; el de la página sigue al tema
            String page = dark ? "0xe8e4dcff" : "0x000000ff";
            assertEquals(line.substring(0, line.indexOf(' ')) + " body=" + ink + " nav=0xe8e4dcff page=" + page, line);
        }
    }

    private static String outline(Node n) {
        if (n instanceof SVGPath p && Logo.OUTLINE.equals(p.getUserData())) {
            return String.valueOf(p.getFill());
        }
        if (n instanceof Parent parent) {
            for (var c : parent.getChildrenUnmodifiable()) {
                var r = outline(c);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }
}
