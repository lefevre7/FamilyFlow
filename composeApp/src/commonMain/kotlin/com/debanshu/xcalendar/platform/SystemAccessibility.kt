package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

/**
 * System-level accessibility preferences detection.
 * Used to auto-enable high-contrast mode based on OS accessibility settings.
 */
interface SystemAccessibility {
    /**
     * Returns true if the system has high-contrast mode enabled in accessibility settings.
     * Platform-specific implementation checks OS-level preferences.
     */
    @Composable
    fun isHighContrastEnabled(): Boolean
}

/**
 * Platform-specific implementation of system accessibility detection.
 */
expect fun getSystemAccessibility(): SystemAccessibility
