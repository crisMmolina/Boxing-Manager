package com.sigebox.central.ui.setup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.service.FightService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Preparar la pelea y emparejar a los jueces (DESIGN v2 §7.1): formulario de la pelea a la
 * izquierda, una tarjeta por lugar a la derecha (QR + código, aprobar con la huella corta, estado de
 * conexión) y el pie con la dirección para los jueces e "Iniciar pelea".
 *
 * <p>Las invitaciones se guardan entre vistas: volver a la pantalla no invalida un QR ya mostrado.
 * Solo "Nuevo código" emite uno nuevo.
 */
public final class SetupScreen {

    public static final String ID = "pelea";

    static final int QR_SIZE = 168;

    private final FightService fights;
    private final JudgeHub hub;
    private final int port;
    private final Runnable onStart;
    private final Map<String, FightService.Invitation> invitations = new HashMap<>();
    private String host;

    public SetupScreen(FightService fights, JudgeHub hub, int port, Runnable onStart) {
        this.fights = fights;
        this.hub = hub;
        this.port = port;
        this.onStart = onStart;
    }

    /** Registra la pantalla bajo {@code id} (los pasos "Pelea" y "Jueces" pueden compartirla). */
    public void register(Screens<Node> screens, String id, String title) {
        screens.register(id, title, this::build);
    }

    Node build() {
        var status = Ui.support("");
        var left = new VBox(16);
        left.setPrefWidth(340);
        left.setMinWidth(340);
        var seats = new FlowPane(16, 16);
        seats.setPrefWrapLength(780); // sin esto el FlowPane pide una sola columna muy alta
        HBox.setHgrow(seats, Priority.ALWAYS);
        var footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_LEFT);
        var root = new VBox(16, Ui.screenTitle("Emparejar jueces"), new HBox(24, left, seats), footer, status);

        String[] signature = {""};
        Runnable refresh = () -> {
            var active = fights.active();
            if (active.isEmpty()) {
                if (!"form".equals(signature[0])) {
                    left.getChildren().setAll(form(status));
                    seats.getChildren().setAll(Ui.support("Crea la pelea para ver el código de cada lugar."));
                    footer.getChildren().clear();
                    signature[0] = "form";
                }
                return;
            }
            var pelea = active.get();
            var connected = hub.connectedSeats(pelea.id());
            var list = fights.seats(pelea.id(), connected);
            var sig = pelea.id() + list + invitations.values() + host;
            if (sig.equals(signature[0])) {
                return; // sin cambios: no se redibuja (no se pierde el foco)
            }
            signature[0] = sig;
            left.getChildren().setAll(summary(pelea));
            seats.getChildren().setAll(list.stream().map(s -> seatCard(pelea.id(), s, status)).toList());
            footer.getChildren().setAll(footer(pelea.id(), connected));
        };

        var timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refresh.run()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                timer.stop();
            }
        });
        refresh.run();
        return root;
    }

    private Node form(Label status) {
        var red = new TextField();
        red.setPromptText("Nombre de la esquina roja");
        var blue = new TextField();
        blue.setPromptText("Nombre de la esquina azul");
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
        var redRow = new HBox(8, Ui.cornerDot(true), Ui.support("Roja"));
        redRow.setAlignment(Pos.CENTER_LEFT);
        var blueRow = new HBox(8, Ui.cornerDot(false), Ui.support("Azul"));
        blueRow.setAlignment(Pos.CENTER_LEFT);
        return Ui.glass(Ui.cardTitle("Pelea"), new VBox(6, redRow, red), new VBox(6, blueRow, blue),
                new HBox(12, Ui.field("Rounds", rounds), Ui.field("Jueces", judges)), create);
    }

    private Node summary(com.sigebox.core.model.Pelea p) {
        var red = new HBox(8, Ui.cornerDot(true), Ui.body(p.esquinaRoja()));
        red.setAlignment(Pos.CENTER_LEFT);
        var blue = new HBox(8, Ui.cornerDot(false), Ui.body(p.esquinaAzul()));
        blue.setAlignment(Pos.CENTER_LEFT);
        var id = Ui.numeral(p.id(), "neutral");
        id.setAccessibleText("Pelea " + String.join(" ", p.id().split("")));
        return Ui.glass(Ui.cardTitle("Pelea"), Ui.field("Código de la pelea", id), red, blue,
                Ui.support(p.rounds() + " rounds · " + p.jueces() + (p.jueces() == 1 ? " juez" : " jueces")));
    }

    private Node seatCard(String fightId, FightService.Seat s, Label status) {
        boolean connected = s.state() == FightService.SeatState.CONECTADO;
        var who = "Juez " + s.seat();
        var head = new HBox(8, Ui.cardTitle(who), Ui.grow(), Ui.dot(connected, who));
        head.setAlignment(Pos.CENTER_LEFT);
        var card = Ui.glass(head);
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
                card.getChildren().addAll(qr(inv.uri()), code, again);
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
            case SIN_CONEXION -> card.getChildren().addAll(Ui.body(s.judgeName()), Ui.support("Reconectando…"));
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
        var hint = Ui.support(pending == 0 ? "Todos los jueces están conectados."
                : "Falta que se conecte" + (pending == 1 ? " 1 juez." : "n " + pending + " jueces."));
        return new HBox(16, Ui.support("Dirección para los jueces"), choose, Ui.support(":" + port), Ui.grow(), hint,
                start);
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
        try {
            var matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE,
                    Map.of(EncodeHintType.MARGIN, 1));
            var img = new WritableImage(matrix.getWidth(), matrix.getHeight());
            var w = img.getPixelWriter();
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    w.setArgb(x, y, matrix.get(x, y) ? 0xFF0F172A : 0xFFFFFFFF);
                }
            }
            var view = new ImageView(img);
            view.setAccessibleText("Código QR para unirse");
            return view;
        } catch (WriterException e) {
            throw new IllegalStateException("no se pudo generar el QR", e);
        }
    }
}
