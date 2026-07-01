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
                it[confidenceCountry] = coin.recognitionResult.confidenceCountry
                it[confidenceDenomination] = coin.recognitionResult.confidenceDenomination
                it[confidenceSeries] = coin.recognitionResult.confidenceSeries
                it[confidenceYear] = coin.recognitionResult.confidenceYear
                it[confidenceEra] = coin.recognitionResult.confidenceEra
                it[mintMark] = coin.recognitionResult.mintMark
                it[mintMarkStatus] = coin.recognitionResult.mintMarkStatus
                it[mintMarkConfidence] = coin.recognitionResult.mintMarkConfidence
                it[metalComposition] = coin.recognitionResult.metalComposition
                it[gradeName] = coin.recognitionResult.gradeName
                it[gradeAbbreviation] = coin.recognitionResult.gradeAbbreviation
                it[gradeNumeric] = coin.recognitionResult.gradeNumeric
                it[gradeConfidence] = coin.recognitionResult.gradeConfidence
                it[rarityQualitative] = coin.recognitionResult.rarityQualitative
                it[rarityScore] = coin.recognitionResult.rarityScore
                it[valueLow] = coin.recognitionResult.valueLow
                it[valueHigh] = coin.recognitionResult.valueHigh
                it[valueCurrency] = coin.recognitionResult.valueCurrency
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
                it[valueDisclaimer] = coin.recognitionResult.valueDisclaimer
                it[obverseLettering] = coin.recognitionResult.obverseLettering
                it[reverseLettering] = coin.recognitionResult.reverseLettering
                it[analysisNotes] = coin.recognitionResult.analysisNotes
                it[historicalContext] = coin.recognitionResult.historicalContext
                it[obverseVisible] = coin.recognitionResult.obverseVisible
                it[reverseVisible] = coin.recognitionResult.reverseVisible
                it[imageFocus] = coin.recognitionResult.imageFocus
                it[imageLighting] = coin.recognitionResult.imageLighting
                it[imageResolution] = coin.recognitionResult.imageResolution
                it[imageCropping] = coin.recognitionResult.imageCropping
                it[imageIssues] = coin.recognitionResult.imageIssues
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
            var predicate: Op<Boolean> = CoinsTable.userId eq userId
            country?.let { predicate = predicate and (CoinsTable.countryOrIssuer eq it) }
            year?.let { predicate = predicate and (CoinsTable.year eq it) }
            minValue?.let { predicate = predicate and (CoinsTable.valueHigh greaterEq it) }
            maxValue?.let { predicate = predicate and (CoinsTable.valueLow lessEq it) }
            setId?.let { predicate = predicate and (CoinsTable.setId eq it) }

            // Project only the columns needed for the aggregates instead of loading every
            // (wide) coin row into memory.
            val valueRows = CoinsTable
                .select(CoinsTable.valueLow, CoinsTable.valueHigh, CoinsTable.countryOrIssuer)
                .where { predicate }
                .toList()

            val totalCoins = valueRows.size
            val totalIssuers = valueRows.mapNotNull { it[CoinsTable.countryOrIssuer] }.distinct().size

            val values = valueRows.mapNotNull { row ->
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

    override suspend fun updateCatalogCoinId(coinId: UUID, catalogCoinId: UUID): Coin? =
        dbQuery(database) {
            val updated = CoinsTable.update({ CoinsTable.id eq coinId }) {
                it[CoinsTable.catalogCoinId] = catalogCoinId
            }
            if (updated == 0) return@dbQuery null
            findById(coinId)
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
                confidenceCountry = this[CoinsTable.confidenceCountry],
                confidenceDenomination = this[CoinsTable.confidenceDenomination],
                confidenceSeries = this[CoinsTable.confidenceSeries],
                confidenceYear = this[CoinsTable.confidenceYear],
                confidenceEra = this[CoinsTable.confidenceEra],
                mintMark = this[CoinsTable.mintMark],
                mintMarkStatus = this[CoinsTable.mintMarkStatus],
                mintMarkConfidence = this[CoinsTable.mintMarkConfidence],
                metalComposition = this[CoinsTable.metalComposition],
                gradeName = this[CoinsTable.gradeName],
                gradeAbbreviation = this[CoinsTable.gradeAbbreviation],
                gradeNumeric = this[CoinsTable.gradeNumeric],
                gradeConfidence = this[CoinsTable.gradeConfidence],
                rarityQualitative = this[CoinsTable.rarityQualitative],
                rarityScore = this[CoinsTable.rarityScore],
                valueLow = this[CoinsTable.valueLow],
                valueHigh = this[CoinsTable.valueHigh],
                valueCurrency = this[CoinsTable.valueCurrency],
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
                valueDisclaimer = this[CoinsTable.valueDisclaimer],
                obverseLettering = this[CoinsTable.obverseLettering],
                reverseLettering = this[CoinsTable.reverseLettering],
                analysisNotes = this[CoinsTable.analysisNotes],
                historicalContext = this[CoinsTable.historicalContext],
                obverseVisible = this[CoinsTable.obverseVisible],
                reverseVisible = this[CoinsTable.reverseVisible],
                imageFocus = this[CoinsTable.imageFocus],
                imageLighting = this[CoinsTable.imageLighting],
                imageResolution = this[CoinsTable.imageResolution],
                imageCropping = this[CoinsTable.imageCropping],
                imageIssues = this[CoinsTable.imageIssues],
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
