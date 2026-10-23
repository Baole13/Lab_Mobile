package com.example.studentmanager.dal;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.studentmanager.models.FruitEntity;

@Database(entities = {FruitEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    // Existing installs can be at v1 (starter DB) or v2; creating an existing table is a no-op.
    private static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            createFruitTable(database);
        }
    };
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            createFruitTable(database);
        }
    };

    public abstract FruitDao fruitDao();

    public static AppDatabase getInstance(Context context) {
        AppDatabase current = instance;
        if (current == null) {
            synchronized (AppDatabase.class) {
                current = instance;
                if (current == null) {
                    current = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "test2.db")
                            .addMigrations(MIGRATION_1_3, MIGRATION_2_3)
                            .build();
                    instance = current;
                }
            }
        }
        return current;
    }

    private static void createFruitTable(SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS Fruit (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, category TEXT NOT NULL, price REAL NOT NULL, " +
                "quantity INTEGER NOT NULL, expiryDate TEXT NOT NULL, description TEXT NOT NULL DEFAULT '')");
    }
}
