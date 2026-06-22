@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.ActiveListingResponse
import com.vlatkogalev.app.api.dto.CoinPricingResponse
import com.vlatkogalev.app.api.dto.PriceRangeResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.app.api.util.toErrorResponse
import com.vlatkogalev.domain.coin.service.CoinService
import com.vlatkogalev.domain.pricing.model.ActiveListing
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.domain.pricing.model.PriceRange
import com.vlatkogalev.domain.pricing.service.CoinPricingService
import com.vlatkogalev.platform.auth.userUuidOrNull
import com.vlatkogalev.platform.core.ErrorResponse
import com.vlatkogalev.platform.core.Result
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import java.util.*

class CoinPricingController(
    private val coinService: CoinService,
    private val pricingService: CoinPricingService,
) {
    fun Route.registerRoutes() {
        get("/{id}/pricing") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@get
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid coin ID"))
                return@get
            }

            when (val coinResult = coinService.getCoin(coinId, userId)) {
                is Result.Success -> {
                    when (val pricingResult = pricingService.getPricing(coinResult.value)) {
                        is Result.Success -> call.respond(pricingResult.value.toResponse())
                        is Result.Failure -> call.respond(HttpStatusCode.ServiceUnavailable, pricingResult.error.toErrorResponse())
                    }
                }
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, coinResult.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get active eBay listings and price estimate for a coin"
        }
    }

    private fun CoinPricingResult.toResponse(): CoinPricingResponse =
        CoinPricingResponse(
            query = query,
            listings = listings.map { it.toResponse() },
            priceRange = priceRange?.toResponse(),
            source = source,
            fetchedAt = fetchedAt.toEpochMilli(),
        )

    private fun ActiveListing.toResponse(): ActiveListingResponse =
        ActiveListingResponse(
            title = title,
            currentPrice = currentPrice,
            currency = currency,
            condition = condition,
            listingUrl = listingUrl,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            listingEndDate = listingEndDate?.toEpochMilli(),
            buyingOptions = buyingOptions,
        )

    private fun PriceRange.toResponse(): PriceRangeResponse =
        PriceRangeResponse(
            low = low,
            high = high,
            median = median,
            mean = mean,
            sampleSize = sampleSize,
        )
}
