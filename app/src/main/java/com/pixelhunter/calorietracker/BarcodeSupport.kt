package com.pixelhunter.calorietracker

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Pure barcode helpers kept outside the scanner UI so failure and API parsing stay testable. */
object BarcodeSupport {
    private val acceptedLengths = setOf(8, 12, 13)

    fun normalize(rawValue: String?): String? {
        val digits = rawValue.orEmpty().filter(Char::isDigit)
        return digits.takeIf { it.length in acceptedLengths }
    }

    fun scannerFailureMessage(reason: String? = null): String =
        if (reason.isNullOrBlank()) "Barkod tarayıcı açılamadı. Numarayı manuel girebilirsin."
        else "Barkod tarayıcı açılamadı ($reason). Numarayı manuel girebilirsin."

    fun parseOpenFoodFacts(barcode: String, payload: String): BarcodeProduct? {
        val response = Json { ignoreUnknownKeys = true; isLenient = true }
            .decodeFromString<OpenFoodFactsResponse>(payload)
        val product = response.product ?: return null
        if (response.status != 1) return null
        val nutrients = product.nutriments
        return BarcodeProduct(
            barcode = barcode,
            name = product.productName?.trim().orEmpty()
                .ifBlank { product.brands?.trim().orEmpty() }
                .ifBlank { "Barkodlu ürün" },
            brand = product.brands.orEmpty(),
            calories100g = nutrients?.calories100g ?: 0.0,
            protein100g = nutrients?.protein100g ?: 0.0,
            carbs100g = nutrients?.carbs100g ?: 0.0,
            fat100g = nutrients?.fat100g ?: 0.0
        )
    }
}

@Serializable
internal data class OpenFoodFactsResponse(val status: Int = 0, val product: OpenFoodFactsProduct? = null)

@Serializable
internal data class OpenFoodFactsProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    val nutriments: OpenFoodFactsNutriments? = null
)

@Serializable
internal data class OpenFoodFactsNutriments(
    @SerialName("energy-kcal_100g") val calories100g: Double? = null,
    @SerialName("proteins_100g") val protein100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbs100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null
)
