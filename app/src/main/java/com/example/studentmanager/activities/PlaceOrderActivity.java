package com.example.studentmanager.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
import com.example.studentmanager.models.Customer;
import com.example.studentmanager.models.Fruit;
import com.example.studentmanager.utils.AuthManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Người 4 — PlaceOrderActivity
 * Áp dụng đầy đủ Grocery App UI Design System Tokens:
 * - 2-column Product Grid (grid-cols-2, gap 14dp)
 * - ProductCard (#FFFFFF, rounded 16dp, border #E2E8F0, p-3.5 = 14dp)
 * - Top-left status badge (Expired / Out of stock: #FEE2E2 bg + #DC2626 text; Available: #ECFDF5 bg + #047857 text)
 * - Fallback solid color placeholder with fruit icon (Rule 5.4)
 * - Single-line text truncation (Rule 5.2) & Formatted price with thousand separators + unit suffix (Rule 5.3: 120.000đ / kg)
 * - QuantityStepper (Rule 3.D): khi quantity = 1 nút trừ chuyển thành icon thùng rác 🗑 màu đỏ (#EF4444)
 * - Horizontal Category Chip Bar (Rule 2.3 & 3.B) & Sticky Bottom CheckoutBar (Rule 3.E)
 */
public class PlaceOrderActivity extends Activity {
    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_AVAILABLE = "AVAILABLE";
    private static final String FILTER_TODAY = "TODAY";
    private static final String FILTER_UNAVAILABLE = "UNAVAILABLE";

    private AuthManager auth;
    private DBHelper db;
    private Customer currentCustomer;

    private EditText etSearchFruit;
    private LinearLayout categoryChipContainer;
    private LinearLayout fruitOrderContainer;
    private TextView tvCustomerInfo;
    private TextView tvSelectedSummary;
    private TextView tvTotalAmount;
    private Button btnSubmitOrder;

    private String activeFilter = FILTER_ALL;
    private final Map<Integer, Integer> selectedQuantities = new LinkedHashMap<>();
    private final Map<Integer, Fruit> fruitMap = new LinkedHashMap<>();
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

        setContentView(R.layout.activity_place_order);

        tvCustomerInfo = findViewById(R.id.tvCustomerInfo);
        etSearchFruit = findViewById(R.id.etSearchFruit);
        categoryChipContainer = findViewById(R.id.categoryChipContainer);
        fruitOrderContainer = findViewById(R.id.fruitOrderContainer);
        tvSelectedSummary = findViewById(R.id.tvSelectedSummary);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnSubmitOrder = findViewById(R.id.btnSubmitOrder);
        Button btnViewOrderHistory = findViewById(R.id.btnViewOrderHistory);
        Button btnBack = findViewById(R.id.btnBackFromPlaceOrder);

        if (currentCustomer != null) {
            tvCustomerInfo.setText(currentCustomer.name + " (KH #" + currentCustomer.id + ") • " + Fruit.todayString());
        } else {
            tvCustomerInfo.setText(auth.getUsername() + " • " + Fruit.todayString());
        }

        int preselectedFruitId = getIntent().getIntExtra("fruitId", -1);
        if (preselectedFruitId != -1) {
            Fruit preFruit = db.getFruitById(preselectedFruitId);
            if (preFruit != null && preFruit.canOrder()) {
                selectedQuantities.put(preselectedFruitId, 1);
            }
        }

        etSearchFruit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderFruitGrid();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSubmitOrder.setOnClickListener(v -> confirmAndSubmitOrder());
        btnViewOrderHistory.setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class)));
        btnBack.setOnClickListener(v -> finish());

        renderCategoryChips();
        loadFruitsAndRender();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) {
            loadFruitsAndRender();
        }
    }

    private void renderCategoryChips() {
        categoryChipContainer.removeAllViews();
        addChip("🍎 Tất cả", FILTER_ALL);
        addChip("✅ Còn đặt được", FILTER_AVAILABLE);
        addChip("⏳ Hạn hôm nay", FILTER_TODAY);
        addChip("⚠️ Hết hạn / Hết hàng", FILTER_UNAVAILABLE);
    }

    private void addChip(String label, String filterCode) {
        Button chip = new Button(this);
        chip.setText(label);
        chip.setAllCaps(false);
        chip.setTextSize(12f);

        boolean isActive = filterCode.equals(activeFilter);
        chip.setBackgroundResource(isActive ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chip.setTextColor(Color.parseColor(isActive ? "#047857" : "#64748B"));
        chip.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
        chip.setPadding(dp(14), 0, dp(14), 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );
        params.rightMargin = dp(10); // gap-2.5 (10px)
        chip.setLayoutParams(params);

        chip.setOnClickListener(v -> {
            activeFilter = filterCode;
            renderCategoryChips();
            renderFruitGrid();
        });

        categoryChipContainer.addView(chip);
    }

    private void loadFruitsAndRender() {
        fruitMap.clear();
        List<Fruit> allFruits = db.getAllFruits();
        for (Fruit f : allFruits) {
            fruitMap.put(f.id, f);
            Integer chosen = selectedQuantities.get(f.id);
            if (chosen != null) {
                if (!f.canOrder()) {
                    selectedQuantities.remove(f.id);
                } else if (chosen > f.quantity) {
                    selectedQuantities.put(f.id, f.quantity);
                }
            }
        }
        renderFruitGrid();
        updateOrderSummary();
    }

    private void renderFruitGrid() {
        fruitOrderContainer.removeAllViews();
        String keyword = etSearchFruit.getText() == null ? "" : etSearchFruit.getText().toString().trim().toLowerCase(Locale.ROOT);

        List<Fruit> filtered = new ArrayList<>();
        for (Fruit fruit : fruitMap.values()) {
            if (!keyword.isEmpty() && !fruit.name.toLowerCase(Locale.ROOT).contains(keyword)) {
                continue;
            }
            if (FILTER_AVAILABLE.equals(activeFilter) && !fruit.canOrder()) continue;
            if (FILTER_TODAY.equals(activeFilter) && !fruit.isExpiringToday()) continue;
            if (FILTER_UNAVAILABLE.equals(activeFilter) && fruit.canOrder()) continue;
            filtered.add(fruit);
        }

        if (filtered.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Không tìm thấy trái cây phù hợp.");
            tvEmpty.setTextColor(Color.parseColor("#64748B"));
            tvEmpty.setTextSize(14f);
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(dp(16), dp(32), dp(16), dp(32));
            fruitOrderContainer.addView(tvEmpty);
            return;
        }

        // Render 2-column Product Grid (grid-cols-2 gap-3.5)
        for (int i = 0; i < filtered.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(2f);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = dp(14);
            row.setLayoutParams(rowParams);

            View leftCard = createProductCard(filtered.get(i), true);
            row.addView(leftCard);

            if (i + 1 < filtered.size()) {
                View rightCard = createProductCard(filtered.get(i + 1), false);
                row.addView(rightCard);
            } else {
                View emptySpacer = new View(this);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 1, 1f);
                spacerParams.leftMargin = dp(7);
                emptySpacer.setLayoutParams(spacerParams);
                row.addView(emptySpacer);
            }

            fruitOrderContainer.addView(row);
        }
    }

    private View createProductCard(Fruit fruit, boolean isLeftColumn) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(14); // p-3.5
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        if (isLeftColumn) {
            cardParams.rightMargin = dp(7);
        } else {
            cardParams.leftMargin = dp(7);
        }
        card.setLayoutParams(cardParams);

        boolean expired = fruit.isExpired();
        boolean outOfStock = fruit.isOutOfStock();
        boolean canOrder = fruit.canOrder();

        // 1. Top-left Status Badge
        TextView tvBadge = new TextView(this);
        tvBadge.setTextSize(11f);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.bottomMargin = dp(8);
        tvBadge.setLayoutParams(badgeParams);

        if (expired) {
            tvBadge.setText("EXPIRED");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_error);
            tvBadge.setTextColor(Color.parseColor("#DC2626"));
        } else if (outOfStock) {
            tvBadge.setText("OUT OF STOCK");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_error);
            tvBadge.setTextColor(Color.parseColor("#DC2626"));
        } else if (fruit.isExpiringToday()) {
            tvBadge.setText("HẠN HÔM NAY");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_warning);
            tvBadge.setTextColor(Color.parseColor("#D97706"));
        } else {
            tvBadge.setText("CÒN HẠN");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_success);
            tvBadge.setTextColor(Color.parseColor("#047857"));
        }
        card.addView(tvBadge);

        // 2. Fallback Image Placeholder with Icon (Rule 5.4)
        TextView tvPlaceholder = new TextView(this);
        tvPlaceholder.setText(resolveFruitEmoji(fruit.name));
        tvPlaceholder.setTextSize(34f);
        tvPlaceholder.setGravity(Gravity.CENTER);
        tvPlaceholder.setBackgroundResource(canOrder ? R.drawable.bg_image_placeholder : R.drawable.bg_badge_error);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(84)
        );
        imgParams.bottomMargin = dp(10);
        tvPlaceholder.setLayoutParams(imgParams);
        card.addView(tvPlaceholder);

        // 3. Product Name (Rule 5.2: line-clamp-1)
        TextView tvName = new TextView(this);
        tvName.setText(fruit.name);
        tvName.setTextSize(14f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#0F172A"));
        tvName.setMaxLines(1);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(tvName);

        // 4. Unit/Weight + Stock + Expiry
        TextView tvUnitMeta = new TextView(this);
        tvUnitMeta.setText("1 kg • Kho: " + fruit.quantity + "\nHSD: " + fruit.expiryDate);
        tvUnitMeta.setTextSize(12f);
        tvUnitMeta.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(2);
        metaParams.bottomMargin = dp(8);
        tvUnitMeta.setLayoutParams(metaParams);
        card.addView(tvUnitMeta);

        // 5. Price Tag (Rule 3.A.5 & 5.3: Emerald #059669 + unit suffix)
        TextView tvPrice = new TextView(this);
        tvPrice.setText(currencyFormat.format(fruit.price) + "đ / kg");
        tvPrice.setTextSize(15f);
        tvPrice.setTypeface(null, Typeface.BOLD);
        tvPrice.setTextColor(Color.parseColor("#059669"));
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        priceParams.bottomMargin = dp(10);
        tvPrice.setLayoutParams(priceParams);
        card.addView(tvPrice);

        // 6. Action Row: Quick Add (+) OR QuantityStepper (Rule 3.D)
        int currentQty = selectedQuantities.containsKey(fruit.id) ? selectedQuantities.get(fruit.id) : 0;

        if (!canOrder) {
            Button btnDisabled = new Button(this);
            btnDisabled.setText(expired ? "Hết hạn" : "Hết hàng");
            btnDisabled.setAllCaps(false);
            btnDisabled.setEnabled(false);
            btnDisabled.setTextSize(12f);
            btnDisabled.setTextColor(Color.parseColor("#94A3B8"));
            btnDisabled.setBackgroundResource(R.drawable.bg_stepper_container);
            btnDisabled.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(38)
            ));
            card.addView(btnDisabled);
        } else if (currentQty == 0) {
            Button btnQuickAdd = new Button(this);
            btnQuickAdd.setText("+ Thêm vào đơn");
            btnQuickAdd.setAllCaps(false);
            btnQuickAdd.setTextSize(13f);
            btnQuickAdd.setTypeface(null, Typeface.BOLD);
            btnQuickAdd.setTextColor(Color.WHITE);
            btnQuickAdd.setBackgroundResource(R.drawable.bg_btn_add);
            btnQuickAdd.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(38) // >= 36dp touch target
            ));
            btnQuickAdd.setOnClickListener(v -> {
                selectedQuantities.put(fruit.id, 1);
                renderFruitGrid();
                updateOrderSummary();
            });
            card.addView(btnQuickAdd);
        } else {
            // Quantity Stepper (Rule 3.D: bg-slate-100 rounded-xl p-1; if qty=1 minus becomes Trash icon)
            LinearLayout stepper = new LinearLayout(this);
            stepper.setOrientation(LinearLayout.HORIZONTAL);
            stepper.setGravity(Gravity.CENTER_VERTICAL);
            stepper.setBackgroundResource(R.drawable.bg_stepper_container);
            int p1 = dp(4);
            stepper.setPadding(p1, p1, p1, p1);
            stepper.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(42)
            ));

            Button btnMinus = new Button(this);
            // Rule 3.D: If quantity = 1, minus icon changes to a Trash can icon (text-red-500)
            btnMinus.setText(currentQty == 1 ? "🗑" : "–");
            btnMinus.setTextColor(Color.parseColor(currentQty == 1 ? "#EF4444" : "#0F172A"));
            btnMinus.setTextSize(14f);
            btnMinus.setTypeface(null, Typeface.BOLD);
            btnMinus.setPadding(0, 0, 0, 0);
            btnMinus.setBackgroundResource(R.drawable.bg_stepper_btn_white);
            btnMinus.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(34)));

            TextView tvCount = new TextView(this);
            tvCount.setText(String.valueOf(currentQty));
            tvCount.setTextSize(14f);
            tvCount.setTypeface(null, Typeface.BOLD);
            tvCount.setTextColor(Color.parseColor("#0F172A"));
            tvCount.setGravity(Gravity.CENTER);
            tvCount.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button btnPlus = new Button(this);
            btnPlus.setText("+");
            btnPlus.setTextColor(Color.WHITE);
            btnPlus.setTextSize(15f);
            btnPlus.setTypeface(null, Typeface.BOLD);
            btnPlus.setPadding(0, 0, 0, 0);
            btnPlus.setBackgroundResource(R.drawable.bg_btn_add);
            btnPlus.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(34)));

            btnMinus.setOnClickListener(v -> {
                int q = selectedQuantities.containsKey(fruit.id) ? selectedQuantities.get(fruit.id) : 0;
                if (q <= 1) {
                    selectedQuantities.remove(fruit.id);
                } else {
                    selectedQuantities.put(fruit.id, q - 1);
                }
                renderFruitGrid();
                updateOrderSummary();
            });

            btnPlus.setOnClickListener(v -> {
                int q = selectedQuantities.containsKey(fruit.id) ? selectedQuantities.get(fruit.id) : 0;
                if (q >= fruit.quantity) {
                    Toast.makeText(this, "Đã đạt tối đa tồn kho (" + fruit.quantity + " kg)!", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedQuantities.put(fruit.id, q + 1);
                renderFruitGrid();
                updateOrderSummary();
            });

            stepper.addView(btnMinus);
            stepper.addView(tvCount);
            stepper.addView(btnPlus);
            card.addView(stepper);
        }

        return card;
    }

    private String resolveFruitEmoji(String name) {
        if (name == null) return "🍎";
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("nho")) return "🍇";
        if (lower.contains("dâu")) return "🍓";
        if (lower.contains("cam") || lower.contains("quýt")) return "🍊";
        if (lower.contains("chuối")) return "🍌";
        if (lower.contains("xoài")) return "🥭";
        if (lower.contains("dưa")) return "🍉";
        return "🍎";
    }

    private void updateOrderSummary() {
        double total = 0;
        int totalUnits = 0;
        StringBuilder details = new StringBuilder();

        for (Map.Entry<Integer, Integer> entry : selectedQuantities.entrySet()) {
            int qty = entry.getValue() == null ? 0 : entry.getValue();
            if (qty <= 0) continue;
            Fruit fruit = fruitMap.get(entry.getKey());
            if (fruit == null || !fruit.canOrder()) continue;

            double lineTotal = fruit.price * qty;
            total += lineTotal;
            totalUnits += qty;
            if (details.length() > 0) details.append(" • ");
            details.append(fruit.name).append(" x").append(qty);
        }

        if (totalUnits == 0) {
            tvSelectedSummary.setText("Chưa chọn trái cây nào (Trái cây Expired hoặc Out of stock đã được khóa)");
            tvTotalAmount.setText("0đ");
            btnSubmitOrder.setEnabled(false);
        } else {
            tvSelectedSummary.setText("Đã chọn (" + totalUnits + " kg): " + details);
            tvTotalAmount.setText(currencyFormat.format(total) + "đ");
            btnSubmitOrder.setEnabled(true);
        }
    }

    private void confirmAndSubmitOrder() {
        if (currentCustomer == null) {
            Toast.makeText(this, "Chỉ tài khoản Customer mới có thể đặt hàng!", Toast.LENGTH_LONG).show();
            return;
        }

        double total = 0;
        StringBuilder summaryBuilder = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : selectedQuantities.entrySet()) {
            int qty = entry.getValue() == null ? 0 : entry.getValue();
            if (qty <= 0) continue;
            Fruit fruit = fruitMap.get(entry.getKey());
            if (fruit == null) continue;
            total += fruit.price * qty;
            summaryBuilder.append("• ").append(fruit.name).append(" x ").append(qty)
                    .append(" = ").append(currencyFormat.format(fruit.price * qty)).append("đ\n");
        }

        if (total <= 0) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 trái cây hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Khách hàng: " + currentCustomer.name + " (#" + currentCustomer.id + ")\n"
                + "Ngày đặt: " + Fruit.todayString() + "\n"
                + "Trạng thái khởi tạo: Pending\n\n"
                + "Chi tiết mặt hàng:\n" + summaryBuilder
                + "\nTổng tiền thanh toán: " + currencyFormat.format(total) + "đ";

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đặt hàng")
                .setMessage(message)
                .setPositiveButton("ĐẶT HÀNG NGAY", (dialog, which) -> {
                    try {
                        long orderId = db.placeOrderWithStockDeduction(currentCustomer.id, selectedQuantities);
                        selectedQuantities.clear();
                        loadFruitsAndRender();
                        Toast.makeText(this, "Đặt hàng thành công! Đơn #" + orderId + " (Pending)", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, OrderHistoryActivity.class));
                    } catch (Exception e) {
                        Toast.makeText(this, "Lỗi đặt hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("HỦY", null)
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
