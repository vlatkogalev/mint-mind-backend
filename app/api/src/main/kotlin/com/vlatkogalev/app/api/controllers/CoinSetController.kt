@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.*
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.domain.coin.service.CoinSetService
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import java.util.*

class CoinSetController(
    private val coinSetService: CoinSetService,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerRoutes() {
        post {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }

            val payload = call.receive<CreateCoinSetRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@post
            }

            when (val result = coinSetService.createSet(userId, payload.name, payload.description)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Create a coin set"
        }

        get {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            when (val result = coinSetService.listSets(userId)) {
                is Result.Success -> call.respond(success(result.value.map { it.toResponse() }))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "List all sets for the user"
        }

        get("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set ID"))
                return@get
            }

            when (val result = coinSetService.getSet(setId, userId)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Get a set by ID"
        }

        patch("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@patch
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set ID"))
                return@patch
            }

            val payload = call.receive<UpdateCoinSetRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@patch
            }

            when (val result = coinSetService.updateSet(setId, userId, payload.name, payload.description)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Update a set's name and description"
        }

        delete("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@delete
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set ID"))
                return@delete
            }

            when (val result = coinSetService.deleteSet(setId, userId)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Set deleted")))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Delete a set"
        }

        post("/{id}/coins") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set ID"))
                return@post
            }

            val payload = call.receive<ModifySetCoinsRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@post
            }

            val coinIds = payload.coinIds.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinIds.size != payload.coinIds.size) {
                call.respond(HttpStatusCode.BadRequest, error("One or more coin IDs are invalid"))
                return@post
            }

            when (val result = coinSetService.addCoinsToSet(setId, userId, coinIds)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Add coins to a set"
        }

        delete("/{id}/coins") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@delete
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set ID"))
                return@delete
            }

            val payload = call.receive<ModifySetCoinsRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@delete
            }

            val coinIds = payload.coinIds.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinIds.size != payload.coinIds.size) {
                call.respond(HttpStatusCode.BadRequest, error("One or more coin IDs are invalid"))
                return@delete
            }

            when (val result = coinSetService.removeCoinsFromSet(setId, userId, coinIds)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Remove coins from a set"
        }
    }

    private fun io.ktor.server.application.ApplicationCall.userUuidOrNull(): UUID? =
        principal<JWTPrincipal>()?.userIdOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun CoinSet.toResponse(): CoinSetResponse =
        CoinSetResponse(
            id = id.toString(),
            name = name,
            description = description,
            previewObverseKeys = previewObverseKeys,
            coinCount = coinIds.size,
            createdAt = createdAt.toEpochMilli(),
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
