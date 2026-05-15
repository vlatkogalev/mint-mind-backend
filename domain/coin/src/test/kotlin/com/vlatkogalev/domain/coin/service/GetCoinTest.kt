package com.vlatkogalev.domain.coin.service

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCoinTest : CoinServiceTestBase() {

    @Test
    fun getCoin_withUnknownId_returnsFailure() {
        val result = service.getCoin(UUID.randomUUID(), TestFixtures.USER_ID)

        assertEquals("Coin not found", assertFailure(result).reason)
    }

    @Test
    fun getCoin_withWrongUserId_returnsFailure() {
        val coin = repo.insert(TestFixtures.makeCoin(userId = TestFixtures.USER_ID))

        val result = service.getCoin(coin.id, TestFixtures.OTHER_USER_ID)

        assertEquals("Coin not found", assertFailure(result).reason)
    }

    @Test
    fun getCoin_success_returnsCoin() {
        val coin = repo.insert(TestFixtures.makeCoin(userId = TestFixtures.USER_ID))

        val result = service.getCoin(coin.id, TestFixtures.USER_ID)

        assertEquals(coin.id, assertSuccess(result).value.id)
        assertEquals(coin.userId, assertSuccess(result).value.userId)
    }

    @Test
    fun getCoin_success_returnsCorrectFields() {
        val coin = repo.insert(
            TestFixtures.makeCoin(
                userId = TestFixtures.USER_ID,
                obverseKey = "users/obverse.jpg",
                reverseKey = "users/reverse.jpg",
                notes = "My favourite coin",
            ),
        )

        val fetched = assertSuccess(service.getCoin(coin.id, TestFixtures.USER_ID)).value

        assertEquals("users/obverse.jpg", fetched.obverseKey)
        assertEquals("users/reverse.jpg", fetched.reverseKey)
        assertEquals("My favourite coin", fetched.notes)
    }

    @Test
    fun getCoin_doesNotExposeCoinsBelongingToOtherUsers() {
        val otherCoin = repo.insert(TestFixtures.makeCoin(userId = TestFixtures.OTHER_USER_ID))

        val result = service.getCoin(otherCoin.id, TestFixtures.USER_ID)

        assertEquals("Coin not found", assertFailure(result).reason)
    }

    @Test
    fun getCoin_withRepositoryException_returnsFailure() {
        repo.throwOnFindById = true

        val result = service.getCoin(UUID.randomUUID(), TestFixtures.USER_ID)

        assertEquals("findById failed", assertFailure(result).reason)
    }
}