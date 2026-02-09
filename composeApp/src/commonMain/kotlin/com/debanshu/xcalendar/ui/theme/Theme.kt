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
private val CalmLightColors =
    lightColorScheme(
        primary = Color(0xFF4C7D73),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1E8E2),
        onPrimaryContainer = Color(0xFF0F2E2B),
        secondary = Color(0xFF6E7FA2),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE1E6F0),
        onSecondaryContainer = Color(0xFF23283A),
        tertiary = Color(0xFFB58B7E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF2E1DA),
        onTertiaryContainer = Color(0xFF3B1F18),
        background = Color(0xFFF9F7F2),
        onBackground = Color(0xFF1F1C19),
        surface = Color(0xFFFAF8F5),
        onSurface = Color(0xFF1F1C19),
        surfaceVariant = Color(0xFFE8E3DB),
        onSurfaceVariant = Color(0xFF4B4540),
        outline = Color(0xFF8C857E),
        outlineVariant = Color(0xFFD0CAC2),
        error = Color(0xFFB2554E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF4D9D6),
        onErrorContainer = Color(0xFF3B0B08),
    )

private val CalmDarkColors =
    darkColorScheme(
        primary = Color(0xFF88BEB2),
        onPrimary = Color(0xFF0E2E2B),
        primaryContainer = Color(0xFF2E4E49),
        onPrimaryContainer = Color(0xFFD1E8E2),
        secondary = Color(0xFFA3B2D4),
        onSecondary = Color(0xFF1E2538),
        secondaryContainer = Color(0xFF39435C),
        onSecondaryContainer = Color(0xFFE1E6F0),
        tertiary = Color(0xFFD4A89B),
        onTertiary = Color(0xFF3A231B),
        tertiaryContainer = Color(0xFF5A3B31),
        onTertiaryContainer = Color(0xFFF2E1DA),
        background = Color(0xFF151312),
        onBackground = Color(0xFFE7E2DD),
        surface = Color(0xFF181615),
        onSurface = Color(0xFFE7E2DD),
        surfaceVariant = Color(0xFF3A3530),
        onSurfaceVariant = Color(0xFFC6BEB7),
        outline = Color(0xFF8B827B),
        outlineVariant = Color(0xFF4A4540),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF7D2A25),
        onErrorContainer = Color(0xFFF4D9D6),
    )

private val HighContrastLightColors =
    lightColorScheme(
        primary = Color(0xFF1C5A52),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFC7E3DD),
        onPrimaryContainer = Color(0xFF001F1C),
        secondary = Color(0xFF2F4F77),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDCE4F3),
        onSecondaryContainer = Color(0xFF0D1B33),
        tertiary = Color(0xFF885A48),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF1DED5),
        onTertiaryContainer = Color(0xFF2B140D),
        background = Color(0xFFFCFBF8),
        onBackground = Color(0xFF11100F),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF11100F),
        surfaceVariant = Color(0xFFE5E0D8),
        onSurfaceVariant = Color(0xFF2D2925),
        outline = Color(0xFF5B534C),
        outlineVariant = Color(0xFFBEB7AF),
        error = Color(0xFFA2241B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF6D9D6),
        onErrorContainer = Color(0xFF2E0402),
    )

private val HighContrastDarkColors =
    darkColorScheme(
        primary = Color(0xFF95CCC0),
        onPrimary = Color(0xFF002A25),
        primaryContainer = Color(0xFF2A5A52),
        onPrimaryContainer = Color(0xFFE0F3EE),
        secondary = Color(0xFFB7C7E6),
        onSecondary = Color(0xFF0C1C35),
        secondaryContainer = Color(0xFF3E4F6D),
        onSecondaryContainer = Color(0xFFE7ECF7),
        tertiary = Color(0xFFE2B6A9),
        onTertiary = Color(0xFF321C14),
        tertiaryContainer = Color(0xFF68493E),
        onTertiaryContainer = Color(0xFFF6E7E1),
        background = Color(0xFF0E0D0C),
        onBackground = Color(0xFFF5F2EE),
        surface = Color(0xFF11100F),
        onSurface = Color(0xFFF5F2EE),
        surfaceVariant = Color(0xFF3A3530),
        onSurfaceVariant = Color(0xFFE0D9D2),
        outline = Color(0xFFA29A92),
        outlineVariant = Color(0xFF57514A),
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
            useDarkTheme && highContrastEnabled -> HighContrastDarkColors
            useDarkTheme -> CalmDarkColors
            highContrastEnabled -> HighContrastLightColors
            else -> CalmLightColors
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
