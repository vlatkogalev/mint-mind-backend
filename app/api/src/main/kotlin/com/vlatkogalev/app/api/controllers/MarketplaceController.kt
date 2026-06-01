@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.MarketplaceListingResponse
import com.vlatkogalev.app.api.dto.MarketplaceListingsResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe

class MarketplaceController(
    private val marketplaceRepository: MarketplaceRepository,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerPublicRoutes() {
        get {
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20)
                .coerceIn(1, 100)
            val cursor = call.request.queryParameters["cursor"]?.toLongOrNull()

            val listings = try {
                marketplaceRepository.findPage(limit, cursor)
            } catch (_: Exception) {
                call.respond(HttpStatusCode.InternalServerError, error("Failed to fetch listings"))
                return@get
            }

            val nextCursor = if (listings.size >= limit) {
                listings.lastOrNull()?.lastSeenAt?.toEpochMilli()
            } else {
                null
            }

            call.respond(
                success(
                    MarketplaceListingsResponse(
                        listings = listings.map { it.toResponse() },
                        nextCursor = nextCursor,
                    ),
                ),
            )
        }.describe {
            tag(ApiTags.MARKETPLACE)
            summary = "List live eBay coin listings"
        }
    }

    private fun MarketplaceListing.toResponse(): MarketplaceListingResponse =
        MarketplaceListingResponse(
            id = id.toString(),
            title = title,
            imageUrl = imageUrl,
            price = price,
            currency = currency,
            itemWebUrl = listingUrl,
            condition = condition,
            buyingOptions = buyingOptions,
            expiresAt = expiresAt?.toEpochMilli(),
            timestamp = lastSeenAt.toEpochMilli(),
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
