package com.example.studentmanager.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.Schema;
import com.example.studentmanager.utils.AuthManager;
import java.util.LinkedHashMap;
import java.util.Map;

public class EditRecordActivity extends Activity {
    private Schema.Table spec;
    private DBHelper db;
    private long recordId;
    private final Map<String, EditText> inputs = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AuthManager auth = new AuthManager(this);
        spec = Schema.find(getIntent().getStringExtra("table"));
        if (!auth.isLoggedIn() || !AuthManager.ADMIN.equalsIgnoreCase(auth.getRole()) || spec == null) {
            finish();
            return;
        }

        db = new DBHelper(this);
        recordId = getIntent().getLongExtra("id", -1);
        setContentView(R.layout.activity_form);
        ((TextView) findViewById(R.id.title)).setText(
                (recordId == -1 ? "Thêm mới " : "Chỉnh sửa ") + spec.title);

        LinearLayout form = findViewById(R.id.form);
        ContentValues current = recordId == -1 ? null : db.findById(spec.name, recordId);
        if (recordId != -1 && current == null) {
            finish();
            return;
        }

        for (Schema.Field field : spec.fields) {
            TextView label = new TextView(this);
            label.setText(field.label + (field.required ? " *" : ""));
            label.setTextSize(15);
            form.addView(label);

            EditText input = new EditText(this);
            input.setSingleLine(true);
            if (field.type == Schema.INTEGER) {
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            } else if (field.type == Schema.DECIMAL) {
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            } else if (field.name.equalsIgnoreCase("email")) {
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            }

            if (current != null && current.getAsString(field.name) != null) {
                input.setText(current.getAsString(field.name));
            }
            form.addView(input);
            inputs.put(field.name, input);
        }

        findViewById(R.id.save).setOnClickListener(v -> save());
        findViewById(R.id.delete).setVisibility(recordId == -1 ? android.view.View.GONE : android.view.View.VISIBLE);
        findViewById(R.id.delete).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa?")
                .setMessage("Bạn có chắc chắn muốn xóa bản ghi này không?")
                .setPositiveButton("XÓA", (dialog, which) -> {
                    try {
                        db.delete(spec.name, recordId);
                        finish();
                    } catch (Exception e) {
                        error(e);
                    }
                })
                .setNegativeButton("HỦY", null)
                .show());
    }

    private void save() {
        ContentValues values = new ContentValues();
        for (Schema.Field f : spec.fields) {
            EditText input = inputs.get(f.name);
            if (input == null) return;
            String s = input.getText().toString().trim();
            if (s.isEmpty()) {
                if (f.required) {
                    input.setError("Bắt buộc nhập");
                    input.requestFocus();
                    return;
                }
                values.putNull(f.name);
                continue;
            }

            try {
                if (f.type == Schema.INTEGER) {
                    long val = Long.parseLong(s);
                    if (f.name.equals("quantity") && val < 0) {
                        input.setError("Số lượng không được âm");
                        return;
                    }
                    values.put(f.name, val);
                } else if (f.type == Schema.DECIMAL) {
                    double n = Double.parseDouble(s);
                    if (f.name.equals("price") && n <= 0) {
                        input.setError("Giá phải lớn hơn 0");
                        return;
                    }
                    values.put(f.name, n);
                } else {
                    if (f.name.equals("password") && recordId == -1) {
                        // Hash password for new accounts
                        values.put(f.name, AuthManager.hash(s));
                    } else {
                        values.put(f.name, s);
                    }
                }
            } catch (NumberFormatException e) {
                input.setError("Định dạng số không hợp lệ");
                return;
            }
        }

        try {
            if (recordId == -1) {
                db.insert(spec.name, values);
            } else {
                db.update(spec.name, recordId, values);
            }
            Toast.makeText(this, "Lưu thành công", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            error(e);
        }
    }

    private void error(Exception e) {
        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
}
