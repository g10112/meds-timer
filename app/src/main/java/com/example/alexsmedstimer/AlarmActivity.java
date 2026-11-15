package com.example.alexsmedstimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.util.List;

public class AlarmActivity extends Activity {
    private int alarmNumber;
    private final android.window.OnBackInvokedCallback backCallback = this::dismissCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        showAlarm(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showAlarm(intent);
    }

    private void configureWindow() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
    }

    private void showAlarm(Intent intent) {
        alarmNumber = intent == null
                ? 0
                : intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_NUMBER, 0);
        boolean test = alarmNumber == AlarmScheduler.TEST_ALARM_ID;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(Ui.dp(this, 26), Ui.dp(this, 38), Ui.dp(this, 26), Ui.dp(this, 38));
        root.setBackgroundColor(Ui.BG);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(Ui.dp(this, 26) + safe.left, Ui.dp(this, 38) + safe.top,
                    Ui.dp(this, 26) + safe.right, Ui.dp(this, 38) + safe.bottom);
            return insets;
        });

        TextView eyebrow = Ui.text(this, test ? "TEST ALARM" : "MEDICATION REMINDER", 13f, Ui.ACCENT, true);
        eyebrow.setGravity(Gravity.CENTER);
        root.addView(eyebrow, Ui.fullWidth(this, 0));

        TextView title = Ui.text(this, test ? "Alarm test" : "Meds reminder", 40f, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, Ui.fullWidth(this, 12));

        String detail = test
                ? "This confirms sound, vibration and the full-screen reminder."
                : "Reminder " + alarmNumber + " of " + AlarmScheduler.ALARM_COUNT;
        TextView detailView = Ui.text(this, detail, 19f, Ui.MUTED, false);
        detailView.setGravity(Gravity.CENTER);
        detailView.setLineSpacing(0f, 1.2f);
        root.addView(detailView, Ui.fullWidth(this, 12));

        List<AlarmScheduler.AlarmEntry> remaining = AlarmScheduler.getRemainingAlarms(this);
        if (!test && !remaining.isEmpty()) {
            StringBuilder upcoming = new StringBuilder("Still scheduled: ");
            for (int i = 0; i < remaining.size(); i++) {
                if (i > 0) {
                    upcoming.append(" • ");
                }
                upcoming.append(AlarmScheduler.formatAlarmTime(this, remaining.get(i).timeMillis));
            }
            TextView upcomingView = Ui.text(this, upcoming.toString(), 15f, Ui.MUTED, false);
            upcomingView.setGravity(Gravity.CENTER);
            root.addView(upcomingView, Ui.fullWidth(this, 18));
        }

        Button taken = Ui.button(this, test ? "STOP TEST ALARM" : "STOP ALARM", Ui.ACCENT, Ui.BG);
        taken.setOnClickListener(v -> dismissCurrent());
        root.addView(taken, Ui.fullWidth(this, 34));

        if (!test && !remaining.isEmpty()) {
            Button cancelAll = Ui.button(this, "STOP AND CANCEL REMAINING", Ui.CARD, Ui.TEXT);
            cancelAll.setOnClickListener(v -> confirmCancelRemaining());
            root.addView(cancelAll, Ui.fullWidth(this, 12));
        }

        setContentView(root);
    }

    private void dismissCurrent() {
        AlarmService.stop(this);
        finishAndRemoveTask();
    }

    private void confirmCancelRemaining() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel remaining reminders?")
                .setMessage("This ends the current medication reminder session. It cannot be undone.")
                .setNegativeButton("Keep reminders", null)
                .setPositiveButton("Cancel session", (dialog, which) -> {
                    AlarmScheduler.cancelSession(this);
                    AlarmService.stop(this);
                    finishAndRemoveTask();
                })
                .show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    @Override
    public void onStop() {
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        super.onStop();
    }
}
