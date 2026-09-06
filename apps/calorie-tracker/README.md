# Calorie Tracker

Android/Kotlin/Jetpack Compose calorie and weight tracker living independently under PostFlow.

## Isolation

This app does not modify PostFlow `src`, `media-engine`, `installer`, or existing environment files. It uses the shared Supabase project with namespaced tables:

- `calorie_profiles`
- `calorie_entries`
- `calorie_weights`

RLS limits every row to its owner (`auth.uid() = user_id`).

## Current V1

- Google OAuth through Supabase Auth
- Daily calorie goal
- Food entry with grams, calories, protein, carbohydrate and fat
- Daily calorie + macro summary
- Weight logging and recent weight history
- Persistent Supabase sync
- Android deep link callback: `calorietracker://login`

## Local setup

1. Install Android Studio Quail (or compatible) and JDK 17.
2. Create `local.properties` in this folder (do not commit it):

```properties
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_KEY=sb_publishable_YOUR_KEY
```

3. In the shared Supabase project SQL editor, run `supabase/schema.sql` once.
4. In Supabase Auth > Providers > Google, enable Google and configure its Client ID/Secret.
5. Add `calorietracker://login` to allowed redirect URLs.
6. Open this folder as an Android Studio project, sync Gradle, and run the `app` configuration.

## Security

- Never place a Supabase secret/service-role key in the Android app.
- The Android app uses only the publishable key.
- RLS is enabled on every exposed table.
- Future apps should use their own table prefix while sharing the same Supabase project.

## Next product milestones

- weekly/monthly dashboard and charts
- reusable foods/favorites
- Open Food Facts search + barcode scanner
- camera/AI food estimation
- reminders
- Google Play billing / premium tier
- tests, CI and signed Play Store release pipeline
