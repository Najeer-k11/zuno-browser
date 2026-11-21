package com.zuno.browser.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zuno.browser.browser.BrowserStore
import com.zuno.browser.browser.Tab
import com.zuno.browser.ui.groupContainer
import com.zuno.browser.ui.luminousElasticClickable

/** What a blank tab (or an empty workspace) shows instead of web content. */
@Composable
fun StartPage(
    store: BrowserStore,
    blockedTotal: Long,
    shieldOn: Boolean,
    onSearch: () -> Unit,
    onOpen: (Tab) -> Unit,
    onTabActions: (Tab) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    if (store.privateMode) { PrivateStart(store, onSearch); return }
    val ws = store.activeWorkspace
    Column(
        Modifier.fillMaxSize().background(cs.background).verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        BrandHero()
        Spacer(Modifier.height(20.dp))
        Text("Zuno", style = MaterialTheme.typography.displaySmallEmphasized, color = cs.onBackground)
        if (ws != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(com.zuno.browser.ui.ZunoIcons.get(ws.icon), null, tint = cs.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(ws.name, style = MaterialTheme.typography.titleLargeEmphasized, color = cs.primary)
            }
        }
        Spacer(Modifier.height(28.dp))

        // Search pill.
        val pill = RoundedCornerShape(30.dp)
        Row(
            Modifier.fillMaxWidth().height(60.dp).clip(pill).background(groupContainer())
                .border(1.25.dp, cs.outlineVariant.copy(alpha = .35f), pill)
                .luminousElasticClickable(shape = pill, maxStretchPercent = .03f, onClick = onSearch)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = cs.primary)
            Spacer(Modifier.width(12.dp))
            Text("Search or type a URL", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        }

        val essentials = store.essentials
        if (essentials.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "PINNED", style = MaterialTheme.typography.labelLargeEmphasized, color = cs.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 10.dp),
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cols = 4
                val gap = 10.dp
                val cell = (maxWidth - gap * (cols - 1)) / cols
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    essentials.chunked(cols).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { t ->
                                val shape = RoundedCornerShape(24.dp)
                                Column(
                                    Modifier.width(cell).clip(shape).background(groupContainer())
                                        .luminousElasticClickable(shape = shape, onClick = { onOpen(t) }, onLongClick = { onTabActions(t) })
                                        .padding(vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    SiteBadge(t.host.ifBlank { t.displayTitle }, 44.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        t.displayTitle, style = MaterialTheme.typography.labelMediumEmphasized, color = cs.onSurface,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        // Shield stats: "not yours" content uses the tertiary family.
        val tile = RoundedCornerShape(24.dp)
        Row(
            Modifier.fillMaxWidth().clip(tile).background(if (shieldOn) cs.tertiaryContainer else cs.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(if (shieldOn) cs.tertiary else cs.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Shield, null, tint = if (shieldOn) cs.onTertiary else cs.onSurfaceVariant) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    formatCount(blockedTotal),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = if (shieldOn) cs.onTertiaryContainer else cs.onSurface,
                )
                Text(
                    if (shieldOn) "ads & trackers blocked by Zuno Shield" else "Zuno Shield is off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = (if (shieldOn) cs.onTertiaryContainer else cs.onSurfaceVariant).copy(alpha = .8f),
                )
            }
        }
        Spacer(Modifier.height(BAR_RESERVE + 24.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** Start page in private mode: what private browsing does, and a way out. */
@Composable
private fun PrivateStart(store: BrowserStore, onSearch: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(cs.background).verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        androidx.compose.foundation.layout.Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
            HeroShapes(outer = 176.dp, inner = 96.dp) {
                Icon(com.zuno.browser.ui.ZunoIcons.Incognito, null, tint = cs.onPrimary, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Private browsing", style = MaterialTheme.typography.displaySmallEmphasized, color = cs.onBackground)
        Text("Nothing here is saved", style = MaterialTheme.typography.titleLargeEmphasized, color = cs.primary)
        Spacer(Modifier.height(28.dp))
        val pill = RoundedCornerShape(30.dp)
        Row(
            Modifier.fillMaxWidth().height(60.dp).clip(pill).background(groupContainer())
                .border(1.25.dp, cs.outlineVariant.copy(alpha = .35f), pill)
                .luminousElasticClickable(shape = pill, maxStretchPercent = .03f, onClick = onSearch)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = cs.primary)
            Spacer(Modifier.width(12.dp))
            Text("Search privately", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
        val points = listOf(
            "No history, searches or form data are kept",
            "Cookies and site data are deleted when you close private tabs",
            "Zuno Shield still blocks ads and trackers",
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            points.forEachIndexed { i, text ->
                val big = 22.dp; val small = 6.dp
                val shape = RoundedCornerShape(
                    topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
                    bottomStart = if (i == points.lastIndex) big else small, bottomEnd = if (i == points.lastIndex) big else small,
                )
                Row(Modifier.fillMaxWidth().clip(shape).background(groupContainer()).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        val exit = RoundedCornerShape(26.dp)
        Row(
            Modifier.height(52.dp).clip(exit).background(cs.primaryContainer)
                .luminousElasticClickable(shape = exit, maxStretchPercent = .04f) { store.switchPrivateMode(false) }
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Leave private mode", style = MaterialTheme.typography.titleMediumEmphasized, color = cs.onPrimaryContainer)
        }
        Spacer(Modifier.height(BAR_RESERVE + 24.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** The Zuno mark (same art as the launcher icon): indigo disc, slowly turning pink Sunny, indigo Z. */
@Composable
private fun BrandHero() {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "brand")
    val spin by t.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(24_000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "spin",
    )
    val breathe by t.animateFloat(
        .97f, 1.03f,
        androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(2200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    androidx.compose.foundation.layout.Box(
        Modifier.size(176.dp).graphicsLayer { scaleX = breathe; scaleY = breathe }
            .clip(androidx.compose.foundation.shape.CircleShape).background(com.zuno.browser.ui.onboarding.BrandIndigo),
        contentAlignment = Alignment.Center,
    ) { com.zuno.browser.ui.onboarding.ZunoMark(124.dp, spin = spin) }
}
