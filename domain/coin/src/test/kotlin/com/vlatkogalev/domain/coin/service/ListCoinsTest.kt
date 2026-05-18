package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CoinSortField
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ListCoinsTest : CoinServiceTestBase() {
    @Test
    fun listCoins_sortsByValueHighToLow() {
        val low = repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(valueLowUsd = 10.0, valueHighUsd = 20.0)))
        val high = repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(valueLowUsd = 100.0, valueHighUsd = 200.0)))

        val coins = assertSuccess(service.listCoins(TestFixtures.USER_ID, null, null, null, null, CoinSortField.VALUE_HIGH_TO_LOW, 20, 0)).value

        assertEquals(listOf(high.id, low.id), coins.map { it.id })
    }

    @Test
    fun listCoins_sortsByReleaseYearOldToNew() {
        val newer = repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(year = 2000)))
        val older = repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(year = 1800)))

        val coins = assertSuccess(service.listCoins(TestFixtures.USER_ID, null, null, null, null, CoinSortField.RELEASE_YEAR_OLD_TO_NEW, 20, 0)).value

        assertEquals(listOf(older.id, newer.id), coins.map { it.id })
    }

    @Test
    fun listCoins_sortsByDateAddedNewToOldByDefault() {
        val older = repo.insert(TestFixtures.makeCoin(createdAt = Instant.parse("2024-01-01T00:00:00Z")))
        val newer = repo.insert(TestFixtures.makeCoin(createdAt = Instant.parse("2024-02-01T00:00:00Z")))

        val coins = assertSuccess(service.listCoins(TestFixtures.USER_ID, null, null, null, null, CoinSortField.DATE_ADDED_NEW_TO_OLD, 20, 0)).value

        assertEquals(listOf(newer.id, older.id), coins.map { it.id })
    }
}
