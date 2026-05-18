package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateCoinSetRequest(
    val name: String,
    val description: String? = null,
) {
    fun validate(): String? {
        if (name.isBlank()) return "name is required"
        if (name.length > 255) return "name must be 255 characters or less"
        return null
    }
}

@Serializable
data class UpdateCoinSetRequest(
    val name: String,
    val description: String? = null,
) {
    fun validate(): String? {
        if (name.isBlank()) return "name is required"
        if (name.length > 255) return "name must be 255 characters or less"
        return null
    }
}

@Serializable
data class ModifySetCoinsRequest(
    val coinIds: List<String>,
) {
    fun validate(): String? {
        if (coinIds.isEmpty()) return "coinIds must not be empty"
        return null
    }
}

@Serializable
data class CoinSetResponse(
    val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val coinIds: List<String>,
    val createdAt: String,
)
