plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(ktorLibs.server.core)
    implementation(libs.aws.s3)
    implementation(libs.aws.auth)
    implementation(libs.kotlinx.coroutines.core)
}
