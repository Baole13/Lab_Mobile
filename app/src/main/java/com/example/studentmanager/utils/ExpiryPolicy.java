package com.example.studentmanager.utils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public final class ExpiryPolicy {
    public enum Status { EXPIRED, EXPIRES_TODAY, EXPIRING_SOON, VALID, INVALID_DATE }

    private ExpiryPolicy() {}

    public static Status evaluate(String expiryIsoDate, String todayIsoDate) {
        Date expiry = parseDate(expiryIsoDate);
        Date today = parseDate(todayIsoDate);
        if (expiry == null || today == null) return Status.INVALID_DATE;

        long days = TimeUnit.MILLISECONDS.toDays(expiry.getTime() - today.getTime());
        if (days < 0) return Status.EXPIRED;
        if (days == 0) return Status.EXPIRES_TODAY;
        if (days <= 3) return Status.EXPIRING_SOON;
        return Status.VALID;
    }

    public static String todayIsoDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static boolean isValidDate(String value) { return parseDate(value) != null; }

    private static Date parseDate(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return null;
        SimpleDateFormat format = dateFormat();
        ParsePosition position = new ParsePosition(0);
        Date parsed = format.parse(value, position);
        return parsed != null && position.getIndex() == value.length() ? parsed : null;
    }

    private static SimpleDateFormat dateFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}
