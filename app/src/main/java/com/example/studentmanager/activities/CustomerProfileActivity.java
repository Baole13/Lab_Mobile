package com.example.studentmanager.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.models.Customer;
import com.example.studentmanager.utils.AuthManager;

/**
 * Displays and allows editing the Customer profile linked to the authenticated Account.id.
 * Role 2 - Member 2 (Authentication & Customer).
 */
public class CustomerProfileActivity extends Activity {
    private DBHelper dbHelper;
    private AuthManager auth;
    private Customer currentCustomer;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = new AuthManager(this);
        // Authorization check: Must be logged in and have CUSTOMER role
        if (!auth.isLoggedIn() || !auth.isCustomer()) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_customer_profile);
        ((TextView) findViewById(R.id.tvAccount)).setText(
            "Tài khoản: " + auth.getUsername() + " · " + auth.getRole());

        dbHelper = new DBHelper(this);
        loadCustomerProfile();

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditDialog());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnProfileLogout).setOnClickListener(view -> {
            auth.logout();
            openLogin();
        });
    }

    private void loadCustomerProfile() {
        TextView fullname = findViewById(R.id.tvFullname);
        TextView phone = findViewById(R.id.tvPhone);
        TextView email = findViewById(R.id.tvEmail);
        TextView status = findViewById(R.id.tvProfileStatus);

        try {
            currentCustomer = dbHelper.getCustomerByAccountId(auth.getAccountId());
            if (currentCustomer != null) {
                fullname.setText(valueOrDash(currentCustomer.getFullname()));
                phone.setText(valueOrDash(currentCustomer.getPhone()));
                email.setText(valueOrDash(currentCustomer.getEmail()));
                status.setText("");
            } else {
                status.setText("Chưa tìm thấy hồ sơ liên kết với tài khoản này.");
            }
        } catch (Exception exception) {
            status.setText("Chưa thể tải hồ sơ: " + exception.getMessage());
        }
    }

    private void showEditDialog() {
        if (currentCustomer == null) {
            Toast.makeText(this, "Không có dữ liệu hồ sơ để chỉnh sửa", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 20);

        final EditText etFullname = new EditText(this);
        etFullname.setHint("Họ và tên");
        etFullname.setText(currentCustomer.getFullname());
        dialogLayout.addView(etFullname);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Số điện thoại");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setText(currentCustomer.getPhone());
        dialogLayout.addView(etPhone);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setText(currentCustomer.getEmail());
        dialogLayout.addView(etEmail);

        new AlertDialog.Builder(this)
            .setTitle("Cập nhật thông tin")
            .setView(dialogLayout)
            .setPositiveButton("LƯU", (dialog, which) -> {
                String newName = etFullname.getText().toString().trim();
                String newPhone = etPhone.getText().toString().trim();
                String newEmail = etEmail.getText().toString().trim();

                if (newName.isEmpty()) {
                    Toast.makeText(this, "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentCustomer.setFullname(newName);
                currentCustomer.setPhone(newPhone);
                currentCustomer.setEmail(newEmail);

                int affected = dbHelper.updateCustomer(currentCustomer);
                if (affected > 0) {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    loadCustomerProfile();
                } else {
                    Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("HỦY", null)
            .show();
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
