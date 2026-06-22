@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

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

            val recognitionResult = mapToRecognitionResult(payload)
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

    private fun mapToRecognitionResult(req: DebugNumistaMatchRequest): RecognitionResult =
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
            rawJson = req.rawJson,
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
