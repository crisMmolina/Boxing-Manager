package com.sigebox.core.protocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Huella de la tarjeta final de un juez (la firma va aparte, en {@link DeviceProof}): SHA-256 en hex (minúsculas) de un texto canónico. El teléfono y la
 * central la calculan igual.
 *
 * <p>Texto canónico, una línea por elemento terminada en {@code \n}: {@code SIGEBOX-FINAL-v1}, el
 * {@code fightId}, el lugar y, por round ascendente, {@code round:rojo:azul}. El estado de la tarjeta no
 * entra.
 *
 * <p>Solo APIs disponibles en Android 8 (API 26): sin {@code HexFormat}.
 */
public final class FinalCardHash {

    private FinalCardHash() {}

    /** @throws IllegalArgumentException si un round aparece dos veces */
    public static String of(String fightId, int seat, List<State.OwnCard> cards) {
        return sha256Hex(canonical(fightId, seat, cards).getBytes(StandardCharsets.UTF_8));
    }

    static String canonical(String fightId, int seat, List<State.OwnCard> cards) {
        List<State.OwnCard> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparing(State.OwnCard::round));
        StringBuilder sb = new StringBuilder("SIGEBOX-FINAL-v1\n").append(fightId).append('\n').append(seat).append('\n');
        Integer previous = null;
        for (State.OwnCard card : sorted) {
            if (card.round().equals(previous)) {
                throw new IllegalArgumentException("round repetido: " + previous);
            }
            previous = card.round();
            sb.append(card.round()).append(':').append(card.red()).append(':').append(card.blue()).append('\n');
        }
        return sb.toString();
    }

    /** SHA-256 en hex (minúsculas). */
    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest(data)) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
