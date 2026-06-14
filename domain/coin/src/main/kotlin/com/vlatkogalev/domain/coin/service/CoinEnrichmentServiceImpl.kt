package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.platform.core.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Duration
import java.time.Instant
import java.util.UUID

private const val RETRY_HOURS = 24L

class CoinEnrichmentServiceImpl(
    private val catalogCoinRepository: CatalogCoinRepository,
    private val providers: List<CoinCatalogProvider>,
    private val nowProvider: () -> Instant = { Instant.now() },
) : CoinEnrichmentService {

    override suspend fun getOrEnrich(fingerprint: CoinFingerprint): CatalogCoin? {
        val normalized = fingerprint.normalized()
        val existing = catalogCoinRepository.findByFingerprint(normalized)

        val catalogCoin = existing ?: run {
            val now = nowProvider()
            val stub = CatalogCoin(
                id = UUID.randomUUID(),
                fingerprint = normalized,
                enrichedAt = null,
                lastEnrichmentAttemptAt = null,
                lastEnrichmentFailedAt = null,
                lastEnrichmentError = null,
                createdAt = now,
                updatedAt = now,
            )
            catalogCoinRepository.save(stub)
        }

        if (catalogCoin.enrichedAt != null) return catalogCoin

        val failedAt = catalogCoin.lastEnrichmentFailedAt
        if (failedAt != null) {
            val hoursSinceFail = Duration.between(failedAt, nowProvider()).toHours()
            if (hoursSinceFail < RETRY_HOURS) return catalogCoin
        }

        return enrich(catalogCoin, normalized, nowProvider())
    }

    override suspend fun enrichById(catalogCoinId: UUID): Result<CatalogCoin> =
        try {
            val catalogCoin = catalogCoinRepository.findById(catalogCoinId)
                ?: return Result.Failure("Catalog coin not found")

            val enriched = enrich(catalogCoin, catalogCoin.fingerprint, nowProvider())
            if (enriched != null && enriched.enrichedAt != null) {
                Result.Success(enriched)
            } else {
                Result.Failure("Failed to enrich catalog coin")
            }
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to enrich catalog coin", ex)
        }

    private suspend fun enrich(
        catalogCoin: CatalogCoin,
        fingerprint: CoinFingerprint,
        now: Instant,
    ): CatalogCoin? {
        if (providers.isEmpty()) {
            return catalogCoinRepository.markEnrichmentFailed(catalogCoin.id, now, "No providers configured")
        }

        return coroutineScope {
            val providerResults = providers.map { provider ->
                async {
                    provider to provider.findCandidates(fingerprint)
                }
            }.map { it.await() }

            var bestCandidate: CoinCatalogCandidate? = null
            var bestScore = 0
            val errors = mutableListOf<String>()

            for ((provider, result) in providerResults) {
                when (result) {
                    is Result.Success -> {
                        for (candidate in result.value) {
                            val score = scoreCandidate(fingerprint, candidate)
                            if (score > bestScore) {
                                bestScore = score
                                bestCandidate = candidate
                            }
                        }
                    }
                    is Result.Failure -> {
                        errors.add("${provider.providerName}: ${result.reason}")
                    }
                }
            }

            val enrichmentResult = if (bestCandidate != null && bestScore > 0) {
                catalogCoinRepository.saveExternalReference(bestCandidate.externalReference)
                catalogCoinRepository.markEnrichmentSuccess(catalogCoin.id, now, bestCandidate)
            } else {
                catalogCoinRepository.markEnrichmentFailed(
                    catalogCoin.id,
                    now,
                    errors.joinToString("; ").ifBlank { "No matching candidates found" },
                )
            }
            enrichmentResult
        }
    }

    private fun scoreCandidate(fingerprint: CoinFingerprint, candidate: CoinCatalogCandidate): Int {
        var score = 0

        if (looseMatch(fingerprint.countryOrIssuer, candidate.countryOrIssuer)) score += 4
        if (looseMatch(fingerprint.denomination, candidate.denomination)) score += 3

        val year = fingerprint.year
        if (year != null) {
            val start = candidate.yearStart
            val end = candidate.yearEnd
            when {
                start != null && end != null && year in start..end -> score += 8
                start != null && end == null && year >= start -> score += 8
                start == null && end != null && year <= end -> score += 8
                else -> score -= 6
            }
        }

        return score
    }

    private fun looseMatch(a: String?, b: String?): Boolean {
        if (a == null || b == null) return false
        if (a.isBlank() || b.isBlank()) return false
        val s1 = a.trim().lowercase()
        val s2 = b.trim().lowercase()
        return s1.contains(s2) || s2.contains(s1)
    }
}
