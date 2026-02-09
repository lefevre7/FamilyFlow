package com.debanshu.xcalendar.platform

actual object PlatformFeatures : PlatformFeatureAvailability {
    private const val reason = "Not supported on this platform yet."
    override val ocr = FeatureSupport(false, reason)
    override val voiceCapture = FeatureSupport(false, reason)
    override val localLlm = FeatureSupport(false, reason)
    override val calendarOAuth = FeatureSupport(false, reason)
    override val widgets = FeatureSupport(false, reason)
    override val notifications = FeatureSupport(false, reason)
}
