package com.vlatkogalev.domain.coin.model

enum class MatchTier { MATCHED, AMBIGUOUS, NO_MATCH }

data class MatchCandidate(
    val catalogCoin: CatalogCoin?,
    val matchableCoin: MatchableCoin,
    val providerName: String,
    val externalId: String?,
    val score: Int,
    val scoreBreakdown: Map<String, Int>,
)

data class MatchResult(
    val tier: MatchTier,
    val bestCandidate: MatchCandidate?,
    val allCandidates: List<MatchCandidate>,
    val fingerprintHash: String,
    val retrievalKey: String,
)
