package com.vlatkogalev.domain.coin.service.signals

import com.vlatkogalev.domain.coin.model.MatchableCoin
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.MatchSignal

class YearSignal : MatchSignal {
    override val name = "year"

    override fun score(recognition: RecognitionResult, candidate: MatchableCoin): Int {
        val year = recognition.year ?: return 0
        val start = candidate.yearStart
        val end = candidate.yearEnd
        return when {
            start != null && end != null && year in start..end -> 100
            start != null && end == null && year >= start -> 100
            start == null && end != null && year <= end -> 100
            else -> 0
        }
    }
}
