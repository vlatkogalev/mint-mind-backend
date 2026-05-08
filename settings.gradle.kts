rootProject.name = "ktor-backend-template"

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
    ":platform:storage",
    ":platform:billing",
    ":platform:database",
    ":platform:logging",
    ":app:api",
    ":app:domain",
    ":app:data",
    ":app:jobs",
)
