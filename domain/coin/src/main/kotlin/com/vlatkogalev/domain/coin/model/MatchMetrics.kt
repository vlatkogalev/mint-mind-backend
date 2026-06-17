package com.vlatkogalev.domain.coin.model

import java.util.concurrent.atomic.AtomicLong

object MatchMetrics {
    val attempts = AtomicLong(0)
    val matched = AtomicLong(0)
    val ambiguous = AtomicLong(0)
    val noMatch = AtomicLong(0)
    val numistaCalls = AtomicLong(0)
    val cacheHits = AtomicLong(0)
    val candidateCountSum = AtomicLong(0)

    fun snapshot(): MetricsSnapshot = MetricsSnapshot(
        attemptsTotal = attempts.get(),
        matchedTotal = matched.get(),
        ambiguousTotal = ambiguous.get(),
        noMatchTotal = noMatch.get(),
        numistaCallsTotal = numistaCalls.get(),
        cacheHitsTotal = cacheHits.get(),
        avgCandidates = if (attempts.get() > 0)
            candidateCountSum.get().toDouble() / attempts.get().toDouble()
        else 0.0,
    )
}

data class MetricsSnapshot(
    val attemptsTotal: Long,
    val matchedTotal: Long,
    val ambiguousTotal: Long,
    val noMatchTotal: Long,
    val numistaCallsTotal: Long,
    val cacheHitsTotal: Long,
    val avgCandidates: Double,
)
