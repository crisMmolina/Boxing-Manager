package com.sigebox.juez.help;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.net.JudgeRuntime;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Hoja "Recuperar la conexión" que abre el "?" de la barra (DESIGN.md v4 §5.8, {@code docs/design/guia-conexion.md}):
 * estado e IP a la que intenta conectarse, pasos del teléfono y después los de la central. Todo sale de
 * {@code strings_help.xml}; no usa internet.
 */
public final class ConnectionHelp {
    private static final Function<Context, String> SAVED_HOST = context -> {
        SessionEntity session = JudgeRuntime.get(context).database().sessionDao().load();
        return session == null || session.host.isEmpty() ? null : session.host;
    };
    private static final Executor BACKGROUND = Executors.newSingleThreadExecutor();

    /** De dónde sale la IP guardada; Room no se lee en el hilo principal. Las pruebas lo reemplazan. */
    static Function<Context, String> hostSource = SAVED_HOST;
    static Executor executor = BACKGROUND;

    private ConnectionHelp() {
    }

    static void reset() {
        hostSource = SAVED_HOST;
        executor = BACKGROUND;
    }

    public static BottomSheetDialog show(Context context, boolean connected) {
        var dialog = new BottomSheetDialog(context);
        var view = LayoutInflater.from(context).inflate(R.layout.dialog_help, null, false);
        ((TextView) view.findViewById(R.id.help_state)).setText(context.getString(R.string.help_state,
                context.getString(connected ? R.string.status_connected : R.string.status_disconnected)));
        TextView host = view.findViewById(R.id.help_host);
        host.setText(R.string.help_host_none);
        view.findViewById(R.id.help_done).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
        Context app = context.getApplicationContext();
        executor.execute(() -> {
            String ip = hostSource.apply(app);
            host.post(() -> host.setText(ip == null
                    ? context.getString(R.string.help_host_none)
                    : context.getString(R.string.help_host, ip)));
        });
        return dialog;
    }
}
