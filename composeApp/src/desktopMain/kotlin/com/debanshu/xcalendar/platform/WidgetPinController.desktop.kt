package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberWidgetPinController(): WidgetPinController =
    object : WidgetPinController {
        override val isSupported: Boolean = false

        override fun requestTodayWidgetPin(): Boolean = false
    }
