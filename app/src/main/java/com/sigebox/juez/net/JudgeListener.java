package com.sigebox.juez.net;

import com.sigebox.core.protocol.Message;

/** Avisos de la conexión. Se llaman en el hilo de la conexión, no en el principal. */
public interface JudgeListener {
    default void onState(ConnectionState state) {}

    /** Cada mensaje válido de la central, después de aplicarlo al estado (p. ej. {@code ROUND_CLOSED} para AND-006). */
    default void onMessage(Message message) {}
}
