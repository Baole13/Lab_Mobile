package com.example.studentmanager.utils;

import android.os.Handler;
import android.os.Looper;
import java.util.Calendar;

public final class ExpiryRefreshScheduler {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh;
    private final Runnable scheduledRefresh = new Runnable() {
        @Override public void run() {
            refresh.run();
            scheduleNextMidnight();
        }
    };

    public ExpiryRefreshScheduler(Runnable refresh) { this.refresh = refresh; }

    public void start() { stop(); scheduleNextMidnight(); }
    public void stop() { handler.removeCallbacks(scheduledRefresh); }

    private void scheduleNextMidnight() {
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        handler.postDelayed(scheduledRefresh, Math.max(1, next.getTimeInMillis() - System.currentTimeMillis()));
    }
}
