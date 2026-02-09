package com.debanshu.xcalendar.platform

actual object PlatformFeatures : PlatformFeatureAvailability {
    override val ocr = FeatureSupport(true)
    override val voiceCapture = FeatureSupport(true)
    override val localLlm = FeatureSupport(true)
    override val calendarOAuth = FeatureSupport(true)
    override val widgets = FeatureSupport(true)
    override val notifications = FeatureSupport(true)
}
