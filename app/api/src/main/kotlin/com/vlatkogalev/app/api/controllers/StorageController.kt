package com.vlatkogalev.app.api.controllers

import com.vlatkogalev.app.api.dto.CreateDownloadUrlsRequest
import com.vlatkogalev.app.api.dto.CreateDownloadUrlsResponse
import com.vlatkogalev.app.api.dto.CreateUploadUrlsRequest
import com.vlatkogalev.app.api.dto.CreateUploadUrlsResponse
import com.vlatkogalev.app.api.dto.PresignedDownloadUrl
import com.vlatkogalev.app.api.dto.PresignedUploadUrl
import com.vlatkogalev.app.api.routes.ApiTags
import com.vlatkogalev.platform.auth.userIdOrNull
import com.vlatkogalev.platform.core.ApiResponse
import com.vlatkogalev.platform.core.config.AwsConfig
import com.vlatkogalev.platform.core.config.loadAwsConfig
import com.vlatkogalev.platform.core.storage.FileStorageService
import com.vlatkogalev.platform.core.time.TimeProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.openapi.describe
import java.util.UUID

class StorageController(
    private val fileStorageService: FileStorageService,
    private val timeProvider: TimeProvider,
    config: AwsConfig = loadAwsConfig(),
) {
    private val uploadUrlTtlSeconds = config.uploadUrlTtlSeconds
    private val downloadUrlTtlSeconds = config.downloadUrlTtlSeconds

    fun Route.registerProtectedRoutes() {
        post("/upload-urls") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.userIdOrNull()?.toLongOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }

            val payload = call.receive<CreateUploadUrlsRequest>()
            if (payload.fileCount !in 1..20) {
                call.respond(HttpStatusCode.BadRequest, error("fileCount must be between 1 and 20"))
                return@post
            }

            val uploadSession = fileStorageService.createUploadSession(prefix = "users/$userId")
            val uploads = (1..payload.fileCount).map { index ->
                val objectKey = "${uploadSession.objectPrefix}/${uploadSession.sessionId}/${UUID.randomUUID()}-$index"
                val uploadUrl = fileStorageService.createPresignedUpload(objectKey, uploadUrlTtlSeconds)
                PresignedUploadUrl(
                    objectKey = objectKey,
                    uploadUrl = uploadUrl.toString(),
                )
            }

            call.respond(
                success(
                    CreateUploadUrlsResponse(
                        sessionId = uploadSession.sessionId,
                        uploads = uploads,
                    ),
                ),
            )
        }.describe {
            tag(ApiTags.STORAGE)
            summary = "Create presigned S3 upload URLs"
        }

        post("/download-urls") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.userIdOrNull()?.toLongOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, error("Invalid token"))
                return@post
            }

            val payload = call.receive<CreateDownloadUrlsRequest>()
            if (payload.objectKeys.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, error("objectKeys must contain at least one key"))
                return@post
            }

            if (payload.objectKeys.size > 20) {
                call.respond(HttpStatusCode.BadRequest, error("objectKeys can contain at most 20 keys"))
                return@post
            }

            val userPrefix = "users/$userId/"
            if (payload.objectKeys.any { it.isBlank() || !it.startsWith(userPrefix) }) {
                call.respond(HttpStatusCode.Forbidden, error("Access denied for one or more requested object keys"))
                return@post
            }

            val downloads = payload.objectKeys.map { objectKey ->
                val downloadUrl = fileStorageService.createPresignedDownload(objectKey, downloadUrlTtlSeconds)
                PresignedDownloadUrl(
                    objectKey = objectKey,
                    downloadUrl = downloadUrl.toString(),
                )
            }

            call.respond(
                success(
                    CreateDownloadUrlsResponse(downloads = downloads),
                ),
            )
        }.describe {
            tag(ApiTags.STORAGE)
            summary = "Create presigned S3 download URLs"
        }
    }

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
