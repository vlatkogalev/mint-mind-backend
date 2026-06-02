plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":domain:user"))
    implementation(project(":domain:billing"))
    implementation(project(":domain:coin"))
    implementation(project(":domain:news"))
    implementation(project(":domain:marketplace"))
    implementation(project(":platform:database"))

    testImplementation(testFixtures(project(":platform:database")))
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(kotlin("test"))
}
