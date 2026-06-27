<!-- topic: Reference -->
<!-- title: Build System and Dependencies -->

[← Architecture Overview](Architecture-Overview) · §7 of 15

---

## 7. Build System and Dependencies
SolaceCore utilizes the Gradle build system to manage its compilation, dependencies, and packaging. The project is structured as a Kotlin Multiplatform (KMP) project, currently targeting the JVM.

### 7.1. Gradle Configuration
*   **Gradle Version:** The project is configured to use Gradle version **8.7**, as specified in `gradle/wrapper/gradle-wrapper.properties`.
*   **Build Scripts:** Kotlin DSL (`.kts`) is used for Gradle build scripts.
    *   `settings.gradle.kts`: Defines the project structure.
    *   `build.gradle.kts` (root): Configures the build for the entire project, including plugins, repositories, and KMP targets.
*   **Project Structure (`settings.gradle.kts`):**
    *   Root project name: `SolaceCore`.
    *   A single included subproject (module): `:lib`. All core source code resides within this `lib` module.
*   **JDK Provisioning:** The `org.gradle.toolchains.foojay-resolver-convention` plugin (version `0.8.0`) is applied to automate JDK discovery and provisioning, ensuring build consistency.

### 7.2. Kotlin Multiplatform (KMP) Setup (`build.gradle.kts`)
*   **Kotlin Version:** The project uses Kotlin version **2.0.21** for its multiplatform capabilities, serialization, and scripting features.
*   **Plugins:**
    *   `kotlin("multiplatform") version "2.0.21"`: The primary plugin enabling KMP.
    *   `kotlin("plugin.serialization") version "2.0.21"`: Enables Kotlin's type-safe serialization.
*   **Targets:**
    *   **JVM:** Currently, the only explicitly defined target is `jvm()`. The setup allows for future expansion to other platforms (e.g., JavaScript, Native).
*   **Source Sets:**
    *   `commonMain` (`lib/src/commonMain/kotlin`): Contains platform-agnostic code shared across all potential targets.
    *   `jvmMain` (`lib/src/jvmMain/kotlin`): Contains JVM-specific implementations and dependencies.
    *   `jvmTest` (`lib/src/jvmTest/kotlin`): Contains tests for the JVM target.

### 7.3. Key Dependencies
Dependencies are managed via a Gradle Version Catalog (`gradle/libs.versions.toml`) and applied in `build.gradle.kts`.

#### 7.3.1. `commonMain` Dependencies:
*   **Kotlin Standard Library:** `kotlin("stdlib")` - Essential for all Kotlin code.
*   **AtomicFU:** `org.jetbrains.kotlinx:atomicfu:0.21.0` - For creating atomic value holders for concurrent programming.
*   **Kotlin Coroutines Core:** `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0` (runtimeOnly) - Provides fundamental coroutine support.
*   **Kotlin Test:** `org.jetbrains.kotlin:kotlin-test:2.0.21` - Core library for writing tests in common code.

#### 7.3.2. `jvmMain` Dependencies:
*   **Kotlin Coroutines (JVM):**
    *   `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0`
    *   `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0` (for testing coroutine-based JVM code)
*   **Kotlin Scripting (all version 2.0.21):** These enable the dynamic scripting capabilities documented in Module 6.
    *   `org.jetbrains.kotlin:kotlin-scripting-common`
    *   `org.jetbrains.kotlin:kotlin-scripting-jvm`
    *   `org.jetbrains.kotlin:kotlin-scripting-jvm-host`
    *   `org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable`
*   **Kotlinx Serialization (JSON):** `org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3` - Used for JSON serialization/deserialization, likely for configuration, actor state, or message payloads on the JVM.
*   **Logging (SLF4J):**
    *   `org.slf4j:slf4j-api:2.0.9` (Simple Logging Facade for Java API)
    *   `org.slf4j:slf4j-simple:2.0.9` (A basic SLF4J implementation that logs to standard output, suitable for development and testing).
*   **Apache Commons Math:** `org.apache.commons:commons-math3:3.6.1` (from version catalog) - Provides mathematical and statistical utilities.
*   **Google Guava:** `com.google.guava:guava:32.1.3-jre` (from version catalog) - Offers core utility libraries, collections, caching, concurrency utilities, I/O, etc.

#### 7.3.3. `jvmTest` Dependencies:
*   **Kotlin Coroutines (JVM):**
    *   `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0`
    *   `org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:1.9.0`
*   **JUnit Jupiter Engine:** `org.junit.jupiter:junit-jupiter-engine:5.10.1` (from version catalog) - The primary testing framework for JVM tests.

This build setup provides a modern, robust foundation for developing SolaceCore, with clear dependency management and a structure conducive to future multiplatform expansion.

---

← [§6 Scripting Module (`io.github.solaceharmony.core.scripting`)](Scripting-Engine)  ·  [Architecture Overview](Architecture-Overview)  ·  [§8 Development Tooling and Practices](Development-Tooling-and-Practices) →
