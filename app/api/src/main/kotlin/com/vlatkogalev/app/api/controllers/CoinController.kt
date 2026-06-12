@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.*
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.service.CoinEnrichmentService
import com.vlatkogalev.domain.coin.service.CoinService
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.storage.FileStorageService
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import java.util.*

class CoinController(
    private val coinService: CoinService,
    private val enrichmentService: CoinEnrichmentService,
    private val fileStorageService: FileStorageService,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerRoutes() {
        post {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }

            val payload = call.receive<SaveCoinRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@post
            }

            val recognitionResult = mapToRecognitionResult(payload.recognitionResult)
            val catalogueNumbers = payload.catalogueNumbers.map { mapToCatalogueNumber(it) }

            when (val result = coinService.saveCoin(
                userId = userId,
                obverseKey = payload.obverseKey,
                reverseKey = payload.reverseKey,
                recognitionResult = recognitionResult,
                catalogueNumbers = catalogueNumbers,
                notes = payload.notes,
            )) {
                is Result.Success -> call.respond(HttpStatusCode.Created, success(result.value.toDetailResponse()))
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

            val country = call.request.queryParameters["country"]
            val year = call.request.queryParameters["year"]?.toIntOrNull()
            val minValue = call.request.queryParameters["minValue"]?.toDoubleOrNull()
            val maxValue = call.request.queryParameters["maxValue"]?.toDoubleOrNull()
            val setId = call.request.queryParameters["setId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val sortBy = call.request.queryParameters["sortBy"]
                ?.let { runCatching { CoinSortField.valueOf(it.uppercase()) }.getOrNull() }
                ?: CoinSortField.DATE_ADDED_NEW_TO_OLD
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val cursor = call.request.queryParameters["cursor"]?.toLongOrNull()

            when (val result = coinService.listCoins(
                userId = userId,
                country = country,
                year = year,
                minValue = minValue,
                maxValue = maxValue,
                setId = setId,
                sortBy = sortBy,
                limit = limit,
                cursor = cursor,
            )) {
                is Result.Success -> {
                    val coins = result.value
                    val nextCursor = if (coins.isNotEmpty() && coins.size >= limit) {
                        coins.last().createdAt.toEpochMilli()
                    } else null
                    val summaries = coins.map { it.toSummaryResponse() }
                    call.respond(success(CoinListResponse(coins = summaries, nextCursor = nextCursor)))
                }
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "List user's coins"
        }

        get("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin ID"))
                return@get
            }

            when (val result = coinService.getCoin(coinId, userId)) {
                is Result.Success -> call.respond(success(result.value.toDetailResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get a single coin"
        }

        delete("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@delete
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin ID"))
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
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@patch
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin ID"))
                return@patch
            }

            val payload = call.receive<UpdateCoinNotesRequest>()

            when (val result = coinService.updateNotes(coinId, userId, payload.notes)) {
                is Result.Success -> call.respond(success(result.value.toDetailResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Update coin notes"
        }

        get("/{id}/images") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin ID"))
                return@get
            }

            when (val result = coinService.getCoin(coinId, userId)) {
                is Result.Success -> {
                    val coin = result.value
                    val obverseUrl = fileStorageService.createPresignedDownload(coin.obverseKey)
                    val reverseUrl = fileStorageService.createPresignedDownload(coin.reverseKey)
                    call.respond(success(CoinImagesResponse(
                        obverseUrl = obverseUrl.toString(),
                        reverseUrl = reverseUrl.toString(),
                    )))
                }
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get presigned download URLs for coin images"
        }

        post("/{id}/enrich") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, error("Invalid coin ID"))
                return@post
            }

            when (val coinResult = coinService.getCoin(coinId, userId)) {
                is Result.Success -> {
                    val catalogCoinId = coinResult.value.catalogCoinId
                    if (catalogCoinId == null) {
                        call.respond(HttpStatusCode.BadRequest, error("Coin is not linked to a catalog entry"))
                        return@post
                    }
                    when (val enrichResult = enrichmentService.enrichById(catalogCoinId)) {
                        is Result.Success -> call.respond(success(mapOf("message" to "Enrichment completed")))
                        is Result.Failure -> call.respond(HttpStatusCode.InternalServerError, error(enrichResult.reason))
                    }
                }
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(coinResult.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Manually trigger catalog enrichment"
        }

        get("/stats") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            val country = call.request.queryParameters["country"]
            val year = call.request.queryParameters["year"]?.toIntOrNull()
            val minValue = call.request.queryParameters["minValue"]?.toDoubleOrNull()
            val maxValue = call.request.queryParameters["maxValue"]?.toDoubleOrNull()
            val setId = call.request.queryParameters["setId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }

            when (val result = coinService.getCollectionStats(
                userId = userId,
                country = country,
                year = year,
                minValue = minValue,
                maxValue = maxValue,
                setId = setId,
            )) {
                is Result.Success -> call.respond(success(result.value.toStatsResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get collection statistics"
        }
    }

    private fun io.ktor.server.application.ApplicationCall.userUuidOrNull(): UUID? =
        principal<JWTPrincipal>()?.userIdOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun mapToRecognitionResult(dto: RecognitionResultDto): RecognitionResult =
        RecognitionResult(
            overallConfidence = runCatching { Confidence.valueOf(dto.overallConfidence.uppercase()) }.getOrDefault(Confidence.LOW),
            countryOrIssuer = dto.countryOrIssuer,
            denomination = dto.denomination,
            seriesName = dto.seriesName,
            year = dto.year,
            mintMark = dto.mintMark,
            metalComposition = dto.metalComposition,
            estimatedGrade = dto.estimatedGrade,
            estimatedGradeValue = dto.estimatedGradeValue,
            rarityQualitative = dto.rarityQualitative,
            valueLow = dto.valueLow,
            valueHigh = dto.valueHigh,
            mintage = dto.mintage,
            obverseDescription = dto.obverseDescription,
            reverseDescription = dto.reverseDescription,
            historicalContext = dto.historicalContext,
            rawJson = dto.rawJson,
        )

    private fun mapToCatalogueNumber(dto: CatalogueNumberDto): CatalogueNumber =
        CatalogueNumber(
            catalogueName = dto.catalogueName,
            number = dto.number,
            confidence = runCatching { Confidence.valueOf(dto.confidence.uppercase()) }.getOrDefault(Confidence.LOW),
        )

    private fun Coin.toDetailResponse(): CoinDetailResponse =
        CoinDetailResponse(
            id = id.toString(),
            userId = userId.toString(),
            obverseUrl = fileStorageService.publicUrl(obverseKey),
            reverseUrl = fileStorageService.publicUrl(reverseKey),
            recognitionResult = RecognitionResultDto(
                overallConfidence = recognitionResult.overallConfidence.name,
                countryOrIssuer = recognitionResult.countryOrIssuer,
                denomination = recognitionResult.denomination,
                seriesName = recognitionResult.seriesName,
                year = recognitionResult.year,
                mintMark = recognitionResult.mintMark,
                metalComposition = recognitionResult.metalComposition,
                estimatedGrade = recognitionResult.estimatedGrade,
                estimatedGradeValue = recognitionResult.estimatedGradeValue,
                rarityQualitative = recognitionResult.rarityQualitative,
                valueLow = recognitionResult.valueLow,
                valueHigh = recognitionResult.valueHigh,
                mintage = recognitionResult.mintage,
                obverseDescription = recognitionResult.obverseDescription,
                reverseDescription = recognitionResult.reverseDescription,
                historicalContext = recognitionResult.historicalContext,
                rawJson = recognitionResult.rawJson,
            ),
            catalogueNumbers = catalogueNumbers.map {
                CatalogueNumberDto(catalogueName = it.catalogueName, number = it.number, confidence = it.confidence.name)
            },
            setId = setId?.toString(),
            catalogCoinId = catalogCoinId?.toString(),
            notes = notes,
            createdAt = createdAt.toEpochMilli(),
        )

    private fun Coin.toSummaryResponse(): CoinSummaryResponse {
        val estimatedValueMean: Double? = run {
            val low = recognitionResult.valueLow
            val high = recognitionResult.valueHigh
            if (low != null && high != null) (low + high) / 2.0 else null
        }
        return CoinSummaryResponse(
            id = id.toString(),
            obverseUrl = fileStorageService.publicUrl(obverseKey),
            reverseUrl = fileStorageService.publicUrl(reverseKey),
            denomination = recognitionResult.denomination,
            countryOrIssuer = recognitionResult.countryOrIssuer,
            year = recognitionResult.year,
            mintage = recognitionResult.mintage,
            estimatedGrade = recognitionResult.estimatedGrade,
            estimatedValueMean = estimatedValueMean,
            setId = setId?.toString(),
            createdAt = createdAt.toEpochMilli(),
        )
    }

    private fun CoinCollectionStats.toStatsResponse(): CoinCollectionStatsResponse =
        CoinCollectionStatsResponse(
            totalCoins = totalCoins,
            totalIssuers = totalIssuers,
            estimatedTotalValueMean = estimatedTotalValueMean,
            highlights = CollectionHighlightsResponse(
                mostValuable = highlights.mostValuable?.toDetailResponse(),
                mostAncient = highlights.mostAncient?.toDetailResponse(),
                rarest = highlights.rarest?.toDetailResponse(),
            ),
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
