rootProject.name = "mint-mind-backend"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.4.3")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    ":platform:core",
    ":platform:auth",
    ":platform:database",
    ":platform:logging",
    ":domain:user",
    ":domain:image",
    ":domain:coin",
    ":domain:billing",
    ":domain:news",
    ":data:postgres",
    ":data:s3",
    ":data:email",
    ":app:api",
    ":app:jobs",
)
