@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.MarketplaceListingResponse
import com.vlatkogalev.app.api.dto.MarketplaceListingsResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*

class MarketplaceController(
    private val marketplaceRepository: MarketplaceRepository,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerRoutes() {
        get("/listings") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val cursor = call.request.queryParameters["cursor"]?.toLongOrNull()

            try {
                val listings = marketplaceRepository.findPage(limit = limit, beforeTimestamp = cursor)
                val nextCursor = if (listings.isNotEmpty() && listings.size >= limit) {
                    listings.last().lastSeenAt.toEpochMilli()
                } else null

                call.respond(
                    success(
                        MarketplaceListingsResponse(
                            listings = listings.map { it.toResponse() },
                            nextCursor = nextCursor,
                        ),
                    ),
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, error(e.message ?: "Failed to fetch listings"))
            }
        }.describe {
            tag(ApiTags.MARKETPLACE)
            summary = "List live eBay coin listings"
        }
    }

    private fun MarketplaceListing.toResponse(): MarketplaceListingResponse =
        MarketplaceListingResponse(
            id = id.toString(),
            ebayItemId = ebayItemId,
            title = title,
            price = price,
            currency = currency,
            condition = condition,
            listingUrl = listingUrl,
            imageUrl = imageUrl,
            buyingOptions = buyingOptions,
            expiresAt = expiresAt?.toString(),
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
