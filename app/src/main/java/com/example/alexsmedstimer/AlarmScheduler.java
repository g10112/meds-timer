package com.example.alexsmedstimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class AlarmScheduler {
    static final int ALARM_COUNT = ReminderSettings.COUNT;
    static final int TEST_ALARM_ID = 99;

    private static final String PREFS = "alex_meds_timer_state";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_STARTED = "started";
    private static final String KEY_TIME_PREFIX = "alarm_time_";
    private static final String KEY_FIRED_PREFIX = "alarm_fired_";
    private static final String KEY_MISSED = "missed_count";
    private static final int REQUEST_BASE = 3200;

    private AlarmScheduler() {}

    static final class AlarmEntry {
        final int number;
        final long timeMillis;

        AlarmEntry(int number, long timeMillis) {
            this.number = number;
            this.timeMillis = timeMillis;
        }
    }

    static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager != null && manager.canScheduleExactAlarms();
    }

    static synchronized boolean scheduleNewSession(Context context) {
        if (hasActiveSession(context) || !canScheduleExactAlarms(context)) {
            return false;
        }

        // Take a snapshot of the configured intervals. Changing settings later
        // must never silently move or cancel alarms in the active session.
        long[] delaysMinutes = ReminderSettings.getDelayMinutes(context);
        cancelPendingIntents(context);
        long now = System.currentTimeMillis();
        android.content.SharedPreferences.Editor editor = prefs(context).edit()
                .clear()
                .putBoolean(KEY_ACTIVE, true)
                .putLong(KEY_STARTED, now);

        for (int i = 0; i < ALARM_COUNT; i++) {
            int number = i + 1;
            long triggerAt = now + delaysMinutes[i] * ReminderSettings.MINUTE_MS;
            editor.putLong(KEY_TIME_PREFIX + number, triggerAt);
            editor.putBoolean(KEY_FIRED_PREFIX + number, false);
        }
        if (!editor.commit()) {
            return false;
        }

        for (int number = 1; number <= ALARM_COUNT; number++) {
            if (!scheduleStoredAlarm(context, number)) {
                cancelSession(context);
                return false;
            }
        }
        updateWidget(context);
        return true;
    }

    static boolean scheduleTestAlarm(Context context) {
        long triggerAt = System.currentTimeMillis() + 10_000L;
        return canScheduleExactAlarms(context) && scheduleAlarm(context, TEST_ALARM_ID, triggerAt);
    }

    static synchronized void markFired(Context context, int number) {
        if (number < 1 || number > ALARM_COUNT) {
            return;
        }
        prefs(context).edit().putBoolean(KEY_FIRED_PREFIX + number, true).commit();
        if (getRemainingAlarms(context).isEmpty()) {
            prefs(context).edit().putBoolean(KEY_ACTIVE, false).apply();
        }
        updateWidget(context);
    }

    static synchronized void markMissed(Context context, int number) {
        if (number < 1 || number > ALARM_COUNT) {
            return;
        }
        if (prefs(context).getBoolean(KEY_FIRED_PREFIX + number, false)) {
            return;
        }
        int missed = prefs(context).getInt(KEY_MISSED, 0) + 1;
        prefs(context).edit()
                .putBoolean(KEY_FIRED_PREFIX + number, true)
                .putInt(KEY_MISSED, missed)
                .commit();
        if (getRemainingAlarms(context).isEmpty()) {
            prefs(context).edit().putBoolean(KEY_ACTIVE, false).apply();
        }
        AlarmService.showMissedReminder(context, missed);
        updateWidget(context);
    }

    static int getMissedCount(Context context) {
        return prefs(context).getInt(KEY_MISSED, 0);
    }

    static synchronized void cancelSession(Context context) {
        cancelPendingIntents(context);
        // Persist cancellation before returning, including across a sudden restart.
        prefs(context).edit().clear().commit();
        updateWidget(context);
    }

    static synchronized void restoreRemaining(Context context) {
        if (!prefs(context).getBoolean(KEY_ACTIVE, false)) {
            updateWidget(context);
            return;
        }

        cancelPendingIntents(context);
        long now = System.currentTimeMillis();
        int missed = 0;
        boolean anyRemaining = false;
        for (int number = 1; number <= ALARM_COUNT; number++) {
            boolean fired = prefs(context).getBoolean(KEY_FIRED_PREFIX + number, false);
            long time = prefs(context).getLong(KEY_TIME_PREFIX + number, 0L);
            if (fired) {
                continue;
            }
            if (time <= now + 2_000L) {
                // The phone was off when this alarm became due. Never silently
                // treat a missed reminder as a successful medicine dose.
                prefs(context).edit().putBoolean(KEY_FIRED_PREFIX + number, true).commit();
                missed++;
            } else {
                // If permission was revoked, preserve the scheduled timestamps
                // and show the missing permission rather than claiming success.
                if (canScheduleExactAlarms(context)) {
                    scheduleAlarm(context, number, time);
                }
                anyRemaining = true;
            }
        }
        if (missed > 0) {
            int total = prefs(context).getInt(KEY_MISSED, 0) + missed;
            prefs(context).edit().putInt(KEY_MISSED, total).commit();
            AlarmService.showMissedReminder(context, total);
        }
        if (!anyRemaining) {
            prefs(context).edit().putBoolean(KEY_ACTIVE, false).apply();
        }
        updateWidget(context);
    }

    static boolean hasActiveSession(Context context) {
        if (!prefs(context).getBoolean(KEY_ACTIVE, false)) {
            return false;
        }
        List<AlarmEntry> remaining = getRemainingAlarms(context);
        if (remaining.isEmpty()) {
            prefs(context).edit().putBoolean(KEY_ACTIVE, false).apply();
            return false;
        }
        return true;
    }

    static List<AlarmEntry> getRemainingAlarms(Context context) {
        List<AlarmEntry> result = new ArrayList<>();
        if (!prefs(context).getBoolean(KEY_ACTIVE, false)) {
            return result;
        }
        for (int number = 1; number <= ALARM_COUNT; number++) {
            long time = prefs(context).getLong(KEY_TIME_PREFIX + number, 0L);
            boolean fired = prefs(context).getBoolean(KEY_FIRED_PREFIX + number, false);
            // An overdue alarm must remain visible until it fires, is
            // cancelled, or is explicitly recorded as missed on restoration.
            if (!fired && time > 0L) {
                result.add(new AlarmEntry(number, time));
            }
        }
        return result;
    }

    static long getStartedAt(Context context) {
        return prefs(context).getLong(KEY_STARTED, 0L);
    }

    static String formatAlarmTime(Context context, long timeMillis) {
        Calendar now = Calendar.getInstance();
        Calendar alarm = Calendar.getInstance();
        alarm.setTimeInMillis(timeMillis);
        boolean sameDay = now.get(Calendar.YEAR) == alarm.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == alarm.get(Calendar.DAY_OF_YEAR);

        if (sameDay) {
            return DateFormat.getTimeFormat(context).format(new Date(timeMillis));
        }

        String pattern = DateFormat.is24HourFormat(context) ? "EEE HH:mm" : "EEE h:mm a";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(timeMillis));
    }

    static String formatCountdown(long targetMillis) {
        if (targetMillis <= System.currentTimeMillis()) {
            return "overdue";
        }
        long remaining = targetMillis - System.currentTimeMillis();
        long totalMinutes = (remaining + 59_999L) / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0L && minutes > 0L) {
            return hours + "h " + minutes + "m";
        }
        if (hours > 0L) {
            return hours + "h";
        }
        return Math.max(1L, minutes) + "m";
    }

    private static boolean scheduleStoredAlarm(Context context, int number) {
        long time = prefs(context).getLong(KEY_TIME_PREFIX + number, 0L);
        return time > System.currentTimeMillis() && scheduleAlarm(context, number, time);
    }

    private static boolean scheduleAlarm(Context context, int number, long triggerAt) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null || !canScheduleExactAlarms(context)) {
            return false;
        }

        PendingIntent fireIntent = alarmPendingIntent(context, number, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent showIntent = new Intent(context, MainActivity.class);
        showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent showPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_BASE + 500 + number,
                showIntent,
                pendingFlags(PendingIntent.FLAG_UPDATE_CURRENT)
        );

        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent);
        try {
            manager.setAlarmClock(info, fireIntent);
            return true;
        } catch (SecurityException exception) {
            return false;
        }
    }

    private static void cancelPendingIntents(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        for (int number = 1; number <= ALARM_COUNT; number++) {
            PendingIntent pendingIntent = alarmPendingIntent(context, number, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                manager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private static PendingIntent alarmPendingIntent(Context context, int number, int baseFlag) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_FIRE_ALARM);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_NUMBER, number);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_BASE + number,
                intent,
                pendingFlags(baseFlag)
        );
    }

    static int pendingFlags(int base) {
        if (Build.VERSION.SDK_INT >= 23) {
            return base | PendingIntent.FLAG_IMMUTABLE;
        }
        return base;
    }

    private static android.content.SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void updateWidget(Context context) {
        AlarmWidgetProvider.updateAll(context);
    }
}
