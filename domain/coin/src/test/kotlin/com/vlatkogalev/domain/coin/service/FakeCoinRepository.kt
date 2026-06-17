package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.CoinCollectionStats
import com.vlatkogalev.domain.coin.model.CoinSortField
import com.vlatkogalev.domain.coin.model.CollectionHighlights
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.platform.core.Result
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
    var throwOnReassignFromUser = false

    fun insert(coin: Coin): Coin {
        coins[coin.id] = coin
        return coin
    }

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

    override suspend fun findById(id: UUID): Coin? {
        if (throwOnFindById) throw RuntimeException("findById failed")
        return coins[id]
    }

    override suspend fun findByUserId(
        userId: UUID, country: String?, year: Int?, minValue: Double?, maxValue: Double?,
        setId: UUID?, sortBy: CoinSortField, limit: Int, beforeTimestamp: Long?,
    ): List<Coin> {
        if (throwOnFindByUserId) throw RuntimeException("findByUserId failed")
        var result = coins.values.filter { it.userId == userId }
        if (setId != null) {
            result = result.filter { it.setId == setId }
        }
        result = when (sortBy) {
            CoinSortField.DATE_ADDED_NEW_TO_OLD -> result.sortedByDescending { it.createdAt }
            CoinSortField.DATE_ADDED_OLD_TO_NEW -> result.sortedBy { it.createdAt }
            CoinSortField.VALUE_HIGH_TO_LOW -> result.sortedByDescending { 
                val low = it.recognitionResult.valueLow ?: 0.0
                val high = it.recognitionResult.valueHigh ?: 0.0
                (low + high) / 2.0
            }
            CoinSortField.VALUE_LOW_TO_HIGH -> result.sortedBy { 
                val low = it.recognitionResult.valueLow ?: 0.0
                val high = it.recognitionResult.valueHigh ?: 0.0
                (low + high) / 2.0
            }
            CoinSortField.RELEASE_YEAR_OLD_TO_NEW -> result.sortedBy { it.recognitionResult.year ?: Int.MAX_VALUE }
            CoinSortField.RELEASE_YEAR_NEW_TO_OLD -> result.sortedByDescending { it.recognitionResult.year ?: 0 }
        }
        return result.take(limit)
    }

    override suspend fun save(coin: Coin): Coin {
        if (throwOnSave) throw RuntimeException("save failed")
        coins[coin.id] = coin
        return coin
    }

    override suspend fun updateNotes(coinId: UUID, userId: UUID, notes: String?): Coin? {
        if (throwOnUpdateNotes) throw RuntimeException("updateNotes failed")
        val existing = coins[coinId] ?: return null
        if (existing.userId != userId) return null
        val updated = existing.copy(notes = notes)
        coins[coinId] = updated
        return updated
    }

    override suspend fun deleteById(coinId: UUID, userId: UUID): Boolean {
        if (throwOnDeleteById) throw RuntimeException("deleteById failed")
        val existing = coins[coinId] ?: return false
        if (existing.userId != userId) return false
        coins.remove(coinId)
        return true
    }

    override suspend fun getCollectionStats(
        userId: UUID, country: String?, year: Int?, minValue: Double?, maxValue: Double?, setId: UUID?,
    ): CoinCollectionStats {
        if (throwOnGetCollectionStats) throw RuntimeException("getCollectionStats failed")
        var userCoins = coins.values.filter { it.userId == userId }
        if (setId != null) userCoins = userCoins.filter { it.setId == setId }
        val totalValue = userCoins.sumOf { coin ->
            val low = coin.recognitionResult.valueLow ?: 0.0
            val high = coin.recognitionResult.valueHigh ?: 0.0
            (low + high) / 2.0
        }
        val meanValue = if (userCoins.isEmpty()) 0.0 else totalValue / userCoins.size
        val mostValuable = userCoins.maxByOrNull {
            val low = it.recognitionResult.valueLow ?: 0.0
            val high = it.recognitionResult.valueHigh ?: 0.0
            (low + high) / 2.0
        }
        val mostAncient = userCoins.filter { it.recognitionResult.year != null }
            .minByOrNull { it.recognitionResult.year!! }
        val rarest = userCoins.filter { it.recognitionResult.mintage != null }
            .minByOrNull { it.recognitionResult.mintage!! }
        return CoinCollectionStats(
            totalCoins = userCoins.size,
            totalIssuers = userCoins.mapNotNull { it.recognitionResult.countryOrIssuer }.distinct().size,
            estimatedTotalValueMean = meanValue,
            highlights = CollectionHighlights(
                mostValuable = mostValuable,
                mostAncient = mostAncient,
                rarest = rarest,
            ),
        )
    }

    override suspend fun reassignFromUser(fromUserId: UUID, toUserId: UUID): Int {
        if (throwOnReassignFromUser) throw RuntimeException("reassignFromUser failed")
        var count = 0
        coins.values.filter { it.userId == fromUserId }.forEach { coin ->
            coins[coin.id] = coin.copy(userId = toUserId)
            count++
        }
        return count
    }

    override suspend fun updateCatalogCoinId(coinId: UUID, catalogCoinId: UUID): Coin? {
        val existing = coins[coinId] ?: return null
        val updated = existing.copy(catalogCoinId = catalogCoinId)
        coins[coinId] = updated
        return updated
    }

    override suspend fun countByUserId(userId: UUID): Int =
        coins.values.count { it.userId == userId }
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
