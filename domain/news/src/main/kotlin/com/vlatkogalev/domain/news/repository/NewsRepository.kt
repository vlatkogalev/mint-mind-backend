package com.vlatkogalev.domain.news.repository

import com.vlatkogalev.domain.news.model.NewsArticle
import java.util.UUID

interface NewsRepository {
    fun existingGuids(guids: Collection<String>): Set<String>

    fun saveAll(articles: List<NewsArticle>)

    fun findById(id: UUID): NewsArticle?

    fun findAll(limit: Int = 20, offset: Int = 0): List<NewsArticle>

    fun countAll(): Int
}