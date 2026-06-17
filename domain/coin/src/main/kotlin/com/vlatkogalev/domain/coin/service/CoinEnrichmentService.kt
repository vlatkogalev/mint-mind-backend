package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.MatchResult
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.platform.core.Result
import java.util.UUID

interface CoinEnrichmentService {
    suspend fun getOrMatch(recognition: RecognitionResult): MatchResult
    suspend fun enrichCoin(coinId: UUID, callerUserId: UUID): Result<MatchResult>
}

interface CoinCatalogProvider {
    val providerName: String
    suspend fun findCandidates(fingerprint: CoinFingerprint): Result<List<CoinCatalogCandidate>>
}
