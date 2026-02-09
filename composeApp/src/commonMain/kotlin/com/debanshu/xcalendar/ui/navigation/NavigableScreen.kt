package com.debanshu.xcalendar.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface NavigableScreen : NavKey {
    @Serializable
    data object Today : NavigableScreen

    @Serializable
    data object Week : NavigableScreen

    @Serializable
    data object Plan : NavigableScreen

    @Serializable
    data object People : NavigableScreen

    @Serializable
    data object Settings : NavigableScreen
}
