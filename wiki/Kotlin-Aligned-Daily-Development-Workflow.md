<!-- topic: Reference -->
<!-- title: Kotlin-Aligned Daily Development Workflow -->

## Development Workflow

### Daily Development

```bash
# Start development
./gradlew desktopRun

# In another terminal, run tests continuously
./gradlew test --continuous

# Check code quality
./gradlew detekt
./gradlew ktlintCheck
```

### Before Commit

```bash
# Run full test suite
./gradlew test

# Check code style
./gradlew ktlintCheck

# Run static analysis
./gradlew detekt

# Build all targets
./gradlew build
```

### IDE Setup

- **IntelliJ IDEA**: Use Kotlin plugin, enable annotation processing
- **VS Code**: Install Kotlin extension, configure workspace settings
- **Android Studio**: Full Kotlin support included

### Debugging

```kotlin
// Add debug logging
private val logger = KotlinLogging.logger {}

suspend fun debugActor() {
    logger.debug { "Processing message: $message" }

    try {
        // Your code here
        logger.info { "Processing completed successfully" }
    } catch (e: Exception) {
        logger.error(e) { "Processing failed" }
        throw e
    }
}
```



[Back to Kotlin-Aligned Quick Start](Kotlin-Aligned-Quick-Start)
