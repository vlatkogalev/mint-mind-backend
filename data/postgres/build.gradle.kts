plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":domain:user"))
    implementation(project(":domain:coin"))
    implementation(project(":domain:news"))
    implementation(project(":domain:marketplace"))
    implementation(project(":domain:billing"))
    implementation(project(":platform:database"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.r2dbc)
    implementation(libs.exposed.java.time)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.r2dbc.postgresql)

    testImplementation(testFixtures(project(":platform:database")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(kotlin("test"))
}