# Kalori Takip – Gizlilik Politikası

Son güncelleme: 6 Eylül 2026

Kalori Takip, kullanıcıların günlük kalori ve makro besin kayıtlarını, kilo geçmişini ve beslenme hedeflerini takip etmelerine yardımcı olan bir Android uygulamasıdır.

## Toplanan ve işlenen bilgiler

Uygulama, kullanıcı hesabı oluşturmak ve oturumu yönetmek için Google ile giriş özelliğini kullanabilir. Bu işlem sırasında Google hesabınızla ilişkili temel profil bilgileri (örneğin e-posta adresi ve kullanıcı kimliği) Supabase Auth üzerinden işlenebilir.

Uygulamaya sizin tarafınızdan girilen beslenme kayıtları; yemek adı, miktar/gram, kalori, protein, karbonhidrat, yağ, öğün türü ve kayıt zamanı gibi bilgileri içerebilir. Kilo takibi kullanıldığında girdiğiniz kilo değerleri ve tarihleri de saklanabilir. Günlük kalori hedefiniz ve ilgili uygulama tercihleri hesabınızla ilişkilendirilebilir.

## Verilerin saklanması

Hesap ve uygulama verileri Supabase altyapısında saklanır. Veritabanı erişimi kullanıcı bazlı Row Level Security (RLS) kurallarıyla sınırlandırılır. Uygulamanın Android istemcisinde Supabase service-role/secret anahtarı kullanılmaz.

## Barkod tarama ve ürün bilgileri

Uygulama, ürün barkodlarını taramak için Google Code Scanner özelliğini kullanabilir. Tarama işlemi Google Play Hizmetleri tarafından gerçekleştirilir ve uygulamaya tarama sonucu olan barkod değeri döndürülür. Uygulama, bulunan barkod numarasını ürün adı ve besin değerlerini sorgulamak amacıyla Open Food Facts hizmetine gönderebilir.

Open Food Facts'tan alınan ürün verilerinin doğruluğu ürün veritabanındaki bilgilere bağlıdır. Kullanıcılar kayıt öncesinde gösterilen değerleri kontrol etmelidir.

## Verilerin kullanım amaçları

Toplanan bilgiler yalnızca uygulamanın temel işlevlerini sağlamak; kullanıcı oturumunu yönetmek; kalori, makro ve kilo geçmişini senkronize etmek; haftalık/aylık özetleri oluşturmak ve kullanıcının istediği kayıtları farklı cihazlarda erişilebilir tutmak amacıyla kullanılır.

## Üçüncü taraf hizmetler

Uygulama işlevlerine bağlı olarak Google Sign-In / Google Play Hizmetleri, Supabase ve Open Food Facts hizmetlerinden yararlanabilir. Bu hizmetlerin kendi gizlilik politikaları ve kullanım koşulları geçerlidir.

## Hesap ve veri silme

Google Play sürümü yayınlanmadan önce uygulama içinde hesap ve kullanıcı verilerini silmeye yönelik bir seçenek sağlanacaktır. Kullanıcı hesabını sildiğinde uygulamanın kendi veritabanında hesabıyla ilişkili kişisel kayıtların silinmesi amaçlanır; yasal veya güvenlik gerekleri nedeniyle tutulması zorunlu bilgiler varsa bunlar ilgili gereklilikler kapsamında ele alınır.

## Çocukların gizliliği

Kalori Takip genel amaçlı bir beslenme takip aracıdır ve özellikle çocuklara yönelik olarak tasarlanmamıştır.

## Güvenlik

Kullanıcı verilerinin yetkisiz erişime karşı korunması için kimlik doğrulama ve veritabanı erişim politikaları uygulanır. İnternet üzerinden yapılan hiçbir veri aktarımı veya depolama yöntemi mutlak güvenlik garantisi vermez.

## Değişiklikler

Bu politika uygulamanın özellikleri veya kullanılan hizmetler değiştikçe güncellenebilir. Güncel sürümde son güncelleme tarihi belirtilir.

## İletişim

Google Play yayını öncesinde geliştirici destek e-posta adresi ve yayınlanmış gizlilik politikası URL'si bu bölüme eklenecektir.
