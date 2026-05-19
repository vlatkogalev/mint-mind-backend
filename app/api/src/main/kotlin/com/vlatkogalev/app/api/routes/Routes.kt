@file:OptIn(ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.routes

import com.vlatkogalev.app.api.controllers.CoinController
import com.vlatkogalev.app.api.controllers.CoinSetController
import com.vlatkogalev.app.api.controllers.NewsController
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
    const val AUTH = "Auth"
    const val STORAGE = "Storage"
    const val WEBHOOKS = "Webhooks"
    const val COINS = "Coins"
    const val SETS = "Sets"
    const val NEWS = "News"
}

fun Application.configureRoutes() {
    val userAuthController by inject<UserAuthController>()
    val storageController by inject<StorageController>()
    val coinController by inject<CoinController>()
    val coinSetController by inject<CoinSetController>()
    val newsController by inject<NewsController>()
    val revenueCatWebhookController by inject<RevenueCatWebhookController>()

    routing {
        authRoutes(userAuthController)
        storageRoutes(storageController)
        coinRoutes(coinController)
        coinSetRoutes(coinSetController)
        newsRoutes(newsController)
        webhookRoutes(revenueCatWebhookController)
        docsRoutes()
    }
}

fun Routing.authRoutes(controller: UserAuthController) {
    route("/auth") {
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

fun Routing.coinRoutes(controller: CoinController) {
    route("/coins") {
        authenticate("jwt-auth") {
            controller.run {
                registerProtectedRoutes()
            }
        }
    }
}

fun Routing.coinSetRoutes(controller: CoinSetController) {
    route("/sets") {
        authenticate("jwt-auth") {
            controller.run {
                registerProtectedRoutes()
            }
        }
    }
}

fun Routing.newsRoutes(controller: NewsController) {
    route("/news") {
        controller.run { registerPublicRoutes() }
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
    swaggerUI(path = "/docs", swaggerFile = "documentation.yaml")
}