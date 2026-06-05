package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.platform.core.Result
import java.util.UUID

interface CoinService {
    suspend fun saveCoin(
        userId: UUID,
        obverseKey: String,
        reverseKey: String,
        recognitionResult: RecognitionResult,
        catalogueNumbers: List<CatalogueNumber>,
        notes: String? = null,
    ): Result<Coin>

    suspend fun getCoin(coinId: UUID, userId: UUID): Result<Coin>
    suspend fun listCoins(
        userId: UUID,
        country: String? = null,
        year: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        setId: UUID? = null,
        sortBy: CoinSortField = CoinSortField.DATE_ADDED_NEW_TO_OLD,
        limit: Int = 20,
        cursor: Long? = null,
    ): Result<List<Coin>>

    suspend fun deleteCoin(coinId: UUID, userId: UUID): Result<Unit>
    suspend fun updateNotes(coinId: UUID, userId: UUID, notes: String?): Result<Coin>
    suspend fun getCollectionStats(
        userId: UUID,
        country: String? = null,
        year: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        setId: UUID? = null,
    ): Result<CoinCollectionStats>
}
