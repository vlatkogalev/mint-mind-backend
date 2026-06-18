package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.EnrichmentAttemptsTable
import com.vlatkogalev.domain.coin.model.EnrichmentAttempt
import com.vlatkogalev.domain.coin.repository.EnrichmentAttemptsRepository
import com.vlatkogalev.platform.database.dbQuery
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.*
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class EnrichmentAttemptsRepositoryImpl(
    private val database: R2dbcDatabase,
) : EnrichmentAttemptsRepository {

    override suspend fun findByHash(hash: String): EnrichmentAttempt? =
        dbQuery(database) {
            EnrichmentAttemptsTable
                .selectAll()
                .where { EnrichmentAttemptsTable.fingerprintHash eq hash }
                .firstOrNull()
                ?.toEnrichmentAttempt()
        }

    override suspend fun upsert(hash: String, retrievalKey: String, result: String, pipelineVersion: Int): EnrichmentAttempt =
        dbQuery(database) {
            val now = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            val existing = EnrichmentAttemptsTable
                .selectAll()
                .where { EnrichmentAttemptsTable.fingerprintHash eq hash }
                .firstOrNull()
            if (existing != null) {
                EnrichmentAttemptsTable.update({ EnrichmentAttemptsTable.fingerprintHash eq hash }) {
                    it[lastAttemptAt] = now
                    it[lastResult] = result
                    it[EnrichmentAttemptsTable.pipelineVersion] = pipelineVersion
                }
            } else {
                EnrichmentAttemptsTable.insert {
                    it[fingerprintHash] = hash
                    it[EnrichmentAttemptsTable.retrievalKey] = retrievalKey
                    it[lastAttemptAt] = now
                    it[lastResult] = result
                    it[EnrichmentAttemptsTable.pipelineVersion] = pipelineVersion
                }
            }
            findByHash(hash)!!
        }

    private fun ResultRow.toEnrichmentAttempt(): EnrichmentAttempt =
        EnrichmentAttempt(
            fingerprintHash = this[EnrichmentAttemptsTable.fingerprintHash],
            retrievalKey = this[EnrichmentAttemptsTable.retrievalKey],
            lastAttemptAt = this[EnrichmentAttemptsTable.lastAttemptAt].toInstant(),
            lastResult = this[EnrichmentAttemptsTable.lastResult],
            pipelineVersion = this[EnrichmentAttemptsTable.pipelineVersion],
        )
}
