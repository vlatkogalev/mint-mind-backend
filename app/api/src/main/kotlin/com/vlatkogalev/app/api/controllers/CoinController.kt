@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.*
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.app.api.util.toErrorResponse
import com.vlatkogalev.app.jobs.CoinEnrichmentQueue
import com.vlatkogalev.domain.coin.model.*
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.domain.coin.service.CoinEnrichmentService
import com.vlatkogalev.domain.coin.service.CoinService
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
import kotlinx.serialization.json.Json
import java.util.*

class CoinController(
    private val coinService: CoinService,
    private val enrichmentService: CoinEnrichmentService,
    private val enrichmentQueue: CoinEnrichmentQueue,
    private val fileStorageService: FileStorageService,
    private val catalogCoinRepository: CatalogCoinRepository,
) {
    fun Route.registerRoutes() {
        post {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@post
            }

            val payload = call.receive<SaveCoinRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", it))
                return@post
            }

            val recognitionResult = mapToRecognitionResult(payload)

            when (val result = coinService.saveCoin(
                userId = userId,
                obverseKey = payload.obverseKey,
                reverseKey = payload.reverseKey,
                recognitionResult = recognitionResult,
                catalogueNumbers = emptyList(),
                notes = payload.notes,
                catalogCoinId = null,
            )) {
                is Result.Success -> {
                    enrichmentQueue.enqueue(result.value.id, recognitionResult)
                    call.respond(
                        HttpStatusCode.Created,
                        result.value.toDetailResponse()
                    )
                }
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Save a recognized coin"
        }

        get {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
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
                    val catalogCoins = catalogCoinRepository
                        .findByIds(coins.mapNotNull { it.catalogCoinId })
                        .associateBy { it.id }
                    val summaries = coins.map { it.toSummaryResponse(it.catalogCoinId?.let(catalogCoins::get)) }
                    call.respond(CoinListResponse(coins = summaries, nextCursor = nextCursor))
                }
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "List user's coins"
        }

        get("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@get
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid coin ID"))
                return@get
            }

            when (val result = coinService.getCoin(coinId, userId)) {
                is Result.Success -> {
                    val coin = result.value
                    val catalogCoin = coin.catalogCoinId?.let { catalogCoinRepository.findById(it) }
                    call.respond(coin.toDetailResponse(catalogCoin))
                }
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get a single coin"
        }

        delete("/{id}") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@delete
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid coin ID"))
                return@delete
            }

            when (val result = coinService.deleteCoin(coinId, userId)) {
                is Result.Success -> call.respond(mapOf("message" to "Coin deleted"))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Delete a coin"
        }

        patch("/{id}/notes") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@patch
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid coin ID"))
                return@patch
            }

            val payload = call.receive<UpdateCoinNotesRequest>()

            when (val result = coinService.updateNotes(coinId, userId, payload.notes)) {
                is Result.Success -> {
                    val coin = result.value
                    val catalogCoin = coin.catalogCoinId?.let { catalogCoinRepository.findById(it) }
                    call.respond(coin.toDetailResponse(catalogCoin))
                }
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Update coin notes"
        }

        get("/{id}/images") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@get
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid coin ID"))
                return@get
            }

            when (val result = coinService.getCoin(coinId, userId)) {
                is Result.Success -> {
                    val coin = result.value
                    val obverseUrl = fileStorageService.createPresignedDownload(coin.obverseKey)
                    val reverseUrl = fileStorageService.createPresignedDownload(coin.reverseKey)
                    call.respond(CoinImagesResponse(
                        obverseUrl = obverseUrl.toString(),
                        reverseUrl = reverseUrl.toString(),
                    ))
                }
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get presigned download URLs for coin images"
        }

        post("/{id}/enrich") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
                return@post
            }

            val coinId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (coinId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", "Invalid coin ID"))
                return@post
            }

            when (val result = enrichmentService.enrichCoin(coinId, userId)) {
                is Result.Success -> {
                    val matchResult = result.value
                    call.respond(matchResult.toDto())
                }
                is Result.Failure -> when (result.error) {
                    is CoinError.Unauthorized ->
                        call.respond(HttpStatusCode.Forbidden, result.error.toErrorResponse())
                    is CoinError.NotFound, is CoinError.SetNotFound ->
                        call.respond(HttpStatusCode.NotFound, result.error.toErrorResponse())
                    else ->
                        call.respond(HttpStatusCode.InternalServerError, result.error.toErrorResponse())
                }
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Manually trigger catalog enrichment"
        }

        get("/stats") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid token"))
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
                is Result.Success -> call.respond(result.value.toStatsResponse())
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, result.error.toErrorResponse())
            }
        }.describe {
            tag(ApiTags.COINS)
            summary = "Get collection statistics"
        }
    }

    private fun mapToRecognitionResult(req: SaveCoinRequest): RecognitionResult =
        RecognitionResult(
            overallConfidence = runCatching { Confidence.valueOf(req.overallConfidence.uppercase()) }.getOrDefault(Confidence.LOW),
            countryOrIssuer = req.countryOrIssuer,
            denomination = req.denomination,
            seriesName = req.seriesName,
            year = req.year,
            era = req.era,
            confidenceCountry = req.confidenceCountry,
            confidenceDenomination = req.confidenceDenomination,
            confidenceSeries = req.confidenceSeries,
            confidenceYear = req.confidenceYear,
            confidenceEra = req.confidenceEra,
            mintMark = req.mintMark,
            mintMarkStatus = req.mintMarkStatus,
            mintMarkConfidence = req.mintMarkConfidence,
            metalComposition = req.metalComposition,
            estimatedGrade = req.estimatedGrade,
            estimatedGradeValue = req.estimatedGradeValue,
            gradeCode = req.gradeCode,
            gradeConfidence = req.gradeConfidence,
            rarityQualitative = req.rarityQualitative,
            rarityScore = req.rarityScore,
            valueLow = req.valueLow,
            valueHigh = req.valueHigh,
            valueCurrency = req.valueCurrency,
            mintage = req.mintage,
            obverseDescription = req.obverseDescription,
            reverseDescription = req.reverseDescription,
            weightGrams = req.weightGrams,
            diameterMm = req.diameterMm,
            thicknessMm = req.thicknessMm,
            edge = req.edge,
            designerObverse = req.designerObverse,
            designerReverse = req.designerReverse,
            positiveFeatures = req.positiveFeatures,
            negativeFeatures = req.negativeFeatures,
            supplySummary = req.supplySummary,
            demandSummary = req.demandSummary,
            valueDisclaimer = req.valueDisclaimer,
            obverseLettering = req.obverseLettering,
            reverseLettering = req.reverseLettering,
            analysisNotes = req.analysisNotes,
            historicalContext = req.historicalContext,
            obverseVisible = req.obverseVisible,
            reverseVisible = req.reverseVisible,
            imageFocus = req.imageFocus,
            imageLighting = req.imageLighting,
            imageResolution = req.imageResolution,
            imageCropping = req.imageCropping,
            imageIssues = req.imageIssues,
            rawJson = req.rawJson.minifiedJson(),
        )

    private fun Coin.toDetailResponse(catalogCoin: CatalogCoin? = null): CoinDetailResponse {
        val r = recognitionResult
        val c = catalogCoin
        return CoinDetailResponse(
            id = id.toString(),
            userId = userId.toString(),
            obverseUrl = fileStorageService.publicUrl(obverseKey),
            reverseUrl = fileStorageService.publicUrl(reverseKey),
            coinData = CoinDataDto(
                countryOrIssuer = c?.fingerprint?.countryOrIssuer ?: r.countryOrIssuer,
                denomination = c?.fingerprint?.denomination ?: r.denomination,
                seriesName = r.seriesName,
                year = c?.fingerprint?.year ?: r.year,
                era = r.era,
                mintMark = c?.fingerprint?.mintMark ?: r.mintMark,
                metalComposition = c?.composition ?: r.metalComposition,
                weightGrams = c?.weightGrams ?: r.weightGrams,
                diameterMm = c?.diameterMm ?: r.diameterMm,
                thicknessMm = c?.thicknessMm ?: r.thicknessMm,
                edge = c?.edgeDescription ?: r.edge,
                obverseDescription = c?.obverseDescription ?: r.obverseDescription,
                reverseDescription = c?.reverseDescription ?: r.reverseDescription,
                historicalContext = c?.historicalContext ?: r.historicalContext,
                obverseLettering = c?.obverseLettering ?: r.obverseLettering,
                reverseLettering = c?.reverseLettering ?: r.reverseLettering,
                designerObverse = c?.obverseDesigners?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: r.designerObverse,
                designerReverse = c?.reverseDesigners?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: r.designerReverse,
                mintage = r.mintage,
                shape = c?.shape,
                technique = c?.technique,
                orientation = c?.orientation,
                mintName = c?.mintName,
                ruler = c?.ruler,
                objectType = c?.objectType,
                demonetized = c?.demonetized,
                tags = c?.tags ?: emptyList(),
                numistaUrl = c?.numistaUrl,
                obverseThumbnailUrl = c?.thumbnailUrl,
                reverseThumbnailUrl = c?.reverseThumbnailUrl,
                minYear = c?.minYear,
                maxYear = c?.maxYear,
            ),
            aiAnalysis = AiAnalysisDto(
                overallConfidence = r.overallConfidence.name,
                confidenceCountry = r.confidenceCountry,
                confidenceDenomination = r.confidenceDenomination,
                confidenceSeries = r.confidenceSeries,
                confidenceYear = r.confidenceYear,
                confidenceEra = r.confidenceEra,
                mintMarkStatus = r.mintMarkStatus,
                mintMarkConfidence = r.mintMarkConfidence,
                estimatedGrade = r.estimatedGrade,
                estimatedGradeValue = r.estimatedGradeValue,
                gradeCode = r.gradeCode,
                gradeConfidence = r.gradeConfidence,
                rarityQualitative = r.rarityQualitative,
                rarityScore = r.rarityScore,
                valueLow = r.valueLow,
                valueHigh = r.valueHigh,
                valueCurrency = r.valueCurrency,
                positiveFeatures = r.positiveFeatures,
                negativeFeatures = r.negativeFeatures,
                supplySummary = r.supplySummary,
                demandSummary = r.demandSummary,
                valueDisclaimer = r.valueDisclaimer,
                analysisNotes = r.analysisNotes,
                obverseVisible = r.obverseVisible,
                reverseVisible = r.reverseVisible,
                imageFocus = r.imageFocus,
                imageLighting = r.imageLighting,
                imageResolution = r.imageResolution,
                imageCropping = r.imageCropping,
                imageIssues = r.imageIssues,
                rawJson = r.rawJson,
            ),
            catalogueNumbers = when {
                c != null && c.catalogReferences.isNotEmpty() -> c.catalogReferences.map {
                    CatalogueNumberDto(it.catalogueName, it.number, it.confidence.name)
                }
                else -> catalogueNumbers.map {
                    CatalogueNumberDto(it.catalogueName, it.number, it.confidence.name)
                }
            },
            setId = setId?.toString(),
            catalogCoinId = catalogCoinId?.toString(),
            notes = notes,
            createdAt = createdAt.toEpochMilli(),
        )
    }

    private fun Coin.toSummaryResponse(catalogCoin: CatalogCoin? = null): CoinSummaryResponse {
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
            estimatedGradeValue = recognitionResult.estimatedGradeValue,
            estimatedValueMean = estimatedValueMean,
            setId = setId?.toString(),
            createdAt = createdAt.toEpochMilli(),
            obverseThumbnailUrl = catalogCoin?.thumbnailUrl,
            reverseThumbnailUrl = catalogCoin?.reverseThumbnailUrl,
        )
    }

    private suspend fun CoinCollectionStats.toStatsResponse(): CoinCollectionStatsResponse {
        val highlightCoins = listOfNotNull(highlights.mostValuable, highlights.mostAncient, highlights.rarest)
        val catalogCoins = catalogCoinRepository
            .findByIds(highlightCoins.mapNotNull { it.catalogCoinId })
            .associateBy { it.id }
        fun Coin.summary() = toSummaryResponse(catalogCoinId?.let(catalogCoins::get))
        return CoinCollectionStatsResponse(
            totalCoins = totalCoins,
            totalIssuers = totalIssuers,
            estimatedTotalValueMean = estimatedTotalValueMean,
            highlights = CollectionHighlightsResponse(
                mostValuable = highlights.mostValuable?.summary(),
                mostAncient = highlights.mostAncient?.summary(),
                rarest = highlights.rarest?.summary(),
            ),
        )
    }

    private fun MatchResult.toDto(): MatchResultDto =
        MatchResultDto(
            tier = tier.name,
            bestCandidate = bestCandidate?.let { bc ->
                MatchCandidateDto(
                    catalogCoinId = bc.catalogCoin?.id?.toString(),
                    providerName = bc.providerName,
                    externalId = bc.externalId,
                    score = bc.score,
                    scoreBreakdown = bc.scoreBreakdown,
                    dataCompleteness = completenessOf(bc),
                )
            },
            allCandidates = allCandidates.map { bc ->
                MatchCandidateDto(
                    catalogCoinId = bc.catalogCoin?.id?.toString(),
                    providerName = bc.providerName,
                    externalId = bc.externalId,
                    score = bc.score,
                    scoreBreakdown = bc.scoreBreakdown,
                    dataCompleteness = completenessOf(bc),
                )
            },
            retrievalKey = retrievalKey,
        )

    private fun completenessOf(bc: MatchCandidate): Map<String, Boolean> =
        mapOf(
            "country" to (bc.matchableCoin.countryOrIssuer != null),
            "denomination" to (bc.matchableCoin.denomination != null),
            "weight" to (bc.matchableCoin.weightGrams != null),
            "diameter" to (bc.matchableCoin.diameterMm != null),
            "composition" to (bc.matchableCoin.composition != null),
            "externalReference" to (bc.externalId != null),
        )

    private fun String.minifiedJson(): String =
        kotlin.runCatching {
            Json.encodeToString(Json.parseToJsonElement(this))
        }.getOrDefault(this)
}
