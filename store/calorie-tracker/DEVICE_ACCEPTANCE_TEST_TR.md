# Kalori Takip – Gerçek cihaz kabul testi

Amaç: Google Play production yüklemesinden önce tek bir Android cihazda release-candidate davranışını doğrulamak.

## A. Kurulum ve hesap
- APK temiz kurulum yapılıyor.
- Koyu/neon yeşil giriş ekranı açılıyor.
- Google ile giriş başarılı.
- OAuth dönüşünde uygulama doğru ekrana dönüyor.
- Uygulamayı kapat/aç: oturum korunuyor.

## B. Onboarding
- Yaş, boy, kilo, aktivite ve haftalık hedef giriliyor.
- Önerilen başlangıç kalorisi gösteriliyor.
- Hedef oluşturulunca ana ekrana geçiliyor.
- İlk kilo kaydı İlerleme ekranında görünüyor.

## C. Ana sayfa
- Kalori halkası ve kalan kalori doğru.
- Protein / karbonhidrat / yağ toplamı doğru güncelleniyor.
- +250 ml su çalışıyor.
- Su azaltma çalışıyor.
- Hedef düzenleme çalışıyor.

## D. Yemek CRUD
- Türkiye kataloğundan yemek ekle.
- Gram değiştir ve hesaplanan kalori/makroyu kontrol et.
- Kahvaltı/öğle/akşam/atıştırmalık ayrımı doğru.
- Manuel yemek ekle.
- Bulut yenilemesinden sonra kayıtlar kalıyor.
- Varsa düzenle/sil akışını kontrol et.

## E. Barkod
- Fiziksel ürün barkodu tara.
- Open Food Facts sonucu açılıyor.
- Gram değiştir.
- Günlüğe ekle.
- Bulunmayan barkod durumunda güvenli hata/manuel akış çalışıyor.

## F. Sesle yemek ekleme
- Mikrofon izni ilk kullanımda doğru isteniyor.
- “150 gram tavuk, 200 gram pilav ve bir bardak ayran” söyle.
- Tanınan metni kontrol et.
- Ayrıştırılmış yiyecek/porsiyonları kontrol et.
- Kullanıcı onayından sonra günlüğe ekleniyor.
- Mikrofon izni reddedildiğinde uygulama çökmüyor.

## G. Tarif / öğün oluşturucu
- En az 3 malzeme ekle.
- Her malzemenin gramını değiştir.
- Toplam kalori ve makroları kontrol et.
- Öğünü kaydet.
- Sağlık & Akıllı Takip içindeki kayıtlı öğünlerde göründüğünü kontrol et.
- Kahvaltı/öğle/akşam seçeneklerinden biriyle tek dokunuşla günlüğe ekle.

## H. Sağlık & Akıllı Takip
- Sesle Ekle kartı açılıyor.
- Tarif Oluştur kartı açılıyor.
- Fotoğraftan AI kartı açılıyor.
- Health Connect destek durumunu doğru gösteriyor.
- READ_STEPS izni isteniyor.
- İzin verilince günlük adım sayısı geliyor.
- Health Connect ayarından izni kaldır; uygulamaya dön ve davranışı kontrol et.

## I. Öğün bildirimleri
- Android 13+ ise bildirim izni isteniyor.
- Hatırlatmaları aç/kapat.
- Alarm kayıtlarının aç/kapat sonrası doğru çalıştığını kontrol et.
- Telefon yeniden başlatıldıktan sonra açık hatırlatmaların yeniden programlandığını kontrol et.

## J. Dinamik hedef
- Farklı tarihlerde en az iki kilo kaydı bulunan test hesabı kullan.
- Öneri oluşuyor.
- Öneri ±250 kcal sınırını aşmıyor.
- Kullanıcı “Öneriyi uygula” demeden kalori hedefi değişmiyor.

## K. AI fotoğraf analizi
Bu bölüm yalnızca server-side AI provider credential etkinleştirilirse production özelliği sayılır.
- Galeriden yemek fotoğrafı seç.
- Analiz sırasında loading görünür.
- Yiyecek isimleri, gram, kalori ve makrolar gösterilir.
- Tahmini sonuç uyarısı görünür.
- Öğün seçip tüm sonuçları günlüğe ekle.
- Çok büyük fotoğraf güvenli şekilde reddedilir.
- AI yapılandırılmamışsa uygulama çökmek yerine açıklayıcı hata gösterir.

## L. Gizlilik ve hesap silme
- Uygulama içi Gizlilik Politikası açılıyor.
- Launcher gizlilik kısayolu açılıyor.
- Hesap silme ekranında geri dönme çalışıyor.
- SADECE disposable test hesabında kalıcı silmeyi onayla.
- Kalori, kilo, hedef ve profil satırlarının silindiğini doğrula.
- Aynı hesabın Auth kullanıcısının da silindiğini doğrula.
- Silinen oturum uygulamada kullanılamıyor.

## M. Son görsel kontrol
- Küçük/orta/büyük ekran ölçeklerinde metin taşması yok.
- Türkçe karakterler doğru.
- Koyu tema dışında parlak/beyaz yanlış ekran yok.
- NavigationBar ve sistem gesture alanları çakışmıyor.
- Klavye açıldığında input alanları erişilebilir.
- Loading ve hata mesajları kullanıcıyı kilitlemiyor.

## Kabul kriteri
Production adayı ancak A–M bölümlerinde kritik hata kalmadığında Play Console’a yüklenir. AI provider credential aktif değilse K bölümü V1 mağaza vaadinden çıkarılır ve uygulama bu özelliği beta/konfigüre edilmemiş olarak ele alır.
