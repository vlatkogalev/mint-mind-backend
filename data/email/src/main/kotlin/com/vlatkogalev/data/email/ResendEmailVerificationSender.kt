package com.vlatkogalev.data.email

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import com.vlatkogalev.domain.user.service.EmailVerificationSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResendEmailVerificationSender(
    apiKey: String,
    private val fromAddress: String,
    private val appBaseUrl: String,
) : EmailVerificationSender {
    private val client = Resend(apiKey)

    override suspend fun sendVerificationEmail(email: String, verificationToken: String) {
        withContext(Dispatchers.IO) {
            val verifyLink = "$appBaseUrl/auth/verify-email?token=$verificationToken"

            val params = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(email)
                .subject("[Action required] Verify your e-mail")
                .html(htmlBody(verifyLink))
                .text(textBody(verifyLink))
                .build()

            client.emails().send(params)
        }
    }

    private fun htmlBody(link: String) = """
        <div style="font-family:sans-serif;max-width:480px;margin:auto;padding:32px">
          <h2 style="color:#1a1a2e">Welcome to MintMind!</h2>
          <p>Please verify your email address to activate your account.</p>
          <a href="$link"
             style="display:inline-block;padding:12px 24px;background:#1a1a2e;color:#fff;
                    border-radius:6px;text-decoration:none;font-weight:bold;margin:16px 0">
            Verify Email
          </a>
          <p style="color:#888;font-size:12px;margin-top:24px">
            If you didn't create a MintMind account, you can safely ignore this email.
          </p>
        </div>
    """.trimIndent()

    private fun textBody(link: String) = """
        Welcome to MintMind!

        Please verify your email by visiting the link below:
        $link

        If you didn't create an account, ignore this email.
    """.trimIndent()
}
