package com.vlatkogalev.domain.user.model

enum class PasswordResetConfirmationResult {
    SUCCESS,
    INVALID_TOKEN,
    EXPIRED_TOKEN,
}