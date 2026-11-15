package com.example.alexsmedstimer;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

/**
 * All editable reminder timing defaults live here.
 *
 * Times are measured in whole minutes AFTER the user presses Start.
 * They are not hours of the day and are not intervals between reminders.
 *
 * This preferences file is separate from AlarmScheduler's session state, so
 * ending a session never wipes out the user's chosen reminder times.
 */
final class ReminderSettings {
    static final int COUNT = 3;
    static final long MAX_DELAY_MINUTES = 48L * 60L;
    static final long MINUTE_MS = 60_000L;
    private static final long[] DEFAULT_MINUTES = {210L, 420L, 630L};
    private static final String PREFS = "meds_timer_options";
    private static final String KEY_DELAY_PREFIX = "delay_minutes_";

    private ReminderSettings() {}

    static long[] getDefaultMinutes() {
        return DEFAULT_MINUTES.clone();
    }

    static long[] getDelayMinutes(Context context) {
        SharedPreferences options = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long[] minutes = new long[COUNT];
        for (int index = 0; index < COUNT; index++) {
            minutes[index] = options.getLong(KEY_DELAY_PREFIX + (index + 1), DEFAULT_MINUTES[index]);
        }
        // Invalid/stale preferences must never lead to unsafe alarm schedules.
        return isValidDelayMinutes(minutes) ? minutes : getDefaultMinutes();
    }

    static synchronized boolean saveDelayMinutes(Context context, long[] minutes) {
        if (!isValidDelayMinutes(minutes)) {
            return false;
        }
        SharedPreferences.Editor edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        for (int index = 0; index < COUNT; index++) {
            edit.putLong(KEY_DELAY_PREFIX + (index + 1), minutes[index]);
        }
        return edit.commit();
    }

    static boolean isValidDelayMinutes(long[] minutes) {
        if (minutes == null || minutes.length != COUNT) {
            return false;
        }
        long previous = 0L;
        for (long delay : minutes) {
            if (delay <= previous || delay > MAX_DELAY_MINUTES) {
                return false;
            }
            previous = delay;
        }
        return true;
    }

    /** Accepts a simple H:MM duration after Start, never a clock time. */
    static long parseClockDuration(String value) {
        if (value == null || !value.trim().matches("[0-9]{1,2}:[0-9]{2}")) {
            return -1L;
        }
        String[] parts = value.trim().split(":");
        long hours = Long.parseLong(parts[0]);
        long minutes = Long.parseLong(parts[1]);
        return minutes >= 60L ? -1L : hours * 60L + minutes;
    }

    static String formatClockDuration(long minutes) {
        return String.format(Locale.US, "%d:%02d", minutes / 60, minutes % 60);
    }

    static String formatDelayMinutes(long minutes) {
        long hours = minutes / 60L;
        long remainder = minutes % 60L;
        if (hours == 0L) {
            return minutes + (minutes == 1L ? " minute" : " minutes");
        }
        String hoursText = hours + (hours == 1L ? " hour" : " hours");
        return remainder == 0L ? hoursText : hoursText + " " + remainder + (remainder == 1L ? " minute" : " minutes");
    }
}
