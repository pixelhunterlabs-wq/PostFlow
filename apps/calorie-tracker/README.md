# Calorie Tracker

Standalone Android/Kotlin/Jetpack Compose app under PostFlow. It is intentionally isolated from PostFlow `src`, `media-engine` and `installer`.

## Shared Supabase backend

The existing **PostFlow_DB** Supabase project is reused. No new paid Supabase project is required.

Existing tables used by the app:

- `calorie_profiles`
- `calorie_food_entries`
- `calorie_weight_entries`
- `calorie_daily_targets`

All four tables use owner-only RLS policies based on `auth.uid() = user_id`.

## V1 features

- Google OAuth via Supabase Auth
- automatic refresh after the `calorietracker://login` OAuth callback
- daily calorie target and remaining calories
- food + grams + calories + protein/carbohydrate/fat logging
- edit and delete food records
- confirmation before destructive deletes
- daily macro summary
- 7-day and 30-day calorie averages
- visual last-7-days calorie progress
- current-month total and daily average
- recent 30-day food history
- quick re-add from recent foods
- weight logging, history and delete flow
- Google Code Scanner barcode flow with manual barcode fallback
- Open Food Facts v2 product lookup and per-gram macro scaling
- Snackbar success/error feedback
- Supabase cloud sync
- in-app account + calorie data deletion flow
- authenticated `delete-calorie-account` Supabase Edge Function
- bundled offline privacy-policy screen (`PrivacyPolicyActivity`)
- Turkish public privacy-policy HTML ready for HTTPS hosting
- Turkish Play Store listing draft
- Google Play Data Safety / Health Apps declaration draft
- custom launcher icon
- Play Store asset specification for icon, feature graphic and screenshots
- unit tests for calorie-state calculations
- GitHub Actions unit-test + debug APK verification
- GitHub Actions release-candidate AAB verification
- optional Play upload-key signing pipeline

## Verified Android builds

Debug APK:

```bash
gradle -p apps/calorie-tracker testDebugUnitTest assembleDebug --stacktrace
```

The Android workflow runs unit tests before building and uploads `calorie-tracker-debug` only after a successful build.

Release candidate AAB:

```bash
gradle -p apps/calorie-tracker bundleRelease --stacktrace
```

The AAB workflow uploads `calorie-tracker-release-aab`.

## Play upload signing

The AAB workflow is signing-ready. When the four repository secrets below exist, the workflow reconstructs the upload keystore in the GitHub runner, exposes only temporary environment variables to Gradle, signs the release bundle, and verifies the resulting AAB with `jarsigner`.

Required GitHub Actions secrets:

- `CALORIE_UPLOAD_KEYSTORE_BASE64`
- `CALORIE_UPLOAD_KEYSTORE_PASSWORD`
- `CALORIE_UPLOAD_KEY_ALIAS`
- `CALORIE_UPLOAD_KEY_PASSWORD`

If these secrets are absent, CI still builds an unsigned release-candidate AAB so pull requests remain testable. The keystore file and passwords must never be committed to Git.

## Account deletion

The Android client invokes the authenticated Supabase Edge Function `delete-calorie-account`. The function derives the caller from the JWT, deletes only that user's calorie/weight/target/profile rows, then deletes the corresponding Auth user. The service-role key remains server-side and is never shipped in the Android app.

Version-controlled function source:

`supabase/functions/delete-calorie-account/`

## Privacy policy

Public-hosting source:

`store/privacy-policy.html`

Bundled in-app copy:

`app/src/main/assets/privacy-policy.html`

Android screen:

`PrivacyPolicyActivity`

The activity is addressable with the internal deep link `calorietracker://privacy`. The main-screen visible entry point still needs final UI wiring before production submission.

## Local setup

Create `local.properties` in this folder when you need to override the defaults:

```properties
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_KEY=sb_publishable_YOUR_KEY
```

Use the publishable key only. Never place a secret/service-role key in the Android client.

Supabase Auth setup:

1. Enable Google provider in the shared project.
2. Configure Google OAuth Client ID / Client Secret.
3. Add `calorietracker://login` to allowed redirect URLs.
4. Open this folder in Android Studio, sync Gradle and run `app`.

## Toolchain

- JDK 17
- Android Gradle Plugin 9.4.0
- Kotlin 2.4.0
- compileSdk 37 / targetSdk 36 / minSdk 26
- Compose BOM 2026.08.00
- supabase-kt BOM 3.8.0
- Ktor 3.2.3
- Google Play Services Code Scanner 16.1.0

## Remaining before Play Store production release

- runtime device test of Google OAuth, barcode scan/Open Food Facts and account deletion using a disposable test account
- create the real Play upload keystore and add the four GitHub Actions secrets
- wire a visible privacy-policy button into the main app UI
- generate final 512x512 Play icon, 1024x500 feature graphic and phone screenshots
- publish privacy policy at a public HTTPS URL
- complete Play Console Data Safety / Health Apps declarations, target audience and content rating
- optional camera/AI food estimation
- optional Play Billing premium tier
- instrumentation/UI tests
