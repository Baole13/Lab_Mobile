package com.example.studentmanager.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

public final class ExpiryBadge {
    private ExpiryBadge() {}

    public static void bind(TextView view, ExpiryPolicy.Status status) {
        Context context = view.getContext();
        int background;
        int foreground;
        String label;
        switch (status) {
            case EXPIRED:
                background = Color.parseColor("#FEE2E2"); foreground = Color.parseColor("#B91C1C"); label = "Đã hết hạn"; break;
            case EXPIRES_TODAY:
                background = Color.parseColor("#FEF3C7"); foreground = Color.parseColor("#92400E"); label = "Hết hạn hôm nay"; break;
            case EXPIRING_SOON:
                background = Color.parseColor("#FEF3C7"); foreground = Color.parseColor("#92400E"); label = "Sắp hết hạn"; break;
            case VALID:
                background = Color.parseColor("#DCFCE7"); foreground = Color.parseColor("#166534"); label = "Còn hạn"; break;
            default:
                background = Color.LTGRAY; foreground = Color.DKGRAY; label = "Ngày không hợp lệ";
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(background);
        drawable.setCornerRadius(24f * context.getResources().getDisplayMetrics().density);
        view.setBackground(drawable);
        view.setTextColor(ColorStateList.valueOf(foreground));
        view.setText(label);
    }
}
