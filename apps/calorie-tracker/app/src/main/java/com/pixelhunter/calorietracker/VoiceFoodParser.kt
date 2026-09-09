package com.pixelhunter.calorietracker

import java.util.Locale

data class ParsedVoiceFood(
    val food: CatalogFood,
    val grams: Double,
    val originalSegment: String,
    val portionNote: String? = null,
    val needsPortionReview: Boolean = false
)

object VoicePortionSupport {
    fun grams(food: CatalogFood, unit: String?, amount: Double?): Pair<Double, String?>? {
        val n = amount ?: 1.0
        if (unit in setOf("gram", "gr", "g")) return n to null
        if (unit in setOf("kilogram", "kg")) return n * 1000 to null
        val name = food.name.lowercase(Locale.forLanguageTag("tr-TR"))
        val each = when {
            unit in setOf("tabak", "porsiyon") && ("pilav" in name || "makarna" in name) -> 200.0
            unit == "kase" && "pilav" in name -> 180.0
            unit == "kase" && "çorba" in name -> 250.0
            unit == "kase" && "yoğurt" in name -> 200.0
            unit in setOf("bardak", "su bardağı") && ("ayran" in name || "süt" in name) -> 200.0
            unit == "dilim" && "ekmek" in name -> 25.0
            unit in setOf("adet", "tane") && "yumurta" in name -> 50.0
            unit in setOf("adet", "tane") && "elma" in name -> 150.0
            unit in setOf("adet", "tane") && "muz" in name -> 120.0
            else -> return null
        }
        return n * each to "${n.toInt()} $unit ≈ ${(n * each).toInt()} g"
    }
}

private val voiceAliases = linkedMapOf(
    "tavuk" to "Tavuk göğsü, pişmiş",
    "tavuk göğsü" to "Tavuk göğsü, pişmiş",
    "yumurta" to "Yumurta",
    "pirinç" to "Pirinç pilavı, pişmiş",
    "pirinç pilavı" to "Pirinç pilavı, pişmiş",
    "pilav" to "Pirinç pilavı, pişmiş",
    "bulgur" to "Bulgur pilavı, pişmiş",
    "bulgur pilavı" to "Bulgur pilavı, pişmiş",
    "mercimek" to "Mercimek çorbası",
    "mercimek çorbası" to "Mercimek çorbası",
    "menemen" to "Menemen",
    "simit" to "Simit",
    "ekmek" to "Beyaz ekmek",
    "tam buğday ekmeği" to "Tam buğday ekmeği",
    "poğaça" to "Poğaça",
    "börek" to "Su böreği",
    "lahmacun" to "Lahmacun",
    "et döner" to "Et döner",
    "tavuk döner" to "Tavuk döner",
    "döner" to "Tavuk döner",
    "adana" to "Adana kebap",
    "kebap" to "Adana kebap",
    "köfte" to "Köfte",
    "yoğurt" to "Yoğurt",
    "ayran" to "Ayran",
    "beyaz peynir" to "Beyaz peynir",
    "kaşar" to "Kaşar peyniri",
    "yulaf" to "Yulaf ezmesi",
    "muz" to "Muz",
    "elma" to "Elma",
    "portakal" to "Portakal",
    "avokado" to "Avokado",
    "patates" to "Patates, haşlanmış",
    "makarna" to "Makarna, pişmiş",
    "nohut" to "Nohut, pişmiş",
    "kuru fasulye" to "Kuru fasulye, pişmiş",
    "cici bebe" to "Cici Bebe bisküvi"
)

fun parseVoiceFoods(transcript: String): List<ParsedVoiceFood> {
    val normalized = transcript.lowercase(Locale.forLanguageTag("tr-TR"))
        .replace(" gramlık ", " gram ")
        .replace(" gr ", " gram ")
    val segments = normalized.split(Regex("\\s*(?:,|;|\\bve\\b|\\bile\\b|\\bsonra\\b)\\s*"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return segments.mapNotNull { segment ->
        val alias = voiceAliases.keys
            .filter { segment.contains(it) }
            .maxByOrNull { it.length }
            ?: return@mapNotNull null
        val targetName = voiceAliases.getValue(alias)
        val food = TurkishFoodCatalog.foods.firstOrNull { it.name == targetName } ?: return@mapNotNull null
        val number = Regex("(\\d+(?:[.,]\\d+)?)").find(segment)?.groupValues?.getOrNull(1)
            ?.replace(',', '.')?.toDoubleOrNull()
        val unit = Regex("\\b(gram|gr|g|kilogram|kg|adet|tane|tabak|kase|su bardağı|çay bardağı|bardak|yemek kaşığı|çay kaşığı|kaşık|dilim|porsiyon)\\b").find(segment)?.value
        val converted = VoicePortionSupport.grams(food, unit, number)
        val grams = when {
            converted != null -> converted.first
            number != null && unit == null -> 100.0
            food.name == "Yumurta" -> 50.0
            food.name == "Simit" -> 100.0
            food.name == "Lahmacun" -> 120.0
            food.name == "Ayran" -> 200.0
            else -> 100.0
        }.coerceIn(1.0, 2000.0)
        ParsedVoiceFood(food, grams, segment, converted?.second, number != null && unit != null && converted == null)
    }
}
