package com.rfidresearchgroup.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.rfidresearchgroup.database.dao.MatchDao;
import com.rfidresearchgroup.database.dao.MatchPlayerDao;
import com.rfidresearchgroup.database.dao.PlayerDao;
import com.rfidresearchgroup.database.entity.MatchEntity;
import com.rfidresearchgroup.database.entity.MatchPlayerEntity;
import com.rfidresearchgroup.database.entity.PlayerEntity;

@Database(
    entities = {
        MatchEntity.class,
        PlayerEntity.class,
        MatchPlayerEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract MatchDao matchDao();
    public abstract PlayerDao playerDao();
    public abstract MatchPlayerDao matchPlayerDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "padel_tennis_database"
            ).build();
        }
        return instance;
    }
}
