@file:OptIn(ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.routes

import com.vlatkogalev.app.api.controllers.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.koin.ktor.ext.inject

object ApiTags {
    const val AUTH = "Auth"
    const val STORAGE = "Storage"
    const val WEBHOOKS = "Webhooks"
    const val COINS = "Coins"
    const val SETS = "Sets"
    const val NEWS = "News"
    const val MARKETPLACE = "Marketplace"
}

fun Application.configureRoutes() {
    val userAuthController by inject<UserAuthController>()
    val storageController by inject<StorageController>()
    val coinController by inject<CoinController>()
    val coinSetController by inject<CoinSetController>()
    val newsController by inject<NewsController>()
    val revenueCatWebhookController by inject<RevenueCatWebhookController>()
    val coinPricingController by inject<CoinPricingController>()
    val marketplaceController by inject<MarketplaceController>()

    routing {
        authRoutes(userAuthController)
        storageRoutes(storageController)
        coinRoutes(coinController, coinPricingController)
        coinSetRoutes(coinSetController)
        newsRoutes(newsController)
        webhookRoutes(revenueCatWebhookController)
        marketplaceRoutes(marketplaceController)
        docsRoutes()
    }
}

fun Routing.authRoutes(controller: UserAuthController) {
    route("/auth") {
        controller.run {
            registerPublicRoutes()
        }

        authenticate("jwt-auth", optional = true) {
            controller.run {
                registerOptionalAuthRoutes()
            }
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

fun Routing.coinRoutes(controller: CoinController, coinPricingController: CoinPricingController) {
    route("/coins") {
        authenticate("jwt-auth") {
            controller.run {
                registerProtectedRoutes()
            }

            coinPricingController.run {
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

fun Routing.marketplaceRoutes(controller: MarketplaceController) {
    route("/marketplace") {
        route("/listings") {
            controller.run { registerPublicRoutes() }
        }
    }
}

fun Routing.docsRoutes() {
    swaggerUI(path = "/docs", swaggerFile = "documentation.yaml")
}
