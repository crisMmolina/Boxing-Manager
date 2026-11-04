package com.sigebox.core.protocol;

import java.util.Locale;

/** Tipos de mensaje del protocolo v1, su DTO y su JSON Schema en {@code core/schemas/}. */
public enum MessageType {
    CHALLENGE(Challenge.class),
    JOIN(Join.class),
    JOIN_PENDING(JoinPending.class),
    JOIN_APPROVED(JoinApproved.class),
    JOIN_REJECTED(JoinRejected.class),
    HELLO(Hello.class),
    STATE(State.class),
    ROUND_OPENED(RoundOpened.class),
    ROUND_CLOSED(RoundClosed.class),
    SUBMIT_ROUND(SubmitRound.class),
    ACK(Ack.class),
    SUBMIT_FINAL_CARD(SubmitFinalCard.class),
    FIGHT_ENDED(FightEnded.class),
    FIGHT_CLOSED(FightClosed.class),
    PING(Ping.class),
    PONG(Pong.class),
    NEXT_FIGHT(NextFight.class),
    CODE_LOOKUP(CodeLookup.class),
    CODE_INFO(CodeInfo.class),
    CODE_UNKNOWN(CodeUnknown.class),
    EVENT_ENDED(EventEnded.class);

    private final Class<? extends Message> dtoClass;

    MessageType(Class<? extends Message> dtoClass) {
        this.dtoClass = dtoClass;
    }

    /** Valor de {@code type} en el JSON. */
    public String wire() {
        return name();
    }

    public Class<? extends Message> dtoClass() {
        return dtoClass;
    }

    /** Nombre del archivo del schema, p. ej. {@code submit_round.schema.json}. */
    public String schemaFile() {
        return name().toLowerCase(Locale.ROOT) + ".schema.json";
    }

    /** @throws ProtocolException si el tipo no existe */
    public static MessageType fromWire(String type) {
        for (MessageType t : values()) {
            if (t.wire().equals(type)) {
                return t;
            }
        }
        throw new ProtocolException("type desconocido");
    }
}
