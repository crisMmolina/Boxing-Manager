package com.sigebox.central.ui.result;

import com.sigebox.central.export.ExportService;
import com.sigebox.central.export.ResultSheet;
import java.io.IOException;
import java.nio.file.Files;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Resultado a pantalla completa, solo en la laptop del operador: la frase del resultado, la
 * tarjeta y el total de cada juez con su nombre, un espacio reservado para el logotipo y los botones
 * de exportar. Esc la cierra.
 */
public final class ResultScreen {

    private ResultScreen() {
    }

    public static Stage show(Window owner, ResultSheet sheet, ExportService export) {
        var stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Resultado · " + sheet.pelea().esquinaRoja() + " vs " + sheet.pelea().esquinaAzul());

        var logo = new Region(); // espacio reservado para el logotipo del evento
        logo.setPrefSize(160, 80);
        logo.getStyleClass().add("logo-slot");

        var fight = new Label(sheet.pelea().esquinaRoja() + "  vs  " + sheet.pelea().esquinaAzul());
        fight.setStyle("-fx-font-size: 28px;");
        var headline = new Label(sheet.headline());
        headline.setStyle("-fx-font-size: 44px; -fx-font-weight: bold;");
        headline.setWrapText(true);

        var grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        int rounds = sheet.pelea().rounds();
        grid.add(bold("Juez"), 0, 0);
        for (int r = 1; r <= rounds; r++) {
            grid.add(bold("R" + r), r, 0);
        }
        grid.add(bold("Total"), rounds + 1, 0);
        grid.add(bold("Ganador"), rounds + 2, 0);
        int y = 1;
        for (var row : sheet.rows()) {
            grid.add(new Label("Juez " + row.seat() + (row.name() == null ? "" : " · " + row.name())), 0, y);
            for (var c : row.cells()) {
                grid.add(new Label(c.red() == null ? "—" : c.red() + "–" + c.blue()), c.round(), y);
            }
            grid.add(bold(row.totalRed() + "–" + row.totalBlue()), rounds + 1, y);
            grid.add(new Label(row.winner()), rounds + 2, y);
            y++;
        }
        grid.setStyle("-fx-font-size: 20px;");

        var status = new Label();
        var pdf = new Button("Exportar PDF");
        pdf.setOnAction(e -> save(stage, status, "pdf", export.pdf(sheet), sheet));
        var csv = new Button("Exportar CSV");
        csv.setOnAction(e -> save(stage, status, "csv", export.csvBytes(sheet), sheet));
        var close = new Button("Cerrar");
        close.setOnAction(e -> stage.close());

        var center = new VBox(24, fight, headline, grid);
        center.setAlignment(Pos.CENTER);
        var root = new BorderPane(center);
        root.setTop(new HBox(logo));
        root.setBottom(new HBox(12, pdf, csv, close, status));
        root.setPadding(new Insets(32));

        stage.setScene(new Scene(root, 1280, 800));
        stage.setFullScreenExitHint("Esc para salir de pantalla completa");
        stage.setFullScreen(true);
        stage.show();
        return stage;
    }

    private static Label bold(String text) {
        var l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        return l;
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
