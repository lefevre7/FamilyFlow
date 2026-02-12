package com.debanshu.xcalendar.ui.components

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DockPositioningTest {

    @Test
    fun fractionsToOffset_clampsToSafeBounds() {
        val config =
            DockLayoutConfig(
                parentWidthPx = 1080f,
                parentHeightPx = 1920f,
                dockWidthPx = 1040f,
                dockHeightPx = 56f,
                safeLeftInsetPx = 0f,
                safeTopInsetPx = 0f,
                safeRightInsetPx = 0f,
                safeBottomInsetPx = 100f,
                edgeMarginPx = 12f,
            )

        val offset =
            DockPositioning.fractionsToOffset(
                fractions = DockPositionFractions(x = 0.5f, y = 1f),
                config = config,
            )

        val bounds = DockPositioning.resolveBounds(config)
        assertEquals(20f, offset.x)
        assertEquals(bounds.maxY, offset.y)
    }

    @Test
    fun snapToNearestHorizontalEdge_snapsToClosestSide() {
        val config =
            DockLayoutConfig(
                parentWidthPx = 500f,
                parentHeightPx = 1000f,
                dockWidthPx = 300f,
                dockHeightPx = 56f,
                safeLeftInsetPx = 0f,
                safeTopInsetPx = 0f,
                safeRightInsetPx = 0f,
                safeBottomInsetPx = 0f,
                edgeMarginPx = 12f,
            )
        val bounds = DockPositioning.resolveBounds(config)

        val snappedLeft =
            DockPositioning.snapToNearestHorizontalEdge(
                offset = DockOffsetPx(bounds.minX + 10f, 100f),
                config = config,
            )
        val snappedRight =
            DockPositioning.snapToNearestHorizontalEdge(
                offset = DockOffsetPx(bounds.maxX - 5f, 100f),
                config = config,
            )

        assertEquals(bounds.minX, snappedLeft.x)
        assertEquals(bounds.maxX, snappedRight.x)
    }

    @Test
    fun normalizedFractions_reprojectAcrossSizeChanges() {
        val portrait =
            DockLayoutConfig(
                parentWidthPx = 1080f,
                parentHeightPx = 1920f,
                dockWidthPx = 1040f,
                dockHeightPx = 56f,
                safeLeftInsetPx = 0f,
                safeTopInsetPx = 0f,
                safeRightInsetPx = 0f,
                safeBottomInsetPx = 120f,
                edgeMarginPx = 12f,
            )
        val landscape =
            DockLayoutConfig(
                parentWidthPx = 1920f,
                parentHeightPx = 1080f,
                dockWidthPx = 1880f,
                dockHeightPx = 56f,
                safeLeftInsetPx = 0f,
                safeTopInsetPx = 0f,
                safeRightInsetPx = 0f,
                safeBottomInsetPx = 0f,
                edgeMarginPx = 12f,
            )

        val portraitOffset =
            DockPositioning.fractionsToOffset(
                fractions = DockPositionFractions(x = 0.5f, y = 0.33f),
                config = portrait,
            )
        val storedFractions = DockPositioning.offsetToFractions(portraitOffset, portrait)
        val landscapeOffset = DockPositioning.fractionsToOffset(storedFractions, landscape)
        val reprojectedFractions = DockPositioning.offsetToFractions(landscapeOffset, landscape)

        assertTrue(abs(storedFractions.x - reprojectedFractions.x) < 0.01f)
        assertTrue(abs(storedFractions.y - reprojectedFractions.y) < 0.01f)
    }

    @Test
    fun shortcutExpansionDirection_prefersAvailableSpace() {
        val config =
            DockLayoutConfig(
                parentWidthPx = 1080f,
                parentHeightPx = 1920f,
                dockWidthPx = 1040f,
                dockHeightPx = 56f,
                safeLeftInsetPx = 0f,
                safeTopInsetPx = 0f,
                safeRightInsetPx = 0f,
                safeBottomInsetPx = 0f,
                edgeMarginPx = 12f,
            )
        val shortcutsHeight = 184f
        val shortcutAnchorOffset = 72f

        val directionNearBottom =
            DockPositioning.chooseShortcutExpansionDirection(
                offset = DockOffsetPx(20f, 1700f),
                config = config,
                shortcutsHeightPx = shortcutsHeight,
                shortcutAnchorOffsetPx = shortcutAnchorOffset,
            )
        val directionNearTop =
            DockPositioning.chooseShortcutExpansionDirection(
                offset = DockOffsetPx(20f, 40f),
                config = config,
                shortcutsHeightPx = shortcutsHeight,
                shortcutAnchorOffsetPx = shortcutAnchorOffset,
            )

        assertEquals(ShortcutExpansionDirection.UP, directionNearBottom)
        assertEquals(ShortcutExpansionDirection.DOWN, directionNearTop)
    }
}
