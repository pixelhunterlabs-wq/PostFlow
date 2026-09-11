# Kalori Takip 1.0.0 – Release Candidate Manifest

Tarih: 7 Eylül 2026

## Kaynak

- Branch: `feature/calorie-tracker-v1`
- Kamera ile çekme doğrulanan commit: `38e5bc650b7309e175839d982dca552ee6880049`
- PR: #8 (draft, unmerged)
- Package: `com.pixelhunter.calorietracker`
- versionCode: `1`
- versionName: `1.0.0`
- targetSdk: `36`

## CI kabulü

Commit `38e5bc650b7309e175839d982dca552ee6880049` için:

- PostFlow CI: PASS
- Android unit tests: PASS
- Debug APK build/upload: PASS
- Release AAB build/upload: PASS

## Son doğrulanmış production AAB

Dosya adı: `KaloriTakip-1.0.0-camera-production.aab`

SHA-256:

`9c5f38dfae635c676665d362011509b10dfc69883ce261252718db236695293e`

AAB mevcut Google Play upload key ile imzalanmış ve `jarsigner` ile doğrulanmıştır.

## Public Google Play URL'leri

Privacy Policy:

`https://jecbdzkunqwgpzbiwdak.supabase.co/functions/v1/calorie-privacy`

Account deletion information:

`https://jecbdzkunqwgpzbiwdak.supabase.co/functions/v1/calorie-delete-account`

Her iki sayfa da public statik HTML Edge Function olarak deploy edilmiştir ve kullanıcı verisine erişmez.

## Backend Edge Functions

- `delete-calorie-account` — authenticated, JWT required
- `analyze-food-photo` — authenticated, JWT required
- `calorie-privacy` — public static HTML
- `calorie-delete-account` — public static HTML

AI fotoğraf analizi kodu hazırdır; gerçek AI sonucu için sunucu tarafında sağlayıcı API credential secret olarak yapılandırılmalıdır. Credential APK/AAB içine konmamalıdır.

## V1 özellikleri

- Google ile giriş
- Supabase cloud sync
- Günlük kalori ve makro takibi
- Kahvaltı / öğle / akşam / atıştırmalık günlüğü
- Türkiye odaklı yiyecek kataloğu
- Favoriler ve son kullanılanlar
- Barkod + Open Food Facts
- Manuel yiyecek ekleme
- Su takibi
- Kilo geçmişi
- 7 günlük ilerleme grafiği
- Haftalık ve aylık rapor
- Onboarding + başlangıç kalori hedefi tahmini
- Health Connect READ_STEPS
- Öğün bildirimleri
- Dinamik kalori hedefi önerisi
- Türkçe sesle yemek ekleme
- Kayıtlı öğünler
- Çok malzemeli tarif / öğün oluşturucu
- Fotoğraftan AI analiz
- Kamera ile yemek fotoğrafı çekme
- Galeriden yemek fotoğrafı seçme
- Uygulama içinden hesap ve veri silme
- Koyu / neon yeşil Material 3 tasarım

## Gerçek cihaz release gate

Production'a çıkmadan aşağıdaki fiziksel testler tamamlanmalıdır:

1. Temiz kurulum ve onboarding
2. Google OAuth + deep link dönüşü
3. Uygulama kapat/aç sonrası session restore
4. Manuel yiyecek ekleme ve cloud sync
5. Barkod tarama + Open Food Facts
6. Favoriler ve tekrar ekleme
7. Su takibi
8. Kilo ekleme + trend/rapor
9. Health Connect izin ver / geri al
10. Android 13+ bildirim izni ve öğün reminder
11. Türkçe sesle yiyecek ekleme
12. Tarif oluştur / kayıtlı öğün ekle
13. Kamera ile fotoğraf çek
14. Galeriden fotoğraf seç
15. AI provider credential etkinse gerçek fotoğraf analizi
16. Gizlilik politikası ekranı
17. Disposable hesapla kalıcı hesap/veri silme
18. Son Play Store screenshot çekimi

## Google Play beyan özeti

- Category: Health & Fitness
- Health Apps: Nutrition and Weight Management
- Physical activity: Steps / Health Connect READ_STEPS
- Medical device: No
- Diagnosis / treatment claims: No
- Personal info: email + user ID for account management
- Health data: nutrition / weight for app functionality
- Microphone: user-triggered voice logging
- Notifications: optional meal reminders
- Photos/videos: only if AI photo analysis is shipped enabled; user-selected/captured meal image is transferred for analysis and not stored in app database/storage

## Release durumu

Kod/CI tarafı release candidate seviyesindedir. PR gerçek cihaz kabul testi bitene kadar draft ve unmerged kalmalıdır.
