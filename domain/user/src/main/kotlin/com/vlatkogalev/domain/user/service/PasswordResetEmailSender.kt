package com.vlatkogalev.domain.user.service

interface PasswordResetEmailSender {
    suspend fun sendPasswordResetEmail(email: String, resetToken: String)
}

class NoopPasswordResetEmailSender : PasswordResetEmailSender {
    override suspend fun sendPasswordResetEmail(email: String, resetToken: String) = Unit
}