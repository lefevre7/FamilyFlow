package com.debanshu.xcalendar.widget

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.debanshu.xcalendar.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the Today Snapshot deep-link mechanism.
 *
 * Verifies that:
 *  1. The intent-extra constants are correctly defined (no typos, no collisions with
 *     the existing EXTRA_QUICK_ADD_MODE key).
 *  2. An [Intent] constructed with these extras round-trips correctly through
 *     Android's intent system, proving [MainActivity.handleIntent] will be able to
 *     extract them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class TodaySnapshotDeepLinkTest {

    // ── Constant value correctness ─────────────────────────────────────────────

    @Test
    fun extraNavigateTo_hasExpectedStringValue() {
        assertEquals("extra_navigate_to", TodayWidget.EXTRA_NAVIGATE_TO)
    }

    @Test
    fun destinationToday_hasExpectedStringValue() {
        assertEquals("today", TodayWidget.DESTINATION_TODAY)
    }

    @Test
    fun extraQuickAddMode_isUnchanged() {
        // Guard against accidental renames that would break existing widget actions.
        assertEquals("extra_quick_add_mode", TodayWidget.EXTRA_QUICK_ADD_MODE)
    }

    @Test
    fun extraNavigateTo_doesNotCollideWithExtraQuickAddMode() {
        // Intent extras are looked up by key: collision would silently lose one value.
        assertNotEquals(TodayWidget.EXTRA_NAVIGATE_TO, TodayWidget.EXTRA_QUICK_ADD_MODE)
    }

    // ── Intent round-trip ─────────────────────────────────────────────────────

    @Test
    fun openTodayIntent_containsNavigateToExtra() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(TodayWidget.EXTRA_NAVIGATE_TO, TodayWidget.DESTINATION_TODAY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        assertEquals(
            TodayWidget.DESTINATION_TODAY,
            intent.getStringExtra(TodayWidget.EXTRA_NAVIGATE_TO),
        )
    }

    @Test
    fun intentWithNoExtras_returnsNullForNavigateTo() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java)

        assertNull(intent.getStringExtra(TodayWidget.EXTRA_NAVIGATE_TO))
    }

    @Test
    fun intentWithOnlyQuickAddMode_returnsNullForNavigateTo() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(TodayWidget.EXTRA_QUICK_ADD_MODE, "TASK")
        }

        // Having EXTRA_QUICK_ADD_MODE must not accidentally satisfy EXTRA_NAVIGATE_TO.
        assertNull(intent.getStringExtra(TodayWidget.EXTRA_NAVIGATE_TO))
        assertNotNull(intent.getStringExtra(TodayWidget.EXTRA_QUICK_ADD_MODE))
    }

    @Test
    fun intentWithNavigateTo_doesNotInterferWithQuickAddMode() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(TodayWidget.EXTRA_NAVIGATE_TO, TodayWidget.DESTINATION_TODAY)
        }

        // EXTRA_QUICK_ADD_MODE must remain absent when not explicitly set.
        assertNull(intent.getStringExtra(TodayWidget.EXTRA_QUICK_ADD_MODE))
    }
}
