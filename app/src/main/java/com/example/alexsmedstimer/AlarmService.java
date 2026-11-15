package com.example.alexsmedstimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class AlarmService extends Service {
    static final String ACTION_START = "com.example.alexsmedstimer.START_ALARM";

    private static final String CHANNEL_ID = "alex_meds_alarm_channel";
    private static final int NOTIFICATION_ID = 4701;
    private static final String MISSED_CHANNEL_ID = "meds_missed_reminders";
    private static final int MISSED_NOTIFICATION_ID = 4702;
    private static final String RUNTIME_PREFS = "alex_meds_timer_runtime";
    private static final String KEY_RINGING = "ringing";
    private static final long MAX_RING_TIME_MS = 15L * 60L * 1000L;

    private final Handler stopHandler = new Handler(Looper.getMainLooper());
    private final Runnable safetyStop = new Runnable() {
        @Override
        public void run() {
            stopAlarmAndSelf();
        }
    };

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int alarmNumber = intent == null
                ? 0
                : intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_NUMBER, 0);

        setRinging(this, true);
        startForeground(NOTIFICATION_ID, buildNotification(alarmNumber));
        acquireWakeLock();
        startSound();
        startVibration();
        stopHandler.removeCallbacks(safetyStop);
        stopHandler.postDelayed(safetyStop, MAX_RING_TIME_MS);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopHandler.removeCallbacks(safetyStop);
        stopPlayback();
        setRinging(this, false);
        stopForeground(true);
        super.onDestroy();
    }

    private Notification buildNotification(int alarmNumber) {
        boolean test = alarmNumber == AlarmScheduler.TEST_ALARM_ID;
        String text = test
                ? "10-second test alarm - tap STOP ALARM"
                : "Medication reminder " + alarmNumber + " of " + AlarmScheduler.ALARM_COUNT;

        Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
        fullScreenIntent.putExtra(AlarmReceiver.EXTRA_ALARM_NUMBER, alarmNumber);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                4800 + alarmNumber,
                fullScreenIntent,
                AlarmScheduler.pendingFlags(PendingIntent.FLAG_UPDATE_CURRENT)
        );

        Intent stopIntent = new Intent(this, AlarmStopReceiver.class);
        stopIntent.setAction(AlarmStopReceiver.ACTION_STOP_ALARM);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                this,
                4901,
                stopIntent,
                AlarmScheduler.pendingFlags(PendingIntent.FLAG_UPDATE_CURRENT)
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Meds Timer")
                .setContentText(text)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(fullScreenPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setDeleteIntent(stopPendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "STOP ALARM", stopPendingIntent);

        return builder.build();
    }

    static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Medication alarms",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Full-screen medication reminder alarms");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    /** Do not ring old dose alarms after a reboot: tell the user what was missed. */
    static void showMissedReminder(Context context, int count) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 26
                && manager.getNotificationChannel(MISSED_CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    MISSED_CHANNEL_ID, "Missed medication reminders",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Alerts when reminders were missed while the phone was off");
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openApp = PendingIntent.getActivity(
                context, 4702, open, AlarmScheduler.pendingFlags(PendingIntent.FLAG_UPDATE_CURRENT));
        String message = count == 1
                ? "A reminder was missed while your phone was unavailable."
                : count + " reminders were missed while your phone was unavailable.";
        Notification notice = new Notification.Builder(context, MISSED_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Meds Timer: missed reminder")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(
                        message + " Check your prescribed schedule. Do not double up a dose."))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build();
        manager.notify(MISSED_NOTIFICATION_ID, notice);
    }

    static void stop(Context context) {
        context.stopService(new Intent(context, AlarmService.class));
        setRinging(context, false);
    }

    static boolean isRinging(Context context) {
        return runtimePrefs(context).getBoolean(KEY_RINGING, false);
    }

    private void startSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return;
        }

        Uri chosen = AlarmPreferences.getAlarmUri(this);
        if (!startSoundUri(chosen)) {
            Uri fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (fallback == null) {
                fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            startSoundUri(fallback);
        }
    }

    private boolean startSoundUri(Uri alarmUri) {
        if (alarmUri == null) {
            return false;
        }
        try {
            stopMediaPlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            return true;
        } catch (Exception ignored) {
            stopMediaPlayer();
            return false;
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        long[] pattern = {0L, 650L, 350L, 650L, 800L};
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void acquireWakeLock() {
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (manager == null) {
            return;
        }
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MedsTimer:AlarmWakeLock");
        wakeLock.acquire(MAX_RING_TIME_MS);
    }

    private void stopAlarmAndSelf() {
        stopPlayback();
        setRinging(this, false);
        stopForeground(true);
        stopSelf();
    }

    private void stopPlayback() {
        stopMediaPlayer();
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    private void stopMediaPlayer() {
        if (mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.stop();
        } catch (Exception ignored) {
        }
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private static void setRinging(Context context, boolean ringing) {
        runtimePrefs(context).edit().putBoolean(KEY_RINGING, ringing).apply();
    }

    private static SharedPreferences runtimePrefs(Context context) {
        return context.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE);
    }
}
