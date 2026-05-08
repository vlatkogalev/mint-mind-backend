package com.vlatkogalev.platform.core

sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val reason: String, val cause: Throwable? = null) : Result<Nothing>
}

inline fun <T> resultOf(block: () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (ex: Exception) {
        Result.Failure(ex.message ?: "Unknown error", ex)
    }
