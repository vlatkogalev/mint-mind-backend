package com.vlatkogalev.domain.coin.repository

import com.vlatkogalev.domain.coin.model.EnrichmentAttempt

interface EnrichmentAttemptsRepository {
    suspend fun findByHash(hash: String): EnrichmentAttempt?
    suspend fun upsert(hash: String, retrievalKey: String, result: String): EnrichmentAttempt
}
