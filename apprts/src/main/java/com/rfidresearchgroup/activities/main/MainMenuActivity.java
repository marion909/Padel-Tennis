package com.rfidresearchgroup.activities.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.rfidresearchgroup.activities.connect.PN532UartConnectActivity;
import com.rfidresearchgroup.activities.statistics.StatisticsActivity;
import com.rfidresearchgroup.rfidtools.R;

public class MainMenuActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main_menu);

        // Button: Spieler registrieren
        Button btnRegisterPlayer = findViewById(R.id.btn_register_player);
        btnRegisterPlayer.setOnClickListener(v -> {
            // Zum PN532 Connect Screen (der dann zu SimpleNameReaderActivity weiterleitet)
            Intent intent = new Intent(this, PN532UartConnectActivity.class);
            startActivity(intent);
        });

        // Button: Padel Tennis
        Button btnPadelTennis = findViewById(R.id.btn_padel_tennis);
        btnPadelTennis.setOnClickListener(v -> {
            // Erst PN532 verbinden, dann zur Konfiguration
            Intent intent = new Intent(this, PN532UartConnectActivity.class);
            intent.putExtra("TARGET_ACTIVITY", "PADEL_CONFIG");
            startActivity(intent);
        });

        // Button: Statistiken
        Button btnStatistics = findViewById(R.id.btn_statistics);
        btnStatistics.setOnClickListener(v -> {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("App beenden?")
            .setMessage("Möchten Sie die App wirklich beenden?")
            .setPositiveButton("Ja", (dialog, which) -> {
                finishAffinity(); // Close all activities
            })
            .setNegativeButton("Nein", null)
            .show();
    }
}
