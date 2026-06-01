package com.vlatkogalev.data.ebay

import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.Confidence
import com.vlatkogalev.domain.coin.model.RecognitionResult
import com.vlatkogalev.platform.core.config.EbayConfig
import com.vlatkogalev.platform.core.config.EbayEnvironment
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EbayCoinPricingServiceQueryTest {

    private val service = EbayCoinPricingService(
        config = EbayConfig(
            oauthEndpoint = "https://api.sandbox.ebay.com/identity/v1/oauth2/token",
            oauthScope = "https://api.ebay.com/oauth/api_scope/buy.marketplace.search",
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            marketplaceId = "EBAY_US",
            environment = EbayEnvironment.SANDBOX,
            maxResultsPerQuery = 10,
            feedPagesToFetch = 5,
            feedRefreshIntervalSeconds = 600,
        ),
        tokenProvider = EbayTokenProvider(
            config = EbayConfig(
                oauthEndpoint = "https://api.sandbox.ebay.com/identity/v1/oauth2/token",
                oauthScope = "https://api.ebay.com/oauth/api_scope/buy.marketplace.search",
                clientId = "test-client-id",
                clientSecret = "test-client-secret",
                marketplaceId = "EBAY_US",
                environment = EbayEnvironment.SANDBOX,
                maxResultsPerQuery = 10,
                feedPagesToFetch = 5,
                feedRefreshIntervalSeconds = 600,
            ),
        ),
    )

    @Test
    fun buildQuery_withAllFields_includesKrauseYearDenominationCountry() {
        val coin = makeCoin(
            krauseNumbers = listOf("KM# 110"),
            country = "United States",
            year = 1921,
            denomination = "1 Dollar",
            seriesName = "Morgan Dollar",
            gradeValue = "VF-30",
        )

        val query = service.buildQuery(coin, includeGrade = true)

        assertContains(query, "KM# 110")
        assertContains(query, "United States")
        assertContains(query, "1921")
        assertContains(query, "1 Dollar")
        assertContains(query, "Morgan Dollar")
        assertContains(query, "VF-30")
    }

    @Test
    fun buildQuery_withIncludeGradeFalse_omitsGrade() {
        val coin = makeCoin(gradeValue = "VF-30", grade = "Very Fine (VF)")

        val query = service.buildQuery(coin, includeGrade = false)

        assertFalse(query.contains("VF-30"))
        assertFalse(query.contains("Very Fine"))
    }

    @Test
    fun buildQuery_withNoKrause_stillBuildsUsefulQuery() {
        val coin = makeCoin(krauseNumbers = emptyList(), country = "Germany", year = 1938)

        val query = service.buildQuery(coin, includeGrade = false)

        assertContains(query, "Germany")
        assertContains(query, "1938")
        assertTrue(query.isNotBlank())
    }

    @Test
    fun buildQuery_prefersGradeValueOverGradeLongForm() {
        val coin = makeCoin(grade = "Very Fine", gradeValue = "VF-30")

        val query = service.buildQuery(coin, includeGrade = true)

        assertContains(query, "VF-30")
        assertFalse(query.contains("Very Fine"))
    }

    @Test
    fun buildQuery_withNullOptionalFields_returnsBlank() {
        val coin = makeCoin(
            krauseNumbers = emptyList(),
            country = null,
            year = null,
            denomination = null,
            seriesName = null,
            grade = null,
            gradeValue = null,
        )

        val query = service.buildQuery(coin, includeGrade = true)

        assertTrue(query.isBlank())
    }

    private fun makeCoin(
        krauseNumbers: List<String> = listOf("KM# 110"),
        country: String? = "United States",
        year: Int? = 1921,
        denomination: String? = "1 Dollar",
        seriesName: String? = "Morgan Dollar",
        grade: String? = "Very Fine (VF)",
        gradeValue: String? = "VF-30",
    ) = Coin(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        obverseKey = "obverse.jpg",
        reverseKey = "reverse.jpg",
        recognitionResult = RecognitionResult(
            overallConfidence = Confidence.HIGH,
            countryOrIssuer = country,
            denomination = denomination,
            seriesName = seriesName,
            year = year,
            mintMark = null,
            metalComposition = null,
            estimatedGrade = grade,
            estimatedGradeValue = gradeValue,
            rarityQualitative = null,
            valueLow = null,
            valueHigh = null,
            mintage = null,
            obverseDescription = null,
            reverseDescription = null,
            historicalContext = null,
            rawJson = "{}",
        ),
        catalogueNumbers = krauseNumbers.map {
            CatalogueNumber(catalogueName = "Krause", number = it, confidence = Confidence.HIGH)
        },
        setId = null,
        notes = null,
        createdAt = Instant.now(),
    )
}
