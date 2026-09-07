package com.pixelhunter.calorietracker

import java.util.Locale

data class ParsedVoiceFood(
    val food: CatalogFood,
    val grams: Double,
    val originalSegment: String
)

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
    val segments = normalized.split(Regex("\\s*(?:,|;|\\bve\\b|\\bile\\b)\\s*"))
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
        val grams = when {
            segment.contains("adet") && food.name == "Yumurta" -> (number ?: 1.0) * 50.0
            segment.contains("bardak") && food.name == "Ayran" -> (number ?: 1.0) * 200.0
            number != null -> number
            food.name == "Yumurta" -> 50.0
            food.name == "Simit" -> 100.0
            food.name == "Lahmacun" -> 120.0
            food.name == "Ayran" -> 200.0
            else -> 100.0
        }.coerceIn(1.0, 2000.0)
        ParsedVoiceFood(food, grams, segment)
    }
}
