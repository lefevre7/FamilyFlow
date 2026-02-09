package com.debanshu.xcalendar.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Extended color scheme for XCalendar-specific colors.
 * These colors supplement Material3's ColorScheme for domain-specific needs.
 */
object XCalendarColors {
    // Holiday colors
    val holiday = Color(0xFF3F7E73)
    val holidayContainer = Color(0xFF3F7E73).copy(alpha = 0.12f)
    val onHoliday = Color.White
    
    // Schedule holiday (green variant)
    val scheduleHoliday = Color(0xFF7FAF8C)
    val scheduleHolidayContainer = Color(0xFF7FAF8C).copy(alpha = 0.16f)
    
    // Event indicator colors for month view
    val eventDot = Color(0xFF6B8FBF)
    
    // Calendar grid colors
    val gridLine = Color(0xFFE3DED6)
    val currentTimeLine = Color(0xFFC96A5D) // Softer coral for current time
    
    // Today highlight
    val todayBackground = Color(0xFF4C7D73)
    val onToday = Color.White
    
    // Weekend day text color
    val weekendText = Color(0xFF6A6560).copy(alpha = 0.7f)
}

/**
 * Access extended colors through XCalendarTheme.
 * Usage: XCalendarTheme.extendedColors.holiday
 */
val XCalendarTheme.extendedColors: XCalendarColors
    @Composable @ReadOnlyComposable
    get() = XCalendarColors

