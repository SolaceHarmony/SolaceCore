<!-- topic: Reference -->
<!-- title: Kotlin-Aligned Core Architectural Principles -->

## Core Architectural Principles

### 1. Actor-Based Architecture ⭐

**Status:** Production Implementation
**Location:** `lib/src/commonMain/kotlin/com/solacecore/actor/` (15 files, 85KB)

SolaceCore implements a sophisticated actor-based architecture where every component is an actor with:

**Actor Supervisor**: Manages actor lifecycle, communication, and fault tolerance
**Actor Inference Engine**: Handles AI model interactions and reasoning
**Actor Lifecycle**: Manages actor creation, supervision, and termination
**Message Passing**: All communication happens via typed messages with Flow-based streams

### 2. Dual-Model Cognition ⭐

**Status:** Production Implementation
**Location:** `lib/src/commonMain/kotlin/com/solacecore/mood/` (18 files, 144KB)

SolaceCore implements genuine dual-model cognition where every agent has two interconnected processing streams:

**Technical Brain** (e.g., Qwen-3, Claude)

- Handles reasoning, coding, tool use, problem-solving
- Receives emotional context as light-touch prompt nudges
- Maintains focus on technical tasks
- Makes final execution decisions

**Emotional Core** (Gemma3-CSM)

- Provides emotional awareness through spiking neural network
- Generates authentic emotional responses
- Modulates attention and memory through valence
- Influences technical decision-making through emotional context

### 3. Kotlin-First Design

**Status:** Production Implementation
**Location:** `lib/src/commonMain/kotlin/com/solacecore/`

All components are built with Kotlin best practices:

**Coroutines & Flow**: Async operations and reactive streams
**Sealed Classes**: Type-safe message passing and state management
**DSL Builders**: Configuration and pipeline construction
**Inline Functions**: Performance-critical operations
**Operator Overloading**: Domain-specific operators
**Null Safety**: Comprehensive null checking and smart casts

### 4. Multiplatform Support

**Status:** Production Implementation
**Targets:** JVM, JS, Native, Android, iOS

```kotlin
// Common code that runs everywhere
interface PlatformProvider {
    fun getPlatformName(): String
    fun createHttpClient(): HttpClient
}

// Platform-specific implementations
class JvmPlatformProvider : PlatformProvider {
    override fun getPlatformName() = "JVM"
    override fun createHttpClient() = JvmHttpClient()
}

class JsPlatformProvider : PlatformProvider {
    override fun getPlatformName() = "JavaScript"
    override fun createHttpClient() = JsHttpClient()
}
```

### 5. Component Architecture

**Status:** Production Implementation
**Location:** `docs/components/`

Modular component system with clear interfaces:

```kotlin
interface SolaceComponent {
    val name: String
    val version: String
    val dependencies: Set<String>

    suspend fun initialize(): Result<Unit>
    suspend fun shutdown(): Result<Unit>
    fun healthCheck(): HealthStatus
}

data class HealthStatus(
    val status: ComponentStatus,
    val message: String? = null,
    val metrics: Map<String, Any> = emptyMap()
)

enum class ComponentStatus {
    HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN
}
```

---


[Back to Kotlin-Aligned Architecture Overview](Kotlin-Aligned-Architecture-Overview)
