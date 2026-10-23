package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studentmanager.models.Order;
import com.example.studentmanager.utils.AuthManager;
import com.example.studentmanager.utils.ExpiryPolicy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Dữ liệu demo được thêm theo ngày, không cập nhật hoặc xóa bản ghi đã có. */
public final class DemoData {
    private DemoData() {}

    public static long insert(SQLiteDatabase db, String today) {
        if (!ExpiryPolicy.isValidDate(today)) throw new IllegalArgumentException("Ngày demo không hợp lệ.");
        String prefix = "[DEMO " + today + "] ";
        long inserted = 0;
        db.beginTransaction();
        try {
            inserted += fruit(db, prefix + "Táo hết hạn", "Nhập khẩu", 120000, 20, day(today, -1), "Đã hết hạn từ hôm qua; không được đặt mua.");
            inserted += fruit(db, prefix + "Cam hết hạn hôm nay", "Nội địa", 45000, 10, today, "Hết hạn hôm nay, vẫn được đặt trong ngày.");
            inserted += fruit(db, prefix + "Dâu còn 1 ngày", "Nội địa", 180000, 1, day(today, 1), "Biên sắp hết hạn và tồn thấp: 1.");
            inserted += fruit(db, prefix + "Xoài còn 2 ngày", "Nội địa", 75000, 5, day(today, 2), "Sắp hết hạn và biên tồn thấp: 5.");
            inserted += fruit(db, prefix + "Chuối còn 3 ngày", "Nội địa", 35000, 6, day(today, 3), "Biên cuối sắp hết hạn; tồn 6 không tính tồn thấp.");
            inserted += fruit(db, prefix + "Ổi còn 4 ngày", "Nội địa", 50000, 6, day(today, 4), "Còn hạn, không thuộc nhóm sắp hết hạn.");
            inserted += fruit(db, prefix + "Nho hết hàng", "Nhập khẩu", 90000, 0, day(today, 14), "Còn hạn nhưng hết hàng; không được đặt mua.");
            inserted += fruit(db, prefix + "Chanh giá lẻ", "Nội địa", 25000.5, 1, day(today, 14), "Kiểm tra giá thập phân và định dạng tiền Việt Nam.");
            inserted += fruit(db, prefix + "Lê tồn thấp", "Nhập khẩu", 60000, 5, day(today, 14), "Biên tồn thấp, còn hạn dài.");
            inserted += fruit(db, prefix + "Thanh long ruột đỏ loại đặc biệt có tên dài để kiểm tra giao diện", "Nội địa", 99000, 30, day(today, 14), "");

            long accountId;
            try (Cursor cursor = db.query(Schema.ACCOUNT, new String[]{"id", "role"}, "username=?",
                    new String[]{"demo_customer"}, null, null, null)) {
                if (cursor.moveToFirst()) {
                    if (!AuthManager.CUSTOMER.equalsIgnoreCase(cursor.getString(1)))
                        throw new IllegalStateException("Tài khoản demo_customer đã được dùng cho vai trò khác.");
                    accountId = cursor.getLong(0);
                } else {
                    ContentValues account = new ContentValues();
                    account.put("username", "demo_customer"); account.put("password", AuthManager.hash("123456"));
                    account.put("role", AuthManager.CUSTOMER);
                    accountId = db.insertOrThrow(Schema.ACCOUNT, null, account); inserted++;
                }
            }
            long customerId;
            try (Cursor cursor = db.query(Schema.CUSTOMER, new String[]{"id"}, "accountId=?",
                    new String[]{String.valueOf(accountId)}, null, null, null)) {
                if (cursor.moveToFirst()) customerId = cursor.getLong(0);
                else {
                    ContentValues customer = new ContentValues();
                    customer.put("fullname", "Khách hàng Demo"); customer.put("phone", "0900000001");
                    customer.put("email", "demo.customer@example.com"); customer.put("accountId", accountId);
                    customerId = db.insertOrThrow(Schema.CUSTOMER, null, customer); inserted++;
                }
            }
            inserted += order(db, customerId, today + " 09:00", 240000, Order.STATUS_PENDING);
            inserted += order(db, customerId, today + " 10:00", 150000, Order.STATUS_CONFIRMED);
            inserted += order(db, customerId, today + " 11:00", 90000, Order.STATUS_COMPLETED);
            db.setTransactionSuccessful();
            return inserted;
        } finally { db.endTransaction(); }
    }

    // shortcut: Nhận diện mẫu theo tên có ngày; đổi tên rồi nạp lại sẽ tạo lại bản gốc.
    private static int fruit(SQLiteDatabase db, String name, String category, double price,
                             int quantity, String expiryDate, String description) {
        try (Cursor cursor = db.query(Schema.FRUIT, new String[]{"id"}, "name=?", new String[]{name}, null, null, null)) {
            if (cursor.moveToFirst()) return 0;
        }
        ContentValues values = new ContentValues();
        values.put("name", name); values.put("category", category); values.put("price", price);
        values.put("quantity", quantity); values.put("expiryDate", expiryDate); values.put("description", description);
        db.insertOrThrow(Schema.FRUIT, null, values);
        return 1;
    }

    private static int order(SQLiteDatabase db, long customerId, String date, double total, String status) {
        try (Cursor cursor = db.query("\"Order\"", new String[]{"id"}, "customerId=? AND orderDate=?",
                new String[]{String.valueOf(customerId), date}, null, null, null)) {
            if (cursor.moveToFirst()) return 0;
        }
        ContentValues values = new ContentValues();
        values.put("customerId", customerId); values.put("orderDate", date);
        values.put("totalAmount", total); values.put("status", status);
        db.insertOrThrow("\"Order\"", null, values);
        return 1;
    }

    private static String day(String today, int offset) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        calendar.clear();
        calendar.set(Integer.parseInt(today.substring(0, 4)), Integer.parseInt(today.substring(5, 7)) - 1,
                Integer.parseInt(today.substring(8, 10)));
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(calendar.getTime());
    }
}
