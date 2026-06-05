plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":domain:user"))
    implementation(project(":domain:news"))
    implementation(project(":domain:marketplace"))
    implementation(project(":data:postgres"))
    implementation(project(":data:ebay"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation(libs.slf4j.api)

    testImplementation(kotlin("test"))
}
