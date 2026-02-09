package com.debanshu.xcalendar.platform

data class FeatureSupport(
    val supported: Boolean,
    val reason: String? = null
)

interface PlatformFeatureAvailability {
    val ocr: FeatureSupport
    val voiceCapture: FeatureSupport
    val localLlm: FeatureSupport
    val calendarOAuth: FeatureSupport
    val widgets: FeatureSupport
    val notifications: FeatureSupport
}

expect object PlatformFeatures : PlatformFeatureAvailability
