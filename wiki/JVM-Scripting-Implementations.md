<!-- topic: Runtime -->
<!-- title: JVM Scripting Implementations -->

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

← [§5 Workflow Module (`io.github.solaceharmony.core.workflow`)](Workflow-Orchestration)  ·  [Architecture Overview](Architecture-Overview)  ·  [§7 Build System and Dependencies](Build-System-and-Dependencies) →


[Back to Scripting Engine](Scripting-Engine)
