package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.platform.core.Result
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
        val existing = repository.save(stubCoin(fingerprint, now).copy(enrichedAt = now))
        val provider = FakeProvider("numista")
        val service = service(repository, listOf(provider), now)

        val result = service.getOrEnrich(fingerprint)

        assertEquals(existing.id, result?.id)
        assertEquals(0, provider.callCount)
    }

    @Test
    fun getOrEnrich_returnsRecentFailureWithoutProviderCalls() {
        val now = Instant.parse("2026-01-02T00:00:00Z")
        val repository = InMemoryCatalogCoinRepository()
        val fingerprint = fingerprint(year = 1921)
        repository.save(stubCoin(fingerprint, now).copy(lastEnrichmentFailedAt = now.minus(Duration.ofHours(1))))
        val provider = FakeProvider("numista")
        val service = service(repository, listOf(provider), now)

        val result = service.getOrEnrich(fingerprint)

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

        val result = service.getOrEnrich(fingerprint)

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

        val result = service.getOrEnrich(fingerprint)
        val reference = result?.id?.let { repository.findExternalReference(it, "numista") }

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

        val result = service.getOrEnrich(fingerprint)

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

        val first = service.getOrEnrich(fingerprint)
        val second = service.getOrEnrich(fingerprint)

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

        val result = service.getOrEnrich(fingerprint)

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

        val result = service.enrichById(UUID.randomUUID())

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

        val first = service.getOrEnrich(fingerprint(year = 1921).copy(title = "Morgan Dollar"))
        val second = service.getOrEnrich(fingerprint(year = 1921).copy(title = "Peace Dollar"))

        assertNotNull(first)
        assertNotNull(second)
        assertTrue(first.id != second.id)
        assertEquals(2, provider.callCount)
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
        )
}

private class FakeProvider(
    override val providerName: String,
    private var response: Result<List<CoinCatalogCandidate>> = Result.Success(emptyList()),
) : CoinCatalogProvider {
    var callCount: Int = 0
        private set

    override fun findCandidates(fingerprint: CoinFingerprint): Result<List<CoinCatalogCandidate>> {
        callCount++
        return response
    }
}

private class InMemoryCatalogCoinRepository : CatalogCoinRepository {
    private val coins = mutableMapOf<UUID, CatalogCoin>()
    private val references = mutableMapOf<Pair<UUID, String>, ExternalCoinReference>()

    override fun findByFingerprint(fingerprint: CoinFingerprint): CatalogCoin? =
        coins.values.firstOrNull {
            it.fingerprint.countryOrIssuer == fingerprint.countryOrIssuer &&
                it.fingerprint.denomination == fingerprint.denomination &&
                it.fingerprint.title == fingerprint.title &&
                it.fingerprint.year == fingerprint.year
        }

    override fun findById(id: UUID): CatalogCoin? = coins[id]

    override fun findByProviderExternalId(provider: String, externalId: String): CatalogCoin? {
        val reference = references.values.firstOrNull { it.provider == provider && it.externalId == externalId } ?: return null
        return coins[reference.catalogCoinId]
    }

    override fun save(catalogCoin: CatalogCoin): CatalogCoin {
        coins[catalogCoin.id] = catalogCoin
        return catalogCoin
    }

    override fun markEnrichmentSuccess(catalogCoinId: UUID, now: Instant): CatalogCoin? {
        val current = coins[catalogCoinId] ?: return null
        val updated = current.copy(
            enrichedAt = now,
            lastEnrichmentAttemptAt = now,
            lastEnrichmentFailedAt = null,
            lastEnrichmentError = null,
            updatedAt = now,
        )
        coins[catalogCoinId] = updated
        return updated
    }

    override fun markEnrichmentFailed(catalogCoinId: UUID, now: Instant, error: String?): CatalogCoin? {
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

    override fun saveExternalReference(reference: ExternalCoinReference): ExternalCoinReference {
        references[reference.catalogCoinId to reference.provider] = reference
        return reference
    }

    override fun findExternalReference(catalogCoinId: UUID, provider: String): ExternalCoinReference? =
        references[catalogCoinId to provider]
}
