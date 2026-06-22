package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.model.MatchMetrics
import com.vlatkogalev.domain.coin.model.MatchResult
import com.vlatkogalev.domain.coin.model.MatchTier
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.model.ConfidenceConfig
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.domain.coin.repository.EnrichmentAttemptsRepository
import java.time.Instant
import java.util.UUID

class EnrichmentPersister(
    private val catalogCoinRepository: CatalogCoinRepository,
    private val enrichmentAttemptsRepository: EnrichmentAttemptsRepository,
) {
    suspend fun persist(
        recognition: RecognitionResult,
        result: MatchResult,
        fingerprintHash: String,
        retrievalKey: String,
        now: Instant,
    ): MatchResult {
        var finalResult = result

        when (finalResult.tier) {
            MatchTier.MATCHED, MatchTier.AMBIGUOUS -> {
                if (finalResult.tier == MatchTier.MATCHED) MatchMetrics.matched.incrementAndGet()
                else MatchMetrics.ambiguous.incrementAndGet()
                val best = finalResult.bestCandidate!!
                val linkedCoin: CatalogCoin = if (best.catalogCoin != null) {
                    catalogCoinRepository.markEnrichmentSuccess(
                        best.catalogCoin.id, now,
                        best.catalogCandidate?.copy(
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
                        ),
                    )
                    best.catalogCoin
                } else {
                    val candidate = best.catalogCandidate
                    val newCoin = CatalogCoin(
                        id = UUID.randomUUID(),
                        fingerprint = recognition.toFingerprint(),
                        title = candidate?.title,
                        composition = candidate?.composition ?: best.matchableCoin.composition,
                        weightGrams = candidate?.weightGrams ?: best.matchableCoin.weightGrams,
                        diameterMm = candidate?.diameterMm ?: best.matchableCoin.diameterMm,
                        obverseDescription = candidate?.obverseDescription,
                        reverseDescription = candidate?.reverseDescription,
                        historicalContext = candidate?.historicalContext,
                        thumbnailUrl = candidate?.thumbnailUrl,
                        numistaUrl = candidate?.numistaUrl,
                        minYear = candidate?.yearStart,
                        maxYear = candidate?.yearEnd,
                        thicknessMm = candidate?.thicknessMm,
                        shape = candidate?.shape,
                        technique = candidate?.technique,
                        orientation = candidate?.orientation,
                        edgeDescription = candidate?.edgeDescription,
                        obverseLettering = candidate?.obverseLettering,
                        reverseLettering = candidate?.reverseLettering,
                        obverseDesigners = candidate?.obverseDesigners ?: emptyList(),
                        reverseDesigners = candidate?.reverseDesigners ?: emptyList(),
                        obversePictureUrl = candidate?.obversePictureUrl,
                        reversePictureUrl = candidate?.reversePictureUrl,
                        reverseThumbnailUrl = candidate?.reverseThumbnailUrl,
                        objectType = candidate?.objectType,
                        demonetized = candidate?.demonetized,
                        ruler = candidate?.ruler,
                        mintName = candidate?.mintName,
                        tags = candidate?.tags ?: emptyList(),
                        catalogReferences = candidate?.catalogReferences ?: emptyList(),
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

        enrichmentAttemptsRepository.upsert(
            fingerprintHash, retrievalKey, finalResult.tier.name, ConfidenceConfig.PIPELINE_VERSION
        )

        return finalResult
    }
}
