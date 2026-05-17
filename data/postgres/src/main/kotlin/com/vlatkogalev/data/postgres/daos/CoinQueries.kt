package com.vlatkogalev.data.postgres.daos

import com.vlatkogalev.data.postgres.entities.CatalogueNumberRecord
import com.vlatkogalev.data.postgres.entities.CoinRecord
import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.platform.database.withTransaction
import java.sql.Connection
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

class CoinQueries(
    private val dataSource: DataSource,
) {
    fun <T> withTransaction(block: (Connection) -> T): T = dataSource.withTransaction(block)

    fun insert(connection: Connection, coin: Coin) {
        connection.prepareStatement(
            """
            INSERT INTO coins(
                id, user_id, obverse_key, reverse_key, notes, created_at,
                overall_confidence, country_or_issuer, denomination, series_name, year, mint_mark,
                metal_composition, estimated_grade, estimated_grade_value, rarity_qualitative,
                value_low_usd, value_high_usd, obverse_description, reverse_description, historical_context, raw_json
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, coin.id)
            statement.setObject(2, coin.userId)
            statement.setString(3, coin.obverseKey)
            statement.setString(4, coin.reverseKey)
            statement.setString(5, coin.notes)
            statement.setObject(6, OffsetDateTime.ofInstant(coin.createdAt, ZoneOffset.UTC))
            statement.setString(7, coin.recognitionResult.overallConfidence.name)
            statement.setString(8, coin.recognitionResult.countryOrIssuer)
            statement.setString(9, coin.recognitionResult.denomination)
            statement.setString(10, coin.recognitionResult.seriesName)
            statement.setObject(11, coin.recognitionResult.year)
            statement.setString(12, coin.recognitionResult.mintMark)
            statement.setString(13, coin.recognitionResult.metalComposition)
            statement.setString(14, coin.recognitionResult.estimatedGrade)
            statement.setString(15, coin.recognitionResult.estimatedGradeValue)
            statement.setString(16, coin.recognitionResult.rarityQualitative)
            statement.setObject(17, coin.recognitionResult.valueLowUsd)
            statement.setObject(18, coin.recognitionResult.valueHighUsd)
            statement.setString(19, coin.recognitionResult.obverseDescription)
            statement.setString(20, coin.recognitionResult.reverseDescription)
            statement.setString(21, coin.recognitionResult.historicalContext)
            statement.setString(22, coin.recognitionResult.rawJson)
            statement.executeUpdate()
        }
    }

    fun insertCatalogueNumbers(connection: Connection, coinId: UUID, numbers: List<CatalogueNumber>) {
        if (numbers.isEmpty()) return

        connection.prepareStatement(
            """
            INSERT INTO coin_catalogue_numbers(coin_id, catalogue_name, number, confidence)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            numbers.forEach { number ->
                statement.setObject(1, coinId)
                statement.setString(2, number.catalogueName)
                statement.setString(3, number.number)
                statement.setString(4, number.confidence.name)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    fun findById(id: UUID): CoinRecord? =
        dataSource.connection.use { connection ->
            findById(connection, id)
        }

    fun findById(connection: Connection, id: UUID): CoinRecord? =
        connection.prepareStatement(
            """
            SELECT ${coinColumns()}
            FROM coins
            WHERE id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { rs -> if (rs.next()) rs.toCoinRecord() else null }
        }

    fun findCatalogueNumbersByCoinId(coinId: UUID): List<CatalogueNumberRecord> =
        dataSource.connection.use { connection ->
            findCatalogueNumbersByCoinId(connection, coinId)
        }

    fun findCatalogueNumbersByCoinId(connection: Connection, coinId: UUID): List<CatalogueNumberRecord> =
        connection.prepareStatement(
            """
            SELECT coin_id, catalogue_name, number, confidence
            FROM coin_catalogue_numbers
            WHERE coin_id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, coinId)
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.toCatalogueNumberRecord())
                }
            }
        }

    fun findCatalogueNumbersByCoinIds(coinIds: List<UUID>): Map<UUID, List<CatalogueNumberRecord>> {
        if (coinIds.isEmpty()) return emptyMap()
        val placeholders = coinIds.joinToString(",") { "?" }

        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT coin_id, catalogue_name, number, confidence
                FROM coin_catalogue_numbers
                WHERE coin_id IN ($placeholders)
                """.trimIndent(),
            ).use { statement ->
                coinIds.forEachIndexed { index, id -> statement.setObject(index + 1, id) }
                statement.executeQuery().use { rs ->
                    buildMap<UUID, MutableList<CatalogueNumberRecord>> {
                        while (rs.next()) {
                            val coinId = rs.getObject("coin_id", UUID::class.java)
                            getOrPut(coinId) { mutableListOf() }.add(rs.toCatalogueNumberRecord())
                        }
                    }
                }
            }
        }
    }

    fun findByUserId(
        userId: UUID,
        country: String?,
        year: Int?,
        minValue: Double?,
        maxValue: Double?,
        limit: Int,
        offset: Int,
    ): List<CoinRecord> =
        dataSource.connection.use { connection ->
            val conditions = mutableListOf("user_id = ?")
            val params = mutableListOf<Any>(userId)

            if (!country.isNullOrBlank()) {
                conditions += "country_or_issuer = ?"
                params += country
            }
            if (year != null) {
                conditions += "year = ?"
                params += year
            }
            if (minValue != null) {
                conditions += "value_low_usd >= ?"
                params += minValue
            }
            if (maxValue != null) {
                conditions += "value_high_usd <= ?"
                params += maxValue
            }

            val whereClause = conditions.joinToString(" AND ")
            val sql =
                """
                SELECT ${coinColumns()}
                FROM coins
                WHERE $whereClause
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                var index = 1
                params.forEach { param ->
                    statement.setObject(index++, param)
                }
                statement.setInt(index++, limit)
                statement.setInt(index, offset)
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(rs.toCoinRecord())
                    }
                }
            }
        }

    fun deleteById(id: UUID, userId: UUID): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                DELETE FROM coins
                WHERE id = ? AND user_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, id)
                statement.setObject(2, userId)
                statement.executeUpdate() > 0
            }
        }

    fun update(coin: Coin) {
        dataSource.connection.use { connection ->
            update(connection, coin)
        }
    }

    fun update(connection: Connection, coin: Coin) {
        connection.prepareStatement(
            """
            UPDATE coins
            SET notes = ?,
                overall_confidence = ?,
                country_or_issuer = ?,
                denomination = ?,
                series_name = ?,
                year = ?,
                mint_mark = ?,
                metal_composition = ?,
                estimated_grade = ?,
                estimated_grade_value = ?,
                rarity_qualitative = ?,
                value_low_usd = ?,
                value_high_usd = ?,
                obverse_description = ?,
                reverse_description = ?,
                historical_context = ?,
                raw_json = ?
            WHERE id = ? AND user_id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, coin.notes)
            statement.setString(2, coin.recognitionResult.overallConfidence.name)
            statement.setString(3, coin.recognitionResult.countryOrIssuer)
            statement.setString(4, coin.recognitionResult.denomination)
            statement.setString(5, coin.recognitionResult.seriesName)
            statement.setObject(6, coin.recognitionResult.year)
            statement.setString(7, coin.recognitionResult.mintMark)
            statement.setString(8, coin.recognitionResult.metalComposition)
            statement.setString(9, coin.recognitionResult.estimatedGrade)
            statement.setString(10, coin.recognitionResult.estimatedGradeValue)
            statement.setString(11, coin.recognitionResult.rarityQualitative)
            statement.setObject(12, coin.recognitionResult.valueLowUsd)
            statement.setObject(13, coin.recognitionResult.valueHighUsd)
            statement.setString(14, coin.recognitionResult.obverseDescription)
            statement.setString(15, coin.recognitionResult.reverseDescription)
            statement.setString(16, coin.recognitionResult.historicalContext)
            statement.setString(17, coin.recognitionResult.rawJson)
            statement.setObject(18, coin.id)
            statement.setObject(19, coin.userId)
            statement.executeUpdate()
        }
    }

    fun updateNotes(connection: Connection, id: UUID, userId: UUID, notes: String?): Boolean =
        connection.prepareStatement(
            """
            UPDATE coins
            SET notes = ?
            WHERE id = ? AND user_id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, notes)
            statement.setObject(2, id)
            statement.setObject(3, userId)
            statement.executeUpdate() > 0
        }

    fun getValueStats(userId: UUID): CoinValueStats =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT
                    COUNT(*) AS total_coins,
                    COALESCE(SUM(value_low_usd), 0) AS total_low,
                    COALESCE(SUM(value_high_usd), 0) AS total_high
                FROM coins
                WHERE user_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) return@use CoinValueStats(0, 0.0, 0.0)
                    CoinValueStats(
                        totalCoins = rs.getInt("total_coins"),
                        estimatedTotalValueLowUsd = rs.getBigDecimal("total_low")?.toDouble() ?: 0.0,
                        estimatedTotalValueHighUsd = rs.getBigDecimal("total_high")?.toDouble() ?: 0.0,
                    )
                }
            }
        }

    fun countByCountry(userId: UUID): Map<String, Int> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT
                    COALESCE(country_or_issuer, 'Unknown') AS country,
                    COUNT(*) AS total
                FROM coins
                WHERE user_id = ?
                GROUP BY COALESCE(country_or_issuer, 'Unknown')
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.executeQuery().use { rs ->
                    buildMap {
                        while (rs.next()) {
                            put(rs.getString("country"), rs.getInt("total"))
                        }
                    }
                }
            }
        }

    fun countByYear(userId: UUID): Map<Int, Int> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT year, COUNT(*) AS total
                FROM coins
                WHERE user_id = ? AND year IS NOT NULL
                GROUP BY year
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.executeQuery().use { rs ->
                    buildMap {
                        while (rs.next()) {
                            put(rs.getInt("year"), rs.getInt("total"))
                        }
                    }
                }
            }
        }

    fun countByUserId(userId: UUID): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM coins WHERE user_id = ?").use { statement ->
                statement.setObject(1, userId)
                statement.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }

    private fun ResultSet.toCoinRecord(): CoinRecord =
        CoinRecord(
            id = getObject("id", UUID::class.java),
            userId = getObject("user_id", UUID::class.java),
            obverseKey = getString("obverse_key"),
            reverseKey = getString("reverse_key"),
            notes = getString("notes"),
            createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
            overallConfidence = getString("overall_confidence"),
            countryOrIssuer = getString("country_or_issuer"),
            denomination = getString("denomination"),
            seriesName = getString("series_name"),
            year = getInt("year").takeUnless { wasNull() },
            mintMark = getString("mint_mark"),
            metalComposition = getString("metal_composition"),
            estimatedGrade = getString("estimated_grade"),
            estimatedGradeValue = getString("estimated_grade_value"),
            rarityQualitative = getString("rarity_qualitative"),
            valueLowUsd = getBigDecimal("value_low_usd")?.toDouble(),
            valueHighUsd = getBigDecimal("value_high_usd")?.toDouble(),
            obverseDescription = getString("obverse_description"),
            reverseDescription = getString("reverse_description"),
            historicalContext = getString("historical_context"),
            rawJson = getString("raw_json"),
        )

    private fun ResultSet.toCatalogueNumberRecord(): CatalogueNumberRecord =
        CatalogueNumberRecord(
            coinId = getObject("coin_id", UUID::class.java),
            catalogueName = getString("catalogue_name"),
            number = getString("number"),
            confidence = getString("confidence"),
        )

    private fun coinColumns(): String =
        """
        id,
        user_id,
        obverse_key,
        reverse_key,
        notes,
        created_at,
        overall_confidence,
        country_or_issuer,
        denomination,
        series_name,
        year,
        mint_mark,
        metal_composition,
        estimated_grade,
        estimated_grade_value,
        rarity_qualitative,
        value_low_usd,
        value_high_usd,
        obverse_description,
        reverse_description,
        historical_context,
        raw_json
        """.trimIndent()
}

data class CoinValueStats(
    val totalCoins: Int,
    val estimatedTotalValueLowUsd: Double,
    val estimatedTotalValueHighUsd: Double,
)
