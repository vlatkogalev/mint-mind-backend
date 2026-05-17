package com.vlatkogalev.data.postgres.entities

import java.time.Instant
import java.util.UUID

data class NewsArticleRecord(
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