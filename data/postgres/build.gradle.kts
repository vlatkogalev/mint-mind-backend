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
    implementation(project(":platform:database"))
}
