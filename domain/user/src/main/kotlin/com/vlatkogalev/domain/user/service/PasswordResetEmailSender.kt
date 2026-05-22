package com.vlatkogalev.domain.user.service

interface PasswordResetEmailSender {
    fun sendPasswordResetEmail(email: String, resetToken: String)
}

class NoopPasswordResetEmailSender : PasswordResetEmailSender {
    override fun sendPasswordResetEmail(email: String, resetToken: String) = Unit
}