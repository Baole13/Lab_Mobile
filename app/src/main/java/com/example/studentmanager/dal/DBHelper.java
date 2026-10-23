package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.studentmanager.utils.AuthManager;
import java.util.ArrayList;
import java.util.List;

/** Generic CRUD: changing Table/Field names in Schema automatically changes forms/listing. */
public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) { super(context, "test2.db", null, Schema.DB_VERSION); }
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
        // DEMO ONLY: destroys existing data; don't do this in production.
        db.execSQL("DROP TABLE IF EXISTS Score");
        db.execSQL("DROP TABLE IF EXISTS Student");
        db.execSQL("DROP TABLE IF EXISTS Teacher");
        db.execSQL("DROP TABLE IF EXISTS Account");
        onCreate(db);
    }
    private void requireTable(String table) {
        if (Schema.find(table) == null) throw new IllegalArgumentException("Invalid table: " + table);
    }
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
        requireTable(table); return getWritableDatabase().insertOrThrow(table, null, values);
    }
    public int update(String table, long id, ContentValues values) {
        requireTable(table);
        return getWritableDatabase().update(table, values, "id=?", new String[]{String.valueOf(id)});
    }
    public int delete(String table, long id) {
        requireTable(table);
        return getWritableDatabase().delete(table, "id=?", new String[]{String.valueOf(id)});
    }
    public String studentCodeForAccount(int accountId) {
        try (Cursor c = getReadableDatabase().query("Student", new String[]{"code"},
                "accountId=?", new String[]{String.valueOf(accountId)}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }
}
