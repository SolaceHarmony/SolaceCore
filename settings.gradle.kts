pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        kotlin("multiplatform") version "2.3.21"
        kotlin("plugin.serialization") version "2.3.21"
        kotlin("plugin.compose") version "2.3.21"
        id("org.jetbrains.compose") version "1.11.0-rc01"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "SolaceCore"
include(":composeApp")
include(":lib")
