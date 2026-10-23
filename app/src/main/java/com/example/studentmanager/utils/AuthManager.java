package com.example.studentmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.models.Account;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Authentication and Session Manager using SharedPreferences.
 * Supports SHA-256 password hashing and seamless authentication with SQLite.
 */
public class AuthManager {
    public static final String ADMIN = "Admin";
    public static final String CUSTOMER = "Customer";

    // Backward-compatibility aliases for template classes
    public static final String TEACHER = ADMIN;
    public static final String STUDENT = CUSTOMER;

    private static final String FILE = "login_accounts";
    private final SharedPreferences prefs;
    private final Context context;

    public AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        seedDemoAccounts();
    }

    private void seedDemoAccounts() {
        if (prefs.contains("accounts_json")) return;
        JSONArray accounts = new JSONArray();
        accounts.put(createAccount(1, "admin", "123456", ADMIN));
        accounts.put(createAccount(2, "customer", "123456", CUSTOMER));
        // Keep teacher/student for legacy test backward-compatibility
        accounts.put(createAccount(3, "teacher", "123456", ADMIN));
        accounts.put(createAccount(4, "student", "123456", CUSTOMER));
        prefs.edit().putString("accounts_json", accounts.toString()).apply();
    }

    private JSONObject createAccount(int id, String name, String password, String role) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("username", name);
            o.put("passwordHash", hash(password));
            o.put("role", role);
        } catch (Exception ignored) {}
        return o;
    }

    /**
     * Authenticates with SQLite first, falls back to SharedPreferences if needed.
     * Saves session info in SharedPreferences on success.
     */
    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        String trimmedUser = username.trim();

        // 1. Try SQLite login
        try {
            DBHelper db = new DBHelper(context);
            Account account = db.checkLogin(trimmedUser, password);
            if (account != null) {
                saveSession(account.getId(), account.getUsername(), account.getRole());
                return true;
            }
        } catch (Exception ignored) {}

        // 2. Fallback to SharedPreferences accounts
        try {
            JSONArray accounts = new JSONArray(prefs.getString("accounts_json", "[]"));
            String hashedInput = hash(password);
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject a = accounts.getJSONObject(i);
                if (trimmedUser.equalsIgnoreCase(a.optString("username")) &&
                    hashedInput.equals(a.optString("passwordHash"))) {
                    saveSession(a.optInt("id"), a.optString("username"), a.optString("role"));
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    public void saveSession(int accountId, String username, String role) {
        prefs.edit()
            .putBoolean("loggedIn", true)
            .putInt("accountId", accountId)
            .putString("username", username)
            .putString("role", role)
            .apply();
    }

    public void logout() {
        prefs.edit()
            .remove("loggedIn")
            .remove("username")
            .remove("role")
            .remove("accountId")
            .apply();
    }

    public boolean isLoggedIn() { return prefs.getBoolean("loggedIn", false); }
    public String getUsername() { return prefs.getString("username", ""); }
    public String getRole() { return prefs.getString("role", ""); }
    public int getAccountId() { return prefs.getInt("accountId", -1); }

    public static String hash(String source) {
        try {
            byte[] result = MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder h = new StringBuilder();
            for (byte b : result) h.append(String.format("%02x", b & 0xff));
            return h.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
