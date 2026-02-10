package com.rfidresearchgroup.activities.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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
import com.rfidresearchgroup.util.Commons;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PadelScoreActivity extends BaseActivity {

    private static final long SCAN_TIMEOUT_MS = 5000; // 5 Sekunden
    
    private int numTeams;
    private boolean goldenPoint;
    private PadelTagAssignmentActivity.TeamData[] teams;
    private SpclMf spclMf;
    
    // Timeout tracking für doppelte Scans
    private final Map<String, Long> lastScanTimes = new ConcurrentHashMap<>();

    // Score tracking
    private int[] points = new int[2];      // 0, 15, 30, 40, or special values for deuce/advantage
    private int[] games = new int[2];
    private int[] sets = new int[2];
    private volatile boolean isTiebreak = false;
    private volatile boolean matchEnded = false;
    private int advantage = -1;             // -1: no advantage, 0: team 0, 1: team 1

    // UI Elements
    private TextView tvTeamAName, tvTeamBName;
    private TextView tvTeamAScore, tvTeamBScore;
    private TextView tvTeamAGames, tvTeamBGames;
    private TextView tvTeamASets, tvTeamBSets;
    private TextView tvSetInfo, tvGameMode, tvGameTime;
    private Button btnPause;
    private TextView btnSettings, btnUndo;
    private Button btnPlusTeamA, btnPlusTeamB;

    // NFC Scanning
    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isScanning = false;
    private ExecutorService nfcExecutor = Executors.newSingleThreadExecutor();
    
    // Match tracking
    private long matchStartTime;
    private int numSets;
    private Executor dbExecutor = Executors.newSingleThreadExecutor();
    
    // Game Timer
    private long elapsedTimeMs = 0L;
    private long lastPauseTime = 0L;
    private volatile boolean isPaused = false;
    private volatile boolean timerStarted = false;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPaused && !matchEnded && timerStarted) {
                elapsedTimeMs += 1000;
                updateTimerDisplay();
                handler.postDelayed(this, 1000);
            }
        }
    };
    
    // Undo functionality
    private List<ScoreState> scoreHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 10;
    
    // Font size
    private static final String PREF_FONT_SIZE = "padel_score_font_size";
    private static final int DEFAULT_FONT_SIZE = 48;
    private static final int MIN_FONT_SIZE = 10;
    private static final int MAX_FONT_SIZE = 100;
    private int currentFontSize = DEFAULT_FONT_SIZE;

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
        tvGameTime = findViewById(R.id.tvGameTime);
        btnPause = findViewById(R.id.btnPause);
        btnSettings = findViewById(R.id.btnSettings);
        btnUndo = findViewById(R.id.btnUndo);
        btnPlusTeamA = findViewById(R.id.btnPlusTeamA);
        btnPlusTeamB = findViewById(R.id.btnPlusTeamB);

        // Set team names
        tvTeamAName.setText(teams[0].name);
        tvTeamBName.setText(teams[1].name);

        // Pause button (toggle scanning and timer)
        btnPause.setOnClickListener(v -> {
            if (isPaused) {
                // Resume
                isPaused = false;
                isScanning = true;
                btnPause.setText("⏸");
                startNfcScanning();
                handler.post(timerRunnable);
                Toast.makeText(this, "Match fortgesetzt", Toast.LENGTH_SHORT).show();
            } else {
                // Pause
                isPaused = true;
                isScanning = false;
                btnPause.setText("▶");
                Toast.makeText(this, "Match pausiert", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Font size button (Aa)
        btnSettings.setOnClickListener(v -> showFontSizeDialog());
        
        // Undo button
        btnUndo.setOnClickListener(v -> performUndo());
        btnUndo.setVisibility(View.GONE); // Initially hidden
        
        // Plus buttons for manual point addition
        btnPlusTeamA.setOnClickListener(v -> {
            saveScoreState();
            addPoint(0);
        });
        
        btnPlusTeamB.setOnClickListener(v -> {
            saveScoreState();
            addPoint(1);
        });
        
        // Load and apply saved font size
        loadFontSize();
        
        // Start game timer
        timerStarted = true;
        handler.post(timerRunnable);
        updateTimerDisplay();

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

            // Use thread pool instead of creating new threads
            nfcExecutor.submit(() -> {
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
                                        runOnUiThread(() -> {
                                            saveScoreState();
                                            addPoint(teamIndex);
                                        });
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Silent catch - scanning continues
                }
            });
            
            // Continue scanning after delay
            if (isScanning && !matchEnded) {
                handler.postDelayed(scanRunnable, 500);
            }
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
        timerStarted = false;
        isPaused = true;
        
        // Clear undo history on match end
        scoreHistory.clear();
        updateUndoButtonVisibility();
        
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
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("Match beenden?")
            .setMessage("Möchten Sie das laufende Match wirklich beenden? Der aktuelle Spielstand geht verloren.")
            .setPositiveButton("Ja, beenden", (dialog, which) -> {
                isScanning = false;
                matchEnded = true;
                timerStarted = false;
                Intent intent = new Intent(this, MainMenuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void updateTimerDisplay() {
        long totalSeconds = elapsedTimeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String timeString = String.format("%02d:%02d", minutes, seconds);
        if (tvGameTime != null) {
            tvGameTime.setText(timeString);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isScanning = false;
        matchEnded = true;
        timerStarted = false;
        handler.removeCallbacks(scanRunnable);
        handler.removeCallbacks(timerRunnable);
        
        // Shutdown NFC executor thread pool
        nfcExecutor.shutdown();
        try {
            if (!nfcExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                nfcExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            nfcExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // ScoreState snapshot class for undo functionality
    private static class ScoreState {
        int[] points;
        int[] games;
        int[] sets;
        boolean isTiebreak;
        int advantage;
        long timestamp;
        
        ScoreState(int[] points, int[] games, int[] sets, boolean isTiebreak, int advantage) {
            this.points = points.clone();
            this.games = games.clone();
            this.sets = sets.clone();
            this.isTiebreak = isTiebreak;
            this.advantage = advantage;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private void saveScoreState() {
        ScoreState state = new ScoreState(points, games, sets, isTiebreak, advantage);
        scoreHistory.add(state);
        
        // Limit history size
        if (scoreHistory.size() > MAX_HISTORY_SIZE) {
            scoreHistory.remove(0);
        }
        
        updateUndoButtonVisibility();
    }
    
    private void restoreScoreState(ScoreState state) {
        points = state.points.clone();
        games = state.games.clone();
        sets = state.sets.clone();
        isTiebreak = state.isTiebreak;
        advantage = state.advantage;
        updateScoreDisplay();
    }
    
    private void performUndo() {
        if (scoreHistory.isEmpty()) {
            Toast.makeText(this, "Keine Änderungen zum Rückgängigmachen", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ScoreState lastState = scoreHistory.remove(scoreHistory.size() - 1);
        restoreScoreState(lastState);
        updateUndoButtonVisibility();
        Toast.makeText(this, "Änderung rückgängig gemacht", Toast.LENGTH_SHORT).show();
    }
    
    private void updateUndoButtonVisibility() {
        if (btnUndo != null) {
            btnUndo.setVisibility(scoreHistory.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
    
    private void showFontSizeDialog() {
        View dialogView = getLayoutInflater().inflate(android.R.layout.select_dialog_item, null);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        
        TextView label = new TextView(this);
        label.setText("Schriftgröße: " + currentFontSize + "sp");
        label.setTextSize(16);
        label.setPadding(0, 0, 0, 20);
        layout.addView(label);
        
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(MAX_FONT_SIZE - MIN_FONT_SIZE);
        seekBar.setProgress(currentFontSize - MIN_FONT_SIZE);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = MIN_FONT_SIZE + progress;
                label.setText("Schriftgröße: " + size + "sp");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(seekBar);
        
        new AlertDialog.Builder(this)
            .setTitle("Schriftgröße anpassen")
            .setView(layout)
            .setPositiveButton("OK", (dialog, which) -> {
                int newSize = MIN_FONT_SIZE + seekBar.getProgress();
                currentFontSize = newSize;
                applyFontSize(newSize);
                saveFontSize(newSize);
                Toast.makeText(this, "Schriftgröße gespeichert", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void applyFontSize(int size) {
        // Main score numbers
        tvTeamAScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        tvTeamBScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        tvTeamAGames.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        tvTeamBGames.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        tvTeamASets.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        tvTeamBSets.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        
        // Team names (50% of main size)
        float nameSize = size * 0.5f;
        tvTeamAName.setTextSize(TypedValue.COMPLEX_UNIT_SP, nameSize);
        tvTeamBName.setTextSize(TypedValue.COMPLEX_UNIT_SP, nameSize);
    }
    
    private void loadFontSize() {
        SharedPreferences prefs = Commons.getPrivatePreferences();
        currentFontSize = prefs.getInt(PREF_FONT_SIZE, DEFAULT_FONT_SIZE);
        applyFontSize(currentFontSize);
    }
    
    private void saveFontSize(int size) {
        SharedPreferences prefs = Commons.getPrivatePreferences();
        prefs.edit().putInt(PREF_FONT_SIZE, size).apply();
    }
}
