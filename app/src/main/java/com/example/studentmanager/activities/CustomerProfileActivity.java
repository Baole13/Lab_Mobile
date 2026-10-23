package com.example.studentmanager.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.models.Customer;
import com.example.studentmanager.utils.AuthManager;

/** Displays the Customer profile linked to the authenticated Account.id. */
public class CustomerProfileActivity extends Activity {
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AuthManager auth = new AuthManager(this);
        if (!auth.isLoggedIn() || !auth.isCustomer()) {
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

        try {
            Customer customer = dbHelper.getCustomerByAccountId(accountId);
            if (customer != null) {
                fullname.setText(valueOrDash(customer.getFullname()));
                phone.setText(valueOrDash(customer.getPhone()));
                email.setText(valueOrDash(customer.getEmail()));
                status.setText("");
            } else {
                status.setText("Chưa tìm thấy hồ sơ Customer liên kết với tài khoản này.");
            }
        } catch (Exception exception) {
            status.setText("Chưa thể tải hồ sơ: " + exception.getMessage());
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

    @Override
    protected void onDestroy() {
        if (dbHelper != null) dbHelper.close();
        super.onDestroy();
    }
}
