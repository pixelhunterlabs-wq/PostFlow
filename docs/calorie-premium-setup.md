# Kalori Takip Premium — 12 October 2026

## Implemented

- Profile and plan comparison show server membership and monthly photo usage.
- Product `kalori_premium`, base plans `monthly` and `yearly`; Billing 9.1.0 retained.
- Real checkout remains disabled (`PremiumBillingConfig.SALES_ENABLED = false`).
- Every protected operation authenticates the Supabase user and verifies stored purchase tokens with Google Play subscriptionsv2.
- Account hash binding, atomic unique token ownership, linked-token replacement, acknowledgement, restore, cancellation until expiry, grace, hold and expiry handling.
- Server-generated reports and goal suggestions, protected weight graph, voice access checked before recognition and save; database trigger rejects unauthorized `source=voice` inserts.
- Atomic UTC calendar-month quota: Free 5 / Premium 100. Both tiers share one monthly counter. Failed requests refund their reservation at most once.
- Clients cannot modify entitlements, quota, or purchases, or read purchase tokens. RLS is enabled.
- Existing Google OAuth, meal history, profiles, water, barcode and weight records are retained.

## Deployed Supabase functions

`verify-google-play-subscription`, `premium-access`, `premium-reports`, `analyze-food-photo`.
Project: `lyjkoypfoopspbeofpvt`.
Migration: `20261012203119_premium_server_enforcement.sql`.

## Required external configuration — not verified from this session

1. Play Console must have the Android application `com.pixelhunter.calorietracker`, subscription `kalori_premium`, both active base plans and configured localized prices.
2. An Android Publisher API service account must have access to this application's purchases/subscriptions. Set its JSON only as the Edge Function secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.
3. Set `OPENAI_API_KEY` as an Edge Function secret for photo analysis. Supabase runtime supplies its URL and service role key; none belong in the APK.
4. Upload a signed build to an internal test track, register license testers and install through Google Play for real Billing test flows. A sideloaded debug APK is suitable for UI/free feature testing, but is not proof of subscription checkout.
5. Test monthly/yearly purchase, pending payment, acknowledgement, restore/reinstall, wrong app account, cancellation, renewal, expiry, grace, hold and refund/revocation. Test actual AI responses and device Google sign-in.
6. Only after these tests pass, enable checkout and build a new release. This change does not enable payments or invent membership for debug accounts.

Subscription changes are refreshed when an authorized operation is requested. This version does not require RTDN delivery; background subscription notification processing is not configured. Database write defense accepts only recently verified (five-minute maximum) access; the normal Android voice path re-verifies before saving. Recognition is provided by the device; its local operation cannot be made tamper-proof on a modified APK. Server writes and report/photo endpoints enforce access.

## Validation

- Six Deno policy tests passed: plan/product validation, active, canceled, expired, pending, paused/hold/revoked, grace and 5/100 quotas.
- Live database tests ran in rolled-back transactions: Free sixth request denied, Premium 101st denied, idempotent refund; free/expired voice denied, active voice and free manual insert allowed.
- Authenticated role cannot execute quota reservation, update membership, or read token records.
- Unauthenticated Edge Function request returned 401.
- Android CI runs Edge Function checks, server policy tests, Gradle tests and assembleDebug, then uploads `app-debug.apk`.
- Local Gradle could not download its distribution because the runtime network route was unavailable; GitHub Actions is the build source.

No live paid purchase or mobile Google login test has been performed in this session. Server-only tables intentionally have no client RLS policies. Existing Supabase advisories for `pg_trgm` in public and password leak protection were not changed, to avoid unrelated schema/auth modifications.
