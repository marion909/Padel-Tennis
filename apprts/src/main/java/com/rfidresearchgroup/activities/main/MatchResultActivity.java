package com.rfidresearchgroup.activities.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.rfidresearchgroup.activities.statistics.StatisticsActivity;
import com.rfidresearchgroup.rfidtools.R;

public class MatchResultActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_match_result);

        // Get match data from intent
        String winnerTeam = getIntent().getStringExtra("WINNER_TEAM");
        String teamAName = getIntent().getStringExtra("TEAM_A_NAME");
        String teamBName = getIntent().getStringExtra("TEAM_B_NAME");
        int setsTeamA = getIntent().getIntExtra("SETS_TEAM_A", 0);
        int setsTeamB = getIntent().getIntExtra("SETS_TEAM_B", 0);
        long durationMs = getIntent().getLongExtra("DURATION_MS", 0);
        int numSets = getIntent().getIntExtra("NUM_SETS", 3);

        // Set views
        TextView tvWinnerTeam = findViewById(R.id.tvWinnerTeam);
        TextView tvTeamAName = findViewById(R.id.tvTeamAName);
        TextView tvTeamBName = findViewById(R.id.tvTeamBName);
        TextView tvFinalScore = findViewById(R.id.tvFinalScore);
        TextView tvMatchDuration = findViewById(R.id.tvMatchDuration);
        TextView tvNumSets = findViewById(R.id.tvNumSets);
        Button btnViewStats = findViewById(R.id.btnViewStats);
        Button btnBackToMenu = findViewById(R.id.btnBackToMenu);

        // Display match result
        tvWinnerTeam.setText(winnerTeam);
        tvTeamAName.setText(teamAName);
        tvTeamBName.setText(teamBName);
        tvFinalScore.setText(setsTeamA + " : " + setsTeamB);
        
        // Format duration
        long minutes = durationMs / 60000;
        long seconds = (durationMs % 60000) / 1000;
        tvMatchDuration.setText(minutes + " Min " + seconds + " Sek");
        
        // Format number of sets
        String setsText = "Best of " + numSets;
        tvNumSets.setText(setsText);

        // Button: View Statistics
        btnViewStats.setOnClickListener(v -> {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
            finish();
        });

        // Button: Back to Main Menu
        btnBackToMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to score screen after match ends
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
