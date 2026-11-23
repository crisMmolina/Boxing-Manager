package com.sigebox.juez.join;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.core.protocol.JoinUri;
import com.sigebox.core.protocol.Message;
import com.sigebox.juez.R;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.terms.TermsActivity;
import com.sigebox.juez.terms.TermsStore;
import java.util.function.Function;

/**
 * Nombre, código de lugar e IP (o el QR, que los llena) → {@code CODE_LOOKUP} → espera de aprobación (spec 004,
 * ADR 0004). Un QR de la forma anterior trae pelea y lugar y se une directo, como antes.
 */
public class JoinActivity extends AppCompatActivity {

    /** De dónde sale la conexión; las pruebas la cambian por una sin llave del equipo. */
    static Function<Context, JudgeConnection> connections = context -> JudgeRuntime.get(context).connection();

    private EditText name;
    private EditText host;
    private EditText code;
    private TextView seatLabel;
    private TextView error;
    private Button submit;
    /** QR de la forma anterior (pelea, lugar y token); con el QR nuevo los datos quedan en los campos. */
    private JoinUri legacy;
    /** Se mandó el código y se espera {@code CODE_INFO} o {@code CODE_UNKNOWN}. */
    private JoinRequest looking;
    private boolean codeRejected;
    /** El QR está llenando los campos: no cuenta como escritura a mano. */
    private boolean filling;
    private JudgeConnection listening;
    /** Sin respuesta en este tiempo se avisa; está muy por arriba de los reintentos de la conexión. */
    static final long LOOKUP_TIMEOUT_MS = 10_000;
    /** Huella de la central que traía el QR nuevo; escribir otra IP u otro código la deja de lado. */
    private String scannedPin;
    private boolean lookupFailed;
    private final Handler timeouts = new Handler(Looper.getMainLooper());
    private final Runnable lookupTimeout = this::onLookupTimeout;
    /** Al escribir el sexto dígito se busca solo, en la siguiente vuelta del hilo (no dentro del setText). */
    private final Runnable autoJoin = this::join;
    /** Sexto dígito sin nombre: se muestra el aviso aunque el nombre siga vacío. */
    private boolean askName;

    private final ActivityResultLauncher<Intent> scanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    onQrScanned(result.getData().getStringExtra(ScanActivity.EXTRA_QR));
                }
            });

    private final JudgeListener codeListener = new JudgeListener() {
        @Override
        public void onState(ConnectionState state) {}

        @Override
        public void onMessage(Message message) {
            if (message instanceof CodeInfo || message instanceof CodeUnknown) {
                runOnUiThread(() -> onCodeAnswer(message));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new TermsStore(this).isAccepted()) {
            startActivity(new Intent(this, TermsActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_join);
        com.sigebox.juez.ui.Screen.setup(this);
        com.sigebox.juez.ui.TopBar.bind(findViewById(R.id.join_root), false, getString(R.string.status_disconnected));
        name = findViewById(R.id.join_name);
        host = findViewById(R.id.join_host);
        code = findViewById(R.id.join_code);
        seatLabel = findViewById(R.id.join_seat);
        error = findViewById(R.id.join_error);
        submit = findViewById(R.id.join_submit);
        // Solo dígitos y a lo más 6, también al pegar (el teclado numérico no basta).
        // Primero se quitan guion y espacios y luego se corta a 6: «123-456» pegado deja los seis dígitos.
        code.setFilters(new InputFilter[] {
                (source, start, end, dest, dstart, dend) -> {
                    StringBuilder digits = new StringBuilder();
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        if (c >= '0' && c <= '9') {
                            digits.append(c);
                        }
                    }
                    return digits.length() == end - start ? null : digits;
                },
                new InputFilter.LengthFilter(JoinRequest.CODE_LENGTH)
        });
        // Va antes del watcher común, que llama a render() y ya ve el aviso de nombre.
        code.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                groupCode(s);
                timeouts.removeCallbacks(autoJoin);
                askName = false;
                if (filling || s.length() != JoinRequest.CODE_LENGTH) {
                    return;
                }
                if (text(name).trim().isEmpty()) {
                    askName = true; // el sexto dígito sin nombre: se pide el nombre en vez de buscar
                    name.requestFocus();
                } else {
                    timeouts.post(autoJoin);
                }
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!filling && s != name.getText()) {
                    legacy = null; // escribir IP o código a mano deja de lado el QR viejo
                    scannedPin = null;
                }
                if (s == name.getText() && !text(name).isEmpty()) {
                    askName = false;
                }
                codeRejected = false;
                lookupFailed = false;
                render();
            }
        };
        for (EditText field : new EditText[] {name, host, code}) {
            field.addTextChangedListener(watcher);
        }
        findViewById(R.id.join_scan).setOnClickListener(v -> scanLauncher.launch(new Intent(this, ScanActivity.class)));
        submit.setOnClickListener(v -> join());
        render();
    }

    @Override
    protected void onDestroy() {
        timeouts.removeCallbacks(lookupTimeout);
        timeouts.removeCallbacks(autoJoin);
        if (listening != null) {
            listening.removeListener(codeListener);
            if (looking != null) {
                listening.stop(); // nadie va a leer la respuesta: no sigue reintentando
            }
        }
        super.onDestroy();
    }

    /** Resultado del escáner (o de una prueba): llena IP y código. Un QR ajeno deja todo como estaba y avisa. */
    public void onQrScanned(String raw) {
        JoinUri uri = QrParser.parse(raw);
        if (uri == null) {
            error.setText(R.string.join_not_sigebox);
            error.setVisibility(View.VISIBLE);
            return;
        }
        filling = true;
        host.setText(uri.port() == JoinRequest.DEFAULT_PORT ? uri.host() : uri.host() + ":" + uri.port());
        code.setText(uri.code() == null ? "" : uri.code());
        filling = false;
        legacy = uri.code() == null ? uri : null;
        scannedPin = uri.code() != null ? uri.certFingerprint() : null;
        render();
    }

    JoinRequest.Result current() {
        if (legacy != null) {
            return JoinRequest.fromQr(legacy, text(name));
        }
        return JoinRequest.withCode(text(host), text(code), text(name), scannedPin);
    }

    private void render() {
        if (legacy != null) {
            seatLabel.setText(getString(R.string.join_seat, legacy.seat(), legacy.fightId()));
            seatLabel.setVisibility(View.VISIBLE);
        } else {
            seatLabel.setVisibility(View.GONE);
        }
        JoinRequest.Result result = current();
        submit.setEnabled(result.error == null && looking == null);
        int message = codeRejected ? R.string.join_code_unknown
                : lookupFailed ? R.string.join_lookup_timeout
                : looking != null ? R.string.join_looking : messageFor(result.error);
        boolean show = message != 0
                && (codeRejected || lookupFailed || looking != null || askName || !text(name).isEmpty());
        error.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            error.setText(message);
        }
    }

    private void join() {
        timeouts.removeCallbacks(autoJoin);
        JoinRequest.Result result = current();
        if (result.error != null || looking != null) {
            return;
        }
        JoinRequest request = result.request;
        JudgeConnection connection = connections.apply(this);
        if (listening == null && request.uri.code() != null) {
            listening = connection;
            connection.addListener(codeListener);
        }
        connection.join(request.uri, request.judgeName);
        if (request.uri.code() == null) {
            openWaiting(request.uri.seat(), request.judgeName, null, null); // QR sin código: aún sin esquinas
            return;
        }
        // Con el código, la pelea y el lugar los dice la central; se espera aquí para avisar si no es válido.
        looking = request;
        codeRejected = false;
        lookupFailed = false;
        timeouts.postDelayed(lookupTimeout, LOOKUP_TIMEOUT_MS);
        render();
    }

    private void onCodeAnswer(Message message) {
        JoinRequest request = looking;
        if (request == null || !request.uri.code().equals(code(message))) {
            return;
        }
        looking = null;
        timeouts.removeCallbacks(lookupTimeout);
        if (message instanceof CodeInfo info) {
            openWaiting(info.seat(), request.judgeName, info.redCorner(), info.blueCorner());
        } else {
            codeRejected = true;
        }
        render();
    }

    /** Nadie contestó: se deja de buscar y se puede reintentar; una respuesta tardía ya no cuenta. */
    private void onLookupTimeout() {
        looking = null;
        lookupFailed = true;
        if (listening != null) {
            listening.stop();
        }
        render();
    }

    private static String code(Message message) {
        return message instanceof CodeInfo info ? info.code() : ((CodeUnknown) message).code();
    }

    private void openWaiting(int seat, String judgeName, String red, String blue) {
        startActivity(new Intent(this, WaitingActivity.class)
                .putExtra(WaitingActivity.EXTRA_SEAT, seat)
                .putExtra(WaitingActivity.EXTRA_NAME, judgeName)
                .putExtra(WaitingActivity.EXTRA_RED, red)
                .putExtra(WaitingActivity.EXTRA_BLUE, blue));
    }

    private static int messageFor(JoinRequest.Error error) {
        if (error == null) {
            return 0;
        }
        switch (error) {
            case NAME_EMPTY:
                return R.string.join_error_name_empty;
            case NAME_TOO_LONG:
                return R.string.join_error_name_too_long;
            case HOST_INVALID:
                return R.string.join_error_host;
            case FIGHT_INVALID:
                return R.string.join_error_fight;
            case SEAT_INVALID:
                return R.string.join_error_seat;
            case CODE_INVALID:
                return R.string.join_error_code;
            case NO_SEAT:
            default:
                return 0;
        }
    }

    /** Tras el tercer dígito va un hueco dibujado; el texto sigue siendo solo dígitos. */
    private static final int GROUP = 3;

    private static void groupCode(Editable s) {
        for (CodeGapSpan old : s.getSpans(0, s.length(), CodeGapSpan.class)) {
            s.removeSpan(old);
        }
        if (s.length() > GROUP) {
            s.setSpan(new CodeGapSpan(), GROUP - 1, GROUP, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** Lo que ve el juez: un espacio donde el campo dibuja el hueco («123 456»). */
    static String visibleCode(CharSequence s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        Spanned spanned = s instanceof Spanned sp ? sp : null;
        for (int i = 0; i < s.length(); i++) {
            out.append(s.charAt(i));
            boolean gap = spanned != null
                    ? spanned.getSpans(i, i + 1, CodeGapSpan.class).length > 0
                    : i == GROUP - 1 && s.length() > GROUP;
            if (gap) {
                out.append(' ');
            }
        }
        return out.toString();
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
