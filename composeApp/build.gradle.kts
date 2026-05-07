@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    // Apply the canonical KMP source-set hierarchy:
    //
    //   commonMain
    //     ├── desktopMain (jvm)
    //     └── webMain
    //           ├── jsMain
    //           └── wasmJsMain
    //
    // This is the standard template documented at
    //   https://kotlinlang.org/docs/multiplatform-hierarchy.html
    applyDefaultHierarchyTemplate()

    jvm("desktop")

    js(IR) {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                }
            }
        }
        binaries.executable()
    }

    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8082
                }
            }
        }
        binaries.executable()
    }

    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.0-0.6.x-compat")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":lib"))
            }
        }
        // webMain is provided by the default hierarchy template; both jsMain and wasmJsMain
        // automatically depend on it. We don't need to declare it here unless we add deps —
        // but the legacy `webMain/kotlin/Platform.kt` (with `actual fun PlatformText`) is now
        // correctly wired into both web targets.
    }
}

compose.desktop {
    application {
        mainClass = "org.solace.composeapp.MainKt"
    }
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}
