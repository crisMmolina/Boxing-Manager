package com.sigebox.juez.terms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.sigebox.juez.R;
import com.sigebox.juez.join.JoinActivity;

/** Primer uso: términos y aviso de privacidad simplificado. Sin aceptar no se continúa (DESIGN.md §6.1). */
public class TermsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);
        CheckBox check = findViewById(R.id.terms_check);
        Button accept = findViewById(R.id.terms_accept);
        accept.setEnabled(check.isChecked());
        check.setOnCheckedChangeListener((view, checked) -> accept.setEnabled(checked));
        accept.setOnClickListener(view -> {
            new TermsStore(this).accept();
            startActivity(new Intent(this, JoinActivity.class));
            finish();
        });
    }
}
