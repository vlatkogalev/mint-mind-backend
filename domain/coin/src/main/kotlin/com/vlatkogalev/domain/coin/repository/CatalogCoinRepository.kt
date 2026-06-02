package com.vlatkogalev.domain.coin.repository

import com.vlatkogalev.domain.coin.model.CatalogCoin
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import java.util.UUID

interface CatalogCoinRepository {
    fun findByFingerprint(fingerprint: CoinFingerprint): CatalogCoin?
    fun findById(id: UUID): CatalogCoin?
    fun findByProviderExternalId(provider: String, externalId: String): CatalogCoin?
    fun save(catalogCoin: CatalogCoin): CatalogCoin
    fun markEnrichmentSuccess(catalogCoinId: UUID, now: java.time.Instant): CatalogCoin?
    fun markEnrichmentFailed(catalogCoinId: UUID, now: java.time.Instant, error: String?): CatalogCoin?
    fun saveExternalReference(reference: ExternalCoinReference): ExternalCoinReference
    fun findExternalReference(catalogCoinId: UUID, provider: String): ExternalCoinReference?
}
