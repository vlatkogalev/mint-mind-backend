package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.model.normalized
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.platform.database.tables.CatalogCoinsTable
import com.vlatkogalev.platform.database.tables.ExternalCoinReferencesTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class CatalogCoinRepositoryImpl : CatalogCoinRepository {
    override suspend fun findByFingerprint(fingerprint: CoinFingerprint): CatalogCoin? =
        newSuspendedTransaction {
            CatalogCoinsTable.selectAll()
                .where {
                    val conditions = buildList {
                        if (!fingerprint.countryOrIssuer.isNullOrBlank()) {
                            add(CatalogCoinsTable.countryOrIssuer eq fingerprint.countryOrIssuer)
                        }
                        if (!fingerprint.denomination.isNullOrBlank()) {
                            add(CatalogCoinsTable.denomination eq fingerprint.denomination)
                        }
                        if (!fingerprint.title.isNullOrBlank()) {
                            add(CatalogCoinsTable.title eq fingerprint.title)
                        }
                        if (fingerprint.year != null) {
                            add(CatalogCoinsTable.year eq fingerprint.year)
                        }
                    }
                    conditions.fold(Op.TRUE as Op<Boolean>) { acc, op -> acc and op }
                }
                .orderBy(CatalogCoinsTable.updatedAt to SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?.toCatalogCoin()
        }

    override suspend fun findById(id: UUID): CatalogCoin? =
        newSuspendedTransaction {
            CatalogCoinsTable.selectAll()
                .where { CatalogCoinsTable.id eq id }
                .singleOrNull()
                ?.toCatalogCoin()
        }

    override suspend fun findByIds(ids: List<UUID>): List<CatalogCoin> =
        if (ids.isEmpty()) emptyList()
        else newSuspendedTransaction {
            CatalogCoinsTable.selectAll()
                .where { CatalogCoinsTable.id inList ids }
                .map { it.toCatalogCoin() }
        }

    override suspend fun findByProviderExternalId(provider: String, externalId: String): CatalogCoin? =
        newSuspendedTransaction {
            ExternalCoinReferencesTable
                .innerJoin(CatalogCoinsTable, { ExternalCoinReferencesTable.catalogCoinId }, { CatalogCoinsTable.id })
                .selectAll()
                .where {
                    (ExternalCoinReferencesTable.provider eq provider) and (ExternalCoinReferencesTable.externalId eq externalId)
                }
                .limit(1)
                .singleOrNull()
                ?.toCatalogCoin()
        }

    override suspend fun save(catalogCoin: CatalogCoin): CatalogCoin =
        newSuspendedTransaction {
            val result = CatalogCoinsTable.upsert(
                CatalogCoinsTable.countryOrIssuer,
                CatalogCoinsTable.denomination,
                CatalogCoinsTable.seriesName,
                CatalogCoinsTable.title,
                CatalogCoinsTable.year,
                CatalogCoinsTable.mintMark,
            ) {
                it[id] = catalogCoin.id
                it[countryOrIssuer] = catalogCoin.fingerprint.countryOrIssuer
                it[denomination] = catalogCoin.fingerprint.denomination
                it[seriesName] = catalogCoin.fingerprint.seriesName
                it[title] = catalogCoin.fingerprint.title
                it[year] = catalogCoin.fingerprint.year
                it[mintMark] = catalogCoin.fingerprint.mintMark
                it[composition] = catalogCoin.composition
                it[weightGrams] = catalogCoin.weightGrams?.toBigDecimal()
                it[diameterMm] = catalogCoin.diameterMm?.toBigDecimal()
                it[obverseDescription] = catalogCoin.obverseDescription
                it[reverseDescription] = catalogCoin.reverseDescription
                it[historicalContext] = catalogCoin.historicalContext
                it[thumbnailUrl] = catalogCoin.thumbnailUrl
                it[numistaUrl] = catalogCoin.numistaUrl
                it[enrichedAt] = catalogCoin.enrichedAt?.toOffsetDateTimeUtc()
                it[lastEnrichmentAttemptAt] = catalogCoin.lastEnrichmentAttemptAt?.toOffsetDateTimeUtc()
                it[lastEnrichmentFailedAt] = catalogCoin.lastEnrichmentFailedAt?.toOffsetDateTimeUtc()
                it[lastEnrichmentError] = catalogCoin.lastEnrichmentError
                it[createdAt] = catalogCoin.createdAt.toOffsetDateTimeUtc()
                it[updatedAt] = catalogCoin.updatedAt.toOffsetDateTimeUtc()
            }
            result.resultedValues?.singleOrNull()?.toCatalogCoin()
                ?: CatalogCoinsTable.selectAll().where { CatalogCoinsTable.id eq catalogCoin.id }.single()
                    .toCatalogCoin()
        }

    override suspend fun markEnrichmentSuccess(catalogCoinId: UUID, now: Instant, candidate: CoinCatalogCandidate): CatalogCoin? =
        newSuspendedTransaction {
            CatalogCoinsTable.update(
                where = { CatalogCoinsTable.id eq catalogCoinId },
                body = {
                    it[enrichedAt] = now.toOffsetDateTimeUtc()
                    it[lastEnrichmentAttemptAt] = now.toOffsetDateTimeUtc()
                    it[lastEnrichmentFailedAt] = null
                    it[lastEnrichmentError] = null
                    it[composition] = candidate.composition
                    it[weightGrams] = candidate.weightGrams?.toBigDecimal()
                    it[diameterMm] = candidate.diameterMm?.toBigDecimal()
                    it[obverseDescription] = candidate.obverseDescription
                    it[reverseDescription] = candidate.reverseDescription
                    it[historicalContext] = candidate.historicalContext
                    it[thumbnailUrl] = candidate.thumbnailUrl
                    it[numistaUrl] = candidate.numistaUrl
                },
            )
            CatalogCoinsTable.selectAll().where { CatalogCoinsTable.id eq catalogCoinId }.singleOrNull()
                ?.toCatalogCoin()
        }

    override suspend fun markEnrichmentFailed(catalogCoinId: UUID, now: Instant, error: String?): CatalogCoin? =
        newSuspendedTransaction {
            CatalogCoinsTable.update(
                where = { CatalogCoinsTable.id eq catalogCoinId },
                body = {
                    it[lastEnrichmentAttemptAt] = now.toOffsetDateTimeUtc()
                    it[lastEnrichmentFailedAt] = now.toOffsetDateTimeUtc()
                    it[lastEnrichmentError] = error
                },
            )
            CatalogCoinsTable.selectAll().where { CatalogCoinsTable.id eq catalogCoinId }.singleOrNull()
                ?.toCatalogCoin()
        }

    override suspend fun saveExternalReference(reference: ExternalCoinReference): ExternalCoinReference =
        newSuspendedTransaction {
            val result = ExternalCoinReferencesTable.upsert(
                ExternalCoinReferencesTable.catalogCoinId,
                ExternalCoinReferencesTable.provider,
            ) {
                it[id] = reference.id
                it[catalogCoinId] = reference.catalogCoinId
                it[provider] = reference.provider
                it[externalId] = reference.externalId
                it[externalUrl] = reference.externalUrl
                it[lastSyncedAt] = reference.lastSyncedAt?.toOffsetDateTimeUtc()
                it[syncStatus] = reference.syncStatus
                it[syncError] = reference.syncError
                it[createdAt] = reference.createdAt.toOffsetDateTimeUtc()
            }
            result.resultedValues?.singleOrNull()?.toExternalCoinReference()
                ?: findExternalReferenceById(reference.id) ?: error("Failed to upsert external reference")
        }

    override suspend fun findExternalReference(catalogCoinId: UUID, provider: String): ExternalCoinReference? =
        newSuspendedTransaction {
            ExternalCoinReferencesTable.selectAll()
                .where {
                    (ExternalCoinReferencesTable.catalogCoinId eq catalogCoinId) and
                            (ExternalCoinReferencesTable.provider eq provider)
                }
                .singleOrNull()
                ?.toExternalCoinReference()
        }

    private fun findExternalReferenceById(id: UUID): ExternalCoinReference? =
        ExternalCoinReferencesTable.selectAll()
            .where { ExternalCoinReferencesTable.id eq id }
            .singleOrNull()
            ?.toExternalCoinReference()

    private fun ResultRow.toCatalogCoin(): CatalogCoin =
        CatalogCoin(
            id = this[CatalogCoinsTable.id],
            fingerprint = CoinFingerprint(
                countryOrIssuer = this[CatalogCoinsTable.countryOrIssuer],
                denomination = this[CatalogCoinsTable.denomination],
                seriesName = this[CatalogCoinsTable.seriesName],
                title = this[CatalogCoinsTable.title],
                year = this[CatalogCoinsTable.year],
                mintMark = this[CatalogCoinsTable.mintMark],
            ).normalized(),
            composition = this[CatalogCoinsTable.composition],
            weightGrams = this[CatalogCoinsTable.weightGrams]?.toDouble(),
            diameterMm = this[CatalogCoinsTable.diameterMm]?.toDouble(),
            obverseDescription = this[CatalogCoinsTable.obverseDescription],
            reverseDescription = this[CatalogCoinsTable.reverseDescription],
            historicalContext = this[CatalogCoinsTable.historicalContext],
            thumbnailUrl = this[CatalogCoinsTable.thumbnailUrl],
            numistaUrl = this[CatalogCoinsTable.numistaUrl],
            enrichedAt = this[CatalogCoinsTable.enrichedAt]?.toInstantUtc(),
            lastEnrichmentAttemptAt = this[CatalogCoinsTable.lastEnrichmentAttemptAt]?.toInstantUtcOrNull(),
            lastEnrichmentFailedAt = this[CatalogCoinsTable.lastEnrichmentFailedAt]?.toInstantUtcOrNull(),
            lastEnrichmentError = this[CatalogCoinsTable.lastEnrichmentError],
            createdAt = this[CatalogCoinsTable.createdAt].toInstantUtc(),
            updatedAt = this[CatalogCoinsTable.updatedAt].toInstantUtc(),
        )

    private fun ResultRow.toExternalCoinReference(): ExternalCoinReference =
        ExternalCoinReference(
            id = this[ExternalCoinReferencesTable.id],
            catalogCoinId = this[ExternalCoinReferencesTable.catalogCoinId],
            provider = this[ExternalCoinReferencesTable.provider],
            externalId = this[ExternalCoinReferencesTable.externalId],
            externalUrl = this[ExternalCoinReferencesTable.externalUrl],
            lastSyncedAt = this[ExternalCoinReferencesTable.lastSyncedAt]?.toInstantUtcOrNull(),
            syncStatus = this[ExternalCoinReferencesTable.syncStatus],
            syncError = this[ExternalCoinReferencesTable.syncError],
            createdAt = this[ExternalCoinReferencesTable.createdAt].toInstantUtc(),
        )

    private fun OffsetDateTime?.toInstantUtcOrNull(): Instant? = this?.toInstant()
    private fun OffsetDateTime.toInstantUtc(): Instant = this.toInstant()
    private fun Instant.toOffsetDateTimeUtc(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)
}
