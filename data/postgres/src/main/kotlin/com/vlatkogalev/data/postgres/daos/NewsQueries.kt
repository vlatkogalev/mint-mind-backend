package com.vlatkogalev.data.postgres.daos

import com.vlatkogalev.data.postgres.entities.NewsArticleRecord
import com.vlatkogalev.domain.news.model.NewsArticle
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

class NewsQueries(
    private val dataSource: DataSource,
) {

    fun existingGuids(guids: Collection<String>): Set<String> {
        if (guids.isEmpty()) return emptySet()
        val placeholders = guids.joinToString(",") { "?" }
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT guid FROM news_articles WHERE guid IN ($placeholders)",
            ).use { stmt ->
                guids.forEachIndexed { i, g -> stmt.setString(i + 1, g) }
                stmt.executeQuery().use { rs ->
                    buildSet { while (rs.next()) add(rs.getString("guid")) }
                }
            }
        }
    }

    fun insertAll(articles: List<NewsArticle>) {
        if (articles.isEmpty()) return
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO news_articles
                    (id, guid, title, link, description, content, author, image_url, published_at, fetched_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (guid) DO NOTHING
                """.trimIndent(),
            ).use { stmt ->
                articles.forEach { a ->
                    stmt.setObject(1, a.id)
                    stmt.setString(2, a.guid)
                    stmt.setString(3, a.title)
                    stmt.setString(4, a.link)
                    stmt.setString(5, a.description)
                    stmt.setString(6, a.content)
                    stmt.setString(7, a.author)
                    stmt.setString(8, a.imageUrl)
                    stmt.setObject(9, OffsetDateTime.ofInstant(a.publishedAt, ZoneOffset.UTC))
                    stmt.setObject(10, OffsetDateTime.ofInstant(a.fetchedAt, ZoneOffset.UTC))
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    fun findById(id: UUID): NewsArticleRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT ${columns()} FROM news_articles WHERE id = ?",
            ).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs -> if (rs.next()) rs.toRecord() else null }
            }
        }

    fun findAll(limit: Int, offset: Int): List<NewsArticleRecord> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT ${columns()}
                FROM news_articles
                ORDER BY published_at DESC
                LIMIT ? OFFSET ?
                """.trimIndent(),
            ).use { stmt ->
                stmt.setInt(1, limit)
                stmt.setInt(2, offset)
                stmt.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toRecord()) }
                }
            }
        }

    fun countAll(): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM news_articles").use { stmt ->
                stmt.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }

    private fun columns() =
        "id, guid, title, link, description, content, author, image_url, published_at, fetched_at"

    private fun ResultSet.toRecord() = NewsArticleRecord(
        id = getObject("id", UUID::class.java),
        guid = getString("guid"),
        title = getString("title"),
        link = getString("link"),
        description = getString("description"),
        content = getString("content"),
        author = getString("author"),
        imageUrl = getString("image_url"),
        publishedAt = getObject("published_at", OffsetDateTime::class.java).toInstant(),
        fetchedAt = getObject("fetched_at", OffsetDateTime::class.java).toInstant(),
    )
}