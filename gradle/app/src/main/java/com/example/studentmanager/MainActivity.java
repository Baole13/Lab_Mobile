package com.example.studentmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.studentmanager.activities.EntityListActivity;
import com.example.studentmanager.activities.LoginActivity;
import com.example.studentmanager.dal.Schema;
import com.example.studentmanager.utils.AuthManager;

public class MainActivity extends Activity {
    private AuthManager auth;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = new AuthManager(this);
        if (!auth.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class)); finish(); return;
        }
        setContentView(R.layout.activity_main);
        ((TextView)findViewById(R.id.tvGreeting)).setText("Hello, " + auth.getUsername() + " (" + auth.getRole() + ")");
        LinearLayout menu = findViewById(R.id.menuContainer);
        if (AuthManager.TEACHER.equals(auth.getRole())) {
            add(menu, "Manage students", Schema.STUDENT);
            add(menu, "Manage teachers", Schema.TEACHER);
            add(menu, "Manage scores", Schema.SCORE);
        } else if (AuthManager.STUDENT.equals(auth.getRole())) {
            add(menu, "My profile (read only)", Schema.STUDENT);
            add(menu, "My scores (read only)", Schema.SCORE);
        }
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }
    private void add(LinearLayout menu, String caption, String table) {
        Button b = new Button(this);b.setText(caption);
        menu.addView(b);
        b.setOnClickListener(v -> {
            Intent i = new Intent(this, EntityListActivity.class);
            i.putExtra("table", table);startActivity(i);
        });
    }
}
