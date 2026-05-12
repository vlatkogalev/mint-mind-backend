@file:OptIn(ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.routes

import com.vlatkogalev.app.api.controllers.RevenueCatWebhookController
import com.vlatkogalev.app.api.controllers.StorageController
import com.vlatkogalev.app.api.controllers.UserAuthController
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

object ApiTags {
    const val HEALTH = "Health"
    const val AUTH = "Auth"
    const val STORAGE = "Storage"
    const val WEBHOOKS = "Webhooks"
}

fun Application.configureRoutes() {
    val userAuthController by inject<UserAuthController>()
    val storageController by inject<StorageController>()
    val revenueCatWebhookController by inject<RevenueCatWebhookController>()
    val timeProvider by inject<TimeProvider>()

    routing {
        healthRoutes(timeProvider)
        authRoutes(userAuthController, timeProvider)
        storageRoutes(storageController)
        webhookRoutes(revenueCatWebhookController)
        docsRoutes()
    }
}

fun Routing.healthRoutes(timeProvider: TimeProvider = TimeProvider.System) {
    get("/health") {
        call.respond(
            ApiResponse(
                success = true,
                data = mapOf("status" to "ok"),
                timestampMillis = timeProvider.nowMillis(),
            ),
        )
    }.describe {
        tag(ApiTags.HEALTH)
        summary = "Service health check"
    }
}

fun Routing.authRoutes(controller: UserAuthController, timeProvider: TimeProvider = TimeProvider.System) {
    route("/auth") {
        get("/health") {
            call.respond(
                ApiResponse(
                    success = true,
                    data = mapOf("auth" to "ready"),
                    timestampMillis = timeProvider.nowMillis(),
                ),
            )
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Authentication subsystem health check"
        }

        controller.run {
            registerPublicRoutes()
        }

        authenticate("jwt-auth") {
            controller.run {
                registerProtectedRoutes()
            }
        }
    }
}

fun Routing.storageRoutes(controller: StorageController) {
    route("/storage") {
        authenticate("jwt-auth") {
            controller.run {
                registerProtectedRoutes()
            }
        }
    }
}

fun Routing.webhookRoutes(controller: RevenueCatWebhookController) {
    route("/webhooks") {
        controller.run {
            registerRoutes()
        }
    }
}

fun Routing.docsRoutes() {
    swaggerUI(path = "/swagger", swaggerFile = "documentation.yaml")
}
