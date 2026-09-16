package com.pixelhunter.calorietracker

import java.util.Locale

/** Local fallback search. Remote catalog search uses the same normalized terms in Postgres. */
object FoodCatalogSearch {
    fun normalize(value: String): String = value
        .lowercase(Locale.forLanguageTag("tr-TR"))
        .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
        .replace('ö', 'o').replace('ş', 's').replace('ü', 'u')
        .replace(Regex("[^a-z0-9]+"), " ").trim()

    fun search(foods: List<CatalogFood>, query: String, category: String? = null, limit: Int = 30): List<CatalogFood> {
        val normalizedQuery = normalize(query)
        return foods.asSequence()
            .filter { category.isNullOrBlank() || category == "Tümü" || it.category == category }
            .map { food ->
                val terms = listOf(food.name, food.category) + food.aliases
                val score = when {
                    normalizedQuery.isBlank() -> 1
                    normalize(food.name) == normalizedQuery -> 0
                    terms.any { normalize(it).startsWith(normalizedQuery) } -> 1
                    terms.any { normalize(it).contains(normalizedQuery) } -> 2
                    terms.any { levenshtein(normalize(it), normalizedQuery) <= 2 } -> 3
                    else -> Int.MAX_VALUE
                }
                score to food
            }
            .filter { it.first != Int.MAX_VALUE }
            .sortedWith(compareBy<Pair<Int, CatalogFood>> { it.first }.thenBy { it.second.name })
            .map { it.second }
            .take(limit)
            .toList()
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var previous = IntArray(right.length + 1) { it }
        left.forEachIndexed { i, c ->
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            right.forEachIndexed { j, other ->
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (c == other) 0 else 1)
            }
            previous = current
        }
        return previous.last()
    }
}
