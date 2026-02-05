// Root project acts as an aggregator only. See lib/build.gradle.kts for KMP config.
// Plugins declared here with apply false to avoid multiple plugin loading warnings

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") apply false
    kotlin("plugin.compose") apply false
    id("org.jetbrains.compose") apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
