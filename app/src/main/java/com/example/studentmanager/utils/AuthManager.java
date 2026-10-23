package com.example.studentmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Demo account list AND logged-in session are stored in SharedPreferences. */
public class AuthManager {
    public static final String TEACHER = "TEACHER", STUDENT = "STUDENT";
    private static final String FILE = "login_accounts";
    private final SharedPreferences prefs;
    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        seedDemoAccounts();
    }
    private void seedDemoAccounts() {
        if (prefs.contains("accounts_json")) return;
        JSONArray accounts = new JSONArray();
        accounts.put(createAccount(1, "teacher", "123456", TEACHER));
        accounts.put(createAccount(2, "student", "123456", STUDENT));
        prefs.edit().putString("accounts_json", accounts.toString()).apply();
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
        try {
            JSONArray accounts = new JSONArray(prefs.getString("accounts_json", "[]"));
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject a = accounts.getJSONObject(i);
                if (username.trim().equals(a.optString("username")) &&
                    hash(password).equals(a.optString("passwordHash"))) {
                    prefs.edit().putBoolean("loggedIn", true)
                        .putString("username", a.optString("username"))
                        .putString("role", a.optString("role"))
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
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
