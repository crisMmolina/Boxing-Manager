package com.sigebox.central.ui.setup;

import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.service.FightService;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.help.HelpButton;
import com.sigebox.central.ui.theme.Skeleton;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.Pelea;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Preparar la pelea y emparejar a los jueces (DESIGN v3 §7.1): formulario de la pelea a la
 * izquierda, una tarjeta por lugar a la derecha (QR + código, aprobar con la huella corta, estado de
 * conexión) y el pie con la dirección para los jueces e "Iniciar pelea".
 *
 * <p>Las invitaciones se guardan entre vistas: volver a la pantalla no invalida un QR ya mostrado.
 * Solo "Nuevo código" emite uno nuevo.
 */
public final class SetupScreen {

    public static final String ID = "pelea";

    private final FightService fights;
    private final JudgeHub hub;
    private final int port;
    private final Runnable onStart;
    private final Executor reads;
    /** Se escriben en el hilo de JavaFX y se leen en la lectura de fondo. */
    private final Map<String, FightService.Invitation> invitations = new ConcurrentHashMap<>();
    private volatile String host;

    /** Lo que la pantalla dibuja; con otro valor se redibuja, con el mismo no se pierde el foco. */
    private record Snapshot(Pelea pelea, List<Integer> connected, List<FightService.Seat> seats,
            List<FightService.Invitation> invitations, String host) {
        static final Snapshot NO_FIGHT = new Snapshot(null, List.of(), List.of(), List.of(), null);
    }

    public SetupScreen(FightService fights, JudgeHub hub, int port, Runnable onStart) {
        this(fights, hub, port, onStart, LiveRefresh.BACKGROUND);
    }

    /** Con las lecturas en {@code reads}: mientras no llegan, la pantalla muestra su skeleton. */
    public SetupScreen(FightService fights, JudgeHub hub, int port, Runnable onStart, Executor reads) {
        this.fights = fights;
        this.hub = hub;
        this.port = port;
        this.onStart = onStart;
        this.reads = reads;
    }

    /** Registra la pantalla bajo {@code id} (los pasos "Pelea" y "Jueces" pueden compartirla). */
    public void register(Screens<Node> screens, String id, String title) {
        screens.register(id, title, this::build);
    }

    Node build() {
        var status = Ui.support("");
        var left = new VBox(16, Skeleton.lines(3));
        left.setPrefWidth(340);
        left.setMinWidth(340);
        var seats = new FlowPane(16, 16, Skeleton.block(240, 200));
        seats.setPrefWrapLength(780); // sin esto el FlowPane pide una sola columna muy alta
        HBox.setHgrow(seats, Priority.ALWAYS);
        var footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_LEFT);
        var next = Ui.next("Un momento, leyendo la pelea.");
        var root = new VBox(16, Ui.screenTitle("Emparejar jueces"), next, new HBox(24, left, seats), footer, status);

        LiveRefresh.watch(root, reads, this::read, s -> {
            if (s.pelea() == null) {
                Ui.next(next, "Crea la pelea para emparejar a los jueces.");
                LiveRefresh.swap(left, List.of(form(status)));
                LiveRefresh.swap(seats, List.of(Ui.support("Crea la pelea para ver el código de cada lugar.")));
                footer.getChildren().clear();
                return;
            }
            var id = s.pelea().id();
            Ui.next(next, fights.pendingSeats(id, s.connected()) > 0
                    ? "Pide a cada juez que escanee su QR y aprueba su huella."
                    : "Inicia la pelea.");
            LiveRefresh.swap(left, List.of(summary(s.pelea())));
            LiveRefresh.swap(seats, s.seats().stream().map(seat -> seatCard(id, seat, status)).toList());
            footer.getChildren().setAll(footer(id, s.connected()));
        });
        return root;
    }

    private Snapshot read() {
        var active = fights.active();
        if (active.isEmpty()) {
            return Snapshot.NO_FIGHT;
        }
        var id = active.get().id();
        var connected = hub.connectedSeats(id);
        return new Snapshot(active.get(), connected, fights.seats(id, connected), List.copyOf(invitations.values()), host);
    }

    private Node form(Label status) {
        var red = new TextField();
        var blue = new TextField();
        var rounds = new Spinner<Integer>(1, 12, 10);
        var judges = new Spinner<Integer>(1, 5, 3);
        var create = Ui.primary("Crear pelea");
        create.setMaxWidth(Double.MAX_VALUE);
        create.setOnAction(e -> {
            try {
                fights.create(red.getText(), blue.getText(), rounds.getValue(), judges.getValue());
                status.setText("");
            } catch (RuntimeException ex) {
                status.setText(ex.getMessage());
            }
        });
        return Ui.card(Ui.cardTitle("Pelea"), Ui.field("Esquina roja", "Ej. Ana Pérez", red),
                Ui.field("Esquina azul", "Ej. Bea Ruiz", blue), new HBox(12, Ui.field("Rounds", rounds), Ui.field("Jueces", judges)), create);
    }

    private Node summary(com.sigebox.core.model.Pelea p) {
        var red = new HBox(8, Ui.cornerDot(true), Ui.body(p.esquinaRoja()));
        red.setAlignment(Pos.CENTER_LEFT);
        var blue = new HBox(8, Ui.cornerDot(false), Ui.body(p.esquinaAzul()));
        blue.setAlignment(Pos.CENTER_LEFT);
        var id = Ui.numeral(p.id(), "neutral");
        id.setAccessibleText("Pelea " + String.join(" ", p.id().split("")));
        // Para unirse a mano (campo "Pelea" de la app) o con judge-sim --fight.
        var copy = Ui.secondary("Copiar");
        copy.setOnAction(e -> {
            var content = new ClipboardContent();
            content.putString(p.id());
            Clipboard.getSystemClipboard().setContent(content);
            copy.setText("Copiado");
        });
        var idRow = new HBox(12, id, copy);
        idRow.setAlignment(Pos.CENTER_LEFT);
        return Ui.card(Ui.cardTitle("Pelea"), Ui.field("Código de la pelea", idRow), red, blue,
                Ui.support(p.rounds() + " rounds · " + p.jueces() + (p.jueces() == 1 ? " juez" : " jueces")));
    }

    private Node seatCard(String fightId, FightService.Seat s, Label status) {
        boolean connected = s.state() == FightService.SeatState.CONECTADO;
        var who = "Juez " + s.seat();
        var head = new HBox(8, Ui.cardTitle(who), Ui.grow(), Ui.dot(connected, who));
        head.setAlignment(Pos.CENTER_LEFT);
        var card = Ui.card(head);
        card.setPrefWidth(240);
        card.setMinWidth(220);
        switch (s.state()) {
            case SIN_CONECTAR, RECHAZADO -> {
                var inv = invitations.computeIfAbsent(key(fightId, s.seat()),
                        k -> fights.invite(fightId, s.seat(), host(), port));
                var code = Ui.numeral(spaced(inv.code()), "neutral");
                code.setAccessibleText("Código " + String.join(" ", inv.code().split("")));
                var again = Ui.secondary("Nuevo código");
                again.setOnAction(e -> {
                    invitations.put(key(fightId, s.seat()), fights.invite(fightId, s.seat(), host(), port));
                    status.setText("Nuevo código para el juez " + s.seat() + ". El anterior ya no sirve.");
                });
                if (s.state() == FightService.SeatState.RECHAZADO) {
                    card.getChildren().add(Ui.support("Rechazado"));
                }
                card.getChildren().addAll(qr(inv.qrUri()), code, again);
            }
            case ESPERANDO_APROBACION -> {
                var approve = Ui.primary("Aprobar");
                approve.setOnAction(e -> {
                    if (!hub.approve(fightId, s.seat())) {
                        status.setText("No hay solicitud pendiente para el juez " + s.seat() + ".");
                    }
                });
                var reject = Ui.secondary("Rechazar");
                reject.setOnAction(e -> hub.reject(fightId, s.seat()));
                approve.setMaxWidth(Double.MAX_VALUE);
                reject.setMaxWidth(Double.MAX_VALUE);
                var print = Ui.numeral(s.shortFingerprint() == null ? "—" : s.shortFingerprint(), "neutral");
                card.getChildren().addAll(Ui.body(s.judgeName() == null ? "Sin nombre" : s.judgeName()),
                        Ui.field("Huella del teléfono", print),
                        Ui.support("Compárala con la que muestra el teléfono del juez."), new VBox(8, approve, reject));
            }
            case CONECTADO -> card.getChildren().add(Ui.body(s.judgeName()));
            case SIN_CONEXION -> card.getChildren().addAll(Ui.body(s.judgeName()), HelpButton.connection(false, who));
        }
        return card;
    }

    private Node footer(String fightId, List<Integer> connected) {
        var addresses = FightService.localAddresses();
        var choose = new ComboBox<>(FXCollections.observableArrayList(addresses));
        choose.setValue(host());
        choose.setOnAction(e -> {
            if (choose.getValue() != null && !choose.getValue().equals(host)) {
                host = choose.getValue();
                invitations.clear(); // el QR lleva la IP: se emiten de nuevo
            }
        });
        int pending = fights.pendingSeats(fightId, connected);
        var start = Ui.primary("Iniciar pelea");
        start.setDisable(pending > 0);
        start.setOnAction(e -> onStart.run());
        var why = Ui.why("Falta que se conecte" + (pending == 1 ? " 1 juez." : "n " + pending + " jueces."), start);
        var address = new HBox(8, Ui.field("Dirección para los jueces", choose), Ui.support(":" + port));
        address.setAlignment(Pos.BOTTOM_LEFT);
        return new HBox(16, address, Ui.grow(), why, start);
    }

    private String host() {
        if (host == null) {
            host = FightService.localAddresses().getFirst();
        }
        return host;
    }

    private static String key(String fightId, int seat) {
        return fightId + "/" + seat;
    }

    /** "482913" → "482 913". */
    static String spaced(String code) {
        return code.length() == 6 ? code.substring(0, 3) + " " + code.substring(3) : code;
    }

    /** QR de 168 px con margen blanco. */
    static ImageView qr(String text) {
        var matrix = QrCode.matrix(text, QrCode.SIZE);
        var img = new WritableImage(matrix.getWidth(), matrix.getHeight());
        var w = img.getPixelWriter();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                w.setArgb(x, y, matrix.get(x, y) ? QrCode.INK : QrCode.PAPER);
            }
        }
        var view = new ImageView(img);
        view.setAccessibleText("Código QR para unirse");
        return view;
    }
}
