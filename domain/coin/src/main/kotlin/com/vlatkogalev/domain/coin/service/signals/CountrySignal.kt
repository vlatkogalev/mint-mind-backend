package com.vlatkogalev.domain.coin.service.signals

import com.vlatkogalev.domain.coin.model.CountryAliasMapping
import com.vlatkogalev.domain.coin.model.MatchableCoin
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.MatchSignal

class CountrySignal : MatchSignal {
    override val name = "country"

    override fun score(recognition: RecognitionResult, candidate: MatchableCoin): Int {
        val recCountry = CountryAliasMapping.normalize(recognition.countryOrIssuer)
        val candCountry = CountryAliasMapping.normalize(candidate.countryOrIssuer)
        return if (looseMatch(recCountry, candCountry)) 100 else 0
    }

    companion object {
        internal fun looseMatch(a: String?, b: String?): Boolean {
            if (a == null || b == null) return false
            if (a.isBlank() || b.isBlank()) return false
            val s1 = a.trim().lowercase()
            val s2 = b.trim().lowercase()
            return s1.contains(s2) || s2.contains(s1)
        }
    }
}
