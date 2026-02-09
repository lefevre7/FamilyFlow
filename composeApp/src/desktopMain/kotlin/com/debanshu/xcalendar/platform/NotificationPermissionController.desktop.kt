package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController =
    object : NotificationPermissionController {
        override val isRequired: Boolean = false
        override val isGranted: Boolean = false
        override fun request() = Unit
    }
