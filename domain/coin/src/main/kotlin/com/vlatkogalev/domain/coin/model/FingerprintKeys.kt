package com.vlatkogalev.domain.coin.model

import java.security.MessageDigest

data class FingerprintKeys(
    val retrievalKey: String,
    val searchQuery: String,
    val hash: String,
    val version: Int = 1,
) {
    companion object {
        fun from(countryOrIssuer: String?, normalizedDenomination: String?, year: Int?): String {
            val country = countryOrIssuer?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: ""
            val denomination = normalizedDenomination?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: ""
            val yearStr = year?.toString() ?: ""
            return "$country|$denomination|$yearStr"
        }

        fun hash(retrievalKey: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(retrievalKey.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
