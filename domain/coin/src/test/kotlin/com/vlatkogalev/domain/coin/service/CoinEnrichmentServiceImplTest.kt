package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.platform.core.Result
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoinEnrichmentServiceImplTest {
    @Test
    fun getOrEnrich_returnsCachedCoinWithoutProviderCalls_whenAlreadyEnriched() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        val existing = runBlocking { repository.save(stubCoin(fingerprint, now).copy(enrichedAt = now)) }
        val provider = FakeProvider("numista")
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.getOrEnrich(fingerprint) }

        assertEquals(existing.id, result?.id)
        assertEquals(0, provider.callCount)
    }

    @Test
    fun getOrEnrich_returnsRecentFailureWithoutProviderCalls() {
        val now = Instant.parse("2026-01-02T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        runBlocking { repository.save(stubCoin(fingerprint, now).copy(lastEnrichmentFailedAt = now.minus(Duration.ofHours(1)))) }
        val provider = FakeProvider("numista")
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.getOrEnrich(fingerprint) }

        assertNotNull(result)
        assertEquals(0, provider.callCount)
    }

    @Test
    fun getOrEnrich_createsStubAndMarksFailure_whenNoProviderMatch() {
        val now = Instant.parse("2026-01-03T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        val provider = FakeProvider("numista")
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.getOrEnrich(fingerprint) }

        assertNotNull(result)
        assertNull(result.enrichedAt)
        assertNotNull(result.lastEnrichmentFailedAt)
        assertEquals(1, provider.callCount)
    }

    @Test
    fun getOrEnrich_ignoresZeroScoreAndPicksPositiveYearMatch() {
        val now = Instant.parse("2026-01-04T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        val candidateNoYear = candidate("1", yearStart = null, yearEnd = null, title = "Other coin")
        val candidateWithYear = candidate("2", yearStart = 1921, yearEnd = 1921, title = "Morgan Dollar")
        val provider = FakeProvider("numista", Result.Success(listOf(candidateNoYear, candidateWithYear)))
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.getOrEnrich(fingerprint) }
        val reference = result?.id?.let { runBlocking { repository.findExternalReference(it, "numista") } }

        assertNotNull(result)
        assertNotNull(result.enrichedAt)
        assertNotNull(reference)
        assertEquals("2", reference.externalId)
    }

    @Test
    fun getOrEnrich_marksFailure_whenProviderErrors() {
        val now = Instant.parse("2026-01-05T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        val provider = FakeProvider("numista", Result.Failure("provider down"))
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.getOrEnrich(fingerprint) }

        assertNotNull(result)
        assertNull(result.enrichedAt)
        assertTrue(result.lastEnrichmentError?.contains("provider down") == true)
    }

    @Test
    fun getOrEnrich_secondCallAfterSuccess_skipsProvider() {
        val now = Instant.parse("2026-01-06T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        val provider = FakeProvider(
            "numista",
            Result.Success(
                listOf(candidate("2", yearStart = 1921, yearEnd = 1921, title = "Morgan Dollar")),
            ),
        )
        val service = service(repository, listOf(provider), now)

        val first = runBlocking { service.getOrEnrich(fingerprint) }
        val second = runBlocking { service.getOrEnrich(fingerprint) }

        assertNotNull(first)
        assertNotNull(second)
        assertNotNull(second.enrichedAt)
        assertEquals(1, provider.callCount)
    }

    @Test
    fun getOrEnrich_marksFailure_whenCandidatesHaveNoPositiveScore() {
        val now = Instant.parse("2026-01-07T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        val provider = FakeProvider(
            "numista",
            Result.Success(
                listOf(
                    candidate(
                        "1",
                        yearStart = 1800,
                        yearEnd = 1801,
                        title = "Completely Different",
                        countryOrIssuer = "France",
                        denomination = "2 Francs",
                    ),
                ),
            ),
        )
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.getOrEnrich(fingerprint) }

        assertNotNull(result)
        assertNull(result.enrichedAt)
        assertNotNull(result.lastEnrichmentFailedAt)
    }

    @Test
    fun enrichById_returnsFailure_whenCatalogCoinMissing() {
        val now = Instant.parse("2026-01-08T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val provider = FakeProvider("numista")
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.enrichById(UUID.randomUUID()) }

        val failure = result as? Result.Failure
        assertNotNull(failure)
        assertEquals("Catalog coin not found", failure.reason)
    }

    @Test
    fun getOrEnrich_sameYearCountryDenominationButDifferentTitle_createsDistinctCatalogEntries() {
        val now = Instant.parse("2026-01-09T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val provider = FakeProvider("numista")
        val service = service(repository, listOf(provider), now)

        val first = runBlocking { service.getOrEnrich(fingerprint(year = 1921).copy(title = "Morgan Dollar")) }
        val second = runBlocking { service.getOrEnrich(fingerprint(year = 1921).copy(title = "Peace Dollar")) }

        assertNotNull(first)
        assertNotNull(second)
        assertTrue(first.id != second.id)
        assertEquals(2, provider.callCount)
    }

    @Test
    fun getOrEnrich_persistsEnrichmentDetailData_whenCandidateMatches() {
        val now = Instant.parse("2026-01-10T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        val candidateWithDetail = candidate(
            "2",
            yearStart = 1921,
            yearEnd = 1921,
            title = "Morgan Dollar",
            composition = "Silver (.900)",
            weightGrams = 26.73,
            diameterMm = 38.1,
            obverseDescription = "Liberty head left",
            reverseDescription = "Eagle with wings spread",
            historicalContext = "Designed by George T. Morgan",
            thumbnailUrl = "https://example.com/thumb.jpg",
            numistaUrl = "https://en.numista.com/catalogue/pieces2.html",
        )
        val provider = FakeProvider("numista", Result.Success(listOf(candidateWithDetail)))
        val service = service(repository, listOf(provider), now)

        val result = runBlocking { service.getOrEnrich(fingerprint) }

        assertNotNull(result)
        assertNotNull(result.enrichedAt)
        assertEquals("Silver (.900)", result.composition)
        assertEquals(26.73, result.weightGrams)
        assertEquals(38.1, result.diameterMm)
        assertEquals("Liberty head left", result.obverseDescription)
        assertEquals("Eagle with wings spread", result.reverseDescription)
        assertEquals("Designed by George T. Morgan", result.historicalContext)
        assertEquals("https://example.com/thumb.jpg", result.thumbnailUrl)
        assertEquals("https://en.numista.com/catalogue/pieces2.html", result.numistaUrl)
    }

    private fun service(
        repository: InMemoryCatalogCoinRepository,
        providers: List<CoinCatalogProvider>,
        now: Instant,
    ): CoinEnrichmentServiceImpl = CoinEnrichmentServiceImpl(
        catalogCoinRepository = repository,
        providers = providers,
        nowProvider = { now },
    )

    private fun fingerprint(year: Int?): CoinFingerprint =
        CoinFingerprint(
            countryOrIssuer = "United States",
            denomination = "1 Dollar",
            seriesName = "Morgan Dollar",
            title = "Morgan Dollar",
            year = year,
            mintMark = "D",
        )

    private fun stubCoin(fingerprint: CoinFingerprint, now: Instant): CatalogCoin =
        CatalogCoin(
            id = UUID.randomUUID(),
            fingerprint = fingerprint,
            enrichedAt = null,
            lastEnrichmentAttemptAt = null,
            lastEnrichmentFailedAt = null,
            lastEnrichmentError = null,
            createdAt = now,
            updatedAt = now,
        )

    private fun candidate(
        externalId: String,
        yearStart: Int?,
        yearEnd: Int?,
        title: String,
        countryOrIssuer: String = "United States",
        denomination: String = "1 Dollar",
        composition: String? = null,
        weightGrams: Double? = null,
        diameterMm: Double? = null,
        obverseDescription: String? = null,
        reverseDescription: String? = null,
        historicalContext: String? = null,
        thumbnailUrl: String? = null,
        numistaUrl: String? = null,
    ): CoinCatalogCandidate =
        CoinCatalogCandidate(
            externalReference = ExternalCoinReference(
                id = UUID.randomUUID(),
                catalogCoinId = UUID(0, 0),
                provider = "numista",
                externalId = externalId,
                externalUrl = null,
                lastSyncedAt = null,
                syncStatus = null,
                syncError = null,
                createdAt = Instant.now(),
            ),
            title = title,
            countryOrIssuer = countryOrIssuer,
            denomination = denomination,
            yearStart = yearStart,
            yearEnd = yearEnd,
            composition = composition,
            weightGrams = weightGrams,
            diameterMm = diameterMm,
            obverseDescription = obverseDescription,
            reverseDescription = reverseDescription,
            historicalContext = historicalContext,
            thumbnailUrl = thumbnailUrl,
            numistaUrl = numistaUrl,
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
                it.fingerprint.title == fingerprint.title &&
                it.fingerprint.year == fingerprint.year
        }

    override suspend fun findById(id: UUID): CatalogCoin? = coins[id]

    override suspend fun findByIds(ids: List<UUID>): List<CatalogCoin> =
        ids.mapNotNull { coins[it] }

    override suspend fun findByProviderExternalId(provider: String, externalId: String): CatalogCoin? {
        val reference = references.values.firstOrNull { it.provider == provider && it.externalId == externalId } ?: return null
        return coins[reference.catalogCoinId]
    }

    override suspend fun save(catalogCoin: CatalogCoin): CatalogCoin {
        coins[catalogCoin.id] = catalogCoin
        return catalogCoin
    }

    override suspend fun markEnrichmentSuccess(catalogCoinId: UUID, now: Instant, candidate: CoinCatalogCandidate): CatalogCoin? {
        val current = coins[catalogCoinId] ?: return null
        val updated = current.copy(
            enrichedAt = now,
            lastEnrichmentAttemptAt = now,
            lastEnrichmentFailedAt = null,
            lastEnrichmentError = null,
            composition = current.composition ?: candidate.composition,
            weightGrams = current.weightGrams ?: candidate.weightGrams,
            diameterMm = current.diameterMm ?: candidate.diameterMm,
            obverseDescription = current.obverseDescription ?: candidate.obverseDescription,
            reverseDescription = current.reverseDescription ?: candidate.reverseDescription,
            historicalContext = current.historicalContext ?: candidate.historicalContext,
            thumbnailUrl = current.thumbnailUrl ?: candidate.thumbnailUrl,
            numistaUrl = current.numistaUrl ?: candidate.numistaUrl,
            updatedAt = now,
        )
        coins[catalogCoinId] = updated
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
}
