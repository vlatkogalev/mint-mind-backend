package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewsArticleResponse(
    val id: String,
    val title: String,
    val link: String,
    val description: String,
    val content: String,
    val author: String?,
    val imageUrl: String?,
    val publishedAt: String,   // ISO-8601
)

@Serializable
data class NewsListResponse(
    val articles: List<NewsArticleResponse>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)