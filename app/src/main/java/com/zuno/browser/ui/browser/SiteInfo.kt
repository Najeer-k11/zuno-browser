package com.zuno.browser.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Https
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zuno.browser.browser.BrowserStore
import com.zuno.browser.browser.SiteSecurity
import com.zuno.browser.browser.Tab
import com.zuno.browser.engine.GeckoEngine
import com.zuno.browser.ui.SettingIcon
import com.zuno.browser.ui.SettingsDivider
import com.zuno.browser.ui.SheetGroup
import com.zuno.browser.ui.SheetShape
import com.zuno.browser.ui.elasticClickable
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.util.Date

/** Zuno Shield status for the tab's site: tertiary card with the per-site switch. */
@Composable
fun ShieldCard(store: BrowserStore, tab: Tab, shieldOn: Boolean, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val allowed = store.isAllowlisted(tab.url)
    val protectedHere = shieldOn && !allowed
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(if (protectedHere) cs.tertiaryContainer else cs.surfaceContainerHigh)
            .elasticClickable(enabled = shieldOn, onClick = onToggle).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(if (protectedHere) cs.tertiary else cs.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Shield, null, tint = if (protectedHere) cs.onTertiary else cs.onSurfaceVariant) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            val fg = if (protectedHere) cs.onTertiaryContainer else cs.onSurface
            Text(
                when {
                    protectedHere -> "Zuno Shield is on"
                    !shieldOn -> "Zuno Shield is off"
                    else -> "Paused for ${tab.host}"
                },
                style = MaterialTheme.typography.titleMediumEmphasized, color = fg,
            )
            Text(
                if (protectedHere) "${tab.adsBlocked} ads · ${tab.trackersBlocked} trackers blocked on this page" else "Ads and trackers are allowed here",
                style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = .75f),
            )
        }
        if (shieldOn) Switch(checked = !allowed, onCheckedChange = { onToggle() })
    }
}

/** What the dock's shield chip opens: connection security, certificate, Shield and site data. */
@Composable
fun SiteInfoSheet(
    store: BrowserStore,
    tab: Tab,
    shieldOn: Boolean,
    onToggleShield: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sec = tab.security
    val isHttps = tab.url.startsWith("https://")
    val state = connectionState(tab.url, sec)
    var showCert by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), shape = SheetShape) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header: connection badge + host.
            val (badgeBg, badgeFg) = when (state) {
                ConnState.SECURE -> cs.primary to cs.onPrimary
                ConnState.MIXED, ConnState.EXCEPTION -> cs.tertiaryContainer to cs.onTertiaryContainer
                ConnState.INSECURE -> cs.errorContainer to cs.onErrorContainer
                ConnState.LOCAL -> cs.surfaceContainerHighest to cs.onSurfaceVariant
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(MaterialShapes.Cookie7Sided.toShape()).background(badgeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when (state) {
                            ConnState.SECURE -> Icons.Rounded.Lock
                            ConnState.INSECURE -> Icons.Rounded.LockOpen
                            ConnState.LOCAL -> Icons.Outlined.Http
                            else -> Icons.Rounded.Warning
                        },
                        null, tint = badgeFg, modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(tab.host.ifBlank { tab.url }, style = MaterialTheme.typography.headlineSmallEmphasized, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        state.title,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = if (state == ConnState.INSECURE) cs.error else if (state == ConnState.SECURE) cs.primary else cs.tertiary,
                    )
                }
            }

            // Connection details.
            SheetGroup {
                InfoLine(if (isHttps) Icons.Outlined.Https else Icons.Outlined.Http, "Connection", state.detail)
                if (sec != null && isHttps) {
                    if (sec.mixedActive == SecurityInformation.CONTENT_LOADED || sec.mixedPassive == SecurityInformation.CONTENT_LOADED) {
                        SettingsDivider()
                        InfoLine(
                            Icons.Outlined.ReportProblem, "Insecure content",
                            if (sec.mixedActive == SecurityInformation.CONTENT_LOADED) "Scripts or frames on this page load over HTTP"
                            else "Some images or media load over HTTP",
                        )
                    }
                    val cert = sec.certificate
                    if (cert != null) {
                        SettingsDivider()
                        InfoLine(
                            Icons.Outlined.VerifiedUser, "Verified by",
                            dnField(cert.issuerX500Principal.name, "O") ?: dnField(cert.issuerX500Principal.name, "CN") ?: "Unknown issuer",
                        )
                        SettingsDivider()
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).elasticClickable { showCert = !showCert }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingIcon(Icons.Outlined.Badge)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Certificate", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                                Text(
                                    if (sec.securityMode == SecurityInformation.SECURITY_MODE_VERIFIED) "Extended validation" else "Domain validated",
                                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                                )
                            }
                            Icon(if (showCert) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (showCert) "Hide details" else "Show details", tint = cs.onSurfaceVariant)
                        }
                        AnimatedVisibility(
                            showCert,
                            enter = expandVertically(spring(dampingRatio = .8f, stiffness = 420f)) + fadeIn(tween(180)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                        ) { CertificateDetails(cert) }
                    }
                }
            }

            if (!tab.isBlank) ShieldCard(store, tab, shieldOn, onToggleShield)

            SheetGroup {
                SheetRow(Icons.Outlined.DeleteSweep, "Clear cookies and site data", "Signs you out of ${tab.host.ifBlank { "this site" }}") {
                    val host = tab.host
                    if (host.isNotBlank()) GeckoEngine.clearSiteData(host) {
                        store.message("Cleared data for $host")
                        tab.session?.reload()
                    }
                    onDismiss()
                }
                SettingsDivider()
                SheetRow(Icons.Outlined.Settings, "Shield and privacy settings") { onSettings() }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CertificateDetails(cert: X509Certificate) {
    val cs = MaterialTheme.colorScheme
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val expired = cert.notAfter.before(Date())
    Column(
        Modifier.fillMaxWidth().padding(start = 76.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CertField("Issued to", dnField(cert.subjectX500Principal.name, "CN") ?: cert.subjectX500Principal.name)
        dnField(cert.subjectX500Principal.name, "O")?.let { CertField("Organisation", it) }
        CertField("Issued by", dnField(cert.issuerX500Principal.name, "CN") ?: cert.issuerX500Principal.name)
        CertField("Valid from", df.format(cert.notBefore))
        CertField("Expires", df.format(cert.notAfter) + if (expired) "  (expired)" else "", warn = expired)
        CertField("SHA-256", sha256(cert), mono = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Fingerprint, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Serial ${cert.serialNumber.toString(16).uppercase()}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("${cert.sigAlgName} · v${cert.version}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun CertField(label: String, value: String, warn: Boolean = false, mono: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMediumEmphasized, color = cs.onSurfaceVariant)
        Text(
            value,
            style = if (mono) MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
            color = if (warn) cs.error else cs.onSurface,
        )
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class ConnState(val title: String, val detail: String) {
    SECURE("Connection is secure", "Encrypted with HTTPS. Information you send stays private."),
    MIXED("Partly secure", "Encrypted, but parts of this page load without HTTPS."),
    EXCEPTION("Certificate exception", "You allowed an untrusted certificate for this site."),
    INSECURE("Connection is not secure", "Not encrypted. Don't enter passwords or payment details."),
    LOCAL("Local page", "This page isn't loaded from the network."),
}

private fun connectionState(url: String, sec: SiteSecurity?): ConnState = when {
    !url.startsWith("http") -> ConnState.LOCAL
    url.startsWith("http://") -> ConnState.INSECURE
    sec == null -> ConnState.SECURE.takeIf { url.startsWith("https://") } ?: ConnState.INSECURE
    sec.isException -> ConnState.EXCEPTION
    !sec.isSecure -> if (sec.mixedActive == SecurityInformation.CONTENT_LOADED || sec.mixedPassive == SecurityInformation.CONTENT_LOADED) ConnState.MIXED else ConnState.INSECURE
    sec.mixedPassive == SecurityInformation.CONTENT_LOADED -> ConnState.MIXED
    else -> ConnState.SECURE
}

/** Reads one attribute (e.g. "CN", "O") from an RFC 2253 distinguished name. */
private fun dnField(dn: String, key: String): String? {
    val parts = Regex("""(?:^|,)\s*([A-Za-z0-9.]+)=((?:\\.|"[^"]*"|[^,])*)""").findAll(dn)
    return parts.firstOrNull { it.groupValues[1].equals(key, true) }
        ?.groupValues?.get(2)?.trim()?.removeSurrounding("\"")?.replace("\\,", ",")?.takeIf { it.isNotBlank() }
}

private fun sha256(cert: X509Certificate): String =
    MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString(":") { "%02X".format(it) }
