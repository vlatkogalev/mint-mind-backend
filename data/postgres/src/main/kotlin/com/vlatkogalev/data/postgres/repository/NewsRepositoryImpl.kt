package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.NewsQueries
import com.vlatkogalev.data.postgres.entities.NewsArticleRecord
import com.vlatkogalev.domain.news.model.NewsArticle
import com.vlatkogalev.domain.news.repository.NewsRepository
import java.util.UUID

class NewsRepositoryImpl(
    private val queries: NewsQueries,
) : NewsRepository {

    override fun existingGuids(guids: Collection<String>): Set<String> =
        queries.existingGuids(guids)

    override fun saveAll(articles: List<NewsArticle>) =
        queries.insertAll(articles)

    override fun findById(id: UUID): NewsArticle? =
        queries.findById(id)?.toDomain()

    override fun findAll(limit: Int, offset: Int): List<NewsArticle> =
        queries.findAll(limit, offset).map { it.toDomain() }

    override fun countAll(): Int = queries.countAll()

    private fun NewsArticleRecord.toDomain() = NewsArticle(
        id = id,
        guid = guid,
        title = title,
        link = link,
        description = description,
        content = content,
        author = author,
        imageUrl = imageUrl,
        publishedAt = publishedAt,
        fetchedAt = fetchedAt,
    )
}