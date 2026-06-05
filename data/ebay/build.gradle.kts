plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":domain:coin"))
    implementation(project(":domain:marketplace"))
    implementation(project(":domain:pricing"))
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.cio)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
}
