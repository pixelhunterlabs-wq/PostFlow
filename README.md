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
- Turkish privacy-policy HTML
- local privacy-policy Activity + `calorietracker://privacy` deep link
- custom launcher icon
- Turkish Play Store listing draft
- Google Play Data Safety / Health Apps declaration draft
- Google Play release checklist
- unit-test gate before debug APK build
- GitHub Actions debug APK build verification
- GitHub Actions release-candidate AAB build verification
- optional Play upload-key signing pipeline

## Verified Android builds

Debug APK:

```bash
gradle -p apps/calorie-tracker assembleDebug --stacktrace
```

The verified workflow uploads `calorie-tracker-debug`.

Release candidate AAB:

```bash
gradle -p apps/calorie-tracker bundleRelease --stacktrace
```

The verified workflow uploads `calorie-tracker-release-aab`.

## Automated tests

The Android CI runs unit tests before building the debug APK. Current tests cover core daily/7-day/30-day/monthly calorie-summary behavior so future UI/refactor work cannot silently break basic calculations.

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

## Privacy

Store HTML source:

`store/privacy-policy.html`

Local Android privacy screen:

`app/src/main/java/com/pixelhunter/calorietracker/PrivacyPolicyActivity.kt`

Deep link:

`calorietracker://privacy`

A public HTTPS copy must still be published before Google Play production submission.

## Release tracking

Use:

`store/PLAY_RELEASE_CHECKLIST.md`

It contains runtime device tests, Play assets, Data Safety, Health Apps, signing, and final submission checks.

## Local setup

Create `local.properties` in this folder when you need to override the defaults:

```properties
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_KEY=sb_publishable_YOUR_KEY
```

Use the publishable key only. Never place a secret/service-role key in the Android client.

Supabase Auth setup:

1. In Supabase Dashboard → Authentication → Providers → Google, enable Google and enter the Google **Web application** OAuth client ID and client secret.
2. In Google Cloud Console → Google Auth Platform → Clients → that Web application client, add this exact Authorized redirect URI:
   `https://jecbdzkunqwgpzbiwdak.supabase.co/auth/v1/callback`
3. In Supabase Dashboard → Authentication → URL Configuration → Redirect URLs, add this exact mobile callback:
   `calorietracker://login`
4. Keep `calorietracker://login` in `ModernMainActivity`'s Android intent filter and in `SupabaseProvider.oauthRedirectUrl`; the values must match exactly.
5. Open this folder in Android Studio, sync Gradle and run `app`.

The Google Cloud Console callback is the Supabase HTTPS callback in step 2. Do not add the
custom `calorietracker://` URI to Google Cloud; Supabase validates that mobile URI in step 3
and redirects to it after it completes the Google callback.

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

- runtime device test of Google OAuth, barcode/Open Food Facts and account deletion using a disposable test account
- create the real Play upload keystore and add the four GitHub Actions secrets
- visibly expose the local privacy-policy screen from the main signed-in UI
- final 512x512 Play icon, phone screenshots and 1024x500 feature graphic
- publish privacy policy at a public HTTPS URL
- complete Play Console Data Safety / Health Apps declarations and content rating
- optional camera/AI food estimation
- optional Play Billing premium tier
