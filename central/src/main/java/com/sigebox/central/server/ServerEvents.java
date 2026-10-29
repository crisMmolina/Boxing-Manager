package com.sigebox.central.server;

import com.sigebox.core.model.TarjetaRound;

/**
 * Lo que el servidor avisa a la UI. Los métodos corren en hilos de Jetty: la UI debe pasar al hilo
 * de JavaFX ({@code Platform.runLater}) antes de tocar nodos. Todos son opcionales.
 */
public interface ServerEvents {

    /** Un teléfono pidió un lugar con firma válida; el organizador debe aprobar o rechazar. */
    record JoinRequest(String fightId, int seat, String judgeName, String deviceId, String fingerprint) {}

    default void onJoinRequested(JoinRequest request) {
    }

    /** Un juez aprobado quedó conectado (aprobación, {@code HELLO} o reconexión). */
    default void onJudgeConnected(String fightId, int seat) {
    }

    default void onJudgeDisconnected(String fightId, int seat) {
    }

    /** Una tarjeta de round quedó guardada (ya se envió su {@code ACK}). */
    default void onCardReceived(TarjetaRound card) {
    }

    /** La tarjeta final del juez quedó guardada con firma válida. */
    default void onFinalCard(String fightId, int seat) {
    }

    /** La tarjeta final no coincide con lo guardado o su firma es inválida; se conserva lo guardado. */
    default void onFinalCardRejected(String fightId, int seat) {
    }

    ServerEvents NONE = new ServerEvents() {};
}
