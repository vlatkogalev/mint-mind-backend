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
        obverseLettering = obverseLettering,
        reverseLettering = reverseLettering,
        designers = obverseDesigners + reverseDesigners,
        thicknessMm = thicknessMm,
    )
