# Kalori Takip – Public Google Play URLs

Son güncelleme: 7 Eylül 2026

Google Play Console için kullanılacak kalıcı public HTTPS sayfalar:

## Gizlilik politikası

https://jecbdzkunqwgpzbiwdak.supabase.co/functions/v1/calorie-privacy

- Public Supabase Edge Function
- JWT gerektirmez
- Yalnızca statik HTML sunar
- Kullanıcı veya uygulama verisi okumaz/yazmaz
- Google OAuth, Supabase, Health Connect, sesli giriş, fotoğraf AI analizi, barkod/Open Food Facts, bildirimler ve hesap silme akışını açıklar

## Hesap ve veri silme bilgileri

https://jecbdzkunqwgpzbiwdak.supabase.co/functions/v1/calorie-delete-account

- Public Supabase Edge Function
- JWT gerektirmez
- Yalnızca statik HTML sunar
- Uygulama içindeki Profil → Hesabımı ve verilerimi sil akışını açıklar
- Gerçek hesap silme işlemi bu public sayfadan yapılmaz; uygulamadaki authenticated `delete-calorie-account` Edge Function üzerinden yapılır

## Google Play alanları

- Privacy Policy URL: `https://jecbdzkunqwgpzbiwdak.supabase.co/functions/v1/calorie-privacy`
- Account deletion URL: `https://jecbdzkunqwgpzbiwdak.supabase.co/functions/v1/calorie-delete-account`

Not: Eski PostFlow Vercel production deployment'ında hazırlanan `/privacy/calorie-tracker` ve `/delete-account/calorie-tracker` yolları henüz deploy edilmediği için Play Console'a verilmemelidir.
