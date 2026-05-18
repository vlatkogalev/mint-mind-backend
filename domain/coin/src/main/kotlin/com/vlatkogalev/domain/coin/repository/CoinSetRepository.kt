package com.vlatkogalev.domain.coin.repository

import com.vlatkogalev.domain.coin.model.CoinSet
import java.util.UUID

interface CoinSetRepository {
    fun create(set: CoinSet): CoinSet
    fun findById(id: UUID): CoinSet?
    fun findByUserId(userId: UUID): List<CoinSet>
    fun addCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet?
    fun removeCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet?
    fun update(setId: UUID, userId: UUID, name: String, description: String?): CoinSet?
    fun deleteById(id: UUID, userId: UUID): Boolean
}
