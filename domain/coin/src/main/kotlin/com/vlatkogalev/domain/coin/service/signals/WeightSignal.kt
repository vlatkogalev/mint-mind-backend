package com.vlatkogalev.domain.coin.service.signals

import com.vlatkogalev.domain.coin.model.MatchableCoin
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.MatchSignal
import kotlin.math.abs

class WeightSignal : MatchSignal {
    override val name = "weight"

    override fun score(recognition: RecognitionResult, candidate: MatchableCoin): Int {
        val recWeight = recognition.weightGrams ?: return 0
        val candWeight = candidate.weightGrams ?: return 0
        val tolerance = maxOf(0.1, 0.05 * candWeight)
        return if (abs(recWeight - candWeight) <= tolerance) 40 else 0
    }
}
