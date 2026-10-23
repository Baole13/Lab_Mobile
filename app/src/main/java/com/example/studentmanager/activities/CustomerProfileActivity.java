package com.example.studentmanager.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.TextView;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.utils.AuthManager;

/** Displays the Customer row linked to the authenticated Account.id. */
public class CustomerProfileActivity extends Activity {
    private DBHelper dbHelper;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AuthManager auth = new AuthManager(this);
        if (!auth.isLoggedIn() || !AuthManager.CUSTOMER.equals(auth.getRole())) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_customer_profile);
        ((TextView) findViewById(R.id.tvAccount)).setText(
            "Tài khoản: " + auth.getUsername() + " · " + auth.getRole());
        showCustomerDetails(auth.getAccountId());

        findViewById(R.id.btnProfileLogout).setOnClickListener(view -> {
            auth.logout();
            openLogin();
        });
    }

    private void showCustomerDetails(int accountId) {
        TextView fullname = findViewById(R.id.tvFullname);
        TextView phone = findViewById(R.id.tvPhone);
        TextView email = findViewById(R.id.tvEmail);
        TextView status = findViewById(R.id.tvProfileStatus);
        dbHelper = new DBHelper(this);

        try (Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT fullname, phone, email FROM Customer WHERE accountId = ? LIMIT 1",
                new String[]{String.valueOf(accountId)})) {
            if (cursor.moveToFirst()) {
                fullname.setText(valueOrDash(cursor.getString(0)));
                phone.setText(valueOrDash(cursor.getString(1)));
                email.setText(valueOrDash(cursor.getString(2)));
                status.setText("");
            } else {
                status.setText("Chưa tìm thấy hồ sơ Customer liên kết với tài khoản này.");
            }
        } catch (SQLiteException exception) {
            status.setText("Chưa thể tải hồ sơ. Kiểm tra bảng Customer có các cột fullname, phone, email, accountId.");
        }
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override protected void onDestroy() {
        if (dbHelper != null) dbHelper.close();
        super.onDestroy();
    }
}
