package ai.solace.core.actors

class SupervisorActor(private val scriptingEngineActor: ScriptingEngineActor) {
        fun deployActor(scriptName: String, vararg params: Map<String, String>): Any? {
            val scriptPath = "src/main/resources/scripts/$scriptName"
            return scriptingEngineActor.executeScript(scriptPath, params)
        }
}
