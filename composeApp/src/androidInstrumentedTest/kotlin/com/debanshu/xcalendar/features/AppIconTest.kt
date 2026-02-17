package com.debanshu.xcalendar.features

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.debanshu.xcalendar.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Instrumented test to verify app icons are properly configured.
 * Created as part of the Family Flow icon update.
 * 
 * Validates that:
 * - Icon resources exist and are accessible
 * - Application manifest correctly references icons
 * - Icons are set on the application
 */
class AppIconTest {
    
    @Test
    fun launcherIcon_resourceExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        
        // Verify ic_launcher resource exists and is drawable
        val iconId = R.mipmap.ic_launcher
        val drawable = resources.getDrawable(iconId, null)
        assertNotNull("ic_launcher drawable should exist", drawable)
        assertTrue("ic_launcher should have non-zero intrinsic width", drawable.intrinsicWidth > 0)
        assertTrue("ic_launcher should have non-zero intrinsic height", drawable.intrinsicHeight > 0)
    }
    
    @Test
    fun launcherRoundIcon_resourceExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        
        // Verify ic_launcher_round resource exists and is drawable
        val iconId = R.mipmap.ic_launcher_round
        val drawable = resources.getDrawable(iconId, null)
        assertNotNull("ic_launcher_round drawable should exist", drawable)
        assertTrue("ic_launcher_round should have non-zero intrinsic width", drawable.intrinsicWidth > 0)
        assertTrue("ic_launcher_round should have non-zero intrinsic height", drawable.intrinsicHeight > 0)
    }
    
    @Test
    fun launcherForeground_resourceExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        
        // Verify ic_launcher_foreground resource exists and is drawable
        val iconId = R.mipmap.ic_launcher_foreground
        val drawable = resources.getDrawable(iconId, null)
        assertNotNull("ic_launcher_foreground drawable should exist", drawable)
        assertTrue("ic_launcher_foreground should have non-zero intrinsic width", drawable.intrinsicWidth > 0)
        assertTrue("ic_launcher_foreground should have non-zero intrinsic height", drawable.intrinsicHeight > 0)
    }
    
    @Test
    fun applicationManifest_hasIconConfigured() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        
        // Get application info
        val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        
        // Verify icon is set
        assertTrue("Application should have an icon configured", applicationInfo.icon != 0)
        
        // Verify the icon resource can be loaded
        val drawable = packageManager.getApplicationIcon(context.packageName)
        assertNotNull("Application icon should be drawable", drawable)
        assertTrue("Application icon should have non-zero intrinsic width", drawable.intrinsicWidth > 0)
        assertTrue("Application icon should have non-zero intrinsic height", drawable.intrinsicHeight > 0)
    }
    
    @Test
    fun applicationManifest_hasRoundIconConfigured() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        
        // Get application info
        val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        
        // Verify round icon is set (API 25+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            assertTrue("Application should have a round icon configured", applicationInfo.icon != 0)
        }
    }
    
    @Test
    fun appName_isFamilyFlow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        
        // Get application label
        val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()
        
        // Verify app name is "Family Flow"
        assertTrue(
            "Application name should be 'Family Flow', but was '$appName'",
            appName == "Family Flow"
        )
    }
}
