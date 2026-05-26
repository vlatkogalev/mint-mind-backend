plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":domain:coin"))
    implementation(project(":domain:pricing"))
    implementation(libs.slf4j.api)
    testImplementation(kotlin("test"))
}