# Advanced Workflow Example

This example demonstrates an advanced workflow that chains multiple actors with error handling and dynamic port usage. It illustrates how to build a resilient pipeline using the SolaceCore framework.

## Overview
1. **TextProcessorActor** performs text normalization.
2. **FilterActor** filters messages based on custom rules.
3. **ScriptActor** performs custom processing defined at runtime.
4. **WorkflowManager** orchestrates message flow and manages errors.

## Sample Code
```kotlin
val supervisor = SupervisorActor()
val workflowManager = WorkflowManager()

val textProcessor = TextProcessorActor()
val filter = FilterActor(rules = listOf("[a-zA-Z]+"))
val scriptActor = ScriptActor(script = "return input.uppercase()")

supervisor.register(textProcessor)
supervisor.register(filter)
supervisor.register(scriptActor)

workflowManager.connectActors(textProcessor.output, filter.input)
workflowManager.connectActors(filter.output, scriptActor.input)

workflowManager.execute("Hello World")
```

This workflow shows how actors can be combined to create complex processing pipelines. You can extend it further by adding additional actors or custom error handling.
