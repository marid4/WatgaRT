package com.wargart.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = NavyPrimary,
    secondary = GoldAccent,
    onSecondary = NavyPrimary,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = NavyPrimary,
    tertiary = NavyPrimary,
    background = LightGrayBg,
    onBackground = NavyPrimary,
    surface = PureWhite,
    onSurface = NavyPrimary,
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = SlateMuted,
    outline = LightBorder,
    error = CrimsonDanger,
    onError = PureWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlueLight,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = PureWhite,
    secondary = GoldAccent,
    onSecondary = NavyPrimary,
    background = NavyDarkSurface,
    onBackground = PureWhite,
    surface = NavyCardDark,
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF17355E),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF264573),
    error = Color(0xFFF87171),
    onError = NavyPrimary
)

private val WargaRtTypography = Typography(
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun WargaRtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = WargaRtTypography,
        content = content
    )
}
