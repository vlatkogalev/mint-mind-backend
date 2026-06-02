package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.domain.news.model.NewsArticle
import com.vlatkogalev.domain.news.repository.NewsRepository
import com.vlatkogalev.platform.database.tables.NewsArticlesTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class NewsRepositoryImpl : NewsRepository {

    override suspend fun existingGuids(guids: Collection<String>): Set<String> {
        if (guids.isEmpty()) return emptySet()
        return newSuspendedTransaction {
            NewsArticlesTable.select(NewsArticlesTable.guid)
                .where { NewsArticlesTable.guid inList guids.toList() }
                .map { it[NewsArticlesTable.guid] }
                .toSet()
        }
    }

    override suspend fun saveAll(articles: List<NewsArticle>) {
        if (articles.isEmpty()) return
        newSuspendedTransaction {
            articles.chunked(BATCH_SIZE).forEach { chunk ->
                NewsArticlesTable.batchInsert(
                    data = chunk,
                    ignore = true,
                    shouldReturnGeneratedValues = false,
                ) { article ->
                    this[NewsArticlesTable.id] = article.id
                    this[NewsArticlesTable.guid] = article.guid
                    this[NewsArticlesTable.title] = article.title
                    this[NewsArticlesTable.link] = article.link
                    this[NewsArticlesTable.description] = article.description
                    this[NewsArticlesTable.content] = article.content
                    this[NewsArticlesTable.author] = article.author
                    this[NewsArticlesTable.imageUrl] = article.imageUrl
                    this[NewsArticlesTable.publishedAt] = OffsetDateTime.ofInstant(article.publishedAt, ZoneOffset.UTC)
                    this[NewsArticlesTable.fetchedAt] = OffsetDateTime.ofInstant(article.fetchedAt, ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun findById(id: UUID): NewsArticle? =
        newSuspendedTransaction {
            NewsArticlesTable.selectAll()
                .where { NewsArticlesTable.id eq id }
                .singleOrNull()
                ?.toNewsArticle()
        }

    override suspend fun findAll(limit: Int, offset: Int): List<NewsArticle> =
        newSuspendedTransaction {
            NewsArticlesTable.selectAll()
                .orderBy(NewsArticlesTable.publishedAt to SortOrder.DESC)
                .limit(limit).offset(offset.toLong())
                .map { it.toNewsArticle() }
        }

    override suspend fun countAll(): Int =
        newSuspendedTransaction {
            NewsArticlesTable.selectAll().count().toInt()
        }

    private fun ResultRow.toNewsArticle() = NewsArticle(
        id = this[NewsArticlesTable.id],
        guid = this[NewsArticlesTable.guid],
        title = this[NewsArticlesTable.title],
        link = this[NewsArticlesTable.link],
        description = this[NewsArticlesTable.description],
        content = this[NewsArticlesTable.content],
        author = this[NewsArticlesTable.author],
        imageUrl = this[NewsArticlesTable.imageUrl],
        publishedAt = this[NewsArticlesTable.publishedAt].toInstant(),
        fetchedAt = this[NewsArticlesTable.fetchedAt].toInstant(),
    )

    companion object {
        private const val BATCH_SIZE = 500
    }
}