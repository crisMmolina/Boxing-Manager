package com.sigebox.central.ui.log;

import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.EventoBitacora;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Vista de bitácora filtrable por pelea y tipo. */
public final class LogScreen {

    public static final String ID = "bitacora";

    private static final String ALL = "Todos";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private LogScreen() {
    }

    public static void register(Screens<Node> screens, LogQuery query) {
        screens.register(ID, "Bitácora", () -> build(query));
    }

    static Node build(LogQuery query) {
        var fight = new ComboBox<String>();
        var type = new ComboBox<String>();
        var table = new TableView<EventoBitacora>();
        table.getColumns().add(column("Hora", e -> TIME.format(Instant.ofEpochMilli(e.horaMs()))));
        table.getColumns().add(column("Pelea", e -> e.peleaId() == null ? LogQuery.GLOBAL : e.peleaId()));
        table.getColumns().add(column("Tipo", EventoBitacora::tipo));
        table.getColumns().add(column("Detalle", e -> e.detalle() == null ? "" : e.detalle()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        var count = Ui.support("");

        Runnable load = () -> {
            var f = fight.getValue();
            var t = type.getValue();
            var rows = query.find(f == null || ALL.equals(f) ? null : f, t == null || ALL.equals(t) ? null : t,
                    LogQuery.DEFAULT_LIMIT);
            table.setItems(FXCollections.observableArrayList(rows));
            count.setText(rows.size() + " eventos" + (rows.size() == LogQuery.DEFAULT_LIMIT ? " (los más recientes)" : ""));
        };
        Runnable reloadFilters = () -> {
            var fights = FXCollections.observableArrayList(ALL);
            fights.addAll(query.fights());
            var types = FXCollections.observableArrayList(ALL);
            types.addAll(query.types());
            var f = fight.getValue();
            var t = type.getValue();
            fight.setItems(fights);
            type.setItems(types);
            fight.setValue(f != null && fights.contains(f) ? f : ALL);
            type.setValue(t != null && types.contains(t) ? t : ALL);
        };
        fight.setOnAction(e -> load.run());
        type.setOnAction(e -> load.run());
        var refresh = Ui.secondary("Actualizar");
        refresh.setOnAction(e -> {
            reloadFilters.run();
            load.run();
        });
        reloadFilters.run();
        load.run();

        var filters = new HBox(12, Ui.field("Pelea", fight), Ui.field("Tipo", type), refresh, count);
        filters.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        var card = Ui.card(filters, table);
        VBox.setVgrow(card, Priority.ALWAYS);
        var root = new VBox(16, Ui.screenTitle("Bitácora"), card);
        return root;
    }

    private static TableColumn<EventoBitacora, String> column(String title,
            java.util.function.Function<EventoBitacora, String> value) {
        var col = new TableColumn<EventoBitacora, String>(title);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        return col;
    }
}
