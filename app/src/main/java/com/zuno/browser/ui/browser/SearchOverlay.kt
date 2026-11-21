package com.zuno.browser.ui.browser

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zuno.browser.browser.BrowserStore
import com.zuno.browser.browser.Tab
import com.zuno.browser.browser.hostOf
import com.zuno.browser.browser.resolveInput
import com.zuno.browser.ui.elasticClickable
import com.zuno.browser.ui.groupContainer
import com.zuno.browser.ui.luminousElasticClickable

/**
 * Full-screen address entry. [editing] is the tab whose URL is being edited, or null for a new tab.
 * Suggests: search, direct URL, open tabs (switch), history.
 */
@Composable
fun SearchOverlay(
    store: BrowserStore,
    editing: Tab?,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val initial = editing?.url?.takeIf { !editing.isBlank && !editing.showHome }.orEmpty()
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val q = field.text.trim()
    val engine = store.settings.searchEngine

    fun go(input: String) {
        if (input.isBlank()) return
        store.submit(input, tab = editing, newTab = editing == null)
        onDismiss()
    }

    Column(Modifier.fillMaxSize().background(cs.background).windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(cs.primaryContainer).elasticClickable(onClick = onDismiss)
                    .semantics { contentDescription = "Close" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = cs.onPrimaryContainer) }
            Spacer(Modifier.width(12.dp))
            Text(
                if (editing == null || editing.showHome) (if (store.privateMode) "New private tab" else "New tab") else "Edit address",
                style = MaterialTheme.typography.titleLargeEmphasized, color = cs.onBackground, modifier = Modifier.weight(1f),
            )
            if (store.privateMode) {
                Row(
                    Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(cs.tertiaryContainer).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(com.zuno.browser.ui.ZunoIcons.Incognito, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Private", style = MaterialTheme.typography.labelLargeEmphasized, color = cs.onTertiaryContainer)
                }
            }
        }

        // Big borderless field: 26dp box, primary @ 5.5%.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(26.dp))
                .background(cs.primary.copy(alpha = .055f)).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = cs.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (field.text.isEmpty()) {
                    Text("Search ${engine.label} or type a URL", style = MaterialTheme.typography.titleLarge, color = cs.onSurfaceVariant.copy(alpha = .6f), maxLines = 1)
                }
                BasicTextField(
                    value = field,
                    onValueChange = { field = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = cs.onSurface),
                    cursorBrush = SolidColor(cs.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onGo = { go(field.text) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            if (field.text.isNotEmpty()) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).elasticClickable { field = TextFieldValue("") }.semantics { contentDescription = "Clear" },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            }
        }

        val matchingTabs = if (q.isEmpty()) emptyList() else store.tabs.filter { t ->
            t !== editing && !t.isBlank && t.private == store.privateMode && (t.title.contains(q, true) || t.url.contains(q, true))
        }.take(4)
        val matchingHistory = if (store.privateMode) emptyList() else (if (q.isEmpty()) store.history.take(12) else store.history.filter { h ->
            h.url.contains(q, true) || h.title.contains(q, true)
        }.take(8)).filter { h -> matchingTabs.none { it.url == h.url } }
        val direct = if (q.isNotEmpty()) resolveInput(q) { "" }.takeIf { it.isNotBlank() } else null

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (q.isNotEmpty()) {
                if (direct != null) {
                    item(key = "direct") { SuggestionRow(Icons.Outlined.Language, direct, "Go to address", strong = true, onFill = null) { go(direct) } }
                }
                item(key = "search") {
                    SuggestionRow(Icons.Outlined.Search, q, "Search with ${engine.label}", strong = direct == null, onFill = null) { go(engine.url(q)) }
                }
            }
            if (matchingTabs.isNotEmpty()) {
                item(key = "tabsHeader") { SectionLabel("Switch to tab") }
                items(matchingTabs, key = { "t" + it.id }) { t ->
                    SuggestionRow(Icons.AutoMirrored.Rounded.OpenInNew, t.displayTitle, hostOf(t.url), strong = false, onFill = null) {
                        store.selectTab(t); onDismiss()
                    }
                }
            }
            if (q.isEmpty() && store.essentials.isNotEmpty() && !store.privateMode) {
                item(key = "essHeader") { SectionLabel("Pinned") }
                item(key = "ess") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        store.essentials.take(5).forEach { t ->
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(groupContainer())
                                    .luminousElasticClickable(shape = RoundedCornerShape(22.dp)) { store.selectTab(t); onDismiss() }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                SiteBadge(t.host, 38.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(t.displayTitle, style = MaterialTheme.typography.labelMedium, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
                            }
                        }
                    }
                }
            }
            if (matchingHistory.isNotEmpty()) {
                item(key = "histHeader") { SectionLabel(if (q.isEmpty()) "Recent" else "History") }
                items(matchingHistory, key = { "h" + it.url }) { h ->
                    SuggestionRow(Icons.Outlined.History, h.title.ifBlank { h.url }, hostOf(h.url), strong = false, onFill = { field = TextFieldValue(h.url, TextRange(h.url.length)) }) { go(h.url) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun SuggestionRow(icon: ImageVector, title: String, subtitle: String, strong: Boolean, onFill: (() -> Unit)?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val bg by animateColorAsState(if (strong) cs.primaryContainer else groupContainer(), tween(200), label = "sugg")
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg)
            .luminousElasticClickable(shape = shape, maxStretchPercent = .03f, maxTranslateDp = 1.2.dp, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (chipBg, chipFg) = if (strong) cs.primary to cs.onPrimary else chipColors()
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(chipBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = chipFg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = if (strong) cs.onPrimaryContainer else cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (strong) cs.onPrimaryContainer.copy(alpha = .75f) else cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onFill != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).elasticClickable(onClick = onFill).semantics { contentDescription = "Edit" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.NorthWest, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
        }
    }
}
