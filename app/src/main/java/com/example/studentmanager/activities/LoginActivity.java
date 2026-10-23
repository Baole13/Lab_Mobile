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
        setContentView(R.layout.activity_login);
        AuthManager auth = new AuthManager(this);
        EditText user = findViewById(R.id.username);
        EditText pass = findViewById(R.id.password);

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String u = user.getText().toString().trim();
            String p = pass.getText().toString();
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }
            if (auth.login(u, p)) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            } else {
                Toast.makeText(this, "Sai tên đăng nhập hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
