package com.zuno.browser.ui.browser

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zuno.browser.browser.Tab
import com.zuno.browser.ui.elasticClickable
import com.zuno.browser.ui.glass
import com.zuno.browser.ui.isAppDark
import com.zuno.browser.ui.luminousElasticClickable
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Slim browser bar: kept small so the page keeps the screen. */
val BAR_HEIGHT = 52.dp
/** Gap between the bar and the navigation bar / screen edge. */
val BAR_GAP = 6.dp
/** Bottom space content must clear while the bar is shown. */
val BAR_RESERVE = BAR_HEIGHT + BAR_GAP

/**
 * The browser's floating dock. Same anatomy as ExpressiveDock (72dp frosted pill + detached 72dp
 * "+"), with browser content: tab-sidebar button, the address pill and the menu.
 * Swiping the address pill left/right switches to the neighbouring tab, Zen-style.
 */
@Composable
fun BrowserDock(
    tab: Tab?,
    tabCount: Int,
    shieldOn: Boolean,
    onTabs: () -> Unit,
    onAddress: () -> Unit,
    onShield: () -> Unit,
    onMenu: () -> Unit,
    onNewTab: () -> Unit,
    onSwipeTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isAppDark()
    val cs = MaterialTheme.colorScheme
    val dockTint = if (dark) cs.surfaceContainer.copy(alpha = .88f) else cs.surfaceContainerLow.copy(alpha = .84f)
    val dockShape = RoundedCornerShape(BAR_HEIGHT / 2)
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }

    Row(modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .glass(dockShape, dockTint, .88f, shadowElevation = 10.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabsButton(tabCount, onTabs)
            Spacer(Modifier.width(4.dp))
            // Address pill: tap to edit, swipe to switch tabs.
            Row(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .graphicsLayer { translationX = drag.value * .35f; alpha = 1f - (abs(drag.value) / (threshold * 3)).coerceAtMost(.5f) }
                    .clip(RoundedCornerShape(20.dp))
                    .pointerInput(tab?.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val v = drag.value
                                if (abs(v) > threshold) onSwipeTab(if (v < 0) 1 else -1)
                                scope.launch { drag.animateTo(0f, spring(dampingRatio = .55f, stiffness = 420f)) }
                            },
                            onDragCancel = { scope.launch { drag.animateTo(0f, spring(dampingRatio = .55f, stiffness = 420f)) } },
                        ) { change, amount ->
                            change.consume()
                            scope.launch { drag.snapTo(drag.value + amount) }
                        }
                    }
                    .luminousElasticClickable(shape = RoundedCornerShape(20.dp), maxStretchPercent = .04f, onClick = onAddress)
                    .padding(start = 4.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShieldChip(tab, shieldOn, onShield)
                Spacer(Modifier.width(8.dp))
                if (tab != null && !tab.isBlank) {
                    if (tab.private) {
                        Icon(com.zuno.browser.ui.ZunoIcons.Incognito, "Private", tint = cs.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                }
                AnimatedContent(
                    targetState = tab?.takeIf { !it.isBlank }?.host?.ifBlank { tab.url } ?: "",
                    transitionSpec = {
                        (slideInHorizontally(spring(dampingRatio = .8f, stiffness = 400f)) { it / 3 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally(spring(dampingRatio = .8f, stiffness = 400f)) { -it / 3 } + fadeOut(tween(150)))
                    },
                    label = "host",
                    modifier = Modifier.weight(1f),
                ) { host ->
                    Text(
                        host.ifBlank { "Search or enter address" },
                        style = if (host.isBlank()) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmallEmphasized,
                        color = if (host.isBlank()) cs.onSurfaceVariant else cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).elasticClickable(onClick = onMenu).semantics { contentDescription = "Menu" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.MoreHoriz, null, tint = if (dark) cs.onSurface else cs.primary, modifier = Modifier.size(24.dp)) }
        }
        // Detached "+" exactly like ExpressiveDock's add button.
        Box(
            Modifier
                .size(BAR_HEIGHT)
                .luminousElasticClickable(shape = CircleShape, maxStretchPercent = 0.08f, maxTranslateDp = 2.dp, onClick = onNewTab)
                .clip(CircleShape)
                .glass(CircleShape, if (dark) cs.primaryContainer.copy(alpha = .72f) else cs.primaryContainer.copy(alpha = .58f), .92f, shadowElevation = 10.dp)
                .semantics { contentDescription = "New tab" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(26.dp)) }
    }
}

/** Tab-count indicator: a Cookie4Sided that wiggles when the count changes. */
@Composable
private fun TabsButton(count: Int, onClick: () -> Unit) {
    val dark = isAppDark()
    val cs = MaterialTheme.colorScheme
    val rot = remember { Animatable(0f) }
    var last by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count != last) {
            last = count
            rot.snapTo(-40f)
            rot.animateTo(0f, spring(dampingRatio = .45f, stiffness = 380f))
        }
    }
    Box(
        Modifier.size(40.dp).clip(CircleShape).elasticClickable(onClick = onClick).semantics { contentDescription = "Tabs ($count)" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).graphicsLayer { rotationZ = rot.value }
                .clip(MaterialShapes.Cookie4Sided.toShape())
                .background(if (dark) androidx.compose.ui.graphics.Color(0xFF24303B) else androidx.compose.ui.graphics.Color.White.copy(alpha = .99f)),
        )
        Text(
            if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = cs.onSurface,
        )
    }
}

/** Zuno Shield badge: shield icon + live per-page blocked count. */
@Composable
private fun ShieldChip(tab: Tab?, shieldOn: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val count = tab?.totalBlocked ?: 0
    val (bg, fg) = if (shieldOn) chipColors() else cs.surfaceContainerHighest to cs.onSurfaceVariant
    Row(
        Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(bg)
            .elasticClickable(onClick = onClick).padding(horizontal = 9.dp)
            .semantics { contentDescription = if (shieldOn) "Zuno Shield: $count blocked" else "Zuno Shield off for this site" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Shield, null, tint = fg, modifier = Modifier.size(16.dp))
        AnimatedVisibility(
            visible = shieldOn && count > 0,
            enter = scaleIn(spring(dampingRatio = .5f, stiffness = 500f)) + fadeIn(tween(150)),
            exit = scaleOut(tween(120)) + fadeOut(tween(120)),
        ) {
            AnimatedContent(count, transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(90)) }, label = "count") { c ->
                Text(
                    if (c > 999) "999+" else c.toString(),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = fg,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}

/** Find-in-page bar that replaces the dock while searching. */
@Composable
fun FindBar(
    query: String,
    onQuery: (String) -> Unit,
    current: Int,
    total: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isAppDark()
    val cs = MaterialTheme.colorScheme
    val dockTint = if (dark) cs.surfaceContainer.copy(alpha = .88f) else cs.surfaceContainerLow.copy(alpha = .84f)
    val shape = RoundedCornerShape(BAR_HEIGHT / 2)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        modifier.fillMaxWidth().padding(horizontal = 10.dp).height(BAR_HEIGHT)
            .glass(shape, dockTint, .88f, shadowElevation = 10.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (bg, fg) = chipColors()
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Search, null, tint = fg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Find in page", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant.copy(alpha = .6f))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = cs.onSurface),
                cursorBrush = SolidColor(cs.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (query.isNotEmpty()) {
            Text(if (total == 0) "0" else "$current/$total", style = MaterialTheme.typography.labelLargeEmphasized, color = cs.primary)
            Spacer(Modifier.width(4.dp))
        }
        SmallRound(Icons.Rounded.KeyboardArrowUp, "Previous", onPrev)
        SmallRound(Icons.Rounded.KeyboardArrowDown, "Next", onNext)
        SmallRound(Icons.Rounded.Close, "Close", onClose)
    }
}

@Composable
private fun SmallRound(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).elasticClickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface) }
}
