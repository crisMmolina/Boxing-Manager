package com.sigebox.central.ui.result;

import com.sigebox.central.export.ExportService;
import com.sigebox.central.export.ResultSheet;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.Ganador;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Resultado a pantalla completa, solo en la laptop del operador: la frase del resultado, la
 * tarjeta y el total de cada juez con su nombre y los botones de exportar (DESIGN v3 §7.4, sin
 * logotipos). Esc la cierra.
 */
public final class ResultScreen {

    private ResultScreen() {
    }

    public static Stage show(Window owner, ResultSheet sheet, ExportService export) {
        var stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Resultado · " + sheet.pelea().esquinaRoja() + " vs " + sheet.pelea().esquinaAzul());

        var p = sheet.pelea();
        var ganador = sheet.resultado().ganador();
        var winner = new Label(switch (ganador) {
            case ROJA -> p.esquinaRoja();
            case AZUL -> p.esquinaAzul();
            case EMPATE -> "Empate";
            case SIN_DECISION -> "Sin decisión";
        });
        winner.getStyleClass().addAll("result-winner", "numeral",
                ganador == Ganador.ROJA ? "red" : ganador == Ganador.AZUL ? "blue" : "neutral");
        var winnerRow = new HBox(16, winner);
        winnerRow.setAlignment(Pos.CENTER_LEFT);
        if (ganador == Ganador.ROJA || ganador == Ganador.AZUL) {
            winnerRow.getChildren().add(Ui.cornerPill(ganador == Ganador.ROJA));
        }
        var main = Ui.card(Ui.support("Resultado final"), winnerRow, Ui.cardTitle(sheet.headline()),
                Ui.support(p.esquinaRoja() + " (roja) vs " + p.esquinaAzul() + " (azul) · " + p.rounds() + " rounds"),
                rounds(sheet));
        HBox.setHgrow(main, Priority.ALWAYS);

        // Una tarjeta por juez con su total en grande y una píldora neutra.
        var judges = new VBox(16);
        judges.setPrefWidth(440);
        judges.setMinWidth(440);
        for (var row : sheet.rows()) {
            var total = new HBox(8, Ui.numeral(String.valueOf(row.totalRed()), "red"), Ui.numeral("–", "neutral"),
                    Ui.numeral(String.valueOf(row.totalBlue()), "blue"));
            total.getChildren().forEach(n -> n.getStyleClass().add("big"));
            total.setAlignment(Pos.CENTER_LEFT);
            var top = new HBox(Ui.cardTitle("Juez " + row.seat() + (row.name() == null ? "" : " · " + row.name())),
                    Ui.grow(), Ui.pill(row.winner()));
            top.setAlignment(Pos.CENTER_LEFT);
            var card = Ui.card(top, total);
            card.getStyleClass().add("compact");
            judges.getChildren().add(card);
        }

        var status = Ui.support("");
        var pdf = Ui.primary("Exportar PDF");
        pdf.setOnAction(e -> save(stage, status, "pdf", export.pdf(sheet), sheet));
        var csv = Ui.secondary("Exportar CSV");
        csv.setOnAction(e -> save(stage, status, "csv", export.csvBytes(sheet), sheet));
        var close = Ui.secondary("Cerrar");
        close.setOnAction(e -> stage.close());
        var actions = new HBox(12, pdf, csv, close, status);
        actions.setAlignment(Pos.CENTER_LEFT);

        var body = new HBox(24, main, judges);
        var root = new BorderPane(body);
        root.getStyleClass().add("app-root");
        root.setTop(new HBox(16, Ui.brand()));
        root.setBottom(actions);
        BorderPane.setMargin(body, new Insets(24, 0, 24, 0));
        root.setPadding(new Insets(40));

        var scene = new Scene(root, 1280, 800);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setFullScreenExitHint("Esc para salir de pantalla completa");
        stage.setFullScreen(true);
        stage.show();
        return stage;
    }

    /** Tarjeta de cada juez por round: "rojo – azul" con cada número en su color. */
    private static GridPane rounds(ResultSheet sheet) {
        var grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);
        int rounds = sheet.pelea().rounds();
        grid.add(Ui.support("Juez"), 0, 0);
        for (int r = 1; r <= rounds; r++) {
            grid.add(Ui.support("R" + r), r, 0);
        }
        int y = 1;
        for (var row : sheet.rows()) {
            grid.add(Ui.body("Juez " + row.seat()), 0, y);
            for (var c : row.cells()) {
                if (c.red() == null) {
                    grid.add(Ui.support("—"), c.round(), y);
                } else {
                    var cell = new HBox(4, Ui.numeral(String.valueOf(c.red()), "red"), Ui.numeral("–", "neutral"),
                            Ui.numeral(String.valueOf(c.blue()), "blue"));
                    grid.add(cell, c.round(), y);
                }
            }
            y++;
        }
        return grid;
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
