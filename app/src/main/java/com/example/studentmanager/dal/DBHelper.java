package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.studentmanager.utils.AuthManager;
import java.util.ArrayList;
import java.util.List;

/** Generic CRUD: changing Table/Field names in Schema automatically changes forms/listing. */
public class DBHelper extends SQLiteOpenHelper {
    private final Context appContext;
    // shortcut: Notifications cover this process's DBHelper writes; use cross-process invalidation if needed.
    private static final ContentObservable fruitChanges = new ContentObservable();
    public DBHelper(Context context) {
        super(context.getApplicationContext(), "test2.db", null, Schema.DB_VERSION);
        appContext = context.getApplicationContext();
    }
    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
    @Override public void onCreate(SQLiteDatabase db) {
        for (String sql : Schema.CREATE_SQL) db.execSQL(sql);
        // Demo accounts in SQLite mirror AuthManager's SharedPreferences demo list.
        ContentValues admin = new ContentValues();
        admin.put("id", 1); admin.put("username", "admin");
        admin.put("password", AuthManager.hash("123456")); admin.put("role", AuthManager.ADMIN);
        db.insertOrThrow("Account", null, admin);
        ContentValues customer = new ContentValues();
        customer.put("id", 2); customer.put("username", "customer");
        customer.put("password", AuthManager.hash("123456")); customer.put("role", AuthManager.CUSTOMER);
        db.insertOrThrow("Account", null, customer);
        db.execSQL("INSERT INTO Student(code,fullname,email,accountId) VALUES('S001','Demo Student','student@example.com',2)");
        db.execSQL("INSERT INTO Teacher(code,fullname,email,accountId) VALUES('T001','Demo Teacher','teacher@example.com',1)");
        db.execSQL("INSERT INTO Score(studentCode,teacherCode,subject,score) VALUES('S001','T001','Android',8.5)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 3 && newV >= 3) db.execSQL(Schema.CREATE_FRUIT_SQL);
    }
    private void requireTable(String table) {
        if (!Schema.FRUIT.equals(table) && Schema.find(table) == null)
            throw new IllegalArgumentException("Invalid table: " + table);
    }
    private void requireFruitAdmin(String table) {
        if (!Schema.FRUIT.equals(table)) return;
        AuthManager auth = new AuthManager(appContext);
        if (!auth.isLoggedIn() || !AuthManager.ADMIN.equals(auth.getRole()))
            throw new SecurityException("Chỉ Admin đã đăng nhập mới được thay đổi trái cây.");
    }
    public static void registerFruitObserver(ContentObserver observer) { fruitChanges.registerObserver(observer); }
    public static void unregisterFruitObserver(ContentObserver observer) { fruitChanges.unregisterObserver(observer); }
    public List<ContentValues> rows(String table, String selection, String[] args) {
        requireTable(table);
        List<ContentValues> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(table, null, selection, args, null, null, "id DESC")) {
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
        requireFruitAdmin(table);
        long id = getWritableDatabase().insertOrThrow(table, null, values);
        if (Schema.FRUIT.equals(table)) fruitChanges.dispatchChange(false);
        return id;
    }
    public int update(String table, long id, ContentValues values) {
        requireTable(table);
        requireFruitAdmin(table);
        int count = getWritableDatabase().update(table, values, "id=?", new String[]{String.valueOf(id)});
        if (count > 0 && Schema.FRUIT.equals(table)) fruitChanges.dispatchChange(false);
        return count;
    }
    public int delete(String table, long id) {
        requireTable(table);
        requireFruitAdmin(table);
        int count = getWritableDatabase().delete(table, "id=?", new String[]{String.valueOf(id)});
        if (count > 0 && Schema.FRUIT.equals(table)) fruitChanges.dispatchChange(false);
        return count;
    }
    public String studentCodeForAccount(int accountId) {
        try (Cursor c = getReadableDatabase().query("Student", new String[]{"code"},
                "accountId=?", new String[]{String.valueOf(accountId)}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }
}
