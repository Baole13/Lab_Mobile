package com.example.studentmanager.utils;

public final class FruitValidation {
    private FruitValidation() {}

    public static void validate(String name, String category, double price, int quantity, String expiryDate) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Vui lòng nhập tên trái cây.");
        if (category == null || category.trim().isEmpty()) throw new IllegalArgumentException("Vui lòng nhập loại trái cây.");
        if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0)
            throw new IllegalArgumentException("Giá phải là số hữu hạn lớn hơn 0.");
        if (quantity < 0) throw new IllegalArgumentException("Số lượng phải là số nguyên không âm.");
        if (!ExpiryPolicy.isValidDate(expiryDate)) throw new IllegalArgumentException("Ngày không hợp lệ. Nhập yyyy-MM-dd.");
    }
}
