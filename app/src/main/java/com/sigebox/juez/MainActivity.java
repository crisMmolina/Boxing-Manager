package com.sigebox.juez;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Punto de entrada. Las tareas de cada pantalla enrutan desde aquí (términos → unión → puntuación). */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
