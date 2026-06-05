@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.NewsArticleResponse
import com.vlatkogalev.app.api.dto.NewsArticleSummaryResponse
import com.vlatkogalev.app.api.dto.NewsListResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.news.model.NewsArticle
import com.vlatkogalev.domain.news.repository.NewsRepository
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import java.util.*

class NewsController(
    private val newsRepository: NewsRepository,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerRoutes() {
        get {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val cursor = call.request.queryParameters["cursor"]?.toLongOrNull()

            try {
                val articles = newsRepository.findAll(
                    limit = limit,
                    beforeTimestamp = cursor,
                )
                val nextCursor = if (articles.isNotEmpty() && articles.size >= limit) {
                    articles.last().publishedAt.toEpochMilli()
                } else null

                call.respond(
                    success(
                        NewsListResponse(
                            articles = articles.map { it.toSummaryResponse() },
                            nextCursor = nextCursor,
                        ),
                    ),
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, error(e.message ?: "Failed to fetch news"))
            }
        }.describe {
            tag(ApiTags.NEWS)
            summary = "List latest news articles"
        }

        get("/{id}") {
            val articleId = call.parameters["id"]
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

            if (articleId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid article ID"))
                return@get
            }

            try {
                val article = newsRepository.findById(articleId)
                if (article == null) {
                    call.respond(HttpStatusCode.NotFound, error("Article not found"))
                } else {
                    call.respond(success(article.toResponse()))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, error(e.message ?: "Failed to fetch article"))
            }
        }.describe {
            tag(ApiTags.NEWS)
            summary = "Get a single news article"
        }
    }

    private fun NewsArticle.toSummaryResponse(): NewsArticleSummaryResponse =
        NewsArticleSummaryResponse(
            id = id.toString(),
            title = title,
            link = link,
            description = description,
            imageUrl = imageUrl,
            publishedAt = publishedAt.toEpochMilli(),
        )

    private fun NewsArticle.toResponse(): NewsArticleResponse =
        NewsArticleResponse(
            id = id.toString(),
            title = title,
            link = link,
            description = description,
            content = content,
            author = author,
            imageUrl = imageUrl,
            publishedAt = publishedAt.toEpochMilli(),
        )

    private fun <T> success(data: T): ApiResponse<T> =
        ApiResponse(
            success = true,
            data = data,
            timestampMillis = timeProvider.nowMillis(),
        )

    private fun error(message: String): ApiResponse<Unit> =
        ApiResponse(
            success = false,
            error = message,
            timestampMillis = timeProvider.nowMillis(),
        )
}
