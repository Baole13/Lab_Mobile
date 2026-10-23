package com.example.studentmanager.models;

import android.content.ContentValues;
import android.database.Cursor;

public class Order {
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_CONFIRMED = "Confirmed";
    public static final String STATUS_COMPLETED = "Completed";

    public int id;
    public int customerId;
    public String orderDate;
    public double totalAmount;
    public String status;

    public Order() {}

    public Order(int id, int customerId, String orderDate, double totalAmount, String status) {
        this.id = id;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Order(int customerId, String orderDate, double totalAmount, String status) {
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        if (id > 0) values.put("id", id);
        values.put("customerId", customerId);
        values.put("orderDate", orderDate);
        values.put("totalAmount", totalAmount);
        values.put("status", status);
        return values;
    }

    public static Order fromCursor(Cursor cursor) {
        Order order = new Order();
        order.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        order.setCustomerId(cursor.getInt(cursor.getColumnIndexOrThrow("customerId")));
        order.setOrderDate(cursor.getString(cursor.getColumnIndexOrThrow("orderDate")));
        order.setTotalAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("totalAmount")));
        order.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
        return order;
    }
}
