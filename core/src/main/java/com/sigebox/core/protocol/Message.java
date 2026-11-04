package com.sigebox.core.protocol;

/**
 * Un mensaje WebSocket del protocolo v1. Todos llevan {@code v}, {@code id} (UUID) y {@code type}.
 *
 * <p>Los campos requeridos se validan al construir el record, también al decodificar: un campo
 * faltante nunca se lee como 0 o {@code false}. También se validan longitudes, rangos y patrones de su Schema.
 * Que el juez, el round y la pelea correspondan a la sesión lo valida {@code com.sigebox.core.validation}.
 */
public sealed interface Message
        permits Challenge, Join, JoinPending, JoinApproved, JoinRejected, Hello, State, RoundOpened, RoundClosed,
                SubmitRound, Ack, SubmitFinalCard, FightEnded, FightClosed, Ping, Pong, NextFight, CodeLookup, CodeInfo,
                CodeUnknown, EventEnded {

    /** Versión del protocolo. */
    int VERSION = 1;

    int v();

    String id();

    String type();
}
