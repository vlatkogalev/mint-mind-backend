package com.vlatkogalev.domain.billing

interface BillingService {
    fun healthCheck(): Boolean
}

class NoopBillingService : BillingService {
    override fun healthCheck(): Boolean = true
}
