package com.sigebox.juez;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Punto de entrada. Las tareas de cada pantalla enrutan desde aquí (términos → unión → puntuación). */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        com.sigebox.juez.ui.Screen.setup(this);
        // Unirse decide: si faltan los términos, los abre primero.
        startActivity(new android.content.Intent(this, com.sigebox.juez.join.JoinActivity.class));
        finish();
    }
}
