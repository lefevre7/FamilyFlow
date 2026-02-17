package com.debanshu.xcalendar.platform

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of system accessibility detection.
 */
class AndroidSystemAccessibility(private val context: Context) : SystemAccessibility {
    @Composable
    override fun isHighContrastEnabled(): Boolean {
        // Check Android's high text contrast setting
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "high_text_contrast_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
}

actual fun getSystemAccessibility(): SystemAccessibility {
    // This will be called from a @Composable context, so we can't get Context here
    // Instead, we'll use a different pattern - return a composable-friendly implementation
    return object : SystemAccessibility {
        @Composable
        override fun isHighContrastEnabled(): Boolean {
            val context = LocalContext.current
            return try {
                Settings.Secure.getInt(
                    context.contentResolver,
                    "high_text_contrast_enabled",
                    0
                ) == 1
            } catch (e: Exception) {
                false
            }
        }
    }
}
