package com.example.studentmanager.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.Schema;
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
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = new AuthManager(this);
        table = getIntent().getStringExtra("table");
        spec = Schema.find(table);
        canEdit = AuthManager.TEACHER.equals(auth.getRole());
        if (!auth.isLoggedIn() || spec == null ||
            (!canEdit && !Schema.SCORE.equals(table) && !Schema.STUDENT.equals(table))) {
            finish(); return;
        }
        db = new DBHelper(this);
        setContentView(R.layout.activity_list);
        ((TextView)findViewById(R.id.title)).setText(spec.title + (canEdit ? "" : " • My data"));
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
        list.setOnItemClickListener((p,v,pos,id) -> {
            if (canEdit) openEdit(rows.get(pos).getAsLong("id"));
            else Toast.makeText(this, "Read only", Toast.LENGTH_SHORT).show();
        });
        list.setOnItemLongClickListener((p,v,pos,id) -> {
            if (!canEdit) return true;
            long recordId = rows.get(pos).getAsLong("id");
            new AlertDialog.Builder(this).setTitle("Delete record?")
                .setMessage("ID: " + recordId)
                .setPositiveButton("DELETE", (d,w) -> {
                    try { db.delete(table, recordId); refresh(); }
                    catch (Exception e) { error(e); }
                }).setNegativeButton("CANCEL", null).show();
            return true;
        });
    }
    private void openEdit(long id) {
        Intent i = new Intent(this, EditRecordActivity.class);
        i.putExtra("table", table); i.putExtra("id", id); startActivity(i);
    }
    @Override protected void onResume() { super.onResume(); if (db != null) refresh(); }
    private void refresh() {
        try {
            String where = null; String[] args = null;
            if (!canEdit) {
                if (Schema.STUDENT.equals(table)) {
                    where = "accountId=?";
                    args = new String[]{String.valueOf(auth.getAccountId())};
                } else {
                    where = "studentCode=?";
                    args = new String[]{db.studentCodeForAccount(auth.getAccountId())};
                }
            }
            // Multi-field search; all column names originate from trusted Schema metadata.
            String text = search == null ? "" : search.getText().toString().trim();
            if (!text.isEmpty()) {
                StringBuilder expression = new StringBuilder("(");
                List<String> bind = new ArrayList<>();
                if (args != null) for (String a : args) bind.add(a);
                for (int i = 0; i < spec.fields.size(); i++) {
                    if (i > 0) expression.append(" OR ");
                    expression.append("CAST(\"").append(spec.fields.get(i).name)
                        .append("\" AS TEXT) LIKE ?");
                    bind.add("%" + text + "%");
                }
                expression.append(")");
                where = where == null ? expression.toString() : "(" + where + ") AND " + expression;
                args = bind.toArray(new String[0]);
            }
            rows = db.rows(table, where, args);
            List<String> labels = new ArrayList<>();
            for (ContentValues row : rows) {
                StringBuilder sb = new StringBuilder("#" + row.getAsString("id"));
                for (Schema.Field f : spec.fields) {
                    String value = row.getAsString(f.name);
                    if (value != null && !value.isEmpty()) sb.append("\n").append(f.label).append(": ").append(value);
                }
                labels.add(sb.toString());
            }
            if (labels.isEmpty()) labels.add("No records yet");
            ListView list = findViewById(R.id.list);
            list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
            list.setOnItemClickListener((p,v,pos,id) -> {
                if (pos >= rows.size()) return;
                if (canEdit) openEdit(rows.get(pos).getAsLong("id"));
                else Toast.makeText(this, "Read only", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) { error(e); }
    }
    private void error(Exception e) { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); }
}
