@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.CoinSetResponse
import com.vlatkogalev.app.api.dto.CreateCoinSetRequest
import com.vlatkogalev.app.api.dto.ModifySetCoinsRequest
import com.vlatkogalev.app.api.dto.UpdateCoinSetRequest
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.coin.model.CoinSet
import com.vlatkogalev.domain.coin.service.CoinSetService
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import java.util.UUID

class CoinSetController(
    private val coinSetService: CoinSetService,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerProtectedRoutes() {
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
            tag(ApiTags.SETS)
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
            tag(ApiTags.SETS)
            summary = "List coin sets"
        }

        get("/{id}") {
            val userId = call.userUuidOrNull()
            val setId = call.setIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set id"))
                return@get
            }

            when (val result = coinSetService.getSet(setId, userId)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.SETS)
            summary = "Get a coin set by id"
        }

        patch("/{id}") {
            val userId = call.userUuidOrNull()
            val setId = call.setIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@patch
            }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set id"))
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
            tag(ApiTags.SETS)
            summary = "Update a coin set"
        }

        delete("/{id}") {
            val userId = call.userUuidOrNull()
            val setId = call.setIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@delete
            }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set id"))
                return@delete
            }

            when (val result = coinSetService.deleteSet(setId, userId)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Set deleted")))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.SETS)
            summary = "Delete a coin set"
        }

        post("/{id}/coins") {
            val userId = call.userUuidOrNull()
            val setId = call.setIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set id"))
                return@post
            }

            val payload = call.receive<ModifySetCoinsRequest>()
            val coinIds = payload.validatedCoinIdsOrNull()
            if (payload.validate() != null || coinIds == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin ids"))
                return@post
            }

            when (val result = coinSetService.addCoinsToSet(setId, userId, coinIds)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.SETS)
            summary = "Add coins to a set"
        }

        delete("/{id}/coins") {
            val userId = call.userUuidOrNull()
            val setId = call.setIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@delete
            }
            if (setId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid set id"))
                return@delete
            }

            val payload = call.receive<ModifySetCoinsRequest>()
            val coinIds = payload.validatedCoinIdsOrNull()
            if (payload.validate() != null || coinIds == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin ids"))
                return@delete
            }

            when (val result = coinSetService.removeCoinsFromSet(setId, userId, coinIds)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.SETS)
            summary = "Remove coins from a set"
        }
    }

    private fun ApplicationCall.userUuidOrNull(): UUID? =
        principal<JWTPrincipal>()?.userIdOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun ApplicationCall.setIdOrNull(): UUID? =
        parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun ModifySetCoinsRequest.validatedCoinIdsOrNull(): List<UUID>? =
        coinIds.map { runCatching { UUID.fromString(it) }.getOrNull() }
            .takeIf { ids -> ids.all { it != null } }
            ?.filterNotNull()

    private fun CoinSet.toResponse(): CoinSetResponse =
        CoinSetResponse(
            id = id.toString(),
            name = name,
            description = description,
            previewObverseKeys = previewObverseKeys,
            createdAt = createdAt.toString(),
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