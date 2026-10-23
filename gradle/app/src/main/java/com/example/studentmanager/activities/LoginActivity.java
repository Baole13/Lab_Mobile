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
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_login);
        AuthManager auth = new AuthManager(this);
        EditText user = findViewById(R.id.username), pass = findViewById(R.id.password);
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            if (auth.login(user.getText().toString(), pass.getText().toString())) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            } else Toast.makeText(this, "Wrong username or password", Toast.LENGTH_SHORT).show();
        });
    }
}
