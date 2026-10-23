package com.example.studentmanager.dal;

import androidx.room.Dao;
import androidx.room.Query;
import com.example.studentmanager.models.FruitEntity;
import java.util.List;

@Dao
public interface FruitDao {
    @Query("SELECT * FROM Fruit ORDER BY id DESC")
    List<FruitEntity> getAll();

    @androidx.room.Insert
    long insert(FruitEntity fruit);

    // Không cập nhật quantity khi chỉ sửa thông tin sản phẩm.
    @Query("UPDATE Fruit SET name=:name, category=:category, price=:price, expiryDate=:expiryDate, description=:description WHERE id=:id")
    int updateInformation(int id, String name, String category, double price, String expiryDate, String description);

    @Query("DELETE FROM Fruit WHERE id=:id")
    int deleteById(int id);

    @Query("SELECT * FROM Fruit WHERE id = :id LIMIT 1")
    FruitEntity getById(int id);
}
