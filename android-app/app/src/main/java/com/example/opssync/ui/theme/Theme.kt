package com.example.opssync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Brand Colors ────────────────────────────────────────────
// These are the STRICT colors defined in the design spec

val Primary    = Color(0xFF213448)  // Dark navy – headers, buttons, nav bar
val Secondary  = Color(0xFF547792)  // Medium blue – secondary elements
val Accent     = Color(0xFF94B4C1)  // Light blue – chips, badges, dividers
val Background = Color(0xFFEAE0CF)  // Warm cream – app background

// ─── Semantic / Status Colors ─────────────────────────────────
val ColorSuccess  = Color(0xFF2E7D32)   // Green for success states
val ColorRunning  = Color(0xFF1565C0)   // Blue for running states
val ColorFailed   = Color(0xFFC62828)   // Red for failed/critical states
val ColorPending  = Color(0xFF6D6D6D)   // Grey for pending states
val ColorWarning  = Color(0xFFE65100)   // Orange for warnings
val ColorMedium   = Color(0xFFE65100)   // Orange for medium severity

// ─── Surface / Background Variants ───────────────────────────
val SurfaceCard   = Color(0xFFF5EFE6)   // Slightly lighter than background for cards
val SurfaceDark   = Color(0xFF213448)   // Dark surface (header card)
val DividerColor  = Color(0xFFD9CFC0)   // Subtle divider

// ─── Status Background Colors (tinted) ───────────────────────
val SuccessBg = Color(0xFFE8F5E9)
val FailedBg  = Color(0xFFFFEBEE)
val RunningBg = Color(0xFFE3F2FD)
val PendingBg = Color(0xFFF5F5F5)

// ─── Color Scheme ─────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = Color.White,
    secondary        = Secondary,
    onSecondary      = Color.White,
    tertiary         = Accent,
    background       = Background,
    onBackground     = Primary,
    surface          = SurfaceCard,
    onSurface        = Primary,
    error            = ColorFailed,
    onError          = Color.White
)

// ─── Typography ───────────────────────────────────────────────
val OpsSyncTypography = Typography(
    // Large display number (e.g., 99.98%, pipeline counts)
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        color = Color.White
    ),
    // Section header (e.g., "Active Pipelines")
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Primary
    ),
    // Card title (pipeline/incident name)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Primary
    ),
    // Sub-label (environment, time, etc.)
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = Secondary
    ),
    // Body text
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = Primary
    ),
    // Smaller supporting text
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = Secondary
    ),
    // Button / label text
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
)

// ─── OpsSync Theme ────────────────────────────────────────────
// Wrap your entire app in this composable
@Composable
fun OpsSyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography   = OpsSyncTypography,
        content      = content
    )
}
