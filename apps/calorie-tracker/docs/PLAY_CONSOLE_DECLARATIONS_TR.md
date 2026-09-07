# Kalori Takip – Google Play Console beyan taslağı

Son güncelleme: 7 Eylül 2026

Bu belge Play Console formlarını doldururken referans olması içindir. Formlar, yayımlanacak son AAB ve SDK listesiyle tekrar karşılaştırılmalıdır.

## Health Apps Declaration

Seçilmesi gereken ana sağlık özelliği:

- Health and fitness → Nutrition and Weight Management

Ek sağlık özelliği / Health Connect kullanımı:

- Physical activity → Steps
- Uygulama yalnızca `android.permission.health.READ_STEPS` ister.
- Adım verisi kullanıcı açıkça izin verirse Health Connect'ten okunur ve günlük aktivite bilgisini göstermek için kullanılır.
- Uygulama Health Connect'e adım verisi yazmaz.
- Adım verisi reklam hedefleme, veri brokerı veya üçüncü taraf pazarlama amacıyla kullanılmaz.

Gerekçe: Uygulama kalori/makro besin alımını, günlük beslenme hedeflerini, kilo kayıtlarını ve isteğe bağlı adım bilgisini takip eder.

Uygulama tıbbi cihaz değildir; teşhis, tedavi, hastalık yönetimi veya klinik karar desteği sağlamaz.

## Data Safety – güncel V1 çalışma taslağı

### Personal info

**Email address**
- Toplanıyor: Evet
- Kullanım: Account management / App functionality
- Google OAuth ve Supabase Auth hesap işlevleri için kullanılır.

**User IDs**
- Toplanıyor: Evet
- Kullanım: Account management / App functionality
- Supabase Auth kullanıcı kimliği kayıtların sahibini ayırmak ve RLS uygulamak için kullanılır.

### Health and fitness

**Nutrition / weight data**
- Toplanıyor: Evet
- Örnekler: günlük kalori kayıtları, protein/karbonhidrat/yağ, beslenme hedefi, kilo geçmişi
- Kullanım: App functionality; kişisel ilerleme ve özet oluşturma
- Saklama: Supabase veritabanı
- Güvenlik: kullanıcı bazlı kimlik doğrulama + RLS
- Kullanıcı silme: uygulama içi hesap silme akışı mevcut

**Steps / physical activity**
- Health Connect üzerinden kullanıcı izniyle okunur.
- Mevcut uygulama kodunda adım verisi Supabase'e veya başka bir geliştirici sunucusuna yüklenmez; cihaz üzerinde gösterilir.
- Play Data Safety formunda “collected” tanımı son gönderim sırasında Google'ın güncel tanımıyla tekrar doğrulanmalıdır; cihaz dışına aktarılmayan Health Connect adımı bu taslakta geliştirici sunucusuna toplanan veri olarak işaretlenmemelidir.
- Health Connect izin beyanında READ_STEPS erişimi açıkça bildirilmelidir.

### Cihaz üzerinde saklanan tercihler

Aşağıdaki veriler uygulamanın yerel depolamasında tutulabilir:
- su takibi
- favori yiyecekler
- kayıtlı öğünler
- haftalık kilo hedefi tercihi
- öğün bildirim tercihi
- onboarding tamamlanma durumu

Android sistem yedeği kapalı olduğundan bu yerel tercihler sistem yedeğine dahil edilmez.

### Mikrofon / sesli yemek ekleme

- İzin: `android.permission.RECORD_AUDIO`
- Kullanım: Kullanıcının isteğiyle Android konuşma tanıma arayüzünü başlatmak ve yemek cümlesini metne dönüştürmek.
- Uygulama kendi sunucusuna ham ses dosyası yüklemez veya saklamaz.
- Kullanılan Android konuşma tanıma sağlayıcısı cihaz/hesap ayarlarına göre sesi kendi hizmetinde işleyebilir; bu sağlayıcının veri uygulamaları Google/cihaz sağlayıcısının politikalarına tabidir.
- Uygulama tanınan metni Türk yiyecek kataloğuyla eşleştirip kullanıcı onayıyla yemek kaydı oluşturur.

### Bildirimler

- İzin: `POST_NOTIFICATIONS` (Android 13+)
- Kullanım: isteğe bağlı kahvaltı, öğle ve akşam yemek kaydı hatırlatmaları
- Varsayılan saatler: 08:00, 13:00, 19:00
- Kullanıcı özelliği kapatabilir.

### Third-party services / SDKs

**Google Sign-In / Google OAuth**
- Amaç: Authentication / Account management

**Supabase Auth / Database / Edge Functions**
- Amaç: Authentication, cloud storage/sync, account deletion

**Android Health Connect**
- Amaç: kullanıcının izin verdiği adım verisini okumak
- İzin: READ_STEPS

**Android Speech Recognition provider**
- Amaç: kullanıcının başlattığı sesli yemek girişini metne çevirmek

**Google Code Scanner / Google Play Services**
- Amaç: Product barcode scanning
- Uygulama doğrudan CAMERA izni istemez; tarama Google Play Services tarafından sağlanır.

**Open Food Facts**
- Amaç: Barkoddan ürün ve besin bilgisi sorgulama
- Gönderilen veri: taranan/yazılan ürün barkodu
- Hesap e-postası veya Supabase kullanıcı kimliği Open Food Facts sorgusuna özellikle eklenmez.

## Dinamik kalori hedefi

- Kullanıcının kendi kilo kayıtlarından yaklaşık haftalık kilo değişimi hesaplanır.
- Kullanıcının seçtiği haftalık hedef ile karşılaştırılır.
- Günlük hedef için en fazla ±250 kcal düzeltme önerilir.
- Öneri otomatik uygulanmaz; kullanıcı “Öneriyi uygula” seçeneğine basarsa uygulanır.
- Bu özellik tıbbi tavsiye olarak sunulmamalıdır.

## Privacy Policy

Üretim öncesinde:
- `/privacy/calorie-tracker` sayfası kalıcı, herkese açık HTTPS alanında yayınlanmalı.
- Uygulama içindeki paketlenmiş gizlilik sayfası da erişilebilir kalmalı.
- Politika; Google OAuth, Supabase, beslenme/kilo verileri, Health Connect adımı, bildirimler, sesli giriş, barkod/Open Food Facts ve hesap silme akışını açıklamalı.

## Account deletion

Uygulamada `Hesabımı ve verilerimi sil` akışı vardır.

Backend:
- authenticated Supabase Edge Function: `delete-calorie-account`
- çağıran kullanıcı JWT üzerinden doğrulanır
- yalnızca çağıranın `calorie_food_entries`, `calorie_weight_entries`, `calorie_daily_targets`, `calorie_profiles` kayıtları silinir
- ardından ilgili Supabase Auth kullanıcısı silinir
- service-role anahtarı Android istemcisine gönderilmez

## Yayın öncesi son kontrol

- Health Apps Declaration: Nutrition and Weight Management + Steps
- Health Connect READ_STEPS erişimi beyanı
- Data Safety formunu son AAB ile eşleştir
- microphone/voice logging açıklamasını permission formunda doğrula
- aktif HTTPS gizlilik politikası URL'si ekle
- hesap silme akışını geçici test hesabıyla gerçek cihazda doğrula
- Health Connect izin/revoke senaryosunu gerçek cihazda test et
- Android 13+ notification permission akışını test et
- sesli girişin Türkçe konuşma sağlayıcısıyla cihaz testini yap
- tıbbi cihaz olmadığını belirten uyarıyı mağaza açıklaması ve uygulamada koru
