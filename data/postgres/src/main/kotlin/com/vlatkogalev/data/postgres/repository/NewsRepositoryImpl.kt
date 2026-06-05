package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.NewsArticlesTable
import com.vlatkogalev.domain.news.model.NewsArticle
import com.vlatkogalev.domain.news.repository.NewsRepository
import com.vlatkogalev.platform.database.dbQuery
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.*
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class NewsRepositoryImpl(
    private val database: R2dbcDatabase,
) : NewsRepository {

    override suspend fun existingGuids(guids: Collection<String>): Set<String> =
        dbQuery(database) {
            NewsArticlesTable
                .select(NewsArticlesTable.guid)
                .where { NewsArticlesTable.guid inList guids.toList() }
                .toList()
                .map { it[NewsArticlesTable.guid] }
                .toSet()
        }

    override suspend fun saveAll(articles: List<NewsArticle>) =
        dbQuery(database) {
            articles.forEach { article ->
                try {
                    NewsArticlesTable.insert {
                        it[id] = article.id
                        it[guid] = article.guid
                        it[title] = article.title
                        it[link] = article.link
                        it[description] = article.description
                        it[content] = article.content
                        it[author] = article.author
                        it[imageUrl] = article.imageUrl
                        it[publishedAt] = OffsetDateTime.ofInstant(article.publishedAt, ZoneOffset.UTC)
                        it[fetchedAt] = OffsetDateTime.ofInstant(article.fetchedAt, ZoneOffset.UTC)
                    }
                } catch (_: Exception) {
                }
            }
        }

    override suspend fun findById(id: UUID): NewsArticle? =
        dbQuery(database) {
            NewsArticlesTable
                .selectAll()
                .where { NewsArticlesTable.id eq id }
                .firstOrNull()
                ?.toNewsArticle()
        }

    override suspend fun findAll(limit: Int, beforeTimestamp: Long?): List<NewsArticle> =
        dbQuery(database) {
            val effectiveLimit = limit.coerceIn(1, 100)
            var query = NewsArticlesTable
                .selectAll()
                .orderBy(NewsArticlesTable.publishedAt to SortOrder.DESC)

            beforeTimestamp?.let {
                query = query.andWhere {
                    NewsArticlesTable.publishedAt less
                        OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
                }
            }

            query.limit(effectiveLimit).toList().map { it.toNewsArticle() }
        }

    private fun ResultRow.toNewsArticle(): NewsArticle =
        NewsArticle(
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
}
