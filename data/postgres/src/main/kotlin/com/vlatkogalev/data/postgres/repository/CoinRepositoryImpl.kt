package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.platform.database.tables.CoinCatalogueNumbersTable
import com.vlatkogalev.platform.database.tables.CoinsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class CoinRepositoryImpl : CoinRepository {
    override suspend fun save(coin: Coin): Coin =
        newSuspendedTransaction {
            val existing = CoinsTable.selectAll().where { CoinsTable.id eq coin.id }.singleOrNull()
            if (existing == null) {
                insertCoin(coin)
                insertCatalogueNumbers(coin.id, coin.catalogueNumbers)
            } else {
                updateCoin(coin)
            }
            findCoinById(coin.id) ?: error("Saved coin could not be loaded")
        }

    override suspend fun findById(id: UUID): Coin? = newSuspendedTransaction { findCoinById(id) }

    override suspend fun updateNotes(id: UUID, userId: UUID, notes: String?): Coin? =
        newSuspendedTransaction {
            val updated = CoinsTable.update(
                where = { (CoinsTable.id eq id) and (CoinsTable.userId eq userId) },
                body = { it[CoinsTable.notes] = notes },
            ) > 0
            if (!updated) return@newSuspendedTransaction null
            findCoinById(id)
        }

    override suspend fun findByUserId(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
        sortBy: CoinSortField,
        limit: Int,
        offset: Int,
    ): List<Coin> =
        newSuspendedTransaction {
            val coinRecords = CoinsTable.selectAll().where {
                (CoinsTable.userId eq userId) and
                        (if (!country.isNullOrBlank()) CoinsTable.countryOrIssuer eq country else Op.TRUE) and
                        (if (year != null) CoinsTable.year eq year else Op.TRUE) and
                        (if (minValue != null) CoinsTable.valueLow greaterEq BigDecimal.valueOf(minValue) else Op.TRUE) and
                        (if (maxValue != null) CoinsTable.valueHigh lessEq BigDecimal.valueOf(maxValue) else Op.TRUE) and
                        (if (setId != null) CoinsTable.setId eq setId else Op.TRUE)
            }
                .orderBy(CoinsTable.createdAt to SortOrder.DESC)
                .limit(limit).offset(offset.toLong())
                .toList()

            if (coinRecords.isEmpty()) return@newSuspendedTransaction emptyList()

            val cataloguesByCoinId = findCatalogueNumbersByCoinIds(coinRecords.map { it[CoinsTable.id] })
            val coins = coinRecords.map { it.toCoin(cataloguesByCoinId[it[CoinsTable.id]].orEmpty()) }
            sortCoins(coins, sortBy)
        }

    override suspend fun getCollectionStats(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
    ): CoinCollectionStats = coroutineScope {
        val filter = buildFilter(userId, country, year, minValue, maxValue, setId)

        val totalCoinsDeferred = async {
            newSuspendedTransaction {
                CoinsTable.selectAll().where { filter }.count().toInt()
            }
        }
        val totalIssuersDeferred = async {
            newSuspendedTransaction {
                CoinsTable.select(CoinsTable.countryOrIssuer).where { filter }
                    .withDistinct().count().toInt()
            }
        }
        val meanValueDeferred = async {
            newSuspendedTransaction {
                val rows = CoinsTable.select(CoinsTable.valueLow, CoinsTable.valueHigh).where { filter }.toList()
                val values = rows.mapNotNull { row ->
                    val low = row[CoinsTable.valueLow]?.toDouble()
                    val high = row[CoinsTable.valueHigh]?.toDouble()
                    if (low != null && high != null) (low + high) / 2.0 else null
                }
                values.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            }
        }
        val mostValuableDeferred = async {
            newSuspendedTransaction {
                CoinsTable.selectAll().where { filter }
                    .orderBy(CoinsTable.valueHigh to SortOrder.DESC_NULLS_LAST)
                    .limit(1).singleOrNull()
                    ?.toCoinWithCatalogueNumbers()
            }
        }
        val mostAncientDeferred = async {
            newSuspendedTransaction {
                CoinsTable.selectAll().where { filter }
                    .orderBy(CoinsTable.year to SortOrder.ASC_NULLS_LAST)
                    .limit(1).singleOrNull()
                    ?.toCoinWithCatalogueNumbers()
            }
        }
        val rarestDeferred = async {
            newSuspendedTransaction {
                CoinsTable.selectAll().where { filter }
                    .orderBy(CoinsTable.mintage to SortOrder.ASC_NULLS_LAST)
                    .limit(1).singleOrNull()
                    ?.toCoinWithCatalogueNumbers()
            }
        }

        CoinCollectionStats(
            totalCoins = totalCoinsDeferred.await(),
            totalIssuers = totalIssuersDeferred.await(),
            estimatedTotalValueMean = meanValueDeferred.await(),
            highlights = CollectionHighlights(
                mostValuable = mostValuableDeferred.await(),
                mostAncient = mostAncientDeferred.await(),
                rarest = rarestDeferred.await(),
            ),
        )
    }

    override suspend fun reassignFromUser(fromUserId: UUID, toUserId: UUID): Int =
        newSuspendedTransaction {
            CoinsTable.update(
                where = { CoinsTable.userId eq fromUserId },
                body = { it[CoinsTable.userId] = toUserId },
            )
        }

    override suspend fun countByUserId(userId: UUID): Int =
        newSuspendedTransaction {
            CoinsTable.selectAll().where { CoinsTable.userId eq userId }.count().toInt()
        }

    override suspend fun deleteById(id: UUID, userId: UUID): Boolean =
        newSuspendedTransaction {
            CoinsTable.deleteWhere { (CoinsTable.id eq id) and (CoinsTable.userId eq userId) } > 0
        }

    private fun insertCoin(coin: Coin) {
        CoinsTable.insert {
            it[id] = coin.id
            it[userId] = coin.userId
            it[obverseKey] = coin.obverseKey
            it[reverseKey] = coin.reverseKey
            it[notes] = coin.notes
            it[createdAt] = OffsetDateTime.ofInstant(coin.createdAt, ZoneOffset.UTC)
            it[overallConfidence] = coin.recognitionResult.overallConfidence.name
            it[countryOrIssuer] = coin.recognitionResult.countryOrIssuer
            it[denomination] = coin.recognitionResult.denomination
            it[seriesName] = coin.recognitionResult.seriesName
            it[year] = coin.recognitionResult.year
            it[mintMark] = coin.recognitionResult.mintMark
            it[metalComposition] = coin.recognitionResult.metalComposition
            it[estimatedGrade] = coin.recognitionResult.estimatedGrade
            it[estimatedGradeValue] = coin.recognitionResult.estimatedGradeValue
            it[rarityQualitative] = coin.recognitionResult.rarityQualitative
            it[valueLow] = coin.recognitionResult.valueLow?.let { BigDecimal.valueOf(it) }
            it[valueHigh] = coin.recognitionResult.valueHigh?.let { BigDecimal.valueOf(it) }
            it[mintage] = coin.recognitionResult.mintage
            it[obverseDescription] = coin.recognitionResult.obverseDescription
            it[reverseDescription] = coin.recognitionResult.reverseDescription
            it[historicalContext] = coin.recognitionResult.historicalContext
            it[rawJson] = coin.recognitionResult.rawJson
            it[setId] = coin.setId
            it[catalogCoinId] = coin.catalogCoinId
        }
    }

    private fun updateCoin(coin: Coin) {
        CoinsTable.update(
            where = { (CoinsTable.id eq coin.id) and (CoinsTable.userId eq coin.userId) },
            body = {
                it[notes] = coin.notes
                it[overallConfidence] = coin.recognitionResult.overallConfidence.name
                it[countryOrIssuer] = coin.recognitionResult.countryOrIssuer
                it[denomination] = coin.recognitionResult.denomination
                it[seriesName] = coin.recognitionResult.seriesName
                it[year] = coin.recognitionResult.year
                it[mintMark] = coin.recognitionResult.mintMark
                it[metalComposition] = coin.recognitionResult.metalComposition
                it[estimatedGrade] = coin.recognitionResult.estimatedGrade
                it[estimatedGradeValue] = coin.recognitionResult.estimatedGradeValue
                it[rarityQualitative] = coin.recognitionResult.rarityQualitative
                it[valueLow] = coin.recognitionResult.valueLow?.let { BigDecimal.valueOf(it) }
                it[valueHigh] = coin.recognitionResult.valueHigh?.let { BigDecimal.valueOf(it) }
                it[mintage] = coin.recognitionResult.mintage
                it[obverseDescription] = coin.recognitionResult.obverseDescription
                it[reverseDescription] = coin.recognitionResult.reverseDescription
                it[historicalContext] = coin.recognitionResult.historicalContext
                it[rawJson] = coin.recognitionResult.rawJson
                it[setId] = coin.setId
                it[catalogCoinId] = coin.catalogCoinId
            },
        )
    }

    private fun insertCatalogueNumbers(coinId: UUID, numbers: List<CatalogueNumber>) {
        if (numbers.isEmpty()) return
        CoinCatalogueNumbersTable.batchInsert(numbers) { number ->
            this[CoinCatalogueNumbersTable.coinId] = coinId
            this[CoinCatalogueNumbersTable.catalogueName] = number.catalogueName
            this[CoinCatalogueNumbersTable.number] = number.number
            this[CoinCatalogueNumbersTable.confidence] = number.confidence.name
        }
    }

    private fun findCoinById(id: UUID): Coin? {
        val coin = CoinsTable.selectAll().where { CoinsTable.id eq id }.singleOrNull() ?: return null
        val numbers = CoinCatalogueNumbersTable.selectAll()
            .where { CoinCatalogueNumbersTable.coinId eq id }
            .map { it.toCatalogueNumber() }
        return coin.toCoin(numbers)
    }

    private fun findCatalogueNumbersByCoinIds(coinIds: List<UUID>): Map<UUID, List<CatalogueNumber>> {
        if (coinIds.isEmpty()) return emptyMap()
        return CoinCatalogueNumbersTable.selectAll()
            .where { CoinCatalogueNumbersTable.coinId inList coinIds }
            .groupBy { it[CoinCatalogueNumbersTable.coinId] }
            .mapValues { (_, rows) -> rows.map { it.toCatalogueNumber() } }
    }

    private fun ResultRow.toCoinWithCatalogueNumbers(): Coin {
        val coinId = this[CoinsTable.id]
        val numberRows = CoinCatalogueNumbersTable.selectAll()
            .where { CoinCatalogueNumbersTable.coinId eq coinId }
            .map { it.toCatalogueNumber() }
        return toCoin(numberRows)
    }

    private fun ResultRow.toCoin(catalogueNumbers: List<CatalogueNumber>): Coin =
        Coin(
            id = this[CoinsTable.id],
            userId = this[CoinsTable.userId],
            obverseKey = this[CoinsTable.obverseKey],
            reverseKey = this[CoinsTable.reverseKey],
            recognitionResult = RecognitionResult(
                overallConfidence = Confidence.valueOf(this[CoinsTable.overallConfidence].uppercase()),
                countryOrIssuer = this[CoinsTable.countryOrIssuer],
                denomination = this[CoinsTable.denomination],
                seriesName = this[CoinsTable.seriesName],
                year = this[CoinsTable.year],
                mintMark = this[CoinsTable.mintMark],
                metalComposition = this[CoinsTable.metalComposition],
                estimatedGrade = this[CoinsTable.estimatedGrade],
                estimatedGradeValue = this[CoinsTable.estimatedGradeValue],
                rarityQualitative = this[CoinsTable.rarityQualitative],
                valueLow = this[CoinsTable.valueLow]?.toDouble(),
                valueHigh = this[CoinsTable.valueHigh]?.toDouble(),
                mintage = this[CoinsTable.mintage],
                obverseDescription = this[CoinsTable.obverseDescription],
                reverseDescription = this[CoinsTable.reverseDescription],
                historicalContext = this[CoinsTable.historicalContext],
                rawJson = this[CoinsTable.rawJson],
            ),
            catalogueNumbers = catalogueNumbers,
            setId = this[CoinsTable.setId],
            catalogCoinId = this[CoinsTable.catalogCoinId],
            notes = this[CoinsTable.notes],
            createdAt = this[CoinsTable.createdAt].toInstant(),
        )

    private fun ResultRow.toCatalogueNumber(): CatalogueNumber =
        CatalogueNumber(
            catalogueName = this[CoinCatalogueNumbersTable.catalogueName],
            number = this[CoinCatalogueNumbersTable.number],
            confidence = Confidence.valueOf(this[CoinCatalogueNumbersTable.confidence].uppercase()),
        )

    private fun buildFilter(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
    ): Op<Boolean> =
        (CoinsTable.userId eq userId) and
                (if (!country.isNullOrBlank()) CoinsTable.countryOrIssuer eq country else Op.TRUE) and
                (if (year != null) CoinsTable.year eq year else Op.TRUE) and
                (if (minValue != null) CoinsTable.valueLow greaterEq BigDecimal.valueOf(minValue) else Op.TRUE) and
                (if (maxValue != null) CoinsTable.valueHigh lessEq BigDecimal.valueOf(maxValue) else Op.TRUE) and
                (if (setId != null) CoinsTable.setId eq setId else Op.TRUE)

    private fun orderByClauses(sortBy: CoinSortField): List<Pair<Expression<*>, SortOrder>> =
        when (sortBy) {
            CoinSortField.DATE_ADDED_OLD_TO_NEW ->
                listOf(CoinsTable.createdAt to SortOrder.ASC)

            CoinSortField.DATE_ADDED_NEW_TO_OLD ->
                listOf(CoinsTable.createdAt to SortOrder.DESC)

            CoinSortField.RELEASE_YEAR_OLD_TO_NEW ->
                listOf(CoinsTable.year to SortOrder.ASC_NULLS_LAST)

            CoinSortField.RELEASE_YEAR_NEW_TO_OLD ->
                listOf(CoinsTable.year to SortOrder.DESC_NULLS_LAST)

            else -> listOf(CoinsTable.createdAt to SortOrder.DESC)
        }

    private fun sortCoins(coins: List<Coin>, sortBy: CoinSortField): List<Coin> =
        when (sortBy) {
            CoinSortField.VALUE_HIGH_TO_LOW -> coins.sortedByDescending { coin ->
                val low = coin.recognitionResult.valueLow ?: Double.NEGATIVE_INFINITY
                val high = coin.recognitionResult.valueHigh ?: Double.NEGATIVE_INFINITY
                (low + high) / 2.0
            }

            CoinSortField.VALUE_LOW_TO_HIGH -> coins.sortedBy { coin ->
                val low = coin.recognitionResult.valueLow ?: Double.POSITIVE_INFINITY
                val high = coin.recognitionResult.valueHigh ?: Double.POSITIVE_INFINITY
                (low + high) / 2.0
            }

            CoinSortField.RELEASE_YEAR_OLD_TO_NEW -> coins.sortedBy { it.recognitionResult.year ?: Int.MAX_VALUE }
            CoinSortField.RELEASE_YEAR_NEW_TO_OLD -> coins.sortedByDescending {
                it.recognitionResult.year ?: Int.MIN_VALUE
            }

            CoinSortField.DATE_ADDED_OLD_TO_NEW -> coins.sortedBy { it.createdAt }
            CoinSortField.DATE_ADDED_NEW_TO_OLD -> coins.sortedByDescending { it.createdAt }
        }
}
