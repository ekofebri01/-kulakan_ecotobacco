package com.aistudio.ecotobacco.kfzqw.scanner.model

/**
 * Model Data Hasil Pemindaian Nota/Struk
 */
data class ScannedItem(
    val name: String,
    val quantity: Double,
    val unit: String = "unit",
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val needsReview: Boolean = false,
    val reviewReason: String? = null,
    val confidence: Float = 1.0f
) {
    // Legacy compatibility property
    val price: Double
        get() = if (unitPrice > 0) unitPrice else (totalPrice / if (quantity > 0) quantity else 1.0)
}
