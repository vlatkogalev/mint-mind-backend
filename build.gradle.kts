plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(ktorLibs.plugins.ktor) apply false
}

group = "com.vlatkogalev.mintmind"
version = "1.0.0"

subprojects {
    group = rootProject.group
    version = rootProject.version
}
