package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CountryAliasMapping
import com.vlatkogalev.domain.coin.model.DenominationAliasMapping
import com.vlatkogalev.domain.coin.model.MatchCandidate
import com.vlatkogalev.domain.coin.model.MatchableCoin
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.domain.coin.model.toMatchableCoin
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.platform.core.Result

class CandidateFetcher(
    private val catalogCoinRepository: CatalogCoinRepository,
    private val providers: List<CoinCatalogProvider>,
) {
    suspend fun fetchDbCoins(recognition: RecognitionResult): List<CatalogCoin> =
        catalogCoinRepository.findByRetrievalKey(
            CountryAliasMapping.normalize(recognition.countryOrIssuer),
            DenominationAliasMapping.normalize(recognition.denomination),
            recognition.year,
        )

    fun toDbCandidates(dbCoins: List<CatalogCoin>): List<MatchCandidate> =
        dbCoins
            .filter { it.enrichedAt != null }
            .map { coin ->
                MatchCandidate(
                    catalogCoin = coin,
                    matchableCoin = coin.toMatchableCoin(),
                    providerName = "Numista",
                    externalId = null,
                    score = 0,
                    scoreBreakdown = emptyMap(),
                )
            }

    suspend fun fetchProviderCandidates(recognition: RecognitionResult): List<MatchCandidate> =
        providers.flatMap { provider ->
            when (val result = provider.findCandidates(recognition.toFingerprint())) {
                is Result.Success -> result.value
                is Result.Failure -> emptyList()
            }
        }.map { candidate ->
            val existingCatalogCoin = catalogCoinRepository.findByProviderExternalId(
                "Numista", candidate.externalReference.externalId
            )
            val matchableCoin = MatchableCoin(
                countryOrIssuer = candidate.countryOrIssuer,
                denomination = candidate.denomination,
                yearStart = candidate.yearStart,
                yearEnd = candidate.yearEnd,
                composition = candidate.composition,
                weightGrams = candidate.weightGrams,
                diameterMm = candidate.diameterMm,
                obverseLettering = candidate.obverseLettering,
                reverseLettering = candidate.reverseLettering,
                designers = candidate.designers,
                thicknessMm = candidate.thicknessMm,
            )
            MatchCandidate(
                catalogCoin = existingCatalogCoin,
                matchableCoin = matchableCoin,
                providerName = "Numista",
                externalId = candidate.externalReference.externalId,
                score = 0,
                scoreBreakdown = emptyMap(),
                catalogCandidate = candidate,
            )
        }

    fun deduplicate(candidates: List<MatchCandidate>): List<MatchCandidate> =
        candidates.distinctBy { candidate ->
            when {
                candidate.externalId != null -> "${candidate.providerName}:${candidate.externalId}"
                candidate.catalogCoin?.id != null -> "catalog:${candidate.catalogCoin.id}"
                else -> "candidate:${candidate.matchableCoin.countryOrIssuer}|${candidate.matchableCoin.denomination}|${candidate.matchableCoin.yearStart}"
            }
        }
}
