package com.debanshu.xcalendar.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * Person avatar color contrast utilities.
 * Ensures person color indicators meet WCAG AA requirements for text overlays.
 */

/**
 * Validates that a person color + text combination meets WCAG AA (4.5:1).
 * 
 * @param personColor The person's avatar/indicator color
 * @param textColor The text color to display on the person color
 * @return true if contrast ratio meets WCAG AA (4.5:1)
 */
fun validatePersonColorContrast(personColor: Color, textColor: Color): Boolean {
    val ratio = calculateContrastRatio(personColor, textColor)
    return ratio >= 4.5
}

/**
 * Calculates contrast ratio between two colors.
 */
private fun calculateContrastRatio(foreground: Color, background: Color): Double {
    val l1 = foreground.luminance().toDouble()
    val l2 = background.luminance().toDouble()
    
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Suggests whether to use light or dark text on a person color.
 * 
 * @param personColor The person's avatar/indicator color
 * @return Color.White if dark text won't meet WCAG AA, otherwise Color.Black
 */
fun suggestTextColorForPersonColor(personColor: Color): Color {
    val whiteContrast = calculateContrastRatio(Color.White, personColor)
    val blackContrast = calculateContrastRatio(Color.Black, personColor)
    
    // Prefer black text for readability, but use white if black doesn't meet AA
    return if (blackContrast >= 4.5) Color.Black else Color.White
}

/**
 * Adjusts a person color to ensure it meets WCAG AA contrast with the given text color.
 * If the color already meets requirements, returns it unchanged.
 * 
 * @param personColor The original person color
 * @param textColor The text color to display on the person color
 * @return Adjusted color that meets WCAG AA with textColor
 */
fun ensurePersonColorContrast(personColor: Color, textColor: Color): Color {
    if (validatePersonColorContrast(personColor, textColor)) {
        return personColor
    }
    
    // Darken or lighten the person color to meet contrast requirements
    val targetLuminance = if (textColor.luminance() > 0.5) {
        // Dark text needs darker background
        0.1f
    } else {
        // Light text needs lighter background
        0.6f
    }
    
    return adjustColorLuminance(personColor, targetLuminance)
}

/**
 * Adjusts color luminance while preserving hue.
 */
private fun adjustColorLuminance(color: Color, targetLuminance: Float): Color {
    val currentLuminance = color.luminance()
    if (currentLuminance == 0f) return color
    
    val factor = targetLuminance / currentLuminance
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}
