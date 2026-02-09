package com.debanshu.xcalendar.domain.widgets

import org.koin.core.annotation.Single

@Single(binds = [WidgetUpdater::class])
class NoOpWidgetUpdater : WidgetUpdater {
    override suspend fun refreshTodayWidget() = Unit
}
