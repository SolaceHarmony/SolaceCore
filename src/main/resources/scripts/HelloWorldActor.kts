// HelloWorldActor.kts

fun main(args: Array<String>) {
    val message = args.getOrElse(0) { "Hello from Kotlin Script" }
    println(message)
}