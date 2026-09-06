# Calorie Tracker

Standalone Android/Kotlin/Jetpack Compose app under PostFlow. It is intentionally isolated from PostFlow `src`, `media-engine` and `installer`.

## Shared Supabase backend

The existing **PostFlow_DB** Supabase project is reused. No new paid Supabase project is required.

Existing tables used by the app:

- `calorie_profiles`
- `calorie_food_entries`
- `calorie_weight_entries`
- `calorie_daily_targets` (reserved for date-specific targets)

All four tables use owner-only RLS policies based on `auth.uid() = user_id`.

## V1 features

- Google OAuth via Supabase Auth
- automatic refresh after the `calorietracker://login` OAuth callback
- daily calorie target
- food + grams + calories + protein/carbohydrate/fat logging
- edit and delete food records
- confirmation before destructive deletes
- daily macro summary and remaining calories
- 7-day and 30-day calorie averages
- visual last-7-days calorie progress
- current-month total and daily average
- recent 30-day food history
- weight logging, history and delete flow
- Snackbar success/error feedback
- Supabase cloud sync
- GitHub Actions debug APK build verification

## Verified Android build

The `Calorie Tracker Android Build` workflow installs the Android 37 preview SDK, uses JDK 17 + Gradle 9.6.0 and runs:

```bash
gradle -p apps/calorie-tracker assembleDebug --stacktrace
```

A successful run uploads the generated APK as the `calorie-tracker-debug` artifact.

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

## Next before Play Store release

- favorite/reusable foods
- Open Food Facts food search + barcode scanner
- optional camera/AI food estimation
- app icon and production visual polish
- privacy policy and Play Console data-safety answers
- signed release AAB / keystore pipeline
- Play Billing only if a premium tier is introduced
- instrumentation/UI tests
