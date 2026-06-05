package com.vlatkogalev.domain.coin.model

data class CoinFingerprint(
    val countryOrIssuer: String?,
    val denomination: String?,
    val seriesName: String?,
    val title: String?,
    val year: Int?,
    val mintMark: String?,
) {
    fun normalized(): CoinFingerprint =
        CoinFingerprint(
            countryOrIssuer = countryOrIssuer?.trim()?.ifBlank { null },
            denomination = denomination?.trim()?.ifBlank { null },
            seriesName = seriesName?.trim()?.ifBlank { null },
            title = title?.trim()?.ifBlank { null },
            year = year,
            mintMark = mintMark?.trim()?.ifBlank { null },
        )
}
