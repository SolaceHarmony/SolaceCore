plugins {
    kotlin("multiplatform") version "2.0.21"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm() // JVM target
    // Add more targets here if needed in the future, like JS or Native.

    sourceSets {
        // Shared code across platforms
        val commonMain by getting {
            kotlin.srcDirs("lib/src/commonMain/kotlin")
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:atomicfu:0.21.0")
                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }

        // JVM-specific code
        val jvmMain by getting {
            kotlin.srcDirs("lib/src/jvmMain/kotlin")
            dependencies {
                // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
            }
        }

        // JVM-specific tests
        val jvmTest by getting {
            kotlin.srcDirs("lib/src/jvmTest/kotlin")
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")

            }
        }
    }
}