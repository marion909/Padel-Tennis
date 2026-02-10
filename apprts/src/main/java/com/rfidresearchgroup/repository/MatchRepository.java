package com.rfidresearchgroup.repository;

import android.content.Context;
import android.util.Log;

import com.rfidresearchgroup.database.AppDatabase;
import com.rfidresearchgroup.database.entity.MatchEntity;
import com.rfidresearchgroup.database.entity.MatchPlayerEntity;
import com.rfidresearchgroup.database.entity.PlayerEntity;
import com.rfidresearchgroup.network.SupabaseClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for managing match data across local (Room) and remote (Supabase) storage
 */
public class MatchRepository {
    private static final String TAG = "MatchRepository";
    
    private final AppDatabase localDb;
    private final SupabaseClient supabaseClient;
    
    public MatchRepository(Context context) {
        this.localDb = AppDatabase.getInstance(context);
        this.supabaseClient = SupabaseClient.getInstance();
    }
    
    /**
     * Save a complete match with all players to both local and remote storage
     * 
     * @param match The match entity to save
     * @param players List of player entities involved in the match
     * @param matchPlayers List of match-player relationships
     * @return The local match ID (from Room database)
     */
    public long saveMatch(MatchEntity match, List<PlayerEntity> players, List<MatchPlayerEntity> matchPlayers) {
        // 1. Save to local database first (always succeeds, even offline)
        long matchId = saveToLocal(match, players, matchPlayers);
        
        // 2. Attempt to sync to Supabase (fire-and-forget, failures only logged)
        syncToSupabase(match, matchId, players, matchPlayers);
        
        return matchId;
    }
    
    /**
     * Save match data to local Room database
     */
    private long saveToLocal(MatchEntity match, List<PlayerEntity> players, List<MatchPlayerEntity> matchPlayers) {
        try {
            // Insert match and get generated ID
            long matchId = localDb.matchDao().insert(match);
            
            // Update match ID in all match-player relationships
            for (MatchPlayerEntity mp : matchPlayers) {
                mp.matchId = matchId;
            }
            
            // Save or update all players
            for (PlayerEntity player : players) {
                PlayerEntity existing = localDb.playerDao().getPlayerByUuid(player.uuid);
                if (existing == null) {
                    localDb.playerDao().insert(player);
                } else {
                    localDb.playerDao().update(player);
                }
            }
            
            // Save all match-player relationships
            localDb.matchPlayerDao().insertAll(matchPlayers);
            
            Log.d(TAG, "Match saved to local database with ID: " + matchId);
            return matchId;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving match to local database", e);
            throw e; // Re-throw since local save is critical
        }
    }
    
    /**
     * Sync match data to Supabase (fire-and-forget)
     */
    private void syncToSupabase(MatchEntity match, long matchId, List<PlayerEntity> players, List<MatchPlayerEntity> matchPlayers) {
        try {
            // Convert MatchEntity to Supabase-compatible map
            Map<String, Object> matchData = new HashMap<>();
            matchData.put("id", matchId);
            matchData.put("timestamp", match.timestamp);
            matchData.put("team_a_name", match.teamAName);
            matchData.put("team_b_name", match.teamBName);
            matchData.put("winner_team_index", match.winnerTeamIndex);
            matchData.put("sets_team_a", match.setsTeamA);
            matchData.put("sets_team_b", match.setsTeamB);
            matchData.put("games_data", match.gamesData);
            matchData.put("duration_ms", match.durationMs);
            matchData.put("golden_point_used", match.goldenPointUsed);
            matchData.put("num_sets", match.numSets);
            matchData.put("total_points", match.totalPoints);
            
            // Insert match to Supabase
            boolean matchSuccess = supabaseClient.insertMatch(matchData);
            
            if (matchSuccess) {
                Log.d(TAG, "Match synced to Supabase successfully");
                
                // Sync all players (upsert - update if exists, insert if new)
                for (PlayerEntity player : players) {
                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("uuid", player.uuid);
                    playerData.put("name", player.name);
                    playerData.put("matches_played", player.matchesPlayed);
                    playerData.put("matches_won", player.matchesWon);
                    playerData.put("total_points", player.totalPoints);
                    playerData.put("total_sets_won", player.totalSetsWon);
                    playerData.put("total_games_won", player.totalGamesWon);
                    playerData.put("last_played", player.lastPlayed);
                    
                    supabaseClient.upsertPlayer(playerData);
                }
                
                // Sync all match-player relationships
                for (MatchPlayerEntity mp : matchPlayers) {
                    Map<String, Object> mpData = new HashMap<>();
                    mpData.put("match_id", mp.matchId);
                    mpData.put("player_uuid", mp.playerUuid);
                    mpData.put("team_index", mp.teamIndex);
                    mpData.put("points_scored", mp.pointsScored);
                    mpData.put("was_winner", mp.wasWinner);
                    
                    supabaseClient.insertMatchPlayer(mpData);
                }
                
                Log.d(TAG, "All match data synced to Supabase");
            } else {
                Log.w(TAG, "Failed to sync match to Supabase - data saved locally only");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing to Supabase (data still saved locally)", e);
            // Don't throw - fire-and-forget strategy
        }
    }
    
    /**
     * Get player by UUID from local database
     */
    public PlayerEntity getPlayerByUuid(String uuid) {
        return localDb.playerDao().getPlayerByUuid(uuid);
    }
    
    /**
     * Update player statistics in local database
     */
    public void updatePlayerStats(String uuid, int points, int setsWon, int gamesWon, long lastPlayed) {
        localDb.playerDao().updatePlayerStats(uuid, points, setsWon, gamesWon, lastPlayed);
    }
    
    /**
     * Increment player wins in local database
     */
    public void incrementPlayerWins(String uuid) {
        localDb.playerDao().incrementPlayerWins(uuid);
    }
    
    /**
     * Insert new player to local database
     */
    public void insertPlayer(PlayerEntity player) {
        localDb.playerDao().insert(player);
    }
}
