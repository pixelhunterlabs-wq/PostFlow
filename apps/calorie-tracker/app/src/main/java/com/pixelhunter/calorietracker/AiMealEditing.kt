package com.pixelhunter.calorietracker

data class EditableAiMeal(val item: AiMealItem, val included: Boolean = true, val baseGrams: Double = item.grams, val baseCalories: Double = item.calories, val baseProtein: Double = item.proteinG, val baseCarbs: Double = item.carbsG, val baseFat: Double = item.fatG)
fun scaleAiMeal(meal: EditableAiMeal, grams: Double): AiMealItem {
    require(grams > 0)
    val r = grams / meal.baseGrams.coerceAtLeast(0.01)
    return meal.item.copy(grams = grams, calories = meal.baseCalories * r, proteinG = meal.baseProtein * r, carbsG = meal.baseCarbs * r, fatG = meal.baseFat * r)
}
fun isValidAiMeal(item: AiMealItem): Boolean = item.grams > 0 && item.calories >= 0 &&
    item.proteinG >= 0 && item.carbsG >= 0 && item.fatG >= 0
fun selectedAiMeals(meals: List<EditableAiMeal>): List<AiMealItem> = meals.filter { it.included }.map { it.item }
fun aiPhotoError(code: Int, body: String = ""): String = when {
    code == 401 -> "Oturum süren dolmuş. Tekrar giriş yap."
    code == 413 -> "Fotoğraf çok büyük. Daha küçük bir fotoğraf seç."
    body.contains("ai_not_configured") -> "AI analizi şu anda yapılandırılmamış."
    body.contains("openai_rate_limited") -> "AI servisi şu anda yoğun. Biraz sonra tekrar dene."
    body.contains("openai_auth_failed") -> "AI servisine şu anda bağlanılamıyor."
    body.contains("openai_bad_request") -> "Fotoğraf şu anda analiz edilemedi. Başka bir fotoğraf dene."
    body.contains("openai_unavailable") -> "AI servisi geçici olarak kullanılamıyor. Biraz sonra tekrar dene."
    else -> "İnternet bağlantını kontrol edip tekrar dene."
}
