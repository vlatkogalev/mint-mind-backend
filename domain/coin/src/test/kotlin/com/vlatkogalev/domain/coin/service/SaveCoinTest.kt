package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.Confidence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveCoinTest : CoinServiceTestBase() {

    @Test
    fun saveCoin_success_returnsCreatedCoin() {
        val result = service.saveCoin(
            userId = TestFixtures.USER_ID,
            obverseKey = "users/obverse.jpg",
            reverseKey = "users/reverse.jpg",
            recognitionResult = TestFixtures.makeRecognitionResult(),
            catalogueNumbers = listOf(TestFixtures.makeCatalogueNumber()),
            notes = null,
        )

        val coin = assertSuccess(result).value
        assertEquals(TestFixtures.USER_ID, coin.userId)
        assertEquals("users/obverse.jpg", coin.obverseKey)
        assertEquals("users/reverse.jpg", coin.reverseKey)
        assertNull(coin.notes)
    }

    @Test
    fun saveCoin_success_generatesUniqueId() {
        val result1 = service.saveCoin(TestFixtures.USER_ID, "k1", "k2", TestFixtures.makeRecognitionResult(), emptyList(), null)
        val result2 = service.saveCoin(TestFixtures.USER_ID, "k3", "k4", TestFixtures.makeRecognitionResult(), emptyList(), null)

        val id1 = assertSuccess(result1).value.id
        val id2 = assertSuccess(result2).value.id
        assertTrue(id1 != id2)
    }

    @Test
    fun saveCoin_success_preservesRecognitionResult() {
        val recognition = TestFixtures.makeRecognitionResult(
            overallConfidence = Confidence.MEDIUM,
            countryOrIssuer = "Germany",
            year = 1938,
            valueLowUsd = 100.0,
            valueHighUsd = 250.0,
        )

        val coin = assertSuccess(service.saveCoin(TestFixtures.USER_ID, "k1", "k2", recognition, emptyList(), null)).value

        assertEquals(Confidence.MEDIUM, coin.recognitionResult.overallConfidence)
        assertEquals("Germany", coin.recognitionResult.countryOrIssuer)
        assertEquals(1938, coin.recognitionResult.year)
        assertEquals(100.0, coin.recognitionResult.valueLowUsd)
        assertEquals(250.0, coin.recognitionResult.valueHighUsd)
    }

    @Test
    fun saveCoin_success_preservesCatalogueNumbers() {
        val numbers = listOf(
            TestFixtures.makeCatalogueNumber("Krause", "KM# 110", Confidence.HIGH),
            TestFixtures.makeCatalogueNumber("Yeoman", "Y# 25", Confidence.MEDIUM),
        )

        val coin = assertSuccess(service.saveCoin(TestFixtures.USER_ID, "k1", "k2", TestFixtures.makeRecognitionResult(), numbers, null)).value

        assertEquals(2, coin.catalogueNumbers.size)
        assertEquals("Krause", coin.catalogueNumbers[0].catalogueName)
        assertEquals("KM# 110", coin.catalogueNumbers[0].number)
        assertEquals("Yeoman", coin.catalogueNumbers[1].catalogueName)
    }

    @Test
    fun saveCoin_success_preservesNotes() {
        val coin = assertSuccess(
            service.saveCoin(TestFixtures.USER_ID, "k1", "k2", TestFixtures.makeRecognitionResult(), emptyList(), "Inherited from grandfather"),
        ).value

        assertEquals("Inherited from grandfather", coin.notes)
    }

    @Test
    fun saveCoin_success_persistsCoinInRepository() {
        val coin = assertSuccess(service.saveCoin(TestFixtures.USER_ID, "k1", "k2", TestFixtures.makeRecognitionResult(), emptyList(), null)).value

        assertNotNull(repo.findById(coin.id))
    }

    @Test
    fun saveCoin_withEmptyCatalogueNumbers_succeeds() {
        val result = service.saveCoin(TestFixtures.USER_ID, "k1", "k2", TestFixtures.makeRecognitionResult(), emptyList(), null)

        val coin = assertSuccess(result).value
        assertTrue(coin.catalogueNumbers.isEmpty())
    }

    @Test
    fun saveCoin_withRepositoryException_returnsFailure() {
        repo.throwOnSave = true

        val result = service.saveCoin(TestFixtures.USER_ID, "k1", "k2", TestFixtures.makeRecognitionResult(), emptyList(), null)

        assertEquals("save failed", assertFailure(result).reason)
    }
}