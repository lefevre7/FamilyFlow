package com.debanshu.xcalendar.platform

import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

@Single(binds = [PlatformNotifier::class])
actual class PlatformNotifierImpl : PlatformNotifier {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun shareText(subject: String, text: String) {
        if (text.isBlank()) return
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
        val chooser =
            Intent.createChooser(shareIntent, "Share").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { context.startActivity(chooser) }
            .onFailure { showToast("Unable to open sharing apps") }
    }
}
