package com.vlatkogalev.platform.core.config

data class AwsConfig(
    val region: String,
    val bucketName: String,
    val uploadUrlTtlSeconds: Long,
    val downloadUrlTtlSeconds: Long,
    val cloudfrontDomain: String,
)

fun loadAwsConfig(env: Map<String, String> = System.getenv()): AwsConfig =
    AwsConfig(
        region = env["AWS_REGION"] ?: "us-east-1",
        bucketName = env["AWS_BUCKET"] ?: "mint-mind-coins-bucket",
        uploadUrlTtlSeconds = env["AWS_UPLOAD_URL_TTL_SECONDS"]?.toLongOrNull() ?: 900,
        downloadUrlTtlSeconds = env["AWS_DOWNLOAD_URL_TTL_SECONDS"]?.toLongOrNull() ?: 900,
        cloudfrontDomain = env["AWS_CLOUDFRONT_DOMAIN"] ?: "d1234567890.cloudfront.net",
    )
