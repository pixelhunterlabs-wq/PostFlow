# Kalori Takip — Google Play Release Checklist

## Build
- [x] `targetSdk = 36`
- [x] Debug APK CI
- [x] Release AAB CI
- [x] Optional upload-key signing pipeline
- [x] Unit tests before APK build
- [ ] Create real Play upload keystore
- [ ] Add GitHub Actions signing secrets
- [ ] Verify signed AAB with production upload key

## Runtime device test
Use a disposable test account.
- [ ] Install latest debug APK on a real Android device
- [ ] Google sign-in completes and returns to the app
- [ ] Daily calorie target can be changed
- [ ] Manual food add/edit/delete works
- [ ] Weight add/delete works
- [ ] Recent food quick-add works
- [ ] Barcode scanner opens without requesting camera permission
- [ ] Known barcode resolves through Open Food Facts
- [ ] Unknown barcode falls back cleanly
- [ ] 7-day / 30-day / monthly summaries update correctly
- [ ] Sign out and sign back in restores cloud data
- [ ] Account deletion removes calorie/weight/target/profile data and Auth account

## Store assets
- [x] Launcher icon wired in Android manifest
- [ ] Final 512x512 Play Store icon PNG
- [ ] 1024x500 feature graphic
- [ ] At least 4 polished phone screenshots
- [ ] Final short description
- [ ] Final full description

## Privacy / policy
- [x] Turkish privacy-policy HTML source
- [x] In-app privacy-policy Activity
- [x] Account and data deletion inside app
- [x] Data Safety / Health Apps draft
- [ ] Publish privacy policy at a public HTTPS URL
- [ ] Add public privacy URL to Play Console
- [ ] Ensure privacy policy is visibly reachable from signed-in app UI
- [ ] Complete Data Safety form in Play Console
- [ ] Health Apps declaration: Nutrition and Weight Management
- [ ] Content rating questionnaire
- [ ] Target audience / age declaration

## Data Safety draft — expected answers for current V1
Review again immediately before submission if features change.

### Data collected
- Account info: email address / user identifier — collected for sign-in and account management.
- Health & fitness: nutrition/calorie/macro logs — collected to provide app functionality and cloud sync.
- Health & fitness: weight records — collected to provide app functionality and history.
- App activity/content entered by user: food names, grams, meal type and goals — collected to provide app functionality.

### Data sharing
- Do not mark user-entered calorie/weight records as sold for advertising.
- Google authentication and Supabase are service providers used to operate the app.
- Barcode value may be sent to Open Food Facts to retrieve product nutrition information when the user uses barcode lookup.
- Re-check Google Play's definition of “shared” for service-provider processing at submission time and answer according to the then-current Play Console wording.

### Security / deletion
- Data is transmitted over HTTPS.
- User data is access-controlled by Supabase RLS.
- The Android app contains only a publishable Supabase key; no `service_role` secret is shipped.
- Users can request deletion directly in-app with “Hesabımı ve verilerimi sil”.

## Health Apps declaration
Current intended category: **Nutrition and Weight Management**.

The app is for general calorie, macro and weight tracking. It does not provide diagnosis, treatment, emergency medical services or claim to replace professional medical advice.

## Before pressing “Send for review”
- [ ] Production AAB uploaded successfully
- [ ] Pre-launch report reviewed
- [ ] Crash/ANR warnings reviewed
- [ ] Store listing has no medical-treatment claims
- [ ] Privacy URL opens publicly without login
- [ ] Account deletion path tested end-to-end
- [ ] All Play Console declarations match the actual release build
