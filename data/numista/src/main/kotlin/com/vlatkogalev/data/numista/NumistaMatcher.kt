package com.vlatkogalev.data.numista

import com.vlatkogalev.domain.coin.model.ConfidenceConfig
import com.vlatkogalev.domain.coin.model.MatchCandidate
import com.vlatkogalev.domain.coin.model.MatchResult
import com.vlatkogalev.domain.coin.model.MatchTier
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.CoinMatcher
import com.vlatkogalev.domain.coin.service.MatchSignal

class NumistaMatcher(
    private val signals: List<MatchSignal>,
) : CoinMatcher {

    override fun match(
        recognition: RecognitionResult,
        candidates: List<MatchCandidate>,
    ): MatchResult {
        if (candidates.isEmpty()) {
            return MatchResult(
                tier = MatchTier.NO_MATCH,
                bestCandidate = null,
                allCandidates = emptyList(),
                fingerprintHash = "",
                retrievalKey = "",
            )
        }

        val scored = candidates.map { candidate ->
            val breakdown = mutableMapOf<String, Int>()
            var total = 0
            for (signal in signals) {
                val s = signal.score(recognition, candidate.matchableCoin)
                breakdown[signal.name] = s
                total += s
            }
            candidate.copy(score = total, scoreBreakdown = breakdown)
        }.filter { it.score > 0 }
            .sortedByDescending { it.score }

        if (scored.isEmpty()) {
            return MatchResult(
                tier = MatchTier.NO_MATCH,
                bestCandidate = null,
                allCandidates = emptyList(),
                fingerprintHash = "",
                retrievalKey = "",
            )
        }

        val best = scored.first()
        val tier = determineTier(scored)

        return MatchResult(
            tier = tier,
            bestCandidate = best,
            allCandidates = scored,
            fingerprintHash = "",
            retrievalKey = "",
        )
    }

    private fun determineTier(scored: List<MatchCandidate>): MatchTier {
        val best = scored.first()
        val score = best.score
        val breakdown = best.scoreBreakdown

        val hasIdentityMatch =
            (breakdown["country"] ?: 0) > 0 &&
            (breakdown["denomination"] ?: 0) > 0 &&
            (breakdown["year"] ?: 0) > 0

        if (score >= ConfidenceConfig.MATCHED_MIN_SCORE) {
            if (!hasIdentityMatch) return MatchTier.AMBIGUOUS
            if (scored.size > 1) {
                val second = scored[1].score
                if (best.score - second < ConfidenceConfig.TOP_TWO_GAP) {
                    return MatchTier.AMBIGUOUS
                }
            }
            return MatchTier.MATCHED
        }

        if (score >= ConfidenceConfig.AMBIGUOUS_MIN_SCORE) {
            return MatchTier.AMBIGUOUS
        }

        return MatchTier.NO_MATCH
    }
}
