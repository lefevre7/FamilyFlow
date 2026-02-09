package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

interface NotificationPermissionController {
    val isRequired: Boolean
    val isGranted: Boolean
    fun request()
}

@Composable
expect fun rememberNotificationPermissionController(): NotificationPermissionController
