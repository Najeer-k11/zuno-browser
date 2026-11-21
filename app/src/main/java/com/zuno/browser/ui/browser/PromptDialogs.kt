package com.zuno.browser.ui.browser

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zuno.browser.browser.PromptRequest
import com.zuno.browser.ui.SheetHeaderButton
import com.zuno.browser.ui.SheetShape
import com.zuno.browser.ui.ExpressiveDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Replay
import com.zuno.browser.ui.elasticClickable
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoSession.PromptDelegate.AlertPrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ButtonPrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ChoicePrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.FilePrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.RepostConfirmPrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.TextPrompt

/** Renders whichever web prompt is pending. [onDone] clears it from the store. */
@Composable
fun PromptHost(request: PromptRequest, onDone: () -> Unit) {
    fun finish(block: () -> Unit) { block(); onDone() }
    when (val p = request.prompt) {
        is AlertPrompt -> ExpressiveDialog(
            onDismiss = { finish { request.dismiss() } },
            icon = Icons.Outlined.Info,
            title = p.title?.takeIf { it.isNotBlank() } ?: "This page says",
            text = p.message.orEmpty(),
            confirmLabel = "OK",
        )
        is ButtonPrompt -> ExpressiveDialog(
            onDismiss = { finish { request.complete(p.confirm(ButtonPrompt.Type.NEGATIVE)) } },
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            title = p.title?.takeIf { it.isNotBlank() } ?: "This page asks",
            text = p.message.orEmpty(),
            confirmLabel = "OK",
            onConfirm = { finish { request.complete(p.confirm(ButtonPrompt.Type.POSITIVE)) } },
            dismissLabel = "Cancel",
        )
        is TextPrompt -> {
            var text by remember { mutableStateOf(p.defaultValue.orEmpty()) }
            val cs = MaterialTheme.colorScheme
            ExpressiveDialog(
                onDismiss = { finish { request.dismiss() } },
                icon = Icons.Outlined.EditNote,
                title = p.title?.takeIf { it.isNotBlank() } ?: "This page asks",
                text = p.message?.takeIf { it.isNotBlank() },
                confirmLabel = "OK",
                onConfirm = { finish { request.complete(p.confirm(text)) } },
                dismissLabel = "Cancel",
            ) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.primary.copy(alpha = .07f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    BasicTextField(
                        value = text, onValueChange = { text = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary), modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        is RepostConfirmPrompt -> ExpressiveDialog(
            onDismiss = { finish { request.complete(p.confirm(AllowOrDeny.DENY)) } },
            icon = Icons.Outlined.Replay,
            title = "Resend data?",
            text = "This page needs to resend information you entered earlier. Repeating an order or payment is possible.",
            confirmLabel = "Resend",
            onConfirm = { finish { request.complete(p.confirm(AllowOrDeny.ALLOW)) } },
            dismissLabel = "Cancel",
        )
        is ChoicePrompt -> ChoiceSheet(p, request, onDone)
        is FilePrompt -> FilePicker(p, request, onDone)
        else -> LaunchedEffect(p) { finish { request.dismiss() } }
    }
}

/** <select> and menus as an expressive bottom sheet. */
@Composable
private fun ChoiceSheet(p: ChoicePrompt, request: PromptRequest, onDone: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val multiple = p.type == ChoicePrompt.Type.MULTIPLE
    val flat = remember(p) {
        buildList {
            fun add(choices: Array<out ChoicePrompt.Choice>, depth: Int) {
                choices.forEach { c ->
                    if (c.separator) return@forEach
                    add(c to depth)
                    c.items?.let { add(it, depth + 1) }
                }
            }
            add(p.choices, 0)
        }
    }
    val selected = remember(p) { mutableStateListOf<String>().apply { addAll(flat.filter { it.first.selected }.map { it.first.id }) } }
    ModalBottomSheet(
        onDismissRequest = { request.dismiss(); onDone() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SheetHeaderButton("Cancel", primary = false, onClick = { request.dismiss(); onDone() })
                Text(p.title?.takeIf { it.isNotBlank() } ?: "Choose", style = MaterialTheme.typography.titleLargeEmphasized, color = cs.onSurface)
                if (multiple) {
                    SheetHeaderButton("Done", primary = true, icon = Icons.Rounded.Check, onClick = { request.complete(p.confirm(selected.toTypedArray())); onDone() })
                } else Spacer(Modifier.width(80.dp))
            }
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).clip(RoundedCornerShape(26.dp)).background(cs.surfaceContainerHigh),
            ) {
                items(flat, key = { it.first.id + it.second }) { (c, depth) ->
                    val isGroup = c.items != null
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                            .elasticClickable(enabled = !c.disabled && !isGroup) {
                                if (multiple) {
                                    if (c.id in selected) selected.remove(c.id) else selected.add(c.id)
                                } else {
                                    request.complete(p.confirm(c)); onDone()
                                }
                            }
                            .padding(start = 16.dp + 16.dp * depth, end = 16.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isGroup) {
                            Text(c.label.uppercase(), style = MaterialTheme.typography.labelLargeEmphasized, color = cs.onSurfaceVariant)
                        } else {
                            if (multiple) Checkbox(checked = c.id in selected, onCheckedChange = null, enabled = !c.disabled)
                            else RadioButton(selected = c.selected, onClick = null, enabled = !c.disabled)
                            Spacer(Modifier.width(12.dp))
                            Text(c.label, style = MaterialTheme.typography.bodyLarge, color = if (c.disabled) cs.onSurface.copy(alpha = .38f) else cs.onSurface)
                        }
                    }
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.width(16.dp))
        }
    }
}

@Composable
private fun FilePicker(p: FilePrompt, request: PromptRequest, onDone: () -> Unit) {
    val context = LocalContext.current
    val multiple = p.type == FilePrompt.Type.MULTIPLE
    val single = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) request.complete(p.confirm(context, uri)) else request.dismiss()
        onDone()
    }
    val many = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) request.complete(p.confirm(context, uris.toTypedArray())) else request.dismiss()
        onDone()
    }
    LaunchedEffect(p) {
        val types = p.mimeTypes?.filter { it.isNotBlank() }?.map { if (it.startsWith(".")) "*/*" else it }?.ifEmpty { null }?.toTypedArray() ?: arrayOf("*/*")
        runCatching { if (multiple) many.launch(types) else single.launch(types) }
            .onFailure { request.dismiss(); onDone() }
    }
}
