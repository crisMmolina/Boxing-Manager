package com.sigebox.central.server;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.time.Duration;

/** Javalin 7 en {@code /ws}: traduce el WebSocket a {@link JudgeHub}. */
public final class JudgeServer implements AutoCloseable {

    public static final int DEFAULT_PORT = 8765;
    public static final String PATH = "/ws";

    /** Los jueces mandan PING cada 5 s; sin tráfico en este tiempo la conexión se da por muerta. */
    static final Duration IDLE_TIMEOUT = Duration.ofSeconds(30);

    private final Javalin app;

    private JudgeServer(Javalin app) {
        this.app = app;
    }

    /** Arranca en {@code 0.0.0.0:port}; {@code port = 0} elige uno libre (pruebas). */
    public static JudgeServer start(JudgeHub hub, int port) {
        var app = Javalin.create(cfg -> {
            cfg.jetty.modifyWebSocketServletFactory(f -> {
                f.setMaxTextMessageSize(JudgeHub.MAX_MESSAGE_CHARS);
                f.setMaxBinaryMessageSize(1024);
                f.setIdleTimeout(IDLE_TIMEOUT);
            });
            cfg.routes.ws(PATH, ws -> {
                ws.onConnect(ctx -> hub.onOpen(new CtxConn(ctx)));
                ws.onMessage(ctx -> hub.onMessage(ctx.sessionId(), ctx.message()));
                ws.onBinaryMessage(ctx -> ctx.closeSession(Conn.POLICY, "binary"));
                ws.onClose(ctx -> hub.onClose(ctx.sessionId()));
                ws.onError(ctx -> hub.onClose(ctx.sessionId()));
            });
        });
        app.start("0.0.0.0", port);
        return new JudgeServer(app);
    }

    public int port() {
        return app.port();
    }

    @Override
    public void close() {
        app.stop();
    }

    private record CtxConn(WsContext ctx) implements Conn {
        @Override
        public String id() {
            return ctx.sessionId();
        }

        @Override
        public void send(String text) {
            if (ctx.session.isOpen()) {
                ctx.send(text);
            }
        }

        @Override
        public void close(int code, String reason) {
            ctx.closeSession(code, reason);
        }
    }
}
