package com.vlatkogalev.platform.core.error

import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureCore(timeProvider: TimeProvider = TimeProvider.System) {
    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(
                    success = false,
                    error = cause.message ?: "Unexpected server error",
                    timestampMillis = timeProvider.nowMillis(),
                ),
            )
        }
    }
}
