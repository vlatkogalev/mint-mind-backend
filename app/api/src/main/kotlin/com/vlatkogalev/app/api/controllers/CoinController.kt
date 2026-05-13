@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.CatalogueNumberDto
import com.vlatkogalev.app.api.dto.CoinImagesResponse
import com.vlatkogalev.app.api.dto.CoinResponse
import com.vlatkogalev.app.api.dto.CollectionStatsResponse
import com.vlatkogalev.app.api.dto.RecognitionResultDto
import com.vlatkogalev.app.api.dto.SaveCoinRequest
import com.vlatkogalev.app.api.dto.UpdateCoinNotesRequest
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.Confidence
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.CoinService
import com.vlatkogalev.domain.coin.service.CollectionStats
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.storage.FileStorageService
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

class CoinController(
    private val coinService: CoinService,
    private val fileStorageService: FileStorageService,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerProtectedRoutes() {
        post {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }

            val payload = call.receive<SaveCoinRequest>()
            val recognitionResult = payload.recognitionResult.toDomainOrNull()
            val catalogueNumbers = payload.catalogueNumbers.mapNotNull { it.toDomainOrNull() }
            if (recognitionResult == null || catalogueNumbers.size != payload.catalogueNumbers.size) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid confidence value"))
                return@post
            }

            when (
                val result = coinService.saveCoin(
                    userId = userId,
                    obverseKey = payload.obverseKey,
                    reverseKey = payload.reverseKey,
                    recognitionResult = recognitionResult,
                    catalogueNumbers = catalogueNumbers,
                    notes = payload.notes,
                )
            ) {
                is Result.Success -> call.respond(HttpStatusCode.Created, success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Save a recognized coin"
        }

        get {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            val year = call.request.queryParameters["year"]?.toIntOrNull()
            val minValue = call.request.queryParameters["minValue"]?.toDoubleOrNull()
            val maxValue = call.request.queryParameters["maxValue"]?.toDoubleOrNull()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            when (
                val result = coinService.listCoins(
                    userId = userId,
                    country = call.request.queryParameters["country"],
                    year = year,
                    minValueUsd = minValue,
                    maxValueUsd = maxValue,
                    limit = limit,
                    offset = offset,
                )
            ) {
                is Result.Success -> call.respond(success(result.value.map { it.toResponse() }))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "List coins in the authenticated user's collection"
        }

        get("/{id}") {
            val userId = call.userUuidOrNull()
            val coinId = call.coinIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin id"))
                return@get
            }

            when (val result = coinService.getCoin(coinId, userId)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get a coin by id"
        }

        delete("/{id}") {
            val userId = call.userUuidOrNull()
            val coinId = call.coinIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@delete
            }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin id"))
                return@delete
            }

            when (val result = coinService.deleteCoin(coinId, userId)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Coin deleted")))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Delete a coin"
        }

        patch("/{id}/notes") {
            val userId = call.userUuidOrNull()
            val coinId = call.coinIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@patch
            }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin id"))
                return@patch
            }

            val payload = call.receive<UpdateCoinNotesRequest>()
            when (val result = coinService.updateNotes(coinId, userId, payload.notes)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Update notes for a coin"
        }

        get("/{id}/images") {
            val userId = call.userUuidOrNull()
            val coinId = call.coinIdOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin id"))
                return@get
            }

            when (val result = coinService.getCoin(coinId, userId)) {
                is Result.Success -> {
                    val coin = result.value
                    call.respond(
                        success(
                            CoinImagesResponse(
                                obverseUrl = fileStorageService.createPresignedDownload(coin.obverseKey).toString(),
                                reverseUrl = fileStorageService.createPresignedDownload(coin.reverseKey).toString(),
                            ),
                        ),
                    )
                }
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Create presigned download URLs for a coin's images"
        }
    }

    fun Route.registerCollectionRoutes() {
        get("/stats") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            when (val result = coinService.getCollectionStats(userId)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COLLECTION)
            summary = "Get collection statistics"
        }
    }

    private fun ApplicationCall.userUuidOrNull(): UUID? =
        principal<JWTPrincipal>()?.userIdOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun ApplicationCall.coinIdOrNull(): UUID? =
        parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun RecognitionResultDto.toDomainOrNull(): RecognitionResult? {
        val confidence = overallConfidence.toConfidenceOrNull() ?: return null
        return RecognitionResult(
            overallConfidence = confidence,
            countryOrIssuer = countryOrIssuer,
            denomination = denomination,
            seriesName = seriesName,
            year = year,
            mintMark = mintMark,
            metalComposition = metalComposition,
            estimatedGrade = estimatedGrade,
            estimatedGradeValue = estimatedGradeValue,
            rarityQualitative = rarityQualitative,
            valueLowUsd = valueLowUsd,
            valueHighUsd = valueHighUsd,
            obverseDescription = obverseDescription,
            reverseDescription = reverseDescription,
            historicalContext = historicalContext,
            rawJson = rawJson,
        )
    }

    private fun CatalogueNumberDto.toDomainOrNull(): CatalogueNumber? {
        val confidence = confidence.toConfidenceOrNull() ?: return null
        return CatalogueNumber(
            catalogueName = catalogueName,
            number = number,
            confidence = confidence,
        )
    }

    private fun String.toConfidenceOrNull(): Confidence? =
        runCatching { Confidence.valueOf(uppercase()) }.getOrNull()

    private fun Coin.toResponse(): CoinResponse =
        CoinResponse(
            id = id.toString(),
            userId = userId.toString(),
            obverseKey = obverseKey,
            reverseKey = reverseKey,
            recognitionResult = recognitionResult.toResponse(),
            catalogueNumbers = catalogueNumbers.map { it.toResponse() },
            notes = notes,
            createdAt = createdAt.toString(),
        )

    private fun RecognitionResult.toResponse(): RecognitionResultDto =
        RecognitionResultDto(
            overallConfidence = overallConfidence.name,
            countryOrIssuer = countryOrIssuer,
            denomination = denomination,
            seriesName = seriesName,
            year = year,
            mintMark = mintMark,
            metalComposition = metalComposition,
            estimatedGrade = estimatedGrade,
            estimatedGradeValue = estimatedGradeValue,
            rarityQualitative = rarityQualitative,
            valueLowUsd = valueLowUsd,
            valueHighUsd = valueHighUsd,
            obverseDescription = obverseDescription,
            reverseDescription = reverseDescription,
            historicalContext = historicalContext,
            rawJson = rawJson,
        )

    private fun CatalogueNumber.toResponse(): CatalogueNumberDto =
        CatalogueNumberDto(
            catalogueName = catalogueName,
            number = number,
            confidence = confidence.name,
        )

    private fun CollectionStats.toResponse(): CollectionStatsResponse =
        CollectionStatsResponse(
            totalCoins = totalCoins,
            estimatedTotalValueLowUsd = estimatedTotalValueLowUsd,
            estimatedTotalValueHighUsd = estimatedTotalValueHighUsd,
            byCountry = byCountry,
            byYear = byYear,
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
