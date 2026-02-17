package com.debanshu.xcalendar.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WCAG 2.1 Contrast Ratio Testing Utilities
 * 
 * WCAG AA Requirements:
 * - Normal text (< 18pt or < 14pt bold): 4.5:1 minimum
 * - Large text (>= 18pt or >= 14pt bold): 3.0:1 minimum
 * 
 * WCAG AAA Requirements:
 * - Normal text: 7.0:1 minimum
 * - Large text: 4.5:1 minimum
 */

/**
 * Calculates the WCAG 2.1 contrast ratio between two colors.
 * 
 * @param foreground The foreground (text) color
 * @param background The background color
 * @return Contrast ratio from 1:1 (no contrast) to 21:1 (maximum contrast)
 */
fun calculateContrastRatio(foreground: Color, background: Color): Double {
    val l1 = foreground.luminance().toDouble()
    val l2 = background.luminance().toDouble()
    
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Checks if contrast ratio meets WCAG AA for normal text (4.5:1).
 */
fun meetsWCAG_AA_NormalText(foreground: Color, background: Color): Boolean {
    return calculateContrastRatio(foreground, background) >= 4.5
}

/**
 * Checks if contrast ratio meets WCAG AA for large text (3.0:1).
 */
fun meetsWCAG_AA_LargeText(foreground: Color, background: Color): Boolean {
    return calculateContrastRatio(foreground, background) >= 3.0
}

/**
 * Checks if contrast ratio meets WCAG AAA for normal text (7.0:1).
 */
fun meetsWCAG_AAA_NormalText(foreground: Color, background: Color): Boolean {
    return calculateContrastRatio(foreground, background) >= 7.0
}

/**
 * Checks if contrast ratio meets WCAG AAA for large text (4.5:1).
 */
fun meetsWCAG_AAA_LargeText(foreground: Color, background: Color): Boolean {
    return calculateContrastRatio(foreground, background) >= 4.5
}

/**
 * Format contrast ratio for display.
 */
fun formatContrastRatio(ratio: Double): String {
    return "%.1f:1".format(ratio)
}

/**
 * Test suite for SoftFeminineColors WCAG AA compliance.
 */
class ContrastRatioTest {
    
    private val softFeminineSurface = Color(0xFFFCF9FA)
    private val softFeminineOnSurface = Color(0xFF1F1C19)
    private val softFeminineSurfaceVariant = Color(0xFFF0E8EB)
    private val softFeminineOnSurfaceVariant = Color(0xFF3F3539)
    private val softFemininePrimary = Color(0xFF846C92)
    private val softFeminineOnPrimary = Color(0xFFFFFFFF)
    private val softFeminineBackground = Color(0xFFFDF8F9)
    private val softFeminineOnBackground = Color(0xFF1F1C19)
    private val softFeminineOutline = Color(0xFF7D6B71)
    
    @Test
    fun softFeminineColors_onSurface_meetsWCAG_AA() {
        val ratio = calculateContrastRatio(softFeminineOnSurface, softFeminineSurface)
        assertTrue(
            ratio >= 4.5,
            "onSurface on surface contrast ratio ${formatContrastRatio(ratio)} should meet WCAG AA (4.5:1)"
        )
    }
    
    @Test
    fun softFeminineColors_onSurfaceVariant_meetsWCAG_AA() {
        val ratio = calculateContrastRatio(softFeminineOnSurfaceVariant, softFeminineSurfaceVariant)
        assertTrue(
            ratio >= 4.5,
            "onSurfaceVariant on surfaceVariant contrast ratio ${formatContrastRatio(ratio)} should meet WCAG AA (4.5:1)"
        )
    }
    
    @Test
    fun softFeminineColors_onPrimary_meetsWCAG_AA() {
        val ratio = calculateContrastRatio(softFeminineOnPrimary, softFemininePrimary)
        assertTrue(
            ratio >= 4.5,
            "onPrimary on primary contrast ratio ${formatContrastRatio(ratio)} should meet WCAG AA (4.5:1)"
        )
    }
    
    @Test
    fun softFeminineColors_onBackground_meetsWCAG_AA() {
        val ratio = calculateContrastRatio(softFeminineOnBackground, softFeminineBackground)
        assertTrue(
            ratio >= 4.5,
            "onBackground on background contrast ratio ${formatContrastRatio(ratio)} should meet WCAG AA (4.5:1)"
        )
    }
    
    @Test
    fun softFeminineColors_outline_meetsWCAG_AA_LargeText() {
        val ratio = calculateContrastRatio(softFeminineOutline, softFeminineSurfaceVariant)
        assertTrue(
            ratio >= 3.0,
            "outline on surfaceVariant contrast ratio ${formatContrastRatio(ratio)} should meet WCAG AA for large text (3.0:1)"
        )
    }
    
    @Test
    fun extendedColors_weekendText_meetsWCAG_AA() {
        val weekendText = Color(0xFF5A4B52)
        val ratio = calculateContrastRatio(weekendText, softFeminineSurface)
        assertTrue(
            ratio >= 4.5,
            "weekendText on surface contrast ratio ${formatContrastRatio(ratio)} should meet WCAG AA (4.5:1)"
        )
    }
    
    @Test
    fun extendedColors_holiday_meetsWCAG_AA() {
        val holiday = Color(0xFF826C88)
        val ratio = calculateContrastRatio(holiday, softFeminineSurface)
        assertTrue(
            ratio >= 4.5,
            "holiday on surface contrast ratio ${formatContrastRatio(ratio)} should meet WCAG AA (4.5:1)"
        )
    }
}
