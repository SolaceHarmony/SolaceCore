package ai.solace.core.actors

import javax.script.ScriptEngineManager
import java.io.File

class ScriptingEngineActor : java.io.Serializable {
    private val engine = ScriptEngineManager().getEngineByExtension("kts")
    fun executeScript(scriptPath: String, args: Array<out Map<String, String>>): Any? {
        val scriptContent = File(scriptPath).readText()

        // Put each argument into the script context
        for ((name, value) in args) {
            engine.put(name, value)
        }

        return engine.eval(scriptContent)
    }

    @Throws(java.io.IOException::class)
    private fun writeObject(out: java.io.ObjectOutputStream) {
        out.defaultWriteObject()
        // Serialize any additional state here if needed
        out.writeObject(engine.factory.engineName)
    }

    @Throws(java.io.IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: java.io.ObjectInputStream) {
        `in`.defaultReadObject()
        // Deserialize any additional state here if needed
        engine = ScriptEngineManager().getEngineByName(`in`.readObject() as String)
    }
}
