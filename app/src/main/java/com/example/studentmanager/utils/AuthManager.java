package com.example.studentmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Demo account list and the active Admin/Customer session are stored in SharedPreferences. */
public class AuthManager {
    public static final String ADMIN = "Admin", CUSTOMER = "Customer";
    private static final String FILE = "login_accounts";
    private final SharedPreferences prefs;
    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        seedDemoAccounts();
    }
    private void seedDemoAccounts() {
        if (hasCurrentDemoAccounts()) return;
        JSONArray accounts = new JSONArray();
        accounts.put(createAccount(1, "admin", "123456", ADMIN));
        accounts.put(createAccount(2, "customer", "123456", CUSTOMER));
        prefs.edit().putString("accounts_json", accounts.toString())
            .remove("loggedIn").remove("username").remove("role").remove("accountId").apply();
    }
    private boolean hasCurrentDemoAccounts() {
        try {
            JSONArray accounts = new JSONArray(prefs.getString("accounts_json", "[]"));
            boolean adminFound = false, customerFound = false;
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject account = accounts.getJSONObject(i);
                adminFound |= "admin".equals(account.optString("username")) &&
                    ADMIN.equals(account.optString("role"));
                customerFound |= "customer".equals(account.optString("username")) &&
                    CUSTOMER.equals(account.optString("role"));
            }
            return adminFound && customerFound;
        } catch (Exception ignored) {
            return false;
        }
    }
    private JSONObject createAccount(int id, String name, String password, String role) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id); o.put("username", name);
            o.put("passwordHash", hash(password)); o.put("role", role);
        } catch (Exception ignored) {}
        return o;
    }
    public boolean login(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty() || password == null || password.isEmpty()) return false;
        try {
            JSONArray accounts = new JSONArray(prefs.getString("accounts_json", "[]"));
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject a = accounts.getJSONObject(i);
                String role = a.optString("role");
                if (normalizedUsername.equals(a.optString("username")) &&
                    hash(password).equals(a.optString("passwordHash")) &&
                    (ADMIN.equals(role) || CUSTOMER.equals(role))) {
                    prefs.edit().putBoolean("loggedIn", true)
                        .putString("username", a.optString("username"))
                        .putString("role", role)
                        .putInt("accountId", a.optInt("id")).apply();
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    public void logout() {
        prefs.edit().remove("loggedIn").remove("username")
            .remove("role").remove("accountId").apply();
    }
    public boolean isLoggedIn() {
        String role = getRole();
        return prefs.getBoolean("loggedIn", false) && getAccountId() > 0 &&
            (ADMIN.equals(role) || CUSTOMER.equals(role));
    }
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
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
