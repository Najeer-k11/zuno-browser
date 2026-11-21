package com.zuno.browser.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zuno.browser.ui.theme.Palette

/*
 * The shared Material 3 Expressive building blocks. Every screen in an app built on this skill is
 * assembled from these, so a change to a radius or a colour role reaches the whole app at once.
 *
 * Origin: Expressive Clock (github.com/samuelbeckman/expressive-clock), itself a fork of
 * Material Clock by Rayane Bouafia. MIT.
 */

fun Color.luminanceIsDark(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

/** Dark-ness of the *effective* theme (respects the in-app Light/Dark override, unlike isSystemInDarkTheme). */
@Composable
fun isAppDark(): Boolean = MaterialTheme.colorScheme.surface.luminanceIsDark()

/** Card/group background: surfaceContainer in dark, surfaceContainerLow in light. */
@Composable
fun groupContainer(): Color =
    if (isAppDark()) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow

// ---------------------------------------------------------------------------------------------
// Headers
// ---------------------------------------------------------------------------------------------

/** Big screen title: displayMediumEmphasized + a primary-coloured titleLargeEmphasized subtitle. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displayMediumEmphasized, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.primary)
            }
        }
        trailing()
    }
}

/** 48dp primaryContainer circle with an icon (prev/next, small actions). */
@Composable
fun RoundTonalButton(icon: ImageVector, label: String, onClick: () -> Unit, size: Int = 48) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).elasticClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
}

/** Sub-page top bar: 52dp tonal back circle + headlineLargeEmphasized title (Settings style). */
@Composable
fun BackTitleBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundTonalButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack, size = 52)
        Spacer(Modifier.width(18.dp))
        Text(title, style = MaterialTheme.typography.headlineLargeEmphasized, color = MaterialTheme.colorScheme.onBackground)
    }
}

// ---------------------------------------------------------------------------------------------
// Sheets
// ---------------------------------------------------------------------------------------------

/** Top corners of every bottom sheet. */
val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/**
 * Cancel / Save pills for sheet headers. Layout:
 * Row(SpaceBetween) { SheetHeaderButton("Cancel", false); Text(title, titleLargeEmphasized); SheetHeaderButton("Save", true, icon = Check) }
 */
@Composable
fun SheetHeaderButton(text: String, primary: Boolean, onClick: () -> Unit, icon: ImageVector? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .height(46.dp)
            .clip(shape)
            .background(
                when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = .10f)
                    primary -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .70f)
                },
            )
            .luminousElasticClickable(enabled = enabled, shape = shape, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
            primary -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLargeEmphasized, color = fg)
    }
}

/** A rounded group inside a sheet (surfaceContainerHigh, 26dp). Put SettingsRows / pills inside. */
@Composable
fun SheetGroup(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 6.dp),
    ) { content() }
}

/** 44dp primaryContainer pill showing a value (date, time, duration). */
@Composable
fun ValuePill(text: String, onClick: () -> Unit) {
    Box(
        Modifier.height(44.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer)
            .elasticClickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLargeEmphasized, color = MaterialTheme.colorScheme.onPrimaryContainer) }
}

/** Full-width 56–64dp primary action (e.g. onboarding CTA, Edit). */
@Composable
fun BigActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.primary, content: Color = MaterialTheme.colorScheme.onPrimary, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier.height(60.dp).clip(shape).background(container).luminousElasticClickable(shape = shape, maxStretchPercent = .04f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = content); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.titleMediumEmphasized, color = content)
    }
}

// ---------------------------------------------------------------------------------------------
// Settings rows
// ---------------------------------------------------------------------------------------------

/** A titled group card: 28dp corners, UPPERCASE labelLargeEmphasized onSurfaceVariant heading. */
@Composable
fun SettingsCard(title: String?, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(groupContainer())
            .padding(top = if (title != null) 18.dp else 8.dp, bottom = 8.dp),
    ) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
        }
        content()
    }
}

/** 46dp, 15dp-corner icon chip. primaryContainer in light, primary in dark. Optional custom tint. */
@Composable
fun SettingIcon(icon: ImageVector, tint: Color? = null) {
    val dark = isAppDark()
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(
            tint?.copy(alpha = if (dark) .9f else .22f)
                ?: if (dark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, null, Modifier.size(23.dp),
            tint = when {
                tint != null && dark -> Color.White
                tint != null -> tint
                dark -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            },
        )
    }
}

/** Row that navigates/opens a picker. Shows a chevron, or a tonal value pill if [trailing] is set. */
@Composable
fun SettingsNavigateRow(icon: ImageVector, title: String, subtitle: String? = null, trailing: String? = null, iconTint: Color? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).elasticClickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon, iconTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Box(Modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        } else {
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Whole-row toggle with a trailing Switch. */
@Composable
fun SettingsSwitchRow(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).elasticClickable(onClick = { onCheckedChange(!checked) }).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Hairline between rows, inset to align with the text column (76dp). */
@Composable
fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 76.dp, end = 20.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)))
}

// ---------------------------------------------------------------------------------------------
// Content tiles
// ---------------------------------------------------------------------------------------------

private val TileShape = RoundedCornerShape(24.dp)

/**
 * The list item of the design system: a 24dp tonal tile with a 6dp accent pill on the left,
 * titleMediumEmphasized title and bodyMedium supporting line. Elastic, luminous press.
 */
@Composable
fun AccentTile(
    title: String,
    supporting: String?,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().clip(TileShape).background(groupContainer().copy(alpha = .94f), TileShape)
            .border(1.25.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f), TileShape)
            .luminousElasticClickable(shape = TileShape, maxStretchPercent = .03f, maxTranslateDp = 1.2.dp, onClick = onClick)
            .padding(start = 14.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(6.dp).height(44.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            extra()
        }
    }
}

/**
 * "Special"/secondary-origin item (holiday, system item, suggestion): tertiaryContainer tile with a
 * 44dp Cookie9Sided badge. Use [strong]=false for a quieter surfaceContainerHigh variant.
 */
@Composable
fun BadgeTile(title: String, supporting: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, strong: Boolean = true) {
    val bg = if (strong) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (strong) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier.fillMaxWidth().clip(TileShape).background(bg, TileShape)
            .luminousElasticClickable(shape = TileShape, maxStretchPercent = .03f, maxTranslateDp = 1.2.dp, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(MaterialShapes.Cookie9Sided.toShape())
                .background(if (strong) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = if (strong) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = .75f))
        }
    }
}

/** Outlined empty state with a tonal action pill. */
@Composable
fun EmptyStateCard(text: String, actionLabel: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(TileShape).border(1.25.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f), TileShape).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        SheetHeaderButton(text = actionLabel, primary = false, onClick = onAction)
    }
}
