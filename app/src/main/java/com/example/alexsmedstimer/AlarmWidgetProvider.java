package com.example.alexsmedstimer;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.List;

public class AlarmWidgetProvider extends AppWidgetProvider {
    protected static final int VARIANT_MINI = 0;
    protected static final int VARIANT_COMPACT = 1;
    protected static final int VARIANT_STANDARD = 2;
    protected static final int VARIANT_LARGE = 3;

    private static final String ACTION_WIDGET_TAP = "com.example.alexsmedstimer.WIDGET_TAP";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateOne(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    getClass(),
                    getWidgetLayout(),
                    getWidgetVariant()
            );
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            android.os.Bundle newOptions
    ) {
        updateOne(
                context,
                appWidgetManager,
                appWidgetId,
                getClass(),
                getWidgetLayout(),
                getWidgetVariant()
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || !ACTION_WIDGET_TAP.equals(intent.getAction())) {
            return;
        }

        if (AlarmScheduler.hasActiveSession(context)) {
            openApp(context, false);
            return;
        }

        if (!hasNotificationPermission(context) || !AlarmScheduler.canScheduleExactAlarms(context)) {
            openApp(context, true);
            return;
        }

        if (AlarmScheduler.scheduleNewSession(context)) {
            List<AlarmScheduler.AlarmEntry> remaining = AlarmScheduler.getRemainingAlarms(context);
            String next = remaining.isEmpty()
                    ? ""
                    : AlarmScheduler.formatAlarmTime(context, remaining.get(0).timeMillis);
            Toast.makeText(context, "Meds timer started. First alarm: " + next, Toast.LENGTH_LONG).show();
        }
        updateAll(context);
    }

    protected int getWidgetLayout() {
        return R.layout.widget_alarm;
    }

    protected int getWidgetVariant() {
        return VARIANT_STANDARD;
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        updateProvider(
                context,
                manager,
                AlarmWidgetMiniProvider.class,
                R.layout.widget_alarm_mini,
                VARIANT_MINI
        );
        updateProvider(
                context,
                manager,
                AlarmWidgetCompactProvider.class,
                R.layout.widget_alarm_compact,
                VARIANT_COMPACT
        );
        updateProvider(
                context,
                manager,
                AlarmWidgetProvider.class,
                R.layout.widget_alarm,
                VARIANT_STANDARD
        );
        updateProvider(
                context,
                manager,
                AlarmWidgetLargeProvider.class,
                R.layout.widget_alarm_large,
                VARIANT_LARGE
        );
    }

    private static void updateProvider(
            Context context,
            AppWidgetManager manager,
            Class<?> providerClass,
            int layout,
            int variant
    ) {
        ComponentName provider = new ComponentName(context, providerClass);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) {
            updateOne(context, manager, id, providerClass, layout, variant);
        }
    }

    private static void updateOne(
            Context context,
            AppWidgetManager manager,
            int appWidgetId,
            Class<?> providerClass,
            int layout,
            int variant
    ) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        views.setTextViewText(
                R.id.widget_title,
                variant == VARIANT_MINI ? "MEDS" : "Meds Timer"
        );

        List<AlarmScheduler.AlarmEntry> remaining = AlarmScheduler.getRemainingAlarms(context);
        if (remaining.isEmpty()) {
            showInactive(context, views, variant);
        } else {
            showActive(context, views, remaining, variant);
        }

        Intent tapIntent = new Intent(context, providerClass);
        tapIntent.setAction(ACTION_WIDGET_TAP);
        PendingIntent tapPendingIntent = PendingIntent.getBroadcast(
                context,
                6100 + appWidgetId,
                tapIntent,
                AlarmScheduler.pendingFlags(PendingIntent.FLAG_UPDATE_CURRENT)
        );
        views.setOnClickPendingIntent(R.id.widget_root, tapPendingIntent);
        manager.updateAppWidget(appWidgetId, views);
    }

    private static void showInactive(Context context, RemoteViews views, int variant) {
        views.setTextViewText(R.id.widget_status, "Tap to start");
        long[] delays = ReminderSettings.getDelayMinutes(context);
        String first = ReminderSettings.formatClockDuration(delays[0]);
        String second = ReminderSettings.formatClockDuration(delays[1]);
        String third = ReminderSettings.formatClockDuration(delays[2]);

        if (variant == VARIANT_MINI) {
            views.setTextViewText(R.id.widget_status, "START");
            views.setTextViewText(R.id.widget_times, "3 reminders");
            views.setViewVisibility(R.id.widget_hint, View.GONE);
        } else if (variant == VARIANT_COMPACT) {
            views.setTextViewText(R.id.widget_times, first + " / " + second + " / " + third);
            views.setViewVisibility(R.id.widget_hint, View.GONE);
        } else if (variant == VARIANT_LARGE) {
            views.setTextViewText(R.id.widget_times,
                    "Reminder 1  +" + first + "\nReminder 2  +" + second +
                    "\nReminder 3  +" + third);
            views.setTextViewText(R.id.widget_hint, "Tap to start your session");
            views.setViewVisibility(R.id.widget_hint, View.VISIBLE);
        } else {
            views.setTextViewText(R.id.widget_times,
                    first + "  •  " + second + "  •  " + third);
            views.setTextViewText(R.id.widget_hint, "Tap to start your three reminders");
            views.setViewVisibility(R.id.widget_hint, View.VISIBLE);
        }
    }

    private static void showActive(
            Context context,
            RemoteViews views,
            List<AlarmScheduler.AlarmEntry> remaining,
            int variant
    ) {
        AlarmScheduler.AlarmEntry next = remaining.get(0);

        if (variant == VARIANT_MINI) {
            views.setTextViewText(
                    R.id.widget_status,
                    AlarmScheduler.formatAlarmTime(context, next.timeMillis)
            );
            views.setTextViewText(
                    R.id.widget_times,
                    remaining.size() + " left"
            );
            views.setViewVisibility(R.id.widget_hint, View.GONE);
            return;
        }

        if (variant == VARIANT_COMPACT) {
            views.setTextViewText(
                    R.id.widget_status,
                    "Next " + AlarmScheduler.formatAlarmTime(context, next.timeMillis)
            );
            views.setTextViewText(
                    R.id.widget_times,
                    remaining.size() + " remaining · " + (next.timeMillis <= System.currentTimeMillis()
                            ? "overdue" : "in " + AlarmScheduler.formatCountdown(next.timeMillis))
            );
            views.setViewVisibility(R.id.widget_hint, View.GONE);
            return;
        }

        views.setTextViewText(
                R.id.widget_status,
                remaining.size() + (remaining.size() == 1 ? " alarm remaining" : " alarms remaining")
        );

        StringBuilder times = new StringBuilder();
        for (int i = 0; i < remaining.size(); i++) {
            AlarmScheduler.AlarmEntry alarm = remaining.get(i);
            if (i > 0) {
                times.append(variant == VARIANT_LARGE ? "\n" : "  •  ");
            }
            if (variant == VARIANT_LARGE) {
                times.append("Alarm ")
                        .append(alarm.number)
                        .append("  ")
                        .append(AlarmScheduler.formatAlarmTime(context, alarm.timeMillis))
                        .append(alarm.timeMillis <= System.currentTimeMillis()
                                ? "  (OVERDUE)"
                                : "  (in " + AlarmScheduler.formatCountdown(alarm.timeMillis) + ")");
            } else {
                times.append(AlarmScheduler.formatAlarmTime(context, alarm.timeMillis));
            }
        }
        views.setTextViewText(R.id.widget_times, times.toString());
        views.setTextViewText(
                R.id.widget_hint,
                variant == VARIANT_LARGE
                        ? "Tap to open · active session cannot restart"
                        : (next.timeMillis <= System.currentTimeMillis() ? "Overdue · " : "Next in " + AlarmScheduler.formatCountdown(next.timeMillis) + " · ") + "tap to view"
        );
        views.setViewVisibility(R.id.widget_hint, View.VISIBLE);
    }

    private static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void openApp(Context context, boolean startRequested) {
        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_START_REQUESTED, startRequested);
        context.startActivity(launch);
    }
}
