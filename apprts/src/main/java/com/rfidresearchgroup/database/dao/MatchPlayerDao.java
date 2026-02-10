package com.rfidresearchgroup.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.rfidresearchgroup.database.entity.MatchPlayerEntity;

import java.util.List;

@Dao
public interface MatchPlayerDao {
    @Insert
    void insert(MatchPlayerEntity matchPlayer);

    @Insert
    void insertAll(List<MatchPlayerEntity> matchPlayers);

    @Query("SELECT * FROM match_players WHERE match_id = :matchId")
    List<MatchPlayerEntity> getPlayersForMatch(long matchId);

    @Query("SELECT * FROM match_players WHERE player_uuid = :playerUuid")
    List<MatchPlayerEntity> getMatchesForPlayer(String playerUuid);
}
