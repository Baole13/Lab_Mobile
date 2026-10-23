package com.example.studentmanager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.studentmanager.activities.CustomerProfileActivity;
import com.example.studentmanager.activities.FruitFormActivity;
import com.example.studentmanager.activities.FruitListActivity;
import com.example.studentmanager.activities.LoginActivity;
import com.example.studentmanager.activities.OrderHistoryActivity;
import com.example.studentmanager.activities.OrderManagementActivity;
import com.example.studentmanager.activities.PlaceOrderActivity;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.models.Customer;
import com.example.studentmanager.models.Fruit;
import com.example.studentmanager.models.Order;
import com.example.studentmanager.utils.AuthManager;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Người 4 — MainActivity (Dashboard Admin/Customer & Integration)
 * Áp dụng Grocery App UI Design System Tokens (Emerald #10B981, Slate #0F172A, Card #FFFFFF).
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
            tvRoleSubtitle.setText("ADMIN DASHBOARD");
            tvGreeting.setText("Xin chào, " + auth.getUsername() + " 👋");

            addActionCard(
                    "🍎",
                    "Quản lý Trái Cây (Fruit CRUD)",
                    "Thêm, sửa, xóa, tìm kiếm & kiểm tra hạn sử dụng",
                    FruitListActivity.class
            );
            addActionCard(
                    "➕",
                    "Thêm Trái Cây Mới",
                    "Tạo mới trái cây (kiểm tra price > 0, hạn dùng)",
                    FruitFormActivity.class
            );
            addActionCard(
                    "📦",
                    "Quản lý Đơn Hàng (Order Management)",
                    "Duyệt trạng thái đơn: Pending → Confirmed → Completed",
                    OrderManagementActivity.class
            );
        } else {
            Customer customer = db.getCustomerByAccountId(auth.getAccountId());
            String displayName = (customer != null && customer.name != null && !customer.name.isEmpty())
                    ? customer.name
                    : auth.getUsername();

            tvRoleSubtitle.setText("CUSTOMER DASHBOARD");
            tvGreeting.setText("Xin chào, " + displayName + " 🛒");

            addActionCard(
                    "🛒",
                    "Đặt Mua Trái Cây (Place Order)",
                    "Chọn trái cây còn hạn & còn hàng, tính tổng tiền tự động",
                    PlaceOrderActivity.class
            );
            addActionCard(
                    "📋",
                    "Lịch Sử Đơn Hàng (Order History)",
                    "Theo dõi trạng thái đơn Pending / Confirmed / Completed",
                    OrderHistoryActivity.class
            );
            addActionCard(
                    "🍉",
                    "Xem & Tìm Kiếm Trái Cây",
                    "Tra cứu danh mục trái cây, giá bán và hạn sử dụng",
                    FruitListActivity.class
            );
            addActionCard(
                    "👤",
                    "Thông Tin Cá Nhân (Customer Profile)",
                    "Xem và cập nhật họ tên, số điện thoại, địa chỉ",
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
            } else if (f.isOutOfStock()) {
                outOfStockFruits++;
            } else {
                availableFruits++;
            }
        }

        if (auth.isAdmin()) {
            List<Order> allOrders = db.getAllOrders();
            int pendingCount = 0;
            int confirmedCount = 0;
            int completedCount = 0;
            double completedRevenue = 0;

            for (Order o : allOrders) {
                if (Order.STATUS_PENDING.equalsIgnoreCase(o.status)) pendingCount++;
                else if (Order.STATUS_CONFIRMED.equalsIgnoreCase(o.status)) confirmedCount++;
                else if (Order.STATUS_COMPLETED.equalsIgnoreCase(o.status)) {
                    completedCount++;
                    completedRevenue += o.totalAmount;
                }
            }

            String summary = "• Trái cây: " + fruits.size() + " loại (Còn bán: " + availableFruits
                    + " | Hết hạn: " + expiredFruits + " | Hết hàng: " + outOfStockFruits + ")\n"
                    + "• Đơn hàng: " + allOrders.size() + " đơn (Pending: " + pendingCount
                    + " | Confirmed: " + confirmedCount + " | Completed: " + completedCount + ")\n"
                    + "• Doanh thu hoàn thành: " + currencyFormat.format(completedRevenue) + "đ";
            tvDashboardSummary.setText(summary);
        } else {
            int customerId = auth.getCustomerId();
            List<Order> myOrders = customerId != -1 ? db.getOrdersByCustomer(customerId) : Collections.emptyList();
            int pendingCount = 0;
            for (Order o : myOrders) {
                if (Order.STATUS_PENDING.equalsIgnoreCase(o.status)) pendingCount++;
            }
            String summary = "• Trái cây tươi sẵn sàng đặt: " + availableFruits + " loại\n"
                    + "• Đơn hàng của bạn: " + myOrders.size() + " đơn (Chờ duyệt: " + pendingCount + ")\n"
                    + "• Ngày hệ thống: " + Fruit.todayString();
            tvDashboardSummary.setText(summary);
        }
    }

    private void addActionCard(String iconEmoji, String title, String subtitle, Class<?> targetActivity) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(14);
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        // Fallback Icon Container (48x48dp rounded-xl #ECFDF5)
        TextView tvIcon = new TextView(this);
        tvIcon.setText(iconEmoji);
        tvIcon.setTextSize(20f);
        tvIcon.setGravity(Gravity.CENTER);
        tvIcon.setBackgroundResource(R.drawable.bg_image_placeholder);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconParams.rightMargin = dp(12);
        tvIcon.setLayoutParams(iconParams);
        card.addView(tvIcon);

        // Text Column
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textCol.setLayoutParams(textParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setMaxLines(2);
        tvSub.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(tvSub);

        card.addView(textCol);

        // Right Arrow Button (36x36dp hit target)
        TextView tvArrow = new TextView(this);
        tvArrow.setText("→");
        tvArrow.setTextSize(16f);
        tvArrow.setTypeface(null, Typeface.BOLD);
        tvArrow.setTextColor(Color.parseColor("#059669"));
        tvArrow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        tvArrow.setLayoutParams(arrowParams);
        card.addView(tvArrow);

        card.setOnClickListener(v -> startActivity(new Intent(this, targetActivity)));
        menuContainer.addView(card);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
