plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":app:domain"))
    implementation(project(":app:data"))
}
