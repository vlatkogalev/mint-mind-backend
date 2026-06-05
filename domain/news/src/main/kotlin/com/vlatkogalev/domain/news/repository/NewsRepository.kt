package com.vlatkogalev.domain.news.repository

import com.vlatkogalev.domain.news.model.NewsArticle
import java.util.UUID

interface NewsRepository {
    suspend fun existingGuids(guids: Collection<String>): Set<String>
    suspend fun saveAll(articles: List<NewsArticle>)
    suspend fun findById(id: UUID): NewsArticle?
    suspend fun findAll(limit: Int = 20, beforeTimestamp: Long? = null): List<NewsArticle>
}
