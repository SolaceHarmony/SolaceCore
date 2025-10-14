package org.solace.composeapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Mount the app into the root element defined in src/webMain/resources/index.html
    ComposeViewport(viewportContainerId = "root") {
        App()
    }
}
