package app.dnsgate

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

val Neon = Color(0xFFFFE600)
val Amber = Color(0xFFFFB000)
val Orange = Color(0xFFFF7A00)
val Bg = Color(0xFF060606)
val CardBg = Color(0xFF14130A)
val Line = Color(0xFF2E2A10)
val Mut = Color(0xFF9D987A)
val Ink = Color(0xFFF7F4DA)
val Bad = Color(0xFFFF5C5C)
val NeonBrush = Brush.horizontalGradient(listOf(Neon, Amber, Orange))

@Composable
fun DnsGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon, onPrimary = Color.Black, background = Bg, surface = CardBg,
            onSurface = Ink, onBackground = Ink
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalContentColor provides Ink,
            content = content
        )
    }
}
