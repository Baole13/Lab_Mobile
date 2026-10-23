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
import com.example.studentmanager.models.Order;
import com.example.studentmanager.utils.AuthManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Người 4 — PlaceOrderActivity
 * Tích hợp với DBHelper, Fruit, Customer, Order trên nhánh dev và Grocery App UI Design System.
 */
public class PlaceOrderActivity extends Activity {
    private static final java.util.concurrent.ExecutorService ORDER_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean submittingOrder;
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

    private String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private boolean isExpiringToday(Fruit fruit) {
        return fruit.getExpiryDate() != null && fruit.getExpiryDate().trim().equals(todayString());
    }

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
            tvCustomerInfo.setText(currentCustomer.getFullname() + " (KH #" + currentCustomer.getId() + ") • " + todayString());
        } else {
            tvCustomerInfo.setText(auth.getUsername() + " • " + todayString());
        }

        int preselectedFruitId = getIntent().getIntExtra("fruitId", -1);
        if (preselectedFruitId != -1) {
            Fruit preFruit = db.getFruitById(preselectedFruitId);
            if (preFruit != null && preFruit.isAvailable()) {
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
        addChip("Tất cả", FILTER_ALL);
        addChip("Còn hàng", FILTER_AVAILABLE);
        addChip("Hết hạn hôm nay", FILTER_TODAY);
        addChip("Không thể đặt", FILTER_UNAVAILABLE);
    }

    private void addChip(String label, String filterCode) {
        Button chip = new Button(this);
        chip.setText(label);
        chip.setAllCaps(false);
        chip.setTextSize(12f);

        boolean isActive = filterCode.equals(activeFilter);
        chip.setBackgroundResource(isActive ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chip.setTextColor(getColor(isActive ? R.color.primary_700 : R.color.text_secondary));
        chip.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
        chip.setPadding(dp(14), 0, dp(14), 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(48)
        );
        params.rightMargin = dp(10);
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
            fruitMap.put(f.getId(), f);
            Integer chosen = selectedQuantities.get(f.getId());
            if (chosen != null) {
                if (!f.isAvailable()) {
                    selectedQuantities.remove(f.getId());
                } else if (chosen > f.getQuantity()) {
                    selectedQuantities.put(f.getId(), f.getQuantity());
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
            String fName = fruit.getName() == null ? "" : fruit.getName().toLowerCase(Locale.ROOT);
            String fCat = fruit.getCategory() == null ? "" : fruit.getCategory().toLowerCase(Locale.ROOT);
            if (!keyword.isEmpty() && !fName.contains(keyword) && !fCat.contains(keyword)) {
                continue;
            }
            if (FILTER_AVAILABLE.equals(activeFilter) && !fruit.isAvailable()) continue;
            if (FILTER_TODAY.equals(activeFilter) && !isExpiringToday(fruit)) continue;
            if (FILTER_UNAVAILABLE.equals(activeFilter) && fruit.isAvailable()) continue;
            filtered.add(fruit);
        }

        if (filtered.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Không tìm thấy trái cây phù hợp.");
            tvEmpty.setTextColor(getColor(R.color.text_secondary));
            tvEmpty.setTextSize(14f);
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(dp(16), dp(32), dp(16), dp(32));
            fruitOrderContainer.addView(tvEmpty);
            return;
        }

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

            row.addView(createProductCard(filtered.get(i), true));

            if (i + 1 < filtered.size()) {
                row.addView(createProductCard(filtered.get(i + 1), false));
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
        int pad = getResources().getDimensionPixelSize(R.dimen.space_lg);
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        if (isLeftColumn) cardParams.rightMargin = dp(7);
        else cardParams.leftMargin = dp(7);
        card.setLayoutParams(cardParams);

        boolean expired = fruit.isExpired();
        boolean outOfStock = fruit.getQuantity() <= 0;
        boolean canOrder = fruit.isAvailable();

        TextView tvBadge = new TextView(this);
        tvBadge.setTextSize(12f);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.bottomMargin = dp(8);
        tvBadge.setLayoutParams(badgeParams);

        com.example.studentmanager.utils.ExpiryBadge.bind(tvBadge,
                com.example.studentmanager.utils.ExpiryPolicy.evaluate(fruit.getExpiryDate(), todayString()));
        if (outOfStock) {
            tvBadge.setText("Hết hàng");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_error);
            tvBadge.setTextColor(getColor(R.color.status_discount_text));
        }
        card.addView(tvBadge);

        TextView tvPlaceholder = new TextView(this);
        tvPlaceholder.setText(resolveFruitEmoji(fruit.getName()));
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

        TextView tvName = new TextView(this);
        tvName.setText(fruit.getName());
        tvName.setTextSize(14f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(getColor(R.color.text_primary));
        tvName.setMaxLines(1);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(tvName);

        TextView tvUnitMeta = new TextView(this);
        tvUnitMeta.setText("1 kg • Kho: " + fruit.getQuantity() + "\nHSD: " + fruit.getExpiryDate());
        tvUnitMeta.setTextSize(12f);
        tvUnitMeta.setTextColor(getColor(R.color.text_secondary));
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(2);
        metaParams.bottomMargin = dp(8);
        tvUnitMeta.setLayoutParams(metaParams);
        card.addView(tvUnitMeta);

        TextView tvPrice = new TextView(this);
        tvPrice.setText(currencyFormat.format(fruit.getPrice()) + "đ / kg");
        tvPrice.setTextSize(16f);
        tvPrice.setTypeface(null, Typeface.BOLD);
        tvPrice.setTextColor(getColor(R.color.primary_700));
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        priceParams.bottomMargin = dp(10);
        tvPrice.setLayoutParams(priceParams);
        card.addView(tvPrice);

        int currentQty = selectedQuantities.containsKey(fruit.getId()) ? selectedQuantities.get(fruit.getId()) : 0;

        if (!canOrder) {
            Button btnDisabled = new Button(this);
            btnDisabled.setText(expired ? "Hết hạn" : "Hết hàng");
            btnDisabled.setAllCaps(false);
            btnDisabled.setEnabled(false);
            btnDisabled.setTextSize(12f);
            btnDisabled.setTextColor(getColor(R.color.text_secondary));
            btnDisabled.setBackgroundResource(R.drawable.bg_stepper_container);
            btnDisabled.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
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
                    dp(48)
            ));
            btnQuickAdd.setOnClickListener(v -> {
                selectedQuantities.put(fruit.getId(), 1);
                renderFruitGrid();
                updateOrderSummary();
            });
            card.addView(btnQuickAdd);
        } else {
            LinearLayout stepper = new LinearLayout(this);
            stepper.setOrientation(LinearLayout.HORIZONTAL);
            stepper.setGravity(Gravity.CENTER_VERTICAL);
            stepper.setBackgroundResource(R.drawable.bg_stepper_container);
            int p1 = dp(4);
            stepper.setPadding(p1, p1, p1, p1);
            stepper.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(56)
            ));

            Button btnMinus = new Button(this);
            btnMinus.setText("–");
            btnMinus.setContentDescription("Giảm số lượng " + fruit.getName());
            btnMinus.setTextColor(getColor(currentQty == 1 ? R.color.status_discount_text : R.color.text_primary));
            btnMinus.setTextSize(14f);
            btnMinus.setTypeface(null, Typeface.BOLD);
            btnMinus.setPadding(0, 0, 0, 0);
            btnMinus.setBackgroundResource(R.drawable.bg_stepper_btn_white);
            btnMinus.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));

            TextView tvCount = new TextView(this);
            tvCount.setText(String.valueOf(currentQty));
            tvCount.setTextSize(14f);
            tvCount.setTypeface(null, Typeface.BOLD);
            tvCount.setTextColor(getColor(R.color.text_primary));
            tvCount.setGravity(Gravity.CENTER);
            tvCount.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button btnPlus = new Button(this);
            btnPlus.setText("+");
            btnPlus.setContentDescription("Tăng số lượng " + fruit.getName());
            btnPlus.setTextColor(Color.WHITE);
            btnPlus.setTextSize(16f);
            btnPlus.setTypeface(null, Typeface.BOLD);
            btnPlus.setPadding(0, 0, 0, 0);
            btnPlus.setBackgroundResource(R.drawable.bg_btn_add);
            btnPlus.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));

            btnMinus.setOnClickListener(v -> {
                int q = selectedQuantities.containsKey(fruit.getId()) ? selectedQuantities.get(fruit.getId()) : 0;
                if (q <= 1) {
                    selectedQuantities.remove(fruit.getId());
                } else {
                    selectedQuantities.put(fruit.getId(), q - 1);
                }
                renderFruitGrid();
                updateOrderSummary();
            });

            btnPlus.setOnClickListener(v -> {
                int q = selectedQuantities.containsKey(fruit.getId()) ? selectedQuantities.get(fruit.getId()) : 0;
                if (q >= fruit.getQuantity()) {
                    Toast.makeText(this, "Đã đạt tối đa tồn kho (" + fruit.getQuantity() + " kg)!", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedQuantities.put(fruit.getId(), q + 1);
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
            if (fruit == null || !fruit.isAvailable()) continue;

            double lineTotal = fruit.getPrice() * qty;
            total += lineTotal;
            totalUnits += qty;
            if (details.length() > 0) details.append(" • ");
            details.append(fruit.getName()).append(" x").append(qty);
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
            if (fruit == null || !fruit.isAvailable()) continue;
            total += fruit.getPrice() * qty;
            summaryBuilder.append("• ").append(fruit.getName()).append(" x ").append(qty)
                    .append(" = ").append(currencyFormat.format(fruit.getPrice() * qty)).append("đ\n");
        }

        if (total <= 0) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 trái cây hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        final double orderTotal = total;
        String nowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        String message = "Khách hàng: " + currentCustomer.getFullname() + " (#" + currentCustomer.getId() + ")\n"
                + "Ngày đặt: " + nowStr + "\n"
                + "Trạng thái khởi tạo: Pending\n\n"
                + "Chi tiết mặt hàng:\n" + summaryBuilder
                + "\nTổng tiền thanh toán: " + currencyFormat.format(total) + "đ";

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đặt hàng")
                .setMessage(message)
                .setPositiveButton("ĐẶT HÀNG NGAY", (dialog, which) -> {
                    if (submittingOrder) return;
                    submittingOrder = true;
                    btnSubmitOrder.setEnabled(false);
                    java.util.Map<Integer, Integer> orderQuantities = new java.util.LinkedHashMap<>();
                    for (Map.Entry<Integer, Integer> entry : selectedQuantities.entrySet()) {
                        if (entry.getValue() != null && entry.getValue() > 0) orderQuantities.put(entry.getKey(), entry.getValue());
                    }
                    int customerId = currentCustomer.getId();
                    ORDER_EXECUTOR.execute(() -> {
                        try (DBHelper orderDatabase = new DBHelper(getApplicationContext())) {
                            long orderId = orderDatabase.placeOrder(customerId, orderQuantities, nowStr, orderTotal);
                            runOnUiThread(() -> {
                                submittingOrder = false;
                                if (isFinishing() || isDestroyed()) return;
                                selectedQuantities.clear(); loadFruitsAndRender();
                                Toast.makeText(this, "Đặt hàng thành công! Đơn #" + orderId + " (Pending)", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(this, OrderHistoryActivity.class));
                            });
                        } catch (Exception error) {
                            runOnUiThread(() -> {
                                submittingOrder = false;
                                if (isFinishing() || isDestroyed()) return;
                                loadFruitsAndRender();
                                Toast.makeText(this, "Không thể đặt hàng: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .setNegativeButton("HỦY", null)
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
