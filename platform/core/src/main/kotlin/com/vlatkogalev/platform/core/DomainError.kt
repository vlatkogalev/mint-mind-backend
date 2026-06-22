package com.vlatkogalev.platform.core

interface DomainError {
    val message: String

    data class Unknown(override val message: String, val cause: Throwable? = null) : DomainError
    data class NotFound(override val message: String) : DomainError
    data class Validation(override val message: String) : DomainError
    data class Unauthorized(override val message: String) : DomainError
    data class Conflict(override val message: String) : DomainError
    data class External(override val message: String, val cause: Throwable? = null) : DomainError
}
