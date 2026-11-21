package com.zuno.browser.ui.browser

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.zuno.browser.browser.monogram
import com.zuno.browser.ui.isAppDark
import com.zuno.browser.ui.luminousElasticClickable

/** A site's monogram on an expressive shape - Zuno's stand-in for favicons. */
@Composable
fun SiteBadge(
    label: String,
    size: Dp,
    selected: Boolean = false,
    polygon: RoundedPolygon = MaterialShapes.Cookie9Sided,
    style: TextStyle = if (size >= 44.dp) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.labelLargeEmphasized,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(size).clip(polygon.toShape()).background(if (selected) cs.primary else cs.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(monogram(label), style = style, color = if (selected) cs.onPrimary else cs.onPrimaryContainer)
    }
}

/** Rotating Cookie12Sided with a counter-rotating Sunny: the hero "moment" of the start page. */
@Composable
fun HeroShapes(outer: Dp, inner: Dp, content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "hero")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "rot")
    val breathe by t.animateFloat(.96f, 1.04f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(outer), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(outer).graphicsLayer { rotationZ = rot; scaleX = breathe; scaleY = breathe }
                .clip(MaterialShapes.Cookie12Sided.toShape()).background(cs.primaryContainer),
        )
        Box(
            Modifier.size(inner).graphicsLayer { rotationZ = -rot * 1.5f }
                .clip(MaterialShapes.Sunny.toShape()).background(cs.primary),
        )
        content()
    }
}

/** Square-ish quick action used in rows inside sheets (Back, Forward, Reload, Share…). */
@Composable
fun ActionTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, active: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val cs = MaterialTheme.colorScheme
    val bg = when {
        active -> cs.primary
        !enabled -> cs.onSurface.copy(alpha = .06f)
        else -> cs.surfaceContainerHigh
    }
    val fg = when {
        active -> cs.onPrimary
        !enabled -> cs.onSurface.copy(alpha = .38f)
        else -> cs.onSurface
    }
    Column(
        modifier.clip(shape).background(bg).luminousElasticClickable(enabled = enabled, shape = shape, maxStretchPercent = .05f, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMediumEmphasized, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Icon chip in the sheet colour language: primaryContainer in light, primary in dark. */
@Composable
fun chipColors(): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return if (isAppDark()) cs.primary to cs.onPrimary else cs.primaryContainer to cs.onPrimaryContainer
}

fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 10_000 -> String.format(java.util.Locale.US, "%.1fk", n / 1_000.0)
    else -> String.format(java.util.Locale.US, "%,d", n)
}
