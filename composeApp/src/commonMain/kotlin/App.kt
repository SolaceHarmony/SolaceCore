package org.solace.composeapp

import androidx.compose.runtime.Composable

@Composable
fun App() {
    Greeting("Solace")
}

@Composable
fun Greeting(name: String) {
    PlatformText("Hello, $name!")
}

@Composable
expect fun PlatformText(text: String)
