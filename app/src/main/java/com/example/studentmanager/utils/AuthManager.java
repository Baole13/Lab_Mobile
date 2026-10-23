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
 * Supports SHA-256 password hashing and seamless authentication with SQLite and SharedPreferences.
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
        if (hasCurrentDemoAccounts()) return;
        JSONArray accounts = new JSONArray();
        accounts.put(createAccount(1, "admin", "123456", ADMIN));
        accounts.put(createAccount(2, "customer", "123456", CUSTOMER));
        // Keep teacher/student for legacy test backward-compatibility
        accounts.put(createAccount(3, "teacher", "123456", ADMIN));
        accounts.put(createAccount(4, "student", "123456", CUSTOMER));
        prefs.edit().putString("accounts_json", accounts.toString()).apply();
    }

    private boolean hasCurrentDemoAccounts() {
        try {
            JSONArray accounts = new JSONArray(prefs.getString("accounts_json", "[]"));
            boolean adminFound = false, customerFound = false;
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject account = accounts.getJSONObject(i);
                adminFound |= "admin".equalsIgnoreCase(account.optString("username")) &&
                    ADMIN.equalsIgnoreCase(account.optString("role"));
                customerFound |= "customer".equalsIgnoreCase(account.optString("username")) &&
                    CUSTOMER.equalsIgnoreCase(account.optString("role"));
            }
            return adminFound && customerFound;
        } catch (Exception ignored) {
            return false;
        }
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
        if (trimmedUser.isEmpty() || password.isEmpty()) return false;

        // 1. Try SQLite login
        try {
            DBHelper db = new DBHelper(context);
            Account account = db.checkLogin(trimmedUser, password);
            if (account != null) {
                String role = account.getRole();
                if (ADMIN.equalsIgnoreCase(role) || CUSTOMER.equalsIgnoreCase(role)) {
                    saveSession(account.getId(), account.getUsername(), role);
                    return true;
                }
            }
        } catch (Exception ignored) {}

        // 2. Fallback to SharedPreferences accounts
        try {
            JSONArray accounts = new JSONArray(prefs.getString("accounts_json", "[]"));
            String hashedInput = hash(password);
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject a = accounts.getJSONObject(i);
                String role = a.optString("role");
                if (trimmedUser.equalsIgnoreCase(a.optString("username")) &&
                    hashedInput.equals(a.optString("passwordHash")) &&
                    (ADMIN.equalsIgnoreCase(role) || CUSTOMER.equalsIgnoreCase(role))) {
                    saveSession(a.optInt("id"), a.optString("username"), role);
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

    public boolean isLoggedIn() {
        String role = getRole();
        return prefs.getBoolean("loggedIn", false) &&
               getAccountId() > 0 &&
               (ADMIN.equalsIgnoreCase(role) || CUSTOMER.equalsIgnoreCase(role));
    }

    public boolean isAdmin() {
        return isLoggedIn() && ADMIN.equalsIgnoreCase(getRole());
    }

    public boolean isCustomer() {
        return isLoggedIn() && CUSTOMER.equalsIgnoreCase(getRole());
    }

    public String getUsername() { return prefs.getString("username", ""); }
    public String getRole() { return prefs.getString("role", ""); }
    public int getAccountId() { return prefs.getInt("accountId", -1); }

    public static String hash(String source) {
        if (source == null) return "";
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
