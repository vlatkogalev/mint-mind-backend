package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.MatchableCoin
import com.vlatkogalev.domain.coin.model.RecognitionResult

interface MatchSignal {
    val name: String
    fun score(recognition: RecognitionResult, candidate: MatchableCoin): Int
}
