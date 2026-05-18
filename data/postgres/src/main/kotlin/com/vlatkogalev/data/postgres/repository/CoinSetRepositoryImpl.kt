package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.CoinSetQueries
import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.domain.coin.repository.CoinSetRepository
import java.util.UUID

class CoinSetRepositoryImpl(
    private val queries: CoinSetQueries,
) : CoinSetRepository {
    override fun create(set: CoinSet): CoinSet =
        queries.withTransaction { connection ->
            queries.insert(connection, set)
            queries.findById(connection, set.id) ?: error("Created set could not be loaded")
        }

    override fun findById(id: UUID): CoinSet? = queries.findById(id)

    override fun findByUserId(userId: UUID): List<CoinSet> = queries.findByUserId(userId)

    override fun addCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet? =
        queries.withTransaction { connection ->
            val set = queries.findById(connection, setId) ?: return@withTransaction null
            if (set.userId != userId) return@withTransaction null
            queries.updateSetIdForCoins(connection, coinIds, setId)
            queries.findById(connection, setId)
        }

    override fun removeCoins(setId: UUID, userId: UUID, coinIds: List<UUID>): CoinSet? =
        queries.withTransaction { connection ->
            val set = queries.findById(connection, setId) ?: return@withTransaction null
            if (set.userId != userId) return@withTransaction null
            queries.clearSetIdForCoins(connection, coinIds, setId)
            queries.findById(connection, setId)
        }

    override fun update(setId: UUID, userId: UUID, name: String, description: String?): CoinSet? =
        queries.withTransaction { connection ->
            val updated = queries.update(connection, setId, userId, name, description)
            if (!updated) return@withTransaction null
            queries.findById(connection, setId)
        }

    override fun deleteById(id: UUID, userId: UUID): Boolean =
        queries.withTransaction { connection -> queries.deleteById(connection, id, userId) }
}
