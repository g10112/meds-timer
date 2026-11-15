# Make a change to Meds Timer

Meds Timer is open source. You can change it or suggest an improvement.

## Change settings without coding

Open the app and tap **Edit reminder times**. Enter three increasing intervals after Start in H:MM format, then save. The new times apply the next time you start reminders.

Tap **Choose alarm sound** to use a different alarm tone. Four home-screen widget sizes are available.

## Edit the app yourself

1. Get the code from [GitHub](https://github.com/g10112/meds-timer): choose **Code → Download ZIP**.
2. Open the folder in Android Studio. Install Android SDK 36 and JDK 17 if prompted.
3. Change one thing, then build using **Build → Build APK(s)**.
4. Check the change on your own phone. To suggest it for the official app, [open a pull request](https://github.com/g10112/meds-timer/pulls).

**Where to change things**

| What | File |
|---|---|
| App name | `app/src/main/res/values/strings.xml` |
| Main-screen words | `app/src/main/java/com/example/alexsmedstimer/MainActivity.java` |
| Alarm-screen words | `app/src/main/java/com/example/alexsmedstimer/AlarmActivity.java` |
| Widget text | `app/src/main/java/com/example/alexsmedstimer/AlarmWidgetProvider.java` |
| Colours | `app/src/main/java/com/example/alexsmedstimer/Ui.java` |
| Default intervals | `app/src/main/java/com/example/alexsmedstimer/ReminderSettings.java` |

**Do not change the Android application ID, signing keys, alarm IDs or preference keys without a migration plan.** Those affect existing installations and future updates.

[Build and publish](BUILD_AND_PUBLISH.md) · [Contribute](../CONTRIBUTING.md)
