package com.debanshu.xcalendar.di

import org.koin.dsl.module

/**
 * Test DI module for instrumented tests.
 * Provides mock implementations of external dependencies.
 */
object TestInstrumentedModule {
    val notifier = FakeNotifier()
    
    val module = module {
        // Test module placeholder
        // Add mock implementations as needed
        single { notifier }
    }
}

// Fake notifier for testing share and notification actions
class FakeNotifier {
    val notifications = mutableListOf<Pair<String, String>>()
    
    fun notify(title: String, message: String) {
        notifications.add(title to message)
    }
    
    fun share(text: String) {
        notifications.add("Share" to text)
    }
    
    fun clear() {
        notifications.clear()
    }
}
