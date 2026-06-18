package com.vlatkogalev.app.jobs

import com.vlatkogalev.domain.coin.model.MatchTier
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.domain.coin.service.CoinEnrichmentService
import com.vlatkogalev.platform.core.StructuredLogger
import java.util.UUID
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class CoinEnrichmentQueue(
    private val enrichmentService: CoinEnrichmentService,
    private val coinRepository: CoinRepository,
    private val maxConcurrency: Int = 4,
) {
    private val logger = StructuredLogger("CoinEnrichmentQueue")

    private val semaphore = Semaphore(permits = maxConcurrency)

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(
        supervisorJob + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            logger.error("Unhandled enrichment error", throwable = e)
        }
    )

    fun enqueue(coinId: UUID, recognition: RecognitionResult) {
        scope.launch {
            semaphore.withPermit {
                try {
                    val matchResult = enrichmentService.getOrMatch(recognition)
                    val catalogCoinId = when (matchResult.tier) {
                        MatchTier.MATCHED, MatchTier.AMBIGUOUS ->
                            matchResult.bestCandidate?.catalogCoin?.id
                        MatchTier.NO_MATCH -> null
                    }
                    if (catalogCoinId != null) {
                        coinRepository.updateCatalogCoinId(coinId, catalogCoinId)
                    }
                } catch (e: Exception) {
                    logger.error("Enrichment failed", mapOf("coinId" to coinId), e)
                }
            }
        }
    }

    fun shutdown() = supervisorJob.cancel()
}
