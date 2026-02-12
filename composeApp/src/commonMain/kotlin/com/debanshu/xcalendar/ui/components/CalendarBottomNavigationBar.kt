package com.debanshu.xcalendar.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.debanshu.xcalendar.common.GlassShaderParams
import com.debanshu.xcalendar.common.ReplacementColor
import com.debanshu.xcalendar.common.applyIf
import com.debanshu.xcalendar.common.createGlassRenderEffect
import com.debanshu.xcalendar.common.noRippleClickable
import com.debanshu.xcalendar.ui.navigation.NavigableScreen
import com.debanshu.xcalendar.ui.theme.LocalSharedTransitionScope
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import xcalendar.composeapp.generated.resources.Res
import xcalendar.composeapp.generated.resources.ic_add
import xcalendar.composeapp.generated.resources.ic_calendar_view_day
import xcalendar.composeapp.generated.resources.ic_calendar_view_schedule
import xcalendar.composeapp.generated.resources.ic_calendar_view_week
import xcalendar.composeapp.generated.resources.ic_description
import xcalendar.composeapp.generated.resources.ic_notifications
import kotlin.math.abs
import kotlin.math.roundToInt

// Fluid dynamics constants
private const val MAX_STRETCH = 0.4f // Maximum horizontal stretch factor (for drag)
private const val CLICK_STRETCH = 0.12f // Subtle stretch for click animations
private const val COMPRESS_RATIO = 0.25f // Ratio of vertical compression to horizontal stretch
private const val DRAG_SENSITIVITY = 30f // Lower = more sensitive deformation during drag
private const val SETTLE_DAMPING = 0.35f // Spring damping for settling (lower = more bouncy)
private const val SETTLE_STIFFNESS = 400f // Spring stiffness for settling

private val DockHorizontalPadding: Dp = 20.dp
private val DockSafeEdgeMargin: Dp = 12.dp
private val DockControlsHeight: Dp = 56.dp
private val ShortcutAnchorOffset: Dp = 72.dp
private val ShortcutSpacing: Dp = 8.dp
private const val ShortcutFabCount: Int = 3

// Reusable animation specifications
private object NavBarAnimationSpecs {
    val indicatorSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh)
    val fluidSettleSpring = spring<Float>(dampingRatio = SETTLE_DAMPING, stiffness = SETTLE_STIFFNESS)
    val fluidOvershootSpring = spring<Float>(dampingRatio = 0.3f, stiffness = 280f)
    val fluidClickSpring = spring<Float>(dampingRatio = 0.4f, stiffness = 600f)
    val clickTransition = tween<Float>(800, easing = FastOutSlowInEasing)
    val fluidStartTween = tween<Float>(100)
}

private sealed interface AnimationPhase {
    data object Idle : AnimationPhase

    data object Dragging : AnimationPhase

    data object ClickAnimating : AnimationPhase

    data object MorphingOut : AnimationPhase

    val showGlassEffect: Boolean get() = this != Idle
}

@Stable
private class NavBarState(
    val indicatorOffset: Animatable<Float, AnimationVector1D>,
    val indicatorScale: Animatable<Float, AnimationVector1D>,
    val boxScale: Animatable<Float, AnimationVector1D>,
    val phase: MutableState<AnimationPhase>,
    val itemMetrics: SnapshotStateMap<Int, ItemMetrics>,
    val dragOffset: MutableState<Float>,
    val dragStartIndex: MutableState<Int>,
    val indicatorWidthPx: MutableState<Float>,
    val indicatorInitialized: MutableState<Boolean>,
    // Fluid deformation: 0 = neutral, positive = horizontal stretch, negative = vertical stretch
    val fluidDeformation: Animatable<Float, AnimationVector1D>,
) {
    // Derived stretch values from single deformation factor
    val glassStretchX: Float get() = 1f + fluidDeformation.value
    val glassStretchY: Float get() = 1f - fluidDeformation.value * COMPRESS_RATIO

    val dragBounds: Pair<Float, Float>
        get() =
            if (itemMetrics.isNotEmpty()) {
                val firstLeft = itemMetrics[0]?.left ?: 0f
                val lastLeft = itemMetrics[itemMetrics.size - 1]?.left ?: 0f
                firstLeft to lastLeft
            } else {
                0f to 0f
            }

    val itemCenters: Map<Int, Float>
        get() = itemMetrics.mapValues { (_, m) -> m.left + m.width / 2f }

    fun findNearestIndex(indicatorCenter: Float): Int =
        itemCenters.minByOrNull { (_, center) -> abs(center - indicatorCenter) }?.key
            ?: dragStartIndex.value
}

@Composable
private fun rememberNavBarState(): NavBarState =
    remember {
        NavBarState(
            indicatorOffset = Animatable(0f),
            indicatorScale = Animatable(1f),
            boxScale = Animatable(1f),
            phase = mutableStateOf(AnimationPhase.Idle),
            itemMetrics = mutableStateMapOf(),
            dragOffset = mutableStateOf(0f),
            dragStartIndex = mutableStateOf(-1),
            indicatorWidthPx = mutableStateOf(0f),
            indicatorInitialized = mutableStateOf(false),
            fluidDeformation = Animatable(0f),
        )
    }

private data class GlassEffectInput(
    val navBarSize: IntSize,
    val state: NavBarState,
    val onSurfaceColor: Color,
    val primaryColor: Color,
)

@Composable
private fun rememberGlassEffect(input: GlassEffectInput): RenderEffect? {
    val (navBarSize, state, onSurfaceColor, primaryColor) = input
    val phase = state.phase.value
    val indicatorOffset = state.indicatorOffset.value
    val dragOffset = state.dragOffset.value
    val indicatorWidthPx = state.indicatorWidthPx.value
    val indicatorScale = state.indicatorScale.value
    val glassStretchX = state.glassStretchX
    val glassStretchY = state.glassStretchY

    return remember(
        navBarSize,
        phase,
        indicatorOffset,
        dragOffset,
        indicatorWidthPx,
        indicatorScale,
        glassStretchX,
        glassStretchY,
        onSurfaceColor,
        primaryColor,
    ) {
        if (!phase.showGlassEffect || navBarSize.width <= 0 || indicatorWidthPx <= 0f) return@remember null

        val effectOffset = if (phase is AnimationPhase.Dragging) dragOffset else indicatorOffset
        val centerX = (effectOffset + indicatorWidthPx / 2f) / navBarSize.width
        val glassWidth = (indicatorWidthPx / navBarSize.width) * 1.1f * indicatorScale * glassStretchX
        val glassHeight = indicatorScale * glassStretchY
        val cornerRadius = glassHeight * 0.5f

        createGlassRenderEffect(
            width = navBarSize.width.toFloat(),
            height = navBarSize.height.toFloat(),
            params =
                GlassShaderParams.waterDroplet().copy(
                    width = glassWidth,
                    height = glassHeight,
                    centerX = centerX,
                    centerY = 0.5f,
                    cornerRadius = cornerRadius,
                    thickness = 0.1f,
                    bevelWidth = 0.25f,
                    sourceColor = ReplacementColor(onSurfaceColor.red, onSurfaceColor.green, onSurfaceColor.blue),
                    targetColor = ReplacementColor(primaryColor.red, primaryColor.green, primaryColor.blue),
                    colorTolerance = 0.5f,
                ),
        )
    }
}

private fun Modifier.indicatorDragGesture(
    state: NavBarState,
    selectedIndex: Int,
    navItems: List<NavItem>,
    coroutineScope: CoroutineScope,
    onViewSelect: (NavigableScreen) -> Unit,
): Modifier =
    pointerInput(selectedIndex) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.phase.value = AnimationPhase.Dragging
                state.dragStartIndex.value = selectedIndex
                state.dragOffset.value = state.indicatorOffset.value
            },
            onDrag = { _, dragAmount ->
                val (minBound, maxBound) = state.dragBounds
                state.dragOffset.value = (state.dragOffset.value + dragAmount.x).coerceIn(minBound, maxBound)

                // Simple deformation based on drag magnitude
                val deformation = (dragAmount.x / DRAG_SENSITIVITY).coerceIn(-1f, 1f) * MAX_STRETCH
                coroutineScope.launch {
                    state.fluidDeformation.snapTo(deformation)
                }
            },
            onDragEnd = {
                coroutineScope.launch {
                    val indicatorCenter = state.dragOffset.value + (state.indicatorWidthPx.value / 2f)
                    val nearestIndex = state.findNearestIndex(indicatorCenter)
                    val targetPos = state.itemMetrics[nearestIndex]?.left ?: state.dragOffset.value

                    // Overshoot effect - reverse deformation briefly
                    val currentDeformation = state.fluidDeformation.value
                    launch {
                        state.fluidDeformation.animateTo(
                            targetValue = -currentDeformation * 0.5f,
                            animationSpec = NavBarAnimationSpecs.fluidOvershootSpring,
                        )
                        state.fluidDeformation.animateTo(0f, NavBarAnimationSpecs.fluidSettleSpring)
                    }

                    // Animate indicator to target position
                    state.indicatorOffset.animateTo(targetPos, NavBarAnimationSpecs.indicatorSpring)
                    state.phase.value = AnimationPhase.MorphingOut

                    if (nearestIndex != state.dragStartIndex.value) {
                        onViewSelect(navItems[nearestIndex].screen)
                    }
                }
            },
        )
    }

private suspend fun NavBarState.animateClickTransition(targetIndex: Int) {
    val targetPos = itemMetrics[targetIndex]?.left ?: indicatorOffset.value

    phase.value = AnimationPhase.ClickAnimating

    // Run all phases concurrently for snappy feel
    kotlinx.coroutines.coroutineScope {
        // Position animation
        launch {
            indicatorOffset.animateTo(targetPos, NavBarAnimationSpecs.clickTransition)
        }

        // Fluid deformation: quick stretch -> subtle overshoot -> settle
        launch {
            // Phase 1: Quick stretch
            fluidDeformation.animateTo(CLICK_STRETCH, NavBarAnimationSpecs.fluidStartTween)
            // Phase 2: Subtle overshoot
            fluidDeformation.animateTo(-CLICK_STRETCH * 0.3f, NavBarAnimationSpecs.fluidClickSpring)
            // Phase 3: Settle
            fluidDeformation.animateTo(0f, NavBarAnimationSpecs.fluidSettleSpring)
        }
    }

    phase.value = AnimationPhase.MorphingOut
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun CalendarBottomNavigationBar(
    modifier: Modifier = Modifier,
    selectedView: NavigableScreen,
    onViewSelect: (NavigableScreen) -> Unit,
    onAddClick: () -> Unit,
    onAddTaskShortcut: (() -> Unit)? = null,
    onAddEventShortcut: (() -> Unit)? = null,
    onAddVoiceShortcut: (() -> Unit)? = null,
    dockPosition: DockPositionFractions = DockPositionFractions(),
    onDockPositionChange: (DockPositionFractions) -> Unit = {},
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val coroutineScope = rememberCoroutineScope()
    val state = rememberNavBarState()
    val currentOnViewSelect by rememberUpdatedState(onViewSelect)
    val reducedMotionEnabled = XCalendarTheme.reducedMotionEnabled
    val currentOnAddClick by rememberUpdatedState(onAddClick)
    val currentOnAddTaskShortcut by rememberUpdatedState(onAddTaskShortcut)
    val currentOnAddEventShortcut by rememberUpdatedState(onAddEventShortcut)
    val currentOnAddVoiceShortcut by rememberUpdatedState(onAddVoiceShortcut)
    val currentOnDockPositionChange by rememberUpdatedState(onDockPositionChange)
    var showAddShortcuts by remember { mutableStateOf(false) }

    val navItems =
        remember {
            listOf(
                NavItem(NavigableScreen.Today, Res.drawable.ic_calendar_view_day, "Today"),
                NavItem(NavigableScreen.Week, Res.drawable.ic_calendar_view_week, "Week"),
                NavItem(NavigableScreen.Plan, Res.drawable.ic_description, "Plan"),
                NavItem(NavigableScreen.People, Res.drawable.ic_calendar_view_schedule, "People"),
                NavItem(NavigableScreen.Settings, Res.drawable.ic_notifications, "Settings"),
            )
        }

    val selectedIndex by remember(selectedView) {
        derivedStateOf { navItems.indexOfFirst { it.screen == selectedView } }
    }

    // Effect 1: Sync indicator position with selection
    LaunchedEffect(selectedIndex, state.itemMetrics.size) {
        if (state.phase.value is AnimationPhase.Dragging ||
            state.phase.value is AnimationPhase.ClickAnimating ||
            selectedIndex < 0
        ) {
            return@LaunchedEffect
        }
        val metrics = state.itemMetrics[selectedIndex] ?: return@LaunchedEffect

        state.indicatorWidthPx.value = metrics.width
        val targetPos = metrics.left

        if (reducedMotionEnabled) {
            state.indicatorOffset.snapTo(targetPos)
            state.indicatorInitialized.value = true
        } else if (state.indicatorInitialized.value && abs(state.indicatorOffset.value - targetPos) >= 0.5f) {
            state.indicatorOffset.animateTo(targetPos, NavBarAnimationSpecs.indicatorSpring)
        } else {
            state.indicatorOffset.snapTo(targetPos)
            state.indicatorInitialized.value = true
        }
    }

    // Effect 2: Handle scale animations based on phase
    LaunchedEffect(state.phase.value) {
        if (reducedMotionEnabled) {
            state.indicatorScale.snapTo(1f)
            state.boxScale.snapTo(1f)
            state.phase.value = AnimationPhase.Idle
            return@LaunchedEffect
        }
        when (val phase = state.phase.value) {
            AnimationPhase.Dragging, AnimationPhase.ClickAnimating -> {
                launch { state.indicatorScale.animateTo(1.25f, NavBarAnimationSpecs.indicatorSpring) }
                launch { state.boxScale.animateTo(1.05f, NavBarAnimationSpecs.indicatorSpring) }
            }

            AnimationPhase.MorphingOut, AnimationPhase.Idle -> {
                launch { state.indicatorScale.animateTo(1f, NavBarAnimationSpecs.indicatorSpring) }
                launch { state.boxScale.animateTo(1f, NavBarAnimationSpecs.indicatorSpring) }
                if (phase == AnimationPhase.MorphingOut) {
                    state.phase.value = AnimationPhase.Idle
                }
            }
        }
    }

    with(sharedTransitionScope) {
        BoxWithConstraints(
            modifier =
                modifier
                    .fillMaxSize()
                    .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f),
        ) {
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val safeInsets = WindowInsets.safeDrawing

            val parentWidthPx = with(density) { maxWidth.toPx() }
            val parentHeightPx = with(density) { maxHeight.toPx() }
            val dockWidth = (maxWidth - (DockHorizontalPadding * 2)).coerceAtLeast(0.dp)
            val dockWidthPx = with(density) { dockWidth.toPx() }
            val dockHeightPx = with(density) { DockControlsHeight.toPx() }
            val safeLeftInsetPx = safeInsets.getLeft(density, layoutDirection).toFloat()
            val safeTopInsetPx = safeInsets.getTop(density).toFloat()
            val safeRightInsetPx = safeInsets.getRight(density, layoutDirection).toFloat()
            val safeBottomInsetPx = safeInsets.getBottom(density).toFloat()
            val dockEdgeMarginPx = with(density) { DockSafeEdgeMargin.toPx() }

            val layoutConfig =
                remember(
                    parentWidthPx,
                    parentHeightPx,
                    dockWidthPx,
                    dockHeightPx,
                    safeLeftInsetPx,
                    safeTopInsetPx,
                    safeRightInsetPx,
                    safeBottomInsetPx,
                    dockEdgeMarginPx,
                ) {
                    DockLayoutConfig(
                        parentWidthPx = parentWidthPx,
                        parentHeightPx = parentHeightPx,
                        dockWidthPx = dockWidthPx,
                        dockHeightPx = dockHeightPx,
                        safeLeftInsetPx = safeLeftInsetPx,
                        safeTopInsetPx = safeTopInsetPx,
                        safeRightInsetPx = safeRightInsetPx,
                        safeBottomInsetPx = safeBottomInsetPx,
                        edgeMarginPx = dockEdgeMarginPx,
                    )
                }

            var dockOffsetPx by remember {
                mutableStateOf(
                    DockPositioning.fractionsToOffset(
                        fractions = dockPosition,
                        config = layoutConfig,
                    ),
                )
            }

            LaunchedEffect(layoutConfig, dockPosition) {
                dockOffsetPx =
                    DockPositioning.fractionsToOffset(
                        fractions = dockPosition,
                        config = layoutConfig,
                    )
            }

            val shortcutHeightPx =
                with(density) {
                    (DockControlsHeight * ShortcutFabCount + ShortcutSpacing * (ShortcutFabCount - 1)).toPx()
                }
            val shortcutAnchorOffsetPx = with(density) { ShortcutAnchorOffset.toPx() }
            val shortcutDirection =
                remember(dockOffsetPx, layoutConfig, shortcutHeightPx, shortcutAnchorOffsetPx) {
                    DockPositioning.chooseShortcutExpansionDirection(
                        offset = dockOffsetPx,
                        config = layoutConfig,
                        shortcutsHeightPx = shortcutHeightPx,
                        shortcutAnchorOffsetPx = shortcutAnchorOffsetPx,
                    )
                }

            Row(
                modifier =
                    Modifier
                        .width(dockWidth)
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                dockOffsetPx.x.roundToInt(),
                                dockOffsetPx.y.roundToInt(),
                            )
                        }.pointerInput(layoutConfig) {
                            detectDragGestures(
                                onDragStart = {
                                    showAddShortcuts = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val nextOffset =
                                        DockPositioning.clampOffset(
                                            offset =
                                                DockOffsetPx(
                                                    x = dockOffsetPx.x + dragAmount.x,
                                                    y = dockOffsetPx.y + dragAmount.y,
                                                ),
                                            config = layoutConfig,
                                        )
                                    dockOffsetPx = nextOffset
                                    currentOnDockPositionChange(
                                        DockPositioning.offsetToFractions(
                                            offset = nextOffset,
                                            config = layoutConfig,
                                        ),
                                    )
                                },
                                onDragEnd = {
                                    val snappedOffset =
                                        DockPositioning.snapToNearestHorizontalEdge(
                                            offset = dockOffsetPx,
                                            config = layoutConfig,
                                        )
                                    dockOffsetPx = snappedOffset
                                    currentOnDockPositionChange(
                                        DockPositioning.offsetToFractions(
                                            offset = snappedOffset,
                                            config = layoutConfig,
                                        ),
                                    )
                                },
                                onDragCancel = {
                                    val snappedOffset =
                                        DockPositioning.snapToNearestHorizontalEdge(
                                            offset = dockOffsetPx,
                                            config = layoutConfig,
                                        )
                                    dockOffsetPx = snappedOffset
                                    currentOnDockPositionChange(
                                        DockPositioning.offsetToFractions(
                                            offset = snappedOffset,
                                            config = layoutConfig,
                                        ),
                                    )
                                },
                            )
                        },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                var navBarSize by remember { mutableStateOf(IntSize.Zero) }

                val glassEffect =
                    rememberGlassEffect(
                        GlassEffectInput(
                            navBarSize = navBarSize,
                            state = state,
                            onSurfaceColor = XCalendarTheme.colorScheme.onSurface,
                            primaryColor = XCalendarTheme.colorScheme.primary,
                        ),
                    )

                Box(
                    modifier =
                        Modifier
                            .height(DockControlsHeight)
                            .weight(1f)
                            .onSizeChanged { navBarSize = it }
                            .graphicsLayer {
                                renderEffect = glassEffect
                                scaleX = state.boxScale.value
                                scaleY = state.boxScale.value
                            },
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(30.dp))
                                .background(XCalendarTheme.colorScheme.surfaceContainer)
                                .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        navItems.forEachIndexed { index, navItem ->
                            BottomNavItem(
                                modifier =
                                    Modifier.onGloballyPositioned { coordinates ->
                                        state.itemMetrics[index] =
                                            ItemMetrics(
                                                left = coordinates.positionInParent().x,
                                                width = coordinates.size.width.toFloat(),
                                            )
                                        if (state.indicatorWidthPx.value == 0f || index == selectedIndex) {
                                            state.indicatorWidthPx.value = coordinates.size.width.toFloat()
                                        }
                                    },
                                showBackground = selectedIndex == index && state.phase.value == AnimationPhase.Idle,
                                onClick = {
                                    if (index != selectedIndex && state.phase.value == AnimationPhase.Idle) {
                                        // Fire selection immediately for responsive feel
                                        currentOnViewSelect(navItem.screen)
                                        if (!reducedMotionEnabled) {
                                            coroutineScope.launch {
                                                state.animateClickTransition(index)
                                            }
                                        }
                                    }
                                },
                                icon = navItem.icon,
                                label = navItem.label,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier.height(DockControlsHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showAddShortcuts) {
                        Column(
                            modifier =
                                if (shortcutDirection == ShortcutExpansionDirection.UP) {
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -ShortcutAnchorOffset)
                                } else {
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = ShortcutAnchorOffset)
                                },
                            verticalArrangement = Arrangement.spacedBy(ShortcutSpacing),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AddShortcutFab(
                                label = "Task",
                                contentDescription = "Quick add task",
                                onClick = {
                                    showAddShortcuts = false
                                    (currentOnAddTaskShortcut ?: currentOnAddClick).invoke()
                                },
                            )
                            AddShortcutFab(
                                label = "Event",
                                contentDescription = "Quick add event",
                                onClick = {
                                    showAddShortcuts = false
                                    (currentOnAddEventShortcut ?: currentOnAddClick).invoke()
                                },
                            )
                            AddShortcutFab(
                                label = "Voice",
                                contentDescription = "Quick voice capture",
                                onClick = {
                                    showAddShortcuts = false
                                    (currentOnAddVoiceShortcut ?: currentOnAddClick).invoke()
                                },
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            showAddShortcuts = false
                            currentOnAddClick()
                        },
                        modifier =
                            Modifier
                                .size(56.dp)
                                .combinedClickable(
                                    onClick = {
                                        showAddShortcuts = false
                                        currentOnAddClick()
                                    },
                                    onLongClick = { showAddShortcuts = !showAddShortcuts },
                                ).semantics {
                                    contentDescription =
                                        "Quick add task, event, and voice shortcuts."
                                },
                        shape = CircleShape,
                        containerColor = XCalendarTheme.colorScheme.primary,
                        contentColor = XCalendarTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

private data class NavItem(
    val screen: NavigableScreen,
    val icon: DrawableResource,
    val label: String,
)

private data class ItemMetrics(
    val left: Float,
    val width: Float,
)

@Composable
private fun AddShortcutFab(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(56.dp)
                .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        containerColor = XCalendarTheme.colorScheme.secondaryContainer,
        contentColor = XCalendarTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = label,
            style = XCalendarTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    modifier: Modifier = Modifier,
    showBackground: Boolean,
    onClick: () -> Unit,
    icon: DrawableResource,
    label: String,
) {
    val tintColor = if (showBackground) XCalendarTheme.colorScheme.primary else XCalendarTheme.colorScheme.onSurface

    Column(
        modifier =
            modifier
                .noRippleClickable(onClick = onClick)
                .weight(1f)
                .fillMaxHeight()
                .applyIf(showBackground) {
                    background(XCalendarTheme.colorScheme.secondaryContainer, RoundedCornerShape(30.dp))
                        .padding(horizontal = 3.dp)
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = tintColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = XCalendarTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = tintColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
