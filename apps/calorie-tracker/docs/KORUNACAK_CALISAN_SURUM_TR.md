# Kalori Takip — Korunacak çalışan sürüm ve regresyon kontrol listesi

Tarih: 2026-10-12
Repository: pixelhunterlabs-wq/PostFlow
Branch: feature/calorie-tracker-v1
Baz alınan uygulama kodu: 9f9969802aa6a46accc6f88df89ff1fb715a00cd
Baz APK CI run: 38306898432 (başarılı otomatik test + debug APK)

## Öncelikli kullanıcı talimatı

Sonraki geliştirmelerde bu sürümde çalışan özellikler KORUNMALI.
Özellik eklerken mevcut kullanıcı kayıtlarını, çalışan Google girişini ve yemek ekleme akışını bozma.
Kod değiştirdikten sonra testleri ve Android debug APK derlemesini yeniden çalıştır. Başarısız
derlemeyi veya eski commit'e ait APK'yı yeni sürüm diye paylaşma.

## İşleyen / uygulanmış temel özellikler

- Google ile giriş ve Supabase oturum yönlendirmesi. Kullanıcı 2026-10-12'de mobil cihazında
  Google girişinin çalıştığını bildirdi. Android callback: calorietracker://login.
- Ana ekran, öğün kaydı ve günlük kalori/makro takibi.
- Türkçe yerel besin kataloğu: başlangıçtaki ~36 kayda 143 yeni kayıt eklendi; yaklaşık
  179 besin kaydı. Yeni kayıtların kalori ve makro değerleri yaklaşık olup doğrulanmalıdır.
- Kahvaltılıklar, sebzeler, et ve tavuk, balık, ev yemekleri, çorbalar, meyveler,
  tatlılar, kuruyemişler, içecekler, salatalar ve atıştırmalıklar.
- Yiyecek arama ve kategori filtreleri; eskiden uygulanan ilk 18 ürün sınırı kaldırıldı.
- Öğün eklerken gram seçeneğine ek olarak ev ölçüleri:
  kahve kaşığı, çay kaşığı, tatlı kaşığı, yemek kaşığı,
  çorba kaşığı, servis kaşığı; çay bardağı, Ajda çay bardağı,
  su bardağı, büyük bardak, kahve fincanı, kupa, kepçe, kase.
- Katı/uygun yiyecekler için silme / normal / tepeleme kaşık miktarı.
- Ölçü ve adet/miktara göre yaklaşık gram ve kalori dönüşümü; kullanıcının
  gramı elle düzeltme imkânı. Kaydedilen öğün gramla tutulur ve 100 g besin
  verileriyle kalori/makro hesaplanır.
- Bardak ve kaşık hesaplama otomatik testleri:
  FoodPortionCalculatorTest.kt.

## Bilinen tamamlanmamış alan

- Premium karşılaştırma ve Play Billing altyapısı var, ancak gerçek abonelikleri
  satmak/yetkilendirmek ayrıca güvenli sunucu doğrulaması ve Google Play testleri gerektirir.
  Premium'un canlı ödemelerle tamamen çalıştığını varsayma.

## Her sonraki APK için zorunlu regresyon kontrolü

1. Google ile giriş açılmalı ve oturum sonrası ana sayfa görünmeli.
2. Öğün Ekle içinde tüm kategori filtreleri ve yüzlerce besin kaydı görünmeli.
3. Arama Türkçe karakterli isimlerle çalışmalı; sonuçlar 18 öğede kesilmemeli.
4. Her kaşık ve bardak seçeneği görünüp seçilebilmeli.
5. Silme, normal ve tepeleme kaşık dönüşümleri doğrulanmalı.
6. Kesirli adet (ör. 1,5 kaşık; 0,5 bardak) çalışmalı.
7. Tahmini gram ve kalori hesapları gösterilmeli, gram elle düzeltilebilmeli.
8. Günlüğe kaydedilen kalori ve makrolar seçilen gramla tutarlı olmalı.
9. Mevcut kullanıcı yemek, su, kilo ve hesap kayıtları korunmalı.
10. Gradle unit testleri ve assembleDebug başarıyla geçmeli; yayınlanan APK
    en güncel commit'ten üretilmiş olmalı.
