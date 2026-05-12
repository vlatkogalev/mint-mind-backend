package com.vlatkogalev.domain.user.service

interface EmailVerificationSender {
    fun sendVerificationEmail(email: String, verificationToken: String)
}

class NoopEmailVerificationSender : EmailVerificationSender {
    override fun sendVerificationEmail(email: String, verificationToken: String) = Unit
}
