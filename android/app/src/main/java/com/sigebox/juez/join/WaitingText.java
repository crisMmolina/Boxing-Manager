package com.sigebox.juez.join;

import com.sigebox.juez.R;
import com.sigebox.juez.net.ConnectionState;

/** Texto principal de la pantalla de espera según el estado de la conexión. */
public final class WaitingText {
    private WaitingText() {}

    public static int forState(ConnectionState state) {
        switch (state.status) {
            case CONNECTED:
                return R.string.status_connected;
            case DISCONNECTED:
                return R.string.status_disconnected;
            case REJECTED:
                return rejected(state);
            case CONNECTING:
            case WAITING_APPROVAL:
            default:
                return R.string.status_waiting_approval;
        }
    }

    private static int rejected(ConnectionState state) {
        if (state.rejectReason == null) {
            return R.string.join_rejected_generic;
        }
        switch (state.rejectReason) {
            case TOKEN_INVALID:
                return R.string.join_rejected_token_invalid;
            case TOKEN_USED:
                return R.string.join_rejected_token_used;
            case SEAT_TAKEN:
                return R.string.join_rejected_seat_taken;
            case BAD_SIGNATURE:
                return R.string.join_rejected_bad_signature;
            case REJECTED:
            default:
                return R.string.join_rejected_generic;
        }
    }
}
