# Kalori Takip — Google Play Console Yayın Paketi (TR)

## Uygulama kimliği
- Uygulama adı: **Kalori Takip**
- Paket adı: `com.pixelhunter.calorietracker`
- İlk sürüm: `1.0.0`
- Version code: `1`
- Kategori: Sağlık ve Fitness
- Sağlık özelliği: Nutrition and Weight Management

## Kısa açıklama
Kalori, makro, kilo ve günlük hedeflerini kolayca takip et.

## Uzun açıklama
Kalori Takip, günlük beslenmeni ve kilo değişimini sade bir arayüzden takip etmeni sağlar.

Yemeklerini manuel ekleyebilir veya ürün barkodunu tarayarak Open Food Facts veritabanından ürün bilgilerini getirebilirsin. Günlük kalori hedefini belirleyebilir, protein, karbonhidrat ve yağ toplamlarını takip edebilir, son 7 gün ve aylık ilerlemeni görüntüleyebilirsin.

Öne çıkan özellikler:
- Google hesabıyla güvenli giriş
- Günlük kalori hedefi
- Kalori, protein, karbonhidrat ve yağ takibi
- Yemek ekleme, düzenleme ve silme
- Barkod tarama ve Open Food Facts ürün sorgulama
- Son kullanılan yiyecekleri hızlı tekrar ekleme
- Kilo kaydı ve kilo geçmişi
- Son 7 gün ve aylık özetler
- Bulut senkronizasyonu
- Hesap ve kişisel verileri uygulama içinden kalıcı silme

Kalori Takip tıbbi teşhis veya tedavi amacı taşımaz. Uygulamadaki beslenme ve kalori bilgileri genel takip amaçlıdır.

## Veri güvenliği — uygulamanın kullandığı veriler

### Hesap bilgileri
- E-posta adresi: Google/Supabase Auth ile hesap oluşturma ve oturum yönetimi için kullanılır.
- Kullanıcı kimliği: Verileri doğru kullanıcıyla ilişkilendirmek ve RLS ile ayırmak için kullanılır.

### Sağlık / fitness verileri
- Günlük kalori hedefi
- Yiyecek adı ve öğün bilgisi
- Gram miktarı
- Kalori
- Protein
- Karbonhidrat
- Yağ
- Kilo kayıtları

Amaç: Kullanıcının beslenme ve kilo geçmişini kaydetmek, senkronize etmek ve uygulama içinde göstermek.

### Barkod verisi
Taranan EAN/UPC barkod numarası ürün bilgisi sorgulamak amacıyla Open Food Facts servisine gönderilir.

### Paylaşım
Kalori ve kilo kayıtları reklamverenlere satılmaz veya reklam hedefleme amacıyla paylaşılmaz. Open Food Facts'a yalnızca ürün sorgusu için barkod numarası gönderilir. Google giriş ve Supabase hizmetleri kendi hizmet işlevleri kapsamında gerekli hesap/oturum verilerini işler.

### Silme
Kullanıcı uygulama içindeki "Hesabımı ve verilerimi sil" işlemiyle hesabını ve Kalori Takip verilerini kalıcı olarak silebilir. Sunucu tarafındaki silme işlemi oturum JWT'sinden çağıran kullanıcıyı belirler; başka bir kullanıcının verileri silinemez.

## Health Apps beyanı
- Uygulama sağlık/fitness özelliği içeriyor: **Evet**
- Kategori: **Nutrition and Weight Management**
- Tıbbi cihaz/teşhis uygulaması: **Hayır**
- Sağlık profesyoneli yerine geçme iddiası: **Hayır**

## Gizlilik politikası
Repo kaynağı: `apps/calorie-tracker/store/privacy-policy.html`
Uygulama içinde aynı politika `PrivacyPolicyActivity` ile çevrimdışı olarak açılır.
Play Console gönderiminden önce bu HTML herkese açık kalıcı bir HTTPS URL'de yayınlanmalıdır.

## Hesap silme
Uygulama içi yol:
1. Google ile giriş yap.
2. Ana ekranın altındaki "Hesabımı ve verilerimi sil" seçeneğine dokun.
3. Kalıcı silme onayını ver.

Silinen veriler:
- Kalori profili
- Yemek kayıtları
- Kilo kayıtları
- Günlük hedef kayıtları
- Supabase Auth hesabı

## Test hesabı / inceleme notu
Google OAuth kullanıldığı için inceleme cihazında normal Google hesap seçici açılır. Uygulama ücretli üyelik duvarı olmadan temel V1 işlevlerine erişim sağlar.

## Play inceleme ekibine not
Kalori Takip, genel amaçlı bir beslenme ve kilo takip uygulamasıdır. Tıbbi teşhis, tedavi veya klinik karar desteği sunmaz. Kullanıcı beslenme kayıtlarını kendisi girer veya barkod tarama ile Open Food Facts'tan ürün bilgisi alır.

## Gerçek cihaz son kontrolü
- Google OAuth giriş ve geri dönüş
- Oturumun uygulama yeniden açıldığında korunması
- Günlük hedef değiştirme
- Manuel yemek ekleme
- Yemek düzenleme ve silme
- Barkod tarama
- Open Food Facts ürün bilgisi
- Son kullanılan yiyeceği tekrar ekleme
- Kilo ekleme ve silme
- 7 günlük grafik/özet
- Aylık özet
- Çıkış yapma
- Gizlilik Politikası kısayolu ve deep link
- Hesap + tüm verileri kalıcı silme

## Release dosyaları
CI çıktıları:
- Debug APK artifact: `calorie-tracker-debug`
- Release AAB artifact: `calorie-tracker-release-aab`

Production AAB için GitHub Actions secrets:
- `CALORIE_UPLOAD_KEYSTORE_BASE64`
- `CALORIE_UPLOAD_KEYSTORE_PASSWORD`
- `CALORIE_UPLOAD_KEY_ALIAS`
- `CALORIE_UPLOAD_KEY_PASSWORD`

Upload key hiçbir zaman repoya commit edilmemelidir.
