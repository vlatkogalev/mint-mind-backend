plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":domain:user"))
    implementation(libs.resend)
    implementation(libs.kotlinx.coroutines.core)
}
