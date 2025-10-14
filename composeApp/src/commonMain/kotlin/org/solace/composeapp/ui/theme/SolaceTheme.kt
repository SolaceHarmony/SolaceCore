package org.solace.composeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SolaceDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF001C2C),
    primaryContainer = Color(0xFF0F172A),
    onPrimaryContainer = Color(0xFFD6F4FF),
    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E1B4B),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF002022),
    background = Color(0xFF020817),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5F5),
    error = Color(0xFFF87171),
    onError = Color(0xFF410002)
)

private val SolaceTypography = Typography()
private val SolaceShapes = Shapes()

@Composable
fun SolaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SolaceDarkColorScheme,
        typography = SolaceTypography,
        shapes = SolaceShapes,
        content = content
    )
}
