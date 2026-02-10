package com.rfidresearchgroup.activities.statistics;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rfidresearchgroup.activities.main.BaseActivity;
import com.rfidresearchgroup.database.AppDatabase;
import com.rfidresearchgroup.database.entity.MatchEntity;
import com.rfidresearchgroup.database.entity.PlayerEntity;
import com.rfidresearchgroup.rfidtools.R;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StatisticsActivity extends BaseActivity {

    private Button btnTabMatches, btnTabPlayers, btnBack;
    private RecyclerView recyclerViewMatches, recyclerViewPlayers;
    private TextView tvEmptyState;
    
    private MatchListAdapter matchAdapter;
    private PlayerListAdapter playerAdapter;
    
    private Executor dbExecutor = Executors.newSingleThreadExecutor();
    
    private boolean showingMatches = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_statistics);

        initViews();
        setupRecyclerViews();
        loadMatches();
    }

    private void initViews() {
        btnTabMatches = findViewById(R.id.btnTabMatches);
        btnTabPlayers = findViewById(R.id.btnTabPlayers);
        btnBack = findViewById(R.id.btnBack);
        recyclerViewMatches = findViewById(R.id.recyclerViewMatches);
        recyclerViewPlayers = findViewById(R.id.recyclerViewPlayers);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        btnTabMatches.setOnClickListener(v -> switchToMatches());
        btnTabPlayers.setOnClickListener(v -> switchToPlayers());
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        recyclerViewMatches.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPlayers.setLayoutManager(new LinearLayoutManager(this));
        
        matchAdapter = new MatchListAdapter(this);
        playerAdapter = new PlayerListAdapter(this);
        
        recyclerViewMatches.setAdapter(matchAdapter);
        recyclerViewPlayers.setAdapter(playerAdapter);
    }

    private void switchToMatches() {
        showingMatches = true;
        btnTabMatches.setBackgroundResource(R.drawable.btn_rounded_teal);
        btnTabPlayers.setBackgroundResource(R.drawable.btn_rounded_grey);
        recyclerViewMatches.setVisibility(View.VISIBLE);
        recyclerViewPlayers.setVisibility(View.GONE);
        loadMatches();
    }

    private void switchToPlayers() {
        showingMatches = false;
        btnTabMatches.setBackgroundResource(R.drawable.btn_rounded_grey);
        btnTabPlayers.setBackgroundResource(R.drawable.btn_rounded_teal);
        recyclerViewMatches.setVisibility(View.GONE);
        recyclerViewPlayers.setVisibility(View.VISIBLE);
        loadPlayers();
    }

    private void loadMatches() {
        dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                List<MatchEntity> matches = db.matchDao().getAllMatches();
                
                runOnUiThread(() -> {
                    if (matches.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerViewMatches.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerViewMatches.setVisibility(View.VISIBLE);
                        matchAdapter.setMatches(matches);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvEmptyState.setText("Fehler beim Laden: " + e.getMessage());
                    tvEmptyState.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void loadPlayers() {
        dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                List<PlayerEntity> players = db.playerDao().getAllPlayers();
                
                runOnUiThread(() -> {
                    if (players.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerViewPlayers.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerViewPlayers.setVisibility(View.VISIBLE);
                        playerAdapter.setPlayers(players);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvEmptyState.setText("Fehler beim Laden: " + e.getMessage());
                    tvEmptyState.setVisibility(View.VISIBLE);
                });
            }
        });
    }
}
