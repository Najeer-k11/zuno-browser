package com.zuno.browser.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zuno.browser.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Dialog
// ---------------------------------------------------------------------------------------------

/**
 * The app's dialog: a 32dp card on surfaceContainerHigh with a 60dp MaterialShapes icon badge,
 * centred headlineSmallEmphasized title, body text, optional custom [content], and two pill
 * buttons. Springs in from .86 scale. Replaces every M3 AlertDialog in the app.
 */
@Composable
fun ExpressiveDialog(
    onDismiss: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    text: String? = null,
    confirmLabel: String? = "OK",
    onConfirm: () -> Unit = onDismiss,
    dismissLabel: String? = null,
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val scale = remember { Animatable(.86f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(160)) }
        scale.animateTo(1f, spring(dampingRatio = .62f, stiffness = 420f))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(32.dp)
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }
                .clip(shape)
                .background(cs.surfaceContainerHigh)
                .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                val rot = remember { Animatable(-50f) }
                LaunchedEffect(Unit) { rot.animateTo(0f, spring(dampingRatio = .5f, stiffness = 260f)) }
                Box(
                    Modifier.size(60.dp).graphicsLayer { rotationZ = rot.value }
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(if (destructive) cs.errorContainer else cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon, null, Modifier.size(28.dp).graphicsLayer { rotationZ = -rot.value },
                        tint = if (destructive) cs.onErrorContainer else cs.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmallEmphasized, color = cs.onSurface, textAlign = TextAlign.Center)
            if (text != null) {
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            if (content != null) {
                Spacer(Modifier.height(16.dp))
                content()
            }
            if (confirmLabel != null || dismissLabel != null) {
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (dismissLabel != null) {
                        DialogButton(dismissLabel, cs.surfaceContainerHighest, cs.onSurface, Modifier.weight(1f), onDismiss)
                    }
                    if (confirmLabel != null) {
                        DialogButton(
                            confirmLabel,
                            if (destructive) cs.error else cs.primary,
                            if (destructive) cs.onError else cs.onPrimary,
                            Modifier.weight(1f), onConfirm,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.height(52.dp).clip(shape).background(bg)
            .luminousElasticClickable(shape = shape, maxStretchPercent = .05f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.titleMediumEmphasized, color = fg) }
}

// ---------------------------------------------------------------------------------------------
// Choice sheet
// ---------------------------------------------------------------------------------------------

/**
 * Single-choice picker as a bottom sheet: a SheetGroup of rows with a leading visual, title,
 * optional subtitle, and a trailing Cookie9Sided check that springs in on the selected row.
 * Selecting animates the check, then closes.
 */
@Composable
fun <T> ChoiceSheet(
    title: String,
    value: T,
    options: List<T>,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
    subtitle: ((T) -> String?)? = null,
    leading: (@Composable (T, Boolean) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf(value) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), shape = SheetShape) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                title, style = MaterialTheme.typography.headlineSmallEmphasized, color = cs.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 14.dp),
            )
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                options.forEachIndexed { i, option ->
                    val selected = option == picked
                    val big = 24.dp
                    val small = 6.dp
                    val shape = RoundedCornerShape(
                        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
                        bottomStart = if (i == options.lastIndex) big else small, bottomEnd = if (i == options.lastIndex) big else small,
                    )
                    ChoiceRow(
                        title = label(option),
                        subtitle = subtitle?.invoke(option),
                        selected = selected,
                        shape = shape,
                        leading = leading?.let { l -> { l(option, selected) } },
                    ) {
                        if (picked == option) { onDismiss(); return@ChoiceRow }
                        picked = option
                        scope.launch { delay(260); onSelect(option) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun ChoiceRow(title: String, subtitle: String?, selected: Boolean, shape: androidx.compose.ui.graphics.Shape, leading: (@Composable () -> Unit)?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primaryContainer else cs.surfaceContainerHigh, tween(220), label = "choiceBg")
    val checkScale by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = .45f, stiffness = 380f), label = "checkScale")
    val checkRot by animateFloatAsState(if (selected) 0f else -60f, spring(dampingRatio = .6f, stiffness = 220f), label = "checkRot")
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg)
            .luminousElasticClickable(shape = shape, maxStretchPercent = .02f, maxTranslateDp = 1.dp, onClick = onClick)
            .semantics { this.selected = selected; role = Role.RadioButton }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = if (selected) MaterialTheme.typography.titleMediumEmphasized else MaterialTheme.typography.titleMedium, color = cs.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(30.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale; rotationZ = checkRot }
                .clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.primary),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(18.dp)) }
    }
}

/** Leading icon chip for choice rows: 15dp-corner, flips to primary when selected. */
@Composable
fun ChoiceIcon(icon: ImageVector, selected: Boolean) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primary else cs.surfaceContainerHighest, tween(220), label = "chipBg")
    val fg by animateColorAsState(if (selected) cs.onPrimary else cs.onSurfaceVariant, tween(220), label = "chipFg")
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Palette picker
// ---------------------------------------------------------------------------------------------

/**
 * "Colour theme" row: compact 40dp three-tone chips (primary / secondary / tertiary) for the
 * wallpaper scheme and each palette. The selected chip morphs to a Cookie9Sided with a check.
 */
@Composable
fun PalettePickerRow(dark: Boolean, dynamic: Boolean, selected: Palette, onDynamic: () -> Unit, onSelect: (Palette) -> Unit) {
    val context = LocalContext.current
    val wallpaper = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
    } else null
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Palette, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Colour theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (dynamic) "Wallpaper" else selected.displayName,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 64.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (wallpaper != null) PaletteChip("Wallpaper", wallpaper.primary, wallpaper.secondaryContainer, wallpaper.tertiary, dynamic, onDynamic)
            Palette.entries.forEach { p ->
                val s = if (dark) p.dark else p.light
                PaletteChip(p.displayName, s.primary, s.secondaryContainer, s.tertiary, !dynamic && p == selected) { onSelect(p) }
            }
        }
    }
}

@Composable
private fun PaletteChip(name: String, a: Color, b: Color, c: Color, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val checkScale by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = .45f, stiffness = 380f), label = "palCheck")
    val rot by animateFloatAsState(if (selected) 0f else -45f, spring(dampingRatio = .6f, stiffness = 240f), label = "palRot")
    val shape = if (selected) MaterialShapes.Cookie9Sided.toShape() else CircleShape
    Column(
        Modifier.width(52.dp).semantics { contentDescription = "$name colour theme"; this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).elasticClickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(40.dp).graphicsLayer { rotationZ = rot }.clip(shape)) {
                // Three-tone split: primary on top, secondary container and tertiary below.
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    drawRect(a, size = Size(w, h / 2f))
                    drawRect(b, topLeft = Offset(0f, h / 2f), size = Size(w / 2f, h / 2f))
                    drawRect(c, topLeft = Offset(w / 2f, h / 2f), size = Size(w / 2f, h / 2f))
                }
            }
            if (!selected) Box(Modifier.size(40.dp).border(1.dp, cs.outlineVariant.copy(alpha = .5f), CircleShape))
            Box(
                Modifier.size(20.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                    .clip(CircleShape).background(cs.surface),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = cs.onSurface, modifier = Modifier.size(14.dp)) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name, style = MaterialTheme.typography.labelSmall,
            color = if (selected) cs.primary else cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
