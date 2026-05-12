@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.ConfirmPasswordResetRequest
import com.vlatkogalev.app.api.dto.DeleteAccountRequest
import com.vlatkogalev.app.api.dto.LoginRequest
import com.vlatkogalev.app.api.dto.LoginSessionResponse
import com.vlatkogalev.app.api.dto.PasswordResetRequestResponse
import com.vlatkogalev.app.api.dto.RefreshTokenRequest
import com.vlatkogalev.app.api.dto.RegisterRequest
import com.vlatkogalev.app.api.dto.RequestPasswordResetRequest
import com.vlatkogalev.app.api.dto.UserResponse
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.domain.user.model.LoginSession
import com.vlatkogalev.domain.user.model.PasswordResetRequestResult
import com.vlatkogalev.domain.user.model.User
import com.vlatkogalev.domain.user.service.UserAuthService
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.openapi.describe
import java.util.UUID

class UserAuthController(
    private val userAuthService: UserAuthService,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerPublicRoutes() {
        post("/register") {
            val payload = call.receive<RegisterRequest>()
            when (val result = userAuthService.register(payload.email, payload.password, payload.firstName, payload.lastName)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Register a new user"
        }

        post("/login") {
            val payload = call.receive<LoginRequest>()
            when (val result = userAuthService.login(payload.email, payload.password)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.Unauthorized, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Authenticate a user and issue tokens"
        }

        post("/refresh") {
            val payload = call.receive<RefreshTokenRequest>()
            when (val result = userAuthService.refresh(payload.refreshToken)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.Unauthorized, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Refresh an access token"
        }

        get("/verify-email") {
            val token = call.request.queryParameters["token"].orEmpty()
            when (val result = userAuthService.verifyEmail(token)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Email verified")))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Verify an email address"
        }

        post("/password-reset/request") {
            val payload = call.receive<RequestPasswordResetRequest>()
            when (val result = userAuthService.requestPasswordReset(payload.email)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Request a password reset"
        }

        post("/password-reset/confirm") {
            val payload = call.receive<ConfirmPasswordResetRequest>()
            when (val result = userAuthService.confirmPasswordReset(payload.token, payload.newPassword)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Password updated")))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Confirm password reset with token"
        }
    }

    fun Route.registerProtectedRoutes() {
        get("/me") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@get
            }

            when (val result = userAuthService.getUserProfile(userId)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.NotFound, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Get the authenticated user's profile"
        }

        post("/logout") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }

            when (val result = userAuthService.logout(userId)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Logged out")))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Logout the authenticated user"
        }

        delete("/account") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@delete
            }

            val payload = call.receive<DeleteAccountRequest>()
            if (!payload.confirm) {
                call.respond(HttpStatusCode.BadRequest, error("Set confirm=true to delete account"))
                return@delete
            }

            when (val result = userAuthService.deleteAccount(userId)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Account deleted")))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Delete the authenticated user's account"
        }
    }

    private fun io.ktor.server.application.ApplicationCall.userUuidOrNull(): UUID? =
        principal<JWTPrincipal>()?.userIdOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun LoginSession.toResponse(): LoginSessionResponse =
        LoginSessionResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = accessTokenExpiresInSeconds,
            refreshTokenExpiresInSeconds = refreshTokenExpiresInSeconds,
        )

    private fun User.toResponse(): UserResponse =
        UserResponse(
            id = id.toString(),
            email = email,
            firstName = firstName,
            lastName = lastName,
            avatarUrl = avatarUrl,
            emailVerified = emailVerified,
        )

    private fun PasswordResetRequestResult.toResponse(): PasswordResetRequestResponse =
        PasswordResetRequestResponse(
            resetToken = resetToken.takeIf { it.isNotBlank() },
            message = "If the email exists, a reset token has been issued",
        )

    private fun <T> success(data: T): ApiResponse<T> =
        ApiResponse(
            success = true,
            data = data,
            timestampMillis = timeProvider.nowMillis(),
        )

    private fun error(message: String): ApiResponse<Unit> =
        ApiResponse(
            success = false,
            error = message,
            timestampMillis = timeProvider.nowMillis(),
        )
}
