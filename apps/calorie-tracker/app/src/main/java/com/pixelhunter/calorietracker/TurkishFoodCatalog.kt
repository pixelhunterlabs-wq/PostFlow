package com.pixelhunter.calorietracker

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class CatalogFood(
    val name: String,
    val calories100g: Double,
    val protein100g: Double,
    val carbs100g: Double,
    val fat100g: Double,
    val category: String = "Diğer",
    val aliases: List<String> = emptyList(),
    val defaultPortionName: String = "100 gram",
    val defaultPortionGrams: Double = 100.0,
    val isEstimate: Boolean = false
)

object TurkishFoodCatalog {
    val foods = listOf(
        CatalogFood("Tavuk göğsü, pişmiş", 165.0, 31.0, 0.0, 3.6),
        CatalogFood("Yumurta", 143.0, 12.6, 0.7, 9.5),
        CatalogFood("Pirinç pilavı, pişmiş", 130.0, 2.7, 28.0, 0.3),
        CatalogFood("Bulgur pilavı, pişmiş", 83.0, 3.1, 18.6, 0.2),
        CatalogFood("Mercimek çorbası", 68.0, 3.5, 10.0, 1.7),
        CatalogFood("Menemen", 105.0, 5.2, 4.0, 7.5),
        CatalogFood("Simit", 338.0, 10.0, 56.0, 8.5),
        CatalogFood("Beyaz ekmek", 265.0, 9.0, 49.0, 3.2),
        CatalogFood("Tam buğday ekmeği", 247.0, 13.0, 41.0, 3.4),
        CatalogFood("Poğaça", 360.0, 8.0, 44.0, 17.0),
        CatalogFood("Su böreği", 310.0, 10.0, 28.0, 18.0),
        CatalogFood("Lahmacun", 230.0, 10.0, 28.0, 8.5),
        CatalogFood("Et döner", 220.0, 22.0, 4.0, 13.0),
        CatalogFood("Tavuk döner", 190.0, 22.0, 4.0, 9.0),
        CatalogFood("Adana kebap", 239.0, 25.0, 2.0, 15.0),
        CatalogFood("Köfte", 220.0, 22.0, 5.0, 13.0),
        CatalogFood("Yoğurt", 61.0, 3.5, 4.7, 3.3),
        CatalogFood("Ayran", 37.0, 2.0, 3.0, 1.7),
        CatalogFood("Beyaz peynir", 264.0, 14.0, 4.0, 21.0),
        CatalogFood("Kaşar peyniri", 404.0, 25.0, 2.0, 33.0),
        CatalogFood("Yulaf ezmesi", 389.0, 16.9, 66.3, 6.9),
        CatalogFood("Muz", 89.0, 1.1, 22.8, 0.3),
        CatalogFood("Elma", 52.0, 0.3, 13.8, 0.2),
        CatalogFood("Portakal", 47.0, 0.9, 11.8, 0.1),
        CatalogFood("Avokado", 160.0, 2.0, 8.5, 14.7),
        CatalogFood("Patates, haşlanmış", 87.0, 1.9, 20.1, 0.1),
        CatalogFood("Makarna, pişmiş", 131.0, 5.0, 25.0, 1.1),
        CatalogFood("Nohut, pişmiş", 164.0, 8.9, 27.4, 2.6),
        CatalogFood("Kuru fasulye, pişmiş", 127.0, 8.7, 22.8, 0.5),
        CatalogFood("Cici Bebe bisküvi", 438.0, 7.0, 73.0, 13.0),
        CatalogFood("Baklava", 428.0, 5.8, 52.0, 21.0, "Tatlılar", listOf("baklawa"), "1 dilim", 35.0, true),
        CatalogFood("Sütlaç", 134.0, 3.1, 24.0, 2.8, "Tatlılar", listOf("sutlac"), "1 küçük kase", 180.0, true),
        CatalogFood("Kazandibi", 155.0, 3.5, 24.0, 5.0, "Tatlılar", listOf("kazan dibi"), "1 porsiyon", 150.0, true),
        CatalogFood("Çiğ köfte", 179.0, 4.2, 30.0, 4.8, "Ev Yemekleri", listOf("cig kofte", "cigkofte"), "1 porsiyon", 150.0, true),
        CatalogFood("Kuru fasulye", 127.0, 8.7, 22.8, 0.5, "Ev Yemekleri", listOf("kurufasulye"), "1 porsiyon", 220.0, true),
        CatalogFood("Tavuk göğsü", 165.0, 31.0, 0.0, 3.6, "Et Tavuk Balık Bakliyat", listOf("tavuk gogsu"), "1 porsiyon", 150.0, false),
        CatalogFood("Sahanda yumurta", 196.0, 13.0, 1.0, 15.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Haşlanmış yumurta", 155.0, 13.0, 1.0, 11.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Omlet", 154.0, 11.0, 2.0, 12.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Çırpılmış yumurta", 149.0, 10.0, 2.0, 11.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Zeytin siyah", 116.0, 1.0, 6.0, 11.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Zeytin yeşil", 145.0, 1.0, 4.0, 15.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Lor peyniri", 98.0, 17.0, 3.0, 2.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Labne", 235.0, 5.0, 5.0, 22.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Tulum peyniri", 350.0, 23.0, 2.0, 28.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Bal", 304.0, 0.0, 82.0, 0.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Reçel", 250.0, 0.0, 65.0, 0.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Tahin", 595.0, 17.0, 21.0, 54.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Pekmez", 290.0, 0.0, 73.0, 0.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Tereyağı", 717.0, 1.0, 0.0, 81.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Krep", 227.0, 6.0, 28.0, 10.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Pankek", 227.0, 6.0, 28.0, 10.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Tost kaşarlı", 295.0, 12.0, 30.0, 14.0, "Kahvaltılıklar", isEstimate = true),
        CatalogFood("Domates", 18.0, 1.0, 4.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Salatalık", 15.0, 1.0, 4.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Havuç", 41.0, 1.0, 10.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Brokoli", 34.0, 3.0, 7.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Karnabahar", 25.0, 2.0, 5.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Ispanak", 23.0, 3.0, 4.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Kabak", 17.0, 1.0, 3.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Patlıcan", 25.0, 1.0, 6.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Biber yeşil", 20.0, 1.0, 5.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Biber kırmızı", 31.0, 1.0, 6.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Mantar", 22.0, 3.0, 3.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Marul", 15.0, 1.0, 3.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Soğan", 40.0, 1.0, 9.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Bezelye", 81.0, 5.0, 14.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Taze fasulye", 31.0, 2.0, 7.0, 0.0, "Sebzeler", isEstimate = true),
        CatalogFood("Mısır haşlanmış", 96.0, 3.0, 21.0, 2.0, "Sebzeler", isEstimate = true),
        CatalogFood("Dana biftek ızgara", 217.0, 27.0, 0.0, 12.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Dana bonfile ızgara", 190.0, 29.0, 0.0, 8.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Dana kuşbaşı pişmiş", 215.0, 28.0, 0.0, 11.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Dana kıyma pişmiş", 250.0, 26.0, 0.0, 15.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Kuzu pirzola", 294.0, 25.0, 0.0, 21.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Kuzu şiş", 260.0, 26.0, 0.0, 17.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Et sote", 190.0, 20.0, 5.0, 10.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Et haşlama", 180.0, 25.0, 3.0, 7.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Tavuk but fırında", 209.0, 26.0, 0.0, 11.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Tavuk kanat fırında", 254.0, 24.0, 0.0, 17.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Tavuk şiş", 165.0, 30.0, 1.0, 4.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Tavuk sote", 145.0, 19.0, 5.0, 6.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Tavuk pane", 245.0, 19.0, 16.0, 12.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Hindi göğüs", 135.0, 29.0, 0.0, 2.0, "Et ve Tavuk", isEstimate = true),
        CatalogFood("Somon fırında", 206.0, 22.0, 0.0, 13.0, "Balık ve Deniz Ürünleri", isEstimate = true),
        CatalogFood("Levrek ızgara", 125.0, 24.0, 0.0, 3.0, "Balık ve Deniz Ürünleri", isEstimate = true),
        CatalogFood("Çipura ızgara", 128.0, 22.0, 0.0, 4.0, "Balık ve Deniz Ürünleri", isEstimate = true),
        CatalogFood("Hamsi tava", 220.0, 20.0, 10.0, 11.0, "Balık ve Deniz Ürünleri", isEstimate = true),
        CatalogFood("Hamsi buğulama", 130.0, 19.0, 2.0, 5.0, "Balık ve Deniz Ürünleri", isEstimate = true),
        CatalogFood("Ton balığı suda", 116.0, 26.0, 0.0, 1.0, "Balık ve Deniz Ürünleri", isEstimate = true),
        CatalogFood("Karides haşlanmış", 99.0, 24.0, 0.0, 0.0, "Balık ve Deniz Ürünleri", isEstimate = true),
        CatalogFood("Yaprak sarma", 165.0, 3.0, 20.0, 8.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Biber dolması", 130.0, 3.0, 18.0, 5.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Karnıyarık", 140.0, 8.0, 8.0, 9.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("İmam bayıldı", 120.0, 2.0, 11.0, 8.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Türlü", 85.0, 2.0, 10.0, 4.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Taze fasulye zeytinyağlı", 80.0, 2.0, 9.0, 4.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Ispanak yemeği", 85.0, 3.0, 7.0, 5.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Bezelye yemeği", 110.0, 4.0, 14.0, 4.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Nohut yemeği", 150.0, 7.0, 20.0, 5.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Mercimek yemeği", 115.0, 7.0, 17.0, 2.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Fırın makarna", 185.0, 8.0, 24.0, 6.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Mantı yoğurtlu", 195.0, 8.0, 25.0, 7.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Bamya yemeği", 75.0, 2.0, 8.0, 4.0, "Ev Yemekleri", isEstimate = true),
        CatalogFood("Ezogelin çorbası", 65.0, 3.0, 10.0, 2.0, "Çorbalar", isEstimate = true),
        CatalogFood("Tarhana çorbası", 60.0, 2.0, 10.0, 1.0, "Çorbalar", isEstimate = true),
        CatalogFood("Yayla çorbası", 75.0, 3.0, 8.0, 3.0, "Çorbalar", isEstimate = true),
        CatalogFood("Domates çorbası", 45.0, 1.0, 6.0, 2.0, "Çorbalar", isEstimate = true),
        CatalogFood("Tavuk çorbası", 60.0, 5.0, 5.0, 2.0, "Çorbalar", isEstimate = true),
        CatalogFood("İşkembe çorbası", 105.0, 9.0, 3.0, 6.0, "Çorbalar", isEstimate = true),
        CatalogFood("Armut", 57.0, 0.0, 15.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Şeftali", 39.0, 1.0, 10.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Kayısı", 48.0, 1.0, 11.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Çilek", 32.0, 1.0, 8.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Üzüm", 69.0, 1.0, 18.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Karpuz", 30.0, 1.0, 8.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Kavun", 34.0, 1.0, 8.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Ananas", 50.0, 1.0, 13.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Kivi", 61.0, 1.0, 15.0, 1.0, "Meyveler", isEstimate = true),
        CatalogFood("Nar", 83.0, 2.0, 19.0, 1.0, "Meyveler", isEstimate = true),
        CatalogFood("İncir", 74.0, 1.0, 19.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Mandalina", 53.0, 1.0, 13.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Kiraz", 63.0, 1.0, 16.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Erik", 46.0, 1.0, 11.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Hurma", 277.0, 2.0, 75.0, 0.0, "Meyveler", isEstimate = true),
        CatalogFood("Kuru kayısı", 241.0, 3.0, 63.0, 1.0, "Meyveler", isEstimate = true),
        CatalogFood("Kuru üzüm", 299.0, 3.0, 79.0, 1.0, "Meyveler", isEstimate = true),
        CatalogFood("Künefe", 400.0, 10.0, 40.0, 22.0, "Tatlılar", isEstimate = true),
        CatalogFood("Kadayıf", 390.0, 5.0, 50.0, 19.0, "Tatlılar", isEstimate = true),
        CatalogFood("Revani", 310.0, 5.0, 49.0, 11.0, "Tatlılar", isEstimate = true),
        CatalogFood("Şekerpare", 380.0, 5.0, 55.0, 16.0, "Tatlılar", isEstimate = true),
        CatalogFood("İrmik helvası", 320.0, 5.0, 46.0, 13.0, "Tatlılar", isEstimate = true),
        CatalogFood("Un helvası", 410.0, 5.0, 48.0, 23.0, "Tatlılar", isEstimate = true),
        CatalogFood("Tavukgöğsü tatlısı", 155.0, 4.0, 26.0, 4.0, "Tatlılar", isEstimate = true),
        CatalogFood("Muhallebi", 120.0, 4.0, 20.0, 3.0, "Tatlılar", isEstimate = true),
        CatalogFood("Profiterol", 290.0, 6.0, 30.0, 16.0, "Tatlılar", isEstimate = true),
        CatalogFood("Çikolatalı pasta", 365.0, 5.0, 47.0, 18.0, "Tatlılar", isEstimate = true),
        CatalogFood("Dondurma vanilyalı", 207.0, 4.0, 24.0, 11.0, "Tatlılar", isEstimate = true),
        CatalogFood("Kabak tatlısı", 190.0, 1.0, 45.0, 1.0, "Tatlılar", isEstimate = true),
        CatalogFood("Aşure", 140.0, 4.0, 27.0, 2.0, "Tatlılar", isEstimate = true),
        CatalogFood("Badem", 579.0, 21.0, 22.0, 50.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Ceviz", 654.0, 15.0, 14.0, 65.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Fındık", 628.0, 15.0, 17.0, 61.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Antep fıstığı", 560.0, 20.0, 28.0, 45.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Yer fıstığı", 567.0, 26.0, 16.0, 49.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Kaju", 553.0, 18.0, 30.0, 44.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Kabak çekirdeği", 559.0, 30.0, 11.0, 49.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Ay çekirdeği", 584.0, 21.0, 20.0, 51.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Leblebi", 364.0, 19.0, 61.0, 6.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Kestane", 196.0, 2.0, 45.0, 1.0, "Kuruyemişler", isEstimate = true),
        CatalogFood("Su", 0.0, 0.0, 0.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Çay şekersiz", 1.0, 0.0, 0.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Çay şekerli", 20.0, 0.0, 5.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Türk kahvesi sade", 2.0, 0.0, 0.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Filtre kahve sade", 1.0, 0.0, 0.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Latte", 50.0, 3.0, 5.0, 2.0, "İçecekler", isEstimate = true),
        CatalogFood("Cappuccino", 45.0, 3.0, 4.0, 2.0, "İçecekler", isEstimate = true),
        CatalogFood("Süt tam yağlı", 61.0, 3.0, 5.0, 3.0, "İçecekler", isEstimate = true),
        CatalogFood("Süt yarım yağlı", 47.0, 3.0, 5.0, 2.0, "İçecekler", isEstimate = true),
        CatalogFood("Kefir", 55.0, 3.0, 5.0, 3.0, "İçecekler", isEstimate = true),
        CatalogFood("Portakal suyu", 45.0, 1.0, 10.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Elma suyu", 46.0, 0.0, 11.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Kola", 42.0, 0.0, 11.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Şekersiz kola", 0.0, 0.0, 0.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Gazoz", 40.0, 0.0, 10.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Limonata", 42.0, 0.0, 11.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Şalgam suyu", 12.0, 0.0, 3.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Maden suyu sade", 0.0, 0.0, 0.0, 0.0, "İçecekler", isEstimate = true),
        CatalogFood("Çoban salata", 35.0, 1.0, 5.0, 1.0, "Salatalar", isEstimate = true),
        CatalogFood("Mevsim salata", 30.0, 1.0, 5.0, 1.0, "Salatalar", isEstimate = true),
        CatalogFood("Gavurdağı salatası", 125.0, 3.0, 8.0, 9.0, "Salatalar", isEstimate = true),
        CatalogFood("Ton balıklı salata", 115.0, 12.0, 5.0, 5.0, "Salatalar", isEstimate = true),
        CatalogFood("Sezar salata", 180.0, 11.0, 8.0, 12.0, "Salatalar", isEstimate = true),
        CatalogFood("Kısır", 165.0, 4.0, 28.0, 5.0, "Salatalar", isEstimate = true),
        CatalogFood("Patates cipsi", 536.0, 7.0, 53.0, 35.0, "Atıştırmalıklar", isEstimate = true),
        CatalogFood("Patlamış mısır yağsız", 387.0, 13.0, 78.0, 5.0, "Atıştırmalıklar", isEstimate = true),
        CatalogFood("Kraker", 430.0, 9.0, 70.0, 13.0, "Atıştırmalıklar", isEstimate = true),
        CatalogFood("Bitter çikolata", 600.0, 8.0, 46.0, 43.0, "Atıştırmalıklar", isEstimate = true),
        CatalogFood("Sütlü çikolata", 535.0, 8.0, 59.0, 30.0, "Atıştırmalıklar", isEstimate = true),
        CatalogFood("Protein bar", 360.0, 30.0, 35.0, 12.0, "Atıştırmalıklar", isEstimate = true)
    )
}

class LocalWellnessStore(context: Context) {
    private val prefs = context.getSharedPreferences("calorie_wellness", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun onboardingDone(userKey: String): Boolean = prefs.getBoolean("onboarding_done_${userKey.lowercase()}", false)
    fun markOnboardingDone(userKey: String) = prefs.edit().putBoolean("onboarding_done_${userKey.lowercase()}", true).commit()

    fun waterMl(date: LocalDate = LocalDate.now()): Int = prefs.getInt("water_$date", 0)
    fun addWater(amount: Int, date: LocalDate = LocalDate.now()): Int {
        val next = (waterMl(date) + amount).coerceIn(0, 6000)
        prefs.edit().putInt("water_$date", next).apply()
        return next
    }
    fun removeWater(amount: Int, date: LocalDate = LocalDate.now()): Int {
        val next = (waterMl(date) - amount).coerceAtLeast(0)
        prefs.edit().putInt("water_$date", next).apply()
        return next
    }

    fun favorites(): List<CatalogFood> = runCatching {
        json.decodeFromString<List<CatalogFood>>(prefs.getString("favorites_json", "[]") ?: "[]")
    }.getOrDefault(emptyList())

    fun toggleFavorite(food: CatalogFood): List<CatalogFood> {
        val current = favorites().toMutableList()
        val index = current.indexOfFirst { it.name == food.name }
        if (index >= 0) current.removeAt(index) else current.add(food)
        prefs.edit().putString("favorites_json", json.encodeToString(current)).apply()
        return current
    }
}

fun recommendedCalories(
    gender: String,
    age: Int,
    heightCm: Double,
    weightKg: Double,
    activityFactor: Double,
    weeklyGoalKg: Double
): Int {
    val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age + if (gender == "Kadın") -161.0 else 5.0
    val maintenance = base * activityFactor
    val dailyAdjustment = (weeklyGoalKg * 7700.0) / 7.0
    return (maintenance - dailyAdjustment).toInt().coerceIn(1200, 4500)
}
