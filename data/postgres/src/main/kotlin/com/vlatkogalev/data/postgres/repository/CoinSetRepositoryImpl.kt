package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.domain.coin.repository.CoinSetRepository
import com.vlatkogalev.platform.database.tables.CoinSetsTable
import com.vlatkogalev.platform.database.tables.CoinsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CoinSetRepositoryImpl : CoinSetRepository {
    override suspend fun create(set: CoinSet): CoinSet =
        newSuspendedTransaction {
            CoinSetsTable.insert {
                it[id] = set.id
                it[userId] = set.userId
                it[name] = set.name
                it[description] = set.description
                it[createdAt] = OffsetDateTime.ofInstant(set.createdAt, ZoneOffset.UTC)
            }
            findCoinSetById(set.id) ?: error("Created set could not be loaded")
        }

    override suspend fun findById(id: UUID): CoinSet? = newSuspendedTransaction { findCoinSetById(id) }

    override suspend fun findByUserId(userId: UUID): List<CoinSet> =
        newSuspendedTransaction {
            CoinSetsTable.selectAll()
                .where { CoinSetsTable.userId eq userId }
                .orderBy(CoinSetsTable.createdAt to SortOrder.DESC)
                .map { it.toCoinSet() }
        }

    override suspend fun addCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet? =
        newSuspendedTransaction {
            val set = findCoinSetById(setId) ?: return@newSuspendedTransaction null
            if (set.userId != userId) return@newSuspendedTransaction null
            if (coinIds.isNotEmpty()) {
                CoinsTable.update(
                    where = {
                        (CoinsTable.id inList coinIds) and (CoinsTable.userId eq userId)
                    },
                    body = { it[CoinsTable.setId] = setId },
                )
            }
            findCoinSetById(setId)
        }

    override suspend fun removeCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet? =
        newSuspendedTransaction {
            val set = findCoinSetById(setId) ?: return@newSuspendedTransaction null
            if (set.userId != userId) return@newSuspendedTransaction null
            if (coinIds.isNotEmpty()) {
                CoinsTable.update(
                    where = {
                        (CoinsTable.id inList coinIds) and (CoinsTable.setId eq setId)
                    },
                    body = { it[CoinsTable.setId] = null },
                )
            }
            findCoinSetById(setId)
        }

    override suspend fun update(setId: UUID, userId: UUID, name: String, description: String?): CoinSet? =
        newSuspendedTransaction {
            val updated = CoinSetsTable.update(
                where = { (CoinSetsTable.id eq setId) and (CoinSetsTable.userId eq userId) },
                body = {
                    it[CoinSetsTable.name] = name
                    it[CoinSetsTable.description] = description
                },
            ) > 0
            if (!updated) return@newSuspendedTransaction null
            findCoinSetById(setId)
        }

    override suspend fun deleteById(id: UUID, userId: UUID): Boolean =
        newSuspendedTransaction {
            CoinSetsTable.deleteWhere { (CoinSetsTable.id eq id) and (CoinSetsTable.userId eq userId) } > 0
        }

    private fun findCoinSetById(id: UUID): CoinSet? =
        CoinSetsTable.selectAll().where { CoinSetsTable.id eq id }.singleOrNull()?.toCoinSet()

    private fun ResultRow.toCoinSet(): CoinSet {
        val setId = this[CoinSetsTable.id]
        val coinIds = CoinsTable.select(CoinsTable.id)
            .where { CoinsTable.setId eq setId }
            .orderBy(CoinsTable.createdAt to SortOrder.DESC)
            .map { it[CoinsTable.id] }
        val previewObverseKeys = CoinsTable.select(CoinsTable.obverseKey)
            .where { CoinsTable.setId eq setId }
            .orderBy(CoinsTable.createdAt to SortOrder.DESC)
            .limit(5)
            .map { it[CoinsTable.obverseKey] }

        return CoinSet(
            id = setId,
            userId = this[CoinSetsTable.userId],
            name = this[CoinSetsTable.name],
            description = this[CoinSetsTable.description],
            coinIds = coinIds,
            previewObverseKeys = previewObverseKeys,
            createdAt = this[CoinSetsTable.createdAt].toInstant(),
        )
    }
}
