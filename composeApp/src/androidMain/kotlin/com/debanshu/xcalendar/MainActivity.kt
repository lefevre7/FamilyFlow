package com.debanshu.xcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.debanshu.xcalendar.ui.components.dialog.QuickAddMode
import com.debanshu.xcalendar.widget.TodayWidget
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val quickAddRequests = MutableStateFlow<QuickAddMode?>(null)
    private val navigateRequests = MutableStateFlow<Boolean?>(null)
    private val onboardingPrefs by lazy {
        getSharedPreferences(ONBOARDING_PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        val hasCompletedOnboarding = onboardingPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        setContent {
            CalendarApp(
                quickAddRequests = quickAddRequests,
                onQuickAddHandled = { quickAddRequests.value = null },
                navigateRequests = navigateRequests,
                onNavigateHandled = { navigateRequests.value = null },
                showOnboardingInitially = !hasCompletedOnboarding,
                onOnboardingCompleted = {
                    onboardingPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
                },
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent ?: return
        val mode = intent.getStringExtra(TodayWidget.EXTRA_QUICK_ADD_MODE)
        if (mode != null) {
            quickAddRequests.value = runCatching { QuickAddMode.valueOf(mode) }.getOrNull()
        }
        val destination = intent.getStringExtra(TodayWidget.EXTRA_NAVIGATE_TO)
        if (destination == TodayWidget.DESTINATION_TODAY) {
            navigateRequests.value = true
        }
    }

    private companion object {
        const val ONBOARDING_PREFS_NAME = "onboarding_prefs"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
