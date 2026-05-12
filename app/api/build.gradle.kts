import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "com.vlatkogalev.app.api.MainKt"
}

ktor {
    fatJar {
        archiveFileName.set("app-all.jar")
    }
}

tasks.withType<ShadowJar>().configureEach {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":platform:auth"))
    implementation(project(":platform:database"))
    implementation(project(":platform:logging"))
    implementation(project(":domain:user"))
    implementation(project(":data:postgres"))
    implementation(project(":data:s3"))

    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.csrf)
    implementation(ktorLibs.server.openapi)
    implementation(ktorLibs.server.swagger)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.routingOpenapi)
    implementation(libs.koin.ktor)
    implementation(libs.koin.loggerSlf4j)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
