package com.vlatkogalev.domain.coin.repository

import com.vlatkogalev.domain.coin.model.CoinSet
import java.util.UUID

interface CoinSetRepository {
    suspend fun create(set: CoinSet): CoinSet
    suspend fun findById(id: UUID): CoinSet?
    suspend fun findByUserId(userId: UUID): List<CoinSet>
    suspend fun addCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet?
    suspend fun removeCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet?
    suspend fun update(setId: UUID, userId: UUID, name: String, description: String?): CoinSet?
    suspend fun deleteById(setId: UUID, userId: UUID): Boolean
}
