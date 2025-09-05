package org.solace.composeapp

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Text

@Composable
actual fun PlatformText(text: String) {
    Text(text)
}
