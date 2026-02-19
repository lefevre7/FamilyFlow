package com.debanshu.xcalendar.features

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.debanshu.xcalendar.R
import com.debanshu.xcalendar.widget.TodayWidget
import com.debanshu.xcalendar.widget.TodayWidgetReceiver
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Instrumented tests verifying the TodayWidget content and configuration.
 *
 * Validates:
 * - "Family Flow" app name constant is correct (source-of-truth for the overline label)
 * - Widget receiver is registered in the AndroidManifest
 * - Widget provider XML resource is accessible
 * - Companion constants are correct and stable
 * - Receiver instantiation returns a TodayWidget glance widget
 */
class WidgetContentTest {

    @Test
    fun widgetAppName_isFamilyFlow() {
        assertEquals(
            "Widget APP_NAME constant must be 'Family Flow' — this drives the overline label",
            "Family Flow",
            TodayWidget.APP_NAME,
        )
    }

    @Test
    fun widgetAppName_matchesAppLabel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appLabel = context.applicationInfo.loadLabel(context.packageManager).toString()
        assertEquals(
            "Widget APP_NAME should match the application label",
            appLabel,
            TodayWidget.APP_NAME,
        )
    }

    @Test
    fun todayWidget_destinationConstant_isCorrect() {
        assertEquals(
            "DESTINATION_TODAY constant should be 'today'",
            "today",
            TodayWidget.DESTINATION_TODAY,
        )
    }

    @Test
    fun todayWidget_extraKeys_areNonEmpty() {
        assertTrue(
            "EXTRA_QUICK_ADD_MODE should be a non-empty string",
            TodayWidget.EXTRA_QUICK_ADD_MODE.isNotEmpty(),
        )
        assertTrue(
            "EXTRA_NAVIGATE_TO should be a non-empty string",
            TodayWidget.EXTRA_NAVIGATE_TO.isNotEmpty(),
        )
    }

    @Test
    fun todayWidgetReceiver_glanceAppWidget_isTodayWidget() {
        val receiver = TodayWidgetReceiver()
        assertNotNull(
            "TodayWidgetReceiver.glanceAppWidget should not be null",
            receiver.glanceAppWidget,
        )
        assertTrue(
            "TodayWidgetReceiver.glanceAppWidget should be an instance of TodayWidget",
            receiver.glanceAppWidget is TodayWidget,
        )
    }

    @Test
    fun widgetReceiver_isRegisteredInManifest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_RECEIVERS,
        )
        val receivers = packageInfo.receivers ?: emptyArray()
        val receiverNames = receivers.map { it.name }
        assertTrue(
            "TodayWidgetReceiver must be declared in AndroidManifest.xml",
            receiverNames.any { it.contains("TodayWidgetReceiver") },
        )
    }

    @Test
    fun widgetInfoXml_resourceExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // R.xml.today_widget_info must resolve to a valid resource
        val xmlId = R.xml.today_widget_info
        assertTrue("today_widget_info XML resource ID must be non-zero", xmlId != 0)

        // Opening the parser must not throw
        val parser = context.resources.getXml(xmlId)
        assertNotNull("today_widget_info XML parser should be non-null", parser)
        parser.close()
    }

    @Test
    fun widgetInfoXml_containsAppWidgetProviderTag() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val parser = context.resources.getXml(R.xml.today_widget_info)

        var foundProvider = false
        var eventType = parser.eventType
        while (eventType != android.content.res.XmlResourceParser.END_DOCUMENT) {
            if (eventType == android.content.res.XmlResourceParser.START_TAG &&
                parser.name == "appwidget-provider"
            ) {
                foundProvider = true
                break
            }
            eventType = parser.next()
        }
        parser.close()

        assertTrue(
            "today_widget_info.xml must contain an <appwidget-provider> tag",
            foundProvider,
        )
    }

    @Test
    fun widgetInfoXml_minHeightAttributeIsPresent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val parser = context.resources.getXml(R.xml.today_widget_info)

        var minHeightPresent = false
        val androidNs = "http://schemas.android.com/apk/res/android"
        var eventType = parser.eventType
        while (eventType != android.content.res.XmlResourceParser.END_DOCUMENT) {
            if (eventType == android.content.res.XmlResourceParser.START_TAG &&
                parser.name == "appwidget-provider"
            ) {
                val value = parser.getAttributeValue(androidNs, "minHeight")
                minHeightPresent = value != null
                break
            }
            eventType = parser.next()
        }
        parser.close()

        assertTrue(
            "today_widget_info.xml appwidget-provider must declare a minHeight attribute",
            minHeightPresent,
        )
    }

    @Test
    fun widgetBgLight_hasCorrectAlpha() {
        // 0x80 = 128; 128/255 ≈ 0.502, rounds to 128
        val alpha = (TodayWidget.WIDGET_BG_LIGHT.alpha * 255f).roundToInt()
        assertEquals(
            "Light bubble background should be 50% transparent (alpha 128/255)",
            128,
            alpha,
        )
    }

    @Test
    fun widgetBgDark_hasCorrectAlpha() {
        val alpha = (TodayWidget.WIDGET_BG_DARK.alpha * 255f).roundToInt()
        assertEquals(
            "Dark bubble background should be 50% transparent (alpha 128/255)",
            128,
            alpha,
        )
    }

    @Test
    fun widgetCornerRadius_is16dp() {
        assertEquals(
            "Widget corner radius should be 16dp — matching the app card rounding pattern",
            16,
            TodayWidget.WIDGET_CORNER_RADIUS,
        )
    }
}
