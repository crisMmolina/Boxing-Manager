package com.sigebox.juez.ui;

import androidx.annotation.StringRes;

import com.sigebox.core.protocol.Ack;
import com.sigebox.juez.R;

/** Frase en español con qué hacer para cada error de la central (spec 003): el juez nunca ve el código. */
public final class ErrorText {
    private ErrorText() {}

    @StringRes
    public static int of(Ack.Error error) {
        switch (error) {
            case MALFORMED:
                return R.string.error_malformed;
            case NOT_APPROVED:
                return R.string.error_not_approved;
            case BAD_SIGNATURE:
                return R.string.error_bad_signature;
            case WRONG_SESSION:
                return R.string.error_wrong_session;
            case INVALID_SCORE:
                return R.string.error_invalid_score;
            case ROUND_NOT_OPEN:
                return R.string.error_round_not_open;
            case FIGHT_NOT_ACTIVE:
                return R.string.error_fight_not_active;
            case ALREADY_FINAL:
            default:
                return R.string.error_already_final;
        }
    }
}
