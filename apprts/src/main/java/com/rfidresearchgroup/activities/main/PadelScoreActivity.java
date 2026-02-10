package com.rfidresearchgroup.activities.main;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rfidresearchgroup.rfidtools.R;
import com.rfidresearchgroup.natives.SpclMf;
import com.rfidresearchgroup.common.util.HexUtil;
import com.rfidresearchgroup.mifare.MifareClassicUtils;
import com.rfidresearchgroup.database.AppDatabase;
import com.rfidresearchgroup.database.entity.MatchEntity;
import com.rfidresearchgroup.database.entity.PlayerEntity;
import com.rfidresearchgroup.database.entity.MatchPlayerEntity;
import com.rfidresearchgroup.repository.MatchRepository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PadelScoreActivity extends BaseActivity {

    private static final long SCAN_TIMEOUT_MS = 5000; // 5 Sekunden
    
    private int numTeams;
    private boolean goldenPoint;
    private PadelTagAssignmentActivity.TeamData[] teams;
    private SpclMf spclMf;
    
    // Timeout tracking für doppelte Scans
    private Map<String, Long> lastScanTimes = new HashMap<>();

    // Score tracking
    private int[] points = new int[2];      // 0, 15, 30, 40, or special values for deuce/advantage
    private int[] games = new int[2];
    private int[] sets = new int[2];
    private boolean isTiebreak = false;
    private boolean matchEnded = false;
    private int advantage = -1;             // -1: no advantage, 0: team 0, 1: team 1

    // UI Elements
    private TextView tvTeamAName, tvTeamBName;
    private TextView tvTeamAScore, tvTeamBScore;
    private TextView tvTeamAGames, tvTeamBGames;
    private TextView tvTeamASets, tvTeamBSets;
    private TextView tvSetInfo, tvGameMode;
    private Button btnPause;

    // NFC Scanning
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;
    
    // Match tracking
    private long matchStartTime;
    private int numSets;
    private Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_padel_score);
        
        // Fullscreen-Modus aktivieren
        hideSystemUI();

        numTeams = getIntent().getIntExtra("NUM_TEAMS", 2);
        goldenPoint = getIntent().getBooleanExtra("GOLDEN_POINT", true);
        numSets = getIntent().getIntExtra("NUM_SETS", 3);
        teams = (PadelTagAssignmentActivity.TeamData[]) getIntent().getSerializableExtra("TEAMS");
        
        matchStartTime = System.currentTimeMillis();
        spclMf = SpclMf.get();

        initViews();
        startNfcScanning();
    }

    private void initViews() {
        tvTeamAName = findViewById(R.id.tvTeamAName);
        tvTeamBName = findViewById(R.id.tvTeamBName);
        tvTeamAScore = findViewById(R.id.tvTeamAScore);
        tvTeamBScore = findViewById(R.id.tvTeamBScore);
        tvTeamAGames = findViewById(R.id.tvTeamAGames);
        tvTeamBGames = findViewById(R.id.tvTeamBGames);
        tvTeamASets = findViewById(R.id.tvTeamASets);
        tvTeamBSets = findViewById(R.id.tvTeamBSets);
        tvSetInfo = findViewById(R.id.tvSetInfo);
        tvGameMode = findViewById(R.id.tvGameMode);
        btnPause = findViewById(R.id.btnPause);

        // Set team names
        tvTeamAName.setText(teams[0].name);
        tvTeamBName.setText(teams[1].name);

        // Pause button (toggle scanning)
        btnPause.setOnClickListener(v -> {
            if (isScanning) {
                isScanning = false;
                btnPause.setText("▶");
                Toast.makeText(this, "Scanning pausiert", Toast.LENGTH_SHORT).show();
            } else {
                isScanning = true;
                btnPause.setText("⏸");
                startNfcScanning();
                Toast.makeText(this, "Scanning fortgesetzt", Toast.LENGTH_SHORT).show();
            }
        });

        updateScoreDisplay();
        updateMatchStatus();
    }

    private void startNfcScanning() {
        isScanning = true;
        handler.post(scanRunnable);
    }

    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isScanning || matchEnded) return;

            new Thread(() -> {
                try {
                    if (spclMf.scanning()) {
                        if (spclMf.connect()) {
                            String uuid = readUuidFromTag();
                            if (uuid != null && !uuid.isEmpty()) {
                                int teamIndex = findTeamByUuid(uuid);
                                if (teamIndex != -1) {
                                    // Timeout-Überprüfung
                                    long currentTime = System.currentTimeMillis();
                                    Long lastScanTime = lastScanTimes.get(uuid);
                                    
                                    if (lastScanTime != null && (currentTime - lastScanTime) < SCAN_TIMEOUT_MS) {
                                        // Innerhalb des Timeouts - Punkt wird nicht gezählt
                                        runOnUiThread(() -> 
                                            Toast.makeText(PadelScoreActivity.this, 
                                                "⏱️ Zu schnell! Warte " + ((SCAN_TIMEOUT_MS - (currentTime - lastScanTime)) / 1000) + " Sekunden", 
                                                Toast.LENGTH_SHORT).show()
                                        );
                                    } else {
                                        // Außerhalb des Timeouts - Punkt wird gezählt
                                        lastScanTimes.put(uuid, currentTime);
                                        runOnUiThread(() -> addPoint(teamIndex));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Silent catch - scanning continues
                }
                
                // Continue scanning after delay
                if (isScanning && !matchEnded) {
                    handler.postDelayed(scanRunnable, 500);
                }
            }).start();
        }
    };

    private String readUuidFromTag() {
        try {
            byte[] defaultKey = HexUtil.hexStringToByteArray("FFFFFFFFFFFF");
            int sector = MifareClassicUtils.blockToSector(4);
            
            if (!spclMf.authA(sector, defaultKey)) {
                return null;
            }
            
            byte[] uuidData1 = spclMf.read(5);
            byte[] uuidData2 = spclMf.read(6);
            
            if (uuidData1 == null || uuidData2 == null) {
                return null;
            }
            
            byte[] uuidBytes = new byte[32];
            System.arraycopy(uuidData1, 0, uuidBytes, 0, 16);
            System.arraycopy(uuidData2, 0, uuidBytes, 16, 16);
            
            String uuid = new String(uuidBytes, StandardCharsets.UTF_8).trim();
            return uuid.replaceAll("\\x00", "");
        } catch (Exception e) {
            return null;
        }
    }

    private int findTeamByUuid(String uuid) {
        for (int i = 0; i < teams.length; i++) {
            for (PadelTagAssignmentActivity.PlayerData player : teams[i].players) {
                if (player.uuid.equals(uuid)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void addPoint(int teamIndex) {
        if (matchEnded) return;

        String playerName = getPlayerName(teamIndex);
        Toast.makeText(this, "Punkt für " + teams[teamIndex].name + " (" + playerName + ")", Toast.LENGTH_SHORT).show();

        if (isTiebreak) {
            addTiebreakPoint(teamIndex);
        } else {
            addRegularPoint(teamIndex);
        }

        updateScoreDisplay();
    }

    private String getPlayerName(int teamIndex) {
        if (teams[teamIndex].players.size() > 0) {
            return teams[teamIndex].players.get(0).name;
        }
        return "";
    }

    private void addRegularPoint(int teamIndex) {
        int opponent = 1 - teamIndex;

        // Check for 40:40 (Deuce)
        if (points[0] == 40 && points[1] == 40) {
            if (goldenPoint) {
                // Golden Point: next point wins the game
                winGame(teamIndex);
                return;
            } else {
                // Advantage mode
                if (advantage == -1) {
                    advantage = teamIndex;
                } else if (advantage == teamIndex) {
                    winGame(teamIndex);
                } else {
                    advantage = -1;
                }
                return;
            }
        }

        // Normal point progression
        if (points[teamIndex] == 0) {
            points[teamIndex] = 15;
        } else if (points[teamIndex] == 15) {
            points[teamIndex] = 30;
        } else if (points[teamIndex] == 30) {
            points[teamIndex] = 40;
        } else if (points[teamIndex] == 40) {
            winGame(teamIndex);
        }
    }

    private void addTiebreakPoint(int teamIndex) {
        points[teamIndex]++;
        
        int opponent = 1 - teamIndex;
        
        // Tiebreak: first to 7 with 2-point lead
        if (points[teamIndex] >= 7 && points[teamIndex] - points[opponent] >= 2) {
            winGame(teamIndex);
        }
    }

    private void winGame(int teamIndex) {
        games[teamIndex]++;
        points[0] = 0;
        points[1] = 0;
        advantage = -1;
        
        int opponent = 1 - teamIndex;
        
        // Check for set win
        if (games[teamIndex] >= 6) {
            if (games[teamIndex] - games[opponent] >= 2) {
                winSet(teamIndex);
                return;
            } else if (games[teamIndex] == 6 && games[opponent] == 6) {
                // Tiebreak
                isTiebreak = true;
                Toast.makeText(this, "TIEBREAK!", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        Toast.makeText(this, teams[teamIndex].name + " gewinnt Game!", Toast.LENGTH_SHORT).show();
    }

    private void winSet(int teamIndex) {
        sets[teamIndex]++;
        games[0] = 0;
        games[1] = 0;
        isTiebreak = false;
        
        // Check for match win (Best of 3)
        if (sets[teamIndex] >= 2) {
            winMatch(teamIndex);
            return;
        }
        
        Toast.makeText(this, teams[teamIndex].name + " gewinnt Satz!", Toast.LENGTH_SHORT).show();
    }

    private void winMatch(int teamIndex) {
        matchEnded = true;
        isScanning = false;
        
        Toast.makeText(this, "🏆 " + teams[teamIndex].name + " GEWINNT DAS MATCH! 🏆", Toast.LENGTH_LONG).show();
        
        // Save match to database in background
        saveMatchToDatabase(teamIndex);
        
        // Navigate to match result screen after short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(this, MatchResultActivity.class);
            intent.putExtra("WINNER_TEAM", teams[teamIndex].name);
            intent.putExtra("TEAM_A_NAME", teams[0].name);
            intent.putExtra("TEAM_B_NAME", teams[1].name);
            intent.putExtra("SETS_TEAM_A", sets[0]);
            intent.putExtra("SETS_TEAM_B", sets[1]);
            intent.putExtra("DURATION_MS", System.currentTimeMillis() - matchStartTime);
            intent.putExtra("NUM_SETS", numSets);
            startActivity(intent);
            finish();
        }, 2000); // 2 seconds delay to show toast
    }
    
    private void saveMatchToDatabase(int winnerTeamIndex) {
        dbExecutor.execute(() -> {
            try {
                MatchRepository repository = new MatchRepository(this);
                long matchEndTime = System.currentTimeMillis();
                
                // Create match entity
                MatchEntity match = new MatchEntity();
                match.timestamp = matchStartTime;
                match.teamAName = teams[0].name;
                match.teamBName = teams[1].name;
                match.winnerTeamIndex = winnerTeamIndex;
                match.setsTeamA = sets[0];
                match.setsTeamB = sets[1];
                match.gamesData = "[]"; // TODO: Track games per set if needed
                match.durationMs = matchEndTime - matchStartTime;
                match.goldenPointUsed = goldenPoint;
                match.numSets = numSets;
                match.totalPoints = calculateTotalPoints();
                
                // Prepare all players and match-player relationships
                List<PlayerEntity> playersToSave = new ArrayList<>();
                List<MatchPlayerEntity> matchPlayers = new ArrayList<>();
                
                for (int teamIdx = 0; teamIdx < teams.length; teamIdx++) {
                    for (PadelTagAssignmentActivity.PlayerData player : teams[teamIdx].players) {
                        // Get or create player entity
                        PlayerEntity playerEntity = repository.getPlayerByUuid(player.uuid);
                        
                        if (playerEntity == null) {
                            // New player - create entry
                            playerEntity = new PlayerEntity();
                            playerEntity.uuid = player.uuid;
                            playerEntity.name = player.name;
                            playerEntity.matchesPlayed = 0;
                            playerEntity.matchesWon = 0;
                            playerEntity.totalPoints = 0;
                            playerEntity.totalSetsWon = 0;
                            playerEntity.totalGamesWon = 0;
                            playerEntity.lastPlayed = 0;
                            repository.insertPlayer(playerEntity);
                        }
                        
                        // Update player stats
                        int setsWon = (teamIdx == winnerTeamIndex) ? sets[winnerTeamIndex] : sets[teamIdx];
                        int gamesWon = games[teamIdx];
                        
                        repository.updatePlayerStats(
                            player.uuid,
                            match.totalPoints / teams[teamIdx].players.size(),
                            setsWon,
                            gamesWon,
                            matchEndTime
                        );
                        
                        // Increment wins if this player's team won
                        if (teamIdx == winnerTeamIndex) {
                            repository.incrementPlayerWins(player.uuid);
                        }
                        
                        // Get updated player entity
                        PlayerEntity updatedPlayer = repository.getPlayerByUuid(player.uuid);
                        playersToSave.add(updatedPlayer);
                        
                        // Create match-player relationship
                        MatchPlayerEntity matchPlayer = new MatchPlayerEntity();
                        matchPlayer.playerUuid = player.uuid;
                        matchPlayer.teamIndex = teamIdx;
                        matchPlayer.pointsScored = match.totalPoints / teams[teamIdx].players.size();
                        matchPlayer.wasWinner = (teamIdx == winnerTeamIndex);
                        matchPlayers.add(matchPlayer);
                    }
                }
                
                // Save to local + sync to Supabase (fire-and-forget)
                long matchId = repository.saveMatch(match, playersToSave, matchPlayers);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Match gespeichert! (ID: " + matchId + ")", Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Fehler beim Speichern: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private int calculateTotalPoints() {
        // Rough estimate: each game has ~4-6 points, multiply by total games played
        int totalGames = games[0] + games[1];
        return totalGames * 5; // Average estimate
    }

    private void resetMatch() {
        points[0] = points[1] = 0;
        games[0] = games[1] = 0;
        sets[0] = sets[1] = 0;
        advantage = -1;
        isTiebreak = false;
        matchEnded = false;
        updateScoreDisplay();
        startNfcScanning();
    }
    
    private void updateMatchStatus() {
        // Update set info
        int currentSet = sets[0] + sets[1] + 1;
        tvSetInfo.setText("Set " + currentSet);
        
        // Update game mode
        if (matchEnded) {
            tvGameMode.setText("Match beendet");
        } else if (isTiebreak) {
            tvGameMode.setText("TIEBREAK");
        } else if (points[0] == 40 && points[1] == 40) {
            if (goldenPoint) {
                tvGameMode.setText("Golden Point");
            } else if (advantage != -1) {
                tvGameMode.setText("Vorteil " + teams[advantage].name);
            } else {
                tvGameMode.setText("Einstand");
            }
        } else {
            tvGameMode.setText("");
        }
    }

    private void updateScoreDisplay() {
        // Points
        if (isTiebreak) {
            tvTeamAScore.setText(String.valueOf(points[0]));
            tvTeamBScore.setText(String.valueOf(points[1]));
        } else {
            tvTeamAScore.setText(getScoreText(0));
            tvTeamBScore.setText(getScoreText(1));
        }
        
        // Games
        tvTeamAGames.setText(String.valueOf(games[0]));
        tvTeamBGames.setText(String.valueOf(games[1]));
        
        // Sets
        tvTeamASets.setText(String.valueOf(sets[0]));
        tvTeamBSets.setText(String.valueOf(sets[1]));
        
        updateMatchStatus();
    }

    private String getScoreText(int teamIndex) {
        if (points[0] == 40 && points[1] == 40) {
            if (!goldenPoint && advantage != -1) {
                return advantage == teamIndex ? "AD" : "40";
            }
            return "40";
        }
        return String.valueOf(points[teamIndex]);
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Android 10 und älter
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isScanning = false;
        handler.removeCallbacks(scanRunnable);
    }
}
