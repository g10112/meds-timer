package com.example.alexsmedstimer;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

final class AlarmPreferences {
    private static final String PREFS = "alex_meds_timer_settings";
    private static final String KEY_SOUND_URI = "alarm_sound_uri";
    private static final String KEY_SOUND_TITLE = "alarm_sound_title";

    private AlarmPreferences() {}

    static Uri getAlarmUri(Context context) {
        String stored = prefs(context).getString(KEY_SOUND_URI, null);
        if (stored != null && stored.length() > 0) {
            try {
                return Uri.parse(stored);
            } catch (Exception ignored) {
            }
        }

        Uri fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (fallback == null) {
            fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return fallback;
    }

    static String getAlarmTitle(Context context) {
        String stored = prefs(context).getString(KEY_SOUND_TITLE, null);
        if (stored != null && stored.length() > 0) {
            return stored;
        }
        return resolveTitle(context, getAlarmUri(context));
    }

    static void setAlarmSound(Context context, Uri uri) {
        if (uri == null) {
            return;
        }
        String title = resolveTitle(context, uri);
        prefs(context).edit()
                .putString(KEY_SOUND_URI, uri.toString())
                .putString(KEY_SOUND_TITLE, title)
                .apply();
    }

    private static String resolveTitle(Context context, Uri uri) {
        if (uri == null) {
            return "Default alarm sound";
        }
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
            if (ringtone != null) {
                String title = ringtone.getTitle(context);
                if (title != null && title.trim().length() > 0) {
                    return title;
                }
            }
        } catch (Exception ignored) {
        }
        return "Selected alarm sound";
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
