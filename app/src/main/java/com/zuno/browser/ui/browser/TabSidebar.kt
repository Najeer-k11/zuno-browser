package com.zuno.browser.ui.browser

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zuno.browser.browser.BrowserStore
import com.zuno.browser.browser.Tab
import com.zuno.browser.browser.Workspace
import com.zuno.browser.ui.RoundTonalButton
import com.zuno.browser.ui.ZunoIcons
import com.zuno.browser.ui.elasticClickable
import com.zuno.browser.ui.groupContainer
import com.zuno.browser.ui.isAppDark
import com.zuno.browser.ui.luminousElasticClickable
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Zen-style vertical tab sidebar:
 *  - Essentials: a global grid of favourite sites, shared by every workspace.
 *  - Workspaces: swipe the tab list sideways (or tap the switcher) to change workspace.
 *  - Per workspace: pinned tabs, a separator with "Clear", then today's tabs.
 */
@Composable
fun TabSidebar(
    store: BrowserStore,
    onSelect: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onTabActions: (Tab) -> Unit,
    onAddWorkspace: () -> Unit,
    onEditWorkspace: (Workspace) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onPrivateChipAt: (androidx.compose.ui.geometry.Offset) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val workspaces = store.workspaces
    val activeIndex = workspaces.indexOfFirst { it.id == store.activeWorkspaceId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = activeIndex) { workspaces.size }

    // Pager -> store (swipe changes workspace) and store -> pager (switcher taps).
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { page ->
            // Only follow the pager in normal mode; opening the sidebar must not leave private mode.
            if (!store.privateMode) workspaces.getOrNull(page)?.let { store.switchWorkspace(it.id) }
        }
    }
    LaunchedEffect(activeIndex) {
        if (pager.currentPage != activeIndex) pager.animateScrollToPage(activeIndex)
    }

    Column(modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.statusBars)) {
        // Normal <-> private: the private panel rises in from below, the workspace view sinks back.
        AnimatedContent(
            targetState = store.privateMode,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                val rise = spring<IntOffset>(dampingRatio = .82f, stiffness = 380f)
                if (targetState) {
                    (slideInVertically(rise) { it / 6 } + fadeIn(tween(260)) + scaleIn(spring(dampingRatio = .8f, stiffness = 380f), initialScale = .94f))
                        .togetherWith(fadeOut(tween(180)) + scaleOut(tween(220), targetScale = .96f))
                } else {
                    (fadeIn(tween(260)) + scaleIn(spring(dampingRatio = .8f, stiffness = 380f), initialScale = .96f))
                        .togetherWith(slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(200)))
                }
            },
            label = "privateSwap",
        ) { isPrivate ->
        if (isPrivate) {
            PrivatePanel(store, onSelect, onNewTab, onTabActions, onSettings, Modifier.fillMaxSize())
        } else Column(Modifier.fillMaxSize()) {
            // Header: workspace badge + name, settings gear.
            val ws = workspaces.getOrNull(pager.currentPage)
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(cs.primaryContainer)
                        .elasticClickable { ws?.let(onEditWorkspace) },
                    contentAlignment = Alignment.Center,
                ) { Icon(ZunoIcons.get(ws?.icon ?: ZunoIcons.DEFAULT), null, tint = cs.onPrimaryContainer, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(ws?.name ?: "", style = MaterialTheme.typography.headlineMediumEmphasized, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val n = ws?.let { store.tabsIn(it.id).size } ?: 0
                    Text(if (n == 1) "1 tab" else "$n tabs", style = MaterialTheme.typography.titleSmallEmphasized, color = cs.primary)
                }
                RoundTonalButton(Icons.Outlined.Settings, "Settings", onSettings)
            }

            Essentials(store, onSelect, onTabActions)

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                key = { workspaces.getOrNull(it)?.id ?: it },
                beyondViewportPageCount = 1,
            ) { page ->
                val w = workspaces.getOrNull(page) ?: return@HorizontalPager
                WorkspaceTabs(store, w, onSelect, onNewTab, onTabActions)
            }
        }
        }

        WorkspaceSwitcher(
            workspaces = workspaces,
            selected = pager.currentPage,
            onSelect = { store.switchWorkspace(workspaces[it].id) },
            onLongPress = { onEditWorkspace(workspaces[it]) },
            onAdd = onAddWorkspace,
            privateActive = store.privateMode,
            privateCount = store.privateTabs.size,
            onPrivate = { store.switchPrivateMode(!store.privateMode) },
            onPrivateChipAt = onPrivateChipAt,
        )
    }
}

/** Private mode's sidebar: its own header, tab list and "close all", no workspaces or Essentials. */
@Composable
private fun PrivatePanel(
    store: BrowserStore,
    onSelect: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onTabActions: (Tab) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val tabs = store.privateTabs
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(cs.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(com.zuno.browser.ui.ZunoIcons.Incognito, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Private", style = MaterialTheme.typography.headlineMediumEmphasized, color = cs.onSurface)
                Text(
                    (if (tabs.size == 1) "1 tab" else "${tabs.size} tabs") + " · nothing is saved",
                    style = MaterialTheme.typography.titleSmallEmphasized, color = cs.tertiary,
                )
            }
            RoundTonalButton(Icons.Outlined.Settings, "Settings", onSettings)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "new-private") {
                val shape = RoundedCornerShape(20.dp)
                Row(
                    Modifier.fillMaxWidth().height(54.dp).clip(shape).luminousElasticClickable(shape = shape, maxStretchPercent = .03f, onClick = onNewTab)
                        .padding(horizontal = 10.dp).animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, null, tint = cs.tertiary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("New private tab", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
                }
            }
            items(tabs, key = { it.id }) { tab -> TabRow(store, tab, onSelect, onTabActions, Modifier.animateItem()) }
            if (tabs.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Pages you open here leave no history, and their cookies and site data are deleted when the tabs close.",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).animateItem(),
                    )
                }
            }
        }
        if (tabs.isNotEmpty()) {
            val shape = RoundedCornerShape(22.dp)
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().height(48.dp).clip(shape).background(cs.errorContainer)
                    .luminousElasticClickable(shape = shape, maxStretchPercent = .03f) { store.closeAllPrivate() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Close, null, tint = cs.onErrorContainer, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Close all private tabs", style = MaterialTheme.typography.labelLargeEmphasized, color = cs.onErrorContainer)
            }
        }
    }
}

@Composable
private fun Essentials(store: BrowserStore, onSelect: (Tab) -> Unit, onTabActions: (Tab) -> Unit) {
    val essentials = store.essentials
    if (essentials.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        val cols = 4
        val gap = 10.dp
        val cell = (maxWidth - gap * (cols - 1)) / cols
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            essentials.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { tab ->
                        val selected = tab.id == store.selectedTabId
                        val shape = RoundedCornerShape(22.dp)
                        val bg by animateColorAsState(if (selected) cs.primaryContainer else groupContainer(), tween(220), label = "ess")
                        Box(
                            Modifier.width(cell).height(60.dp).clip(shape).background(bg)
                                .border(if (selected) 2.dp else 1.25.dp, if (selected) cs.primary else cs.outlineVariant.copy(alpha = .35f), shape)
                                .luminousElasticClickable(shape = shape, onClick = { onSelect(tab) }, onLongClick = { onTabActions(tab) })
                                .semantics { contentDescription = tab.displayTitle },
                            contentAlignment = Alignment.Center,
                        ) {
                            SiteBadge(tab.host.ifBlank { tab.displayTitle }, 34.dp, selected = selected)
                            if (tab.loaded && !selected) {
                                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(5.dp).clip(CircleShape).background(cs.primary))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceTabs(
    store: BrowserStore,
    ws: Workspace,
    onSelect: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onTabActions: (Tab) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val all = store.tabsIn(ws.id)
    val pinned = emptyList<Tab>()   // pinned tabs live in the grid above
    val today = all
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (pinned.isNotEmpty()) {
            items(pinned, key = { it.id }) { tab -> TabRow(store, tab, onSelect, onTabActions, Modifier.animateItem()) }
        }
        item(key = "sep-${ws.id}") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).animateItem(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(cs.outlineVariant.copy(alpha = .45f)))
                if (today.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Row(
                        Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(cs.surfaceContainerHigh)
                            .elasticClickable { store.clearUnpinned(ws.id) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelMediumEmphasized, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
        item(key = "new-${ws.id}") {
            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier.fillMaxWidth().height(54.dp).clip(shape).luminousElasticClickable(shape = shape, maxStretchPercent = .03f, onClick = onNewTab)
                    .padding(horizontal = 10.dp).animateItem(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, null, tint = cs.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("New tab", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
            }
        }
        items(today, key = { it.id }) { tab -> TabRow(store, tab, onSelect, onTabActions, Modifier.animateItem()) }
    }
}

@Composable
private fun TabRow(store: BrowserStore, tab: Tab, onSelect: (Tab) -> Unit, onTabActions: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val selected = tab.id == store.selectedTabId
    val dismiss = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismiss.currentValue) {
        if (dismiss.currentValue == SwipeToDismissBoxValue.EndToStart) store.closeTab(tab)
    }
    val shape = RoundedCornerShape(20.dp)
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            if (dismiss.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    Modifier.fillMaxSize().clip(shape).background(cs.errorContainer).padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) { Icon(Icons.Rounded.Close, null, tint = cs.onErrorContainer) }
            }
        },
    ) {
        val bg by animateColorAsState(
            when {
                selected && isAppDark() -> cs.primaryContainer.copy(alpha = .55f)
                selected -> cs.primaryContainer.copy(alpha = .75f)
                else -> cs.surfaceContainerLow.copy(alpha = 0f)
            },
            tween(220), label = "tabBg",
        )
        Row(
            Modifier.fillMaxWidth().height(58.dp).clip(shape).background(if (dismiss.dismissDirection != SwipeToDismissBoxValue.Settled) groupContainer() else bg)
                .luminousElasticClickable(shape = shape, maxStretchPercent = .03f, maxTranslateDp = 1.2.dp, onClick = { onSelect(tab) }, onLongClick = { onTabActions(tab) })
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                SiteBadge(tab.host.ifBlank { tab.displayTitle }, 36.dp, selected = selected)
                if (tab.loading) {
                    val spin by animateFloatAsState(if (tab.loading) 1f else 0f, spring(dampingRatio = .5f), label = "load")
                    Box(Modifier.align(Alignment.TopEnd).size(10.dp).graphicsLayer { scaleX = spin; scaleY = spin }.clip(MaterialShapes.Cookie4Sided.toShape()).background(cs.tertiary))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).graphicsLayer { alpha = if (tab.loaded || selected) 1f else .72f }) {
                Text(
                    tab.displayTitle,
                    style = if (selected) MaterialTheme.typography.titleSmallEmphasized else MaterialTheme.typography.titleSmall,
                    color = if (selected) cs.onPrimaryContainer else cs.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (tab.host.isNotBlank()) {
                    Text(tab.host, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (tab.private) Icon(com.zuno.browser.ui.ZunoIcons.Incognito, "Private", tint = cs.tertiary, modifier = Modifier.padding(horizontal = 4.dp).size(16.dp))
            if (tab.pinned) {
                Icon(Icons.Rounded.PushPin, "Pinned", tint = cs.primary, modifier = Modifier.padding(horizontal = 10.dp).size(18.dp))
            } else {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).elasticClickable { store.closeTab(tab) }.semantics { contentDescription = "Close ${tab.displayTitle}" },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

/** Bottom row of workspace icons; the active one sits on a springing Cookie9Sided. */
@Composable
private fun WorkspaceSwitcher(
    workspaces: List<Workspace>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onAdd: () -> Unit,
    privateActive: Boolean,
    privateCount: Int,
    onPrivate: () -> Unit,
    onPrivateChipAt: (androidx.compose.ui.geometry.Offset) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            items(workspaces.size, key = { workspaces[it].id }) { i ->
                val isSel = i == selected && !privateActive
                val scale by animateFloatAsState(if (isSel) 1f else 0f, spring(dampingRatio = .45f, stiffness = 380f), label = "wsScale")
                val rot by animateFloatAsState(if (isSel) 0f else -60f, spring(dampingRatio = .6f, stiffness = 220f), label = "wsRot")
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .luminousElasticClickable(shape = CircleShape, onClick = { onSelect(i) }, onLongClick = { onLongPress(i) })
                        .semantics { contentDescription = "Workspace ${workspaces[i].name}" }
                        .animateItem(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(48.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rot }.clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.primary))
                    Icon(
                        ZunoIcons.get(workspaces[i].icon), null,
                        tint = if (isSel) cs.onPrimary else cs.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        // Private mode chip: a mask that springs onto a tertiary cookie when active.
        val pScale by animateFloatAsState(if (privateActive) 1f else 0f, spring(dampingRatio = .45f, stiffness = 380f), label = "pvScale")
        Box(Modifier.size(52.dp).onGloballyPositioned { c ->
            val p = c.positionInRoot(); onPrivateChipAt(androidx.compose.ui.geometry.Offset(p.x + c.size.width / 2f, p.y + c.size.height / 2f))
        }) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape).elasticClickable(onClick = onPrivate)
                    .semantics { contentDescription = if (privateActive) "Leave private mode" else "Private mode" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(48.dp).graphicsLayer { scaleX = pScale; scaleY = pScale }.clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.tertiary))
                Icon(com.zuno.browser.ui.ZunoIcons.Incognito, null, tint = if (privateActive) cs.onTertiary else cs.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
            // Count of open private tabs, outside the clipped tap area so it isn't cut off.
            if (!privateActive && privateCount > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(cs.tertiary),
                    contentAlignment = Alignment.Center,
                ) { Text("$privateCount", style = MaterialTheme.typography.labelSmallEmphasized, color = cs.onTertiary) }
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(cs.primaryContainer).elasticClickable(onClick = onAdd)
                .semantics { contentDescription = "New workspace" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = cs.onPrimaryContainer) }
    }
}
