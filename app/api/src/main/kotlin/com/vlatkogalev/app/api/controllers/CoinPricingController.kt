@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.CoinPricingResponse
import com.vlatkogalev.app.api.dto.PriceRangeResponse
import com.vlatkogalev.app.api.dto.SoldListingResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.coin.service.CoinService
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.domain.pricing.model.PriceRange
import com.vlatkogalev.domain.pricing.model.SoldListing
import com.vlatkogalev.domain.pricing.service.CoinPricingService
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import java.util.UUID

class CoinPricingController(
    private val coinService: CoinService,
    private val coinPricingService: CoinPricingService,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerProtectedRoutes() {
        get("/{id}/pricing") {
            val userId = call.userUuidOrNull()
            val coinId = call.coinIdOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin id"))
                return@get
            }

            // First verify the coin belongs to this user
            val coinResult = coinService.getCoin(coinId, userId)
            if (coinResult is Result.Failure) {
                call.respond(HttpStatusCode.NotFound, error(coinResult.reason))
                return@get
            }
            val coin = (coinResult as Result.Success).value

            when (val pricingResult = coinPricingService.getPricing(coin)) {
                is Result.Success -> call.respond(success(pricingResult.value.toResponse()))
                is Result.Failure -> call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    error(pricingResult.reason),
                )
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get recent eBay sold listings and price estimate for a coin"
        }
    }

    private fun ApplicationCall.userUuidOrNull(): UUID? =
        principal<JWTPrincipal>()?.userIdOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun ApplicationCall.coinIdOrNull(): UUID? =
        parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun CoinPricingResult.toResponse(): CoinPricingResponse =
        CoinPricingResponse(
            query = query,
            recentSales = recentSales.map { it.toResponse() },
            priceRange = priceRange?.toResponse(),
            source = source,
            fetchedAt = fetchedAt.toString(),
        )

    private fun SoldListing.toResponse(): SoldListingResponse =
        SoldListingResponse(
            title = title,
            soldPrice = soldPrice,
            currency = currency,
            soldAt = soldAt.toString(),
            condition = condition,
            listingUrl = listingUrl,
            imageUrl = imageUrl,
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