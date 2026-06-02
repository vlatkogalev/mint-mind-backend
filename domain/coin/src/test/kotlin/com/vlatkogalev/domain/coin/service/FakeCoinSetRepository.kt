package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.domain.coin.model.CoinSortField
import com.vlatkogalev.domain.coin.repository.CoinSetRepository
import kotlinx.coroutines.runBlocking
import java.util.UUID

class FakeCoinSetRepository(
    private val coinRepository: FakeCoinRepository,
) : CoinSetRepository {
    private val sets = mutableMapOf<UUID, CoinSet>()

    fun reset() {
        sets.clear()
    }

    override suspend fun create(set: CoinSet): CoinSet {
        sets[set.id] = set
        return set
    }

    override suspend fun findById(id: UUID): CoinSet? = sets[id]?.withCurrentCoinData()

    override suspend fun findByUserId(userId: UUID): List<CoinSet> =
        sets.values.filter { it.userId == userId }.map { it.withCurrentCoinData() }

    override suspend fun addCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet? {
        val set = sets[setId] ?: return null
        if (set.userId != userId) return null
        coinIds.forEach { coinId ->
            val coin = coinRepository.findById(coinId) ?: return null
            coinRepository.save(coin.copy(setId = setId))
        }
        return findById(setId)
    }

    override suspend fun removeCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet? {
        val set = sets[setId] ?: return null
        if (set.userId != userId) return null
        coinIds.forEach { coinId ->
            val coin = coinRepository.findById(coinId) ?: return@forEach
            if (coin.setId == setId) coinRepository.save(coin.copy(setId = null))
        }
        return findById(setId)
    }

    override suspend fun update(setId: UUID, userId: UUID, name: String, description: String?): CoinSet? {
        val set = sets[setId] ?: return null
        if (set.userId != userId) return null
        val updated = set.copy(name = name, description = description)
        sets[setId] = updated
        return updated.withCurrentCoinData()
    }

    override suspend fun deleteById(id: UUID, userId: UUID): Boolean {
        val set = sets[id] ?: return false
        if (set.userId != userId) return false

        coinRepository.findByUserId(
            userId = userId,
            country = null,
            year = null,
            minValue = null,
            maxValue = null,
            setId = null,
            sortBy = CoinSortField.DATE_ADDED_NEW_TO_OLD,
            limit = Int.MAX_VALUE,
            offset = 0,
        ).filter { it.setId == id }.forEach { coin ->
            coinRepository.save(coin.copy(setId = null))
        }

        sets.remove(id)
        return true
    }

    private fun CoinSet.withCurrentCoinData(): CoinSet {
        val coinsInSet = runBlocking {
            coinRepository.findByUserId(
                userId = userId,
                country = null,
                year = null,
                minValue = null,
                maxValue = null,
                setId = null,
                sortBy = CoinSortField.DATE_ADDED_NEW_TO_OLD,
                limit = Int.MAX_VALUE,
                offset = 0,
            )
        }.filter { it.setId == id }

        return copy(
            coinIds = coinsInSet.map { it.id },
            previewObverseKeys = coinsInSet.take(5).map { it.obverseKey },
        )
    }
}