package com.pixelhunter.calorietracker

data class EditableAiMeal(val item: AiMealItem, val included: Boolean = true, val baseGrams: Double = item.grams, val baseCalories: Double = item.calories, val baseProtein: Double = item.proteinG, val baseCarbs: Double = item.carbsG, val baseFat: Double = item.fatG)
fun scaleAiMeal(meal: EditableAiMeal, grams: Double): AiMealItem {
    require(grams > 0)
    val r = grams / meal.baseGrams.coerceAtLeast(0.01)
    return meal.item.copy(grams = grams, calories = meal.baseCalories * r, proteinG = meal.baseProtein * r, carbsG = meal.baseCarbs * r, fatG = meal.baseFat * r)
}
fun aiPhotoError(code: Int, body: String = ""): String = when {
    code == 401 -> "Oturumunun süresi doldu. Lütfen tekrar giriş yap."
    code == 413 -> "Fotoğraf çok büyük. Daha küçük bir fotoğraf seç."
    code == 503 && body.contains("ai_not_configured") -> "AI fotoğraf analizi şu anda kullanıma hazır değil."
    code == 502 -> "Fotoğraf şu anda analiz edilemedi. Lütfen tekrar dene."
    else -> "Bağlantı kurulamadı. İnternet bağlantını kontrol edip tekrar dene."
}
