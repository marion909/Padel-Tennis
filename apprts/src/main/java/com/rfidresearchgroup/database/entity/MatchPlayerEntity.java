package com.rfidresearchgroup.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "match_players",
    primaryKeys = {"match_id", "player_uuid"},
    foreignKeys = {
        @ForeignKey(
            entity = MatchEntity.class,
            parentColumns = "id",
            childColumns = "match_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = PlayerEntity.class,
            parentColumns = "uuid",
            childColumns = "player_uuid",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index("match_id"),
        @Index("player_uuid")
    }
)
public class MatchPlayerEntity {
    @ColumnInfo(name = "match_id")
    public long matchId;

    @NonNull
    @ColumnInfo(name = "player_uuid")
    public String playerUuid;

    @ColumnInfo(name = "team_index")
    public int teamIndex;

    @ColumnInfo(name = "points_scored")
    public int pointsScored;

    @ColumnInfo(name = "was_winner")
    public boolean wasWinner;
}
