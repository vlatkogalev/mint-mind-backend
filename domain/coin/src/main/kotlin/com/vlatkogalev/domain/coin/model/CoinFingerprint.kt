package com.vlatkogalev.domain.coin.model

data class CoinFingerprint(
    val countryOrIssuer: String?,
    val denomination: String?,
    val seriesName: String?,
    val year: Int?,
    val mintMark: String?,
) {
    fun normalized(): CoinFingerprint =
        CoinFingerprint(
            countryOrIssuer = countryOrIssuer?.trim()?.lowercase()?.ifBlank { null },
            denomination = denomination?.trim()?.lowercase()?.ifBlank { null },
            seriesName = seriesName?.trim()?.lowercase()?.ifBlank { null },
            year = year,
            mintMark = mintMark?.trim()?.lowercase()?.ifBlank { null },
        )

    fun toKeys(): FingerprintKeys {
        val normalizedCountry = CountryAliasMapping.normalize(countryOrIssuer)
        val normalizedDenom = DenominationAliasMapping.normalize(denomination)
        val retrievalKey = FingerprintKeys.from(normalizedCountry, normalizedDenom, year)
        val searchQuery = buildList {
            countryOrIssuer?.let { add(it) }
            denomination?.let { add(it) }
            year?.toString()?.let { add(it) }
        }.joinToString(" ")
        val hash = FingerprintKeys.hash(retrievalKey)
        return FingerprintKeys(
            retrievalKey = retrievalKey,
            searchQuery = searchQuery,
            hash = hash,
            version = 1,
        )
    }
}
