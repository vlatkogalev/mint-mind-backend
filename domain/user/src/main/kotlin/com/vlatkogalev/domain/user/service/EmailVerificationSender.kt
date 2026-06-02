package com.vlatkogalev.domain.user.service

interface EmailVerificationSender {
    suspend fun sendVerificationEmail(email: String, verificationToken: String)
}

class NoopEmailVerificationSender : EmailVerificationSender {
    override suspend fun sendVerificationEmail(email: String, verificationToken: String) = Unit
}
