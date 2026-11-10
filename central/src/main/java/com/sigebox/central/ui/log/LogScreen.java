package com.sigebox.central.ui.log;

import com.sigebox.central.ui.Labels;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Skeleton;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.EventoBitacora;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.util.StringConverter;

/** Vista de bitácora filtrable por pelea y tipo; los eventos nuevos aparecen solos. */
public final class LogScreen {

    public static final String ID = "bitacora";

    private static final String ALL = "Todos";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /** Filtros elegidos; {@code null} es "Todos". */
    private record Filter(String fight, String type) {
    }

    private record Snapshot(Filter filter, List<EventoBitacora> rows, List<String> fights, List<String> types,
            java.util.Map<String, String> nombres) {
    }

    private LogScreen() {
    }

    public static void register(Screens<Node> screens, LogQuery query) {
        register(screens, query, LiveRefresh.BACKGROUND);
    }

    /** Como {@link #register(Screens, LogQuery)}, con las lecturas en {@code reads}. */
    public static void register(Screens<Node> screens, LogQuery query, Executor reads) {
        screens.register(ID, "Bitácora", () -> build(query, reads));
    }

    static Node build(LogQuery query) {
        return build(query, LiveRefresh.DIRECT);
    }

    static Node build(LogQuery query, Executor reads) {
        var fight = new ComboBox<String>();
        var type = new ComboBox<String>();
        fight.getItems().add(ALL);
        type.getItems().add(ALL);
        fight.setValue(ALL);
        type.setValue(ALL);
        type.setConverter(new StringConverter<>() { // muestra la etiqueta; el valor sigue siendo el código
            @Override
            public String toString(String code) {
                return typeLabel(code);
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        var table = new TableView<EventoBitacora>();
        table.getColumns().add(column("Hora", e -> TIME.format(Instant.ofEpochMilli(e.horaMs()))));
        // Spec 005: la pelea se nombra por su lugar y sus esquinas; el id interno no se muestra.
        var nombres = new AtomicReference<java.util.Map<String, String>>(java.util.Map.of());
        table.getColumns().add(column("Pelea", e -> LogQuery.nombre(nombres.get(), e.peleaId())));
        table.getColumns().get(0).setMinWidth(96); // la hora completa, sin "…"
        fight.setConverter(new StringConverter<>() { // muestra el nombre; el valor sigue siendo el id
            @Override
            public String toString(String id) {
                return ALL.equals(id) ? ALL : LogQuery.nombre(nombres.get(), id);
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        var tipo = column("Tipo", e -> e.tipo());
        tipo.setCellFactory(c -> new javafx.scene.control.TableCell<>() { // DESIGN v5: el tipo como chip
            @Override
            protected void updateItem(String code, boolean empty) {
                super.updateItem(code, empty);
                setText(null);
                setGraphic(empty || code == null ? null : typeChip(code));
            }
        });
        table.getColumns().add(tipo);
        table.getColumns().add(column("Detalle",
                e -> Labels.mensaje(e.tipo(), e.detalle()).replace("función ", "evento ")));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        var slot = new VBox(Skeleton.lines(3));
        VBox.setVgrow(slot, Priority.ALWAYS);
        var count = Ui.support("");

        var filters = new HBox(12, Ui.field("Pelea", fight), Ui.field("Tipo", type), count);
        filters.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        var card = Ui.card(filters, slot);
        VBox.setVgrow(card, Priority.ALWAYS);
        var root = new VBox(16, new com.sigebox.central.ui.theme.PageHeader("Bitácora"),
                Ui.next("Filtra por pelea o tipo para revisar lo ocurrido."), card);

        var filter = new AtomicReference<>(new Filter(null, null));
        var live = LiveRefresh.watch(root, reads, () -> {
            var f = filter.get();
            return new Snapshot(f, query.find(f.fight(), f.type(), LogQuery.DEFAULT_LIMIT), query.fights(), query.types(),
                    query.nombres());
        }, s -> {
            nombres.set(s.nombres());
            sync(fight, s.fights());
            sync(type, s.types());
            show(table, s.rows());
            count.setText(s.rows().size() + " eventos"
                    + (s.rows().size() == LogQuery.DEFAULT_LIMIT ? " (los más recientes)" : ""));
            LiveRefresh.swap(slot, List.of(table));
        });
        Runnable choose = () -> {
            filter.set(new Filter(chosen(fight), chosen(type)));
            live.poke();
        };
        fight.setOnAction(e -> choose.run());
        type.setOnAction(e -> choose.run());
        return root;
    }

    private static String chosen(ComboBox<String> box) {
        var value = box.getValue();
        return value == null || ALL.equals(value) ? null : value;
    }

    /** Actualiza las opciones sin cambiar la lista: un filtro abierto sigue abierto y conserva lo elegido. */
    private static void sync(ComboBox<String> box, List<String> options) {
        var next = new ArrayList<>(List.of(ALL));
        next.addAll(options);
        var items = box.getItems();
        if (items.equals(next)) {
            return;
        }
        var value = box.getValue();
        items.retainAll(next);
        for (int i = 0; i < next.size(); i++) {
            if (!items.contains(next.get(i))) {
                items.add(i, next.get(i));
            }
        }
        if (value == null || !next.contains(value)) {
            box.setValue(ALL);
        }
    }

    /** Cambia las filas solo si son otras y conserva la seleccionada. */
    private static void show(TableView<EventoBitacora> table, List<EventoBitacora> rows) {
        if (table.getItems().equals(rows)) {
            return;
        }
        var selected = table.getSelectionModel().getSelectedItem();
        table.getItems().setAll(rows);
        if (selected != null && rows.contains(selected)) {
            table.getSelectionModel().select(selected);
        }
    }

    /** Etiqueta del filtro de tipo: "Todos" o la etiqueta en español del código guardado. */
    static String typeLabel(String code) {
        return code == null || ALL.equals(code) ? ALL : Labels.evento(code);
    }

    /** El tipo de un evento como chip: verde lo que salió bien, dorado lo que espera, neutro lo demás. */
    static javafx.scene.control.Label typeChip(String code) {
        return Ui.chip(Labels.evento(code), typeVariant(code), null);
    }

    static String typeVariant(String code) {
        if (code.contains("APPROVED") || code.contains("CLOSED") || code.contains("CONNECTED") && !code.contains("DIS")
                || code.endsWith("_SENT") || code.contains("RECEIVED") || code.contains("ACCEPTED")) {
            return "success";
        }
        if (code.contains("REQUESTED") || code.contains("PENDING") || code.contains("OPENED") || code.contains("WAIT")) {
            return "pending";
        }
        return "neutral";
    }

    private static TableColumn<EventoBitacora, String> column(String title,
            java.util.function.Function<EventoBitacora, String> value) {
        var col = new TableColumn<EventoBitacora, String>(title);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        return col;
    }
}
