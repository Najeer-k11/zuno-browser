package com.zuno.browser.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/** How a dock icon reacts when tapped. Give each destination a different one. */
enum class TapMotion { FLIP, WIGGLE, HALF_TURN, FULL_TURN }

data class DockDestination(val key: String, val label: String, val icon: ImageVector, val motion: TapMotion)

val DOCK_HEIGHT = 72.dp
/** Bottom padding every scrolling screen must reserve so the last item clears the floating dock. */
val DOCK_RESERVE = DOCK_HEIGHT + 20.dp + 12.dp

/**
 * The floating glass navigation dock: a frosted pill with a sliding white (dark: #24303B) circular
 * indicator, plus an optional detached round "+" action. Place it with
 * `Modifier.align(BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 20.dp)`
 * over full-bleed content (no NavigationBar, no Scaffold bottomBar).
 *
 * Accessibility: each icon carries its label as contentDescription. For 4–5 destinations widen
 * [dockWidth] (≈ 68dp per item).
 */
@Composable
fun ExpressiveDock(
    destinations: List<DockDestination>,
    selected: DockDestination,
    onSelect: (DockDestination) -> Unit,
    modifier: Modifier = Modifier,
    showAdd: Boolean = true,
    addLabel: String = "Add",
    onAdd: () -> Unit = {},
    dockWidth: androidx.compose.ui.unit.Dp = 68.dp * destinations.size,
) {
    val dark = isAppDark()
    val blue = MaterialTheme.colorScheme.primary
    val dockTint = if (dark) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .88f)
    else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .84f)
    val dockShape = RoundedCornerShape(DOCK_HEIGHT / 2)
    val addAlpha by animateFloatAsState(if (showAdd) 1f else 0f, tween(180), label = "addAlpha")
    val addScale by animateFloatAsState(if (showAdd) 1f else .78f, spring(dampingRatio = .9f, stiffness = 620f), label = "addScale")

    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .width(dockWidth)
                .height(DOCK_HEIGHT)
                .luminousElasticClickable(shape = dockShape, maxStretchPercent = 0.06f, maxTranslateDp = 1.8.dp)
                .glass(dockShape, dockTint, .88f, shadowElevation = 18.dp)
                .blueGlow(dockShape),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val itemWidth = maxWidth / destinations.size
                val indicator = 50.dp
                val x by animateDpAsState(
                    itemWidth * destinations.indexOf(selected).coerceAtLeast(0) + (itemWidth - indicator) / 2,
                    spring(dampingRatio = 0.84f, stiffness = 600f),
                    label = "dockIndicator",
                )
                Box(
                    Modifier.offset(x = x, y = (DOCK_HEIGHT - indicator) / 2).size(indicator).clip(CircleShape)
                        .background(if (dark) Color(0xFF24303B) else Color.White.copy(alpha = .99f)),
                )
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    destinations.forEach { d ->
                        DockItem(d, d == selected, if (dark) Color.White.copy(alpha = .94f) else blue) { onSelect(d) }
                    }
                }
            }
        }
        if (showAdd || addAlpha > 0f) {
            Box(
                Modifier
                    .size(DOCK_HEIGHT)
                    .graphicsLayer { alpha = addAlpha; scaleX = addScale; scaleY = addScale }
                    .luminousElasticClickable(enabled = showAdd, shape = CircleShape, maxStretchPercent = 0.08f, maxTranslateDp = 2.dp, onClick = onAdd)
                    .clip(CircleShape)
                    .glass(
                        CircleShape,
                        if (dark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f),
                        .92f,
                        shadowElevation = 18.dp,
                    )
                    .blueGlow(CircleShape)
                    .clearAndSetSemantics { contentDescription = addLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(31.dp))
            }
        }
    }
}

@Composable
private fun RowScope.DockItem(d: DockDestination, selected: Boolean, iconColor: Color, onClick: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    val rot = remember { Animatable(0f) }
    val scale by animateFloatAsState(if (selected) 1f else .94f, tween(180), label = "dockScale")
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        rot.snapTo(0f)
        when (d.motion) {
            TapMotion.FLIP, TapMotion.FULL_TURN -> rot.animateTo(360f, tween(420))
            TapMotion.HALF_TURN -> rot.animateTo(180f, tween(320))
            TapMotion.WIGGLE -> {
                rot.animateTo(-6f, tween(65)); rot.animateTo(6f, tween(85))
                rot.animateTo(-3f, tween(65)); rot.animateTo(0f, tween(65))
            }
        }
        rot.snapTo(0f)
    }
    Box(
        Modifier.weight(1f).height(DOCK_HEIGHT).clickable(remember { MutableInteractionSource() }, null) { taps++; onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            d.icon, d.label,
            tint = if (selected) MaterialTheme.colorScheme.onSurface else iconColor,
            modifier = Modifier.size(27.dp).graphicsLayer {
                scaleX = scale; scaleY = scale
                if (d.motion == TapMotion.FLIP) rotationY = rot.value else rotationZ = rot.value
                cameraDistance = 18f * density
            },
        )
    }
}
