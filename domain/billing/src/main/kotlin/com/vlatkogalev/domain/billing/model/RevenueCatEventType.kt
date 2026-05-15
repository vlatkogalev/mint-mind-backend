package com.vlatkogalev.domain.billing.model

enum class RevenueCatEventType {
    INITIAL_PURCHASE,
    RENEWAL,
    CANCELLATION,
    EXPIRATION,
    UNCANCELLATION,
}