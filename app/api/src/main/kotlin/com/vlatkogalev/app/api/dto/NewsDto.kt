package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewsArticleSummaryResponse(
    val id: String,
    val title: String,
    val link: String,
    val description: String,
    val imageUrl: String?,
    val publishedAt: String,
)

@Serializable
data class NewsArticleResponse(
    val id: String,
    val title: String,
    val link: String,
    val description: String,
    val content: String,
    val author: String?,
    val imageUrl: String?,
    val publishedAt: String,
)

@Serializable
data class NewsListResponse(
    val articles: List<NewsArticleSummaryResponse>,
    val nextCursor: Long?,
)