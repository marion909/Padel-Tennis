package com.rfidresearchgroup.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rfidresearchgroup.database.entity.PlayerEntity;

import java.util.List;

@Dao
public interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(PlayerEntity player);

    @Update
    void update(PlayerEntity player);

    @Query("SELECT * FROM players WHERE uuid = :uuid")
    PlayerEntity getPlayerByUuid(String uuid);

    @Query("SELECT * FROM players ORDER BY matches_played DESC")
    List<PlayerEntity> getAllPlayers();

    @Query("SELECT * FROM players ORDER BY matches_won DESC LIMIT :limit")
    List<PlayerEntity> getTopPlayersByWins(int limit);

    @Query("UPDATE players SET matches_played = matches_played + 1, " +
           "total_points = total_points + :points, " +
           "total_sets_won = total_sets_won + :setsWon, " +
           "total_games_won = total_games_won + :gamesWon, " +
           "last_played = :timestamp " +
           "WHERE uuid = :uuid")
    void updatePlayerStats(String uuid, int points, int setsWon, int gamesWon, long timestamp);

    @Query("UPDATE players SET matches_won = matches_won + 1 WHERE uuid = :uuid")
    void incrementPlayerWins(String uuid);
}
