package com.vlatkogalev.app.api

import com.vlatkogalev.app.api.di.appModule
import com.vlatkogalev.app.api.routes.configureRoutes
import com.vlatkogalev.platform.auth.configureAuth
import com.vlatkogalev.platform.core.error.configureCore
import com.vlatkogalev.platform.logging.configureLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import org.koin.core.logger.Level
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.module() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)

        allowNonSimpleContentTypes = true

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    install(Koin) {
        slf4jLogger(Level.INFO)
        modules(appModule)
    }

    configureCore()
    configureLogging()
    configureAuth()
    configureRoutes()
}
