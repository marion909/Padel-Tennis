package com.rfidresearchgroup.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.rfidresearchgroup.database.entity.MatchEntity;

import java.util.List;

@Dao
public interface MatchDao {
    @Insert
    long insert(MatchEntity match);

    @Query("SELECT * FROM matches ORDER BY timestamp DESC")
    List<MatchEntity> getAllMatches();

    @Query("SELECT * FROM matches WHERE id = :matchId")
    MatchEntity getMatchById(long matchId);

    @Query("SELECT COUNT(*) FROM matches")
    int getMatchCount();

    @Query("DELETE FROM matches WHERE id = :matchId")
    void deleteMatch(long matchId);
}
