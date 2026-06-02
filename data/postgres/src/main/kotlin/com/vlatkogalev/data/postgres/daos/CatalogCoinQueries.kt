package com.vlatkogalev.data.postgres.daos

import com.vlatkogalev.data.postgres.entities.CatalogCoinRecord
import com.vlatkogalev.data.postgres.entities.ExternalCoinReferenceRecord
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

class CatalogCoinQueries(
    private val dataSource: DataSource,
) {
    fun findById(id: UUID): CatalogCoinRecord? =
        dataSource.connection.use { connection ->
            findById(connection, id)
        }

    fun findById(connection: Connection, id: UUID): CatalogCoinRecord? =
        connection.prepareStatement(
            """
            SELECT ${catalogColumns()}
            FROM catalog_coins
            WHERE id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { rs ->
                if (rs.next()) rs.toCatalogCoinRecord() else null
            }
        }

    fun findByFingerprint(fingerprint: CoinFingerprint): CatalogCoinRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT ${catalogColumns()}
                FROM catalog_coins
                WHERE country_or_issuer IS NOT DISTINCT FROM ?
                  AND denomination IS NOT DISTINCT FROM ?
                  AND title IS NOT DISTINCT FROM ?
                  AND year IS NOT DISTINCT FROM ?
                ORDER BY updated_at DESC
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, fingerprint.countryOrIssuer)
                statement.setString(2, fingerprint.denomination)
                statement.setString(3, fingerprint.title)
                statement.setObject(4, fingerprint.year)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.toCatalogCoinRecord() else null
                }
            }
        }

    fun findByProviderExternalId(provider: String, externalId: String): CatalogCoinRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT ${catalogColumnsAlias("c")}
                FROM external_coin_references r
                INNER JOIN catalog_coins c ON c.id = r.catalog_coin_id
                WHERE r.provider = ? AND r.external_id = ?
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, provider)
                statement.setString(2, externalId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.toCatalogCoinRecord() else null
                }
            }
        }

    fun upsertCatalogCoin(connection: Connection, record: CatalogCoinRecord): CatalogCoinRecord {
        return connection.prepareStatement(
            """
            INSERT INTO catalog_coins(
                id, country_or_issuer, denomination, series_name, title, year, mint_mark,
                enriched_at, last_enrichment_attempt_at, last_enrichment_failed_at, last_enrichment_error,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (
                COALESCE(country_or_issuer, ''),
                COALESCE(denomination, ''),
                COALESCE(series_name, ''),
                COALESCE(title, ''),
                COALESCE(year, -2147483648),
                COALESCE(mint_mark, '')
            )
            DO UPDATE SET
                country_or_issuer = EXCLUDED.country_or_issuer,
                denomination = EXCLUDED.denomination,
                series_name = EXCLUDED.series_name,
                title = EXCLUDED.title,
                year = EXCLUDED.year,
                mint_mark = EXCLUDED.mint_mark,
                enriched_at = EXCLUDED.enriched_at,
                last_enrichment_attempt_at = EXCLUDED.last_enrichment_attempt_at,
                last_enrichment_failed_at = EXCLUDED.last_enrichment_failed_at,
                last_enrichment_error = EXCLUDED.last_enrichment_error
            RETURNING ${catalogColumns()}
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, record.id)
            statement.setString(2, record.countryOrIssuer)
            statement.setString(3, record.denomination)
            statement.setString(4, record.seriesName)
            statement.setString(5, record.title)
            statement.setObject(6, record.year)
            statement.setString(7, record.mintMark)
            statement.setObject(8, record.enrichedAt?.toOffsetDateTimeUtc())
            statement.setObject(9, record.lastEnrichmentAttemptAt?.toOffsetDateTimeUtc())
            statement.setObject(10, record.lastEnrichmentFailedAt?.toOffsetDateTimeUtc())
            statement.setString(11, record.lastEnrichmentError)
            statement.setObject(12, record.createdAt.toOffsetDateTimeUtc())
            statement.setObject(13, record.updatedAt.toOffsetDateTimeUtc())
            statement.executeQuery().use { rs ->
                check(rs.next()) { "Catalog coin upsert returned no row" }
                rs.toCatalogCoinRecord()
            }
        }
    }

    fun updateEnrichmentSuccess(connection: Connection, id: UUID, now: Instant): CatalogCoinRecord? =
        connection.prepareStatement(
            """
            UPDATE catalog_coins
            SET enriched_at = ?,
                last_enrichment_attempt_at = ?,
                last_enrichment_failed_at = NULL,
                last_enrichment_error = NULL
            WHERE id = ?
            RETURNING ${catalogColumns()}
            """.trimIndent(),
        ).use { statement ->
            val nowUtc = now.toOffsetDateTimeUtc()
            statement.setObject(1, nowUtc)
            statement.setObject(2, nowUtc)
            statement.setObject(3, id)
            statement.executeQuery().use { rs ->
                if (rs.next()) rs.toCatalogCoinRecord() else null
            }
        }

    fun updateEnrichmentFailure(connection: Connection, id: UUID, now: Instant, error: String?): CatalogCoinRecord? =
        connection.prepareStatement(
            """
            UPDATE catalog_coins
            SET last_enrichment_attempt_at = ?,
                last_enrichment_failed_at = ?,
                last_enrichment_error = ?
            WHERE id = ?
            RETURNING ${catalogColumns()}
            """.trimIndent(),
        ).use { statement ->
            val nowUtc = now.toOffsetDateTimeUtc()
            statement.setObject(1, nowUtc)
            statement.setObject(2, nowUtc)
            statement.setString(3, error)
            statement.setObject(4, id)
            statement.executeQuery().use { rs ->
                if (rs.next()) rs.toCatalogCoinRecord() else null
            }
        }

    fun findExternalReference(connection: Connection, catalogCoinId: UUID, provider: String): ExternalCoinReferenceRecord? =
        connection.prepareStatement(
            """
            SELECT ${externalColumns()}
            FROM external_coin_references
            WHERE catalog_coin_id = ? AND provider = ?
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, catalogCoinId)
            statement.setString(2, provider)
            statement.executeQuery().use { rs ->
                if (rs.next()) rs.toExternalCoinReferenceRecord() else null
            }
        }

    fun findExternalReference(catalogCoinId: UUID, provider: String): ExternalCoinReferenceRecord? =
        dataSource.connection.use { connection ->
            findExternalReference(connection, catalogCoinId, provider)
        }

    fun upsertExternalReference(connection: Connection, record: ExternalCoinReferenceRecord): ExternalCoinReferenceRecord =
        connection.prepareStatement(
            """
            INSERT INTO external_coin_references(
                id, catalog_coin_id, provider, external_id, external_url,
                last_synced_at, sync_status, sync_error, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (catalog_coin_id, provider)
            DO UPDATE SET
                external_id = EXCLUDED.external_id,
                external_url = EXCLUDED.external_url,
                last_synced_at = EXCLUDED.last_synced_at,
                sync_status = EXCLUDED.sync_status,
                sync_error = EXCLUDED.sync_error
            RETURNING ${externalColumns()}
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, record.id)
            statement.setObject(2, record.catalogCoinId)
            statement.setString(3, record.provider)
            statement.setString(4, record.externalId)
            statement.setString(5, record.externalUrl)
            statement.setObject(6, record.lastSyncedAt?.toOffsetDateTimeUtc())
            statement.setString(7, record.syncStatus)
            statement.setString(8, record.syncError)
            statement.setObject(9, record.createdAt.toOffsetDateTimeUtc())
            statement.executeQuery().use { rs ->
                check(rs.next()) { "External reference upsert returned no row" }
                rs.toExternalCoinReferenceRecord()
            }
        }

    private fun ResultSet.toCatalogCoinRecord(): CatalogCoinRecord =
        CatalogCoinRecord(
            id = getObject("id", UUID::class.java),
            countryOrIssuer = getString("country_or_issuer"),
            denomination = getString("denomination"),
            seriesName = getString("series_name"),
            title = getString("title"),
            year = getInt("year").takeUnless { wasNull() },
            mintMark = getString("mint_mark"),
            enrichedAt = getObject("enriched_at", OffsetDateTime::class.java)?.toInstant(),
            lastEnrichmentAttemptAt = getObject("last_enrichment_attempt_at", OffsetDateTime::class.java)?.toInstant(),
            lastEnrichmentFailedAt = getObject("last_enrichment_failed_at", OffsetDateTime::class.java)?.toInstant(),
            lastEnrichmentError = getString("last_enrichment_error"),
            createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
            updatedAt = getObject("updated_at", OffsetDateTime::class.java).toInstant(),
        )

    private fun ResultSet.toExternalCoinReferenceRecord(): ExternalCoinReferenceRecord =
        ExternalCoinReferenceRecord(
            id = getObject("id", UUID::class.java),
            catalogCoinId = getObject("catalog_coin_id", UUID::class.java),
            provider = getString("provider"),
            externalId = getString("external_id"),
            externalUrl = getString("external_url"),
            lastSyncedAt = getObject("last_synced_at", OffsetDateTime::class.java)?.toInstant(),
            syncStatus = getString("sync_status"),
            syncError = getString("sync_error"),
            createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
        )

    private fun catalogColumns(): String =
        """
        id,
        country_or_issuer,
        denomination,
        series_name,
        title,
        year,
        mint_mark,
        enriched_at,
        last_enrichment_attempt_at,
        last_enrichment_failed_at,
        last_enrichment_error,
        created_at,
        updated_at
        """.trimIndent()

    private fun catalogColumnsAlias(alias: String): String =
        """
        id,
        country_or_issuer,
        denomination,
        series_name,
        title,
        year,
        mint_mark,
        enriched_at,
        last_enrichment_attempt_at,
        last_enrichment_failed_at,
        last_enrichment_error,
        created_at,
        updated_at
        """.trimIndent().lines().joinToString(",\n") { line ->
            val trimmed = line.trim()
            "$alias.$trimmed"
        }

    private fun externalColumns(): String =
        """
        id,
        catalog_coin_id,
        provider,
        external_id,
        external_url,
        last_synced_at,
        sync_status,
        sync_error,
        created_at
        """.trimIndent()
}

private fun Instant.toOffsetDateTimeUtc(): OffsetDateTime = OffsetDateTime.ofInstant(this, java.time.ZoneOffset.UTC)
