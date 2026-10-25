package com.sigebox.juez.join;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.sigebox.core.protocol.JoinUri;
import com.sigebox.juez.R;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.terms.TermsActivity;
import com.sigebox.juez.terms.TermsStore;

/** Nombre del juez + lugar (QR o código manual) → {@code JOIN} (DESIGN.md §6.2). */
public class JoinActivity extends AppCompatActivity {

    private EditText name;
    private EditText host;
    private EditText fight;
    private EditText seat;
    private EditText code;
    private View manualGroup;
    private TextView seatLabel;
    private TextView error;
    private Button submit;
    private JoinUri scanned;

    private final ActivityResultLauncher<Intent> scanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    onQrScanned(result.getData().getStringExtra(ScanActivity.EXTRA_QR));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new TermsStore(this).isAccepted()) {
            startActivity(new Intent(this, TermsActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_join);
        name = findViewById(R.id.join_name);
        host = findViewById(R.id.join_host);
        fight = findViewById(R.id.join_fight);
        seat = findViewById(R.id.join_seat_input);
        code = findViewById(R.id.join_code);
        manualGroup = findViewById(R.id.join_manual_group);
        seatLabel = findViewById(R.id.join_seat);
        error = findViewById(R.id.join_error);
        submit = findViewById(R.id.join_submit);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                render();
            }
        };
        for (EditText field : new EditText[] {name, host, fight, seat, code}) {
            field.addTextChangedListener(watcher);
        }
        findViewById(R.id.join_scan).setOnClickListener(v -> scanLauncher.launch(new Intent(this, ScanActivity.class)));
        findViewById(R.id.join_manual).setOnClickListener(v -> {
            scanned = null;
            manualGroup.setVisibility(View.VISIBLE);
            render();
        });
        submit.setOnClickListener(v -> join());
        render();
    }

    /** Resultado del escáner (o de una prueba). Un QR ajeno deja el lugar como estaba y avisa. */
    public void onQrScanned(String raw) {
        JoinUri uri = QrParser.parse(raw);
        if (uri == null) {
            error.setText(R.string.join_not_sigebox);
            error.setVisibility(View.VISIBLE);
            return;
        }
        scanned = uri;
        manualGroup.setVisibility(View.GONE);
        render();
    }

    private JoinRequest.Result current() {
        if (manualGroup.getVisibility() == View.VISIBLE) {
            return JoinRequest.manual(text(host), text(fight), text(seat), text(code), text(name));
        }
        return JoinRequest.fromQr(scanned, text(name));
    }

    private void render() {
        if (scanned != null) {
            seatLabel.setText(getString(R.string.join_seat, scanned.seat(), scanned.fightId()));
        } else {
            seatLabel.setText(R.string.join_no_seat);
        }
        JoinRequest.Result result = current();
        submit.setEnabled(result.error == null);
        int message = messageFor(result.error);
        boolean show = message != 0 && !text(name).isEmpty();
        error.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            error.setText(message);
        }
    }

    private void join() {
        JoinRequest.Result result = current();
        if (result.error != null) {
            return;
        }
        JoinRequest request = result.request;
        JudgeRuntime.get(this).connection().join(request.uri, request.judgeName);
        startActivity(new Intent(this, WaitingActivity.class)
                .putExtra(WaitingActivity.EXTRA_SEAT, request.uri.seat())
                .putExtra(WaitingActivity.EXTRA_NAME, request.judgeName));
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

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
