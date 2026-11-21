package com.zuno.browser.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Https
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zuno.browser.BuildConfig
import com.zuno.browser.browser.BrowserStore
import com.zuno.browser.browser.monogram
import com.zuno.browser.data.BrowserSettings
import com.zuno.browser.data.SearchEngine
import com.zuno.browser.engine.GeckoEngine
import com.zuno.browser.ui.ChoiceSheet
import com.zuno.browser.ui.ExpressiveDialog
import com.zuno.browser.ui.PalettePickerRow
import com.zuno.browser.ui.browser.formatCount
import com.zuno.browser.ui.elasticClickable
import com.zuno.browser.ui.groupContainer
import com.zuno.browser.ui.isAppDark
import com.zuno.browser.ui.luminousElasticClickable
import com.zuno.browser.ui.theme.DarkMode
import org.mozilla.geckoview.WebExtension

private const val UBO_ID = "uBlock0@raymondhill.net"

/**
 * Zuno's settings: a Shield panel with quick tiles up top, then sections of connected
 * segmented rows (each row its own rounded piece; outer corners large, inner corners small).
 */
@Composable
fun SettingsScreen(
    store: BrowserStore,
    settings: BrowserSettings,
    onUpdate: ((BrowserSettings) -> BrowserSettings) -> Unit,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var enginePicker by remember { mutableStateOf(false) }
    var allowDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var extensions by remember { mutableStateOf<List<WebExtension>>(emptyList()) }
    var installing by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) { GeckoEngine.listUserExtensions { extensions = it } }

    Column(Modifier.fillMaxSize().background(cs.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            // Header.
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(cs.surfaceContainerHigh).elasticClickable(onClick = onBack)
                    .semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = cs.onSurface) }
            Spacer(Modifier.height(18.dp))
            Text("Settings", style = MaterialTheme.typography.displaySmallEmphasized, color = cs.onBackground, modifier = Modifier.padding(start = 4.dp))
            Text(
                "Zuno Browser ${BuildConfig.VERSION_NAME.removeSuffix("-debug")}",
                style = MaterialTheme.typography.titleMediumEmphasized, color = cs.primary, modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(20.dp))

            // Shield panel.
            ShieldHero(settings.adBlock, settings.blockedTotal) { onUpdate { it.copy(adBlock = !it.adBlock) } }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Outlined.GppGood, "Strict", "tracking", settings.strictTracking, Modifier.weight(1f)) { onUpdate { it.copy(strictTracking = !it.strictTracking) } }
                QuickTile(Icons.Outlined.Cookie, "Reject", "cookie banners", settings.cookieBanners, Modifier.weight(1f)) { onUpdate { it.copy(cookieBanners = !it.cookieBanners) } }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Outlined.Https, "HTTPS", "only", settings.httpsOnly, Modifier.weight(1f)) { onUpdate { it.copy(httpsOnly = !it.httpsOnly) } }
                QuickTile(Icons.Outlined.PrivacyTip, "Privacy", "control (GPC)", settings.globalPrivacyControl, Modifier.weight(1f)) { onUpdate { it.copy(globalPrivacyControl = !it.globalPrivacyControl) } }
            }

            // Look.
            SectionTitle("Look")
            Segments(
                {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RowBadge(Icons.Outlined.BrightnessAuto)
                            Spacer(Modifier.width(14.dp))
                            Text("Theme", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        }
                        Spacer(Modifier.height(12.dp))
                        ThemeSegments(settings.theme.darkMode) { m -> onUpdate { it.copy(theme = it.theme.copy(darkMode = m)) } }
                    }
                },
                {
                    ToggleRow(Icons.Outlined.Contrast, "Pure black", "True-black surfaces in dark mode", settings.theme.amoledBlack) { v ->
                        onUpdate { it.copy(theme = it.theme.copy(amoledBlack = v)) }
                    }
                },
                {
                    PalettePickerRow(
                        dark = isAppDark(),
                        dynamic = settings.theme.dynamicColor,
                        selected = settings.theme.palette,
                        onDynamic = { onUpdate { it.copy(theme = it.theme.copy(dynamicColor = true)) } },
                        onSelect = { p -> onUpdate { it.copy(theme = it.theme.copy(dynamicColor = false, palette = p)) } },
                    )
                },
            )

            // Browsing.
            SectionTitle("Browsing")
            Segments(
                { NavRow(Icons.Outlined.Search, "Search engine", settings.searchEngine.label) { enginePicker = true } },
                { ToggleRow(Icons.Outlined.UnfoldLess, "Compact mode", "Bar slides away while you scroll", settings.compactMode) { v -> onUpdate { it.copy(compactMode = v) } } },
                { ToggleRow(Icons.Outlined.Headphones, "Background play", "Keep music and videos playing when you leave Zuno", settings.backgroundPlay) { v -> onUpdate { it.copy(backgroundPlay = v) } } },
                { ToggleRow(Icons.Outlined.Code, "JavaScript", "Turn off for a faster, quieter web", settings.javaScript) { v -> onUpdate { it.copy(javaScript = v) } } },
            )

            // Shield extras.
            SectionTitle("Blocking")
            val ubo = extensions.firstOrNull { it.id == UBO_ID }
            val rows = buildList<@Composable () -> Unit> {
                add { NavRow(Icons.Outlined.Block, "Allowed sites", if (settings.allowlist.isEmpty()) "None" else "${settings.allowlist.size} paused") { allowDialog = true } }
                add {
                    NavRow(
                        Icons.Outlined.Extension, "uBlock Origin",
                        when {
                            installing -> "Installing…"
                            ubo != null -> "Installed · tap to remove"
                            else -> "Add full filter lists"
                        },
                        badge = if (ubo != null) "On" else "Get",
                    ) {
                        if (installing) return@NavRow
                        if (ubo != null) GeckoEngine.uninstall(ubo) { refresh++ }
                        else {
                            installing = true
                            GeckoEngine.installUBlockOrigin { r ->
                                installing = false
                                store.message(r.fold({ "$it installed" }, { "Couldn't install: ${it.message}" }))
                                refresh++
                            }
                        }
                    }
                }
                extensions.filter { it.id != UBO_ID }.forEach { ext ->
                    add { NavRow(Icons.Outlined.Extension, ext.metaData.name ?: ext.id, "Tap to remove", badge = "On") { GeckoEngine.uninstall(ext) { refresh++ } } }
                }
            }
            Segments(*rows.toTypedArray())

            // Data.
            SectionTitle("Data")
            Segments(
                { NavRow(Icons.Outlined.History, "Clear history", "${store.history.size} entries") { store.clearHistory(); store.message("History cleared") } },
                { NavRow(Icons.Outlined.DeleteSweep, "Clear browsing data", "Cookies, cache and site storage", danger = true) { clearDialog = true } },
            )

            // Footer.
            Spacer(Modifier.height(36.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(48.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text("Z", style = MaterialTheme.typography.titleLargeEmphasized, color = cs.onPrimaryContainer) }
                Spacer(Modifier.height(10.dp))
                Text("Zuno Browser", style = MaterialTheme.typography.titleMediumEmphasized, color = cs.onSurface)
                Text(
                    "Version ${BuildConfig.VERSION_NAME}\nGeckoView ${org.mozilla.geckoview.BuildConfig.MOZILLA_VERSION} · MPL 2.0\nGoogle Sans Flex · SIL OFL 1.1",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(28.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (enginePicker) {
        ChoiceSheet(
            title = "Search engine",
            value = settings.searchEngine,
            options = SearchEngine.entries,
            label = { it.label },
            subtitle = { android.net.Uri.parse(it.template).host?.removePrefix("www.") },
            leading = { e, sel -> EngineBadge(e.label, sel) },
            onDismiss = { enginePicker = false },
            onSelect = { e -> onUpdate { it.copy(searchEngine = e) }; enginePicker = false },
        )
    }
    if (clearDialog) {
        ExpressiveDialog(
            onDismiss = { clearDialog = false },
            icon = Icons.Outlined.DeleteSweep,
            title = "Clear browsing data?",
            text = "Signs you out of sites and removes cookies, cache and site storage. Your tabs stay open.",
            confirmLabel = "Clear",
            dismissLabel = "Cancel",
            destructive = true,
            onConfirm = {
                clearDialog = false
                GeckoEngine.clearBrowsingData { store.message("Browsing data cleared") }
            },
        )
    }
    if (allowDialog) {
        ExpressiveDialog(
            onDismiss = { allowDialog = false },
            icon = Icons.Outlined.Block,
            title = "Allowed sites",
            text = if (settings.allowlist.isEmpty()) "Shield is active everywhere. Pause it for a site from the shield in the address bar." else "Zuno Shield is paused on these sites.",
            confirmLabel = "Done",
        ) {
            if (settings.allowlist.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    settings.allowlist.sorted().forEach { host ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cs.surfaceContainerHighest).padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(host, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).elasticClickable { onUpdate { it.copy(allowlist = it.allowlist - host) } }
                                    .semantics { contentDescription = "Remove $host" },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Close, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, top = 28.dp, bottom = 10.dp),
    )
}

/** Connected segments: 24dp outer corners, 6dp inner corners, 3dp gaps. */
@Composable
private fun Segments(vararg rows: @Composable () -> Unit) {
    val bg = groupContainer()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEachIndexed { i, row ->
            val big = 24.dp
            val small = 6.dp
            val shape: Shape = RoundedCornerShape(
                topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
                bottomStart = if (i == rows.lastIndex) big else small, bottomEnd = if (i == rows.lastIndex) big else small,
            )
            Box(Modifier.fillMaxWidth().clip(shape).background(bg)) { row() }
        }
    }
}

/** Round leading badge: secondaryContainer circle with the icon in onSecondaryContainer. */
@Composable
private fun RowBadge(icon: ImageVector, danger: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(if (danger) cs.errorContainer else cs.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (danger) cs.onErrorContainer else cs.onSecondaryContainer, modifier = Modifier.size(21.dp)) }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().luminousElasticClickable(shape = RoundedCornerShape(6.dp), maxStretchPercent = .02f, maxTranslateDp = 1.dp) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowBadge(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, value: String?, badge: String? = null, danger: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().luminousElasticClickable(shape = RoundedCornerShape(6.dp), maxStretchPercent = .02f, maxTranslateDp = 1.dp, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowBadge(icon, danger)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (danger) cs.error else cs.onSurface)
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (badge != null) {
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(cs.primary).padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(badge, style = MaterialTheme.typography.labelLargeEmphasized, color = cs.onPrimary)
            }
        } else {
            Icon(Icons.Rounded.ChevronRight, null, tint = cs.onSurfaceVariant)
        }
    }
}

/** Big Shield tile: blocked counter + master switch. "Not yours" content -> tertiary family. */
@Composable
private fun ShieldHero(on: Boolean, blocked: Long, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (on) cs.tertiaryContainer else cs.surfaceContainerHigh, tween(260), label = "heroBg")
    val fg = if (on) cs.onTertiaryContainer else cs.onSurface
    val rot by animateFloatAsState(if (on) 0f else -90f, spring(dampingRatio = .5f, stiffness = 260f), label = "heroRot")
    val shape = RoundedCornerShape(32.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg)
            .luminousElasticClickable(shape = shape, maxStretchPercent = .03f, onClick = onToggle)
            .semantics { role = Role.Switch; selected = on }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).graphicsLayer { rotationZ = rot }.clip(MaterialShapes.SoftBurst.toShape())
                .background(if (on) cs.tertiary else cs.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Shield, null, tint = if (on) cs.onTertiary else cs.onSurfaceVariant, modifier = Modifier.size(30.dp).graphicsLayer { rotationZ = -rot })
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(if (on) formatCount(blocked) else "Off", style = MaterialTheme.typography.displaySmallEmphasized, color = fg)
            Text(if (on) "ads & trackers blocked by Zuno Shield" else "Zuno Shield isn't blocking ads", style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = .8f))
        }
        Switch(checked = on, onCheckedChange = { onToggle() })
    }
}

/** Quick-settings style tile; fills with primary when on. */
@Composable
private fun QuickTile(icon: ImageVector, title: String, subtitle: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (on) cs.primaryContainer else groupContainer(), tween(220), label = "tileBg")
    val corner: Dp by animateDpAsState(if (on) 28.dp else 20.dp, spring(dampingRatio = .55f, stiffness = 500f), label = "tileCorner")
    val shape = RoundedCornerShape(corner)
    Row(
        modifier.height(76.dp).clip(shape).background(bg)
            .luminousElasticClickable(shape = shape, maxStretchPercent = .05f, onClick = onClick)
            .semantics { role = Role.Switch; selected = on }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val chipBg by animateColorAsState(if (on) cs.primary else cs.surfaceContainerHighest, tween(220), label = "tileChip")
        Box(
            Modifier.size(40.dp).clip(if (on) MaterialShapes.Cookie9Sided.toShape() else CircleShape).background(chipBg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (on) cs.onPrimary else cs.onSurfaceVariant, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = if (on) cs.onPrimaryContainer else cs.onSurface, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = (if (on) cs.onPrimaryContainer else cs.onSurfaceVariant).copy(alpha = .85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** System / Light / Dark as a connected segmented control. */
@Composable
private fun ThemeSegments(value: DarkMode, onSelect: (DarkMode) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val items = listOf(
        DarkMode.SYSTEM to Icons.Outlined.BrightnessAuto,
        DarkMode.LIGHT to Icons.Outlined.LightMode,
        DarkMode.DARK to Icons.Outlined.DarkMode,
    )
    Row(Modifier.fillMaxWidth().padding(start = 54.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        items.forEachIndexed { i, (mode, icon) ->
            val sel = mode == value
            val outer by animateDpAsState(if (sel) 22.dp else 18.dp, spring(dampingRatio = .55f, stiffness = 500f), label = "segOuter")
            val inner by animateDpAsState(if (sel) 22.dp else 6.dp, spring(dampingRatio = .55f, stiffness = 500f), label = "segInner")
            val shape = RoundedCornerShape(
                topStart = if (i == 0) outer else inner, bottomStart = if (i == 0) outer else inner,
                topEnd = if (i == items.lastIndex) outer else inner, bottomEnd = if (i == items.lastIndex) outer else inner,
            )
            val bg by animateColorAsState(if (sel) cs.primary else cs.surfaceContainerHighest, tween(200), label = "segBg")
            val fg by animateColorAsState(if (sel) cs.onPrimary else cs.onSurfaceVariant, tween(200), label = "segFg")
            Row(
                Modifier.weight(1f).height(44.dp).clip(shape).background(bg)
                    .luminousElasticClickable(shape = shape, maxStretchPercent = .05f) { onSelect(mode) }
                    .semantics { role = Role.RadioButton; selected = sel },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(if (sel) Icons.Rounded.Check else icon, null, tint = fg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(mode.label, style = MaterialTheme.typography.labelLargeEmphasized, color = fg)
            }
        }
    }
}

/** Search engine monogram on a cookie; flips to primary when selected. */
@Composable
private fun EngineBadge(label: String, selected: Boolean) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primary else cs.secondaryContainer, tween(220), label = "engBg")
    Box(Modifier.size(44.dp).clip(MaterialShapes.Cookie7Sided.toShape()).background(bg), contentAlignment = Alignment.Center) {
        Text(monogram(label), style = MaterialTheme.typography.titleMediumEmphasized, color = if (selected) cs.onPrimary else cs.onSecondaryContainer)
    }
}
