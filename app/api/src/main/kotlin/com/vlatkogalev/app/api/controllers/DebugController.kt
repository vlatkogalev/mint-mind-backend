@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.DebugNumistaMatchRequest
import com.vlatkogalev.app.api.dto.MatchCandidateDto
import com.vlatkogalev.app.api.dto.MatchResultDto
import com.vlatkogalev.app.api.dto.MetricsResponseDto
import com.vlatkogalev.app.api.dto.RecognitionResultDto
import com.vlatkogalev.domain.coin.model.Confidence
import com.vlatkogalev.domain.coin.model.MatchMetrics
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.service.CoinEnrichmentService
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*

class DebugController(
    private val enrichmentService: CoinEnrichmentService,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerRoutes() {
        post("/debug/numista-match") {
            val payload = call.receive<DebugNumistaMatchRequest>()

            val recognitionResult = mapToRecognitionResult(payload.recognitionResult)
            val matchResult = enrichmentService.getOrMatch(recognitionResult)

            val dto = MatchResultDto(
                tier = matchResult.tier.name,
                bestCandidate = matchResult.bestCandidate?.let {
                    MatchCandidateDto(
                        catalogCoinId = it.catalogCoin?.id?.toString(),
                        providerName = it.providerName,
                        externalId = it.externalId,
                        score = it.score,
                        scoreBreakdown = it.scoreBreakdown,
                    )
                },
                allCandidates = matchResult.allCandidates.map {
                    MatchCandidateDto(
                        catalogCoinId = it.catalogCoin?.id?.toString(),
                        providerName = it.providerName,
                        externalId = it.externalId,
                        score = it.score,
                        scoreBreakdown = it.scoreBreakdown,
                    )
                },
                retrievalKey = matchResult.retrievalKey,
            )

            call.respond(
                ApiResponse(
                    success = true,
                    data = dto,
                    timestampMillis = timeProvider.nowMillis(),
                )
            )
        }.describe {
            summary = "Debug Numista matching for a recognition result"
        }

        get("/debug/numista-metrics") {
            val snapshot = MatchMetrics.snapshot()
            call.respond(
                ApiResponse(
                    success = true,
                    data = MetricsResponseDto(
                        attemptsTotal = snapshot.attemptsTotal,
                        matchedTotal = snapshot.matchedTotal,
                        ambiguousTotal = snapshot.ambiguousTotal,
                        noMatchTotal = snapshot.noMatchTotal,
                        numistaCallsTotal = snapshot.numistaCallsTotal,
                        cacheHitsTotal = snapshot.cacheHitsTotal,
                        avgCandidatesPerMatch = snapshot.avgCandidates,
                    ),
                    timestampMillis = timeProvider.nowMillis(),
                )
            )
        }.describe {
            summary = "Get enrichment matching metrics"
        }
    }

    private fun mapToRecognitionResult(dto: RecognitionResultDto): RecognitionResult =
        RecognitionResult(
            overallConfidence = runCatching { Confidence.valueOf(dto.overallConfidence.uppercase()) }.getOrDefault(Confidence.LOW),
            countryOrIssuer = dto.countryOrIssuer,
            denomination = dto.denomination,
            seriesName = dto.seriesName,
            year = dto.year,
            era = dto.era,
            confidenceCountry = dto.confidenceCountry,
            confidenceDenomination = dto.confidenceDenomination,
            confidenceSeries = dto.confidenceSeries,
            confidenceYear = dto.confidenceYear,
            confidenceEra = dto.confidenceEra,
            mintMark = dto.mintMark,
            mintMarkStatus = dto.mintMarkStatus,
            mintMarkConfidence = dto.mintMarkConfidence,
            metalComposition = dto.metalComposition,
            estimatedGrade = dto.estimatedGrade,
            estimatedGradeValue = dto.estimatedGradeValue,
            gradeCode = dto.gradeCode,
            gradeConfidence = dto.gradeConfidence,
            rarityQualitative = dto.rarityQualitative,
            rarityScore = dto.rarityScore,
            valueLow = dto.valueLow,
            valueHigh = dto.valueHigh,
            valueCurrency = dto.valueCurrency,
            mintage = dto.mintage,
            obverseDescription = dto.obverseDescription,
            reverseDescription = dto.reverseDescription,
            weightGrams = dto.weightGrams,
            diameterMm = dto.diameterMm,
            thicknessMm = dto.thicknessMm,
            edge = dto.edge,
            designerObverse = dto.designerObverse,
            designerReverse = dto.designerReverse,
            positiveFeatures = dto.positiveFeatures,
            negativeFeatures = dto.negativeFeatures,
            supplySummary = dto.supplySummary,
            demandSummary = dto.demandSummary,
            valueDisclaimer = dto.valueDisclaimer,
            obverseLettering = dto.obverseLettering,
            reverseLettering = dto.reverseLettering,
            analysisNotes = dto.analysisNotes,
            historicalContext = dto.historicalContext,
            obverseVisible = dto.obverseVisible,
            reverseVisible = dto.reverseVisible,
            imageFocus = dto.imageFocus,
            imageLighting = dto.imageLighting,
            imageResolution = dto.imageResolution,
            imageCropping = dto.imageCropping,
            imageIssues = dto.imageIssues,
            rawJson = dto.rawJson,
        )
}
