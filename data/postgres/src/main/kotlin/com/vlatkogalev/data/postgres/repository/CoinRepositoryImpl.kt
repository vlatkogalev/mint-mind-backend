package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.CoinQueries
import com.vlatkogalev.data.postgres.entities.CatalogueNumberRecord
import com.vlatkogalev.data.postgres.entities.CoinRecord
import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.CoinCollectionStats
import com.vlatkogalev.domain.coin.model.Confidence
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.repository.CoinRepository
import java.util.UUID

class CoinRepositoryImpl(
    private val queries: CoinQueries,
) : CoinRepository {
    override fun save(coin: Coin): Coin =
        queries.withTransaction { connection ->
            val existing = queries.findById(connection, coin.id)
            if (existing == null) {
                queries.insert(connection, coin)
                queries.insertCatalogueNumbers(connection, coin.id, coin.catalogueNumbers)
            } else {
                queries.update(connection, coin)
            }

            val saved = queries.findById(connection, coin.id) ?: error("Saved coin could not be loaded")
            val numbers = queries.findCatalogueNumbersByCoinId(connection, coin.id)
            saved.toDomain(numbers)
        }

    override fun findById(id: UUID): Coin? {
        val coin = queries.findById(id) ?: return null
        val numbers = queries.findCatalogueNumbersByCoinId(id)
        return coin.toDomain(numbers)
    }

    override fun updateNotes(id: UUID, userId: UUID, notes: String?): Coin? =
        queries.withTransaction { connection ->
            val updated = queries.updateNotes(connection, id, userId, notes)
            if (!updated) return@withTransaction null
            val coin = queries.findById(connection, id) ?: return@withTransaction null
            val numbers = queries.findCatalogueNumbersByCoinId(connection, id)
            coin.toDomain(numbers)
        }

    override fun findByUserId(
        userId: UUID,
        country: String?,
        year: Int?,
        minValueUsd: Double?,
        maxValueUsd: Double?,
        limit: Int,
        offset: Int,
    ): List<Coin> =
        queries.findByUserId(
            userId = userId,
            country = country,
            year = year,
            minValue = minValueUsd,
            maxValue = maxValueUsd,
            limit = limit,
            offset = offset,
        ).map { coin ->
            coin.toDomain(queries.findCatalogueNumbersByCoinId(coin.id))
        }

    override fun getCollectionStats(userId: UUID): CoinCollectionStats {
        val valueStats = queries.getValueStats(userId)
        return CoinCollectionStats(
            totalCoins = valueStats.totalCoins,
            estimatedTotalValueLowUsd = valueStats.estimatedTotalValueLowUsd,
            estimatedTotalValueHighUsd = valueStats.estimatedTotalValueHighUsd,
            byCountry = queries.countByCountry(userId),
            byYear = queries.countByYear(userId),
        )
    }

    override fun countByUserId(userId: UUID): Int = queries.countByUserId(userId)

    override fun deleteById(id: UUID, userId: UUID): Boolean = queries.deleteById(id, userId)

    private fun CoinRecord.toDomain(catalogueNumbers: List<CatalogueNumberRecord>): Coin =
        Coin(
            id = id,
            userId = userId,
            obverseKey = obverseKey,
            reverseKey = reverseKey,
            recognitionResult = RecognitionResult(
                overallConfidence = Confidence.valueOf(overallConfidence.uppercase()),
                countryOrIssuer = countryOrIssuer,
                denomination = denomination,
                seriesName = seriesName,
                year = year,
                mintMark = mintMark,
                metalComposition = metalComposition,
                estimatedGrade = estimatedGrade,
                estimatedGradeValue = estimatedGradeValue,
                rarityQualitative = rarityQualitative,
                valueLowUsd = valueLowUsd,
                valueHighUsd = valueHighUsd,
                obverseDescription = obverseDescription,
                reverseDescription = reverseDescription,
                historicalContext = historicalContext,
                rawJson = rawJson,
            ),
            catalogueNumbers = catalogueNumbers.map { it.toDomain() },
            notes = notes,
            createdAt = createdAt,
        )

    private fun CatalogueNumberRecord.toDomain(): CatalogueNumber =
        CatalogueNumber(
            catalogueName = catalogueName,
            number = number,
            confidence = Confidence.valueOf(confidence.uppercase()),
        )
}
