package com.vlatkogalev.domain.coin.service

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoinSetServiceTest {
    private val coinRepository = FakeCoinRepository()
    private val coinSetRepository = FakeCoinSetRepository(coinRepository)
    private lateinit var service: CoinSetServiceImpl

    @BeforeTest
    fun setup() {
        coinRepository.reset()
        coinSetRepository.reset()
        service = CoinSetServiceImpl(coinSetRepository, coinRepository)
    }

    @Test
    fun createSet_success() {
        val set = assertSuccess(service.createSet(TestFixtures.USER_ID, "Ancients", "Old coins")).value

        assertEquals(TestFixtures.USER_ID, set.userId)
        assertEquals("Ancients", set.name)
        assertEquals("Old coins", set.description)
    }

    @Test
    fun addCoinsToSet_movesCoinFromOtherSet() {
        val coin = coinRepository.insert(TestFixtures.makeCoin())
        val firstSet = assertSuccess(service.createSet(TestFixtures.USER_ID, "First", null)).value
        val secondSet = assertSuccess(service.createSet(TestFixtures.USER_ID, "Second", null)).value

        assertSuccess(service.addCoinsToSet(firstSet.id, TestFixtures.USER_ID, listOf(coin.id)))
        val updatedSecond = assertSuccess(service.addCoinsToSet(secondSet.id, TestFixtures.USER_ID, listOf(coin.id))).value

        assertEquals(listOf(coin.id), updatedSecond.coinIds)
        assertEquals(secondSet.id, coinRepository.findById(coin.id)?.setId)
    }

    @Test
    fun removeCoinsFromSet_clearsMembership() {
        val coin = coinRepository.insert(TestFixtures.makeCoin())
        val set = assertSuccess(service.createSet(TestFixtures.USER_ID, "Set", null)).value
        assertSuccess(service.addCoinsToSet(set.id, TestFixtures.USER_ID, listOf(coin.id)))

        val updated = assertSuccess(service.removeCoinsFromSet(set.id, TestFixtures.USER_ID, listOf(coin.id))).value

        assertEquals(emptyList(), updated.coinIds)
        assertNull(coinRepository.findById(coin.id)?.setId)
    }

    @Test
    fun deleteSet_doesNotDeleteCoins() {
        val coin = coinRepository.insert(TestFixtures.makeCoin())
        val set = assertSuccess(service.createSet(TestFixtures.USER_ID, "Set", null)).value
        assertSuccess(service.addCoinsToSet(set.id, TestFixtures.USER_ID, listOf(coin.id)))

        assertSuccess(service.deleteSet(set.id, TestFixtures.USER_ID))

        assertEquals(coin.id, coinRepository.findById(coin.id)?.id)
        assertNull(coinRepository.findById(coin.id)?.setId)
    }

    @Test
    fun listSets_returnsOnlyUsersSets() {
        val own = assertSuccess(service.createSet(TestFixtures.USER_ID, "Own", null)).value
        assertSuccess(service.createSet(TestFixtures.OTHER_USER_ID, "Other", null))

        val sets = assertSuccess(service.listSets(TestFixtures.USER_ID)).value

        assertEquals(listOf(own.id), sets.map { it.id })
    }

    @Test
    fun addCoinsToSet_withOtherUsersCoin_returnsFailure() {
        val coin = coinRepository.insert(TestFixtures.makeCoin(userId = TestFixtures.OTHER_USER_ID))
        val set = assertSuccess(service.createSet(TestFixtures.USER_ID, "Set", null)).value

        val failure = assertFailure(service.addCoinsToSet(set.id, TestFixtures.USER_ID, listOf(coin.id)))

        assertEquals("One or more coin IDs are invalid or do not belong to the user", failure.reason)
    }

    private fun <T> assertSuccess(result: com.vlatkogalev.platform.core.Result<T>): com.vlatkogalev.platform.core.Result.Success<T> =
        kotlin.test.assertIs<com.vlatkogalev.platform.core.Result.Success<T>>(result)

    private fun assertFailure(result: com.vlatkogalev.platform.core.Result<*>): com.vlatkogalev.platform.core.Result.Failure =
        kotlin.test.assertIs<com.vlatkogalev.platform.core.Result.Failure>(result)
}
