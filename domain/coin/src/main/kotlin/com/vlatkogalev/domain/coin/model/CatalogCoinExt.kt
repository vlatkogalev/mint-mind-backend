package com.vlatkogalev.domain.coin.model

fun CatalogCoin.toMatchableCoin(): MatchableCoin =
    MatchableCoin(
        countryOrIssuer = fingerprint.countryOrIssuer,
        denomination = fingerprint.denomination,
        yearStart = fingerprint.year,
        yearEnd = fingerprint.year,
        composition = composition,
        weightGrams = weightGrams,
        diameterMm = diameterMm,
        obverseLettering = null,
        reverseLettering = null,
        designers = emptyList(),
    )
