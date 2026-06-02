plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(ktorLibs.server.core)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.hikari.cp)
    implementation(libs.postgresql)

    testFixturesImplementation(project(":platform:core"))
    testFixturesImplementation(libs.testcontainers.core)
    testFixturesImplementation(libs.testcontainers.postgresql)
    testFixturesImplementation(kotlin("test"))
}
