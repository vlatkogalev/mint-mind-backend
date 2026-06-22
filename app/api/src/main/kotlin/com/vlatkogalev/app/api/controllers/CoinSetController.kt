@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.*
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.app.api.util.toErrorResponse
import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.domain.coin.service.CoinSetService
import com.vlatkogalev.platform.auth.userUuidOrNull
import com.vlatkogalev.platform.core.ErrorResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.storage.FileStorageService
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
    private val fileStorageService: FileStorageService,
) {
    fun Route.registerRoutes() {
        post {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@post
            }

            val payload = call.receive<CreateCoinSetRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", it))
                return@post
            }

            when (val result = coinSetService.createSet(userId, payload.name, payload.description)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, result.value.toResponse())
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Create a coin set"
        }

        get {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@get
            }

            when (val result = coinSetService.listSets(userId)) {
                is Result.Success -> call.respond(result.value.map { it.toResponse() })
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "List all sets for the user"
        }

        get("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@get
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid set ID"))
                return@get
            }

            when (val result = coinSetService.getSet(setId, userId)) {
                is Result.Success -> call.respond(result.value.toResponse())
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Get a set by ID"
        }

        patch("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@patch
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid set ID"))
                return@patch
            }

            val payload = call.receive<UpdateCoinSetRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", it))
                return@patch
            }

            when (val result = coinSetService.updateSet(setId, userId, payload.name, payload.description)) {
                is Result.Success -> call.respond(result.value.toResponse())
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Update a set's name and description"
        }

        delete("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@delete
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid set ID"))
                return@delete
            }

            when (val result = coinSetService.deleteSet(setId, userId)) {
                is Result.Success -> call.respond(mapOf("message" to "Set deleted"))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Delete a set"
        }

        post("/{id}/coins") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@post
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid set ID"))
                return@post
            }

            val payload = call.receive<ModifySetCoinsRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", it))
                return@post
            }

            val coinIds = payload.coinIds.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinIds.size != payload.coinIds.size) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "One or more coin IDs are invalid"))
                return@post
            }

            when (val result = coinSetService.addCoinsToSet(setId, userId, coinIds)) {
                is Result.Success -> call.respond(result.value.toResponse())
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Add coins to a set"
        }

        delete("/{id}/coins") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@delete
            }

            val setId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid set ID"))
                return@delete
            }

            val payload = call.receive<ModifySetCoinsRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", it))
                return@delete
            }

            val coinIds = payload.coinIds.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinIds.size != payload.coinIds.size) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "One or more coin IDs are invalid"))
                return@delete
            }

            when (val result = coinSetService.removeCoinsFromSet(setId, userId, coinIds)) {
                is Result.Success -> call.respond(result.value.toResponse())
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COIN_SETS)
            summary = "Remove coins from a set"
        }
    }

    private fun CoinSet.toResponse(): CoinSetResponse =
        CoinSetResponse(
            id = id.toString(),
            name = name,
            description = description,
            previewObverseUrls = previewObverseKeys.map { fileStorageService.publicUrl(it) },
            coinCount = coinIds.size,
            createdAt = createdAt.toEpochMilli(),
        )
}
