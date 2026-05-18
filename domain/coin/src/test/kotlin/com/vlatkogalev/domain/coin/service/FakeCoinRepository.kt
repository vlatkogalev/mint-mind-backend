package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.platform.core.Result
import java.time.Instant
import java.util.*
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

    fun reset() {
        coins.clear()
        throwOnSave = false
        throwOnFindById = false
        throwOnFindByUserId = false
        throwOnUpdateNotes = false
        throwOnDeleteById = false
        throwOnGetCollectionStats = false
    }

    fun insert(coin: Coin): Coin {
        coins[coin.id] = coin
        return coin
    }

    override fun save(coin: Coin): Coin {
        if (throwOnSave) error("save failed")
        coins[coin.id] = coin
        return coin
    }

    override fun findById(id: UUID): Coin? {
        if (throwOnFindById) error("findById failed")
        return coins[id]
    }

    override fun updateNotes(id: UUID, userId: UUID, notes: String?): Coin? {
        if (throwOnUpdateNotes) error("updateNotes failed")
        val coin = coins[id] ?: return null
        if (coin.userId != userId) return null
        val updated = coin.copy(notes = notes)
        coins[id] = updated
        return updated
    }

    override fun findByUserId(
        userId: UUID,
        country: String?,
        year: Int?,
        minValueUsd: Double?,
        maxValueUsd: Double?,
        sortBy: CoinSortField,
        limit: Int,
        offset: Int,
    ): List<Coin> {
        if (throwOnFindByUserId) error("findByUserId failed")
        return coins.values
            .asSequence()
            .filter { it.userId == userId }
            .filter { country == null || it.recognitionResult.countryOrIssuer == country }
            .filter { year == null || it.recognitionResult.year == year }
            .filter { minValueUsd == null || (it.recognitionResult.valueLowUsd ?: 0.0) >= minValueUsd }
            .filter { maxValueUsd == null || (it.recognitionResult.valueHighUsd ?: 0.0) <= maxValueUsd }
            .sortedWith(sortBy.comparator())
            .drop(offset.coerceAtLeast(0))
            .take(limit.coerceIn(1, 100))
            .toList()
    }

    override fun getCollectionStats(userId: UUID): CoinCollectionStats {
        if (throwOnGetCollectionStats) error("getCollectionStats failed")
        val userCoins = coins.values.filter { it.userId == userId }
        return CoinCollectionStats(
            totalCoins = userCoins.size,
            totalIssuers = userCoins.mapNotNull { it.recognitionResult.countryOrIssuer }.distinct().size,
            estimatedTotalValueMeanUsd = userCoins.mapNotNull { it.meanValue() }.average().takeUnless { it.isNaN() } ?: 0.0,
            highlights = CollectionHighlights(
                mostValuable = userCoins.maxByOrNull { it.meanValue() ?: Double.NEGATIVE_INFINITY },
                mostAncient = userCoins.filter { it.recognitionResult.year != null }.minByOrNull { it.recognitionResult.year!! },
                rarest = userCoins.filter { it.recognitionResult.mintage != null }.minByOrNull { it.recognitionResult.mintage!! },
            ),
        )
    }

    override fun countByUserId(userId: UUID): Int = coins.values.count { it.userId == userId }

    override fun deleteById(id: UUID, userId: UUID): Boolean {
        if (throwOnDeleteById) error("deleteById failed")
        val coin = coins[id] ?: return false
        if (coin.userId != userId) return false
        coins.remove(id)
        return true
    }

    private fun Coin.meanValue(): Double? {
        val low = recognitionResult.valueLowUsd ?: return null
        val high = recognitionResult.valueHighUsd ?: return null
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
        valueLowUsd: Double? = 25.0,
        valueHighUsd: Double? = 50.0,
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
        valueLowUsd = valueLowUsd,
        valueHighUsd = valueHighUsd,
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
        notes = notes,
        createdAt = createdAt,
    )
}

abstract class CoinServiceTestBase {
    protected val repo = FakeCoinRepository()
    protected lateinit var service: CoinServiceImpl

    @BeforeTest
    fun setup() {
        repo.reset()
        service = CoinServiceImpl(repo)
    }

    protected fun <T> assertSuccess(result: Result<T>): Result.Success<T> = assertIs<Result.Success<T>>(result)
    protected fun assertFailure(result: Result<*>): Result.Failure = assertIs<Result.Failure>(result)
}
