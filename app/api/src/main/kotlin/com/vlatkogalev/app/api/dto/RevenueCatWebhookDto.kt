package com.vlatkogalev.app.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RevenueCatWebhookRequest(
    val event: RevenueCatWebhookEvent,
)

@Serializable
data class RevenueCatWebhookEvent(
    val type: String,
    @SerialName("app_user_id")
    val appUserId: String? = null,
    @SerialName("original_app_user_id")
    val originalAppUserId: String? = null,
    @SerialName("expiration_at_ms")
    val expirationAtMillis: Long? = null,
    @SerialName("product_id")
    val productId: String? = null,
    @SerialName("entitlement_id")
    val entitlementId: String? = null,
)
