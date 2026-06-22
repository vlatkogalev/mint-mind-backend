package com.vlatkogalev.app.api.util

object HtmlTemplates {
    val emailVerified: String = loadResource("templates/email-verified.html")
    val emailVerificationFailed: String = loadResource("templates/email-verification-failed.html")

    private fun loadResource(path: String): String =
        HtmlTemplates::class.java.classLoader
            .getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: error("Template not found: $path")
}
