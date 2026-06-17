package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.MatchCandidate
import com.vlatkogalev.domain.coin.model.MatchResult
import com.vlatkogalev.domain.coin.model.RecognitionResult

interface CoinMatcher {
    fun match(
        recognition: RecognitionResult,
        candidates: List<MatchCandidate>,
    ): MatchResult
}
