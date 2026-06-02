package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.platform.core.Result
import java.util.UUID

interface CoinSetService {
    suspend fun createSet(userId: UUID, name: String, description: String?): Result<CoinSet>
    suspend fun getSet(setId: UUID, userId: UUID): Result<CoinSet>
    suspend fun listSets(userId: UUID): Result<List<CoinSet>>
    suspend fun addCoinsToSet(setId: UUID, userId: UUID, coinIds: List<UUID>): Result<CoinSet>
    suspend fun removeCoinsFromSet(setId: UUID, userId: UUID, coinIds: List<UUID>): Result<CoinSet>
    suspend fun updateSet(setId: UUID, userId: UUID, name: String, description: String?): Result<CoinSet>
    suspend fun deleteSet(setId: UUID, userId: UUID): Result<Unit>
}
