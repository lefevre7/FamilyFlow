package com.debanshu.xcalendar.platform

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.debanshu.xcalendar.widget.TodayWidgetReceiver

@Composable
actual fun rememberWidgetPinController(): WidgetPinController {
    val context = LocalContext.current
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    val provider = remember(context) { ComponentName(context, TodayWidgetReceiver::class.java) }
    val isSupported =
        remember(appWidgetManager) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported
        }

    return object : WidgetPinController {
        override val isSupported: Boolean = isSupported

        override fun requestTodayWidgetPin(): Boolean {
            if (!isSupported) return false
            return appWidgetManager.requestPinAppWidget(provider, null, null)
        }
    }
}
