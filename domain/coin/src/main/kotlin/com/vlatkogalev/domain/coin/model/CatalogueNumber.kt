package com.vlatkogalev.domain.coin.model

data class CatalogueNumber(
    val catalogueName: String,
    val number: String?,
    val confidence: Confidence,
)
