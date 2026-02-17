package com.debanshu.xcalendar.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Extended color scheme for XCalendar-specific colors.
 * These colors supplement Material3's ColorScheme for domain-specific needs.
 * Soft Lavender Feminine Palette.
 */
object XCalendarColors {
    // Holiday colors - soft lavender
    val holiday = Color(0xFF826C88) // Contrast: 4.5:1 on surface (AA)
    val holidayContainer = Color(0xFFB497BD).copy(alpha = 0.12f)
    val onHoliday = Color.White // Contrast: 4.8:1 (AA)
    
    // Schedule holiday (mint-green variant for variety)
    val scheduleHoliday = Color(0xFF8BB5A3) // Soft mint
    val scheduleHolidayContainer = Color(0xFF8BB5A3).copy(alpha = 0.16f)
    
    // Event indicator colors for month view - rose-pink
    val eventDot = Color(0xFFD4A5B8) // Dusty rose-pink
    
    // Calendar grid colors - soft mauve
    val gridLine = Color(0xFFE5D9DE) // Soft mauve-pink
    val currentTimeLine = Color(0xFFE39B9B) // Coral-pink for current time
    
    // Today highlight - soft purple
    val todayBackground = Color(0xFF846C92)
    val onToday = Color.White
    
    // Weekend day text color - warm gray-mauve
    val weekendText = Color(0xFF5A4B52) // Contrast: 4.9:1 on surface (AA)
}

/**
 * Access extended colors through XCalendarTheme.
 * Usage: XCalendarTheme.extendedColors.holiday
 */
val XCalendarTheme.extendedColors: XCalendarColors
    @Composable @ReadOnlyComposable
    get() = XCalendarColors

