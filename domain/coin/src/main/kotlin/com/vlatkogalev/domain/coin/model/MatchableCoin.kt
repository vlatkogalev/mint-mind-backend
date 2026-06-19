package com.vlatkogalev.domain.coin.model

data class MatchableCoin(
    val countryOrIssuer: String?,
    val denomination: String?,
    val yearStart: Int?,
    val yearEnd: Int?,
    val composition: String?,
    val weightGrams: Double?,
    val diameterMm: Double?,
    val obverseLettering: String? = null,
    val reverseLettering: String? = null,
    val designers: List<String> = emptyList(),
    val thicknessMm: Double? = null,
)
