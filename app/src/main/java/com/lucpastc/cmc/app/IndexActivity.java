package com.lucpastc.cmc.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class IndexActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        Button btnSingleplayer = findViewById(R.id.btnSingleplayer);
        Button btnMultiplayer = findViewById(R.id.btnMultiplayer);
        Button btnTest = findViewById(R.id.btnTest);
        Button btnOptions = findViewById(R.id.btnOptions);
        Button btnQuit = findViewById(R.id.btnQuit);

        btnSingleplayer.setOnClickListener(v -> {
            Intent intent = new Intent(IndexActivity.this, SpActivity.class);
            startActivity(intent);
            finish();
        });

        btnMultiplayer.setOnClickListener(v -> {
            // Em breve / Multiplayer
        });

        btnTest.setOnClickListener(v -> {
            // Em breve / Test
        });

        btnOptions.setOnClickListener(v -> {
            // Em breve / Options
        });

        btnQuit.setOnClickListener(v -> finish());
    }
}