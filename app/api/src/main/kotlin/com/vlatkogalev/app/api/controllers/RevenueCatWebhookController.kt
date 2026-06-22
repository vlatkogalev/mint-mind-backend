@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.RevenueCatWebhookRequest
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.billing.model.RevenueCatEventType
import com.vlatkogalev.domain.billing.model.RevenueCatSubscriptionEvent
import com.vlatkogalev.domain.billing.model.SubscriptionPlan
import com.vlatkogalev.domain.billing.service.SubscriptionService
import com.vlatkogalev.platform.core.ErrorResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.RevenueCatConfig
import com.vlatkogalev.platform.core.config.loadRevenueCatConfig
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import java.time.Instant
import java.util.UUID

class RevenueCatWebhookController(
    private val subscriptionService: SubscriptionService,
    config: RevenueCatConfig = loadRevenueCatConfig(),
) {
    private val webhookSecret = config.webhookSecret

    fun Route.registerRoutes() {
        post("/revenuecat") {
            val authorization = call.request.headers[HttpHeaders.Authorization]
            if (!isAuthorized(authorization)) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Unauthorized"))
                return@post
            }

            val payload = call.receive<RevenueCatWebhookRequest>()
            val event = payload.toDomainEvent()
            if (event == null) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Ignored RevenueCat event"))
                return@post
            }

            when (val result = subscriptionService.handleRevenueCatEvent(event)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, mapOf("message" to "Processed RevenueCat event"))
                is Result.Failure -> call.respond(HttpStatusCode.OK, mapOf("message" to result.reason))
            }
        }.describe {
            tag(ApiTags.WEBHOOKS)
            summary = "Handle RevenueCat subscription webhooks"
        }
    }

    private fun RevenueCatWebhookRequest.toDomainEvent(): RevenueCatSubscriptionEvent? {
        val eventType = runCatching { RevenueCatEventType.valueOf(event.type.uppercase()) }.getOrNull() ?: return null
        val customerId = event.appUserId ?: event.originalAppUserId ?: return null
        val userId = event.appUserId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        return RevenueCatSubscriptionEvent(
            revenueCatCustomerId = customerId,
            userId = userId,
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
}
