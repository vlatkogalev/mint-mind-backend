package com.vlatkogalev.domain.coin.repository

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.CoinCollectionStats
import com.vlatkogalev.domain.coin.model.CoinSortField
import java.util.UUID

interface CoinRepository {
    fun save(coin: Coin): Coin
    fun findById(id: UUID): Coin?
    fun updateNotes(id: UUID, userId: UUID, notes: String?): Coin?
    fun findByUserId(
        userId: UUID,
        country: String? = null,
        year: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        setId: UUID? = null,
        sortBy: CoinSortField = CoinSortField.DATE_ADDED_NEW_TO_OLD,
        limit: Int = 20,
        offset: Int = 0,
    ): List<Coin>
    fun getCollectionStats(
        userId: UUID,
        country: String? = null,
        year: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        setId: UUID? = null,
    ): CoinCollectionStats
    fun reassignFromUser(fromUserId: UUID, toUserId: UUID): Int
    fun countByUserId(userId: UUID): Int
    fun deleteById(id: UUID, userId: UUID): Boolean
}