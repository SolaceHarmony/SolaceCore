[← Back to index](./README.md) · §6 of 15

---

## 6. Scripting Module (`io.github.solaceharmony.core.scripting`)
The `scripting` module in SolaceCore provides a robust framework for integrating and executing dynamic Kotlin scripts (`.kts` files). This allows for flexible and updatable logic within the system, particularly for actor behaviors. The design emphasizes Kotlin script integration, hot-reloading, validation, versioning, and persistent storage.

### 6.1. Overview and Design Goals
The scripting engine is designed with the following key requirements and features in mind:

*   **Kotlin Script Integration:**
    *   Leverage Kotlin's native scripting capabilities.
    *   Support compilation and execution of `.kts` script files.
    *   Provide a sandboxed environment for safe script execution (details of sandboxing TBD from further code analysis).
*   **Hot-Reloading:**
    *   Enable dynamic updates to actor logic by reloading scripts without system downtime.
    *   Support automatic detection of script changes and subsequent reloading.
    *   Ensure actor state is preserved across script reloads.
*   **Script Validation:**
    *   Perform syntax and semantic validation of scripts before execution.
    *   Verify that scripts adhere to any required interfaces or contracts.
    *   Offer clear error reporting for invalid scripts.
*   **Versioning and Rollback:**
    *   Track different versions of scripts.
    *   Allow rollback to previously known good versions if issues arise with new script versions.
*   **Script Storage:**
    *   Persistently store scripts and their metadata (version, creation date, etc.).
    *   Support loading scripts from various sources (e.g., file system, database - specific backends TBD).

### 6.2. Core Scripting Interfaces
The foundational interfaces for the scripting engine are defined as follows:

#### 6.2.1. `CompiledScript` Interface
Represents a script that has been successfully compiled by the `ScriptEngine` and is ready for execution.

*   **Purpose:** To serve as a handle or representation of a validated and prepared script.
*   **Key Properties:**
    *   `val name: String`: The unique name assigned to the script.
    *   `val compilationTimestamp: Long`: The epoch milliseconds timestamp indicating when the script was compiled.

#### 6.2.2. `ScriptEngine` Interface
Defines the contract for the core component responsible for compiling and executing Kotlin scripts.

*   **Purpose:** To manage the lifecycle of scripts, including their compilation and execution.
*   **Key Methods:**
    *   `suspend fun compile(scriptSource: String, scriptName: String): CompiledScript`: Takes Kotlin script source code as a string and a name, then compiles it, returning a `CompiledScript` instance.
    *   `suspend fun execute(compiledScript: CompiledScript, parameters: Map<String, Any?>): Any?`: Executes a previously compiled `CompiledScript`, passing in a map of parameters. It returns an optional `Any?` result from the script execution.
    *   `suspend fun eval(scriptSource: String, scriptName: String, parameters: Map<String, Any?> = emptyMap()): Any?`: A convenience method that combines compilation and execution in a single step.

```mermaid
classDiagram
    direction LR
    package "io.github.solaceharmony.core.scripting" {
        interface CompiledScript {
            <<Interface>>
            +name: String
            +compilationTimestamp: Long
        }

        interface ScriptEngine {
            <<Interface>>
            +compile(scriptSource: String, scriptName: String): CompiledScript
            +execute(compiledScript: CompiledScript, parameters: Map): Any?
            +eval(scriptSource: String, scriptName: String, parameters: Map): Any?
        }
        ScriptEngine ..> CompiledScript : creates & uses
    }
```
These core interfaces establish the foundation for dynamic script execution. The subsequent components (`ScriptActor`, `ScriptValidator`, `ScriptVersionManager`, `ScriptStorage`) will build upon these to provide a complete scripting solution.
### 6.3. Supporting Scripting Components
To provide a full-featured scripting environment, several other components work in conjunction with the `ScriptEngine` and `CompiledScript`.

#### 6.3.1. `ScriptValidator` Interface
Ensures the integrity and correctness of scripts before they are compiled or executed.

*   **Purpose:** To validate script source code for syntax, semantic errors, and adherence to any predefined contracts or required interfaces.
*   **Key Method:**
    *   `suspend fun validate(scriptSource: String): ValidationResult`: Analyzes the script source and returns a `ValidationResult`.
*   **Supporting Data Classes:**
    *   `data class ValidationResult(val isValid: Boolean, val errors: List<ValidationError> = emptyList())`: Indicates if the script is valid and provides a list of errors if not.
    *   `data class ValidationError(val message: String, val line: Int, val column: Int)`: Details a specific validation error, including its location.

#### 6.3.2. `ScriptVersionManager` Interface
Manages different versions of scripts, allowing for history tracking and rollbacks.

*   **Purpose:** To enable robust script lifecycle management by maintaining multiple versions and providing a mechanism to revert to previous states.
*   **Key Methods:**
    *   `suspend fun addVersion(scriptName: String, scriptSource: String): Int`: Stores a new version of the script source and returns its assigned version number.
    *   `suspend fun getVersion(scriptName: String, version: Int): String?`: Retrieves the source code for a specific version of a named script.
    *   `suspend fun getLatestVersion(scriptName: String): Pair<Int, String>?`: Fetches the most recent version number and source code for a script.
    *   `suspend fun rollback(scriptName: String, version: Int): Boolean`: Intended to facilitate reverting a script to a specified older version (the exact mechanics of how this impacts running `ScriptActor`s would depend on its interaction with `ScriptEngine` and `ScriptActor.reloadScript()`).

#### 6.3.3. `ScriptStorage` Interface
Handles the persistent storage and retrieval of script source code and associated metadata.

*   **Purpose:** To provide a durable store for scripts, allowing them to be loaded across application sessions or shared in a distributed environment.
*   **Key Methods:**
    *   `suspend fun saveScript(scriptName: String, scriptSource: String, metadata: Map<String, Any> = emptyMap())`: Persists the script source and any additional metadata.
    *   `suspend fun loadScript(scriptName: String): Pair<String, Map<String, Any>>?`: Loads a script's source code and its metadata from storage.
    *   `suspend fun listScripts(): List<String>`: Returns a list of names of all scripts available in the storage.
    *   `suspend fun deleteScript(scriptName: String): Boolean`: Removes a script from persistent storage.

#### 6.3.4. `ScriptActor` Class
A specialized `Actor` implementation whose behavior is defined by a dynamically loaded and compiled Kotlin script.

*   **Purpose:** To enable actors with logic that can be updated at runtime (hot-reloading) without restarting the actor or the system.
*   **Inheritance:** Extends `io.github.solaceharmony.core.actor.Actor`.
*   **Key Features:**
    *   **Constructor:** Takes a `ScriptEngine`, initial `scriptSource` string, and `scriptName`.
    *   **Script Execution:**
        *   During initialization (via a custom `initialize` method or its `start` override), it compiles its `scriptSource` into a `CompiledScript` using the provided `ScriptEngine`.
        *   When its input port receives a message, the `processMessage` handler passes the message and a reference to the `ScriptActor` instance itself (`this`) as parameters to `scriptEngine.execute(compiledScript, parameters)`. The executed script is expected to use these parameters to perform its logic (e.g., access `actor.getPort(...).send(...)` to send results).
    *   **Hot-Reloading (`reloadScript(newScriptSource: String)`):**
        *   Allows updating the actor's behavior by providing new script source code.
        *   The method updates the internal `scriptSource` and recompiles it using the `ScriptEngine`, replacing the existing `compiledScript`. This change takes effect for subsequent message processing.
    *   **Port Setup:** The `initialize` method (which can be called with input/output port names and types) uses the base `Actor.createPort()` to set up communication channels. The input port's handler is wired to the `ScriptActor`'s internal `processMessage` method.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.actor" {
        abstract class Actor { <<Abstract>> }
    }

    package "io.github.solaceharmony.core.scripting" {
        interface ScriptEngine { <<Interface>> }
        interface CompiledScript { <<Interface>> }
        interface ScriptValidator { <<Interface>> }
        class ValidationResult { +isValid: Boolean }
        interface ScriptVersionManager { <<Interface>> }
        interface ScriptStorage { <<Interface>> }

        ScriptEngine ..> CompiledScript : creates

        class ScriptActor {
            -scriptEngine: ScriptEngine
            -scriptSource: String
            -scriptName: String
            -compiledScript: CompiledScript?
            +initialize(inputPortName, inputClass, outputPortName, outputClass)
            +reloadScript(newScriptSource: String)
            #processMessage(message: Any)
        }
        Actor <|-- ScriptActor
        ScriptActor o-- ScriptEngine : uses
        ScriptActor o-- CompiledScript : holds
        ScriptValidator ..> ValidationResult : produces

    }
    note for ScriptActor "Script logic is executed via ScriptEngine,\nscript can access 'actor' instance and 'message'."
```
The scripting module, with these components, offers a powerful way to introduce dynamic and manageable custom logic into the SolaceCore system, especially for defining actor behaviors.
### 6.4. JVM-Specific Scripting Implementations
The `io.github.solaceharmony.core.scripting` package within the `jvmMain` source set provides concrete implementations for the scripting interfaces, tailored for the Java Virtual Machine environment.

#### 6.4.1. `JvmScriptEngine` Class
This class implements the `ScriptEngine` interface for the JVM, providing capabilities to compile and execute Kotlin-based scripts (`.kts`). It leverages Kotlin's official `kotlin.script.experimental.*` APIs for robust script handling.

*   **Core Scripting Infrastructure:**
    *   **Scripting Host:** Utilizes `kotlin.script.experimental.jvmhost.BasicJvmScriptingHost` as the central component for orchestrating script compilation and evaluation.
    *   **Compilation Configuration:** A `compilationConfiguration` is defined using `createJvmCompilationConfigurationFromTemplate<SimpleScript>`. Key aspects of this configuration include:
        *   **JVM Integration:** Configures the JVM environment for scripting, notably attempting to update the classpath using `JvmScriptEngine::class.java.classLoader.getResources("").toList()` to allow scripts to access project classes and dependencies.
        *   **Implicit Receivers:** Sets `Any::class` as an implicit receiver, allowing scripts to call methods on a general context object if provided.
        *   **Compiler Options:** Appends specific compiler options, such as setting the `-jvm-target` to "17".
    *   **Base Script Definition:** Scripts are expected to implicitly or explicitly extend a base class, in this case, `SimpleScript` (an abstract class defined within `JvmScriptEngine.kt`).

*   **Compilation Process (`compile` method):**
    *   **Caching:** Implements a `scriptCache` (a `mutableMapOf<String, KotlinCompiledScript>`) to store and retrieve already compiled scripts by name, avoiding redundant recompilation.
    *   **Asynchronous Execution:** Compilation is performed asynchronously using `withContext(Dispatchers.IO)`.
    *   **Compilation Invocation:** Calls `scriptingHost.compiler.invoke()` with the script source (converted via `toScriptSource()`) and the predefined `compilationConfiguration`.
    *   **Result Handling:**
        *   On `ResultWithDiagnostics.Success`, it wraps the resulting `kotlin.script.experimental.api.CompiledScript` in an internal `KotlinCompiledScript` data class (which also stores the script name and compilation timestamp), caches it, and returns it.
        *   On `ResultWithDiagnostics.Failure`, it extracts error messages from the diagnostics and throws a `ScriptCompilationException`.
    *   General exceptions during compilation are also caught and wrapped in `ScriptCompilationException`.

*   **Execution Process (`execute` and `eval` methods):**
    *   **`execute(compiledScript, parameters)`:**
        *   Expects an instance of the internal `KotlinCompiledScript`.
        *   **Asynchronous Execution:** Performed using `withContext(Dispatchers.IO)`.
        *   **Evaluation Configuration:** Creates a `ScriptEvaluationConfiguration` where:
            *   Input `parameters` are made available to the script via `providedProperties`.
            *   The JVM classpath is configured similarly to the compilation phase.
        *   **Evaluation Invocation:** Calls `scriptingHost.evaluator.invoke()` with the `kotlinCompiledScript` from the `KotlinCompiledScript` wrapper and the evaluation configuration.
        *   **Result Handling:**
            *   On `ResultWithDiagnostics.Success`, it returns the `scriptInstance` from the `returnValue`.
            *   On `ResultWithDiagnostics.Failure`, it throws a `ScriptExecutionException` with extracted error messages.
    *   **`eval(scriptSource, scriptName, parameters)`:**
        *   Provides a convenience method to compile and execute in one step.
        *   Internally, it uses `scriptingHost.eval()` which handles both compilation (using the shared `compilationConfiguration`) and evaluation (with a dynamically created `evaluationConfiguration` for parameters).
        *   Error handling distinguishes between compilation and execution phases to throw `ScriptCompilationException` or `ScriptExecutionException` accordingly.

*   **Internal `KotlinCompiledScript` Class:**
    *   A private data class implementing the public `io.github.solaceharmony.core.scripting.CompiledScript` interface.
    *   It holds the `name`, `compilationTimestamp`, and the actual `kotlin.script.experimental.api.CompiledScript` object obtained from the Kotlin scripting host.

*   **Custom Exceptions:**
    *   `ScriptCompilationException(message: String)`: Thrown when script compilation fails.
    *   `ScriptExecutionException(message: String)`: Thrown when script execution fails.

This implementation represents a significant advancement from a simulated engine, providing a functional foundation for dynamic Kotlin scripting within SolaceCore, complete with compilation, execution, parameter passing, and basic caching.

#### 6.4.2. `FileScriptStorage` Class
Implements the `ScriptStorage` interface using the local file system.

*   **Purpose:** To provide persistent storage for script source code and their metadata on the JVM.
*   **Constructor:** `FileScriptStorage(private val baseDirectory: String)`
*   **Storage Mechanism:**
    *   Scripts are stored as `.kts` files within a `scripts` subdirectory of the `baseDirectory`.
    *   Associated metadata for each script is stored in a corresponding `.json` file (e.g., `scriptName.kts` and `scriptName.json`).
    *   Uses `kotlinx.serialization.json.Json` for serializing/deserializing metadata maps.
*   **Operations:** Implements `saveScript`, `loadScript`, `listScripts`, and `deleteScript` by performing standard file I/O operations (create, read, write, list, delete) within the designated directory structure. All operations use `Dispatchers.IO`.

#### 6.4.3. `FileScriptVersionManager` Class
Implements the `ScriptVersionManager` interface, also using a file-based approach for the JVM.

*   **Purpose:** To manage and track different versions of scripts, enabling retrieval of specific versions and rollback capabilities.
*   **Constructor:** `FileScriptVersionManager(private val baseDirectory: String, private val scriptStorage: ScriptStorage)`
*   **Storage Mechanism:**
    *   Script versions are stored as individual `.kts` files within a `versions` subdirectory of `baseDirectory`, further organized into subdirectories named after the `scriptName` (e.g., `baseDirectory/versions/scriptName/1.kts`, `baseDirectory/versions/scriptName/2.kts`).
*   **Operations:**
    *   `addVersion()`: Determines the next version number, saves the new script version into its version-specific file path, and then updates the main script entry in the provided `scriptStorage` with metadata reflecting the new current version and timestamp.
    *   `getVersion()`: Reads the content of the specified version file.
    *   `getLatestVersion()`: Determines the highest version number (by checking metadata in `scriptStorage` and actual version files) and returns its source from `scriptStorage`.
    *   `rollback()`: Retrieves the source of the target rollback version, then saves this source back into the main `scriptStorage` as the current version, updating metadata to indicate the rollback.
    *   All operations use `Dispatchers.IO`.

#### 6.4.4. `SimpleScriptValidator` Class
A basic, non-compiler-based implementation of the `ScriptValidator` interface for the JVM.

*   **Purpose:** To perform rudimentary checks on script source code.
*   **Validation Logic:**
    *   Checks for unbalanced parentheses, brackets, and braces.
    *   Flags multiple statements on a single line not separated by semicolons (though semicolons are largely optional in Kotlin).
    *   Checks for empty import statements or imports ending with a semicolon.
    *   Flags an empty script.
*   **Limitations:** This validator does **not** perform full syntactic or semantic analysis that a Kotlin compiler would. It's a lightweight, preliminary checker.

#### 6.4.5. `ScriptManager` Class
A JVM-specific orchestrator class that integrates the various scripting components.

*   **Purpose:** To provide a unified, high-level API for managing the entire script lifecycle, from validation and compilation to storage, versioning, execution, and hot-reloading.
*   **Constructor:** `ScriptManager(scriptEngine, scriptStorage, scriptVersionManager, scriptValidator)`
*   **Key Functionalities:**
    *   Maintains an in-memory cache (`compiledScriptCache`) for `CompiledScript` objects.
    *   `compileAndSave()`: Orchestrates validation (`ScriptValidator`), compilation (`ScriptEngine`), saving (`ScriptStorage`), versioning (`ScriptVersionManager`), and caching.
    *   `loadAndCompile()`: Retrieves a script from `ScriptStorage` (if not cached), compiles it, and caches the result.
    *   `execute()`: Ensures a script is loaded/compiled, then executes it via `ScriptEngine`.
    *   `reloadScript()`: Clears a script from the cache and forces a `loadAndCompile` to pick up changes from `ScriptStorage`.
    *   `rollback()`: Uses `ScriptVersionManager` to perform a rollback and then reloads the script.
    *   Delegates `listScripts()` and `deleteScript()` to `ScriptStorage` (managing cache for delete).
*   **Exception Defined:** `ScriptValidationException`.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.scripting (commonMain)" {
        interface ScriptEngine { <<Interface>> }
        interface CompiledScript { <<Interface>> }
        interface ScriptValidator { <<Interface>> }
        class ValidationResult { }
        interface ScriptVersionManager { <<Interface>> }
        interface ScriptStorage { <<Interface>> }
    }

    package "io.github.solaceharmony.core.scripting (jvmMain)" {
        class JvmScriptEngine {
            +compile(): CompiledScript
            +execute(): Any?
            +eval(): Any?
        }
        ScriptEngine <|-- JvmScriptEngine
        JvmScriptEngine ..> "SimpleCompiledScript" : (inner class) creates & uses
        class "SimpleCompiledScript" {
             +name: String
             +compilationTimestamp: Long
             +source: String
        }
        CompiledScript <|-- "SimpleCompiledScript"


        class FileScriptStorage {
            +saveScript()
            +loadScript()
        }
        ScriptStorage <|-- FileScriptStorage

        class FileScriptVersionManager {
            +addVersion(): Int
            +getVersion(): String?
            +rollback(): Boolean
        }
        ScriptVersionManager <|-- FileScriptVersionManager
        FileScriptVersionManager o-- ScriptStorage : uses

        class SimpleScriptValidator {
            +validate(): ValidationResult
        }
        ScriptValidator <|-- SimpleScriptValidator

        class ScriptManager {
            -scriptEngine: ScriptEngine
            -scriptStorage: ScriptStorage
            -scriptVersionManager: ScriptVersionManager
            -scriptValidator: ScriptValidator
            +compileAndSave(): CompiledScript
            +loadAndCompile(): CompiledScript?
            +execute(): Any?
            +reloadScript(): CompiledScript?
            +rollback(): CompiledScript?
        }
        ScriptManager o-- ScriptEngine
        ScriptManager o-- ScriptStorage
        ScriptManager o-- ScriptVersionManager
        ScriptManager o-- ScriptValidator
        ScriptManager ..> CompiledScript : caches
    }
    note for JvmScriptEngine "Uses Kotlin scripting APIs for compilation and execution."
    note for SimpleScriptValidator "Performs basic, non-compiler checks."
```
These JVM implementations provide a functional, albeit with some current simplifications (like `JvmScriptEngine` and `SimpleScriptValidator`), scripting subsystem for SolaceCore, enabling dynamic code execution with support for file-based persistence and versioning.

---

← [§5 Workflow Module (`io.github.solaceharmony.core.workflow`)](./05-workflow-module-io-github-solaceharmony-core-workflow.md)  ·  [Index](./README.md)  ·  [§7 Build System and Dependencies](./07-build-system-and-dependencies.md) →
