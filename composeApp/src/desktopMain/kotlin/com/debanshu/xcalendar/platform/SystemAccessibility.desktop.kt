package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

/**
 * Desktop stub implementation of system accessibility detection.
 */
actual fun getSystemAccessibility(): SystemAccessibility {
    return object : SystemAccessibility {
        @Composable
        override fun isHighContrastEnabled(): Boolean = false
    }
}
