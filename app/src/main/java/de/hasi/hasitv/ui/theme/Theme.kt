package de.hasi.hasitv.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Colors ──────────────────────────────────────────────────────
object HasiColors {
    // Dark
    val DarkBg       = Color(0xFF0A0A0A)
    val DarkSurface  = Color(0xFF1C1C1E)
    val DarkSurface2 = Color(0xFF2C2C2E)
    val DarkBorder   = Color(0xFF3A3A3C)
    // Light
    val LightBg      = Color(0xFFF2F2F7)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurface2= Color(0xFFE5E5EA)
    val LightBorder  = Color(0xFFC6C6C8)
    // Accent
    val Red          = Color(0xFFE50914)
    val RedLight     = Color(0xFFFF3D3D)
    val White        = Color(0xFFFFFFFF)
    val TextSecondary= Color(0xFF999999)
}

private val DarkColorScheme = darkColorScheme(
    primary          = HasiColors.Red,
    onPrimary        = HasiColors.White,
    background       = HasiColors.DarkBg,
    surface          = HasiColors.DarkSurface,
    surfaceVariant   = HasiColors.DarkSurface2,
    onBackground     = HasiColors.White,
    onSurface        = HasiColors.White,
    outline          = HasiColors.DarkBorder,
)

private val LightColorScheme = lightColorScheme(
    primary          = HasiColors.Red,
    onPrimary        = HasiColors.White,
    background       = HasiColors.LightBg,
    surface          = HasiColors.LightSurface,
    surfaceVariant   = HasiColors.LightSurface2,
    onBackground     = Color(0xFF1A1A1A),
    onSurface        = Color(0xFF1A1A1A),
    outline          = HasiColors.LightBorder,
)

@Composable
fun HasiTVTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content     = content
    )
}
