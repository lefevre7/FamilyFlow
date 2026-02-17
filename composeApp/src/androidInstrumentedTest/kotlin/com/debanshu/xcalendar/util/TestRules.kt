package com.debanshu.xcalendar.util

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.work.testing.WorkManagerTestInitHelper
import com.debanshu.xcalendar.di.TestInstrumentedModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
// LeakCanary disabled due to Kotlin stdlib packaging issues
// import leakcanary.LeakAssertions
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

/**
 * Custom Compose test rule that sets up Koin with test modules.
 * Automatically initializes WorkManager, Koin DI, and test dispatchers.
 * 
 * Usage:
 * ```
 * @get:Rule
 * val composeTestRule = createInstrumentedComposeRule<MainActivity>(testModule)
 * ```
 */
fun createInstrumentedComposeRule(
    vararg testModules: Module
): AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> {
    val rule = createAndroidComposeRule<ComponentActivity>()
    
    // Initialize test environment before each test
    val setupRule = object : TestWatcher() {
        @OptIn(ExperimentalCoroutinesApi::class)
        private val testDispatcher: TestDispatcher = StandardTestDispatcher()
        
        override fun starting(description: Description) {
            super.starting(description)
            
            // Initialize test dispatcher
            @OptIn(ExperimentalCoroutinesApi::class)
            Dispatchers.setMain(testDispatcher)
            
            // Initialize WorkManager for testing
            val context = ApplicationProvider.getApplicationContext<Context>()
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
            
            // TODO: Mark onboarding as complete to skip it in tests
            // context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
            //     .edit()
            //     .putBoolean("onboarding_completed", true)
            //     .apply()
            
            // Stop any existing Koin instance
            stopKoin()
            
            // Start Koin with test modules
            startKoin {
                androidContext(context)
                modules(*testModules)
            }
        }
        
        override fun finished(description: Description) {
            super.finished(description)
            
            // Clean up Koin
            stopKoin()
            
            // Reset main dispatcher
            @OptIn(ExperimentalCoroutinesApi::class)
            Dispatchers.resetMain()
        }
    }
    
    return rule
}

/**
 * Rule that checks for memory leaks after each test.
 * Uses LeakCanary to detect leaked objects.
 * DISABLED: LeakCanary removed due to Kotlin stdlib packaging issues in test APK.
 */
/*
class LeakDetectionRule : TestWatcher() {
    override fun finished(description: Description) {
        super.finished(description)
        // LeakAssertions.assertNoLeaks()
    }
}
*/

/**
 * Rule that provides test data and mocks for instrumented tests.
 * Extends the base test module with instrumented-test-specific setup.
 * NOTE: Currently unused - tests use createAndroidComposeRule directly.
 */
/*
class TestDataRule : TestWatcher() {
    lateinit var testModule: TestInstrumentedModule
        private set
    
    override fun starting(description: Description) {
        super.starting(description)
        testModule = TestInstrumentedModule()
    }
    
    override fun finished(description: Description) {
        super.finished(description)
    }
}
*/
