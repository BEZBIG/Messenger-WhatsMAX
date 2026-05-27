/** Material3 тема и кастомные цвета мессенджера. */
package com.whatsmax.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val PrimaryBlue     = Color(0xFF0088CC)
private val BackgroundLight = Color(0xFFF5F5F5)
private val BackgroundDark  = Color(0xFF1A1A1A)
private val SurfaceLight    = Color(0xFFFFFFFF)
private val SurfaceDark     = Color(0xFF2B2B2B)

private val MessageOwnLight   = Color(0xFFDCF8C6)
private val MessageOwnDark    = Color(0xFF056162)
private val MessageOtherLight = Color(0xFFFFFFFF)
private val MessageOtherDark  = Color(0xFF323739)

val OnlineIndicator = Color(0xFF4CAF50)

private val LightColorScheme = lightColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFB3E5FC),
    secondary        = Color(0xFF26A69A),
    background       = BackgroundLight,
    surface          = SurfaceLight,
    onSurface        = Color(0xFF1C1C1E),
    surfaceVariant   = Color(0xFFEEEEEE),
    outline          = Color(0xFFBDBDBD)
)

private val DarkColorScheme = darkColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF004D77),
    secondary        = Color(0xFF4DB6AC),
    background       = BackgroundDark,
    surface          = SurfaceDark,
    onSurface        = Color(0xFFE5E5E5),
    surfaceVariant   = Color(0xFF3A3A3A),
    outline          = Color(0xFF5C5C5C)
)

@Composable
fun WhatsMAXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}

val messageBubbleOwn: Color
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) MessageOwnDark else MessageOwnLight

val messageBubbleOther: Color
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) MessageOtherDark else MessageOtherLight
