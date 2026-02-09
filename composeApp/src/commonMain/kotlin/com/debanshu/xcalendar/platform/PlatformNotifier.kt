package com.debanshu.xcalendar.platform

interface PlatformNotifier {
    fun showToast(message: String)
}

expect class PlatformNotifierImpl() : PlatformNotifier
