package com.debanshu.xcalendar.domain.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.debanshu.xcalendar.widget.TodayWidget
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

@Single(binds = [WidgetUpdater::class])
class AndroidWidgetUpdater : WidgetUpdater {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }

    override suspend fun refreshTodayWidget() {
        val manager = GlanceAppWidgetManager(context)
        val widget = TodayWidget()
        val ids = manager.getGlanceIds(TodayWidget::class.java)
        ids.forEach { widget.update(context, it) }
    }
}
