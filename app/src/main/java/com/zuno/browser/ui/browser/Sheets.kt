package com.zuno.browser.ui.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zuno.browser.browser.BrowserStore
import com.zuno.browser.browser.ContextMenuRequest
import com.zuno.browser.browser.Tab
import com.zuno.browser.browser.Workspace
import com.zuno.browser.ui.BigActionButton
import com.zuno.browser.ui.SettingIcon
import com.zuno.browser.ui.SettingsDivider
import com.zuno.browser.ui.SheetGroup
import com.zuno.browser.ui.SheetHeaderButton
import com.zuno.browser.ui.SheetShape
import com.zuno.browser.ui.ZunoIcons
import com.zuno.browser.ui.elasticClickable
import com.zuno.browser.ui.luminousElasticClickable

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("link", text))
}

fun shareText(context: Context, text: String, title: String? = null) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (title != null) putExtra(Intent.EXTRA_SUBJECT, title)
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Sheet header for a site: 52dp MaterialShapes badge + headlineMediumEmphasized title. */
@Composable
private fun SiteHeader(tab: Tab) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        SiteBadge(tab.host.ifBlank { tab.displayTitle }, 52.dp, selected = true, polygon = MaterialShapes.Cookie7Sided)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tab.displayTitle, style = MaterialTheme.typography.headlineSmallEmphasized, color = cs.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!tab.isBlank) Text(tab.url, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Plain action row inside a SheetGroup. */
@Composable
fun SheetRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).elasticClickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SheetSwitchRow(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).elasticClickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SheetBody(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        content()
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        Spacer(Modifier.height(8.dp))
    }
}

/** The page menu ("…" in the dock). */
@Composable
fun MenuSheet(
    store: BrowserStore,
    tab: Tab?,
    shieldOn: Boolean,
    onToggleShieldForSite: () -> Unit,
    onFind: () -> Unit,
    onSettings: () -> Unit,
    onNewPrivate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), shape = SheetShape) {
        SheetBody {
            if (tab != null && !tab.isBlank) {
                SiteHeader(tab)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionTile(Icons.AutoMirrored.Outlined.ArrowBack, "Back", Modifier.weight(1f), enabled = tab.canGoBack) { store.goBack() }
                    ActionTile(Icons.AutoMirrored.Outlined.ArrowForward, "Forward", Modifier.weight(1f), enabled = tab.canGoForward) { store.goForward() }
                    ActionTile(if (tab.loading) Icons.Rounded.Stop else Icons.Outlined.Refresh, if (tab.loading) "Stop" else "Reload", Modifier.weight(1f)) { store.reload(); onDismiss() }
                    ActionTile(Icons.Outlined.Share, "Share", Modifier.weight(1f)) { shareText(context, tab.url, tab.title); onDismiss() }
                }

                ShieldCard(store, tab, shieldOn, onToggleShieldForSite)

                SheetGroup {
                    SheetSwitchRow(Icons.Outlined.DesktopWindows, "Desktop site", null, tab.desktop) { store.toggleDesktop(tab) }
                    SettingsDivider()
                    SheetSwitchRow(Icons.Outlined.PushPin, "Pin tab", "Keep it in the grid at the top of the sidebar", tab.essential) { store.togglePin(tab) }
                }
                SheetGroup {
                    SheetRow(Icons.Outlined.FindInPage, "Find in page") { onFind() }
                    SettingsDivider()
                    SheetRow(Icons.Outlined.ContentCopy, "Copy link") { copyToClipboard(context, tab.url); store.message("Link copied"); onDismiss() }
                    SettingsDivider()
                    SheetSwitchRow(com.zuno.browser.ui.ZunoIcons.Incognito, "Private mode", "Separate tabs, nothing saved", store.privateMode) { onNewPrivate() }
                    SettingsDivider()
                    SheetRow(Icons.Outlined.Settings, "Settings") { onSettings() }
                }
            } else {
                Text("Zuno", style = MaterialTheme.typography.headlineMediumEmphasized, color = cs.onSurface, modifier = Modifier.padding(start = 8.dp))
                SheetGroup {
                    SheetSwitchRow(com.zuno.browser.ui.ZunoIcons.Incognito, "Private mode", "Separate tabs, nothing saved", store.privateMode) { onNewPrivate() }
                    SettingsDivider()
                    SheetRow(Icons.Outlined.Settings, "Settings") { onSettings() }
                }
            }
        }
    }
}

/** Long-press on a tab (sidebar row or essential). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabActionsSheet(store: BrowserStore, tab: Tab, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), shape = SheetShape) {
        SheetBody {
            SiteHeader(tab)
            SheetGroup {
                SheetSwitchRow(Icons.Outlined.PushPin, "Pinned", "In the grid at the top of the sidebar", tab.essential) { store.togglePin(tab) }
                SettingsDivider()
                SheetRow(Icons.Outlined.FileCopy, "Duplicate") { store.duplicate(tab); onDismiss() }
                if (!tab.isBlank) {
                    SettingsDivider()
                    SheetRow(Icons.Outlined.ContentCopy, "Copy link") { copyToClipboard(context, tab.url); store.message("Link copied"); onDismiss() }
                }
            }
            if (store.workspaces.size > 1) {
                SheetGroup {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SettingIcon(Icons.Outlined.DriveFileMove)
                        Spacer(Modifier.width(14.dp))
                        Text("Move to workspace", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    }
                    FlowRow(
                        Modifier.padding(start = 76.dp, end = 16.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        store.workspaces.forEach { w ->
                            val current = !tab.essential && w.id == tab.workspaceId
                            SheetHeaderButton(
                                text = w.name,
                                primary = current,
                                icon = if (current) Icons.Rounded.Check else ZunoIcons.get(w.icon),
                                onClick = { if (!current) { store.moveToWorkspace(tab, w.id); onDismiss() } },
                            )
                        }
                    }
                }
            }
            BigActionButton(
                "Close tab", { store.closeTab(tab); onDismiss() }, Modifier.fillMaxWidth(),
                container = cs.errorContainer, content = cs.onErrorContainer, icon = Icons.Rounded.Close,
            )
        }
    }
}

/** Create / edit a workspace: Cancel · title · Save header, big name field, icon grid. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceSheet(existing: Workspace?, canDelete: Boolean, onSave: (String, String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var icon by remember { mutableStateOf(existing?.icon ?: ZunoIcons.keys.random()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), shape = SheetShape) {
        SheetBody {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SheetHeaderButton("Cancel", primary = false, onClick = onDismiss)
                Text(if (existing == null) "New workspace" else "Workspace", style = MaterialTheme.typography.titleLargeEmphasized, color = cs.onSurface)
                SheetHeaderButton("Save", primary = true, icon = Icons.Rounded.Check, enabled = name.isNotBlank(), onClick = { onSave(name.trim(), icon) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(ZunoIcons.get(icon), null, tint = cs.onPrimaryContainer, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).clip(RoundedCornerShape(26.dp)).background(cs.primary.copy(alpha = .055f)).padding(horizontal = 18.dp, vertical = 16.dp)) {
                    if (name.isEmpty()) Text("Name", style = MaterialTheme.typography.headlineMediumEmphasized, color = cs.onSurfaceVariant.copy(alpha = .6f))
                    BasicTextField(
                        value = name, onValueChange = { name = it.take(24) }, singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMediumEmphasized.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary), modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            SheetGroup {
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 6,
                ) {
                    ZunoIcons.keys.forEach { key ->
                        val sel = key == icon
                        Box(
                            Modifier.size(48.dp).clip(if (sel) MaterialShapes.Cookie9Sided.toShape() else RoundedCornerShape(16.dp))
                                .background(if (sel) cs.primary else Color.Transparent)
                                .luminousElasticClickable(shape = RoundedCornerShape(16.dp)) { icon = key },
                            contentAlignment = Alignment.Center,
                        ) { Icon(ZunoIcons.get(key), key, tint = if (sel) cs.onPrimary else cs.onSurface, modifier = Modifier.size(24.dp)) }
                    }
                }
            }
            if (existing != null && canDelete) {
                BigActionButton(
                    "Delete workspace", onDelete, Modifier.fillMaxWidth(),
                    container = cs.errorContainer, content = cs.onErrorContainer, icon = Icons.Rounded.Delete,
                )
            }
        }
    }
}

/** Long-press on a link or image in a page. */
@Composable
fun ContextMenuSheet(store: BrowserStore, req: ContextMenuRequest, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val link = req.linkUri
    val src = req.srcUri
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), shape = SheetShape) {
        SheetBody {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(req.title?.takeIf { it.isNotBlank() } ?: if (req.isImage) "Image" else "Link", style = MaterialTheme.typography.titleLargeEmphasized, color = cs.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(link ?: src.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (link != null) {
                SheetGroup {
                    SheetRow(Icons.AutoMirrored.Outlined.OpenInNew, "Open in new tab") { store.newTab(link, select = false, parent = req.tab); store.message("Opened in background"); onDismiss() }
                    SettingsDivider()
                    SheetRow(com.zuno.browser.ui.ZunoIcons.Incognito, "Open in private tab") { store.newTab(link, private = true, parent = req.tab); onDismiss() }
                    SettingsDivider()
                    SheetRow(Icons.Outlined.ContentCopy, "Copy link") { copyToClipboard(context, link); store.message("Link copied"); onDismiss() }
                    SettingsDivider()
                    SheetRow(Icons.Outlined.Share, "Share link") { shareText(context, link); onDismiss() }
                }
            }
            if (req.isImage && src != null) {
                SheetGroup {
                    SheetRow(Icons.Outlined.Image, "Open image in new tab") { store.newTab(src, parent = req.tab); onDismiss() }
                    SettingsDivider()
                    SheetRow(Icons.Outlined.ContentCopy, "Copy image address") { copyToClipboard(context, src); store.message("Copied"); onDismiss() }
                }
            }
        }
    }
}
