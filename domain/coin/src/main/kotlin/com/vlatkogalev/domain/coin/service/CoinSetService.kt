package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.platform.core.Result
import java.util.UUID

interface CoinSetService {
    fun createSet(userId: UUID, name: String, description: String?): Result<CoinSet>
    fun getSet(setId: UUID, userId: UUID): Result<CoinSet>
    fun listSets(userId: UUID): Result<List<CoinSet>>
    fun addCoinsToSet(setId: UUID, userId: UUID, coinIds: List<UUID>): Result<CoinSet>
    fun removeCoinsFromSet(setId: UUID, userId: UUID, coinIds: List<UUID>): Result<CoinSet>
    fun updateSet(setId: UUID, userId: UUID, name: String, description: String?): Result<CoinSet>
    fun deleteSet(setId: UUID, userId: UUID): Result<Unit>
}
