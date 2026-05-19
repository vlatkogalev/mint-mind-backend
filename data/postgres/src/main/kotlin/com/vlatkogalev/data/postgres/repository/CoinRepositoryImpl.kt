package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.CoinQueries
import com.vlatkogalev.data.postgres.entities.CatalogueNumberRecord
import com.vlatkogalev.data.postgres.entities.CoinRecord
import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.CoinCollectionStats
import com.vlatkogalev.domain.coin.model.CollectionHighlights
import com.vlatkogalev.domain.coin.model.CoinSortField
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
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
        sortBy: CoinSortField,
        limit: Int,
        offset: Int,
    ): List<Coin> {
        val coinRecords = queries.findByUserId(
            userId = userId,
            country = country,
            year = year,
            minValue = minValue,
            maxValue = maxValue,
            setId = setId,
            sortBy = sortBy,
            limit = limit,
            offset = offset,
        )
        if (coinRecords.isEmpty()) return emptyList()

        val cataloguesByCoinId = queries.findCatalogueNumbersByCoinIds(coinRecords.map { it.id })
        return coinRecords.map { coin ->
            coin.toDomain(cataloguesByCoinId[coin.id].orEmpty())
        }
    }

    override fun getCollectionStats(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
    ): CoinCollectionStats = CoinCollectionStats(
        totalCoins = queries.countCoins(
            userId = userId,
            country = country,
            year = year,
            minValue = minValue,
            maxValue = maxValue,
            setId = setId,
        ),

        totalIssuers = queries.countDistinctIssuers(
            userId = userId,
            country = country,
            year = year,
            minValue = minValue,
            maxValue = maxValue,
            setId = setId,
        ),

        estimatedTotalValueMean = queries.getMeanValue(
            userId = userId,
            country = country,
            year = year,
            minValue = minValue,
            maxValue = maxValue,
            setId = setId,
        ),

        highlights = CollectionHighlights(
            mostValuable = queries.findMostValuable(
                userId = userId,
                country = country,
                year = year,
                minValue = minValue,
                maxValue = maxValue,
                setId = setId,
            )?.toDomainWithCatalogueNumbers(),

            mostAncient = queries.findMostAncient(
                userId = userId,
                country = country,
                year = year,
                minValue = minValue,
                maxValue = maxValue,
                setId = setId,
            )?.toDomainWithCatalogueNumbers(),

            rarest = queries.findRarest(
                userId = userId,
                country = country,
                year = year,
                minValue = minValue,
                maxValue = maxValue,
                setId = setId,
            )?.toDomainWithCatalogueNumbers(),
        ),
    )

    override fun countByUserId(userId: UUID): Int = queries.countByUserId(userId)

    override fun deleteById(id: UUID, userId: UUID): Boolean = queries.deleteById(id, userId)

    private fun CoinRecord.toDomainWithCatalogueNumbers(): Coin =
        toDomain(queries.findCatalogueNumbersByCoinId(id))

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
                valueLow = valueLow,
                valueHigh = valueHigh,
                mintage = mintage,
                obverseDescription = obverseDescription,
                reverseDescription = reverseDescription,
                historicalContext = historicalContext,
                rawJson = rawJson,
            ),
            catalogueNumbers = catalogueNumbers.map { it.toDomain() },
            setId = setId,
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