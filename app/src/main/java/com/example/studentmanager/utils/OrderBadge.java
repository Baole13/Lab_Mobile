package com.example.studentmanager.utils;

import android.widget.TextView;
import com.example.studentmanager.R;
import com.example.studentmanager.models.Order;

public final class OrderBadge {
    private OrderBadge() {}
    public static void bind(TextView view, String status) {
        if (Order.STATUS_PENDING.equalsIgnoreCase(status)) {
            view.setText("Chờ xác nhận");
            view.setBackgroundResource(R.drawable.bg_badge_warning);
            view.setTextColor(view.getContext().getColor(R.color.secondary_600));
        } else if (Order.STATUS_CONFIRMED.equalsIgnoreCase(status)) {
            view.setText("Đã xác nhận");
            view.setBackgroundResource(R.drawable.bg_badge_confirmed);
            view.setTextColor(view.getContext().getColor(R.color.status_confirmed_text));
        } else if (Order.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            view.setText("Hoàn thành");
            view.setBackgroundResource(R.drawable.bg_badge_success);
            view.setTextColor(view.getContext().getColor(R.color.primary_700));
        } else {
            view.setText("Chưa xác định");
            view.setBackgroundResource(R.drawable.bg_chip_inactive);
            view.setTextColor(view.getContext().getColor(R.color.text_secondary));
        }
    }
}
