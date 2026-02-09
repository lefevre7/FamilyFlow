package com.debanshu.xcalendar.platform

import org.koin.core.annotation.Single

@Single(binds = [PlatformNotifier::class])
actual class PlatformNotifierImpl : PlatformNotifier {
    override fun showToast(message: String) = Unit
}
