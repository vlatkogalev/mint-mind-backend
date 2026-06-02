package com.vlatkogalev.domain.coin.model

data class CoinCatalogCandidate(
    val externalReference: ExternalCoinReference,
    val title: String?,
    val countryOrIssuer: String?,
    val denomination: String?,
    val yearStart: Int?,
    val yearEnd: Int?,
)
