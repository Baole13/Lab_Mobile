package com.example.studentmanager.dal;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.studentmanager.models.FruitEntity;

@Database(entities = {FruitEntity.class}, version = Schema.DB_VERSION, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;
    public abstract FruitDao fruitDao();
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) instance = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, Schema.DB_NAME).build();
            }
        }
        return instance;
    }
}
