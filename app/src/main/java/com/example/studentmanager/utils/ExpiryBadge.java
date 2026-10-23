package com.example.studentmanager.utils;

import android.widget.TextView;
import com.example.studentmanager.R;

public final class ExpiryBadge {
    private ExpiryBadge() {}
    public static void bind(TextView view, ExpiryPolicy.Status status) {
        int background; int color; String label;
        switch (status) {
            case EXPIRED:
                background = R.drawable.bg_badge_error; color = R.color.status_discount_text; label = "Đã hết hạn"; break;
            case EXPIRES_TODAY:
                background = R.drawable.bg_badge_warning; color = R.color.secondary_600; label = "Hết hạn hôm nay"; break;
            case EXPIRING_SOON:
                background = R.drawable.bg_badge_warning; color = R.color.secondary_600; label = "Sắp hết hạn"; break;
            case VALID:
                background = R.drawable.bg_badge_success; color = R.color.primary_700; label = "Còn hạn"; break;
            default:
                background = R.drawable.bg_chip_inactive; color = R.color.text_secondary; label = "Ngày không hợp lệ";
        }
        view.setBackgroundResource(background);
        view.setTextColor(view.getContext().getColor(color));
        view.setText(label);
    }
}
