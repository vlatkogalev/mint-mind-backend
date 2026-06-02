package com.vlatkogalev.domain.coin.repository

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import java.util.UUID

interface CatalogCoinRepository {
    suspend fun findByFingerprint(fingerprint: CoinFingerprint): CatalogCoin?
    suspend fun findById(id: UUID): CatalogCoin?
    suspend fun findByIds(ids: List<UUID>): List<CatalogCoin>
    suspend fun findByProviderExternalId(provider: String, externalId: String): CatalogCoin?
    suspend fun save(catalogCoin: CatalogCoin): CatalogCoin
    suspend fun markEnrichmentSuccess(catalogCoinId: UUID, now: java.time.Instant, candidate: CoinCatalogCandidate): CatalogCoin?
    suspend fun markEnrichmentFailed(catalogCoinId: UUID, now: java.time.Instant, error: String?): CatalogCoin?
    suspend fun saveExternalReference(reference: ExternalCoinReference): ExternalCoinReference
    suspend fun findExternalReference(catalogCoinId: UUID, provider: String): ExternalCoinReference?
}
