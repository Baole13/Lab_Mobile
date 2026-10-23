package com.example.studentmanager.models;

import android.content.ContentValues;
import android.database.Cursor;

public class Fruit {
    public int id;
    public String name;
    public String category;
    public double price;
    public int quantity;
    public String expiryDate; // Format: YYYY-MM-DD
    public String description;

    public Fruit() {}

    public Fruit(int id, String name, String category, double price, int quantity, String expiryDate, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.description = description;
    }

    public Fruit(String name, String category, double price, int quantity, String expiryDate, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Checks if the fruit is expired compared to the current date.
     * Rule: expiryDate < today => Expired (true); expiryDate >= today => Valid (false).
     */
    public boolean isExpired() {
        return com.example.studentmanager.utils.ExpiryPolicy.evaluate(expiryDate,
                com.example.studentmanager.utils.ExpiryPolicy.todayIsoDate())
                == com.example.studentmanager.utils.ExpiryPolicy.Status.EXPIRED;
    }

    /**
     * Checks if the fruit can be ordered by customers:
     * - Quantity must be > 0
     * - Must not be expired
     */
    public boolean isAvailable() {
        return quantity > 0 && com.example.studentmanager.utils.ExpiryPolicy.isValidDate(expiryDate) && !isExpired();
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        if (id > 0) values.put("id", id);
        values.put("name", name);
        values.put("category", category);
        values.put("price", price);
        values.put("quantity", quantity);
        values.put("expiryDate", expiryDate);
        values.put("description", description);
        return values;
    }

    public static Fruit fromCursor(Cursor cursor) {
        Fruit fruit = new Fruit();
        fruit.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        fruit.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        fruit.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        fruit.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("price")));
        fruit.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow("quantity")));
        fruit.setExpiryDate(cursor.getString(cursor.getColumnIndexOrThrow("expiryDate")));
        fruit.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        return fruit;
    }
}
