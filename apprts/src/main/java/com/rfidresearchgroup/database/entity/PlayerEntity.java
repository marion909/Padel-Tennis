package com.rfidresearchgroup.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import androidx.annotation.NonNull;

@Entity(tableName = "players")
public class PlayerEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uuid")
    public String uuid;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "matches_played")
    public int matchesPlayed;

    @ColumnInfo(name = "matches_won")
    public int matchesWon;

    @ColumnInfo(name = "total_points")
    public int totalPoints;

    @ColumnInfo(name = "total_sets_won")
    public int totalSetsWon;

    @ColumnInfo(name = "total_games_won")
    public int totalGamesWon;

    @ColumnInfo(name = "last_played")
    public long lastPlayed;
}
