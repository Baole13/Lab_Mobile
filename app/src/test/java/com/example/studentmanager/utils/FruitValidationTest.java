package com.example.studentmanager.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class FruitValidationTest {
    @Test public void validatesInputBoundaries() {
        FruitValidation.validate("Táo", "Nội địa", 1, 0, "2024-02-29");
        rejects(" ", "Loại", 1, 0, "2026-10-23");
        rejects("Táo", " ", 1, 0, "2026-10-23");
        for (double price : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY})
            rejects("Táo", "Loại", price, 0, "2026-10-23");
        rejects("Táo", "Loại", 1, -1, "2026-10-23");
        rejects("Táo", "Loại", 1, 0, "2026-02-29");
        rejects("Táo", "Loại", 1, 0, "2026-1-01");
        rejects("Táo", "Loại", 1, 0, "2026-10-23abc");
    }
    private void rejects(String name, String category, double price, int quantity, String date) {
        try { FruitValidation.validate(name, category, price, quantity, date); fail("Expected invalid input"); }
        catch (IllegalArgumentException expected) { }
    }
    @Test public void searchesVietnameseAndFiltersBoundaries() {
        assertTrue(FruitListPolicy.matches("Táo đỏ", "Trái cây", 5, ExpiryPolicy.Status.EXPIRING_SOON, "tao do", 3));
        assertTrue(FruitListPolicy.matches("Táo", "Nhập khẩu", 5, ExpiryPolicy.Status.VALID, "nhap", 5));
        assertFalse(FruitListPolicy.matches("Táo", "Loại", 0, ExpiryPolicy.Status.VALID, "", 5));
        assertFalse(FruitListPolicy.matches("Táo", "Loại", 6, ExpiryPolicy.Status.VALID, "", 5));
        assertTrue(FruitListPolicy.matches("Táo", "Loại", 0, ExpiryPolicy.Status.EXPIRED, "", 4));
        assertFalse(FruitListPolicy.matches("Táo", "Loại", 1, ExpiryPolicy.Status.EXPIRES_TODAY, "", 1));
        assertTrue(FruitListPolicy.matches("Táo", "Loại", 1, ExpiryPolicy.Status.EXPIRES_TODAY, "", 2));
    }
}
