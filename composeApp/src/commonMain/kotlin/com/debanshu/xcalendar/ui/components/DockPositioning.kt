package com.debanshu.xcalendar.ui.components

internal data class DockPositionFractions(
    val x: Float = 0.5f,
    val y: Float = 1f,
) {
    fun normalized(): DockPositionFractions =
        DockPositionFractions(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f),
        )
}

internal data class DockOffsetPx(
    val x: Float,
    val y: Float,
)

internal data class DockLayoutConfig(
    val parentWidthPx: Float,
    val parentHeightPx: Float,
    val dockWidthPx: Float,
    val dockHeightPx: Float,
    val safeLeftInsetPx: Float,
    val safeTopInsetPx: Float,
    val safeRightInsetPx: Float,
    val safeBottomInsetPx: Float,
    val edgeMarginPx: Float,
)

internal data class DockBoundsPx(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
)

internal enum class ShortcutExpansionDirection {
    UP,
    DOWN,
}

internal object DockPositioning {

    fun resolveBounds(config: DockLayoutConfig): DockBoundsPx {
        val safeLeft = config.safeLeftInsetPx
        val safeTop = config.safeTopInsetPx
        val safeRight = config.parentWidthPx - config.safeRightInsetPx
        val safeBottom = config.parentHeightPx - config.safeBottomInsetPx

        val rawMinX = safeLeft + config.edgeMarginPx
        val rawMaxX = safeRight - config.edgeMarginPx - config.dockWidthPx
        val rawMinY = safeTop + config.edgeMarginPx
        val rawMaxY = safeBottom - config.edgeMarginPx - config.dockHeightPx

        val minX: Float
        val maxX: Float
        if (rawMaxX >= rawMinX) {
            minX = rawMinX
            maxX = rawMaxX
        } else {
            val centered = (rawMinX + rawMaxX) / 2f
            minX = centered
            maxX = centered
        }

        val minY: Float
        val maxY: Float
        if (rawMaxY >= rawMinY) {
            minY = rawMinY
            maxY = rawMaxY
        } else {
            val centered = (rawMinY + rawMaxY) / 2f
            minY = centered
            maxY = centered
        }

        return DockBoundsPx(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
        )
    }

    fun clampOffset(
        offset: DockOffsetPx,
        config: DockLayoutConfig,
    ): DockOffsetPx {
        val bounds = resolveBounds(config)
        return DockOffsetPx(
            x = offset.x.coerceIn(bounds.minX, bounds.maxX),
            y = offset.y.coerceIn(bounds.minY, bounds.maxY),
        )
    }

    fun fractionsToOffset(
        fractions: DockPositionFractions,
        config: DockLayoutConfig,
    ): DockOffsetPx {
        val normalized = fractions.normalized()
        val centerX = normalized.x * config.parentWidthPx
        val centerY = normalized.y * config.parentHeightPx
        val rawOffset =
            DockOffsetPx(
                x = centerX - (config.dockWidthPx / 2f),
                y = centerY - (config.dockHeightPx / 2f),
            )
        return clampOffset(rawOffset, config)
    }

    fun offsetToFractions(
        offset: DockOffsetPx,
        config: DockLayoutConfig,
    ): DockPositionFractions {
        val clamped = clampOffset(offset, config)
        val centerX = clamped.x + (config.dockWidthPx / 2f)
        val centerY = clamped.y + (config.dockHeightPx / 2f)

        val normalizedX =
            if (config.parentWidthPx > 0f) {
                (centerX / config.parentWidthPx).coerceIn(0f, 1f)
            } else {
                0.5f
            }
        val normalizedY =
            if (config.parentHeightPx > 0f) {
                (centerY / config.parentHeightPx).coerceIn(0f, 1f)
            } else {
                1f
            }

        return DockPositionFractions(
            x = normalizedX,
            y = normalizedY,
        )
    }

    fun snapToNearestHorizontalEdge(
        offset: DockOffsetPx,
        config: DockLayoutConfig,
    ): DockOffsetPx {
        val clamped = clampOffset(offset, config)
        val bounds = resolveBounds(config)
        val snappedX =
            if ((clamped.x - bounds.minX) <= (bounds.maxX - clamped.x)) {
                bounds.minX
            } else {
                bounds.maxX
            }
        return DockOffsetPx(
            x = snappedX,
            y = clamped.y,
        )
    }

    fun chooseShortcutExpansionDirection(
        offset: DockOffsetPx,
        config: DockLayoutConfig,
        shortcutsHeightPx: Float,
        shortcutAnchorOffsetPx: Float,
    ): ShortcutExpansionDirection {
        val clamped = clampOffset(offset, config)
        val bounds = resolveBounds(config)
        val safeTop = bounds.minY
        val safeBottom = bounds.maxY + config.dockHeightPx

        val required = shortcutAnchorOffsetPx + shortcutsHeightPx
        val availableAbove = clamped.y - safeTop
        val availableBelow = safeBottom - clamped.y

        val canExpandUp = availableAbove >= required
        val canExpandDown = availableBelow >= required

        return when {
            canExpandUp && !canExpandDown -> ShortcutExpansionDirection.UP
            canExpandDown && !canExpandUp -> ShortcutExpansionDirection.DOWN
            availableAbove >= availableBelow -> ShortcutExpansionDirection.UP
            else -> ShortcutExpansionDirection.DOWN
        }
    }
}
