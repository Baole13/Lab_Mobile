package com.example.studentmanager.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.models.Order;
import com.example.studentmanager.utils.AuthManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Người 4 — OrderManagementActivity
 * Quản lý đơn hàng cho Admin theo Grocery App UI Design System:
 * - SearchBar 48dp (#F1F5F9 rounded-2xl)
 * - Horizontal Category/Status Chips (Pending / Confirmed / Completed)
 * - Admin Order Cards (#FFFFFF rounded-2xl border #E2E8F0) với nút CTA chuyển trạng thái trực tiếp:
 *   + Pending -> Confirmed
 *   + Confirmed -> Completed
 */
public class OrderManagementActivity extends Activity {
    private AuthManager auth;
    private DBHelper db;

    private TextView tvAdminOrderSummary;
    private EditText etSearchOrder;
    private LinearLayout adminOrdersCardContainer;

    private Button btnAll;
    private Button btnPending;
    private Button btnConfirmed;
    private Button btnCompleted;

    private String statusFilter = "ALL";
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthManager(this);
        if (!auth.isLoggedIn() || !auth.isAdmin()) {
            Toast.makeText(this, "Chỉ Admin mới có quyền quản lý đơn hàng!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = new DBHelper(this);
        setContentView(R.layout.activity_order_management);

        tvAdminOrderSummary = findViewById(R.id.tvAdminOrderSummary);
        etSearchOrder = findViewById(R.id.etSearchOrder);
        adminOrdersCardContainer = findViewById(R.id.adminOrdersCardContainer);

        btnAll = findViewById(R.id.btnAdminFilterAll);
        btnPending = findViewById(R.id.btnAdminFilterPending);
        btnConfirmed = findViewById(R.id.btnAdminFilterConfirmed);
        btnCompleted = findViewById(R.id.btnAdminFilterCompleted);
        Button btnBack = findViewById(R.id.btnAdminOrderBack);

        btnAll.setOnClickListener(v -> setStatusFilter("ALL"));
        btnPending.setOnClickListener(v -> setStatusFilter(Order.STATUS_PENDING));
        btnConfirmed.setOnClickListener(v -> setStatusFilter(Order.STATUS_CONFIRMED));
        btnCompleted.setOnClickListener(v -> setStatusFilter(Order.STATUS_COMPLETED));
        btnBack.setOnClickListener(v -> finish());

        etSearchOrder.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshOrders();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) {
            refreshOrders();
        }
    }

    private void setStatusFilter(String filter) {
        statusFilter = filter;
        updateChipStyles();
        refreshOrders();
    }

    private void updateChipStyles() {
        styleChip(btnAll, "ALL".equals(statusFilter));
        styleChip(btnPending, Order.STATUS_PENDING.equalsIgnoreCase(statusFilter));
        styleChip(btnConfirmed, Order.STATUS_CONFIRMED.equalsIgnoreCase(statusFilter));
        styleChip(btnCompleted, Order.STATUS_COMPLETED.equalsIgnoreCase(statusFilter));
    }

    private void styleChip(Button chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chip.setTextColor(Color.parseColor(active ? "#047857" : "#64748B"));
        chip.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void refreshOrders() {
        updateChipStyles();
        adminOrdersCardContainer.removeAllViews();

        List<Order> allOrders = db.getAllOrders();
        int pendingCount = 0;
        int confirmedCount = 0;
        int completedCount = 0;
        String keyword = etSearchOrder.getText() == null ? "" : etSearchOrder.getText().toString().trim().toLowerCase(Locale.ROOT);

        List<Order> displayedOrders = new ArrayList<>();
        for (Order o : allOrders) {
            if (Order.STATUS_PENDING.equalsIgnoreCase(o.status)) pendingCount++;
            else if (Order.STATUS_CONFIRMED.equalsIgnoreCase(o.status)) confirmedCount++;
            else if (Order.STATUS_COMPLETED.equalsIgnoreCase(o.status)) completedCount++;

            boolean matchesStatus = "ALL".equals(statusFilter) || statusFilter.equalsIgnoreCase(o.status);
            if (!matchesStatus) continue;

            if (!keyword.isEmpty()) {
                String idStr = "#" + o.id;
                String custName = o.customerName == null ? "" : o.customerName.toLowerCase(Locale.ROOT);
                String dateStr = o.orderDate == null ? "" : o.orderDate.toLowerCase(Locale.ROOT);
                if (!idStr.contains(keyword) && !custName.contains(keyword) && !dateStr.contains(keyword)) {
                    continue;
                }
            }
            displayedOrders.add(o);
        }

        tvAdminOrderSummary.setText("Tổng: " + allOrders.size()
                + " (Pending: " + pendingCount
                + " | Confirmed: " + confirmedCount
                + " | Completed: " + completedCount + ")");

        if (displayedOrders.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Không có đơn hàng nào khớp bộ lọc.");
            tvEmpty.setTextColor(Color.parseColor("#64748B"));
            tvEmpty.setTextSize(14f);
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(dp(16), dp(36), dp(16), dp(36));
            adminOrdersCardContainer.addView(tvEmpty);
            return;
        }

        for (Order order : displayedOrders) {
            adminOrdersCardContainer.addView(createAdminOrderCard(order));
        }
    }

    private View createAdminOrderCard(Order order) {
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

        // Top Row: Order ID + Status Badge
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        String customerDisplay = (order.customerName != null && !order.customerName.isEmpty())
                ? order.customerName + " (KH #" + order.customerId + ")"
                : "Khách hàng #" + order.customerId;

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Đơn #" + order.id + " • " + customerDisplay);
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(tvTitle);

        TextView tvBadge = new TextView(this);
        tvBadge.setText(order.status.toUpperCase(Locale.ROOT));
        tvBadge.setTextSize(11f);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        applyStatusBadgeStyle(tvBadge, order.status);
        topRow.addView(tvBadge);

        card.addView(topRow);

        // Row 2: Order Date
        TextView tvMeta = new TextView(this);
        tvMeta.setText("Ngày đặt: " + order.orderDate);
        tvMeta.setTextSize(12f);
        tvMeta.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(4);
        metaParams.bottomMargin = dp(10);
        tvMeta.setLayoutParams(metaParams);
        card.addView(tvMeta);

        // Row 3: Price (Emerald bold) + Next Status Action Button (40dp+ touch target)
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvPrice = new TextView(this);
        tvPrice.setText(currencyFormat.format(order.totalAmount) + "đ / đơn");
        tvPrice.setTextSize(16f);
        tvPrice.setTypeface(null, Typeface.BOLD);
        tvPrice.setTextColor(Color.parseColor("#059669"));
        tvPrice.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        actionRow.addView(tvPrice);

        String nextStatus = order.getNextAllowedStatus();
        Button btnAction = new Button(this);
        btnAction.setAllCaps(false);
        btnAction.setTextSize(12f);
        btnAction.setTypeface(null, Typeface.BOLD);
        btnAction.setPadding(dp(14), 0, dp(14), 0);
        btnAction.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
        ));

        if (Order.STATUS_CONFIRMED.equals(nextStatus)) {
            btnAction.setText("Xác nhận → Confirmed");
            btnAction.setTextColor(Color.WHITE);
            btnAction.setBackgroundResource(R.drawable.bg_btn_primary);
            btnAction.setOnClickListener(v -> confirmTransition(order, Order.STATUS_CONFIRMED));
        } else if (Order.STATUS_COMPLETED.equals(nextStatus)) {
            btnAction.setText("Hoàn thành → Completed");
            btnAction.setTextColor(Color.WHITE);
            btnAction.setBackgroundResource(R.drawable.bg_btn_primary);
            btnAction.setOnClickListener(v -> confirmTransition(order, Order.STATUS_COMPLETED));
        } else {
            btnAction.setText("✓ Đã hoàn thành");
            btnAction.setEnabled(false);
            btnAction.setTextColor(Color.parseColor("#047857"));
            btnAction.setBackgroundResource(R.drawable.bg_badge_success);
        }

        actionRow.addView(btnAction);
        card.addView(actionRow);

        card.setOnClickListener(v -> showStatusUpdateDialog(order));
        return card;
    }

    private void confirmTransition(Order order, String nextStatus) {
        boolean updated = db.updateOrderStatus(order.id, nextStatus);
        if (updated) {
            Toast.makeText(this,
                    "Đơn #" + order.id + ": " + order.status + " → " + nextStatus,
                    Toast.LENGTH_SHORT).show();
            refreshOrders();
        } else {
            Toast.makeText(this, "Không thể cập nhật trạng thái đơn!", Toast.LENGTH_SHORT).show();
        }
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

    private void showStatusUpdateDialog(Order order) {
        String nextStatus = order.getNextAllowedStatus();
        String customerDisplay = (order.customerName != null && !order.customerName.isEmpty())
                ? order.customerName + " (KH #" + order.customerId + ")"
                : "Khách hàng #" + order.customerId;

        String info = "Mã đơn: #" + order.id + "\n"
                + "Khách hàng: " + customerDisplay + "\n"
                + "Ngày đặt: " + order.orderDate + "\n"
                + "Tổng tiền: " + currencyFormat.format(order.totalAmount) + "đ\n"
                + "Trạng thái hiện tại: " + order.status;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Chi tiết & Duyệt đơn #" + order.id);

        if (nextStatus != null) {
            String actionLabel = Order.STATUS_CONFIRMED.equals(nextStatus)
                    ? "XÁC NHẬN ĐƠN (→ Confirmed)"
                    : "HOÀN THÀNH ĐƠN (→ Completed)";

            builder.setMessage(info + "\n\nChuyển trạng thái tiếp theo: " + order.status + " → " + nextStatus)
                    .setPositiveButton(actionLabel, (dialog, which) -> confirmTransition(order, nextStatus))
                    .setNegativeButton("ĐÓNG", null);
        } else {
            builder.setMessage(info + "\n\nĐơn hàng này đã ở trạng thái Completed (Hoàn thành).")
                    .setPositiveButton("ĐÓNG", null);
        }

        builder.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
