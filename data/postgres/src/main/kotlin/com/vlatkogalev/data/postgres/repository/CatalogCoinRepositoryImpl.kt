package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.CatalogCoinsTable
import com.vlatkogalev.data.postgres.tables.ExternalCoinReferencesTable
import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.platform.database.dbQuery
import kotlinx.coroutines.flow.firstOrNull
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
            CatalogCoinsTable
                .selectAll()
                .where {
                    CatalogCoinsTable.countryOrIssuer eq normalized.countryOrIssuer
                }.andWhere {
                    CatalogCoinsTable.denomination eq normalized.denomination
                }.andWhere {
                    CatalogCoinsTable.seriesName eq normalized.seriesName
                }.andWhere {
                    CatalogCoinsTable.title eq normalized.title
                }.andWhere {
                    CatalogCoinsTable.year eq normalized.year
                }.andWhere {
                    CatalogCoinsTable.mintMark eq normalized.mintMark
                }
                .firstOrNull()
                ?.toCatalogCoin()
        }

    override suspend fun findById(id: UUID): CatalogCoin? =
        dbQuery(database) {
            CatalogCoinsTable
                .selectAll()
                .where { CatalogCoinsTable.id eq id }
                .firstOrNull()
                ?.toCatalogCoin()
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
            CatalogCoinsTable
                .selectAll()
                .where { CatalogCoinsTable.id eq ref[ExternalCoinReferencesTable.catalogCoinId] }
                .firstOrNull()
                ?.toCatalogCoin()
        }

    override suspend fun save(catalogCoin: CatalogCoin): CatalogCoin =
        dbQuery(database) {
            CatalogCoinsTable.insert {
                it[id] = catalogCoin.id
                it[countryOrIssuer] = catalogCoin.fingerprint.countryOrIssuer
                it[denomination] = catalogCoin.fingerprint.denomination
                it[seriesName] = catalogCoin.fingerprint.seriesName
                it[title] = catalogCoin.fingerprint.title
                it[year] = catalogCoin.fingerprint.year
                it[mintMark] = catalogCoin.fingerprint.mintMark
                it[composition] = catalogCoin.composition
                it[weightGrams] = catalogCoin.weightGrams
                it[diameterMm] = catalogCoin.diameterMm
                it[obverseDescription] = catalogCoin.obverseDescription
                it[reverseDescription] = catalogCoin.reverseDescription
                it[historicalContext] = catalogCoin.historicalContext
                it[thumbnailUrl] = catalogCoin.thumbnailUrl
                it[numistaUrl] = catalogCoin.numistaUrl
                it[enrichedAt] = catalogCoin.enrichedAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                it[lastEnrichmentAttemptAt] = catalogCoin.lastEnrichmentAttemptAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                it[lastEnrichmentFailedAt] = catalogCoin.lastEnrichmentFailedAt?.let { at -> OffsetDateTime.ofInstant(at, ZoneOffset.UTC) }
                it[lastEnrichmentError] = catalogCoin.lastEnrichmentError
            }
            catalogCoin
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
                    it[composition] = c.composition
                    it[weightGrams] = c.weightGrams
                    it[diameterMm] = c.diameterMm
                    it[obverseDescription] = c.obverseDescription
                    it[reverseDescription] = c.reverseDescription
                    it[historicalContext] = c.historicalContext
                    it[thumbnailUrl] = c.thumbnailUrl
                    it[numistaUrl] = c.numistaUrl
                }
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
            ),
            composition = this[CatalogCoinsTable.composition],
            weightGrams = this[CatalogCoinsTable.weightGrams],
            diameterMm = this[CatalogCoinsTable.diameterMm],
            obverseDescription = this[CatalogCoinsTable.obverseDescription],
            reverseDescription = this[CatalogCoinsTable.reverseDescription],
            historicalContext = this[CatalogCoinsTable.historicalContext],
            thumbnailUrl = this[CatalogCoinsTable.thumbnailUrl],
            numistaUrl = this[CatalogCoinsTable.numistaUrl],
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
