plugins {
    kotlin("multiplatform") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    macosArm64()

    // Consistent JDK locally and in CI
    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.24.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")

                // Kotlin scripting deps (JVM-only)
                implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.2.20")
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.2.20")
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.2.20")
                implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:2.2.20")
                implementation("org.jetbrains.kotlin:kotlin-main-kts:2.2.20")
                implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:2.2.20")
                implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.2.20")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

                // Logging (JVM)
                implementation("org.slf4j:slf4j-api:2.0.13")
                implementation("org.slf4j:slf4j-simple:2.0.13")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:1.10.2")
            }
        }

        val macosArm64Test by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
