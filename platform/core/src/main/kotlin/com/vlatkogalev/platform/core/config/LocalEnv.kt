package com.vlatkogalev.platform.core.config

import java.io.File

fun loadLocalEnv(filePath: String = ".env") {
    val file = File(filePath)
    if (!file.isFile) return

    file.useLines { lines ->
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

            val eqIndex = trimmed.indexOf('=')
            if (eqIndex <= 0) return@forEach

            val key = trimmed.substring(0, eqIndex).trim()
            val rawValue = trimmed.substring(eqIndex + 1).trim()
            val value = unquote(rawValue)

            if (key.isNotEmpty()) {
                System.setProperty(key, value)
            }
        }
    }
}

fun envValue(env: Map<String, String>, key: String): String? =
    env[key] ?: System.getProperty(key)

fun requiredEnv(env: Map<String, String>, key: String): String =
    envValue(env, key) ?: error("$key environment variable is required")

private fun unquote(value: String): String {
    if (value.length >= 2) {
        val first = value.first()
        val last = value.last()
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length - 1)
        }
    }
    return value
}
