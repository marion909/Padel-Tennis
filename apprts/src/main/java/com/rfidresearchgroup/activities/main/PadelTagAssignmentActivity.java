package com.rfidresearchgroup.activities.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rfidresearchgroup.rfidtools.R;
import com.rfidresearchgroup.natives.SpclMf;
import com.rfidresearchgroup.common.util.HexUtil;
import com.rfidresearchgroup.mifare.MifareClassicUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PadelTagAssignmentActivity extends BaseActivity {

    private int numTeams;
    private boolean goldenPoint;
    private int numSets;
    private String teamAName;
    private String teamBName;
    private SpclMf spclMf;
    
    private List<TeamData> teams = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private int totalPlayers = 0;
    
    // UI Elements
    private ProgressBar progressBar;
    private TextView tvProgress;
    private TextView tvTeamBadge;
    private TextView tvPlayerName;
    private Button btnScan;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_padel_tag_assignment);

        numTeams = getIntent().getIntExtra("NUM_TEAMS", 2);
        goldenPoint = getIntent().getBooleanExtra("GOLDEN_POINT", true);
        numSets = getIntent().getIntExtra("NUM_SETS", 3);
        teamAName = getIntent().getStringExtra("TEAM_A_NAME");
        teamBName = getIntent().getStringExtra("TEAM_B_NAME");
        
        if (teamAName == null) teamAName = "Team A";
        if (teamBName == null) teamBName = "Team B";
        
        spclMf = SpclMf.get();

        initViews();
        setupTeams();
        showCurrentPlayer();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        tvProgress = findViewById(R.id.tvProgress);
        tvTeamBadge = findViewById(R.id.tvTeamBadge);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        btnScan = findViewById(R.id.btnScan);
        tvStatus = findViewById(R.id.tvStatus);
        
        btnScan.setOnClickListener(v -> scanCurrentPlayer());
    }

    private void setupTeams() {
        teams.clear();
        
        // Create 2 teams
        TeamData teamA = new TeamData(teamAName);
        TeamData teamB = new TeamData(teamBName);
        teams.add(teamA);
        teams.add(teamB);
        
        // Calculate total players
        int playersPerTeam = (numTeams == 2) ? 1 : 2;
        totalPlayers = playersPerTeam * 2; // 2 teams
        
        currentPlayerIndex = 0;
    }

    private void showCurrentPlayer() {
        if (currentPlayerIndex >= totalPlayers) {
            startMatch();
            return;
        }
        
        // Determine which team and player number
        int playersPerTeam = totalPlayers / 2;
        int teamIndex = currentPlayerIndex / playersPerTeam;
        int playerNum = (currentPlayerIndex % playersPerTeam) + 1;
        
        TeamData currentTeam = teams.get(teamIndex);
        
        // Update UI
        tvProgress.setText("Player " + (currentPlayerIndex + 1) + " of " + totalPlayers);
        progressBar.setMax(totalPlayers);
        progressBar.setProgress(currentPlayerIndex + 1);
        
        tvTeamBadge.setText(currentTeam.name);
        tvTeamBadge.setBackgroundResource(teamIndex == 0 ? R.drawable.badge_team_a : R.drawable.badge_team_b);
        
        String playerLabel = currentTeam.name.replace("Team ", "Player ") + playerNum;
        tvPlayerName.setText(playerLabel);
        
        tvStatus.setVisibility(android.view.View.GONE);
        btnScan.setEnabled(true);
    }

    private void scanCurrentPlayer() {
        btnScan.setEnabled(false);
        tvStatus.setVisibility(android.view.View.VISIBLE);
        tvStatus.setText("Scanning...");
        
        new Thread(() -> {
            try {
                // Scan for tag
                if (!spclMf.scanning()) {
                    runOnUiThread(() -> {
                        tvStatus.setText("❌ No tag found!");
                        tvStatus.setTextColor(0xFFFF6B6B);
                        btnScan.setEnabled(true);
                    });
                    return;
                }
                
                // Connect to tag
                if (!spclMf.connect()) {
                    runOnUiThread(() -> {
                        tvStatus.setText("❌ Connection failed!");
                        tvStatus.setTextColor(0xFFFF6B6B);
                        btnScan.setEnabled(true);
                    });
                    return;
                }
                
                // Default MIFARE key
                byte[] defaultKey = HexUtil.hexStringToByteArray("FFFFFFFFFFFF");
                
                // Authenticate sector 1 (Blocks 4-7) with key A
                int sector = MifareClassicUtils.blockToSector(4);
                if (!spclMf.authA(sector, defaultKey)) {
                    runOnUiThread(() -> {
                        tvStatus.setText("❌ Authentication failed!");
                        tvStatus.setTextColor(0xFFFF6B6B);
                        btnScan.setEnabled(true);
                    });
                    return;
                }
                
                // Read name from block 4
                byte[] nameData = spclMf.read(4);
                if (nameData == null || nameData.length < 16) {
                    runOnUiThread(() -> {
                        tvStatus.setText("❌ Read failed!");
                        tvStatus.setTextColor(0xFFFF6B6B);
                        btnScan.setEnabled(true);
                    });
                    return;
                }
                
                // Read UUID part 1 from block 5
                byte[] uuidData1 = spclMf.read(5);
                if (uuidData1 == null || uuidData1.length < 16) {
                    runOnUiThread(() -> {
                        tvStatus.setText("❌ UUID read failed!");
                        tvStatus.setTextColor(0xFFFF6B6B);
                        btnScan.setEnabled(true);
                    });
                    return;
                }
                
                // Read UUID part 2 from block 6
                byte[] uuidData2 = spclMf.read(6);
                if (uuidData2 == null || uuidData2.length < 16) {
                    runOnUiThread(() -> {
                        tvStatus.setText("❌ UUID read failed!");
                        tvStatus.setTextColor(0xFFFF6B6B);
                        btnScan.setEnabled(true);
                    });
                    return;
                }
                
                // Convert bytes to strings
                String name = new String(nameData, 0, 16, StandardCharsets.UTF_8).trim();
                name = name.replaceAll("\\x00", "");
                
                byte[] uuidBytes = new byte[32];
                System.arraycopy(uuidData1, 0, uuidBytes, 0, 16);
                System.arraycopy(uuidData2, 0, uuidBytes, 16, 16);
                String uuid = new String(uuidBytes, StandardCharsets.UTF_8).trim();
                uuid = uuid.replaceAll("\\x00", "");
                
                if (name.isEmpty()) {
                    runOnUiThread(() -> {
                        tvStatus.setText("❌ No player registered on tag!");
                        tvStatus.setTextColor(0xFFFF6B6B);
                        btnScan.setEnabled(true);
                    });
                    return;
                }
                
                final String finalName = name;
                final String finalUuid = uuid;
                
                // Check if UUID already assigned
                if (isUuidAlreadyAssigned(finalUuid)) {
                    runOnUiThread(() -> {
                        tvStatus.setText("❌ Player already assigned!");
                        tvStatus.setTextColor(0xFFFF6B6B);
                        btnScan.setEnabled(true);
                    });
                    return;
                }
                
                // Assign player
                int playersPerTeam = totalPlayers / 2;
                int teamIndex = currentPlayerIndex / playersPerTeam;
                PlayerData player = new PlayerData(finalName, finalUuid);
                teams.get(teamIndex).addPlayer(player);
                
                runOnUiThread(() -> {
                    tvStatus.setText("✓ " + finalName + " assigned");
                    tvStatus.setTextColor(0xFF4DD0E1);
                    
                    // Move to next player after delay
                    tvStatus.postDelayed(() -> {
                        currentPlayerIndex++;
                        showCurrentPlayer();
                    }, 1500);
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvStatus.setText("❌ Error: " + e.getMessage());
                    tvStatus.setTextColor(0xFFFF6B6B);
                    btnScan.setEnabled(true);
                });
            }
        }).start();
    }

    private void skipCurrentPlayer() {
        currentPlayerIndex++;
        showCurrentPlayer();
    }

    private boolean isUuidAlreadyAssigned(String uuid) {
        for (TeamData team : teams) {
            for (PlayerData player : team.players) {
                if (player != null && player.uuid.equals(uuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startMatch() {
        Intent intent = new Intent(this, PadelScoreActivity.class);
        intent.putExtra("NUM_TEAMS", numTeams);
        intent.putExtra("GOLDEN_POINT", goldenPoint);
        intent.putExtra("NUM_SETS", numSets);
        intent.putExtra("TEAM_A_NAME", teamAName);
        intent.putExtra("TEAM_B_NAME", teamBName);
        intent.putExtra("TEAMS", teams.toArray(new TeamData[0]));
        startActivity(intent);
        finish();
    }

    // Data classes
    public static class TeamData implements java.io.Serializable {
        String name;
        List<PlayerData> players = new ArrayList<>();
        
        public TeamData(String name) {
            this.name = name;
        }
        
        public void addPlayer(PlayerData player) {
            players.add(player);
        }
    }

    public static class PlayerData implements java.io.Serializable {
        String name;
        String uuid;
        
        public PlayerData(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }
}
