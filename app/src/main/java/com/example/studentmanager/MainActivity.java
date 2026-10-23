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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = new AuthManager(this);
        if (!auth.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.tvGreeting)).setText(
                "Xin chào, " + auth.getUsername() + " (" + auth.getRole() + ")");

        LinearLayout menu = findViewById(R.id.menuContainer);
        if (AuthManager.ADMIN.equalsIgnoreCase(auth.getRole())) {
            add(menu, "📦 Quản lý Trái Cây (Fruits)", Schema.FRUIT);
            add(menu, "📑 Quản lý Đơn Hàng (Orders)", Schema.ORDER);
            add(menu, "👥 Quản lý Khách Hàng (Customers)", Schema.CUSTOMER);
        } else {
            add(menu, "🍎 Xem & Tìm Trái Cây (Fruits)", Schema.FRUIT);
            add(menu, "🛒 Lịch Sử Đơn Hàng (Orders)", Schema.ORDER);
            add(menu, "👤 Hồ Sơ Khách Hàng (Profile)", Schema.CUSTOMER);
        }

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private void add(LinearLayout menu, String caption, String table) {
        Button b = new Button(this);
        b.setText(caption);
        menu.addView(b);
        b.setOnClickListener(v -> {
            Intent i = new Intent(this, EntityListActivity.class);
            i.putExtra("table", table);
            startActivity(i);
        });
    }
}
