package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.platform.core.Result
import java.util.UUID

interface CoinService {
    fun saveCoin(
        userId: UUID,
        obverseKey: String,
        reverseKey: String,
        recognitionResult: RecognitionResult,
        catalogueNumbers: List<CatalogueNumber>,
        notes: String?,
    ): Result<Coin>

    fun getCoin(coinId: UUID, userId: UUID): Result<Coin>

    fun listCoins(
        userId: UUID,
        country: String?,
        year: Int?,
        minValueUsd: Double?,
        maxValueUsd: Double?,
        limit: Int,
        offset: Int,
    ): Result<List<Coin>>

    fun deleteCoin(coinId: UUID, userId: UUID): Result<Unit>

    fun updateNotes(coinId: UUID, userId: UUID, notes: String?): Result<Coin>

    fun getCollectionStats(userId: UUID): Result<CollectionStats>
}

data class CollectionStats(
    val totalCoins: Int,
    val estimatedTotalValueLowUsd: Double,
    val estimatedTotalValueHighUsd: Double,
    val byCountry: Map<String, Int>,
    val byYear: Map<Int, Int>,
)
