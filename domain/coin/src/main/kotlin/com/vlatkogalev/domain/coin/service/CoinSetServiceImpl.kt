package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.domain.coin.repository.CoinSetRepository
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.platform.core.Result
import java.util.UUID

class CoinSetServiceImpl(
    private val coinSetRepository: CoinSetRepository,
    private val coinRepository: CoinRepository,
) : CoinSetService {

    override suspend fun createSet(userId: UUID, name: String, description: String?): Result<CoinSet> =
        try {
            val set = CoinSet(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                description = description,
                coinIds = emptyList(),
                previewObverseKeys = emptyList(),
                createdAt = java.time.Instant.now(),
            )
            Result.Success(coinSetRepository.create(set))
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to create set", ex)
        }

    override suspend fun getSet(setId: UUID, userId: UUID): Result<CoinSet> =
        try {
            val set = coinSetRepository.findById(setId)
                ?: return Result.Failure("Set not found")
            if (set.userId != userId) return Result.Failure("Set not found")
            Result.Success(set)
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to get set", ex)
        }

    override suspend fun listSets(userId: UUID): Result<List<CoinSet>> =
        try {
            Result.Success(coinSetRepository.findByUserId(userId))
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to list sets", ex)
        }

    override suspend fun addCoinsToSet(setId: UUID, userId: UUID, coinIds: List<UUID>): Result<CoinSet> =
        try {
            val set = coinSetRepository.findById(setId)
                ?: return Result.Failure("Set not found")
            if (set.userId != userId) return Result.Failure("Set not found")

            coinIds.forEach { coinId ->
                val coin = coinRepository.findById(coinId) ?: return Result.Failure("Coin not found: $coinId")
                if (coin.userId != userId) return Result.Failure("Coin not found: $coinId")
            }

            val updatedSet = coinSetRepository.addCoins(setId, userId, coinIds)
                ?: return Result.Failure("Set not found")
            Result.Success(updatedSet)
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to add coins to set", ex)
        }

    override suspend fun removeCoinsFromSet(setId: UUID, userId: UUID, coinIds: List<UUID>): Result<CoinSet> =
        try {
            val set = coinSetRepository.findById(setId)
                ?: return Result.Failure("Set not found")
            if (set.userId != userId) return Result.Failure("Set not found")

            val updatedSet = coinSetRepository.removeCoins(setId, userId, coinIds)
                ?: return Result.Failure("Set not found")
            Result.Success(updatedSet)
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to remove coins from set", ex)
        }

    override suspend fun updateSet(setId: UUID, userId: UUID, name: String, description: String?): Result<CoinSet> =
        try {
            val updatedSet = coinSetRepository.update(setId, userId, name, description)
                ?: return Result.Failure("Set not found")
            Result.Success(updatedSet)
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to update set", ex)
        }

    override suspend fun deleteSet(setId: UUID, userId: UUID): Result<Unit> =
        try {
            if (coinSetRepository.deleteById(setId, userId)) Result.Success(Unit)
            else Result.Failure("Set not found")
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to delete set", ex)
        }
}
