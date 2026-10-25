package com.sigebox.juez.terms;

import android.content.Context;
import android.content.SharedPreferences;

/** Si este teléfono ya aceptó términos y aviso de privacidad (RJ-12). Cambiar la versión los vuelve a pedir. */
public final class TermsStore {
    private static final String PREFS = "sigebox_terms";
    private static final String KEY = "accepted_v1";

    private final SharedPreferences prefs;

    public TermsStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isAccepted() {
        return prefs.getBoolean(KEY, false);
    }

    public void accept() {
        prefs.edit().putBoolean(KEY, true).apply();
    }
}
