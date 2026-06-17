package com.vlatkogalev.domain.coin.service.signals

import com.vlatkogalev.domain.coin.model.DenominationAliasMapping
import com.vlatkogalev.domain.coin.model.MatchableCoin
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.MatchSignal

class DenominationSignal : MatchSignal {
    override val name = "denomination"

    override fun score(recognition: RecognitionResult, candidate: MatchableCoin): Int {
        val recDenom = DenominationAliasMapping.normalize(recognition.denomination)
        val candDenom = DenominationAliasMapping.normalize(candidate.denomination)
        return if (CountrySignal.looseMatch(recDenom, candDenom)) 100 else 0
    }
}
