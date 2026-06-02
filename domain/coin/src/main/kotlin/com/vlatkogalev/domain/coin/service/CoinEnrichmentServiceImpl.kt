package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.normalized
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.platform.core.Result
import java.time.Duration
import java.time.Instant
import java.util.UUID

class CoinEnrichmentServiceImpl(
    private val catalogCoinRepository: CatalogCoinRepository,
    private val providers: List<CoinCatalogProvider>,
    private val retryWindow: Duration = Duration.ofHours(24),
    private val nowProvider: () -> Instant = Instant::now,
) : CoinEnrichmentService {
    override fun getOrEnrich(fingerprint: CoinFingerprint): CatalogCoin? {
        val normalized = fingerprint.normalized()
        val now = nowProvider()

        val catalogCoin = catalogCoinRepository.findByFingerprint(normalized)
            ?: createStub(normalized, now)
            ?: return null

        if (catalogCoin.enrichedAt != null) return catalogCoin

        val failedAt = catalogCoin.lastEnrichmentFailedAt
        if (failedAt != null && failedAt.plus(retryWindow).isAfter(now)) {
            return catalogCoin
        }

        return enrich(catalogCoin, normalized, now)
    }

    override fun enrichById(catalogCoinId: UUID): Result<CatalogCoin> {
        val catalogCoin = catalogCoinRepository.findById(catalogCoinId)
            ?: return Result.Failure("Catalog coin not found")
        val enriched = enrich(catalogCoin, catalogCoin.fingerprint.normalized(), nowProvider())
        if (enriched.enrichedAt == null) {
            return Result.Failure(enriched.lastEnrichmentError ?: "No viable provider candidate")
        }
        return Result.Success(enriched)
    }

    private fun createStub(fingerprint: CoinFingerprint, now: Instant): CatalogCoin? =
        catalogCoinRepository.save(
            CatalogCoin(
                id = UUID.randomUUID(),
                fingerprint = fingerprint,
                enrichedAt = null,
                lastEnrichmentAttemptAt = null,
                lastEnrichmentFailedAt = null,
                lastEnrichmentError = null,
                createdAt = now,
                updatedAt = now,
            ),
        )

    private fun enrich(catalogCoin: CatalogCoin, fingerprint: CoinFingerprint, now: Instant): CatalogCoin {
        val providerErrors = mutableListOf<String>()

        for (provider in providers) {
            when (val result = provider.findCandidates(fingerprint)) {
                is Result.Success -> {
                    val winner = result.value
                        .asSequence()
                        .map { it to scoreCandidate(fingerprint, it) }
                        .filter { (_, score) -> score > 0 }
                        .maxByOrNull { (_, score) -> score }
                        ?.first
                    if (winner != null) {
                        catalogCoinRepository.saveExternalReference(
                            winner.externalReference.copy(
                                catalogCoinId = catalogCoin.id,
                                lastSyncedAt = now,
                                syncStatus = "success",
                                syncError = null,
                            ),
                        )
                        return catalogCoinRepository.markEnrichmentSuccess(catalogCoin.id, now) ?: catalogCoin
                    }
                }

                is Result.Failure -> providerErrors += "${provider.providerName}: ${result.reason}"
            }
        }

        val reason = providerErrors
            .ifEmpty { listOf("No viable provider candidate") }
            .joinToString(" | ")
            .take(1000)
        return catalogCoinRepository.markEnrichmentFailed(catalogCoin.id, now, reason) ?: catalogCoin
    }

    private fun scoreCandidate(fingerprint: CoinFingerprint, candidate: CoinCatalogCandidate): Int {
        var score = 0

        if (fingerprint.countryOrIssuer.matchesLoose(candidate.countryOrIssuer)) score += 4
        if (fingerprint.denomination.matchesLoose(candidate.denomination)) score += 3
        if (fingerprint.title.matchesLoose(candidate.title)) score += 5

        if (fingerprint.year != null) {
            val inRange = candidate.isYearInRange(fingerprint.year)
            if (inRange == true) score += 8
            else if (inRange == false) score -= 6
        }

        return score
    }

    private fun String?.matchesLoose(other: String?): Boolean {
        val left = this?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return false
        val right = other?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return false
        return left == right || left in right || right in left
    }

    private fun CoinCatalogCandidate.isYearInRange(year: Int): Boolean? {
        val start = yearStart
        val end = yearEnd
        if (start == null && end == null) return null
        val lower = start ?: Int.MIN_VALUE
        val upper = end ?: Int.MAX_VALUE
        return year in lower..upper
    }
}
