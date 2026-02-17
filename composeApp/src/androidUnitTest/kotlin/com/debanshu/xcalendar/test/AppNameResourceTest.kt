package com.debanshu.xcalendar.test

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.debanshu.xcalendar.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Unit test to verify strings.xml contains the correct "Family Flow" app name.
 * Created as part of the ADHD MOM → Family Flow rename (Item 42).
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class AppNameResourceTest {
    
    @Test
    fun appName_inStringsXml_isFamilyFlow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        
        assertEquals(
            expected = "Family Flow",
            actual = appName,
            message = "App name in strings.xml should be 'Family Flow'"
        )
    }
    
    @Test
    fun appName_doesNotContainOldName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        
        assertFalse(
            actual = appName.contains("ADHD MOM", ignoreCase = true),
            message = "App name should not contain old 'ADHD MOM' branding"
        )
    }
}
