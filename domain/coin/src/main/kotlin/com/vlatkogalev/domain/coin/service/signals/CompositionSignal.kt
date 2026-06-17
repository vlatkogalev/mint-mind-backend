package com.vlatkogalev.domain.coin.service.signals

import com.vlatkogalev.domain.coin.model.MatchableCoin
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.MatchSignal

class CompositionSignal : MatchSignal {
    override val name = "composition"

    override fun score(recognition: RecognitionResult, candidate: MatchableCoin): Int {
        val recComp = recognition.metalComposition
        val candComp = candidate.composition
        return if (CountrySignal.looseMatch(recComp, candComp)) 40 else 0
    }
}
