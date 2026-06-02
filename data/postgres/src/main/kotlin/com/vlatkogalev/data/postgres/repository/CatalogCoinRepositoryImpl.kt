package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.CatalogCoinQueries
import com.vlatkogalev.data.postgres.entities.CatalogCoinRecord
import com.vlatkogalev.data.postgres.entities.ExternalCoinReferenceRecord
import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.model.normalized
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.platform.database.withTransaction
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class CatalogCoinRepositoryImpl(
    private val dataSource: DataSource,
    private val queries: CatalogCoinQueries,
) : CatalogCoinRepository {
    override fun findByFingerprint(fingerprint: CoinFingerprint): CatalogCoin? =
        queries.findByFingerprint(fingerprint.normalized())?.toDomain()

    override fun findById(id: UUID): CatalogCoin? = queries.findById(id)?.toDomain()

    override fun findByProviderExternalId(provider: String, externalId: String): CatalogCoin? =
        queries.findByProviderExternalId(provider, externalId)?.toDomain()

    override fun save(catalogCoin: CatalogCoin): CatalogCoin =
        dataSource.withTransaction { connection ->
            queries.upsertCatalogCoin(connection, catalogCoin.toRecord()).toDomain()
        }

    override fun markEnrichmentSuccess(catalogCoinId: UUID, now: Instant): CatalogCoin? =
        dataSource.withTransaction { connection ->
            queries.updateEnrichmentSuccess(connection, catalogCoinId, now)?.toDomain()
        }

    override fun markEnrichmentFailed(catalogCoinId: UUID, now: Instant, error: String?): CatalogCoin? =
        dataSource.withTransaction { connection ->
            queries.updateEnrichmentFailure(connection, catalogCoinId, now, error)?.toDomain()
        }

    override fun saveExternalReference(reference: ExternalCoinReference): ExternalCoinReference =
        dataSource.withTransaction { connection ->
            queries.upsertExternalReference(connection, reference.toRecord()).toDomain()
        }

    override fun findExternalReference(catalogCoinId: UUID, provider: String): ExternalCoinReference? =
        queries.findExternalReference(catalogCoinId, provider)?.toDomain()

    private fun CatalogCoin.toRecord(): CatalogCoinRecord =
        CatalogCoinRecord(
            id = id,
            countryOrIssuer = fingerprint.countryOrIssuer,
            denomination = fingerprint.denomination,
            seriesName = fingerprint.seriesName,
            title = fingerprint.title,
            year = fingerprint.year,
            mintMark = fingerprint.mintMark,
            enrichedAt = enrichedAt,
            lastEnrichmentAttemptAt = lastEnrichmentAttemptAt,
            lastEnrichmentFailedAt = lastEnrichmentFailedAt,
            lastEnrichmentError = lastEnrichmentError,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun CatalogCoinRecord.toDomain(): CatalogCoin =
        CatalogCoin(
            id = id,
            fingerprint = CoinFingerprint(
                countryOrIssuer = countryOrIssuer,
                denomination = denomination,
                seriesName = seriesName,
                title = title,
                year = year,
                mintMark = mintMark,
            ).normalized(),
            enrichedAt = enrichedAt,
            lastEnrichmentAttemptAt = lastEnrichmentAttemptAt,
            lastEnrichmentFailedAt = lastEnrichmentFailedAt,
            lastEnrichmentError = lastEnrichmentError,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun ExternalCoinReference.toRecord(): ExternalCoinReferenceRecord =
        ExternalCoinReferenceRecord(
            id = id,
            catalogCoinId = catalogCoinId,
            provider = provider,
            externalId = externalId,
            externalUrl = externalUrl,
            lastSyncedAt = lastSyncedAt,
            syncStatus = syncStatus,
            syncError = syncError,
            createdAt = createdAt,
        )

    private fun ExternalCoinReferenceRecord.toDomain(): ExternalCoinReference =
        ExternalCoinReference(
            id = id,
            catalogCoinId = catalogCoinId,
            provider = provider,
            externalId = externalId,
            externalUrl = externalUrl,
            lastSyncedAt = lastSyncedAt,
            syncStatus = syncStatus,
            syncError = syncError,
            createdAt = createdAt,
        )
}
