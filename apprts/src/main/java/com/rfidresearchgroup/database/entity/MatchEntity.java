package com.rfidresearchgroup.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "matches")
public class MatchEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "team_a_name")
    public String teamAName;

    @ColumnInfo(name = "team_b_name")
    public String teamBName;

    @ColumnInfo(name = "winner_team_index")
    public int winnerTeamIndex;

    @ColumnInfo(name = "sets_team_a")
    public int setsTeamA;

    @ColumnInfo(name = "sets_team_b")
    public int setsTeamB;

    @ColumnInfo(name = "games_data")
    public String gamesData; // JSON array of game scores per set

    @ColumnInfo(name = "duration_ms")
    public long durationMs;

    @ColumnInfo(name = "golden_point_used")
    public boolean goldenPointUsed;

    @ColumnInfo(name = "num_sets")
    public int numSets;

    @ColumnInfo(name = "total_points")
    public int totalPoints;
}
