@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.AiAnalysisDto
import com.vlatkogalev.app.api.dto.CoinDataDto
import com.vlatkogalev.app.api.dto.DebugNumistaMatchRequest
import com.vlatkogalev.app.api.dto.MatchCandidateDto
import com.vlatkogalev.app.api.dto.MatchResultDto
import com.vlatkogalev.app.api.dto.MetricsResponseDto
import com.vlatkogalev.domain.coin.model.Confidence
import com.vlatkogalev.domain.coin.model.MatchCandidate
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

            val recognitionResult = mapToRecognitionResult(payload.coinData, payload.aiAnalysis)
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
                        dataCompleteness = completenessOf(it),
                    )
                },
                allCandidates = matchResult.allCandidates.map {
                    MatchCandidateDto(
                        catalogCoinId = it.catalogCoin?.id?.toString(),
                        providerName = it.providerName,
                        externalId = it.externalId,
                        score = it.score,
                        scoreBreakdown = it.scoreBreakdown,
                        dataCompleteness = completenessOf(it),
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

    private fun mapToRecognitionResult(coinData: CoinDataDto, aiAnalysis: AiAnalysisDto): RecognitionResult =
        RecognitionResult(
            overallConfidence = runCatching { Confidence.valueOf(aiAnalysis.overallConfidence.uppercase()) }.getOrDefault(Confidence.LOW),
            countryOrIssuer = coinData.countryOrIssuer,
            denomination = coinData.denomination,
            seriesName = coinData.seriesName,
            year = coinData.year,
            era = coinData.era,
            confidenceCountry = aiAnalysis.confidenceCountry,
            confidenceDenomination = aiAnalysis.confidenceDenomination,
            confidenceSeries = aiAnalysis.confidenceSeries,
            confidenceYear = aiAnalysis.confidenceYear,
            confidenceEra = aiAnalysis.confidenceEra,
            mintMark = coinData.mintMark,
            mintMarkStatus = aiAnalysis.mintMarkStatus,
            mintMarkConfidence = aiAnalysis.mintMarkConfidence,
            metalComposition = coinData.metalComposition,
            estimatedGrade = aiAnalysis.estimatedGrade,
            estimatedGradeValue = aiAnalysis.estimatedGradeValue,
            gradeCode = aiAnalysis.gradeCode,
            gradeConfidence = aiAnalysis.gradeConfidence,
            rarityQualitative = aiAnalysis.rarityQualitative,
            rarityScore = aiAnalysis.rarityScore,
            valueLow = aiAnalysis.valueLow,
            valueHigh = aiAnalysis.valueHigh,
            valueCurrency = aiAnalysis.valueCurrency,
            mintage = coinData.mintage,
            obverseDescription = coinData.obverseDescription,
            reverseDescription = coinData.reverseDescription,
            weightGrams = coinData.weightGrams,
            diameterMm = coinData.diameterMm,
            thicknessMm = coinData.thicknessMm,
            edge = coinData.edge,
            designerObverse = coinData.designerObverse,
            designerReverse = coinData.designerReverse,
            positiveFeatures = aiAnalysis.positiveFeatures,
            negativeFeatures = aiAnalysis.negativeFeatures,
            supplySummary = aiAnalysis.supplySummary,
            demandSummary = aiAnalysis.demandSummary,
            valueDisclaimer = aiAnalysis.valueDisclaimer,
            obverseLettering = coinData.obverseLettering,
            reverseLettering = coinData.reverseLettering,
            analysisNotes = aiAnalysis.analysisNotes,
            historicalContext = coinData.historicalContext,
            obverseVisible = aiAnalysis.obverseVisible,
            reverseVisible = aiAnalysis.reverseVisible,
            imageFocus = aiAnalysis.imageFocus,
            imageLighting = aiAnalysis.imageLighting,
            imageResolution = aiAnalysis.imageResolution,
            imageCropping = aiAnalysis.imageCropping,
            imageIssues = aiAnalysis.imageIssues,
            rawJson = aiAnalysis.rawJson,
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
}
