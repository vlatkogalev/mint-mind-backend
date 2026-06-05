@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.*
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.app.api.service.SessionMergeService
import com.vlatkogalev.app.api.util.HtmlTemplates
import com.vlatkogalev.domain.user.model.AuthSession
import com.vlatkogalev.domain.user.model.LoginSession
import com.vlatkogalev.domain.user.model.User
import com.vlatkogalev.domain.user.service.UserAuthService
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import java.util.*

class UserAuthController(
    private val userAuthService: UserAuthService,
    private val sessionMergeService: SessionMergeService,
    private val timeProvider: TimeProvider,
) {
    fun Route.registerPublicRoutes() {
        post("/anonymous") {
            val payload = call.receive<AnonymousAuthRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@post
            }
            when (val result = userAuthService.authenticateAnonymous(payload.installationId)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Authenticate or create an anonymous user session"
        }

        post("/register") {
            val payload = call.receive<RegisterRequest>()
            when (val result =
                userAuthService.register(payload.email, payload.password, payload.firstName, payload.lastName)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Register a new user"
        }

        post("/login") {
            val payload = call.receive<LoginRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@post
            }
            when (val result = userAuthService.login(payload.email, payload.password)) {
                is Result.Success -> {
                    val installationId = payload.installationId?.trim().orEmpty()
                    if (installationId.isNotEmpty()) {
                        sessionMergeService.mergeIfNeeded(installationId, payload.email)
                    }
                    call.respond(success(result.value.toResponse()))
                }
                is Result.Failure -> call.respond(HttpStatusCode.Unauthorized, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Authenticate a user and issue tokens"
        }

        post("/refresh") {
            val payload = call.receive<RefreshTokenRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@post
            }
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
            when (userAuthService.verifyEmail(token)) {
                is Result.Success -> call.respondText(HtmlTemplates.emailVerified, ContentType.Text.Html)
                is Result.Failure -> call.respondText(
                    HtmlTemplates.emailVerificationFailed,
                    ContentType.Text.Html,
                    HttpStatusCode.BadRequest
                )
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Verify email address"
        }

        post("/resend-verification") {
            val payload = call.receive<ResendVerificationRequest>()
            when (val result = userAuthService.resendVerification(payload.email)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Verification email has been sent")))
                is Result.Failure -> call.respond(HttpStatusCode.TooManyRequests, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Resend email verification link"
        }

        post("/password-reset/request") {
            val payload = call.receive<RequestPasswordResetRequest>()
            when (val result = userAuthService.requestPasswordReset(payload.email)) {
                is Result.Success -> call.respond(
                    success(PasswordResetRequestResponse(message = "If the email exists, a reset link has been sent")),
                )
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Request a password reset"
        }

        post("/password-reset/confirm") {
            val payload = call.receive<ConfirmPasswordResetRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@post
            }
            when (val result = userAuthService.confirmPasswordReset(payload.token, payload.newPassword)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Password updated")))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Confirm password reset with token"
        }
    }

    fun Route.registerOptionalAuthRoutes() {
        post("/upgrade-account") {
            val currentUserId = call.userUuidOrNull()
            if (currentUserId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Anonymous authentication is required"))
                return@post
            }
            val payload = call.receive<SignupRequest>()
            payload.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, error(it))
                return@post
            }
            when (val result = userAuthService.signup(payload.email, payload.password, currentUserId)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Create an account or upgrade an anonymous account"
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

        patch("/me") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@patch
            }
            val payload = call.receive<UpdateProfileRequest>()
            when (val result = userAuthService.updateProfile(userId, payload.firstName, payload.lastName)) {
                is Result.Success -> call.respond(success(result.value.toResponse()))
                is Result.Failure -> call.respond(HttpStatusCode.BadRequest, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Update the authenticated user's profile"
        }

        delete("/me") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
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

        post("/logout") {
            val userId = call.userUuidOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }
            when (val result = userAuthService.logout(userId)) {
                is Result.Success -> call.respond(success(mapOf("message" to "Logged out")))
                is Result.Failure -> call.respond(HttpStatusCode.InternalServerError, error(result.reason))
            }
        }.describe {
            tag(ApiTags.AUTH)
            summary = "Logout and invalidate the current refresh token"
        }
    }

    private fun io.ktor.server.application.ApplicationCall.userUuidOrNull(): UUID? =
        principal<JWTPrincipal>()?.userIdOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun AuthSession.toResponse(): AuthSessionResponse =
        AuthSessionResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = accessTokenExpiresInSeconds,
            refreshTokenExpiresInSeconds = refreshTokenExpiresInSeconds,
            user = user.toResponse(),
        )

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
            isAnonymous = isAnonymous,
            upgradedAt = upgradedAt?.toString(),
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
