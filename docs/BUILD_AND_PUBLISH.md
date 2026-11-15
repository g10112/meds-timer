# Build and release Meds Timer

This page is for people maintaining the official project. **Users should download the APK from [Releases](https://github.com/g10112/meds-timer/releases/latest).**

## Build from source

You need Android Studio, JDK 17 and Android SDK 36. Clone the repository and open it in Android Studio.

The project uses Gradle 8.13 and Android Gradle Plugin 8.13.2. Its Gradle wrapper is included. From a terminal:

    ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

On Windows, use gradlew.bat instead of ./gradlew. This command builds an internal developer APK only; **do not publish it**.

## Official release

Only the maintainer with the permanent signing key can publish an installable update.

1. Keep the original signing keystore and passwords securely backed up. Never commit or send them by email.
2. Configure the repository Actions secrets: MEDS_RELEASE_KEYSTORE_BASE64, MEDS_RELEASE_STORE_PASSWORD, MEDS_RELEASE_KEY_ALIAS and MEDS_RELEASE_KEY_PASSWORD.
3. Make changes and update versionName and versionCode in app/build.gradle.kts. Both must increase for subsequent releases as appropriate.
4. Check scheduled alarms, widgets, restart recovery, exact-alarm permissions, full-screen notification and the new application ID on real devices.
5. Run **Actions → Build signed Meds Timer release**, enter the matching tag (for example v1.0.0), and confirm the device checks are complete.
6. The workflow runs tests and lint, builds and verifies the signed APK and publishes it with a SHA-256 checksum under [Releases](https://github.com/g10112/meds-timer/releases).

**Never publish an APK signed with a temporary debug key.** The certificate and application ID must remain unchanged for future updates.

Official public application ID: io.github.g10112.medstimer. Minimum Android version: 14. Target SDK: 36. Any APK from the earlier debug version is a separate installation.

## Wider distribution

GitHub Releases is the main download. Obtainium can follow that release page. F-Droid requires an independent packaging/build review; Google Play requires a developer account, Android App Bundle, listing, disclosures and approval. Neither store publishes automatically.

The full source is GPL-3.0-or-later. Keep the official app free, offline, without ads or tracking.
