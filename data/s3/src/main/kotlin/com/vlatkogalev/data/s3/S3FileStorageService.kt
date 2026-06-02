package com.vlatkogalev.data.s3

import com.vlatkogalev.platform.core.config.AwsConfig
import com.vlatkogalev.platform.core.config.loadAwsConfig
import com.vlatkogalev.platform.core.storage.FileStorageService
import com.vlatkogalev.platform.core.storage.UploadSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL
import java.time.Duration
import java.util.UUID

class S3FileStorageService(
    config: AwsConfig = loadAwsConfig(),
) : FileStorageService {

    private val presigner = S3Presigner.builder()
        .region(Region.of(config.region))
        .build()

    private val bucket = config.bucketName

    override suspend fun createPresignedUpload(key: String, ttlSeconds: Long): URL =
        withContext(Dispatchers.IO) {
            presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(ttlSeconds))
                    .putObjectRequest(
                        PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    )
                    .build(),
            ).url()
        }

    override suspend fun createPresignedDownload(key: String, ttlSeconds: Long): URL =
        withContext(Dispatchers.IO) {
            presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(ttlSeconds))
                    .getObjectRequest(
                        GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    )
                    .build(),
            ).url()
        }

    override suspend fun createUploadSession(prefix: String): UploadSession =
        UploadSession(
            sessionId = UUID.randomUUID().toString(),
            objectPrefix = prefix,
        )
}
