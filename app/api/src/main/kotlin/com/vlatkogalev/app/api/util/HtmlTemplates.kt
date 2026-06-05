package com.vlatkogalev.app.api.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object HtmlTemplates {
    val emailVerified: String by lazy {
        runBlocking(Dispatchers.IO) { loadResource("templates/email-verified.html") }
    }

    val emailVerificationFailed: String by lazy {
        runBlocking(Dispatchers.IO) { loadResource("templates/email-verification-failed.html") }
    }

    private fun loadResource(path: String): String =
        HtmlTemplates::class.java.classLoader
            .getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: error("Template not found: $path")
}