@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.MarketplaceListingResponse
import com.vlatkogalev.app.api.dto.MarketplaceListingsResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import com.vlatkogalev.platform.core.ErrorResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*

class MarketplaceController(
    private val marketplaceRepository: MarketplaceRepository,
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
                    MarketplaceListingsResponse(
                        listings = listings.map { it.toResponse() },
                        nextCursor = nextCursor,
                    ),
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("INTERNAL_ERROR", e.message ?: "Failed to fetch listings"),
                )
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
            thumbnailUrl = thumbnailUrl,
            buyingOptions = buyingOptions,
            expiresAt = expiresAt?.toEpochMilli(),
            timestamp = lastSeenAt.toEpochMilli(),
        )
}
