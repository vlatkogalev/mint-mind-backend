package com.vlatkogalev.domain.pricing.service

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.platform.core.Result

interface CoinPricingService {
    suspend fun getPricing(coin: Coin, minResults: Int = 3): Result<CoinPricingResult>
}
