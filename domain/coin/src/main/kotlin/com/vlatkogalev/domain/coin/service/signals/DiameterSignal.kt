package com.vlatkogalev.domain.coin.service.signals

import com.vlatkogalev.domain.coin.model.MatchableCoin
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.MatchSignal
import kotlin.math.abs

class DiameterSignal : MatchSignal {
    override val name = "diameter"

    override fun score(recognition: RecognitionResult, candidate: MatchableCoin): Int {
        val recDiam = recognition.diameterMm ?: return 0
        val candDiam = candidate.diameterMm ?: return 0
        return if (abs(recDiam - candDiam) <= 1.0) 40 else 0
    }
}
