package com.debanshu.xcalendar.platform

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    val context = LocalContext.current
    val isRequired = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    var granted by remember { mutableStateOf(!isRequired) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted = isGranted
    }

    LaunchedEffect(isRequired) {
        if (isRequired) {
            val current =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            granted = current == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    return object : NotificationPermissionController {
        override val isRequired: Boolean = isRequired
        override val isGranted: Boolean = granted

        override fun request() {
            if (isRequired) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
