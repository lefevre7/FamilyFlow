package com.debanshu.xcalendar.platform

import android.content.Context
import android.widget.Toast
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

@Single(binds = [PlatformNotifier::class])
actual class PlatformNotifierImpl : PlatformNotifier {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
