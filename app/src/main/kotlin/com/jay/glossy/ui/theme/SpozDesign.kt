/**
 * Spo~z — design system
 * Licensed under GPL-3.0
 *
 * Single source of truth for Spo~z's look, translated 1:1 from the design
 * tokens in the Move Sync prototype. Every screen reads its colours, type and
 * shape from here, so the whole app stays consistent and there is exactly one
 * place to change a token.
 *
 * Tokens:
 *   light  bg #DCD0FF  card #FFFFFF  ink #0B0B0F  mut #6F6890
 *   dark   bg #0B0B0F  card #18161F  ink #FFFFFF  mut #A29CC0
 *   accents  purple #B79CFF  orange #FFB347  blue #8E9CFF  pink #FF6FC8  green #7BE0C3
 *   font   Poppins (400/500/600/700), user-switchable
 */

package com.jay.glossy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Raw colour tokens exactly as they appear in the prototype. */
object SpozPalette {
    val LightBackground = Color(0xFFDCD0FF)
    val LightSurface = Color(0xFFFFFFFF)
    val LightInk = Color(0xFF0B0B0F)
    val LightMuted = Color(0xFF6F6890)

    val DarkBackground = Color(0xFF0B0B0F)
    val DarkSurface = Color(0xFF18161F)
    val DarkInk = Color(0xFFFFFFFF)
    val DarkMuted = Color(0xFFA29CC0)
}

/**
 * The five accent colours the prototype cycles through for covers, chips and
 * highlights. Order matters — same order as the prototype's CL array.
 */
object SpozAccents {
    val Purple = Color(0xFFB79CFF)
    val Orange = Color(0xFFFFB347)
    val Blue = Color(0xFF8E9CFF)
    val Pink = Color(0xFFFF6FC8)
    val Green = Color(0xFF7BE0C3)

    val all = listOf(Purple, Orange, Blue, Pink, Green)

    /** Accent for an index, matching the prototype's CL[i % 5]. */
    fun forIndex(i: Int): Color = all[((i % all.size) + all.size) % all.size]
}

/** Default accent — the prototype's ST.acc default of #b79cff. */
val SpozDefaultAccent: Color = SpozAccents.Purple

fun spozLightColorScheme(accent: Color = SpozDefaultAccent): ColorScheme =
    lightColorScheme(
        primary = accent,
        onPrimary = SpozPalette.LightInk,
        secondary = SpozAccents.Blue,
        tertiary = SpozAccents.Green,
        background = SpozPalette.LightBackground,
        onBackground = SpozPalette.LightInk,
        surface = SpozPalette.LightSurface,
        onSurface = SpozPalette.LightInk,
        surfaceVariant = SpozPalette.LightBackground,
        onSurfaceVariant = SpozPalette.LightMuted,
        outline = SpozPalette.LightMuted,
        error = SpozAccents.Pink,
    )

fun spozDarkColorScheme(accent: Color = SpozDefaultAccent): ColorScheme =
    darkColorScheme(
        primary = accent,
        onPrimary = SpozPalette.DarkInk,
        secondary = SpozAccents.Blue,
        tertiary = SpozAccents.Green,
        background = SpozPalette.DarkBackground,
        onBackground = SpozPalette.DarkInk,
        surface = SpozPalette.DarkSurface,
        onSurface = SpozPalette.DarkInk,
        surfaceVariant = SpozPalette.DarkSurface,
        onSurfaceVariant = SpozPalette.DarkMuted,
        outline = SpozPalette.DarkMuted,
        error = SpozAccents.Pink,
    )

/** Rounded, generous corners — the prototype leans on large radii. */
val SpozShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Type scale built on the prototype's weights (400/500/600/700).
 * [brandFont] is injected so the listener can switch fonts at runtime.
 */
fun spozTypography(brandFont: FontFamily = FontFamily.Default): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 60.sp),
        headlineLarge = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
        headlineMedium = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
        titleLarge = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
        bodyMedium = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp),
        labelLarge = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp),
        labelMedium = TextStyle(fontFamily = brandFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )
}

/**
 * The Spo~z theme wrapper. Mirrors the prototype's apply(): dark/light from the
 * system (with an override), and a user-chosen accent colour.
 */
@Composable
fun SpozTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = SpozDefaultAccent,
    brandFont: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) spozDarkColorScheme(accent) else spozLightColorScheme(accent)
    MaterialTheme(
        colorScheme = scheme,
        shapes = SpozShapes,
        typography = spozTypography(brandFont),
        content = content,
    )
}
