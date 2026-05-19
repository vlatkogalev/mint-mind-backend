package com.vlatkogalev.data.postgres.daos

import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.platform.database.withTransaction
import java.sql.Connection
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

class CoinSetQueries(
    private val dataSource: DataSource,
) {
    fun <T> withTransaction(block: (Connection) -> T): T = dataSource.withTransaction(block)

    fun insert(connection: Connection, set: CoinSet) {
        connection.prepareStatement(
            """
            INSERT INTO coin_sets(id, user_id, name, description, created_at)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, set.id)
            statement.setObject(2, set.userId)
            statement.setString(3, set.name)
            statement.setString(4, set.description)
            statement.setObject(5, OffsetDateTime.ofInstant(set.createdAt, ZoneOffset.UTC))
            statement.executeUpdate()
        }
    }

    fun findById(id: UUID): CoinSet? =
        dataSource.connection.use { connection -> findById(connection, id) }

    fun findById(connection: Connection, id: UUID): CoinSet? =
        connection.prepareStatement(
            """
            SELECT ${setColumns()}
            FROM coin_sets
            WHERE id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { rs ->
                if (rs.next()) rs.toCoinSet(connection) else null
            }
        }

    fun findByUserId(userId: UUID): List<CoinSet> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT ${setColumns()}
                FROM coin_sets
                WHERE user_id = ?
                ORDER BY created_at DESC
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(rs.toCoinSet(connection))
                    }
                }
            }
        }

    fun updateSetIdForCoins(connection: Connection, coinIds: List<UUID>, setId: UUID) {
        if (coinIds.isEmpty()) return
        val placeholders = coinIds.joinToString(",") { "?" }
        connection.prepareStatement(
            """
            UPDATE coins
            SET set_id = ?
            WHERE id IN ($placeholders)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, setId)
            coinIds.forEachIndexed { index, coinId -> statement.setObject(index + 2, coinId) }
            statement.executeUpdate()
        }
    }

    fun clearSetIdForCoins(connection: Connection, coinIds: List<UUID>, setId: UUID) {
        if (coinIds.isEmpty()) return
        val placeholders = coinIds.joinToString(",") { "?" }
        connection.prepareStatement(
            """
            UPDATE coins
            SET set_id = NULL
            WHERE set_id = ? AND id IN ($placeholders)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, setId)
            coinIds.forEachIndexed { index, coinId -> statement.setObject(index + 2, coinId) }
            statement.executeUpdate()
        }
    }

    fun update(connection: Connection, setId: UUID, userId: UUID, name: String, description: String?): Boolean =
        connection.prepareStatement(
            """
            UPDATE coin_sets
            SET name = ?, description = ?
            WHERE id = ? AND user_id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, name)
            statement.setString(2, description)
            statement.setObject(3, setId)
            statement.setObject(4, userId)
            statement.executeUpdate() > 0
        }

    fun deleteById(connection: Connection, id: UUID, userId: UUID): Boolean =
        connection.prepareStatement(
            """
            DELETE FROM coin_sets
            WHERE id = ? AND user_id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, id)
            statement.setObject(2, userId)
            statement.executeUpdate() > 0
        }

    private fun findCoinIds(connection: Connection, setId: UUID): List<UUID> =
        connection.prepareStatement(
            """
            SELECT id
            FROM coins
            WHERE set_id = ?
            ORDER BY created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, setId)
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.getObject("id", UUID::class.java))
                }
            }
        }

    private fun findPreviewObverseKeys(connection: Connection, setId: UUID): List<String> =
        connection.prepareStatement(
            """
            SELECT obverse_key
            FROM coins
            WHERE set_id = ?
            ORDER BY created_at DESC
            LIMIT 5
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, setId)
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.getString("obverse_key"))
                }
            }
        }

    private fun ResultSet.toCoinSet(connection: Connection): CoinSet {
        val id = getObject("id", UUID::class.java)
        return CoinSet(
            id = id,
            userId = getObject("user_id", UUID::class.java),
            name = getString("name"),
            description = getString("description"),
            coinIds = findCoinIds(connection, id),
            previewObverseKeys = findPreviewObverseKeys(connection, id),
            createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
        )
    }

    private fun setColumns(): String = "id, user_id, name, description, created_at"
}