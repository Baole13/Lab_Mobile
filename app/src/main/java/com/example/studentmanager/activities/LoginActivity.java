package com.example.studentmanager.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import com.example.studentmanager.MainActivity;
import com.example.studentmanager.R;
import com.example.studentmanager.utils.AuthManager;

public class LoginActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AuthManager auth = new AuthManager(this);
        if (auth.isLoggedIn()) {
            openDashboard();
            return;
        }
        setContentView(R.layout.activity_login);
        EditText user = findViewById(R.id.username);
        EditText pass = findViewById(R.id.password);

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String u = user.getText().toString().trim();
            String p = pass.getText().toString().trim();

            if (u.isEmpty()) {
                user.setError("Vui lòng nhập tên đăng nhập");
                user.requestFocus();
                return;
            }
            if (p.isEmpty()) {
                pass.setError("Vui lòng nhập mật khẩu");
                pass.requestFocus();
                return;
            }

            if (auth.login(u, p)) {
                openDashboard();
            } else {
                Toast.makeText(this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
