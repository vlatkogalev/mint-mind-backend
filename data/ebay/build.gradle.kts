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
    implementation(project(":domain:pricing"))
    implementation(libs.slf4j.api)
    implementation(ktorLibs.serialization.kotlinx.json)
    testImplementation(kotlin("test"))
}