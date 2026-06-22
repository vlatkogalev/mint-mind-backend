package com.vlatkogalev.domain.coin.model

import com.vlatkogalev.platform.core.DomainError

sealed interface CoinError : DomainError {
    data class NotFound(override val message: String = "Coin not found") : CoinError
    data class SetNotFound(override val message: String = "Set not found") : CoinError
    data class Unauthorized(override val message: String = "Unauthorized") : CoinError
    data class Validation(override val message: String) : CoinError
}
