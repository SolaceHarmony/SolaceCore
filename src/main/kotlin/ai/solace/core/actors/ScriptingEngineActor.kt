package ai.solace.core.actors

import javax.script.ScriptEngineManager
import java.io.File

class ScriptingEngineActor {
    private val engine = ScriptEngineManager().getEngineByExtension("kts")
    fun executeScript(scriptPath: String, args: Array<out Map<String, String>>): Any? {
        val scriptContent = File(scriptPath).readText()

        // Put each argument into the script context
        for ((name, value) in args) {
            engine.put(name, value)
        }

        return engine.eval(scriptContent)
    }
}
