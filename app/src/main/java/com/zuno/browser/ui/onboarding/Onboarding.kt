package com.zuno.browser.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Https
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zuno.browser.data.BrowserSettings
import com.zuno.browser.data.SearchEngine
import com.zuno.browser.ui.PalettePickerRow
import com.zuno.browser.ui.elasticClickable
import com.zuno.browser.ui.groupContainer
import com.zuno.browser.ui.isAppDark
import com.zuno.browser.ui.luminousElasticClickable
import com.zuno.browser.ui.theme.DarkMode
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class Feature(val icon: ImageVector, val text: String)
private data class Page(val title: String, val subtitle: String, val features: List<Feature>, val art: @Composable () -> Unit)

private val PAGES = listOf(
    Page(
        "Zuno Browser",
        "A fast, private, expressive browser built on Mozilla's Gecko engine. No Chromium inside.",
        listOf(Feature(Icons.Outlined.Lock, "Independent Gecko engine"), Feature(Icons.Outlined.Star, "Material Expressive design")),
    ) { WelcomeArt() },
    Page(
        "Zuno Shield",
        "Ads, trackers and cookie pop-ups are blocked before they load. Pause it per site anytime.",
        listOf(
            Feature(Icons.Outlined.Shield, "Ad and tracker blocking"),
            Feature(Icons.Outlined.Cookie, "Auto-reject cookie banners"),
            Feature(Icons.Outlined.Https, "HTTPS-only and privacy signals"),
        ),
    ) { ShieldArt() },
    Page(
        "Tabs, the Zen way",
        "Vertical tabs in a sidebar, pinned sites in a quick grid, and spaces for each part of your life.",
        listOf(
            Feature(Icons.Outlined.Layers, "Workspaces, swipe to switch"),
            Feature(Icons.Outlined.PushPin, "Pinned tabs in every workspace"),
            Feature(Icons.Outlined.Swipe, "Swipe the address bar to change tabs"),
        ),
    ) { TabsArt() },
    Page(
        "Keep the music going",
        "YouTube, YouTube Music and other sites keep playing when you leave Zuno, with lock screen controls.",
        listOf(
            Feature(Icons.Outlined.Headphones, "Background play"),
            Feature(Icons.Outlined.Notifications, "Media controls in notifications"),
        ),
    ) { PlayArt() },
)
private const val PAGE_COUNT = 5   // 4 feature pages + "make it yours"

@Composable
fun Onboarding(settings: BrowserSettings, onUpdate: ((BrowserSettings) -> BrowserSettings) -> Unit, onDone: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val pager = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    val finish = {
        if (Build.VERSION.SDK_INT >= 33 && settings.backgroundPlay) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else onDone()
    }
    val last = pager.currentPage == PAGE_COUNT - 1

    Column(
        Modifier.fillMaxSize().background(cs.background)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Top bar: step label + Skip
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${pager.currentPage + 1} of $PAGE_COUNT", style = MaterialTheme.typography.labelLargeEmphasized, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
            if (!last) {
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainerHigh)
                        .elasticClickable { scope.launch { pager.animateScrollToPage(PAGE_COUNT - 1) } }.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Skip", style = MaterialTheme.typography.labelLargeEmphasized, color = cs.onSurface) }
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val offset = (pager.currentPage - page + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - offset * .6f; val s = 1f - offset * .08f; scaleX = s; scaleY = s }) {
                if (page < PAGES.size) FeaturePage(PAGES[page], pager, page) else SetupPage(settings, onUpdate)
            }
        }

        // Bottom: indicator + next / start
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, bottom = 18.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            PageDots(pager, Modifier.weight(1f))
            val w by animateDpAsState(if (last) 196.dp else 64.dp, spring(dampingRatio = .7f, stiffness = 420f), label = "ctaW")
            val shape = RoundedCornerShape(32.dp)
            Box(
                Modifier.height(64.dp).width(w).clip(shape).background(cs.primary)
                    .luminousElasticClickable(shape = shape, maxStretchPercent = .05f) {
                        if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                    .semantics { contentDescription = if (last) "Start browsing" else "Next" },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(last, transitionSpec = { (fadeIn(tween(200)) + scaleIn(spring(dampingRatio = .6f))) togetherWith fadeOut(tween(120)) }, label = "cta") { isLast ->
                    if (isLast) Text("Start browsing", style = MaterialTheme.typography.titleMediumEmphasized, color = cs.onPrimary, maxLines = 1)
                    else Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = cs.onPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun FeaturePage(page: Page, pager: PagerState, index: Int) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { page.art() }
        Spacer(Modifier.height(18.dp))
        Text(page.title, style = MaterialTheme.typography.displaySmallEmphasized, color = cs.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(page.subtitle, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            page.features.forEachIndexed { i, f ->
                // features rise in one after another when the page settles
                val shown = pager.settledPage == index
                val y by animateDpAsState(if (shown) 0.dp else 18.dp, spring(dampingRatio = .7f, stiffness = 300f - i * 40f), label = "fy")
                val big = 22.dp; val small = 6.dp
                val shape = RoundedCornerShape(
                    topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
                    bottomStart = if (i == page.features.lastIndex) big else small, bottomEnd = if (i == page.features.lastIndex) big else small,
                )
                Row(
                    Modifier.fillMaxWidth().graphicsLayer { translationY = y.toPx(); alpha = if (shown) 1f else .4f }
                        .clip(shape).background(groupContainer()).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(f.icon, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(f.text, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Last page: a live overview of the main settings, bound to the real values. */
@Composable
private fun SetupPage(settings: BrowserSettings, onUpdate: ((BrowserSettings) -> BrowserSettings) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZunoMark(56.dp, sunny = cs.primaryContainer, z = cs.onPrimaryContainer)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Make it yours", style = MaterialTheme.typography.headlineMediumEmphasized, color = cs.onBackground)
                Text("Change any of this later in Settings", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(18.dp))

        SectionLabel("Theme")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            DarkMode.entries.forEachIndexed { i, m ->
                Segment(m.label, m == settings.theme.darkMode, i, DarkMode.entries.size, Modifier.weight(1f)) {
                    onUpdate { s -> s.copy(theme = s.theme.copy(darkMode = m)) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(groupContainer())) {
            PalettePickerRow(
                dark = isAppDark(), dynamic = settings.theme.dynamicColor, selected = settings.theme.palette,
                onDynamic = { onUpdate { it.copy(theme = it.theme.copy(dynamicColor = true)) } },
                onSelect = { p -> onUpdate { it.copy(theme = it.theme.copy(dynamicColor = false, palette = p)) } },
            )
        }

        SectionLabel("Search with")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchEngine.entries.forEach { e ->
                val on = e == settings.searchEngine
                val bg by animateColorAsState(if (on) cs.primary else cs.surfaceContainerHigh, tween(200), label = "eng")
                Row(
                    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(bg)
                        .elasticClickable { onUpdate { it.copy(searchEngine = e) } }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (on) { Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
                    Text(e.label, style = MaterialTheme.typography.labelLargeEmphasized, color = if (on) cs.onPrimary else cs.onSurface)
                }
            }
        }

        SectionLabel("Basics")
        val rows = listOf(
            Triple(Icons.Outlined.Shield, "Block ads and trackers", settings.adBlock) to { v: Boolean -> onUpdate { it.copy(adBlock = v) } },
            Triple(Icons.Outlined.Cookie, "Reject cookie banners", settings.cookieBanners) to { v: Boolean -> onUpdate { it.copy(cookieBanners = v) } },
            Triple(Icons.Outlined.Headphones, "Background play", settings.backgroundPlay) to { v: Boolean -> onUpdate { it.copy(backgroundPlay = v) } },
            Triple(Icons.Outlined.UnfoldLess, "Compact mode", settings.compactMode) to { v: Boolean -> onUpdate { it.copy(compactMode = v) } },
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            rows.forEachIndexed { i, (r, set) ->
                val (icon, label, on) = r
                val big = 22.dp; val small = 6.dp
                val shape = RoundedCornerShape(
                    topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
                    bottomStart = if (i == rows.lastIndex) big else small, bottomEnd = if (i == rows.lastIndex) big else small,
                )
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(groupContainer())
                        .luminousElasticClickable(shape = shape, maxStretchPercent = .02f) { set(!on) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
                    Switch(checked = on, onCheckedChange = set)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMediumEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun Segment(label: String, selected: Boolean, i: Int, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val outer by animateDpAsState(if (selected) 22.dp else 18.dp, spring(dampingRatio = .55f, stiffness = 500f), label = "o")
    val inner by animateDpAsState(if (selected) 22.dp else 6.dp, spring(dampingRatio = .55f, stiffness = 500f), label = "i")
    val shape = RoundedCornerShape(
        topStart = if (i == 0) outer else inner, bottomStart = if (i == 0) outer else inner,
        topEnd = if (i == count - 1) outer else inner, bottomEnd = if (i == count - 1) outer else inner,
    )
    val bg by animateColorAsState(if (selected) cs.primary else cs.surfaceContainerHigh, tween(200), label = "b")
    Row(
        modifier.height(46.dp).clip(shape).background(bg).luminousElasticClickable(shape = shape, maxStretchPercent = .05f, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) { Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(label, style = MaterialTheme.typography.labelLargeEmphasized, color = if (selected) cs.onPrimary else cs.onSurface)
    }
}

/** Page indicator: the current page is a stretched pill, the others dots; colour tweens, size springs. */
@Composable
private fun PageDots(pager: PagerState, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(PAGE_COUNT) { i ->
            val on = i == pager.currentPage
            val w by animateDpAsState(if (on) 28.dp else 10.dp, spring(dampingRatio = .6f, stiffness = 500f), label = "dw")
            val c by animateColorAsState(if (on) cs.primary else cs.outlineVariant, tween(220), label = "dc")
            Box(Modifier.height(10.dp).width(w).clip(if (on) RoundedCornerShape(5.dp) else MaterialShapes.Cookie4Sided.toShape()).background(c))
        }
    }
}
