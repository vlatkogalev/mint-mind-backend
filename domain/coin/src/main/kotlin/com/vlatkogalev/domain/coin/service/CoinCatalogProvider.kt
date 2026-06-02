package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.platform.core.Result

interface CoinCatalogProvider {
    val providerName: String

    fun findCandidates(fingerprint: CoinFingerprint): Result<List<CoinCatalogCandidate>>
}
