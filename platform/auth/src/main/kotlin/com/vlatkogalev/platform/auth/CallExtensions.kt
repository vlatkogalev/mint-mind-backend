package com.vlatkogalev.platform.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

fun ApplicationCall.userUuidOrNull(): UUID? =
    principal<JWTPrincipal>()
        ?.userIdOrNull()
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
