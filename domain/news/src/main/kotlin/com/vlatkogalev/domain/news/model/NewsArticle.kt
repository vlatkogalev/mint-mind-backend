package com.vlatkogalev.domain.news.model

import java.util.UUID
import java.time.Instant

data class NewsArticle(
    val id: UUID,
    val guid: String,
    val title: String,
    val link: String,
    val description: String,
    val content: String,
    val author: String?,
    val imageUrl: String?,
    val publishedAt: Instant,
    val fetchedAt: Instant,
)
