package com.vlatkogalev.domain.pricing.service

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.platform.core.Result

interface CoinPricingService {
    /**
     * Fetches recent sold listings for a coin.
     *
     * Tries two passes:
     * 1. With grade (narrow) — if >= [minResults] results come back, return immediately.
     * 2. Without grade (broad) — fallback when pass 1 is thin.
     */
    fun getPricing(coin: Coin, minResults: Int = 3): Result<CoinPricingResult>
}