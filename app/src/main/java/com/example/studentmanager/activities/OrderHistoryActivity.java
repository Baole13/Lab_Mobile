package com.example.studentmanager.activities;

import android.app.Activity;
import android.app.AlertDialog;
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

import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.models.Customer;
import com.example.studentmanager.models.Order;
import com.example.studentmanager.utils.AuthManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Người 4 — OrderHistoryActivity
 * Hiển thị lịch sử đơn hàng của Customer theo Grocery App UI Design System:
 * - Horizontal Category/Status Chips (Active: #ECFDF5 + #047857; Inactive: #F1F5F9 + #64748B)
 * - Order Cards (#FFFFFF, rounded 16dp, border #E2E8F0, p-3.5 = 14dp)
 * - Formatted currency (240.000đ) và Status Badges rõ ràng.
 */
public class OrderHistoryActivity extends Activity {
    private AuthManager auth;
    private DBHelper db;
    private Customer currentCustomer;

    private TextView tvHistorySubtitle;
    private LinearLayout orderHistoryCardContainer;
    private Button btnFilterAll;
    private Button btnFilterPending;
    private Button btnFilterConfirmed;
    private Button btnFilterCompleted;

    private String currentFilterStatus = "ALL";
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
        currentCustomer = db.getCustomerByAccountId(auth.getAccountId());

        setContentView(R.layout.activity_order_history);

        tvHistorySubtitle = findViewById(R.id.tvHistorySubtitle);
        orderHistoryCardContainer = findViewById(R.id.orderHistoryCardContainer);

        btnFilterAll = findViewById(R.id.btnHistoryFilterAll);
        btnFilterPending = findViewById(R.id.btnHistoryFilterPending);
        btnFilterConfirmed = findViewById(R.id.btnHistoryFilterConfirmed);
        btnFilterCompleted = findViewById(R.id.btnHistoryFilterCompleted);
        Button btnNewOrder = findViewById(R.id.btnHistoryNewOrder);
        Button btnBack = findViewById(R.id.btnHistoryBack);

        btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
        btnFilterPending.setOnClickListener(v -> setFilter(Order.STATUS_PENDING));
        btnFilterConfirmed.setOnClickListener(v -> setFilter(Order.STATUS_CONFIRMED));
        btnFilterCompleted.setOnClickListener(v -> setFilter(Order.STATUS_COMPLETED));

        btnNewOrder.setOnClickListener(v ->
                startActivity(new Intent(this, PlaceOrderActivity.class)));
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) {
            refreshOrderHistory();
        }
    }

    private void setFilter(String status) {
        currentFilterStatus = status;
        updateChipStyles();
        refreshOrderHistory();
    }

    private void updateChipStyles() {
        styleChip(btnFilterAll, "ALL".equals(currentFilterStatus));
        styleChip(btnFilterPending, Order.STATUS_PENDING.equalsIgnoreCase(currentFilterStatus));
        styleChip(btnFilterConfirmed, Order.STATUS_CONFIRMED.equalsIgnoreCase(currentFilterStatus));
        styleChip(btnFilterCompleted, Order.STATUS_COMPLETED.equalsIgnoreCase(currentFilterStatus));
    }

    private void styleChip(Button chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chip.setTextColor(Color.parseColor(active ? "#047857" : "#64748B"));
        chip.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void refreshOrderHistory() {
        updateChipStyles();
        orderHistoryCardContainer.removeAllViews();

        List<Order> sourceOrders = currentCustomer != null
                ? db.getOrdersByCustomer(currentCustomer.id)
                : new ArrayList<>();

        int pendingCount = 0;
        int confirmedCount = 0;
        int completedCount = 0;
        List<Order> filtered = new ArrayList<>();

        for (Order order : sourceOrders) {
            if (Order.STATUS_PENDING.equalsIgnoreCase(order.status)) pendingCount++;
            else if (Order.STATUS_CONFIRMED.equalsIgnoreCase(order.status)) confirmedCount++;
            else if (Order.STATUS_COMPLETED.equalsIgnoreCase(order.status)) completedCount++;

            if ("ALL".equals(currentFilterStatus) || currentFilterStatus.equalsIgnoreCase(order.status)) {
                filtered.add(order);
            }
        }

        String customerLabel = currentCustomer != null
                ? currentCustomer.name + " (#" + currentCustomer.id + ")"
                : auth.getUsername();

        tvHistorySubtitle.setText(customerLabel + " • Tổng: " + sourceOrders.size()
                + " (Pending: " + pendingCount
                + " | Confirmed: " + confirmedCount
                + " | Completed: " + completedCount + ")");

        if (filtered.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Chưa có đơn hàng nào trong danh mục này.");
            tvEmpty.setTextColor(Color.parseColor("#64748B"));
            tvEmpty.setTextSize(14f);
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(dp(16), dp(36), dp(16), dp(36));
            orderHistoryCardContainer.addView(tvEmpty);
            return;
        }

        for (Order order : filtered) {
            orderHistoryCardContainer.addView(createOrderCard(order));
        }
    }

    private View createOrderCard(Order order) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(14);
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        // Row 1: Order ID + Status Badge
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvOrderId = new TextView(this);
        tvOrderId.setText("Đơn hàng #" + order.id);
        tvOrderId.setTextSize(16f);
        tvOrderId.setTypeface(null, Typeface.BOLD);
        tvOrderId.setTextColor(Color.parseColor("#0F172A"));
        tvOrderId.setMaxLines(1);
        tvOrderId.setEllipsize(TextUtils.TruncateAt.END);
        tvOrderId.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(tvOrderId);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(order.status.toUpperCase(Locale.ROOT));
        tvStatus.setTextSize(11f);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setPadding(dp(10), dp(4), dp(10), dp(4));
        applyStatusBadgeStyle(tvStatus, order.status);
        topRow.addView(tvStatus);

        card.addView(topRow);

        // Row 2: Order Date
        TextView tvDate = new TextView(this);
        tvDate.setText("Ngày đặt: " + order.orderDate + " • Mã KH: #" + order.customerId);
        tvDate.setTextSize(12f);
        tvDate.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateParams.topMargin = dp(6);
        dateParams.bottomMargin = dp(10);
        tvDate.setLayoutParams(dateParams);
        card.addView(tvDate);

        // Row 3: Total Price (Emerald #059669 bold) + Detail Link
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTotal = new TextView(this);
        tvTotal.setText(currencyFormat.format(order.totalAmount) + "đ / đơn");
        tvTotal.setTextSize(16f);
        tvTotal.setTypeface(null, Typeface.BOLD);
        tvTotal.setTextColor(Color.parseColor("#059669"));
        tvTotal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bottomRow.addView(tvTotal);

        TextView tvViewDetail = new TextView(this);
        tvViewDetail.setText("Xem chi tiết →");
        tvViewDetail.setTextSize(12f);
        tvViewDetail.setTypeface(null, Typeface.BOLD);
        tvViewDetail.setTextColor(Color.parseColor("#047857"));
        bottomRow.addView(tvViewDetail);

        card.addView(bottomRow);
        card.setOnClickListener(v -> showOrderDetailDialog(order));
        return card;
    }

    private void applyStatusBadgeStyle(TextView badge, String status) {
        if (Order.STATUS_PENDING.equalsIgnoreCase(status)) {
            badge.setBackgroundResource(R.drawable.bg_badge_warning);
            badge.setTextColor(Color.parseColor("#D97706"));
        } else if (Order.STATUS_CONFIRMED.equalsIgnoreCase(status)) {
            badge.setBackgroundResource(R.drawable.bg_badge_confirmed);
            badge.setTextColor(Color.parseColor("#1D4ED8"));
        } else {
            badge.setBackgroundResource(R.drawable.bg_badge_success);
            badge.setTextColor(Color.parseColor("#047857"));
        }
    }

    private void showOrderDetailDialog(Order order) {
        String statusExplanation;
        if (Order.STATUS_PENDING.equalsIgnoreCase(order.status)) {
            statusExplanation = "Pending (Đang chờ Admin xác nhận)";
        } else if (Order.STATUS_CONFIRMED.equalsIgnoreCase(order.status)) {
            statusExplanation = "Confirmed (Đã xác nhận, đang chuẩn bị giao)";
        } else {
            statusExplanation = "Completed (Đơn hàng đã hoàn thành)";
        }

        String message = "Mã đơn hàng: #" + order.id + "\n"
                + "Mã khách hàng (customerId): #" + order.customerId + "\n"
                + "Khách hàng: " + (order.customerName != null ? order.customerName : (currentCustomer != null ? currentCustomer.name : "")) + "\n"
                + "Ngày đặt hàng: " + order.orderDate + "\n"
                + "Tổng tiền: " + currencyFormat.format(order.totalAmount) + "đ\n"
                + "Trạng thái: " + statusExplanation;

        new AlertDialog.Builder(this)
                .setTitle("Chi tiết đơn hàng #" + order.id)
                .setMessage(message)
                .setPositiveButton("ĐÓNG", null)
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
