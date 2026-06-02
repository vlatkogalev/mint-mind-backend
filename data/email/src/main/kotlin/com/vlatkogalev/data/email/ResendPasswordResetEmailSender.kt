package com.vlatkogalev.data.email

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import com.vlatkogalev.domain.user.service.PasswordResetEmailSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResendPasswordResetEmailSender(
    apiKey: String,
    private val fromAddress: String,
    private val appBaseUrl: String,
) : PasswordResetEmailSender {
    private val client = Resend(apiKey)

    override suspend fun sendPasswordResetEmail(email: String, resetToken: String) {
        withContext(Dispatchers.IO) {
            val resetLink = "$appBaseUrl/auth/reset-password?token=$resetToken"

            val params = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(email)
                .subject("[Action required] Reset your password")
                .html(htmlBody(resetLink))
                .text(textBody(resetLink))
                .build()

            client.emails().send(params)
        }
    }

    private fun htmlBody(link: String) = """
        <div style="font-family:sans-serif;max-width:480px;margin:auto;padding:32px">
          <h2 style="color:#1a1a2e">Reset your password</h2>
          <p>We received a request to reset your password. Click below to choose a new one.</p>
          <a href="$link"
             style="display:inline-block;padding:12px 24px;background:#1a1a2e;color:#fff;
                    border-radius:6px;text-decoration:none;font-weight:bold;margin:16px 0">
            Reset Password
          </a>
          <p style="color:#888;font-size:12px;margin-top:24px">
            This link expires in 15 minutes. If you didn't request a password reset, you can safely ignore this email.
          </p>
        </div>
    """.trimIndent()

    private fun textBody(link: String) = """
        Reset your password by visiting the link below:
        $link

        This link expires in 15 minutes. If you didn't request a password reset, ignore this email.
    """.trimIndent()
}