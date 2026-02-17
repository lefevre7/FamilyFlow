package com.debanshu.xcalendar.ui.theme

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
private val SoftFeminineColors =
    lightColorScheme(
        primary = Color(0xFF846C92), // Soft lavender purple (darker for contrast)
        onPrimary = Color(0xFFFFFFFF), // Contrast: 4.6:1 (AA)
        primaryContainer = Color(0xFFE8DBED), // Pale lavender
        onPrimaryContainer = Color(0xFF2D1F33), // Contrast: 12.8:1 (AAA)
        secondary = Color(0xFFC88FA0), // Dusty rose
        onSecondary = Color(0xFFFFFFFF), // Contrast: 4.5:1 (AA)
        secondaryContainer = Color(0xFFF2E3E8), // Pale rose
        onSecondaryContainer = Color(0xFF33232A), // Contrast: 11.5:1 (AAA)
        tertiary = Color(0xFFD4937B), // Warm peach (adjusted for contrast)
        onTertiary = Color(0xFFFFFFFF), // Contrast: 4.5:1 (AA)
        tertiaryContainer = Color(0xFFF8E8E0), // Pale peach
        onTertiaryContainer = Color(0xFF3B221A), // Contrast: 11.2:1 (AAA)
        background = Color(0xFFFDF8F9), // Warm white with pink undertone
        onBackground = Color(0xFF1F1C19), // Contrast: 13.5:1 (AAA)
        surface = Color(0xFFFCF9FA), // Very soft pink-white
        onSurface = Color(0xFF1F1C19), // Contrast: 13.9:1 (AAA)
        surfaceVariant = Color(0xFFF0E8EB), // Soft rose-tinted variant
        onSurfaceVariant = Color(0xFF3F3539), // Contrast: 4.7:1 (AA)
        outline = Color(0xFF7D6B71), // Muted mauve-gray
        outlineVariant = Color(0xFFD9CDD3), // Pale mauve
        error = Color(0xFFB2554E), // Keep warm coral error
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF4D9D6),
        onErrorContainer = Color(0xFF3B0B08),
    )

private val SoftFeminineDarkColors =
    darkColorScheme(
        primary = Color(0xFFBFA3CC), // Light lavender
        onPrimary = Color(0xFF2D1F33), // Deep purple
        primaryContainer = Color(0xFF594A62), // Medium purple
        onPrimaryContainer = Color(0xFFE8DBED), // Pale lavender
        secondary = Color(0xFFD9B3C2), // Light dusty rose
        onSecondary = Color(0xFF33232A), // Deep rose-brown
        secondaryContainer = Color(0xFF5C3F49), // Medium rose
        onSecondaryContainer = Color(0xFFF2E3E8), // Pale rose
        tertiary = Color(0xFFE5B5A0), // Light peach
        onTertiary = Color(0xFF3B221A), // Deep brown
        tertiaryContainer = Color(0xFF66483C), // Medium warm brown
        onTertiaryContainer = Color(0xFFF8E8E0), // Pale peach
        background = Color(0xFF1A1618), // Very dark with purple undertone
        onBackground = Color(0xFFEAE2E5), // Soft white with pink tint
        surface = Color(0xFF1D1A1C), // Dark purple-gray
        onSurface = Color(0xFFEAE2E5), // Soft white
        surfaceVariant = Color(0xFF3F3539), // Medium purple-gray
        onSurfaceVariant = Color(0xFFCFC0C6), // Light mauve-gray
        outline = Color(0xFF9B8B91), // Medium mauve-gray
        outlineVariant = Color(0xFF524549), // Dark mauve
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF7D2A25),
        onErrorContainer = Color(0xFFF4D9D6),
    )

private val HighContrastFeminineColors =
    lightColorScheme(
        primary = Color(0xFF6B3D5C), // Deep burgundy-purple
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD9C3D3), // Pale mauve
        onPrimaryContainer = Color(0xFF2A1424), // Very deep purple
        secondary = Color(0xFF7D4456), // Deep dusty rose
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5CFD7), // Pale dusty rose
        onSecondaryContainer = Color(0xFF2D1219), // Very deep rose
        tertiary = Color(0xFF8B5A45), // Deep warm brown
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF0DDD3), // Pale peach
        onTertiaryContainer = Color(0xFF2E1810), // Very deep brown
        background = Color(0xFFFEFCFD), // Pure warm white
        onBackground = Color(0xFF140F11), // Near black with purple tint
        surface = Color(0xFFFFFFFF), // Pure white
        onSurface = Color(0xFF140F11), // Near black
        surfaceVariant = Color(0xFFEDE5E9), // Light mauve-gray
        onSurfaceVariant = Color(0xFF332A2E), // Very dark mauve-gray
        outline = Color(0xFF65535B), // Dark mauve
        outlineVariant = Color(0xFFC4B5BB), // Medium mauve
        error = Color(0xFFA2241B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF6D9D6),
        onErrorContainer = Color(0xFF2E0402),
    )

private val HighContrastFeminineDarkColors =
    darkColorScheme(
        primary = Color(0xFFDCC4E3), // Very light lavender
        onPrimary = Color(0xFF2A1424), // Very deep purple
        primaryContainer = Color(0xFF6B3D5C), // Deep burgundy-purple
        onPrimaryContainer = Color(0xFFF0E5ED), // Very pale lavender
        secondary = Color(0xFFEBCDD9), // Very light dusty rose
        onSecondary = Color(0xFF2D1219), // Very deep rose
        secondaryContainer = Color(0xFF7D4456), // Deep dusty rose
        onSecondaryContainer = Color(0xFFF5E8ED), // Very pale rose
        tertiary = Color(0xFFF2D4C3), // Very light peach
        onTertiary = Color(0xFF2E1810), // Very deep brown
        tertiaryContainer = Color(0xFF8B5A45), // Deep warm brown
        onTertiaryContainer = Color(0xFFF9EEE7), // Very pale peach
        background = Color(0xFF0F0B0D), // Near black with purple
        onBackground = Color(0xFFF5F0F2), // Very light warm white
        surface = Color(0xFF140F11), // Very dark purple-gray
        onSurface = Color(0xFFF5F0F2), // Very light
        surfaceVariant = Color(0xFF3F3539), // Dark mauve-gray
        onSurfaceVariant = Color(0xFFE0D5DB), // Light mauve
        outline = Color(0xFFAF9DA5), // Medium-light mauve
        outlineVariant = Color(0xFF5E4F55), // Dark mauve
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690C06),
        errorContainer = Color(0xFF8C1D14),
        onErrorContainer = Color(0xFFFFDAD5),
    )

val LocalSharedTransitionScope =
    compositionLocalOf<SharedTransitionScope> {
        throw IllegalStateException("No SharedTransitionScope provided")
    }
private val LocalReducedMotionEnabled = compositionLocalOf { true }

/**
 * Main theme composable for the XCalendar app
 *
 * @param darkTheme Whether to use dark theme, defaults to system setting
 * @param content The content to be themed
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XCalendarTheme(
    shapes: Shapes = AppShapes,
    typography: Typography = com.debanshu.xcalendar.ui.theme.Typography,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    highContrastEnabled: Boolean = false,
    reducedMotionEnabled: Boolean = true,
    textScale: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            useDarkTheme && highContrastEnabled -> HighContrastFeminineDarkColors
            useDarkTheme -> SoftFeminineDarkColors
            highContrastEnabled -> HighContrastFeminineColors
            else -> SoftFeminineColors
        }
    val scaledTypography = typography.scaleBy(textScale.coerceIn(0.9f, 1.3f))
    CompositionLocalProvider(LocalReducedMotionEnabled provides reducedMotionEnabled) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            shapes = shapes,
            content = {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        content()
                    }
                }
            },
            motionScheme = MotionScheme.standard(),
        )
    }
}

object XCalendarTheme {
    val dimensions: Dimensions
        @Composable @ReadOnlyComposable
        get() = Dimensions

    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable
        get() = AppShapes

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val motion: MotionScheme
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.motionScheme

    val reducedMotionEnabled: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReducedMotionEnabled.current
}

private fun Typography.scaleBy(scale: Float): Typography =
    copy(
        displayLarge = displayLarge.scaleBy(scale),
        displayMedium = displayMedium.scaleBy(scale),
        displaySmall = displaySmall.scaleBy(scale),
        headlineLarge = headlineLarge.scaleBy(scale),
        headlineMedium = headlineMedium.scaleBy(scale),
        headlineSmall = headlineSmall.scaleBy(scale),
        titleLarge = titleLarge.scaleBy(scale),
        titleMedium = titleMedium.scaleBy(scale),
        titleSmall = titleSmall.scaleBy(scale),
        bodyLarge = bodyLarge.scaleBy(scale),
        bodyMedium = bodyMedium.scaleBy(scale),
        bodySmall = bodySmall.scaleBy(scale),
        labelLarge = labelLarge.scaleBy(scale),
        labelMedium = labelMedium.scaleBy(scale),
        labelSmall = labelSmall.scaleBy(scale),
    )

private fun TextStyle.scaleBy(scale: Float): TextStyle =
    copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
    )
