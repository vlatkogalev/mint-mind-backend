package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.CoinCollectionStats
import com.vlatkogalev.domain.coin.model.CoinSortField
import com.vlatkogalev.domain.coin.model.CollectionHighlights
import com.vlatkogalev.domain.coin.model.Confidence
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.platform.core.Result
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.assertIs

class FakeCoinRepository : CoinRepository {
    private val coins = mutableMapOf<UUID, Coin>()

    var throwOnSave = false
    var throwOnFindById = false
    var throwOnFindByUserId = false
    var throwOnUpdateNotes = false
    var throwOnDeleteById = false
    var throwOnGetCollectionStats = false
    var throwOnReassignFromUser = false

    fun reset() {
        coins.clear()
        throwOnSave = false
        throwOnFindById = false
        throwOnFindByUserId = false
        throwOnUpdateNotes = false
        throwOnDeleteById = false
        throwOnGetCollectionStats = false
        throwOnReassignFromUser = false
    }

    fun insert(coin: Coin): Coin {
        coins[coin.id] = coin
        return coin
    }

    override suspend fun save(coin: Coin): Coin {
        if (throwOnSave) error("save failed")
        coins[coin.id] = coin
        return coin
    }

    override suspend fun findById(id: UUID): Coin? {
        if (throwOnFindById) error("findById failed")
        return coins[id]
    }

    override suspend fun updateNotes(id: UUID, userId: UUID, notes: String?): Coin? {
        if (throwOnUpdateNotes) error("updateNotes failed")
        val coin = coins[id] ?: return null
        if (coin.userId != userId) return null
        val updated = coin.copy(notes = notes)
        coins[id] = updated
        return updated
    }

    override suspend fun findByUserId(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
        sortBy: CoinSortField,
        limit: Int,
        beforeTimestamp: Long?,
    ): List<Coin> {
        if (throwOnFindByUserId) error("findByUserId failed")
        return coins.values
            .asSequence()
            .filter { it.userId == userId }
            .filter { country == null || it.recognitionResult.countryOrIssuer == country }
            .filter { year == null || it.recognitionResult.year == year }
            .filter { minValue == null || (it.recognitionResult.valueLow ?: 0.0) >= minValue }
            .filter { maxValue == null || (it.recognitionResult.valueHigh ?: 0.0) <= maxValue }
            .filter { setId == null || it.setId == setId }
            .filter { beforeTimestamp == null || it.createdAt.toEpochMilli() < beforeTimestamp }
            .sortedByDescending { it.createdAt }
            .take(limit.coerceIn(1, 100))
            .sortedWith(sortBy.comparator())
            .toList()
    }

    override suspend fun getCollectionStats(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
    ): CoinCollectionStats {
        if (throwOnGetCollectionStats) error("getCollectionStats failed")

        val userCoins = coins.values
            .asSequence()
            .filter { it.userId == userId }
            .filter { country == null || it.recognitionResult.countryOrIssuer == country }
            .filter { year == null || it.recognitionResult.year == year }
            .filter { minValue == null || (it.recognitionResult.valueLow ?: 0.0) >= minValue }
            .filter { maxValue == null || (it.recognitionResult.valueHigh ?: 0.0) <= maxValue }
            .filter { setId == null || it.setId == setId }
            .toList()

        return CoinCollectionStats(
            totalCoins = userCoins.size,

            totalIssuers = userCoins
                .mapNotNull { it.recognitionResult.countryOrIssuer }
                .distinct()
                .size,

            estimatedTotalValueMean = userCoins
                .mapNotNull { it.meanValue() }
                .average()
                .takeUnless { it.isNaN() }
                ?: 0.0,

            highlights = CollectionHighlights(
                mostValuable = userCoins
                    .maxByOrNull { it.meanValue() ?: Double.NEGATIVE_INFINITY },

                mostAncient = userCoins
                    .filter { it.recognitionResult.year != null }
                    .minByOrNull { it.recognitionResult.year!! },

                rarest = userCoins
                    .filter { it.recognitionResult.mintage != null }
                    .minByOrNull { it.recognitionResult.mintage!! },
            ),
        )
    }

    override suspend fun countByUserId(userId: UUID): Int = coins.values.count { it.userId == userId }

    override suspend fun reassignFromUser(fromUserId: UUID, toUserId: UUID): Int {
        if (throwOnReassignFromUser) error("reassignFromUser failed")
        var count = 0
        coins.entries
            .filter { it.value.userId == fromUserId }
            .forEach { (id, coin) ->
                coins[id] = coin.copy(userId = toUserId)
                count++
            }
        return count
    }

    override suspend fun deleteById(id: UUID, userId: UUID): Boolean {
        if (throwOnDeleteById) error("deleteById failed")
        val coin = coins[id] ?: return false
        if (coin.userId != userId) return false
        coins.remove(id)
        return true
    }

    private fun Coin.meanValue(): Double? {
        val low = recognitionResult.valueLow ?: return null
        val high = recognitionResult.valueHigh ?: return null
        return (low + high) / 2.0
    }

    private fun CoinSortField.comparator(): Comparator<Coin> =
        when (this) {
            CoinSortField.VALUE_HIGH_TO_LOW -> compareByDescending<Coin> { it.meanValue() ?: Double.NEGATIVE_INFINITY }
            CoinSortField.VALUE_LOW_TO_HIGH -> compareBy<Coin> { it.meanValue() ?: Double.POSITIVE_INFINITY }
            CoinSortField.RELEASE_YEAR_OLD_TO_NEW -> compareBy<Coin> { it.recognitionResult.year ?: Int.MAX_VALUE }
            CoinSortField.RELEASE_YEAR_NEW_TO_OLD -> compareByDescending<Coin> { it.recognitionResult.year ?: Int.MIN_VALUE }
            CoinSortField.DATE_ADDED_OLD_TO_NEW -> compareBy { it.createdAt }
            CoinSortField.DATE_ADDED_NEW_TO_OLD -> compareByDescending { it.createdAt }
        }
}

object TestFixtures {
    val USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val OTHER_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

    fun makeRecognitionResult(
        overallConfidence: Confidence = Confidence.HIGH,
        countryOrIssuer: String? = "United States",
        year: Int? = 1921,
        valueLow: Double? = 25.0,
        valueHigh: Double? = 50.0,
        mintage: Long? = 1_000_000,
        rawJson: String = """{"is_coin": true}""",
    ) = RecognitionResult(
        overallConfidence = overallConfidence,
        countryOrIssuer = countryOrIssuer,
        denomination = "1 Dollar",
        seriesName = "Morgan Dollar",
        year = year,
        mintMark = "D",
        metalComposition = "90% Silver, 10% Copper",
        estimatedGrade = "Very Fine (VF)",
        estimatedGradeValue = "VF-30",
        rarityQualitative = "Common",
        valueLow = valueLow,
        valueHigh = valueHigh,
        mintage = mintage,
        obverseDescription = "Lady Liberty facing left",
        reverseDescription = "Eagle with wings spread",
        historicalContext = "Minted during the silver era",
        rawJson = rawJson,
    )

    fun makeCatalogueNumber(
        catalogueName: String = "Krause",
        number: String? = "KM# 110",
        confidence: Confidence = Confidence.HIGH,
    ) = CatalogueNumber(
        catalogueName = catalogueName,
        number = number,
        confidence = confidence,
    )

    fun makeCoin(
        id: UUID = UUID.randomUUID(),
        userId: UUID = USER_ID,
        obverseKey: String = "users/$userId/obverse.jpg",
        reverseKey: String = "users/$userId/reverse.jpg",
        recognitionResult: RecognitionResult = makeRecognitionResult(),
        catalogueNumbers: List<CatalogueNumber> = listOf(makeCatalogueNumber()),
        setId: UUID? = null,
        catalogCoinId: UUID? = null,
        notes: String? = null,
        createdAt: Instant = Instant.now(),
    ) = Coin(
        id = id,
        userId = userId,
        obverseKey = obverseKey,
        reverseKey = reverseKey,
        recognitionResult = recognitionResult,
        catalogueNumbers = catalogueNumbers,
        setId = setId,
        catalogCoinId = catalogCoinId,
        notes = notes,
        createdAt = createdAt,
    )
}

abstract class CoinServiceTestBase {
    protected val repo = FakeCoinRepository()
    private val enrichmentRepository = FakeCatalogCoinRepository()
    private val enrichmentService = CoinEnrichmentServiceImpl(
        catalogCoinRepository = enrichmentRepository,
        providers = emptyList(),
    )
    protected lateinit var service: CoinServiceImpl

    @BeforeTest
    fun setup() {
        repo.reset()
        enrichmentRepository.reset()
        service = CoinServiceImpl(repo, enrichmentService)
    }

    protected fun <T> assertSuccess(result: Result<T>): Result.Success<T> = assertIs<Result.Success<T>>(result)
    protected fun assertFailure(result: Result<*>): Result.Failure = assertIs<Result.Failure>(result)
}

private class FakeCatalogCoinRepository : CatalogCoinRepository {
    private val byId = mutableMapOf<UUID, com.vlatkogalev.domain.coin.model.CatalogCoin>()
    private val references = mutableMapOf<Pair<UUID, String>, com.vlatkogalev.domain.coin.model.ExternalCoinReference>()

    fun reset() {
        byId.clear()
        references.clear()
    }

    override suspend fun findByFingerprint(fingerprint: com.vlatkogalev.domain.coin.model.CoinFingerprint): com.vlatkogalev.domain.coin.model.CatalogCoin? =
        byId.values.firstOrNull {
            it.fingerprint.countryOrIssuer == fingerprint.countryOrIssuer &&
                it.fingerprint.denomination == fingerprint.denomination &&
                it.fingerprint.title == fingerprint.title &&
                it.fingerprint.year == fingerprint.year
        }

    override suspend fun findById(id: UUID): com.vlatkogalev.domain.coin.model.CatalogCoin? = byId[id]

    override suspend fun findByIds(ids: List<UUID>): List<com.vlatkogalev.domain.coin.model.CatalogCoin> =
        ids.mapNotNull { byId[it] }

    override suspend fun findByProviderExternalId(provider: String, externalId: String): com.vlatkogalev.domain.coin.model.CatalogCoin? {
        val reference = references.values.firstOrNull { it.provider == provider && it.externalId == externalId } ?: return null
        return byId[reference.catalogCoinId]
    }

    override suspend fun save(catalogCoin: com.vlatkogalev.domain.coin.model.CatalogCoin): com.vlatkogalev.domain.coin.model.CatalogCoin {
        byId[catalogCoin.id] = catalogCoin
        return catalogCoin
    }

    override suspend fun markEnrichmentSuccess(catalogCoinId: UUID, now: Instant, candidate: com.vlatkogalev.domain.coin.model.CoinCatalogCandidate): com.vlatkogalev.domain.coin.model.CatalogCoin? {
        val coin = byId[catalogCoinId] ?: return null
        val updated = coin.copy(
            enrichedAt = now,
            lastEnrichmentAttemptAt = now,
            lastEnrichmentFailedAt = null,
            lastEnrichmentError = null,
            composition = coin.composition ?: candidate.composition,
            weightGrams = coin.weightGrams ?: candidate.weightGrams,
            diameterMm = coin.diameterMm ?: candidate.diameterMm,
            obverseDescription = coin.obverseDescription ?: candidate.obverseDescription,
            reverseDescription = coin.reverseDescription ?: candidate.reverseDescription,
            historicalContext = coin.historicalContext ?: candidate.historicalContext,
            thumbnailUrl = coin.thumbnailUrl ?: candidate.thumbnailUrl,
            numistaUrl = coin.numistaUrl ?: candidate.numistaUrl,
            updatedAt = now,
        )
        byId[catalogCoinId] = updated
        return updated
    }

    override suspend fun markEnrichmentFailed(catalogCoinId: UUID, now: Instant, error: String?): com.vlatkogalev.domain.coin.model.CatalogCoin? {
        val coin = byId[catalogCoinId] ?: return null
        val updated = coin.copy(
            lastEnrichmentAttemptAt = now,
            lastEnrichmentFailedAt = now,
            lastEnrichmentError = error,
            updatedAt = now,
        )
        byId[catalogCoinId] = updated
        return updated
    }

    override suspend fun saveExternalReference(reference: com.vlatkogalev.domain.coin.model.ExternalCoinReference): com.vlatkogalev.domain.coin.model.ExternalCoinReference {
        references[reference.catalogCoinId to reference.provider] = reference
        return reference
    }

    override suspend fun findExternalReference(catalogCoinId: UUID, provider: String): com.vlatkogalev.domain.coin.model.ExternalCoinReference? =
        references[catalogCoinId to provider]
}
