package com.example.studentmanager.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

@Entity(tableName = "Fruit")
public class FruitEntity {
    @PrimaryKey(autoGenerate = true)
    // SQLiteOpenHelper hiện có khai báo khóa chính không kèm NOT NULL.
    public Integer id;

    @NonNull public String name = "";
    @NonNull public String category = "";
    public double price;
    public int quantity;
    @NonNull public String expiryDate = "";
    @NonNull @ColumnInfo(defaultValue = "''") public String description = "";
}
