package com.debanshu.xcalendar.ui

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule

inline fun <reified A : ComponentActivity> createIntentComposeRule(
    context: Context = ApplicationProvider.getApplicationContext(),
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> {
    val activityRule = ActivityScenarioRule<A>(Intent(context, A::class.java))
    return AndroidComposeTestRule(
        activityRule = activityRule,
        activityProvider = { rule ->
            var activity: A? = null
            rule.scenario.onActivity { launched -> activity = launched }
            checkNotNull(activity)
        },
    )
}
