package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CoinSortField
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListCoinsTest : CoinServiceTestBase() {
    @Test
    fun listCoins_sortsByValueHighToLow() {
        val low = repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(valueLow = 10.0, valueHigh = 20.0)))
        val high = repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(valueLow = 100.0, valueHigh = 200.0)))

        val coins = assertSuccess(runBlocking { service.listCoins(TestFixtures.USER_ID, null, null, null, null, null, CoinSortField.VALUE_HIGH_TO_LOW, 20, null) }).value

        assertEquals(listOf(high.id, low.id), coins.map { it.id })
    }

    @Test
    fun listCoins_sortsByReleaseYearOldToNew() {
        val newer = repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(year = 2000)))
        val older = repo.insert(TestFixtures.makeCoin(recognitionResult = TestFixtures.makeRecognitionResult(year = 1800)))

        val coins = assertSuccess(runBlocking { service.listCoins(TestFixtures.USER_ID, null, null, null, null, null, CoinSortField.RELEASE_YEAR_OLD_TO_NEW, 20, null) }).value

        assertEquals(listOf(older.id, newer.id), coins.map { it.id })
    }

    @Test
    fun listCoins_sortsByDateAddedNewToOldByDefault() {
        val older = repo.insert(TestFixtures.makeCoin(createdAt = Instant.parse("2024-01-01T00:00:00Z")))
        val newer = repo.insert(TestFixtures.makeCoin(createdAt = Instant.parse("2024-02-01T00:00:00Z")))

        val coins = assertSuccess(runBlocking { service.listCoins(TestFixtures.USER_ID, null, null, null, null, null, CoinSortField.DATE_ADDED_NEW_TO_OLD, 20, null) }).value

        assertEquals(listOf(newer.id, older.id), coins.map { it.id })
    }

    @Test
    fun listCoins_filtersBySetId_returnsOnlyCoinsInThatSet() {
        val setId = UUID.randomUUID()
        val inSet = repo.insert(TestFixtures.makeCoin(setId = setId))
        repo.insert(TestFixtures.makeCoin(setId = null))
        repo.insert(TestFixtures.makeCoin(setId = UUID.randomUUID()))

        val coins = assertSuccess(runBlocking { service.listCoins(TestFixtures.USER_ID, null, null, null, null, setId, CoinSortField.DATE_ADDED_NEW_TO_OLD, 20, null) }).value

        assertEquals(listOf(inSet.id), coins.map { it.id })
    }

    @Test
    fun listCoins_filtersBySetId_returnsEmptyWhenNoneMatch() {
        repo.insert(TestFixtures.makeCoin(setId = UUID.randomUUID()))

        val coins = assertSuccess(runBlocking { service.listCoins(TestFixtures.USER_ID, null, null, null, null, UUID.randomUUID(), CoinSortField.DATE_ADDED_NEW_TO_OLD, 20, null) }).value

        assertTrue(coins.isEmpty())
    }

    @Test
    fun listCoins_withNullSetId_returnsAllCoinsRegardlessOfSetMembership() {
        val withSet = repo.insert(TestFixtures.makeCoin(setId = UUID.randomUUID()))
        val withoutSet = repo.insert(TestFixtures.makeCoin(setId = null))

        val coins = assertSuccess(runBlocking { service.listCoins(TestFixtures.USER_ID, null, null, null, null, null, CoinSortField.DATE_ADDED_NEW_TO_OLD, 20, null) }).value

        assertEquals(setOf(withSet.id, withoutSet.id), coins.map { it.id }.toSet())
    }

    @Test
    fun listCoins_filtersBySetId_doesNotReturnOtherUsersCoinsInSameSet() {
        val setId = UUID.randomUUID()
        val ownCoin = repo.insert(TestFixtures.makeCoin(userId = TestFixtures.USER_ID, setId = setId))
        repo.insert(TestFixtures.makeCoin(userId = TestFixtures.OTHER_USER_ID, setId = setId))

        val coins = assertSuccess(runBlocking { service.listCoins(TestFixtures.USER_ID, null, null, null, null, setId, CoinSortField.DATE_ADDED_NEW_TO_OLD, 20, null) }).value

        assertEquals(listOf(ownCoin.id), coins.map { it.id })
    }
}
