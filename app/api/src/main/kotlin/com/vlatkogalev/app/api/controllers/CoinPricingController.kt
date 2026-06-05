@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.ActiveListingResponse
import com.vlatkogalev.app.api.dto.CoinPricingResponse
import com.vlatkogalev.app.api.dto.PriceRangeResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.coin.service.CoinService
import com.vlatkogalev.domain.pricing.model.ActiveListing
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.domain.pricing.model.PriceRange
import com.vlatkogalev.domain.pricing.service.CoinPricingService
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.time.TimeProvider
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
    private val timeProvider: TimeProvider,
) {
    fun Route.registerRoutes() {
        get("/{id}/pricing") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin ID"))
                return@get
            }

            when (val coinResult = coinService.getCoin(coinId, userId)) {
                is Result.Success -> {
                    when (val pricingResult = pricingService.getPricing(coinResult.value)) {
                        is Result.Success -> call.respond(success(pricingResult.value.toResponse()))
                        is Result.Failure -> call.respond(HttpStatusCode.ServiceUnavailable, error(pricingResult.reason))
                    }
                }
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(coinResult.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get active eBay listings and price estimate for a coin"
        }
    }

    private fun io.ktor.server.application.ApplicationCall.userUuidOrNull(): UUID? =
        principal<JWTPrincipal>()?.userIdOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }

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
