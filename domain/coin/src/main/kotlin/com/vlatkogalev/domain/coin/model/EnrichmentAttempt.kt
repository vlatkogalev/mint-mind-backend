package com.vlatkogalev.domain.coin.model

import java.time.Instant

data class EnrichmentAttempt(
    val fingerprintHash: String,
    val retrievalKey: String,
    val lastAttemptAt: Instant,
    val lastResult: String,
)
