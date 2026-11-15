# F-Droid submission

**Status: submission materials prepared. Not submitted or listed on F-Droid.**

Source: https://github.com/g10112/meds-timer

App ID: `io.github.g10112.medstimer` · Version: `1.0.0` · Licence: `GPL-3.0-or-later`

The app has public source, a stable release tag and upstream Fastlane metadata.

## Submit

1. Sign in to [GitLab](https://gitlab.com) and fork [F-Droid's fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. Add `metadata/io.github.g10112.medstimer.yml` using the [draft metadata](../distribution/fdroid/io.github.g10112.medstimer.yml).
3. Run `fdroid lint io.github.g10112.medstimer` and an F-Droid test build (or use fdroiddata CI). Fix any findings.
4. Submit a merge request to fdroiddata for inclusion and respond to the F-Droid reviewers.

The F-Droid build recipe removes the release-signing assignment **in the F-Droid build sandbox only** so F-Droid can produce and sign its own APK. The normal official GitHub release stays signed by the project key.

**Important:** Reproducible developer-signed F-Droid builds have not been verified. F-Droid may publish a build with a different signing key, so people already using the GitHub-signed APK should continue updating through GitHub or Obtainium. Do not claim cross-store updates are compatible without verifying signatures.

F-Droid approval and publication are managed by F-Droid, not by this GitHub repository. See [official instructions](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/).
