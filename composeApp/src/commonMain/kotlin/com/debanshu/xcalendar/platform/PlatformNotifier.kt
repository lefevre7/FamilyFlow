package com.debanshu.xcalendar.platform

interface PlatformNotifier {
    fun showToast(message: String)

    fun shareText(subject: String, text: String)
}

expect class PlatformNotifierImpl() : PlatformNotifier
