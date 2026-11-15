package com.example.alexsmedstimer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.util.List;

public class MainActivity extends Activity {
    public static final String EXTRA_START_REQUESTED = "start_requested";

    private static final int REQUEST_NOTIFICATIONS = 7101;
    private static final int REQUEST_ALARM_SOUND = 7102;
    private static final int ACTION_NONE = 0;
    private static final int ACTION_START_SESSION = 1;
    private static final int ACTION_TEST_ALARM = 2;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshUi();
            refreshHandler.postDelayed(this, 30_000L);
        }
    };

    private TextView sessionText;
    private TextView missedText;
    private TextView readinessText;
    private TextView alarmSoundText;
    private TextView scheduleDescriptionText;
    private TextView reminderTimesText;
    private Button startButton;
    private Button cancelButton;
    private Button setupButton;
    private Button stopRingingButton;
    private int pendingAction = ACTION_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Meds Timer");
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        AlarmService.createNotificationChannel(this);
        buildUi();
        if (readLaunchIntent(getIntent())) {
            beginAction(ACTION_START_SESSION);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (readLaunchIntent(intent)) {
            beginAction(ACTION_START_SESSION);
        }
        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.post(refreshRunnable);
        continuePendingActionIfReady();
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private boolean readLaunchIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_REQUESTED, false)) {
            intent.removeExtra(EXTRA_START_REQUESTED);
            return true;
        }
        return false;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 28), Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView eyebrow = Ui.text(this, "MEDICATION REMINDER SESSION", 12f, Ui.ACCENT, true);
        content.addView(eyebrow);

        TextView title = Ui.text(this, "Meds Timer", 36f, Ui.TEXT, true);
        content.addView(title, Ui.fullWidth(this, 6));

        scheduleDescriptionText = Ui.text(
                this,
                "The three reminders are scheduled from when you tap Start.",
                17f,
                Ui.MUTED,
                false
        );
        scheduleDescriptionText.setLineSpacing(0f, 1.18f);
        content.addView(scheduleDescriptionText, Ui.fullWidth(this, 8));

        startButton = Ui.button(this, "START MEDS TIMER", Ui.ACCENT, Ui.BG);
        startButton.setOnClickListener(v -> beginAction(ACTION_START_SESSION));
        content.addView(startButton, Ui.fullWidth(this, 26));

        LinearLayout sessionCard = Ui.card(this);
        TextView sessionHeading = Ui.text(this, "CURRENT SESSION", 12f, Ui.ACCENT, true);
        sessionCard.addView(sessionHeading);
        sessionText = Ui.text(this, "No active session.", 18f, Ui.TEXT, true);
        sessionText.setLineSpacing(Ui.dp(this, 3), 1.14f);
        sessionCard.addView(sessionText, Ui.fullWidth(this, 10));
        missedText = Ui.text(this, "", 15f, Ui.DANGER, true);
        sessionCard.addView(missedText, Ui.fullWidth(this, 10));
        content.addView(sessionCard, Ui.fullWidth(this, 16));

        stopRingingButton = Ui.button(this, "STOP RINGING ALARM", Ui.DANGER, Ui.BG);
        stopRingingButton.setOnClickListener(v -> {
            AlarmService.stop(this);
            Toast.makeText(this, "Alarm stopped.", Toast.LENGTH_SHORT).show();
            refreshUi();
        });
        content.addView(stopRingingButton, Ui.fullWidth(this, 12));

        cancelButton = Ui.button(this, "CANCEL REMAINING ALARMS", Ui.CARD, Ui.TEXT);
        cancelButton.setOnClickListener(v -> showCancelConfirmation());
        content.addView(cancelButton, Ui.fullWidth(this, 12));

        LinearLayout safetyCard = Ui.card(this);
        TextView safetyHeading = Ui.text(this, "ACCIDENTAL-TAP PROTECTION", 12f, Ui.SUCCESS, true);
        safetyCard.addView(safetyHeading);
        TextView safetyText = Ui.text(
                this,
                "Once a session is active, neither the widget nor this app can restart it. Tapping the widget only shows the alarms still remaining.",
                15f,
                Ui.MUTED,
                false
        );
        safetyText.setLineSpacing(0f, 1.18f);
        safetyCard.addView(safetyText, Ui.fullWidth(this, 8));
        content.addView(safetyCard, Ui.fullWidth(this, 16));

        LinearLayout readinessCard = Ui.card(this);
        TextView readinessHeading = Ui.text(this, "PHONE READINESS", 12f, Ui.ACCENT, true);
        readinessCard.addView(readinessHeading);
        readinessText = Ui.text(this, "Checking permissions…", 15f, Ui.TEXT, false);
        readinessText.setLineSpacing(Ui.dp(this, 3), 1.12f);
        readinessCard.addView(readinessText, Ui.fullWidth(this, 9));
        setupButton = Ui.button(this, "COMPLETE SETUP", Ui.CARD_BORDER, Ui.TEXT);
        setupButton.setOnClickListener(v -> completeNextSetupStep());
        readinessCard.addView(setupButton, Ui.fullWidth(this, 13));
        content.addView(readinessCard, Ui.fullWidth(this, 16));

        LinearLayout soundCard = Ui.card(this);
        TextView soundHeading = Ui.text(this, "ALARM SOUND", 12f, Ui.ACCENT, true);
        soundCard.addView(soundHeading);
        alarmSoundText = Ui.text(this, "Default alarm sound", 17f, Ui.TEXT, true);
        soundCard.addView(alarmSoundText, Ui.fullWidth(this, 8));
        TextView soundHelp = Ui.text(
                this,
                "Choose any alarm tone installed on this phone. The same sound is used for tests and medication reminders.",
                14f,
                Ui.MUTED,
                false
        );
        soundHelp.setLineSpacing(0f, 1.15f);
        soundCard.addView(soundHelp, Ui.fullWidth(this, 6));
        Button chooseSoundButton = Ui.button(this, "CHOOSE ALARM SOUND", Ui.CARD_BORDER, Ui.TEXT);
        chooseSoundButton.setOnClickListener(v -> chooseAlarmSound());
        soundCard.addView(chooseSoundButton, Ui.fullWidth(this, 12));
        content.addView(soundCard, Ui.fullWidth(this, 16));

        LinearLayout timesCard = Ui.card(this);
        timesCard.addView(Ui.text(this, "REMINDER TIMES", 12f, Ui.ACCENT, true));
        reminderTimesText = Ui.text(this, "", 16f, Ui.TEXT, false);
        timesCard.addView(reminderTimesText, Ui.fullWidth(this, 9));
        timesCard.addView(Ui.text(this,
                "Intervals are measured from Start. Use only timings from your prescribed schedule. Changes affect your next session, not alarms already running.",
                14f, Ui.MUTED, false), Ui.fullWidth(this, 10));
        Button changeTimesButton = Ui.button(this, "EDIT REMINDER TIMES", Ui.CARD_BORDER, Ui.TEXT);
        changeTimesButton.setOnClickListener(v -> showEditReminderTimes());
        timesCard.addView(changeTimesButton, Ui.fullWidth(this, 10));
        content.addView(timesCard, Ui.fullWidth(this, 16));

        Button testButton = Ui.button(this, "TEST ALARM IN 10 SECONDS", Ui.CARD_BORDER, Ui.TEXT);
        testButton.setOnClickListener(v -> beginAction(ACTION_TEST_ALARM));
        content.addView(testButton, Ui.fullWidth(this, 12));

        Button widgetButton = Ui.button(this, "ADD HOME-SCREEN WIDGET", Ui.CARD_BORDER, Ui.TEXT);
        widgetButton.setOnClickListener(v -> requestWidgetPin());
        content.addView(widgetButton, Ui.fullWidth(this, 12));

        Button fullScreenButton = Ui.button(this, "FULL-SCREEN ALARM SETTINGS", Ui.CARD, Ui.MUTED);
        fullScreenButton.setOnClickListener(v -> openFullScreenIntentSettings());
        content.addView(fullScreenButton, Ui.fullWidth(this, 12));

        Button privacyButton = Ui.button(this, "PRIVACY & SAFETY", Ui.CARD, Ui.TEXT);
        privacyButton.setOnClickListener(v -> showPrivacyAndSafety());
        content.addView(privacyButton, Ui.fullWidth(this, 12));

        TextView footer = Ui.text(
                this,
                "Remaining alarms are restored after a phone restart. Cancelling a session always requires confirmation.",
                13f,
                Ui.MUTED,
                false
        );
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        footer.setLineSpacing(0f, 1.18f);
        content.addView(footer, Ui.fullWidth(this, 24));

        setContentView(scroll);
    }

    private void beginAction(int action) {
        if (action == ACTION_START_SESSION && AlarmScheduler.hasActiveSession(this)) {
            Toast.makeText(this, "A session is already active. Nothing was changed.", Toast.LENGTH_LONG).show();
            refreshUi();
            return;
        }

        pendingAction = action;
        if (!hasNotificationPermission()) {
            requestNotificationPermission();
            return;
        }
        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            openExactAlarmSettings();
            return;
        }
        executePendingAction();
    }

    private void continuePendingActionIfReady() {
        refreshUi();
        if (pendingAction == ACTION_NONE) {
            return;
        }
        if (!hasNotificationPermission() || !AlarmScheduler.canScheduleExactAlarms(this)) {
            return;
        }
        executePendingAction();
    }

    private void executePendingAction() {
        int action = pendingAction;
        pendingAction = ACTION_NONE;

        if (action == ACTION_START_SESSION) {
            if (AlarmScheduler.hasActiveSession(this)) {
                Toast.makeText(this, "A session is already active. Nothing was changed.", Toast.LENGTH_LONG).show();
                refreshUi();
                return;
            }
            if (AlarmScheduler.scheduleNewSession(this)) {
                List<AlarmScheduler.AlarmEntry> alarms = AlarmScheduler.getRemainingAlarms(this);
                String first = alarms.isEmpty() ? "" : AlarmScheduler.formatAlarmTime(this, alarms.get(0).timeMillis);
                Toast.makeText(this, "Session started. First alarm: " + first, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "The session could not be scheduled.", Toast.LENGTH_LONG).show();
            }
        } else if (action == ACTION_TEST_ALARM) {
            boolean scheduled = AlarmScheduler.scheduleTestAlarm(this);
            Toast.makeText(this, scheduled ? "Alarm check scheduled for 10 seconds from now."
                    : "The alarm could not be scheduled. Check your permissions.", Toast.LENGTH_LONG).show();
        }
        refreshUi();
    }

    private void refreshUi() {
        if (sessionText == null) {
            return;
        }

        List<AlarmScheduler.AlarmEntry> alarms = AlarmScheduler.getRemainingAlarms(this);
        if (alarms.isEmpty()) {
            sessionText.setText("No active session.\nTap START or the home-screen widget.");
            sessionText.setTextColor(Ui.TEXT);
            startButton.setText("START MEDS TIMER");
            startButton.setEnabled(true);
            Ui.visible(cancelButton, false);
        } else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < alarms.size(); i++) {
                AlarmScheduler.AlarmEntry alarm = alarms.get(i);
                if (i > 0) {
                    text.append("\n");
                }
                text.append("Alarm ")
                        .append(alarm.number)
                        .append("  ·  ")
                        .append(AlarmScheduler.formatAlarmTime(this, alarm.timeMillis))
                        .append(alarm.timeMillis <= System.currentTimeMillis() ? "  ·  OVERDUE" :
                                "  ·  in " + AlarmScheduler.formatCountdown(alarm.timeMillis));
            }
            sessionText.setText(text.toString());
            sessionText.setTextColor(Ui.SUCCESS);
            startButton.setText("SESSION ACTIVE — TAP WIDGET TO VIEW");
            startButton.setEnabled(false);
            Ui.visible(cancelButton, true);
        }

        int missed = AlarmScheduler.getMissedCount(this);
        if (missed > 0) {
            missedText.setText(missed == 1
                    ? "1 reminder was missed while your phone was unavailable. Check your schedule; do not double up a dose."
                    : missed + " reminders were missed while your phone was unavailable. Check your schedule; do not double up a dose.");
        }
        Ui.visible(missedText, missed > 0);

        long[] delays = ReminderSettings.getDelayMinutes(this);
        scheduleDescriptionText.setText("Your next session will ring after "
                + ReminderSettings.formatDelayMinutes(delays[0]) + ", "
                + ReminderSettings.formatDelayMinutes(delays[1]) + " and "
                + ReminderSettings.formatDelayMinutes(delays[2]) + ".");
        reminderTimesText.setText("Reminder 1: " + ReminderSettings.formatDelayMinutes(delays[0])
                + "\nReminder 2: " + ReminderSettings.formatDelayMinutes(delays[1])
                + "\nReminder 3: " + ReminderSettings.formatDelayMinutes(delays[2]));
        Ui.visible(stopRingingButton, AlarmService.isRinging(this));
        alarmSoundText.setText(AlarmPreferences.getAlarmTitle(this));

        boolean notifications = hasNotificationPermission();
        boolean exact = AlarmScheduler.canScheduleExactAlarms(this);
        readinessText.setText(
                (notifications ? "✓" : "✕") + " Notifications\n"
                        + (exact ? "✓" : "✕") + " Exact alarm timing\n"
                        + "✓ Alarm sound and vibration"
        );
        readinessText.setTextColor(notifications && exact ? Ui.SUCCESS : Ui.DANGER);

        if (!notifications) {
            setupButton.setText("ALLOW NOTIFICATIONS");
            setupButton.setEnabled(true);
        } else if (!exact) {
            setupButton.setText("ALLOW EXACT ALARMS");
            setupButton.setEnabled(true);
        } else {
            setupButton.setText("READY");
            setupButton.setEnabled(false);
        }

        AlarmWidgetProvider.updateAll(this);
    }

    private void showEditReminderTimes() {
        long[] existing = ReminderSettings.getDelayMinutes(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 22), Ui.dp(this, 12), Ui.dp(this, 22), Ui.dp(this, 12));
        scroll.addView(form);
        form.addView(Ui.text(this,
                "Enter H:MM after Start, e.g. 3:30. Times must increase and be within 48 hours. " +
                "Only use your prescribed intervals. Active alarms will not change.",
                15f, Ui.TEXT, false), Ui.fullWidth(this, 12));

        EditText[] inputs = new EditText[ReminderSettings.COUNT];
        for (int i = 0; i < ReminderSettings.COUNT; i++) {
            String label = "Reminder " + (i + 1) + " — hours:minutes after Start";
            form.addView(Ui.text(this, label, 15f, Ui.TEXT, true), Ui.fullWidth(this, 4));
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
            input.setText(ReminderSettings.formatClockDuration(existing[i]));
            input.setSelectAllOnFocus(true);
            input.setContentDescription(label);
            input.setTextColor(Ui.TEXT);
            inputs[i] = input;
            form.addView(input, Ui.fullWidth(this, 12));
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit reminder times")
                .setView(scroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save for next session", null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    long[] values = new long[ReminderSettings.COUNT];
                    for (int i = 0; i < inputs.length; i++) {
                        values[i] = ReminderSettings.parseClockDuration(inputs[i].getText().toString());
                        if (values[i] < 0) {
                            inputs[i].setError("Use H:MM, for example 3:30");
                            inputs[i].requestFocus();
                            return;
                        }
                    }
                    if (!ReminderSettings.isValidDelayMinutes(values)) {
                        Toast.makeText(this, "Times must increase and be within 48 hours.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!ReminderSettings.saveDelayMinutes(this, values)) {
                        Toast.makeText(this, "Unable to save reminder times.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    dialog.dismiss();
                    refreshUi();
                    Toast.makeText(this, "Saved for the next session.", Toast.LENGTH_LONG).show();
                }));
        dialog.show();
    }

    private void showCancelConfirmation() {
        if (!AlarmScheduler.hasActiveSession(this)) {
            refreshUi();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Cancel this session?")
                .setMessage("All remaining medication alarms in this session will be removed.")
                .setNegativeButton("Keep alarms", null)
                .setPositiveButton("Cancel session", (dialog, which) -> {
                    AlarmScheduler.cancelSession(this);
                    Toast.makeText(this, "Remaining alarms cancelled.", Toast.LENGTH_LONG).show();
                    refreshUi();
                })
                .show();
    }

    private void completeNextSetupStep() {
        if (!hasNotificationPermission()) {
            requestNotificationPermission();
        } else if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            openExactAlarmSettings();
        }
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) {
            return;
        }
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            pendingAction = ACTION_NONE;
            Toast.makeText(this, "Notifications are required for medication alarms.", Toast.LENGTH_LONG).show();
        } else {
            continuePendingActionIfReady();
        }
        refreshUi();
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }
        try {
            Intent intent = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception error) {
            openAppSettings();
        }
    }

    private void openFullScreenIntentSettings() {
        try {
            Intent intent = new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT");
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception error) {
            openAppSettings();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void chooseAlarmSound() {
        try {
            Intent picker = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose alarm sound");
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, AlarmPreferences.getAlarmUri(this));
            startActivityForResult(picker, REQUEST_ALARM_SOUND);
        } catch (Exception error) {
            Toast.makeText(this, "The phone's alarm sound picker could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_ALARM_SOUND || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri selected = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (selected != null) {
            AlarmPreferences.setAlarmSound(this, selected);
            Toast.makeText(this, "Alarm sound changed.", Toast.LENGTH_SHORT).show();
            refreshUi();
        }
    }

    /** Public, offline privacy information, accessible without an account or browser. */
    private void showPrivacyAndSafety() {
        String info = "Meds Timer works entirely offline. No account is needed. "
                + "The app has no ads, subscriptions, analytics or tracking. "
                + "It does not collect medication names, prescriptions, contact details, "
                + "location or other personal information. No data is shared with anyone.\n\n"
                + "Your reminder intervals, alarm sound and current session are stored "
                + "only on this phone. Android backup of those settings is disabled. "
                + "Uninstalling the app removes its locally stored settings.\n\n"
                + "Permissions are used only to schedule and show medication reminder "
                + "alarms: notifications, exact alarms, full-screen alerts, vibration, "
                + "wake locks and restart recovery. Phone settings can prevent an "
                + "alarm from ringing.\n\n"
                + "Meds Timer is a reminder, not a dose tracker. This app is not a "
                + "medical device and does not diagnose, treat, cure or prevent any "
                + "medical condition. Follow your prescribed schedule, keep a "
                + "backup reminder for time-critical doses and consult a healthcare "
                + "professional for medical advice. Never double up a dose because "
                + "a reminder was missed.\n\n"
                + "Open source: github.com/g10112/meds-timer";
        new AlertDialog.Builder(this)
                .setTitle("Privacy & safety")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    private void requestWidgetPin() {
        final String[] choices = {
                "Mini - 1 x 1",
                "Compact - 2 x 1",
                "Standard - 3 x 2",
                "Large - 4 x 3"
        };
        new AlertDialog.Builder(this)
                .setTitle("Choose widget size")
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) {
                        pinWidget(AlarmWidgetMiniProvider.class);
                    } else if (which == 1) {
                        pinWidget(AlarmWidgetCompactProvider.class);
                    } else if (which == 3) {
                        pinWidget(AlarmWidgetLargeProvider.class);
                    } else {
                        pinWidget(AlarmWidgetProvider.class);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pinWidget(Class<?> providerClass) {
        if (Build.VERSION.SDK_INT < 26) {
            Toast.makeText(this, "Long-press the home screen and add the widget manually.", Toast.LENGTH_LONG).show();
            return;
        }
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, providerClass);
        if (!manager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(this, "Long-press the home screen and choose one of the Meds Timer widgets.", Toast.LENGTH_LONG).show();
            return;
        }
        manager.requestPinAppWidget(provider, null, null);
    }
}
