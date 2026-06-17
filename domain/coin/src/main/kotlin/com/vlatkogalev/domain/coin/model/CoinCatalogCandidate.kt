package com.vlatkogalev.domain.coin.model

data class CoinCatalogCandidate(
    val externalReference: ExternalCoinReference,
    val title: String?,
    val countryOrIssuer: String?,
    val denomination: String?,
    val yearStart: Int?,
    val yearEnd: Int?,
    val composition: String? = null,
    val weightGrams: Double? = null,
    val diameterMm: Double? = null,
    val obverseDescription: String? = null,
    val reverseDescription: String? = null,
    val historicalContext: String? = null,
    val thumbnailUrl: String? = null,
    val numistaUrl: String? = null,
    val obverseLettering: String? = null,
    val reverseLettering: String? = null,
    val designers: List<String> = emptyList(),
)
