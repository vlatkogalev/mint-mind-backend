package com.vlatkogalev.platform.billing

interface BillingService {
    fun healthCheck(): Boolean
}

class NoopBillingService : BillingService {
    override fun healthCheck(): Boolean = true
}
