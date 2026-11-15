# Google Play listing — Meds Timer

**Status: listing copy prepared. Not submitted to Google Play.**

The Android App Bundle (`.aab`) was built in the [v1.0.0 release workflow](https://github.com/g10112/meds-timer/actions/runs/43770760454) and kept as a workflow artifact. Download the signed bundle before the workflow artifact expires.

## Listing

- App title: **Meds Timer**
- Category: **Medical**
- Price: **Free**
- Advertising: **No**
- Short description: **Simple, offline medication reminder alarms**
- Full description: [Store listing text](../fastlane/metadata/android/en-US/full_description.txt)
- Support: https://github.com/g10112/meds-timer/issues
- Privacy policy: https://github.com/g10112/meds-timer/blob/main/docs/PRIVACY.md
- Website and source: https://github.com/g10112/meds-timer

## Store graphics

- **App icon:** 512 × 512 PNG. Match the actual shipped app icon.
- **Feature graphic:** 1024 × 500 RGB PNG.
- **Screenshots:** at least two real captures from the actual running app on your phone. Suggested: home screen with the three configured intervals; active session with remaining alarms; widget; alarm screen. Crop carefully; do not show private notifications or medical information. Do not present generated mockups as actual app screenshots.

## Compliance review

- The Health apps declaration should identify **Medication and Treatment Management**. Do not choose “no health features.”
- Include the precise disclaimer: **This app is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Consult a healthcare professional for medical advice.**
- Complete the Data safety disclosure accurately after inspecting the final bundle. The app's source has no internet permission, analytics, ads or account services; check your declaration against the released APK.
- Privacy policy must be publicly accessible and **available within the app**. The v1.0.0 app has a privacy document in GitHub, but an in-app privacy page/link still needs to be included in a subsequent release before submitting to Play.
- Check exact-alarm and full-screen intent declarations, target SDK, content ratings and device compatibility in the Play Console.
- If using a new personal Play developer account created after 13 November 2023, Google requires a closed test with **12 opted-in testers for at least 14 continuous days**, followed by a production-access application.
- **Signing:** Google Play App Signing may use a different app-signing certificate from GitHub. Do not assume updates can be installed across stores.

Google Play uploads and approvals require access to your own [Play Console](https://play.google.com/console). Do not claim the app is published there before the store listing is live.
