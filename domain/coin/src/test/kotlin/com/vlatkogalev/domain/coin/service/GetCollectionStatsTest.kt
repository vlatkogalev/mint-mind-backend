package com.vlatkogalev.domain.coin.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetCollectionStatsTest : CoinServiceTestBase() {
    @Test
    fun getCollectionStats_success_computesTotalsAndHighlights() {
        val valuable = repo.insert(
            TestFixtures.makeCoin(
                recognitionResult = TestFixtures.makeRecognitionResult(
                    countryOrIssuer = "Rome",
                    year = 250,
                    valueLow = 900.0,
                    valueHigh = 1_100.0,
                    mintage = 20_000,
                ),
            ),
        )
        val ancient = repo.insert(
            TestFixtures.makeCoin(
                recognitionResult = TestFixtures.makeRecognitionResult(
                    countryOrIssuer = "Greece",
                    year = -300,
                    valueLow = 100.0,
                    valueHigh = 200.0,
                    mintage = 10_000,
                ),
            ),
        )
        val rarest = repo.insert(
            TestFixtures.makeCoin(
                recognitionResult = TestFixtures.makeRecognitionResult(
                    countryOrIssuer = "Rome",
                    year = 100,
                    valueLow = 400.0,
                    valueHigh = 500.0,
                    mintage = 50,
                ),
            ),
        )
        repo.insert(TestFixtures.makeCoin(userId = TestFixtures.OTHER_USER_ID))

        val stats = assertSuccess(service.getCollectionStats(TestFixtures.USER_ID)).value

        assertEquals(3, stats.totalCoins)
        assertEquals(2, stats.totalIssuers)
        assertEquals(533.3333333333334, stats.estimatedTotalValueMean)
        assertEquals(valuable.id, stats.highlights.mostValuable?.id)
        assertEquals(ancient.id, stats.highlights.mostAncient?.id)
        assertEquals(rarest.id, stats.highlights.rarest?.id)
    }

    @Test
    fun getCollectionStats_withoutMintage_returnsNullRarest() {
        repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(mintage = null)))

        val stats = assertSuccess(service.getCollectionStats(TestFixtures.USER_ID)).value

        assertNull(stats.highlights.rarest)
    }
}
