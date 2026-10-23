package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.studentmanager.models.Account;
import com.example.studentmanager.models.Customer;
import com.example.studentmanager.models.Fruit;
import com.example.studentmanager.models.Order;
import com.example.studentmanager.utils.AuthManager;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite Database Helper for Fruit Shop Management.
 * Provides strongly-typed CRUD APIs and seed data for the entire development team.
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final android.database.ContentObservable fruitChanges = new android.database.ContentObservable();
    public static void registerFruitObserver(android.database.ContentObserver observer) { fruitChanges.registerObserver(observer); }
    public static void unregisterFruitObserver(android.database.ContentObserver observer) { fruitChanges.unregisterObserver(observer); }


    private final Context appContext;
    public DBHelper(Context context) {
        super(context.getApplicationContext(), Schema.DB_NAME, null, Schema.DB_VERSION);
        appContext = context.getApplicationContext();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Create all 4 tables
        for (String sql : Schema.CREATE_SQL) {
            db.execSQL(sql);
        }

        // 2. Seed Default Accounts (Admin & Customer) with SHA-256 hashed passwords
        ContentValues adminAcc = new ContentValues();
        adminAcc.put("username", "admin");
        adminAcc.put("password", AuthManager.hash("123456"));
        adminAcc.put("role", Schema.ROLE_ADMIN);
        long adminId = db.insertOrThrow(Schema.ACCOUNT, null, adminAcc);

        ContentValues customerAcc = new ContentValues();
        customerAcc.put("username", "customer");
        customerAcc.put("password", AuthManager.hash("123456"));
        customerAcc.put("role", Schema.ROLE_CUSTOMER);
        long customerAccId = db.insertOrThrow(Schema.ACCOUNT, null, customerAcc);

        // 3. Seed Default Customer profile linked to customerAccId
        ContentValues customerProfile = new ContentValues();
        customerProfile.put("fullname", "Nguyen Van A");
        customerProfile.put("phone", "0901234567");
        customerProfile.put("email", "customer@fruitshop.com");
        customerProfile.put("accountId", customerAccId);
        long customerId = db.insertOrThrow(Schema.CUSTOMER, null, customerProfile);

        // 4. Seed Fruit catalog with all test cases (Fresh, Expired, Out-of-Stock)
        // 4.1 Fresh fruit (Valid, far expiry)
        ContentValues f1 = new ContentValues();
        f1.put("name", "Táo Envy Mỹ");
        f1.put("category", "Trái cây nhập khẩu");
        f1.put("price", 120000.0);
        f1.put("quantity", 50);
        f1.put("expiryDate", "2026-12-31");
        f1.put("description", "Táo Envy nhập khẩu từ Mỹ, giòn ngọt mọng nước.");
        db.insert(Schema.FRUIT, null, f1);

        // 4.2 Fresh fruit (Valid domestic)
        ContentValues f2 = new ContentValues();
        f2.put("name", "Cam Sành Hàm Yên");
        f2.put("category", "Trái cây nội địa");
        f2.put("price", 45000.0);
        f2.put("quantity", 100);
        f2.put("expiryDate", "2026-11-25");
        f2.put("description", "Cam sành Hàm Yên mọng nước, nhiều vitamin C.");
        db.insert(Schema.FRUIT, null, f2);

        // 4.3 Expired fruit (Used by Member 3 & Member 4 to test expiry block)
        ContentValues f3 = new ContentValues();
        f3.put("name", "Dâu Tây Đà Lạt");
        f3.put("category", "Trái cây nội địa");
        f3.put("price", 180000.0);
        f3.put("quantity", 20);
        f3.put("expiryDate", "2026-10-01");
        f3.put("description", "Dâu tây Đà Lạt loại 1 (HSD đã qua để kiểm tra quy tắc hết hạn).");
        db.insert(Schema.FRUIT, null, f3);

        // 4.4 Out of stock fruit (quantity = 0 to test out-of-stock block)
        ContentValues f4 = new ContentValues();
        f4.put("name", "Xoài Cát Hòa Lộc");
        f4.put("category", "Trái cây nội địa");
        f4.put("price", 75000.0);
        f4.put("quantity", 0);
        f4.put("expiryDate", "2026-11-30");
        f4.put("description", "Xoài cát Hòa Lộc thơm ngon (Tạm hết hàng để test quantity = 0).");
        db.insert(Schema.FRUIT, null, f4);

        // 5. Seed Sample Orders for Customer 1
        ContentValues o1 = new ContentValues();
        o1.put("customerId", customerId);
        o1.put("orderDate", "2026-10-22 10:30");
        o1.put("totalAmount", 240000.0);
        o1.put("status", Order.STATUS_PENDING);
        db.insert("\"" + Schema.ORDER + "\"", null, o1);

        ContentValues o2 = new ContentValues();
        o2.put("customerId", customerId);
        o2.put("orderDate", "2026-10-20 14:15");
        o2.put("totalAmount", 90000.0);
        o2.put("status", Order.STATUS_COMPLETED);
        db.insert("\"" + Schema.ORDER + "\"", null, o2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        throw new IllegalStateException("Chưa có migration từ phiên bản " + oldV + " sang " + newV + ". Giữ nguyên dữ liệu hiện tại.");
    }

    private void requireTable(String table) {
        if (Schema.find(table) == null) {
            throw new IllegalArgumentException("Invalid table: " + table);
        }
    }

    // =========================================================================
    // 1. AUTHENTICATION & ACCOUNT APIS (Dành cho Người 2)
    // =========================================================================

    /**
     * Checks credentials against the Account table with SHA-256 hashed password.
     * @return Account if valid, null otherwise.
     */
    public Account checkLogin(String username, String plainPassword) {
        if (username == null || plainPassword == null) return null;
        String hashed = AuthManager.hash(plainPassword);
        try (Cursor c = getReadableDatabase().query(
                Schema.ACCOUNT, null,
                "username = ? AND password = ?",
                new String[]{username.trim(), hashed},
                null, null, null)) {
            if (c.moveToFirst()) {
                return Account.fromCursor(c);
            }
        }
        return null;
    }

    public Account getAccountById(int accountId) {
        try (Cursor c = getReadableDatabase().query(
                Schema.ACCOUNT, null, "id = ?",
                new String[]{String.valueOf(accountId)}, null, null, null)) {
            return c.moveToFirst() ? Account.fromCursor(c) : null;
        }
    }

    public Account getAccountByUsername(String username) {
        try (Cursor c = getReadableDatabase().query(
                Schema.ACCOUNT, null, "username = ?",
                new String[]{username}, null, null, null)) {
            return c.moveToFirst() ? Account.fromCursor(c) : null;
        }
    }

    public long insertAccount(Account account) {
        ContentValues cv = account.toContentValues();
        // Ensure password is saved as hash if not hashed
        return getWritableDatabase().insertOrThrow(Schema.ACCOUNT, null, cv);
    }

    // =========================================================================
    // 2. CUSTOMER APIS (Dành cho Người 2)
    // =========================================================================

    public Customer getCustomerByAccountId(int accountId) {
        try (Cursor c = getReadableDatabase().query(
                Schema.CUSTOMER, null, "accountId = ?",
                new String[]{String.valueOf(accountId)}, null, null, null)) {
            return c.moveToFirst() ? Customer.fromCursor(c) : null;
        }
    }

    public Customer getCustomerById(int customerId) {
        try (Cursor c = getReadableDatabase().query(
                Schema.CUSTOMER, null, "id = ?",
                new String[]{String.valueOf(customerId)}, null, null, null)) {
            return c.moveToFirst() ? Customer.fromCursor(c) : null;
        }
    }

    public long insertCustomer(Customer customer) {
        return getWritableDatabase().insertOrThrow(Schema.CUSTOMER, null, customer.toContentValues());
    }

    public int updateCustomer(Customer customer) {
        return getWritableDatabase().update(
                Schema.CUSTOMER, customer.toContentValues(),
                "id = ?", new String[]{String.valueOf(customer.getId())});
    }

    // =========================================================================
    // 3. FRUIT MANAGEMENT APIS (Dành cho Người 3 & Người 2)
    // =========================================================================

    public List<Fruit> getAllFruits() {
        List<Fruit> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(Schema.FRUIT, null, null, null, null, null, "id DESC")) {
            while (c.moveToNext()) {
                list.add(Fruit.fromCursor(c));
            }
        }
        return list;
    }

    public Fruit getFruitById(int id) {
        try (Cursor c = getReadableDatabase().query(
                Schema.FRUIT, null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? Fruit.fromCursor(c) : null;
        }
    }

    /**
     * Search fruit by name OR category.
     */
    public List<Fruit> searchFruits(String keyword) {
        List<Fruit> list = new ArrayList<>();
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Cursor c = getReadableDatabase().query(
                Schema.FRUIT, null,
                "name LIKE ? OR category LIKE ?",
                new String[]{pattern, pattern},
                null, null, "id DESC")) {
            while (c.moveToNext()) {
                list.add(Fruit.fromCursor(c));
            }
        }
        return list;
    }

    public long insertFruit(Fruit fruit) {
        return getWritableDatabase().insertOrThrow(Schema.FRUIT, null, fruit.toContentValues());
    }

    public int updateFruit(Fruit fruit) {
        return getWritableDatabase().update(
                Schema.FRUIT, fruit.toContentValues(),
                "id = ?", new String[]{String.valueOf(fruit.getId())});
    }

    public int deleteFruit(int id) {
        return getWritableDatabase().delete(
                Schema.FRUIT, "id = ?",
                new String[]{String.valueOf(id)});
    }

    /**
     * Atomically reduces fruit stock quantity when placing an order.
     * @return true if successful, false if insufficient stock or fruit not found.
     */
    public boolean reduceFruitQuantity(int fruitId, int quantityToDeduct) {
        if (quantityToDeduct <= 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        int rows;
        try (android.database.sqlite.SQLiteStatement statement = db.compileStatement(
                "UPDATE Fruit SET quantity=quantity-? WHERE id=? AND quantity>=?")) {
            statement.bindLong(1, quantityToDeduct); statement.bindLong(2, fruitId);
            statement.bindLong(3, quantityToDeduct); rows = statement.executeUpdateDelete();
        }
        if (rows > 0 && !db.inTransaction()) fruitChanges.dispatchChange(false);
        return rows > 0;
    }

    public long placeOrder(int customerId, java.util.Map<Integer, Integer> quantities,
                           String orderDate, double expectedTotal) {
        AuthManager auth = new AuthManager(appContext);
        Customer customer = getCustomerByAccountId(auth.getAccountId());
        if (!auth.isCustomer() || customer == null || customer.getId() != customerId)
            throw new SecurityException("Bạn chỉ được đặt đơn cho tài khoản của mình.");
        SQLiteDatabase db = getWritableDatabase();
        long orderId;
        // Trừ tồn và tạo đơn cùng transaction; bất kỳ lỗi nào cũng hoàn lại toàn bộ tồn kho.
        db.beginTransaction();
        try {
            double total = 0;
            for (java.util.Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
                Integer quantity = entry.getValue();
                if (entry.getKey() == null || quantity == null || quantity <= 0)
                    throw new IllegalArgumentException("Số lượng đặt hàng không hợp lệ.");
                Fruit fruit = getFruitById(entry.getKey());
                if (fruit == null || !fruit.isAvailable() || !reduceFruitQuantity(fruit.getId(), quantity))
                    throw new IllegalStateException("Trái cây đã hết hạn hoặc không đủ tồn kho. Vui lòng chọn lại.");
                total += fruit.getPrice() * quantity;
            }
            if (total <= 0 || Double.isNaN(total) || Double.isInfinite(total))
                throw new IllegalArgumentException("Tổng tiền đơn hàng không hợp lệ.");
            if (Double.compare(total, expectedTotal) != 0)
                throw new IllegalStateException("Giá trái cây đã thay đổi. Vui lòng kiểm tra lại đơn hàng.");
            orderId = insertOrder(new Order(customerId, orderDate, total, Order.STATUS_PENDING));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        fruitChanges.dispatchChange(false);
        return orderId;
    }

    // =========================================================================
    // 4. ORDER APIS (Dành cho Người 4)
    // =========================================================================

    /**
     * Creates a new Order record.
     */
    public long insertOrder(Order order) {
        return getWritableDatabase().insertOrThrow("\"" + Schema.ORDER + "\"", null, order.toContentValues());
    }

    /**
     * Get order history of a specific Customer, latest first.
     */
    public List<Order> getOrdersByCustomer(int customerId) {
        List<Order> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(
                "\"" + Schema.ORDER + "\"", null,
                "customerId = ?",
                new String[]{String.valueOf(customerId)},
                null, null, "id DESC")) {
            while (c.moveToNext()) {
                list.add(Order.fromCursor(c));
            }
        }
        return list;
    }

    /**
     * Get all orders for Admin order management, latest first.
     */
    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(
                "\"" + Schema.ORDER + "\"", null,
                null, null, null, null, "id DESC")) {
            while (c.moveToNext()) {
                list.add(Order.fromCursor(c));
            }
        }
        return list;
    }

    /**
     * Updates order status: Pending -> Confirmed -> Completed.
     */
    public int updateOrderStatus(int orderId, String newStatus) {
        ContentValues cv = new ContentValues();
        cv.put("status", newStatus);
        return getWritableDatabase().update(
                "\"" + Schema.ORDER + "\"", cv,
                "id = ?", new String[]{String.valueOf(orderId)});
    }

    // =========================================================================
    // 5. GENERIC CRUD (Hỗ trợ Activity generic của template có sẵn)
    // =========================================================================

    public List<ContentValues> rows(String table, String selection, String[] args) {
        requireTable(table);
        String safeTable = table.equalsIgnoreCase("Order") ? "\"Order\"" : table;
        List<ContentValues> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(safeTable, null, selection, args, null, null, "id DESC")) {
            while (c.moveToNext()) {
                ContentValues row = new ContentValues();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    if (c.isNull(i)) row.putNull(c.getColumnName(i));
                    else row.put(c.getColumnName(i), c.getString(i));
                }
                result.add(row);
            }
        }
        return result;
    }

    public ContentValues findById(String table, long id) {
        List<ContentValues> matches = rows(table, "id=?", new String[]{String.valueOf(id)});
        return matches.isEmpty() ? null : matches.get(0);
    }

    public long insert(String table, ContentValues values) {
        requireTable(table);
        String safeTable = table.equalsIgnoreCase("Order") ? "\"Order\"" : table;
        return getWritableDatabase().insertOrThrow(safeTable, null, values);
    }

    public int update(String table, long id, ContentValues values) {
        requireTable(table);
        String safeTable = table.equalsIgnoreCase("Order") ? "\"Order\"" : table;
        int count = getWritableDatabase().update(safeTable, values, "id=?", new String[]{String.valueOf(id)});
        if (count > 0 && Schema.FRUIT.equals(table)) fruitChanges.dispatchChange(false);
        return count;
    }

    public int delete(String table, long id) {
        requireTable(table);
        String safeTable = table.equalsIgnoreCase("Order") ? "\"Order\"" : table;
        return getWritableDatabase().delete(safeTable, "id=?", new String[]{String.valueOf(id)});
    }
}
