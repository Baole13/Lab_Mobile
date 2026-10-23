package com.example.studentmanager.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExpiryPolicyTest {
    @Test public void classifiesExpiryBoundaries() {
        String today = "2026-10-23";
        assertEquals(ExpiryPolicy.Status.EXPIRED, ExpiryPolicy.evaluate("2026-10-22", today));
        assertEquals(ExpiryPolicy.Status.EXPIRES_TODAY, ExpiryPolicy.evaluate(today, today));
        assertEquals(ExpiryPolicy.Status.EXPIRING_SOON, ExpiryPolicy.evaluate("2026-10-24", today));
        assertEquals(ExpiryPolicy.Status.EXPIRING_SOON, ExpiryPolicy.evaluate("2026-10-26", today));
        assertEquals(ExpiryPolicy.Status.VALID, ExpiryPolicy.evaluate("2026-10-27", today));
    }

    @Test public void rejectsInvalidCalendarDates() {
        assertEquals(ExpiryPolicy.Status.INVALID_DATE,
                ExpiryPolicy.evaluate("2026-02-30", "2026-02-28"));
    }
}
