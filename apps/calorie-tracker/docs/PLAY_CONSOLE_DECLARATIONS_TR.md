# Kalori Takip – Google Play Console beyan taslağı

Son güncelleme: 6 Eylül 2026

Bu belge Play Console formlarını doldururken referans olması içindir. Formlar, yayımlanacak son uygulama sürümündeki SDK ve veri akışlarıyla tekrar karşılaştırılmalıdır.

## Health Apps Declaration

Seçilmesi gereken ana sağlık özelliği:

- Health and fitness → Nutrition and Weight Management

Gerekçe: Uygulama kalori/makro besin alımını, günlük beslenme hedeflerini ve kilo kayıtlarını takip eder.

Uygulama tıbbi cihaz değildir; teşhis, tedavi, hastalık yönetimi veya klinik karar desteği sağlamaz.

Mağaza açıklaması ve uygulama içinde uygun yerde açık bir uyarı bulunmalıdır:

> Kalori Takip tıbbi bir cihaz değildir ve herhangi bir hastalığı teşhis etmez, tedavi etmez, iyileştirmez veya önlemez. Besin verileri bilgi amaçlıdır ve üçüncü taraf veritabanlarında hata veya eksiklik bulunabilir.

## Data Safety – mevcut V1 için çalışma taslağı

### Personal info

**Email address**
- Toplanıyor: Evet
- Kullanım: Account management / App functionality
- Paylaşım: Uygulamanın kendi işlevi kapsamında üçüncü taraf reklam ağına satılmaz/paylaşılmaz; Google OAuth ve Supabase Auth veri işleyen hizmetlerdir.
- Kullanıcı silme talebinde uygulama hesabıyla ilişkili veri silme akışı vardır.

**User IDs**
- Toplanıyor: Evet
- Kullanım: Account management / App functionality
- Supabase Auth kullanıcı kimliği, kayıtların sahibini ayırmak ve RLS uygulamak için kullanılır.

### Health and fitness

Kullanıcının girdiği beslenme, kalori, makro ve kilo kayıtları kişisel/sensitive health and fitness data olarak ele alınmalıdır.

**Health / fitness related data**
- Toplanıyor: Evet
- Örnekler: günlük kalori kayıtları, protein/karbonhidrat/yağ, beslenme hedefi, kilo geçmişi
- Kullanım: App functionality; kişisel ilerleme/özet oluşturma
- Paylaşım: Reklam veya veri brokerı amacıyla paylaşılmaz
- Saklama: Supabase veritabanı
- Güvenlik: kullanıcı bazlı kimlik doğrulama + RLS
- Kullanıcı silme: uygulama içi hesap silme akışı mevcut

### App activity / user-generated content değerlendirmesi

Yemek adı ve kullanıcının manuel girdiği kayıtlar uygulama içeriği sayılabilir. Play Console formundaki güncel kategori ifadeleri, gönderim sırasında yeniden kontrol edilmelidir. Bu veriler yalnızca kalori takibinin temel işlevi için saklanır.

### Third-party services / SDKs

**Google Sign-In / Google OAuth**
- Amaç: Authentication / Account management

**Supabase Auth / Database / Edge Functions**
- Amaç: Authentication, cloud storage/sync, account deletion

**Google Code Scanner / Google Play Services**
- Amaç: Product barcode scanning
- Uygulama doğrudan CAMERA izni istemez; tarama Google Play Services tarafından sağlanır.

**Open Food Facts**
- Amaç: Barkoddan ürün ve besin bilgisi sorgulama
- Gönderilen veri: taranan/yazılan ürün barkodu
- Kullanıcının hesap e-postası veya Supabase kullanıcı kimliği Open Food Facts sorgusuna özellikle eklenmez.

## Data sharing notları

Play Console'daki “collected” ve “shared” tanımları Google'ın güncel Data Safety kurallarına göre değerlendirilmelidir. Bir SDK'nın cihazdan hangi verileri otomatik gönderdiği son release dependency seti üzerinden ayrıca kontrol edilmelidir.

Bu V1'de reklam SDK'sı, analytics SDK'sı veya üçüncü taraf veri brokerı eklenmemiştir.

## Privacy Policy

Play Store üretim sürümünden önce:

- `docs/PRIVACY_POLICY_TR.md` aktif, herkese açık, coğrafi engeli olmayan bir HTTPS URL'de yayınlanmalı.
- Gizlilik politikası PDF olmamalı.
- Uygulama içinden de gizlilik politikasına erişilebilir olmalı.
- Politika; Google OAuth, Supabase, beslenme/kilo verileri, barkod/Open Food Facts ve hesap silme akışını açıklamalı.

## Account deletion

Uygulamada `Hesabımı ve verilerimi sil` akışı vardır.

Backend:
- authenticated Supabase Edge Function: `delete-calorie-account`
- çağıran kullanıcı JWT üzerinden doğrulanır
- yalnızca çağıranın `calorie_food_entries`, `calorie_weight_entries`, `calorie_daily_targets`, `calorie_profiles` kayıtları silinir
- ardından ilgili Supabase Auth kullanıcısı silinir
- service-role anahtarı Android istemcisine gönderilmez

Google Play'in web üzerinden hesap silme URL'si istemesi halinde ayrıca herkese açık bir hesap silme sayfası hazırlanmalıdır; bu alan Play Console hesabı oluşturulurken doğrulanacaktır.

## Yayın öncesi son kontrol

- Health Apps Declaration: Nutrition and Weight Management
- Data Safety formu son APK/AAB ve SDK listesiyle eşleştir
- aktif HTTPS gizlilik politikası URL'si ekle
- uygulama içinde gizlilik politikası linki ekle
- hesap silme akışını geçici test hesabıyla gerçek cihazda doğrula
- tıbbi cihaz olmadığını belirten uyarıyı mağaza açıklaması ve uygun uygulama ekranına ekle
