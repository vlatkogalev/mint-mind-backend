package com.vlatkogalev.platform.core

sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val error: DomainError) : Result<Nothing> {
        constructor(message: String) : this(DomainError.Validation(message))
        constructor(message: String, cause: Throwable?) : this(DomainError.Unknown(message, cause))

        val reason: String get() = error.message
        val cause: Throwable?
            get() = when (val e = error) {
                is DomainError.Unknown -> e.cause
                is DomainError.External -> e.cause
                else -> null
            }
    }
}

inline fun <T> resultOf(block: () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (ex: Exception) {
        Result.Failure(DomainError.Unknown(ex.message ?: "Unknown error", ex))
    }
