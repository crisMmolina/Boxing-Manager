package com.sigebox.juez.join;

import com.sigebox.core.protocol.JoinUri;

/** Texto leído de un QR → lugar de juez, o {@code null} si no es un QR de SIGEBOX válido. */
public final class QrParser {
    private QrParser() {}

    public static JoinUri parse(String raw) {
        if (raw == null || !raw.startsWith(JoinUri.SCHEME + "://")) {
            return null;
        }
        try {
            return JoinUri.parse(raw);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
