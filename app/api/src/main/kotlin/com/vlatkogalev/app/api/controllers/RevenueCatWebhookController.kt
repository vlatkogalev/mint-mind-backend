@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.RevenueCatWebhookRequest
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.billing.RevenueCatEventType
import com.vlatkogalev.domain.billing.RevenueCatSubscriptionEvent
import com.vlatkogalev.domain.billing.SubscriptionPlan
import com.vlatkogalev.domain.billing.SubscriptionService
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.RevenueCatConfig
import com.vlatkogalev.platform.core.config.loadRevenueCatConfig
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.openapi.describe
import java.time.Instant

class RevenueCatWebhookController(
    private val subscriptionService: SubscriptionService,
    private val timeProvider: TimeProvider,
    config: RevenueCatConfig = loadRevenueCatConfig(),
) {
    private val webhookSecret = config.webhookSecret

    fun Route.registerRoutes() {
        post("/revenuecat") {
            val authorization = call.request.headers[HttpHeaders.Authorization]
            if (!isAuthorized(authorization)) {
                call.respond(HttpStatusCode.Unauthorized, error("Unauthorized"))
                return@post
            }

            val payload = call.receive<RevenueCatWebhookRequest>()
            val event = payload.toDomainEvent()
            if (event == null) {
                call.respond(HttpStatusCode.OK, success(mapOf("message" to "Ignored RevenueCat event")))
                return@post
            }

            when (val result = subscriptionService.handleRevenueCatEvent(event)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, success(mapOf("message" to "Processed RevenueCat event")))
                is Result.Failure -> call.respond(HttpStatusCode.OK, success(mapOf("message" to result.reason)))
            }
        }.describe {
            tag(ApiTags.WEBHOOKS)
            summary = "Handle RevenueCat subscription webhooks"
        }
    }

    private fun RevenueCatWebhookRequest.toDomainEvent(): RevenueCatSubscriptionEvent? {
        val eventType = runCatching { RevenueCatEventType.valueOf(event.type.uppercase()) }.getOrNull() ?: return null
        val customerId = event.appUserId ?: event.originalAppUserId ?: return null
        return RevenueCatSubscriptionEvent(
            revenueCatCustomerId = customerId,
            type = eventType,
            expiresAt = event.expirationAtMillis?.let(Instant::ofEpochMilli),
            plan = event.entitlementId?.toPlan() ?: event.productId?.toPlan(),
        )
    }

    private fun String.toPlan(): SubscriptionPlan? =
        when {
            contains("enterprise", ignoreCase = true) -> SubscriptionPlan.ENTERPRISE
            contains("pro", ignoreCase = true) -> SubscriptionPlan.PRO
            contains("free", ignoreCase = true) -> SubscriptionPlan.FREE
            else -> null
        }

    private fun isAuthorized(authorization: String?): Boolean =
        webhookSecret.isNotBlank() && authorization == "Bearer $webhookSecret"

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
