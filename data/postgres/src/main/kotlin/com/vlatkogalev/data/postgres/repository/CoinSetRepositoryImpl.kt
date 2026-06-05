package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.CoinSetsTable
import com.vlatkogalev.data.postgres.tables.CoinsTable
import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.domain.coin.repository.CoinSetRepository
import com.vlatkogalev.platform.database.dbQuery
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CoinSetRepositoryImpl(
    private val database: R2dbcDatabase,
) : CoinSetRepository {

    override suspend fun create(set: CoinSet): CoinSet =
        dbQuery(database) {
            CoinSetsTable.insert {
                it[id] = set.id
                it[userId] = set.userId
                it[name] = set.name
                it[description] = set.description
            }
            set
        }

    override suspend fun findById(id: UUID): CoinSet? =
        dbQuery(database) {
            val row = CoinSetsTable
                .selectAll()
                .where { CoinSetsTable.id eq id }
                .firstOrNull()
                ?: return@dbQuery null
            row.toCoinSet()
        }

    override suspend fun findByUserId(userId: UUID): List<CoinSet> =
        dbQuery(database) {
            CoinSetsTable
                .selectAll()
                .where { CoinSetsTable.userId eq userId }
                .orderBy(CoinSetsTable.createdAt to SortOrder.DESC)
                .toList()
                .map { it.toCoinSet() }
        }

    override suspend fun addCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet? =
        dbQuery(database) {
            val set = CoinSetsTable
                .selectAll()
                .where { (CoinSetsTable.id eq setId) and (CoinSetsTable.userId eq userId) }
                .firstOrNull()
                ?: return@dbQuery null

            coinIds.forEach { coinId ->
                CoinsTable.update({
                    (CoinsTable.id eq coinId) and (CoinsTable.userId eq userId)
                }) {
                    it[CoinsTable.setId] = setId
                }
            }

            set.toCoinSet()
        }

    override suspend fun removeCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet? =
        dbQuery(database) {
            val set = CoinSetsTable
                .selectAll()
                .where { (CoinSetsTable.id eq setId) and (CoinSetsTable.userId eq userId) }
                .firstOrNull()
                ?: return@dbQuery null

            coinIds.forEach { coinId ->
                CoinsTable.update({
                    (CoinsTable.id eq coinId) and (CoinsTable.userId eq userId) and (CoinsTable.setId eq setId)
                }) {
                    it[CoinsTable.setId] = null
                }
            }

            set.toCoinSet()
        }

    override suspend fun update(setId: UUID, userId: UUID, name: String, description: String?): CoinSet? =
        dbQuery(database) {
            val updated = CoinSetsTable.update({
                (CoinSetsTable.id eq setId) and (CoinSetsTable.userId eq userId)
            }) {
                it[CoinSetsTable.name] = name
                it[CoinSetsTable.description] = description
            }
            if (updated == 0) return@dbQuery null
            findById(setId)
        }

    override suspend fun deleteById(setId: UUID, userId: UUID): Boolean =
        dbQuery(database) {
            CoinSetsTable.deleteWhere {
                (CoinSetsTable.id eq setId) and (CoinSetsTable.userId eq userId)
            } > 0
        }

    private suspend fun ResultRow.toCoinSet(): CoinSet =
        dbQuery(database) {
            val setId = this@toCoinSet[CoinSetsTable.id]
            val coinIds = CoinsTable
                .selectAll()
                .where { CoinsTable.setId eq setId }
                .orderBy(CoinsTable.createdAt to SortOrder.DESC)
                .toList()
                .map { it[CoinsTable.id] }

            val previewObverseKeys = CoinsTable
                .selectAll()
                .where { CoinsTable.setId eq setId }
                .orderBy(CoinsTable.createdAt to SortOrder.DESC)
                .limit(5)
                .toList()
                .map { it[CoinsTable.obverseKey] }

            CoinSet(
                id = this@toCoinSet[CoinSetsTable.id],
                userId = this@toCoinSet[CoinSetsTable.userId],
                name = this@toCoinSet[CoinSetsTable.name],
                description = this@toCoinSet[CoinSetsTable.description],
                coinIds = coinIds,
                previewObverseKeys = previewObverseKeys,
                createdAt = this@toCoinSet[CoinSetsTable.createdAt].toInstant(),
            )
        }
}
