[← Architecture Overview](../../wiki/Architecture-Overview.md) · §9 of 15

---

## 9. JVM-Specific Utilities (`io.github.solaceharmony.core.util`)
The `io.github.solaceharmony.core.util` package within `jvmMain` houses utility classes and functions specific to the JVM environment.

### 9.1. Logging (`LoggerProvider.kt`)
SolaceCore utilizes SLF4J (Simple Logging Facade for Java) for its logging needs on the JVM. The `LoggerProvider` object offers a centralized way to obtain logger instances.

*   **`LoggerProvider` Object:**
    *   **Purpose:** To provide a consistent factory for creating `org.slf4j.Logger` instances.
    *   **Mechanism:** It wraps `org.slf4j.LoggerFactory`.
    *   **Methods:**
        *   `fun getLogger(clazz: Class<*>): Logger`: Creates or retrieves a logger named after the fully qualified name of the given `clazz`. This is the standard way to get a logger for a specific class.
        *   `fun getLogger(name: String): Logger`: Creates or retrieves a logger with a custom `name`.
*   **`Any.logger` Extension Property:**
    *   **Definition:** `val Any.logger: Logger get() = LoggerProvider.getLogger(this.javaClass)`
    *   **Purpose:** Provides a highly convenient way for any class instance to obtain its dedicated logger. Inside any class, `logger.info(...)` can be used directly.

**Integration with Build:**
The `build.gradle.kts` file includes dependencies for `org.slf4j:slf4j-api` and `org.slf4j:slf4j-simple`. The `slf4j-simple` binding logs all messages of INFO level and above to `System.err`. This is a basic binding suitable for development or simple applications; for production, a more configurable logging backend (like Logback or Log4j2) would typically be used with SLF4J.

**Usage Example:**
```kotlin
// Inside some class in jvmMain
class MyService {
    // private val log = LoggerProvider.getLogger(MyService::class.java) // Traditional way
    private val log = logger // Using the extension property

    fun doSomething() {
        log.info("Doing something...")
        try {
            // ...
        } catch (e: Exception) {
            log.error("Error doing something", e)
        }
    }
}
```
This utility ensures a standardized approach to logging across the JVM-specific codebase.

---

← [§8 Development Tooling and Practices](./08-development-tooling-and-practices.md)  ·  [Architecture Overview](../../wiki/Architecture-Overview.md)  ·  [§10 Testing Strategy (JVM Target)](./10-testing-strategy-jvm-target.md) →
