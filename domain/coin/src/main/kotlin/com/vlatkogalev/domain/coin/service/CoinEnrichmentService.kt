package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.platform.core.Result
import java.util.UUID

interface CoinEnrichmentService {
    suspend fun getOrEnrich(fingerprint: CoinFingerprint): CatalogCoin?
    suspend fun enrichById(catalogCoinId: UUID): Result<CatalogCoin>
}
