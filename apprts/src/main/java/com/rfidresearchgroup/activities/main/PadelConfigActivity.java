package com.rfidresearchgroup.activities.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;

import com.rfidresearchgroup.rfidtools.R;

public class PadelConfigActivity extends BaseActivity {

    private RadioGroup rgTeams;
    private RadioGroup rgSets;
    private SwitchCompat switchGoldenPoint;
    private EditText edtTeamA;
    private EditText edtTeamB;
    private TextView tvSetsInfo;
    private Button btnStartGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_padel_config);

        initViews();
        initActions();
    }

    private void initViews() {
        rgTeams = findViewById(R.id.rgTeams);
        rgSets = findViewById(R.id.rgSets);
        switchGoldenPoint = findViewById(R.id.switchGoldenPoint);
        edtTeamA = findViewById(R.id.edtTeamA);
        edtTeamB = findViewById(R.id.edtTeamB);
        tvSetsInfo = findViewById(R.id.tvSetsInfo);
        btnStartGame = findViewById(R.id.btnStartGame);
    }

    private void initActions() {
        // Update sets info text
        rgSets.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSets1) {
                tvSetsInfo.setText("Best of 1");
            } else if (checkedId == R.id.rbSets3) {
                tvSetsInfo.setText("Best of 3");
            } else if (checkedId == R.id.rbSets5) {
                tvSetsInfo.setText("Best of 5");
            }
        });
        
        btnStartGame.setOnClickListener(v -> {
            int selectedTeamsId = rgTeams.getCheckedRadioButtonId();
            int selectedSetsId = rgSets.getCheckedRadioButtonId();

            if (selectedTeamsId == -1 || selectedSetsId == -1) {
                Toast.makeText(this, "Bitte alle Optionen auswählen", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine number of teams
            int numTeams = 2; // Default
            if (selectedTeamsId == R.id.rbTeams2) {
                numTeams = 2;
            } else if (selectedTeamsId == R.id.rbTeams4) {
                numTeams = 4;
            }

            // Determine golden point mode
            boolean goldenPoint = switchGoldenPoint.isChecked();
            
            // Determine number of sets
            int numSets = 3; // Default
            if (selectedSetsId == R.id.rbSets1) {
                numSets = 1;
            } else if (selectedSetsId == R.id.rbSets3) {
                numSets = 3;
            } else if (selectedSetsId == R.id.rbSets5) {
                numSets = 5;
            }
            
            // Get team names
            String teamAName = edtTeamA.getText().toString().trim();
            String teamBName = edtTeamB.getText().toString().trim();
            if (teamAName.isEmpty()) teamAName = "Team A";
            if (teamBName.isEmpty()) teamBName = "Team B";

            // Start tag assignment activity
            Intent intent = new Intent(this, PadelTagAssignmentActivity.class);
            intent.putExtra("NUM_TEAMS", numTeams);
            intent.putExtra("GOLDEN_POINT", goldenPoint);
            intent.putExtra("NUM_SETS", numSets);
            intent.putExtra("TEAM_A_NAME", teamAName);
            intent.putExtra("TEAM_B_NAME", teamBName);
            startActivity(intent);
        });
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
