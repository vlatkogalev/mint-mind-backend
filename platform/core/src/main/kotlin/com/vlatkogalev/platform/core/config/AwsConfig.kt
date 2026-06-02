package com.vlatkogalev.platform.core.config

data class AwsConfig(
    val region: String,
    val bucketName: String,
    val uploadUrlTtlSeconds: Long,
    val downloadUrlTtlSeconds: Long,
)

fun loadAwsConfig(env: Map<String, String> = System.getenv()): AwsConfig =
    AwsConfig(
        region = envValue(env, "AWS_REGION") ?: "us-east-1",
        bucketName = envValue(env, "AWS_BUCKET") ?: "mint-mind-coins-bucket",
        uploadUrlTtlSeconds = envValue(env, "AWS_UPLOAD_URL_TTL_SECONDS")?.toLongOrNull() ?: 900,
        downloadUrlTtlSeconds = envValue(env, "AWS_DOWNLOAD_URL_TTL_SECONDS")?.toLongOrNull() ?: 900,
    )
