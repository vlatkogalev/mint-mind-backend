package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.CatalogCoinCatalogueNumbersTable
import com.vlatkogalev.data.postgres.tables.CatalogCoinObverseDesignersTable
import com.vlatkogalev.data.postgres.tables.CatalogCoinReverseDesignersTable
import com.vlatkogalev.data.postgres.tables.CatalogCoinTagsTable
import com.vlatkogalev.data.postgres.tables.CatalogCoinsTable
import com.vlatkogalev.data.postgres.tables.ExternalCoinReferencesTable
import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.Confidence
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
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

class CatalogCoinRepositoryImpl(
    private val database: R2dbcDatabase,
) : CatalogCoinRepository {

    override suspend fun findByFingerprint(fingerprint: CoinFingerprint): CatalogCoin? =
        dbQuery(database) {
            val normalized = fingerprint.normalized()
            val coin = CatalogCoinsTable
                .selectAll()
                .where { fingerprintOp(normalized) }
                .firstOrNull()
                ?.toCatalogCoin()
            coin?.let { populateDetails(it) }
        }

    override suspend fun findByRetrievalKey(country: String?, denomination: String?, year: Int?): List<CatalogCoin> =
        dbQuery(database) {
            val coins = CatalogCoinsTable
                .selectAll()
                .where {
                    retrievalKeyOp(country, CatalogCoinsTable.countryOrIssuer) and
                        retrievalKeyOp(denomination, CatalogCoinsTable.denomination) and
                        if (year != null) CatalogCoinsTable.year eq year
                        else CatalogCoinsTable.year.isNull()
                }
                .toList()
                .map { it.toCatalogCoin() }
            populateDetailsBatch(coins)
        }

    override suspend fun findById(id: UUID): CatalogCoin? =
        dbQuery(database) {
            val coin = CatalogCoinsTable
                .selectAll()
                .where { CatalogCoinsTable.id eq id }
                .firstOrNull()
                ?.toCatalogCoin()
            coin?.let { populateDetails(it) }
        }

    override suspend fun findByProviderExternalId(provider: String, externalId: String): CatalogCoin? =
        dbQuery(database) {
            val ref = ExternalCoinReferencesTable
                .selectAll()
                .where {
                    (ExternalCoinReferencesTable.provider eq provider) and
                        (ExternalCoinReferencesTable.externalId eq externalId)
                }
                .firstOrNull()
                ?: return@dbQuery null
            val coin = CatalogCoinsTable
                .selectAll()
                .where { CatalogCoinsTable.id eq ref[ExternalCoinReferencesTable.catalogCoinId] }
                .firstOrNull()
                ?.toCatalogCoin()
            coin?.let { populateDetails(it) }
        }

    override suspend fun save(catalogCoin: CatalogCoin): CatalogCoin =
        dbQuery(database) {
            val normalized = catalogCoin.fingerprint.normalized()
            try {
                CatalogCoinsTable.insert {
                    it[id] = catalogCoin.id
                    it[countryOrIssuer] = normalized.countryOrIssuer
                    it[denomination] = normalized.denomination
                    it[seriesName] = normalized.seriesName
                    it[year] = normalized.year
                    it[mintMark] = normalized.mintMark
                    it[composition] = catalogCoin.composition
                    it[weightGrams] = catalogCoin.weightGrams
                    it[diameterMm] = catalogCoin.diameterMm
                    it[obverseDescription] = catalogCoin.obverseDescription
                    it[reverseDescription] = catalogCoin.reverseDescription
                    it[historicalContext] = catalogCoin.historicalContext
                    it[thumbnailUrl] = catalogCoin.thumbnailUrl
                    it[numistaUrl] = catalogCoin.numistaUrl
                    it[minYear] = catalogCoin.minYear
                    it[maxYear] = catalogCoin.maxYear
                    it[thicknessMm] = catalogCoin.thicknessMm
                    it[shape] = catalogCoin.shape
                    it[technique] = catalogCoin.technique
                    it[orientation] = catalogCoin.orientation
                    it[edgeDescription] = catalogCoin.edgeDescription
                    it[obverseLettering] = catalogCoin.obverseLettering
                    it[reverseLettering] = catalogCoin.reverseLettering
                    it[obversePictureUrl] = catalogCoin.obversePictureUrl
                    it[reversePictureUrl] = catalogCoin.reversePictureUrl
                    it[reverseThumbnailUrl] = catalogCoin.reverseThumbnailUrl
                    it[objectType] = catalogCoin.objectType
                    it[demonetized] = catalogCoin.demonetized
                    it[ruler] = catalogCoin.ruler
                    it[mintName] = catalogCoin.mintName
                    it[enrichedAt] = catalogCoin.enrichedAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                    it[lastEnrichmentAttemptAt] = catalogCoin.lastEnrichmentAttemptAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                    it[lastEnrichmentFailedAt] = catalogCoin.lastEnrichmentFailedAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                    it[lastEnrichmentError] = catalogCoin.lastEnrichmentError
                    it[title] = catalogCoin.title
                }
            } catch (_: Exception) {
                CatalogCoinsTable.update({ fingerprintOp(normalized) }) {
                    it[composition] = catalogCoin.composition
                    it[weightGrams] = catalogCoin.weightGrams
                    it[diameterMm] = catalogCoin.diameterMm
                    it[obverseDescription] = catalogCoin.obverseDescription
                    it[reverseDescription] = catalogCoin.reverseDescription
                    it[historicalContext] = catalogCoin.historicalContext
                    it[thumbnailUrl] = catalogCoin.thumbnailUrl
                    it[numistaUrl] = catalogCoin.numistaUrl
                    it[minYear] = catalogCoin.minYear
                    it[maxYear] = catalogCoin.maxYear
                    it[thicknessMm] = catalogCoin.thicknessMm
                    it[shape] = catalogCoin.shape
                    it[technique] = catalogCoin.technique
                    it[orientation] = catalogCoin.orientation
                    it[edgeDescription] = catalogCoin.edgeDescription
                    it[obverseLettering] = catalogCoin.obverseLettering
                    it[reverseLettering] = catalogCoin.reverseLettering
                    it[obversePictureUrl] = catalogCoin.obversePictureUrl
                    it[reversePictureUrl] = catalogCoin.reversePictureUrl
                    it[reverseThumbnailUrl] = catalogCoin.reverseThumbnailUrl
                    it[objectType] = catalogCoin.objectType
                    it[demonetized] = catalogCoin.demonetized
                    it[ruler] = catalogCoin.ruler
                    it[mintName] = catalogCoin.mintName
                    it[enrichedAt] = catalogCoin.enrichedAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                    it[lastEnrichmentAttemptAt] = catalogCoin.lastEnrichmentAttemptAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                    it[lastEnrichmentFailedAt] = catalogCoin.lastEnrichmentFailedAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                    it[lastEnrichmentError] = catalogCoin.lastEnrichmentError
                    it[title] = catalogCoin.title
                }
            }
            writeJoinTables(catalogCoin)
            findByFingerprint(normalized)!!
        }

    override suspend fun markEnrichmentSuccess(
        catalogCoinId: UUID,
        now: Instant,
        candidate: CoinCatalogCandidate?,
    ): CatalogCoin? =
        dbQuery(database) {
            CatalogCoinsTable.update({ CatalogCoinsTable.id eq catalogCoinId }) {
                it[enrichedAt] = OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
                it[lastEnrichmentAttemptAt] = OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
                it[lastEnrichmentFailedAt] = null
                it[lastEnrichmentError] = null
                candidate?.let { c ->
                    it[title] = c.title
                    it[composition] = c.composition
                    it[weightGrams] = c.weightGrams
                    it[diameterMm] = c.diameterMm
                    it[obverseDescription] = c.obverseDescription
                    it[reverseDescription] = c.reverseDescription
                    it[historicalContext] = c.historicalContext
                    it[thumbnailUrl] = c.thumbnailUrl
                    it[numistaUrl] = c.numistaUrl
                    it[thicknessMm] = c.thicknessMm
                    it[shape] = c.shape
                    it[technique] = c.technique
                    it[orientation] = c.orientation
                    it[edgeDescription] = c.edgeDescription
                    it[obverseLettering] = c.obverseLettering
                    it[reverseLettering] = c.reverseLettering
                    it[obversePictureUrl] = c.obversePictureUrl
                    it[reversePictureUrl] = c.reversePictureUrl
                    it[reverseThumbnailUrl] = c.reverseThumbnailUrl
                    it[objectType] = c.objectType
                    it[demonetized] = c.demonetized
                    it[ruler] = c.ruler
                    it[mintName] = c.mintName
                }
            }
            if (candidate != null) {
                deleteJoinTables(catalogCoinId)
                writeJoinTablesFromCandidate(catalogCoinId, candidate)
            }
            findById(catalogCoinId)
        }

    override suspend fun markEnrichmentFailed(catalogCoinId: UUID, now: Instant, error: String?): CatalogCoin? =
        dbQuery(database) {
            CatalogCoinsTable.update({ CatalogCoinsTable.id eq catalogCoinId }) {
                it[lastEnrichmentAttemptAt] = OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
                it[lastEnrichmentFailedAt] = OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
                it[lastEnrichmentError] = error
            }
            findById(catalogCoinId)
        }

    override suspend fun saveExternalReference(reference: ExternalCoinReference): ExternalCoinReference =
        dbQuery(database) {
            try {
                ExternalCoinReferencesTable.insert {
                    it[id] = reference.id
                    it[catalogCoinId] = reference.catalogCoinId
                    it[provider] = reference.provider
                    it[externalId] = reference.externalId
                    it[externalUrl] = reference.externalUrl
                    it[lastSyncedAt] = reference.lastSyncedAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                    it[syncStatus] = reference.syncStatus
                    it[syncError] = reference.syncError
                }
            } catch (_: Exception) {
                ExternalCoinReferencesTable.update({
                    (ExternalCoinReferencesTable.provider eq reference.provider) and
                        (ExternalCoinReferencesTable.externalId eq reference.externalId)
                }) {
                    it[catalogCoinId] = reference.catalogCoinId
                    it[externalUrl] = reference.externalUrl
                    it[lastSyncedAt] = reference.lastSyncedAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                    it[syncStatus] = reference.syncStatus
                    it[syncError] = reference.syncError
                }
            }
            reference
        }

    override suspend fun findExternalReference(catalogCoinId: UUID, provider: String): ExternalCoinReference? =
        dbQuery(database) {
            ExternalCoinReferencesTable
                .selectAll()
                .where {
                    (ExternalCoinReferencesTable.catalogCoinId eq catalogCoinId) and
                        (ExternalCoinReferencesTable.provider eq provider)
                }
                .firstOrNull()
                ?.toExternalCoinReference()
        }

    override suspend fun findOrCreateExternalReference(
        provider: String,
        externalId: String,
        catalogCoin: CatalogCoin,
        now: Instant,
    ): CatalogCoin = dbQuery(database) {
        try {
            ExternalCoinReferencesTable.insert {
                it[id] = UUID.randomUUID()
                it[catalogCoinId] = catalogCoin.id
                it[this.provider] = provider
                it[this.externalId] = externalId
                it[lastSyncedAt] = OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
                it[syncStatus] = "synced"
            }
        } catch (_: Exception) {
        }
        findByProviderExternalId(provider, externalId) ?: catalogCoin
    }

    private suspend fun populateDetails(coin: CatalogCoin): CatalogCoin =
        populateDetailsBatch(listOf(coin)).first()

    private suspend fun populateDetailsBatch(coins: List<CatalogCoin>): List<CatalogCoin> {
        if (coins.isEmpty()) return coins
        val ids = coins.map { it.id }.toSet()

        val obverseDesigners = dbQuery(database) {
            CatalogCoinObverseDesignersTable
                .selectAll()
                .where { CatalogCoinObverseDesignersTable.catalogCoinId inList ids }
                .orderBy(CatalogCoinObverseDesignersTable.position)
                .toList()
                .groupBy({ it[CatalogCoinObverseDesignersTable.catalogCoinId] }) {
                    it[CatalogCoinObverseDesignersTable.designer]
                }
        }

        val reverseDesigners = dbQuery(database) {
            CatalogCoinReverseDesignersTable
                .selectAll()
                .where { CatalogCoinReverseDesignersTable.catalogCoinId inList ids }
                .orderBy(CatalogCoinReverseDesignersTable.position)
                .toList()
                .groupBy({ it[CatalogCoinReverseDesignersTable.catalogCoinId] }) {
                    it[CatalogCoinReverseDesignersTable.designer]
                }
        }

        val tags = dbQuery(database) {
            CatalogCoinTagsTable
                .selectAll()
                .where { CatalogCoinTagsTable.catalogCoinId inList ids }
                .toList()
                .groupBy({ it[CatalogCoinTagsTable.catalogCoinId] }) {
                    it[CatalogCoinTagsTable.tag]
                }
        }

        val catalogueNumbers = dbQuery(database) {
            CatalogCoinCatalogueNumbersTable
                .selectAll()
                .where { CatalogCoinCatalogueNumbersTable.catalogCoinId inList ids }
                .toList()
                .groupBy({ it[CatalogCoinCatalogueNumbersTable.catalogCoinId] }) { row ->
                    CatalogueNumber(
                        catalogueName = row[CatalogCoinCatalogueNumbersTable.catalogueName],
                        number = row[CatalogCoinCatalogueNumbersTable.number],
                        confidence = Confidence.valueOf(row[CatalogCoinCatalogueNumbersTable.confidence]),
                    )
                }
        }

        return coins.map { coin ->
            coin.copy(
                obverseDesigners = obverseDesigners[coin.id] ?: emptyList(),
                reverseDesigners = reverseDesigners[coin.id] ?: emptyList(),
                tags = tags[coin.id] ?: emptyList(),
                catalogReferences = catalogueNumbers[coin.id] ?: emptyList(),
            )
        }
    }

    private suspend fun writeJoinTables(catalogCoin: CatalogCoin) {
        catalogCoin.obverseDesigners.forEachIndexed { index, designer ->
            CatalogCoinObverseDesignersTable.insert {
                it[id] = UUID.randomUUID()
                it[CatalogCoinObverseDesignersTable.catalogCoinId] = catalogCoin.id
                it[CatalogCoinObverseDesignersTable.designer] = designer
                it[position] = index
            }
        }
        catalogCoin.reverseDesigners.forEachIndexed { index, designer ->
            CatalogCoinReverseDesignersTable.insert {
                it[id] = UUID.randomUUID()
                it[CatalogCoinReverseDesignersTable.catalogCoinId] = catalogCoin.id
                it[CatalogCoinReverseDesignersTable.designer] = designer
                it[position] = index
            }
        }
        catalogCoin.tags.forEach { tag ->
            CatalogCoinTagsTable.insert {
                it[id] = UUID.randomUUID()
                it[CatalogCoinTagsTable.catalogCoinId] = catalogCoin.id
                it[CatalogCoinTagsTable.tag] = tag
            }
        }
        catalogCoin.catalogReferences.forEach { ref ->
            CatalogCoinCatalogueNumbersTable.insert {
                it[id] = UUID.randomUUID()
                it[CatalogCoinCatalogueNumbersTable.catalogCoinId] = catalogCoin.id
                it[catalogueName] = ref.catalogueName
                it[number] = ref.number
                it[confidence] = ref.confidence.name
            }
        }
    }

    private suspend fun writeJoinTablesFromCandidate(catalogCoinId: UUID, candidate: CoinCatalogCandidate) {
        candidate.obverseDesigners.forEachIndexed { index, designer ->
            CatalogCoinObverseDesignersTable.insert {
                it[id] = UUID.randomUUID()
                it[CatalogCoinObverseDesignersTable.catalogCoinId] = catalogCoinId
                it[CatalogCoinObverseDesignersTable.designer] = designer
                it[position] = index
            }
        }
        candidate.reverseDesigners.forEachIndexed { index, designer ->
            CatalogCoinReverseDesignersTable.insert {
                it[id] = UUID.randomUUID()
                it[CatalogCoinReverseDesignersTable.catalogCoinId] = catalogCoinId
                it[CatalogCoinReverseDesignersTable.designer] = designer
                it[position] = index
            }
        }
        candidate.tags.forEach { tag ->
            CatalogCoinTagsTable.insert {
                it[id] = UUID.randomUUID()
                it[CatalogCoinTagsTable.catalogCoinId] = catalogCoinId
                it[CatalogCoinTagsTable.tag] = tag
            }
        }
        candidate.catalogReferences.forEach { ref ->
            CatalogCoinCatalogueNumbersTable.insert {
                it[id] = UUID.randomUUID()
                it[CatalogCoinCatalogueNumbersTable.catalogCoinId] = catalogCoinId
                it[catalogueName] = ref.catalogueName
                it[number] = ref.number
                it[confidence] = ref.confidence.name
            }
        }
    }

    private suspend fun deleteJoinTables(catalogCoinId: UUID) {
        CatalogCoinObverseDesignersTable.deleteWhere {
            CatalogCoinObverseDesignersTable.catalogCoinId eq catalogCoinId
        }
        CatalogCoinReverseDesignersTable.deleteWhere {
            CatalogCoinReverseDesignersTable.catalogCoinId eq catalogCoinId
        }
        CatalogCoinTagsTable.deleteWhere {
            CatalogCoinTagsTable.catalogCoinId eq catalogCoinId
        }
        CatalogCoinCatalogueNumbersTable.deleteWhere {
            CatalogCoinCatalogueNumbersTable.catalogCoinId eq catalogCoinId
        }
    }

    private fun fingerprintOp(normalized: CoinFingerprint): Op<Boolean> {
        var op = if (normalized.countryOrIssuer != null)
            CatalogCoinsTable.countryOrIssuer eq normalized.countryOrIssuer
        else
            CatalogCoinsTable.countryOrIssuer.isNull()
        op = op and (if (normalized.denomination != null)
            CatalogCoinsTable.denomination eq normalized.denomination
        else
            CatalogCoinsTable.denomination.isNull())
        op = op and (if (normalized.seriesName != null)
            CatalogCoinsTable.seriesName eq normalized.seriesName
        else
            CatalogCoinsTable.seriesName.isNull())
        op = op and (if (normalized.year != null)
            CatalogCoinsTable.year eq normalized.year
        else
            CatalogCoinsTable.year.isNull())
        op = op and (if (normalized.mintMark != null)
            CatalogCoinsTable.mintMark eq normalized.mintMark
        else
            CatalogCoinsTable.mintMark.isNull())
        return op
    }

    private fun retrievalKeyOp(value: String?, column: Column<String?>): Op<Boolean> =
        if (value != null) column eq value
        else column.isNull()

    private fun ResultRow.toCatalogCoin(): CatalogCoin =
        CatalogCoin(
            id = this[CatalogCoinsTable.id],
            fingerprint = CoinFingerprint(
                countryOrIssuer = this[CatalogCoinsTable.countryOrIssuer],
                denomination = this[CatalogCoinsTable.denomination],
                seriesName = this[CatalogCoinsTable.seriesName],
                year = this[CatalogCoinsTable.year],
                mintMark = this[CatalogCoinsTable.mintMark],
            ),
            title = this[CatalogCoinsTable.title],
            composition = this[CatalogCoinsTable.composition],
            weightGrams = this[CatalogCoinsTable.weightGrams],
            diameterMm = this[CatalogCoinsTable.diameterMm],
            obverseDescription = this[CatalogCoinsTable.obverseDescription],
            reverseDescription = this[CatalogCoinsTable.reverseDescription],
            historicalContext = this[CatalogCoinsTable.historicalContext],
            thumbnailUrl = this[CatalogCoinsTable.thumbnailUrl],
            numistaUrl = this[CatalogCoinsTable.numistaUrl],
            minYear = this[CatalogCoinsTable.minYear],
            maxYear = this[CatalogCoinsTable.maxYear],
            thicknessMm = this[CatalogCoinsTable.thicknessMm],
            shape = this[CatalogCoinsTable.shape],
            technique = this[CatalogCoinsTable.technique],
            orientation = this[CatalogCoinsTable.orientation],
            edgeDescription = this[CatalogCoinsTable.edgeDescription],
            obverseLettering = this[CatalogCoinsTable.obverseLettering],
            reverseLettering = this[CatalogCoinsTable.reverseLettering],
            obversePictureUrl = this[CatalogCoinsTable.obversePictureUrl],
            reversePictureUrl = this[CatalogCoinsTable.reversePictureUrl],
            reverseThumbnailUrl = this[CatalogCoinsTable.reverseThumbnailUrl],
            objectType = this[CatalogCoinsTable.objectType],
            demonetized = this[CatalogCoinsTable.demonetized],
            ruler = this[CatalogCoinsTable.ruler],
            mintName = this[CatalogCoinsTable.mintName],
            enrichedAt = this[CatalogCoinsTable.enrichedAt]?.toInstant(),
            lastEnrichmentAttemptAt = this[CatalogCoinsTable.lastEnrichmentAttemptAt]?.toInstant(),
            lastEnrichmentFailedAt = this[CatalogCoinsTable.lastEnrichmentFailedAt]?.toInstant(),
            lastEnrichmentError = this[CatalogCoinsTable.lastEnrichmentError],
            createdAt = this[CatalogCoinsTable.createdAt].toInstant(),
            updatedAt = this[CatalogCoinsTable.updatedAt].toInstant(),
        )

    private fun ResultRow.toExternalCoinReference(): ExternalCoinReference =
        ExternalCoinReference(
            id = this[ExternalCoinReferencesTable.id],
            catalogCoinId = this[ExternalCoinReferencesTable.catalogCoinId],
            provider = this[ExternalCoinReferencesTable.provider],
            externalId = this[ExternalCoinReferencesTable.externalId],
            externalUrl = this[ExternalCoinReferencesTable.externalUrl],
            lastSyncedAt = this[ExternalCoinReferencesTable.lastSyncedAt]?.toInstant(),
            syncStatus = this[ExternalCoinReferencesTable.syncStatus],
            syncError = this[ExternalCoinReferencesTable.syncError],
            createdAt = this[ExternalCoinReferencesTable.createdAt].toInstant(),
        )
}
