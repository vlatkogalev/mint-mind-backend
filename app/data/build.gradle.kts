plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":platform:auth"))
    implementation(project(":platform:database"))
    implementation(project(":platform:storage"))
    implementation(project(":app:domain"))
    implementation(libs.koin.ktor)
}
