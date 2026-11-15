package com.example.alexsmedstimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmStopReceiver extends BroadcastReceiver {
    static final String ACTION_STOP_ALARM = "com.example.alexsmedstimer.NOTIFICATION_STOP_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_STOP_ALARM.equals(intent.getAction())) {
            AlarmService.stop(context);
        }
    }
}
