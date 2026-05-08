plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(ktorLibs.server.core)
    implementation(libs.flyway.core)
    implementation(libs.hikari.cp)
    implementation(libs.h2database.h2)
    implementation(libs.postgresql)
}
