package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.domain.coin.repository.EnrichmentAttemptsRepository
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.StructuredLogger
import java.time.Instant
import java.util.UUID

class CoinEnrichmentServiceImpl(
    private val catalogCoinRepository: CatalogCoinRepository,
    private val enrichmentAttemptsRepository: EnrichmentAttemptsRepository,
    private val coinRepository: CoinRepository,
    private val providers: List<CoinCatalogProvider>,
    private val matcher: CoinMatcher,
    private val nowProvider: () -> Instant = { Instant.now() },
) : CoinEnrichmentService {

    private val logger = StructuredLogger("CoinEnrichmentService")

    private val cooldownChecker = CooldownChecker(enrichmentAttemptsRepository)
    private val candidateFetcher = CandidateFetcher(catalogCoinRepository, providers)
    private val persister = EnrichmentPersister(catalogCoinRepository, enrichmentAttemptsRepository)

    override suspend fun getOrMatch(recognition: RecognitionResult): MatchResult {
        val keys = recognition.toFingerprint().toKeys()
        val now = nowProvider()
        MatchMetrics.attempts.incrementAndGet()

        val dbCoins = candidateFetcher.fetchDbCoins(recognition)

        when (val cooldown = cooldownChecker.check(keys.hash, now)) {
            is CooldownResult.Active -> {
                MatchMetrics.cacheHits.incrementAndGet()
                val dbCandidates = candidateFetcher.toDbCandidates(dbCoins)
                if (dbCandidates.isNotEmpty()) {
                    val result = matcher.match(recognition, dbCandidates)
                    return result.copy(fingerprintHash = keys.hash, retrievalKey = keys.retrievalKey)
                }
                val cachedTier = runCatching { MatchTier.valueOf(cooldown.lastResult) }
                    .getOrDefault(MatchTier.NO_MATCH)
                return MatchResult(
                    tier = cachedTier,
                    bestCandidate = null,
                    allCandidates = emptyList(),
                    fingerprintHash = keys.hash,
                    retrievalKey = keys.retrievalKey,
                )
            }
            CooldownResult.Expired -> Unit
        }

        MatchMetrics.numistaCalls.incrementAndGet()

        val dbCandidates = candidateFetcher.toDbCandidates(dbCoins)
        val numistaCandidates = candidateFetcher.fetchProviderCandidates(recognition)
        val allCandidates = candidateFetcher.deduplicate(dbCandidates + numistaCandidates)

        logger.info("match retrievalKey=${keys.retrievalKey} query=${keys.searchQuery} dbCandidates=${dbCandidates.size} numistaCandidates=${numistaCandidates.size} allCandidates=${allCandidates.size}")
        val result = matcher.match(recognition, allCandidates)
        val topScore = result.allCandidates.firstOrNull()?.score
        val secondScore = result.allCandidates.getOrNull(1)?.score
        logger.info("match retrievalKey=${keys.retrievalKey} tier=${result.tier} topScore=$topScore secondScore=$secondScore candidateCount=${result.allCandidates.size}")
        val finalResult = result.copy(fingerprintHash = keys.hash, retrievalKey = keys.retrievalKey)

        MatchMetrics.candidateCountSum.addAndGet(allCandidates.size.toLong())

        return persister.persist(recognition, finalResult, keys.hash, keys.retrievalKey, now)
    }

    override suspend fun enrichCoin(coinId: UUID, callerUserId: UUID): Result<MatchResult> {
        val coin = coinRepository.findById(coinId)
            ?: return Result.Failure(CoinError.NotFound())
        if (coin.userId != callerUserId) {
            return Result.Failure(CoinError.Unauthorized())
        }

        val matchResult = getOrMatch(coin.recognitionResult)

        if (matchResult.tier != MatchTier.NO_MATCH && coin.catalogCoinId == null) {
            matchResult.bestCandidate?.catalogCoin?.let { catalogCoin ->
                coinRepository.updateCatalogCoinId(coinId, catalogCoin.id)
            }
        }

        return Result.Success(matchResult)
    }
}
