<!-- topic: Runtime -->
<!-- title: Scripting Supporting Components -->

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


[Back to Scripting Engine](Scripting-Engine)
