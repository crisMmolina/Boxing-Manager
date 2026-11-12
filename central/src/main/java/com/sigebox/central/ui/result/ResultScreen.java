package com.sigebox.central.ui.result;

import com.sigebox.central.export.ExportService;
import com.sigebox.central.export.ResultSheet;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.central.ui.theme.Ui;
import java.io.IOException;
import java.nio.file.Files;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Resultado a pantalla completa, solo en la laptop del operador: los puntos de cada juez con sus
 * descuentos y su suma, y los botones de exportar (DESIGN v3 §7.4, sin logotipos). El ganador y la
 * decisión solo van en el PDF y el CSV. Esc la cierra.
 */
public final class ResultScreen {

    private ResultScreen() {
    }

    public static Stage show(Window owner, ResultSheet sheet, ExportService export) {
        var stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Resultado · " + sheet.pelea().esquinaRoja() + " vs " + sheet.pelea().esquinaAzul());
        var scene = new Scene(content(stage, sheet, export), 1280, 800);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setFullScreenExitHint("Esc para salir de pantalla completa");
        stage.setFullScreen(true);
        stage.show();
        return stage;
    }

    /** Lo que muestra la ventana {@code stage}, que no hace falta que esté abierta. */
    public static BorderPane content(Stage stage, ResultSheet sheet, ExportService export) {
        var status = Ui.support("");
        var pdf = Ui.primary("Exportar PDF");
        pdf.setOnAction(e -> save(stage, status, "pdf", export.pdf(sheet), sheet));
        var csv = Ui.secondary("Exportar CSV");
        csv.setOnAction(e -> save(stage, status, "csv", export.csvBytes(sheet), sheet));
        var xlsx = Ui.secondary("Exportar Excel");
        xlsx.setOnAction(e -> save(stage, status, "xlsx", export.xlsx(sheet), sheet));
        var close = Ui.secondary("Cerrar ventana"); // "Cerrar pelea" y "Cerrar round" son otra cosa
        close.setOnAction(e -> stage.close());
        var actions = new HBox(12, pdf, csv, xlsx, close, status);
        actions.setAlignment(Pos.CENTER_LEFT);

        var body = body(sheet);
        var root = new BorderPane(body);
        root.getStyleClass().add("app-root");
        // DESIGN v5 §5.12: el encabezado de la sección con la pelea como contexto; los totales entran contando.
        var header = new com.sigebox.central.ui.theme.PageHeader("Resultado")
                .fight(sheet.pelea().esquinaRoja(), sheet.pelea().esquinaAzul());
        root.setTop(new VBox(16, Ui.brand(false), header, Ui.next("Exporta el resultado o cierra esta ventana.")));
        CountUp.playAll(body);
        root.setBottom(actions);
        BorderPane.setMargin(body, new Insets(24, 0, 24, 0));
        root.setPadding(new Insets(40));
        return root;
    }

    /** La tabla de puntos y, al lado, la suma de cada juez en grande. */
    static HBox body(ResultSheet sheet) {
        var p = sheet.pelea();
        var main = Ui.card(Ui.support("Resultado final"),
                Ui.support(p.esquinaRoja() + " (roja) vs " + p.esquinaAzul() + " (azul) · " + p.rounds() + " rounds"),
                ResultTable.of(sheet));
        HBox.setHgrow(main, Priority.ALWAYS);

        var judges = new VBox(16);
        judges.setPrefWidth(440);
        judges.setMinWidth(440);
        for (var row : sheet.rows()) {
            var red = Ui.numeral(String.valueOf(row.totalRed()), "red");
            var blue = Ui.numeral(String.valueOf(row.totalBlue()), "blue");
            red.getStyleClass().add("judge-total");
            blue.getStyleClass().add("judge-total");
            var total = new HBox(8, red, Ui.numeral("–", "neutral"), blue);
            total.getChildren().forEach(n -> n.getStyleClass().add("big"));
            total.setAlignment(Pos.CENTER_LEFT);
            var card = Ui.card(Ui.cardTitle("Juez " + row.seat() + (row.name() == null ? "" : " · " + row.name())),
                    total);
            card.getStyleClass().add("compact");
            judges.getChildren().add(card);
        }
        return new HBox(24, main, judges);
    }

    private static void save(Stage stage, Label status, String ext, byte[] data, ResultSheet sheet) {
        var chooser = new FileChooser();
        chooser.setInitialFileName("resultado-" + sheet.pelea().id() + "." + ext);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(ext.toUpperCase(), "*." + ext));
        var file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            Files.write(file.toPath(), data);
            status.setText("Guardado: " + file.getName());
        } catch (IOException ex) {
            status.setText("No se pudo guardar: " + ex.getMessage());
        }
    }
}
