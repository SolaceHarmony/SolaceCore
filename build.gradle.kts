plugins {
    kotlin("multiplatform") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
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
                implementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
            }
        }

        // JVM-specific code
        val jvmMain by getting {
            kotlin.srcDirs("lib/src/jvmMain/kotlin")
            dependencies {
                // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

                // Kotlin scripting dependencies
                implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.0.21")
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.0.21")
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.0.21")
                implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:2.0.21")
                implementation("org.jetbrains.kotlin:kotlin-main-kts:2.0.21")
                implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:2.0.21")
                implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.0.21")

                // Serialization for script metadata and state
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                // Logging
                implementation("org.slf4j:slf4j-api:2.0.9")
                implementation("org.slf4j:slf4j-simple:2.0.9")
            }
        }

        // JVM-specific tests
        val jvmTest by getting {
            kotlin.srcDirs("lib/src/jvmTest/kotlin")
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:1.9.0")

            }
        }
    }
}
