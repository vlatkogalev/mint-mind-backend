@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.NewsArticleResponse
import com.vlatkogalev.app.api.dto.NewsListResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.news.model.NewsArticle
import com.vlatkogalev.domain.news.repository.NewsRepository
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import java.util.UUID

class NewsController(
    private val newsRepository: NewsRepository,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerPublicRoutes() {
        get {
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20)
                .coerceIn(1, 100)
            val offset = (call.request.queryParameters["offset"]?.toIntOrNull() ?: 0)
                .coerceAtLeast(0)

            val articles = newsRepository.findAll(limit, offset)
            val total = newsRepository.countAll()

            call.respond(
                success(
                    NewsListResponse(
                        articles = articles.map { it.toResponse() },
                        total = total,
                        limit = limit,
                        offset = offset,
                    ),
                ),
            )
        }.describe {
            tag(ApiTags.NEWS)
            summary = "List latest news articles"
        }

        get("/{id}") {
            val id = call.parameters["id"]
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid article id"))
                return@get
            }

            val article = newsRepository.findById(id)
            if (article == null) {
                call.respond(HttpStatusCode.NotFound, error("Article not found"))
                return@get
            }

            call.respond(success(article.toResponse()))
        }.describe {
            tag(ApiTags.NEWS)
            summary = "Get a single news article by id"
        }
    }

    private fun NewsArticle.toResponse() = NewsArticleResponse(
        id = id.toString(),
        title = title,
        link = link,
        description = description,
        content = content,
        author = author,
        imageUrl = imageUrl,
        publishedAt = publishedAt.toString(),
    )

    private fun <T> success(data: T): ApiResponse<T> = ApiResponse(
        success = true,
        data = data,
        timestampMillis = timeProvider.nowMillis(),
    )

    private fun error(message: String): ApiResponse<Unit> = ApiResponse(
        success = false,
        error = message,
        timestampMillis = timeProvider.nowMillis(),
    )
}