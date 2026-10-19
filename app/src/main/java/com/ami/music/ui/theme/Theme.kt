package com.ami.music.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Lime = Color(0xFFB7F56B)
val Ink = Color(0xFF10110F)
val Panel = Color(0xFF1C1E1A)
val Muted = Color(0xFFA5AA9E)
private val colors = darkColorScheme(
    primary = Lime, onPrimary = Ink, secondary = Lime, onSecondary = Ink,
    background = Ink, onBackground = Color(0xFFF3F4ED), surface = Ink,
    onSurface = Color(0xFFF3F4ED), surfaceVariant = Panel, onSurfaceVariant = Muted,
    outline = Color(0xFF3C4037), error = Color(0xFFFFB4AB),
)
@Composable fun AmiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.6).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
    ), content = content)
}
