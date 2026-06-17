package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.domain.coin.repository.EnrichmentAttemptsRepository
import com.vlatkogalev.platform.core.Result
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CoinEnrichmentServiceImplTest {
    @Test
    fun getOrMatch_returnsNoMatch_whenNoCandidatesAndEmptyResponse() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val provider = FakeProvider("numista", Result.Success(emptyList()))
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.getOrMatch(makeRecognitionResult()) }

        assertEquals(MatchTier.NO_MATCH, result.tier)
        assertEquals(1, provider.callCount)
    }

    @Test
    fun fingerprintNormalizedLowercasesFields() {
        val fingerprint = CoinFingerprint(
            countryOrIssuer = "United States",
            denomination = "1 Dollar",
            seriesName = "Morgan",
            year = 1921,
            mintMark = "S",
        )
        val normalized = fingerprint.normalized()
        assertEquals("united states", normalized.countryOrIssuer)
        assertEquals("1 dollar", normalized.denomination)
        assertEquals("morgan", normalized.seriesName)
        assertEquals("s", normalized.mintMark)
        assertEquals(1921, normalized.year)
    }

    @Test
    fun getOrMatch_hitsCooldownAndSkipsNumista() {
        val now = Instant.parse("2026-01-02T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val provider = FakeProvider("numista", Result.Failure("should not be called"))
        val svc = service(repository, listOf(provider), now)

        val first = runBlocking { svc.getOrMatch(makeRecognitionResult()) }
        assertEquals(MatchTier.NO_MATCH, first.tier)
        assertEquals(1, provider.callCount)

        val second = runBlocking { svc.getOrMatch(makeRecognitionResult()) }
        assertEquals(MatchTier.NO_MATCH, second.tier)
        assertEquals(1, provider.callCount)
    }

    @Test
    fun getOrMatch_cooldownReturnsScoredBestCandidateWhenDbCoinMatches() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val recognition = makeRecognitionResult()

        val dbCoin = CatalogCoin(
            id = UUID.randomUUID(),
            fingerprint = CoinFingerprint(
                countryOrIssuer = "united states",
                denomination = "1 dollar",
                seriesName = null,
                year = 1921,
                mintMark = null,
            ),
            enrichedAt = now,
            lastEnrichmentAttemptAt = now,
            lastEnrichmentFailedAt = null,
            lastEnrichmentError = null,
            createdAt = now,
            updatedAt = now,
        )
        runBlocking { repository.save(dbCoin) }

        val provider = FakeProvider("numista", Result.Success(emptyList()))
        val svc = service(repository, listOf(provider), now, matcher = ScoringMatcher())

        val first = runBlocking { svc.getOrMatch(recognition) }
        assertEquals(1, provider.callCount)

        val second = runBlocking { svc.getOrMatch(recognition) }
        assertEquals(1, provider.callCount)

        assertEquals(MatchTier.MATCHED, second.tier)
        assertEquals(100, second.bestCandidate?.score)
        assert(second.allCandidates.isNotEmpty())
    }

    private fun service(
        repository: InMemoryCatalogCoinRepository,
        providers: List<CoinCatalogProvider>,
        now: Instant,
        matcher: CoinMatcher = NoopMatcher(),
    ): CoinEnrichmentServiceImpl = CoinEnrichmentServiceImpl(
        catalogCoinRepository = repository,
        enrichmentAttemptsRepository = InMemoryEnrichmentAttemptsRepository(),
        coinRepository = FakeCoinRepository(),
        providers = providers,
        matcher = matcher,
        nowProvider = { now },
    )

    private fun makeRecognitionResult(): RecognitionResult =
        RecognitionResult(
            overallConfidence = Confidence.HIGH,
            countryOrIssuer = "United States",
            denomination = "1 Dollar",
            seriesName = null,
            year = 1921,
            mintMark = null,
            metalComposition = null,
            estimatedGrade = null,
            estimatedGradeValue = null,
            rarityQualitative = null,
            valueLow = null,
            valueHigh = null,
            mintage = null,
            obverseDescription = null,
            reverseDescription = null,
            historicalContext = null,
            rawJson = "{}",
        )
}

private class FakeProvider(
    override val providerName: String,
    private var response: Result<List<CoinCatalogCandidate>> = Result.Success(emptyList()),
) : CoinCatalogProvider {
    var callCount: Int = 0
        private set

    override suspend fun findCandidates(fingerprint: CoinFingerprint): Result<List<CoinCatalogCandidate>> {
        callCount++
        return response
    }
}

private class InMemoryCatalogCoinRepository : CatalogCoinRepository {
    private val coins = mutableMapOf<UUID, CatalogCoin>()
    private val references = mutableMapOf<Pair<UUID, String>, ExternalCoinReference>()

    override suspend fun findByFingerprint(fingerprint: CoinFingerprint): CatalogCoin? =
        coins.values.firstOrNull {
            it.fingerprint.countryOrIssuer == fingerprint.countryOrIssuer &&
                it.fingerprint.denomination == fingerprint.denomination &&
                it.fingerprint.year == fingerprint.year
        }

    override suspend fun findByRetrievalKey(country: String?, denomination: String?, year: Int?): List<CatalogCoin> =
        coins.values.filter {
            (country == null && it.fingerprint.countryOrIssuer == null || country != null && it.fingerprint.countryOrIssuer == country) &&
                (denomination == null && it.fingerprint.denomination == null || denomination != null && it.fingerprint.denomination == denomination) &&
                year == it.fingerprint.year
        }

    override suspend fun findById(id: UUID): CatalogCoin? = coins[id]

    override suspend fun findByProviderExternalId(provider: String, externalId: String): CatalogCoin? {
        val reference = references.values.firstOrNull { it.provider == provider && it.externalId == externalId } ?: return null
        return coins[reference.catalogCoinId]
    }

    override suspend fun save(catalogCoin: CatalogCoin): CatalogCoin {
        coins[catalogCoin.id] = catalogCoin
        return catalogCoin
    }

    override suspend fun markEnrichmentSuccess(
        catalogCoinId: UUID,
        now: Instant,
        candidate: CoinCatalogCandidate?,
    ): CatalogCoin? {
        val current = coins[catalogCoinId] ?: return null
        val updated = current.copy(
            enrichedAt = now,
            lastEnrichmentAttemptAt = now,
            lastEnrichmentFailedAt = null,
            lastEnrichmentError = null,
            title = current.title ?: candidate?.title,
            composition = current.composition ?: candidate?.composition,
            weightGrams = current.weightGrams ?: candidate?.weightGrams,
            diameterMm = current.diameterMm ?: candidate?.diameterMm,
            obverseDescription = current.obverseDescription ?: candidate?.obverseDescription,
            reverseDescription = current.reverseDescription ?: candidate?.reverseDescription,
            historicalContext = current.historicalContext ?: candidate?.historicalContext,
            thumbnailUrl = current.thumbnailUrl ?: candidate?.thumbnailUrl,
            numistaUrl = current.numistaUrl ?: candidate?.numistaUrl,
            updatedAt = now,
        )
        coins[catalogCoinId] = updated
        candidate?.let { c ->
            val oldKey = c.externalReference.catalogCoinId to c.externalReference.provider
            val newKey = catalogCoinId to c.externalReference.provider
            val ref = references.remove(oldKey) ?: references[oldKey]
            if (ref != null) {
                references[newKey] = ref.copy(catalogCoinId = catalogCoinId)
            }
        }
        return updated
    }

    override suspend fun markEnrichmentFailed(catalogCoinId: UUID, now: Instant, error: String?): CatalogCoin? {
        val current = coins[catalogCoinId] ?: return null
        val updated = current.copy(
            lastEnrichmentAttemptAt = now,
            lastEnrichmentFailedAt = now,
            lastEnrichmentError = error,
            updatedAt = now,
        )
        coins[catalogCoinId] = updated
        return updated
    }

    override suspend fun saveExternalReference(reference: ExternalCoinReference): ExternalCoinReference {
        references[reference.catalogCoinId to reference.provider] = reference
        return reference
    }

    override suspend fun findExternalReference(catalogCoinId: UUID, provider: String): ExternalCoinReference? =
        references[catalogCoinId to provider]

    override suspend fun findOrCreateExternalReference(
        provider: String,
        externalId: String,
        catalogCoin: CatalogCoin,
        now: Instant,
    ): CatalogCoin {
        references[catalogCoin.id to provider] = ExternalCoinReference(
            id = UUID.randomUUID(),
            catalogCoinId = catalogCoin.id,
            provider = provider,
            externalId = externalId,
            externalUrl = null,
            lastSyncedAt = now,
            syncStatus = "synced",
            syncError = null,
            createdAt = now,
        )
        return catalogCoin
    }
}

private class InMemoryEnrichmentAttemptsRepository : EnrichmentAttemptsRepository {
    private val attempts = mutableMapOf<String, EnrichmentAttempt>()

    override suspend fun findByHash(hash: String): EnrichmentAttempt? =
        attempts[hash]

    override suspend fun upsert(hash: String, retrievalKey: String, result: String): EnrichmentAttempt {
        val attempt = EnrichmentAttempt(
            fingerprintHash = hash,
            retrievalKey = retrievalKey,
            lastAttemptAt = Instant.now(),
            lastResult = result,
        )
        attempts[hash] = attempt
        return attempt
    }
}

private class NoopMatcher : CoinMatcher {
    override fun match(
        recognition: RecognitionResult,
        candidates: List<MatchCandidate>,
    ): MatchResult =
        MatchResult(
            tier = MatchTier.NO_MATCH,
            bestCandidate = null,
            allCandidates = emptyList(),
            fingerprintHash = "",
            retrievalKey = "",
        )
}

private class ScoringMatcher : CoinMatcher {
    override fun match(
        recognition: RecognitionResult,
        candidates: List<MatchCandidate>,
    ): MatchResult {
        if (candidates.isEmpty()) return MatchResult(
            MatchTier.NO_MATCH, null, emptyList(), "", ""
        )
        val scored = candidates.map { candidate ->
            val countryMatch = CountryAliasMapping.normalize(recognition.countryOrIssuer) ==
                CountryAliasMapping.normalize(candidate.matchableCoin.countryOrIssuer)
            candidate.copy(score = if (countryMatch) 100 else 0, scoreBreakdown = mapOf("country" to (if (countryMatch) 100 else 0)))
        }.filter { it.score > 0 }.sortedByDescending { it.score }
        if (scored.isEmpty()) return MatchResult(MatchTier.NO_MATCH, null, emptyList(), "", "")
        return MatchResult(MatchTier.MATCHED, scored.first(), scored, "", "")
    }
}
