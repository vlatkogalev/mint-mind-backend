package com.vlatkogalev.domain.coin.repository

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.CoinCollectionStats
import com.vlatkogalev.domain.coin.model.CoinSortField
import java.util.UUID

interface CoinRepository {
    suspend fun save(coin: Coin): Coin
    suspend fun findById(id: UUID): Coin?
    suspend fun updateNotes(id: UUID, userId: UUID, notes: String?): Coin?
    suspend fun findByUserId(
        userId: UUID,
        country: String? = null,
        year: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        setId: UUID? = null,
        sortBy: CoinSortField = CoinSortField.DATE_ADDED_NEW_TO_OLD,
        limit: Int = 20,
        beforeTimestamp: Long? = null,
    ): List<Coin>
    suspend fun getCollectionStats(
        userId: UUID,
        country: String? = null,
        year: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        setId: UUID? = null,
    ): CoinCollectionStats
    suspend fun reassignFromUser(fromUserId: UUID, toUserId: UUID): Int
    suspend fun countByUserId(userId: UUID): Int
    suspend fun deleteById(id: UUID, userId: UUID): Boolean
}