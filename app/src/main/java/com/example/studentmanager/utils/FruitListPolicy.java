package com.example.studentmanager.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class FruitListPolicy {
    private FruitListPolicy() {}
    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd').trim();
    }
    public static boolean matches(String name, String category, int quantity,
                                  ExpiryPolicy.Status expiry, String search, int filter) {
        String query = normalize(search);
        if (!normalize(name).contains(query) && !normalize(category).contains(query)) return false;
        switch (filter) {
            case 1: return expiry == ExpiryPolicy.Status.EXPIRED;
            case 2: return expiry == ExpiryPolicy.Status.EXPIRES_TODAY;
            case 3: return expiry == ExpiryPolicy.Status.EXPIRING_SOON;
            case 4: return quantity == 0;
            case 5: return quantity > 0 && quantity <= 5;
            default: return true;
        }
    }
}
