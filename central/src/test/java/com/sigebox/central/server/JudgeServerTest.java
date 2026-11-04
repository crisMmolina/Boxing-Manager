package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.Messages;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pruebas con un socket real en un puerto libre (nunca el 8765). */
class JudgeServerTest {

    @TempDir
    Path dir;

    @Test
    void defaultPortIs8765() {
        assertEquals(8765, JudgeServer.DEFAULT_PORT);
    }

    @Test
    void realSocketGetsChallengeAndOversizeIsClosed() throws Exception {
        try (var db = Database.open(dir.resolve("s.db"));
                var hub = new JudgeHub(db, new SeatTokens(), ServerEvents.NONE);
                var server = JudgeServer.start(hub, 0)) {
            var first = new CompletableFuture<String>();
            var closed = new CompletableFuture<Integer>();
            var ws = HttpClient.newHttpClient().newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + server.port() + JudgeServer.PATH),
                            new WebSocket.Listener() {
                                @Override
                                public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
                                    first.complete(data.toString());
                                    w.request(1);
                                    return null;
                                }

                                @Override
                                public CompletionStage<?> onClose(WebSocket w, int code, String reason) {
                                    closed.complete(code);
                                    return null;
                                }

                                @Override
                                public void onError(WebSocket w, Throwable error) {
                                    closed.complete(-1);
                                }
                            })
                    .get(5, TimeUnit.SECONDS);

            assertInstanceOf(Challenge.class, Messages.decode(first.get(5, TimeUnit.SECONDS)));

            ws.sendText("x".repeat(JudgeHub.MAX_MESSAGE_CHARS + 1024), true);
            int code = closed.get(5, TimeUnit.SECONDS);
            assertTrue(code == 1009 || code == -1 || code == 1006, "cierre inesperado: " + code);
        }
    }
}
