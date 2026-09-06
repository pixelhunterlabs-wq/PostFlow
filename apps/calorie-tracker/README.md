# Calorie Tracker

Standalone Android/Kotlin/Jetpack Compose app under PostFlow. It is intentionally isolated from PostFlow `src`, `media-engine` and `installer`.

## Shared Supabase backend

The existing **PostFlow_DB** Supabase project is reused. No new paid Supabase project is required.

Existing tables used by the app:

- `calorie_profiles`
- `calorie_food_entries`
- `calorie_weight_entries`
- `calorie_daily_targets` (reserved for date-specific targets)

All four tables already have owner-only RLS policies based on `auth.uid() = user_id`.

## V1 features

- Google OAuth via Supabase Auth
- daily calorie target
- food + grams + calories + protein/carbohydrate/fat logging
- daily macro summary
- 7-day and 30-day calorie averages
- recent 30-day food history
- weight logging and weight history
- Supabase cloud sync
- deep-link callback: `calorietracker://login`

## Local setup

Create `local.properties` in this folder:

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
- compileSdk 37 / targetSdk 36 / minSdk 26
- Compose BOM 2026.08.00
- supabase-kt BOM 3.8.0

## Planned V2

- favorite/reusable foods
- Open Food Facts search + barcode scanner
- camera/AI food estimation
- reminders
- charts and deeper weekly/monthly analytics
- Play Billing premium tier
- tests + CI + signed Play Store release pipeline
