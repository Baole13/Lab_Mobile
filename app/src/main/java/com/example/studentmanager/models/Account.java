package com.example.studentmanager.models;

import android.content.ContentValues;
import android.database.Cursor;

public class Account {
    public int id;
    public String username;
    public String password;
    public String role;

    public Account() {}

    public Account(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Account(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        if (id > 0) values.put("id", id);
        values.put("username", username);
        values.put("password", password);
        values.put("role", role);
        return values;
    }

    public static Account fromCursor(Cursor cursor) {
        Account account = new Account();
        account.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        account.setUsername(cursor.getString(cursor.getColumnIndexOrThrow("username")));
        account.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("password")));
        account.setRole(cursor.getString(cursor.getColumnIndexOrThrow("role")));
        return account;
    }
}
