<!-- topic: Runtime -->
<!-- title: Scripting Module Architecture -->

[← Architecture Overview](Architecture-Overview) · §6 of 15

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


[Back to Scripting Engine](Scripting-Engine)
