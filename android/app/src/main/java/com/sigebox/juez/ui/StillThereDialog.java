package com.sigebox.juez.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sigebox.juez.R;
import com.sigebox.juez.net.JudgeRuntime;

import java.util.concurrent.ExecutorService;
import com.sigebox.juez.net.CentralSilence;
import java.util.function.IntSupplier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 * AND-032: tras 10 min sin mensajes de la central, y sin round abierto, pregunta «¿La pelea sigue?» con «Seguir
 * esperando» y «Salir de la pelea». Se instala en Esperando aprobación, Puntuar, Round enviado y Siguiente pelea;
 * {@link #start()} en onStart y {@link #stop()} en onStop.
 */
public final class StillThereDialog {

    static final long CHECK_EVERY_MS = 30_000L;
    private static final String JOIN_ACTIVITY = "com.sigebox.juez.join.JoinActivity";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final Activity activity;
    private JudgeRuntime runtime;
    private final BooleanSupplier roundOpen;
    /** Sin inyectar se toman de {@link JudgeRuntime} al primer uso. */
    private CentralSilence silence;
    private IntSupplier pending;
    private IntSupplier leaveAction;
    /** Entre que se decide preguntar y que el diálogo aparece (se cuentan las tarjetas fuera del hilo principal). */
    private boolean asking;
    private final Executor io;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable check = this::check;
    private AlertDialog dialog;

    public StillThereDialog(Activity activity, BooleanSupplier roundOpen) {
        this(activity, roundOpen, null, null, null, IO);
    }

    /**
     * Para pruebas: {@code silence} cuenta el tiempo sin la central, {@code pending} cuenta las tarjetas sin ACK,
     * {@code leaveAction} sale de la pelea y devuelve las que quedan guardadas, {@code io} corre lo que toca Room fuera
     * del hilo principal.
     */
    public StillThereDialog(Activity activity, BooleanSupplier roundOpen, CentralSilence silence, IntSupplier pending,
            IntSupplier leaveAction, Executor io) {
        this.activity = activity;
        this.roundOpen = roundOpen;
        this.silence = silence;
        this.pending = pending;
        this.leaveAction = leaveAction;
        this.io = io;
    }

    private CentralSilence silence() {
        if (silence == null) {
            silence = runtime().silence();
        }
        return silence;
    }

    private JudgeRuntime runtime() {
        if (runtime == null) {
            // Perezoso: Siguiente pelea puede correr con una conexión de prueba, sin llave del equipo.
            runtime = JudgeRuntime.get(activity);
        }
        return runtime;
    }

    public void start() {
        handler.removeCallbacks(check);
        handler.postDelayed(check, CHECK_EVERY_MS);
    }

    public void stop() {
        handler.removeCallbacks(check);
        asking = false;
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void check() {
        if (dialog == null && !asking && silence().shouldAsk(roundOpen.getAsBoolean())) {
            asking = true;
            if (pending == null) {
                pending = () -> runtime().pendingCards();
            }
            io.execute(() -> {
                int cards = pending.getAsInt();
                handler.post(() -> {
                    asking = false;
                    if (dialog == null && !activity.isFinishing()) {
                        show(cards);
                    }
                });
            });
        }
        handler.postDelayed(check, CHECK_EVERY_MS);
    }

    /** {@code cards}: tarjetas sin ACK; si hay, el juez sabe antes de decidir que salir no las borra. */
    private void show(int cards) {
        CharSequence message = cards > 0
                ? activity.getResources().getQuantityString(R.plurals.still_there_message_pending, cards, cards)
                : activity.getString(R.string.still_there_message);
        // «Seguir esperando» es la acción principal (rellena); «Salir de la pelea», la secundaria (contorno).
        dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Sigebox_StillThere)
                .setTitle(R.string.still_there_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.still_there_keep_waiting, (d, w) -> {
                    dialog = null;
                    silence().keepWaiting();
                })
                .setNegativeButton(R.string.still_there_leave, (d, w) -> {
                    dialog = null;
                    leave();
                })
                .show();
    }

    private void leave() {
        handler.removeCallbacks(check);
        // Cuenta y borra en Room: fuera del hilo principal; la navegación vuelve al principal.
        if (leaveAction == null) {
            leaveAction = () -> runtime().leaveFight(() -> { }).leave();
        }
        io.execute(() -> {
            int kept = leaveAction.getAsInt();
            handler.post(() -> {
                if (kept > 0) {
                    Toast.makeText(activity.getApplicationContext(), activity.getResources()
                            .getQuantityString(R.plurals.still_there_kept, kept, kept), Toast.LENGTH_LONG).show();
                }
                activity.startActivity(new Intent().setClassName(activity, JOIN_ACTIVITY)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                activity.finish();
            });
        });
    }
}
