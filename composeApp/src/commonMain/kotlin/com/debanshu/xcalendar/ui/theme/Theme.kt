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

val LocalSharedTransitionScope =
    compositionLocalOf<SharedTransitionScope> {
        throw IllegalStateException("No SharedTransitionScope provided")
    }

/**
 * Main theme composable for the XCalendar app
 *
 * @param darkTheme Whether to use dark theme, defaults to system setting
 * @param content The content to be themed
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XCalendarTheme(
    shapes: Shapes = XCalendarTheme.shapes,
    typography: Typography = XCalendarTheme.typography,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) CalmDarkColors else CalmLightColors
    CompositionLocalProvider {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = typography,
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
        get() = Typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable
        get() = AppShapes

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val motion: MotionScheme
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.motionScheme
}
