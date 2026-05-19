package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.CoinCollectionStats
import com.vlatkogalev.domain.coin.model.CoinSortField
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.platform.core.Result
import java.time.Instant
import java.util.UUID

class CoinServiceImpl(
    private val coinRepository: CoinRepository,
) : CoinService {

    override fun saveCoin(
        userId: UUID,
        obverseKey: String,
        reverseKey: String,
        recognitionResult: RecognitionResult,
        catalogueNumbers: List<CatalogueNumber>,
        notes: String?,
    ): Result<Coin> = try {
        val coin = Coin(
            id = UUID.randomUUID(),
            userId = userId,
            obverseKey = obverseKey,
            reverseKey = reverseKey,
            recognitionResult = recognitionResult,
            catalogueNumbers = catalogueNumbers,
            setId = null,
            notes = notes,
            createdAt = Instant.now(),
        )
        Result.Success(coinRepository.save(coin))
    } catch (ex: Exception) {
        Result.Failure(ex.message ?: "Failed to save coin", ex)
    }

    override fun getCoin(coinId: UUID, userId: UUID): Result<Coin> = try {
        val coin = coinRepository.findById(coinId)
        when {
            coin == null -> Result.Failure("Coin not found")
            coin.userId != userId -> Result.Failure("Coin not found")
            else -> Result.Success(coin)
        }
    } catch (ex: Exception) {
        Result.Failure(ex.message ?: "Failed to fetch coin", ex)
    }

    override fun listCoins(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
        sortBy: CoinSortField,
        limit: Int,
        offset: Int,
    ): Result<List<Coin>> = try {
        Result.Success(
            coinRepository.findByUserId(
                userId = userId,
                country = country,
                year = year,
                minValue = minValue,
                maxValue = maxValue,
                setId = setId,
                sortBy = sortBy,
                limit = limit.coerceIn(1, 100),
                offset = offset.coerceAtLeast(0),
            ),
        )
    } catch (ex: Exception) {
        Result.Failure(ex.message ?: "Failed to list coins", ex)
    }

    override fun deleteCoin(coinId: UUID, userId: UUID): Result<Unit> = try {
        if (coinRepository.deleteById(coinId, userId)) Result.Success(Unit)
        else Result.Failure("Coin not found")
    } catch (ex: Exception) {
        Result.Failure(ex.message ?: "Failed to delete coin", ex)
    }

    override fun updateNotes(coinId: UUID, userId: UUID, notes: String?): Result<Coin> = try {
        val updatedCoin = coinRepository.updateNotes(coinId, userId, notes)
        if (updatedCoin == null) Result.Failure("Coin not found") else Result.Success(updatedCoin)
    } catch (ex: Exception) {
        Result.Failure(ex.message ?: "Failed to update notes", ex)
    }

    override fun getCollectionStats(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
    ): Result<CoinCollectionStats> = try {
        Result.Success(
            coinRepository.getCollectionStats(
                userId = userId,
                country = country,
                year = year,
                minValue = minValue,
                maxValue = maxValue,
                setId = setId,
            ),
        )
    } catch (ex: Exception) {
        Result.Failure(ex.message ?: "Failed to get collection stats", ex)
    }
}