package org.solace.composeapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Pass the body element directly instead of a container id.
    ComposeViewport(document.body!!) {
        App()
    }
}
