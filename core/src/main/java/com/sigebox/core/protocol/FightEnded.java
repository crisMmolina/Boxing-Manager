package com.sigebox.core.protocol;

import com.sigebox.core.model.MotivoTerminacion;

/**
 * FIGHT_ENDED (central → juez).
 *
 * <p>La pelea terminó (último round o terminación); cada juez envía su tarjeta final.
 *
 * <p>Opcionales ({@code null} se omite): {@code reason}.
 */
public record FightEnded(
        int v,
        String id,
        String type,
        String fightId,
        MotivoTerminacion reason) implements Message {

    public static final String TYPE = "FIGHT_ENDED";

    public FightEnded {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public FightEnded(String fightId, MotivoTerminacion reason) {
        this(VERSION, Fields.newId(), TYPE, fightId, reason);
    }
}
