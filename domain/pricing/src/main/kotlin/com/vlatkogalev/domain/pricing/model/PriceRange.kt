package com.vlatkogalev.domain.pricing.model

data class PriceRange(
    val low: Double,
    val high: Double,
    val median: Double,
    val mean: Double,
    val sampleSize: Int,
)