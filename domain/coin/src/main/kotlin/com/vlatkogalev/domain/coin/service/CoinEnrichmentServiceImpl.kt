package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.domain.coin.repository.EnrichmentAttemptsRepository
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.StructuredLogger
import java.time.Duration
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

    override suspend fun getOrMatch(recognition: RecognitionResult): MatchResult {
        val keys = recognition.toFingerprint().toKeys()
        val now = nowProvider()
        MatchMetrics.attempts.incrementAndGet()

        val existingAttempt = enrichmentAttemptsRepository.findByHash(keys.hash)
        val cooldownActive = existingAttempt != null &&
            existingAttempt.pipelineVersion == ConfidenceConfig.PIPELINE_VERSION &&
            Duration.between(existingAttempt.lastAttemptAt, now).toHours() < ConfidenceConfig.COOLDOWN_HOURS

        val dbCoins = catalogCoinRepository.findByRetrievalKey(
            CountryAliasMapping.normalize(recognition.countryOrIssuer),
            DenominationAliasMapping.normalize(recognition.denomination),
            recognition.year,
        )

        if (cooldownActive) {
            MatchMetrics.cacheHits.incrementAndGet()
            if (dbCoins.isNotEmpty()) {
                val candidates = dbCoins
                    .filter { it.enrichedAt != null }
                    .map { coin ->
                    MatchCandidate(
                        catalogCoin = coin,
                        matchableCoin = coin.toMatchableCoin(),
                        providerName = "Numista",
                        externalId = null,
                        score = 0,
                        scoreBreakdown = emptyMap(),
                    )
                }
                val result = matcher.match(recognition, candidates)
                return result.copy(fingerprintHash = keys.hash, retrievalKey = keys.retrievalKey)
            }
            val cachedTier = runCatching { MatchTier.valueOf(existingAttempt.lastResult) }
                .getOrDefault(MatchTier.NO_MATCH)
            return MatchResult(
                tier = cachedTier,
                bestCandidate = null,
                allCandidates = emptyList(),
                fingerprintHash = keys.hash,
                retrievalKey = keys.retrievalKey,
            )
        }

        MatchMetrics.numistaCalls.incrementAndGet()

        val dbCandidates = dbCoins
            .filter { it.enrichedAt != null }
            .map { coin ->
            MatchCandidate(
                catalogCoin = coin,
                matchableCoin = coin.toMatchableCoin(),
                providerName = "Numista",
                externalId = null,
                score = 0,
                scoreBreakdown = emptyMap(),
            )
        }

        val numistaCandidates = providers.flatMap { provider ->
            when (val result = provider.findCandidates(recognition.toFingerprint())) {
                is Result.Success -> result.value
                is Result.Failure -> emptyList()
            }
        }.map { candidate ->
            val existingCatalogCoin = catalogCoinRepository.findByProviderExternalId(
                "Numista", candidate.externalReference.externalId
            )
            val matchableCoin = MatchableCoin(
                countryOrIssuer = candidate.countryOrIssuer,
                denomination = candidate.denomination,
                yearStart = candidate.yearStart,
                yearEnd = candidate.yearEnd,
                composition = candidate.composition,
                weightGrams = candidate.weightGrams,
                diameterMm = candidate.diameterMm,
                obverseLettering = candidate.obverseLettering,
                reverseLettering = candidate.reverseLettering,
                designers = candidate.designers,
            )
            MatchCandidate(
                catalogCoin = existingCatalogCoin,
                matchableCoin = matchableCoin,
                providerName = "Numista",
                externalId = candidate.externalReference.externalId,
                score = 0,
                scoreBreakdown = emptyMap(),
            )
        }

        val allCandidates = (dbCandidates + numistaCandidates)
            .distinctBy { candidate ->
                when {
                    candidate.externalId != null -> "${candidate.providerName}:${candidate.externalId}"
                    candidate.catalogCoin?.id != null -> "catalog:${candidate.catalogCoin.id}"
                    else -> "candidate:${candidate.matchableCoin.countryOrIssuer}|${candidate.matchableCoin.denomination}|${candidate.matchableCoin.yearStart}"
                }
            }
        logger.info("match retrievalKey=${keys.retrievalKey} query=${keys.searchQuery} dbCandidates=${dbCandidates.size} numistaCandidates=${numistaCandidates.size} allCandidates=${allCandidates.size}")
        val result = matcher.match(recognition, allCandidates)
        val topScore = result.allCandidates.firstOrNull()?.score
        val secondScore = result.allCandidates.getOrNull(1)?.score
        logger.info("match retrievalKey=${keys.retrievalKey} tier=${result.tier} topScore=$topScore secondScore=$secondScore candidateCount=${result.allCandidates.size}")
        var finalResult = result.copy(fingerprintHash = keys.hash, retrievalKey = keys.retrievalKey)

        MatchMetrics.candidateCountSum.addAndGet(allCandidates.size.toLong())

        when (finalResult.tier) {
            MatchTier.MATCHED, MatchTier.AMBIGUOUS -> {
                if (finalResult.tier == MatchTier.MATCHED) MatchMetrics.matched.incrementAndGet()
                else MatchMetrics.ambiguous.incrementAndGet()
                val best = finalResult.bestCandidate!!
                val linkedCoin: CatalogCoin = if (best.catalogCoin != null) {
                    catalogCoinRepository.markEnrichmentSuccess(
                        best.catalogCoin.id, now,
                        CoinCatalogCandidate(
                            externalReference = ExternalCoinReference(
                                id = UUID.randomUUID(),
                                catalogCoinId = best.catalogCoin.id,
                                provider = best.providerName,
                                externalId = best.externalId ?: "",
                                externalUrl = null,
                                lastSyncedAt = now,
                                syncStatus = "synced",
                                syncError = null,
                                createdAt = now,
                            ),
                            title = null,
                            countryOrIssuer = best.matchableCoin.countryOrIssuer,
                            denomination = best.matchableCoin.denomination,
                            yearStart = best.matchableCoin.yearStart,
                            yearEnd = best.matchableCoin.yearEnd,
                            composition = best.matchableCoin.composition,
                            weightGrams = best.matchableCoin.weightGrams,
                            diameterMm = best.matchableCoin.diameterMm,
                        )
                    )
                    best.catalogCoin
                } else {
                    val newCoin = CatalogCoin(
                        id = UUID.randomUUID(),
                        fingerprint = recognition.toFingerprint(),
                        title = null,
                        composition = best.matchableCoin.composition,
                        weightGrams = best.matchableCoin.weightGrams,
                        diameterMm = best.matchableCoin.diameterMm,
                        obverseDescription = null,
                        reverseDescription = null,
                        historicalContext = null,
                        thumbnailUrl = null,
                        numistaUrl = null,
                        enrichedAt = now,
                        lastEnrichmentAttemptAt = now,
                        lastEnrichmentFailedAt = null,
                        lastEnrichmentError = null,
                        createdAt = now,
                        updatedAt = now,
                    )
                    catalogCoinRepository.save(newCoin)
                    if (best.externalId != null) {
                        catalogCoinRepository.findOrCreateExternalReference(
                            provider = best.providerName,
                            externalId = best.externalId,
                            catalogCoin = newCoin,
                            now = now,
                        )
                    }
                    newCoin
                }
                finalResult = finalResult.copy(
                    bestCandidate = best.copy(catalogCoin = linkedCoin)
                )
            }
            MatchTier.NO_MATCH -> MatchMetrics.noMatch.incrementAndGet()
        }

        check(
            (finalResult.tier != MatchTier.MATCHED && finalResult.tier != MatchTier.AMBIGUOUS) ||
            finalResult.bestCandidate?.catalogCoin != null
        ) {
            "MATCHED or AMBIGUOUS result must have a linked CatalogCoin"
        }

        enrichmentAttemptsRepository.upsert(
            keys.hash, keys.retrievalKey, finalResult.tier.name, ConfidenceConfig.PIPELINE_VERSION
        )

        return finalResult
    }

    override suspend fun enrichCoin(coinId: UUID, callerUserId: UUID): Result<MatchResult> {
        val coin = coinRepository.findById(coinId)
            ?: return Result.Failure("Coin not found")
        if (coin.userId != callerUserId) {
            return Result.Failure("Unauthorized")
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
