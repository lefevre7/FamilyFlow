package com.debanshu.xcalendar.android

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class ManifestReminderPermissionsTest {
    @Test
    fun manifest_declaresReminderPermissions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val requestedPermissions = readRequestedPermissions(context)

        assertTrue(requestedPermissions.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertTrue(requestedPermissions.contains("android.permission.VIBRATE"))
        assertTrue(requestedPermissions.contains("android.permission.WAKE_LOCK"))
    }

    @Suppress("DEPRECATION")
    private fun readRequestedPermissions(context: Context): Set<String> {
        val packageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
                )
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_PERMISSIONS,
                )
            }
        return packageInfo.requestedPermissions?.toSet().orEmpty()
    }
}
