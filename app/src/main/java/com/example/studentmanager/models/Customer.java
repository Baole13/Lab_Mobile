package com.example.studentmanager.models;

import android.content.ContentValues;
import android.database.Cursor;

public class Customer {
    public int id;
    public String fullname;
    public String phone;
    public String email;
    public int accountId;

    public Customer() {}

    public Customer(int id, String fullname, String phone, String email, int accountId) {
        this.id = id;
        this.fullname = fullname;
        this.phone = phone;
        this.email = email;
        this.accountId = accountId;
    }

    public Customer(String fullname, String phone, String email, int accountId) {
        this.fullname = fullname;
        this.phone = phone;
        this.email = email;
        this.accountId = accountId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        if (id > 0) values.put("id", id);
        values.put("fullname", fullname);
        values.put("phone", phone);
        values.put("email", email);
        values.put("accountId", accountId);
        return values;
    }

    public static Customer fromCursor(Cursor cursor) {
        Customer customer = new Customer();
        customer.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        customer.setFullname(cursor.getString(cursor.getColumnIndexOrThrow("fullname")));
        customer.setPhone(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
        customer.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
        customer.setAccountId(cursor.getInt(cursor.getColumnIndexOrThrow("accountId")));
        return customer;
    }
}
