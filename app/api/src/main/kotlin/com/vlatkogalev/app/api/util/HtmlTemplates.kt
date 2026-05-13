package com.vlatkogalev.app.api.util

object HtmlTemplates {
    val emailVerified: String by lazy {
        loadResource("templates/email-verified.html")
    }

    val emailVerificationFailed: String by lazy {
        loadResource("templates/email-verification-failed.html")
    }

    private fun loadResource(path: String): String =
        HtmlTemplates::class.java.classLoader
            .getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: error("Template not found: $path")
}