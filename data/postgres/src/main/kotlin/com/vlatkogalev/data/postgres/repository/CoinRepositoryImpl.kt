package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.*
import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.platform.database.dbQuery
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.*
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CoinRepositoryImpl(
    private val database: R2dbcDatabase,
) : CoinRepository {

    override suspend fun save(coin: Coin): Coin =
        dbQuery(database) {
            CoinsTable.insert {
                it[id] = coin.id
                it[userId] = coin.userId
                it[obverseKey] = coin.obverseKey
                it[reverseKey] = coin.reverseKey
                it[setId] = coin.setId
                it[catalogCoinId] = coin.catalogCoinId
                it[notes] = coin.notes
                it[overallConfidence] = coin.recognitionResult.overallConfidence.name
                it[countryOrIssuer] = coin.recognitionResult.countryOrIssuer
                it[denomination] = coin.recognitionResult.denomination
                it[seriesName] = coin.recognitionResult.seriesName
                it[year] = coin.recognitionResult.year
                it[era] = coin.recognitionResult.era
                it[mintMark] = coin.recognitionResult.mintMark
                it[metalComposition] = coin.recognitionResult.metalComposition
                it[estimatedGrade] = coin.recognitionResult.estimatedGrade
                it[estimatedGradeValue] = coin.recognitionResult.estimatedGradeValue
                it[rarityQualitative] = coin.recognitionResult.rarityQualitative
                it[valueLow] = coin.recognitionResult.valueLow
                it[valueHigh] = coin.recognitionResult.valueHigh
                it[mintage] = coin.recognitionResult.mintage
                it[obverseDescription] = coin.recognitionResult.obverseDescription
                it[reverseDescription] = coin.recognitionResult.reverseDescription
                it[weightGrams] = coin.recognitionResult.weightGrams
                it[diameterMm] = coin.recognitionResult.diameterMm
                it[thicknessMm] = coin.recognitionResult.thicknessMm
                it[edge] = coin.recognitionResult.edge
                it[designerObverse] = coin.recognitionResult.designerObverse
                it[designerReverse] = coin.recognitionResult.designerReverse
                it[positiveFeatures] = coin.recognitionResult.positiveFeatures
                it[negativeFeatures] = coin.recognitionResult.negativeFeatures
                it[supplySummary] = coin.recognitionResult.supplySummary
                it[demandSummary] = coin.recognitionResult.demandSummary
                it[obverseLettering] = coin.recognitionResult.obverseLettering
                it[reverseLettering] = coin.recognitionResult.reverseLettering
                it[analysisNotes] = coin.recognitionResult.analysisNotes
                it[historicalContext] = coin.recognitionResult.historicalContext
                it[rawJson] = coin.recognitionResult.rawJson
            }

            coin.catalogueNumbers.forEach { cn ->
                CoinCatalogueNumbersTable.insert {
                    it[id] = UUID.randomUUID()
                    it[coinId] = coin.id
                    it[catalogueName] = cn.catalogueName
                    it[number] = cn.number
                    it[confidence] = cn.confidence.name
                }
            }

            coin
        }

    override suspend fun findById(id: UUID): Coin? =
        dbQuery(database) {
            val row = CoinsTable
                .selectAll()
                .where { CoinsTable.id eq id }
                .firstOrNull()
                ?: return@dbQuery null

            val catalogueNumbers = CoinCatalogueNumbersTable
                .selectAll()
                .where { CoinCatalogueNumbersTable.coinId eq id }
                .toList()
                .map { it.toCatalogueNumber() }

            row.toCoin(catalogueNumbers)
        }

    override suspend fun updateNotes(coinId: UUID, userId: UUID, notes: String?): Coin? =
        dbQuery(database) {
            val updated = CoinsTable.update({
                (CoinsTable.id eq coinId) and (CoinsTable.userId eq userId)
            }) {
                it[CoinsTable.notes] = notes
            }
            if (updated == 0) return@dbQuery null
            findById(coinId)
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
        beforeTimestamp: Long?,
    ): List<Coin> =
        dbQuery(database) {
            val effectiveLimit = limit.coerceIn(1, 100)
            var query = CoinsTable
                .selectAll()
                .where { CoinsTable.userId eq userId }

            country?.let { query = query.andWhere { CoinsTable.countryOrIssuer eq it } }
            year?.let { query = query.andWhere { CoinsTable.year eq it } }
            minValue?.let { query = query.andWhere { CoinsTable.valueHigh greaterEq it } }
            maxValue?.let { query = query.andWhere { CoinsTable.valueLow lessEq it } }
            setId?.let {
                query = query.andWhere { CoinsTable.setId eq it }
            } ?: run {
            }

            query = when (sortBy) {
                CoinSortField.VALUE_HIGH_TO_LOW -> query.orderBy(CoinsTable.valueHigh to SortOrder.DESC)
                CoinSortField.VALUE_LOW_TO_HIGH -> query.orderBy(CoinsTable.valueLow to SortOrder.ASC)
                CoinSortField.RELEASE_YEAR_OLD_TO_NEW -> query.orderBy(CoinsTable.year to SortOrder.ASC)
                CoinSortField.RELEASE_YEAR_NEW_TO_OLD -> query.orderBy(CoinsTable.year to SortOrder.DESC)
                CoinSortField.DATE_ADDED_OLD_TO_NEW -> query.orderBy(CoinsTable.createdAt to SortOrder.ASC)
                CoinSortField.DATE_ADDED_NEW_TO_OLD -> query.orderBy(CoinsTable.createdAt to SortOrder.DESC)
            }

            beforeTimestamp?.let {
                query = query.andWhere {
                    CoinsTable.createdAt less
                        OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
                }
            }

            val rows = query.limit(effectiveLimit).toList()
            rows.map { row ->
                val catalogueNumbers = CoinCatalogueNumbersTable
                    .selectAll()
                    .where { CoinCatalogueNumbersTable.coinId eq row[CoinsTable.id] }
                    .toList()
                    .map { it.toCatalogueNumber() }
                row.toCoin(catalogueNumbers)
            }
        }

    override suspend fun getCollectionStats(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
    ): CoinCollectionStats =
        dbQuery(database) {
            var query = CoinsTable
                .selectAll()
                .where { CoinsTable.userId eq userId }

            country?.let { query = query.andWhere { CoinsTable.countryOrIssuer eq it } }
            year?.let { query = query.andWhere { CoinsTable.year eq it } }
            minValue?.let { query = query.andWhere { CoinsTable.valueHigh greaterEq it } }
            maxValue?.let { query = query.andWhere { CoinsTable.valueLow lessEq it } }
            setId?.let { query = query.andWhere { CoinsTable.setId eq it } }

            val rows = query.toList()
            val totalCoins = rows.size
            val totalIssuers = rows.mapNotNull { it[CoinsTable.countryOrIssuer] }.distinct().size
            val coinIds = rows.map { it[CoinsTable.id] }

            val values = rows.mapNotNull { row ->
                val low = row[CoinsTable.valueLow]
                val high = row[CoinsTable.valueHigh]
                if (low != null && high != null) (low + high) / 2.0 else null
            }
            val estimatedTotalValueMean = if (values.isNotEmpty()) values.sum() / values.size else 0.0

            val highlights = buildHighlights(userId, country, year, minValue, maxValue, setId)

            CoinCollectionStats(
                totalCoins = totalCoins,
                totalIssuers = totalIssuers,
                estimatedTotalValueMean = estimatedTotalValueMean,
                highlights = highlights,
            )
        }

    override suspend fun reassignFromUser(fromUserId: UUID, toUserId: UUID): Int =
        dbQuery(database) {
            CoinsTable.update({ CoinsTable.userId eq fromUserId }) {
                it[userId] = toUserId
            }
        }

    override suspend fun countByUserId(userId: UUID): Int =
        dbQuery(database) {
            CoinsTable
                .selectAll()
                .where { CoinsTable.userId eq userId }
                .count()
                .toInt()
        }

    override suspend fun deleteById(coinId: UUID, userId: UUID): Boolean =
        dbQuery(database) {
            CoinsTable.deleteWhere {
                (CoinsTable.id eq coinId) and (CoinsTable.userId eq userId)
            } > 0
        }

    private suspend fun buildHighlights(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        setId: UUID?,
    ): CollectionHighlights =
        dbQuery(database) {
            var mostValuableRow = CoinsTable
                .selectAll()
                .where { CoinsTable.userId eq userId }
            country?.let { mostValuableRow = mostValuableRow.andWhere { CoinsTable.countryOrIssuer eq it } }
            year?.let { mostValuableRow = mostValuableRow.andWhere { CoinsTable.year eq it } }
            minValue?.let { mostValuableRow = mostValuableRow.andWhere { CoinsTable.valueHigh greaterEq it } }
            maxValue?.let { mostValuableRow = mostValuableRow.andWhere { CoinsTable.valueLow lessEq it } }
            setId?.let { mostValuableRow = mostValuableRow.andWhere { CoinsTable.setId eq it } }
            val mostValuable = mostValuableRow
                .orderBy(CoinsTable.valueHigh to SortOrder.DESC)
                .limit(1)
                .firstOrNull()

            var mostAncientRow = CoinsTable
                .selectAll()
                .where { CoinsTable.userId eq userId }
            country?.let { mostAncientRow = mostAncientRow.andWhere { CoinsTable.countryOrIssuer eq it } }
            year?.let { mostAncientRow = mostAncientRow.andWhere { CoinsTable.year eq it } }
            minValue?.let { mostAncientRow = mostAncientRow.andWhere { CoinsTable.valueHigh greaterEq it } }
            maxValue?.let { mostAncientRow = mostAncientRow.andWhere { CoinsTable.valueLow lessEq it } }
            setId?.let { mostAncientRow = mostAncientRow.andWhere { CoinsTable.setId eq it } }
            val mostAncient = mostAncientRow
                .orderBy(CoinsTable.year to SortOrder.ASC)
                .limit(1)
                .firstOrNull()

            var rarestRow = CoinsTable
                .selectAll()
                .where { CoinsTable.userId eq userId }
            country?.let { rarestRow = rarestRow.andWhere { CoinsTable.countryOrIssuer eq it } }
            year?.let { rarestRow = rarestRow.andWhere { CoinsTable.year eq it } }
            minValue?.let { rarestRow = rarestRow.andWhere { CoinsTable.valueHigh greaterEq it } }
            maxValue?.let { rarestRow = rarestRow.andWhere { CoinsTable.valueLow lessEq it } }
            setId?.let { rarestRow = rarestRow.andWhere { CoinsTable.setId eq it } }
            val rarest = rarestRow
                .orderBy(CoinsTable.mintage to SortOrder.ASC, CoinsTable.mintage to SortOrder.ASC_NULLS_LAST)
                .limit(1)
                .firstOrNull()
                ?.takeIf { it[CoinsTable.mintage] != null }

            CollectionHighlights(
                mostValuable = mostValuable?.toCoin(emptyList()),
                mostAncient = mostAncient?.toCoin(emptyList()),
                rarest = rarest?.toCoin(emptyList()),
            )
        }

    private fun ResultRow.toCoin(catalogueNumbers: List<CatalogueNumber>): Coin =
        Coin(
            id = this[CoinsTable.id],
            userId = this[CoinsTable.userId],
            obverseKey = this[CoinsTable.obverseKey],
            reverseKey = this[CoinsTable.reverseKey],
            recognitionResult = RecognitionResult(
                overallConfidence = Confidence.valueOf(this[CoinsTable.overallConfidence]),
                countryOrIssuer = this[CoinsTable.countryOrIssuer],
                denomination = this[CoinsTable.denomination],
                seriesName = this[CoinsTable.seriesName],
                year = this[CoinsTable.year],
                era = this[CoinsTable.era],
                mintMark = this[CoinsTable.mintMark],
                metalComposition = this[CoinsTable.metalComposition],
                estimatedGrade = this[CoinsTable.estimatedGrade],
                estimatedGradeValue = this[CoinsTable.estimatedGradeValue],
                rarityQualitative = this[CoinsTable.rarityQualitative],
                valueLow = this[CoinsTable.valueLow],
                valueHigh = this[CoinsTable.valueHigh],
                mintage = this[CoinsTable.mintage],
                obverseDescription = this[CoinsTable.obverseDescription],
                reverseDescription = this[CoinsTable.reverseDescription],
                weightGrams = this[CoinsTable.weightGrams],
                diameterMm = this[CoinsTable.diameterMm],
                thicknessMm = this[CoinsTable.thicknessMm],
                edge = this[CoinsTable.edge],
                designerObverse = this[CoinsTable.designerObverse],
                designerReverse = this[CoinsTable.designerReverse],
                positiveFeatures = this[CoinsTable.positiveFeatures],
                negativeFeatures = this[CoinsTable.negativeFeatures],
                supplySummary = this[CoinsTable.supplySummary],
                demandSummary = this[CoinsTable.demandSummary],
                obverseLettering = this[CoinsTable.obverseLettering],
                reverseLettering = this[CoinsTable.reverseLettering],
                analysisNotes = this[CoinsTable.analysisNotes],
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
            confidence = Confidence.valueOf(this[CoinCatalogueNumbersTable.confidence]),
        )
}
