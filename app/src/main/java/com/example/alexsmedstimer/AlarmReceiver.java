package com.example.alexsmedstimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    static final String ACTION_FIRE_ALARM = "com.example.alexsmedstimer.FIRE_ALARM";
    static final String EXTRA_ALARM_NUMBER = "alarm_number";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_FIRE_ALARM.equals(intent.getAction())) {
            return;
        }

        int alarmNumber = intent.getIntExtra(EXTRA_ALARM_NUMBER, 0);
        boolean isTest = alarmNumber == AlarmScheduler.TEST_ALARM_ID;
        if (!isTest && (alarmNumber < 1 || alarmNumber > AlarmScheduler.ALARM_COUNT)) {
            return;
        }

        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.setAction(AlarmService.ACTION_START);
        serviceIntent.putExtra(EXTRA_ALARM_NUMBER, alarmNumber);

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            if (!isTest) {
                AlarmScheduler.markFired(context, alarmNumber);
            }
        } catch (RuntimeException error) {
            if (!isTest) {
                AlarmScheduler.markMissed(context, alarmNumber);
            }
        }
    }
}
