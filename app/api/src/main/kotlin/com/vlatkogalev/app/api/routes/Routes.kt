package com.vlatkogalev.app.api.routes

import com.vlatkogalev.app.api.controllers.CoinController
import com.vlatkogalev.app.api.controllers.CoinPricingController
import com.vlatkogalev.app.api.controllers.CoinSetController
import com.vlatkogalev.app.api.controllers.DebugController
import com.vlatkogalev.app.api.controllers.MarketplaceController
import com.vlatkogalev.app.api.controllers.NewsController
import com.vlatkogalev.app.api.controllers.RevenueCatWebhookController
import com.vlatkogalev.app.api.controllers.StorageController
import com.vlatkogalev.app.api.controllers.UserAuthController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

object ApiTags {
    const val AUTH = "Auth"
    const val COINS = "Coins"
    const val COIN_SETS = "Coin Sets"
    const val MARKETPLACE = "Marketplace"
    const val NEWS = "News"
    const val STORAGE = "Storage"
    const val WEBHOOKS = "Webhooks"
}

fun Application.configureRoutes() {
    val userAuthController by inject<UserAuthController>()
    val coinController by inject<CoinController>()
    val coinSetController by inject<CoinSetController>()
    val newsController by inject<NewsController>()
    val marketplaceController by inject<MarketplaceController>()
    val coinPricingController by inject<CoinPricingController>()
    val storageController by inject<StorageController>()
    val revenueCatWebhookController by inject<RevenueCatWebhookController>()
    val debugController by inject<DebugController>()

    routing {
        authRoutes(userAuthController)
        coinRoutes(coinController)
        coinSetRoutes(coinSetController)
        coinPricingRoutes(coinPricingController)
        newsRoutes(newsController)
        marketplaceRoutes(marketplaceController)
        storageRoutes(storageController)
        webhookRoutes(revenueCatWebhookController)
        debugRoutes(debugController)
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

fun Routing.coinRoutes(controller: CoinController) {
    route("/coins") {
        authenticate("jwt-auth") {
            controller.run {
                registerRoutes()
            }
        }
    }
}

fun Routing.coinSetRoutes(controller: CoinSetController) {
    route("/sets") {
        authenticate("jwt-auth") {
            controller.run {
                registerRoutes()
            }
        }
    }
}

fun Routing.coinPricingRoutes(controller: CoinPricingController) {
    route("/coins") {
        authenticate("jwt-auth") {
            controller.run {
                registerRoutes()
            }
        }
    }
}

fun Routing.newsRoutes(controller: NewsController) {
    route("/news") {
        controller.run {
            registerRoutes()
        }
    }
}

fun Routing.marketplaceRoutes(controller: MarketplaceController) {
    route("/marketplace") {
        controller.run {
            registerRoutes()
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
    swaggerUI(path = "/docs", swaggerFile = "documentation.yaml")
}

fun Routing.debugRoutes(controller: DebugController) {
    controller.run {
        registerRoutes()
    }
}