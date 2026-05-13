package com.vlatkogalev.domain.coin.repository

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.CoinCollectionStats
import java.util.UUID

interface CoinRepository {
    fun save(coin: Coin): Coin
    fun findById(id: UUID): Coin?
    fun updateNotes(id: UUID, userId: UUID, notes: String?): Coin?
    fun findByUserId(
        userId: UUID,
        country: String? = null,
        year: Int? = null,
        minValueUsd: Double? = null,
        maxValueUsd: Double? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): List<Coin>
    fun getCollectionStats(userId: UUID): CoinCollectionStats
    fun countByUserId(userId: UUID): Int
    fun deleteById(id: UUID, userId: UUID): Boolean
}
