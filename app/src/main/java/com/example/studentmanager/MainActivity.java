package com.example.studentmanager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.studentmanager.activities.CustomerProfileActivity;
import com.example.studentmanager.activities.EditRecordActivity;
import com.example.studentmanager.activities.EntityListActivity;
import com.example.studentmanager.activities.LoginActivity;
import com.example.studentmanager.activities.OrderHistoryActivity;
import com.example.studentmanager.activities.OrderManagementActivity;
import com.example.studentmanager.activities.PlaceOrderActivity;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.Schema;
import com.example.studentmanager.models.Customer;
import com.example.studentmanager.models.Fruit;
import com.example.studentmanager.models.Order;
import com.example.studentmanager.utils.AuthManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Người 4 — MainActivity (Dashboard Admin/Customer & Integration)
 * Tích hợp đầy đủ với DBHelper, Schema, AuthManager trên nhánh dev và Grocery App UI Design System.
 */
public class MainActivity extends Activity {
    private AuthManager auth;
    private DBHelper db;
    private TextView tvGreeting;
    private TextView tvRoleSubtitle;
    private TextView tvDashboardSummary;
    private LinearLayout menuContainer;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthManager(this);
        if (!auth.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db = new DBHelper(this);
        setContentView(R.layout.activity_main);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvRoleSubtitle = findViewById(R.id.tvRoleSubtitle);
        tvDashboardSummary = findViewById(R.id.tvDashboardSummary);
        menuContainer = findViewById(R.id.menuContainer);

        setupMenuByRole();

        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            auth.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth != null && auth.isLoggedIn() && db != null) {
            refreshDashboardStats();
        }
    }

    private void setupMenuByRole() {
        menuContainer.removeAllViews();

        if (auth.isAdmin()) {
            tvRoleSubtitle.setText("Quản trị viên");
            tvGreeting.setText("Xin chào, " + auth.getUsername() + "");

            addTableCard(
                    "🍎",
                    "Quản lý trái cây",
                    "Thêm, sửa, xóa, tìm kiếm & kiểm tra hạn sử dụng",
                    Schema.FRUIT
            );
            addNewRecordCard(
                    "➕",
                    "Thêm trái cây",
                    "Tạo mới trái cây (kiểm tra price > 0, hạn dùng)",
                    Schema.FRUIT
            );
            addActivityCard(
                    "📦",
                    "Quản lý đơn hàng",
                    "Xác nhận và theo dõi tiến độ đơn hàng",
                    OrderManagementActivity.class
            );
            addTableCard(
                    "👥",
                    "Quản lý khách hàng",
                    "Xem và quản lý danh sách hồ sơ khách hàng",
                    Schema.CUSTOMER
            );
        } else {
            Customer customer = db.getCustomerByAccountId(auth.getAccountId());
            String displayName = (customer != null && customer.getFullname() != null && !customer.getFullname().isEmpty())
                    ? customer.getFullname()
                    : auth.getUsername();

            tvRoleSubtitle.setText("Khách hàng");
            tvGreeting.setText("Xin chào, " + displayName + "");

            addActivityCard(
                    "🛒",
                    "Đặt mua trái cây",
                    "Chọn trái cây còn hạn & còn hàng, tính tổng tiền tự động",
                    PlaceOrderActivity.class
            );
            addActivityCard(
                    "📋",
                    "Lịch sử đơn hàng",
                    "Theo dõi đơn chờ xác nhận và đã hoàn thành",
                    OrderHistoryActivity.class
            );
            addTableCard(
                    "🍉",
                    "Danh mục trái cây",
                    "Tra cứu danh mục trái cây, giá bán và hạn sử dụng",
                    Schema.FRUIT
            );
            addActivityCard(
                    "👤",
                    "Hồ sơ của tôi",
                    "Xem và cập nhật họ tên, số điện thoại, email",
                    CustomerProfileActivity.class
            );
        }
    }

    private void refreshDashboardStats() {
        List<Fruit> fruits = db.getAllFruits();
        int availableFruits = 0;
        int expiredFruits = 0;
        int outOfStockFruits = 0;
        for (Fruit f : fruits) {
            if (f.isExpired()) {
                expiredFruits++;
            } else if (f.getQuantity() <= 0) {
                outOfStockFruits++;
            } else {
                availableFruits++;
            }
        }

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        if (auth.isAdmin()) {
            List<Order> allOrders = db.getAllOrders();
            int pendingCount = 0;
            int confirmedCount = 0;
            int completedCount = 0;
            double completedRevenue = 0;

            for (Order o : allOrders) {
                if (Order.STATUS_PENDING.equalsIgnoreCase(o.getStatus())) pendingCount++;
                else if (Order.STATUS_CONFIRMED.equalsIgnoreCase(o.getStatus())) confirmedCount++;
                else if (Order.STATUS_COMPLETED.equalsIgnoreCase(o.getStatus())) {
                    completedCount++;
                    completedRevenue += o.getTotalAmount();
                }
            }

            String summary = "• Trái cây: " + fruits.size() + " loại (Còn bán: " + availableFruits
                    + " | Hết hạn: " + expiredFruits + " | Hết hàng: " + outOfStockFruits + ")\n"
                    + "• Đơn hàng: " + allOrders.size() + " đơn (Chờ xác nhận: " + pendingCount
                    + " | Đã xác nhận: " + confirmedCount + " | Hoàn thành: " + completedCount + ")\n"
                    + "• Doanh thu hoàn thành: " + currencyFormat.format(completedRevenue) + "đ";
            tvDashboardSummary.setText(summary);
        } else {
            Customer customer = db.getCustomerByAccountId(auth.getAccountId());
            List<Order> myOrders = customer != null
                    ? db.getOrdersByCustomer(customer.getId())
                    : Collections.emptyList();
            int pendingCount = 0;
            for (Order o : myOrders) {
                if (Order.STATUS_PENDING.equalsIgnoreCase(o.getStatus())) pendingCount++;
            }
            String summary = "• Trái cây tươi sẵn sàng đặt: " + availableFruits + " loại\n"
                    + "• Đơn hàng của bạn: " + myOrders.size() + " đơn (Chờ duyệt: " + pendingCount + ")\n"
                    + "• Ngày hệ thống: " + todayStr;
            tvDashboardSummary.setText(summary);
        }
    }

    private void addActivityCard(String iconEmoji, String title, String subtitle, Class<?> targetActivity) {
        addMenuCardView(iconEmoji, title, subtitle, () ->
                startActivity(new Intent(this, targetActivity)));
    }

    private void addTableCard(String iconEmoji, String title, String subtitle, String table) {
        addMenuCardView(iconEmoji, title, subtitle, () -> {
            Intent i = new Intent(this, EntityListActivity.class);
            i.putExtra("table", table);
            startActivity(i);
        });
    }

    private void addNewRecordCard(String iconEmoji, String title, String subtitle, String table) {
        addMenuCardView(iconEmoji, title, subtitle, () -> {
            Intent i = new Intent(this, EditRecordActivity.class);
            i.putExtra("table", table);
            i.putExtra("id", -1L);
            startActivity(i);
        });
    }

    private void addMenuCardView(String iconEmoji, String title, String subtitle, Runnable onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = getResources().getDimensionPixelSize(R.dimen.space_lg);
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(iconEmoji);
        tvIcon.setTextSize(20f);
        tvIcon.setGravity(Gravity.CENTER);
        tvIcon.setBackgroundResource(R.drawable.bg_image_placeholder);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconParams.rightMargin = dp(12);
        tvIcon.setLayoutParams(iconParams);
        card.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(getColor(R.color.text_primary));
        tvTitle.setMaxLines(2);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(getColor(R.color.text_secondary));
        tvSub.setMaxLines(2);
        tvSub.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(tvSub);

        card.addView(textCol);

        TextView tvArrow = new TextView(this);
        tvArrow.setText("→");
        tvArrow.setTextSize(16f);
        tvArrow.setTypeface(null, Typeface.BOLD);
        tvArrow.setTextColor(getColor(R.color.primary_700));
        tvArrow.setGravity(Gravity.CENTER);
        tvArrow.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        card.addView(tvArrow);

        card.setOnClickListener(v -> onClick.run());
        menuContainer.addView(card);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
