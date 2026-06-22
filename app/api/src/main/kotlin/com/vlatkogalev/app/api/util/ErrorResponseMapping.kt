package com.vlatkogalev.app.api.util

import com.vlatkogalev.domain.coin.model.CoinError
import com.vlatkogalev.platform.core.DomainError
import com.vlatkogalev.platform.core.ErrorResponse

fun DomainError.toErrorResponse(): ErrorResponse = ErrorResponse(code = errorCode(), message = message)

private fun DomainError.errorCode(): String =
    when (this) {
        is CoinError.NotFound -> "COIN_NOT_FOUND"
        is CoinError.SetNotFound -> "SET_NOT_FOUND"
        is CoinError.Unauthorized -> "UNAUTHORIZED"
        is CoinError.Validation -> "VALIDATION_ERROR"
        is DomainError.NotFound -> "NOT_FOUND"
        is DomainError.Validation -> "VALIDATION_ERROR"
        is DomainError.Unauthorized -> "UNAUTHORIZED"
        is DomainError.Conflict -> "CONFLICT"
        is DomainError.External -> "EXTERNAL_ERROR"
        is DomainError.Unknown -> "INTERNAL_ERROR"
        else -> "ERROR"
    }
