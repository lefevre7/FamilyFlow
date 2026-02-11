package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

interface WidgetPinController {
    val isSupported: Boolean
    fun requestTodayWidgetPin(): Boolean
}

@Composable
expect fun rememberWidgetPinController(): WidgetPinController
