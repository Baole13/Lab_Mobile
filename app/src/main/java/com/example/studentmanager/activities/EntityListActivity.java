package com.example.studentmanager.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.Schema;
import com.example.studentmanager.models.Customer;
import com.example.studentmanager.utils.AuthManager;
import java.util.ArrayList;
import java.util.List;

public class EntityListActivity extends Activity {
    private String table;
    private Schema.Table spec;
    private DBHelper db;
    private AuthManager auth;
    private boolean canEdit;
    private List<ContentValues> rows = new ArrayList<>();
    private EditText search;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = new AuthManager(this);
        table = getIntent().getStringExtra("table");
        spec = Schema.find(table);
        canEdit = AuthManager.ADMIN.equalsIgnoreCase(auth.getRole());

        if (!auth.isLoggedIn() || spec == null) {
            finish();
            return;
        }

        if (Schema.FRUIT.equals(spec.name) && auth.isAdmin()) {
            startActivity(new Intent(this, FruitManagementActivity.class)); finish(); return;
        }
        db = new DBHelper(this);
        setContentView(R.layout.activity_list);
        ((TextView) findViewById(R.id.title)).setText(spec.title + (canEdit ? "" : " (Xem)"));

        View add = findViewById(R.id.add);
        add.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        add.setOnClickListener(v -> openEdit(-1));

        search = findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refresh(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        ListView list = findViewById(R.id.list);
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            if (!canEdit || pos >= rows.size()) return false;
            long recordId = rows.get(pos).getAsLong("id");
            new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa?")
                .setMessage("Bạn có chắc chắn muốn xóa bản ghi ID: " + recordId + " không?")
                .setPositiveButton("XÓA", (d, w) -> {
                    try {
                        db.delete(table, recordId);
                        refresh();
                    } catch (Exception e) {
                        error(e);
                    }
                })
                .setNegativeButton("HỦY", null)
                .show();
            return true;
        });
    }

    private void openEdit(long id) {
        Intent i = new Intent(this, EditRecordActivity.class);
        i.putExtra("table", table);
        i.putExtra("id", id);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) refresh();
    }

    private void refresh() {
        try {
            String where = null;
            String[] args = null;

            // If customer is viewing profile or orders, scope to their own data
            if (!canEdit) {
                if (Schema.CUSTOMER.equalsIgnoreCase(table)) {
                    where = "accountId = ?";
                    args = new String[]{String.valueOf(auth.getAccountId())};
                } else if (Schema.ORDER.equalsIgnoreCase(table)) {
                    Customer c = db.getCustomerByAccountId(auth.getAccountId());
                    if (c != null) {
                        where = "customerId = ?";
                        args = new String[]{String.valueOf(c.getId())};
                    }
                }
            }

            // Multi-field search
            String text = search == null ? "" : search.getText().toString().trim();
            if (!text.isEmpty()) {
                StringBuilder expression = new StringBuilder("(");
                List<String> bind = new ArrayList<>();
                if (args != null) {
                    for (String a : args) bind.add(a);
                }
                for (int i = 0; i < spec.fields.size(); i++) {
                    if (i > 0) expression.append(" OR ");
                    expression.append("CAST(\"").append(spec.fields.get(i).name).append("\" AS TEXT) LIKE ?");
                    bind.add("%" + text + "%");
                }
                expression.append(")");
                where = where == null ? expression.toString() : "(" + where + ") AND " + expression;
                args = bind.toArray(new String[0]);
            }

            rows = db.rows(table, where, args);
            List<String> labels = new ArrayList<>();
            for (ContentValues row : rows) {
                StringBuilder sb = new StringBuilder("Mã #" + row.getAsString("id"));
                for (Schema.Field f : spec.fields) {
                    String value = row.getAsString(f.name);
                    if (value != null && !value.isEmpty()) {
                        sb.append("\n• ").append(f.label).append(": ").append(value);
                    }
                }
                labels.add(sb.toString());
            }

            if (labels.isEmpty()) labels.add("Chưa có dữ liệu");
            ListView list = findViewById(R.id.list);
            list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
            list.setOnItemClickListener((p, v, pos, id) -> {
                if (pos >= rows.size()) return;
                if (canEdit) {
                    openEdit(rows.get(pos).getAsLong("id"));
                } else {
                    Toast.makeText(this, "Chỉ xem (Chế độ Customer)", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            error(e);
        }
    }

    private void error(Exception e) {
        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
}
