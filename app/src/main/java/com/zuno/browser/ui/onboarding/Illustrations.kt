package com.zuno.browser.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.zuno.browser.ui.ZunoIcons
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Brand colours of the launcher icon (fixed, not theme roles): indigo tile, pink sunny. */
val BrandIndigo = Color(0xFF3A44B8)
val BrandPink = Color(0xFFFFD8E4)

/** The Zuno mark drawn natively: pink Sunny with an indigo rounded Z. */
@Composable
fun ZunoMark(size: Dp, modifier: Modifier = Modifier, spin: Float = 0f, sunny: Color = BrandPink, z: Color = BrandIndigo) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }.clip(MaterialShapes.Sunny.toShape()).background(sunny))
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            val l = s * .345f; val r = s * .655f; val t = s * .345f; val b = s * .655f
            val path = Path().apply { moveTo(l, t); lineTo(r, t); lineTo(l, b); lineTo(r, b) }
            drawPath(path, z, style = Stroke(width = s * .13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Page 1: the mark turning slowly, with MaterialShapes orbiting it. */
@Composable
fun WelcomeArt() {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "welcome")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(20_000, easing = LinearEasing)), label = "spin")
    val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "orbit")
    val breathe by t.animateFloat(.94f, 1.04f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(250.dp).graphicsLayer { scaleX = breathe; scaleY = breathe }.clip(CircleShape).background(BrandIndigo))
        ZunoMark(170.dp, spin = spin)
        val shapes = listOf(
            Triple(MaterialShapes.Cookie4Sided, cs.tertiaryContainer, 0f),
            Triple(MaterialShapes.Clover4Leaf, cs.secondaryContainer, 120f),
            Triple(MaterialShapes.Cookie9Sided, cs.primaryContainer, 240f),
        )
        shapes.forEach { (shape, color, phase) ->
            val a = Math.toRadians((orbit + phase).toDouble())
            val rx = 132f; val ry = 116f
            Box(
                Modifier.offset { IntOffset((cos(a) * rx * density).roundToInt(), (sin(a) * ry * density).roundToInt()) }
                    .size(34.dp).graphicsLayer { rotationZ = -orbit * 2 + phase }.clip(shape.toShape()).background(color),
            )
        }
    }
}

/** Page 2: ads fly at the shield and bounce off while the blocked counter ticks up. */
@Composable
fun ShieldArt() {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "shield")
    val cycle by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "cycle")
    val pulse by t.animateFloat(1f, 1.08f, infiniteRepeatable(keyframes { durationMillis = 800; 1f at 0; 1.08f at 120; 1f at 420 }), label = "pulse")
    val count by t.animateFloat(128f, 248f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "count")
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(240.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(cs.tertiaryContainer.copy(alpha = .55f)))
        // incoming ads from four directions, staggered
        repeat(4) { i ->
            val p = (cycle + i * .25f) % 1f
            val angle = (i * 90 + 35) * PI / 180
            val far = 128f; val near = 54f
            // fly in (0..0.6), bounce back + fade (0.6..1)
            val dist = if (p < .6f) far - (far - near) * (p / .6f) else near + (far - near) * .55f * ((p - .6f) / .4f)
            val alpha = if (p < .6f) 1f else 1f - (p - .6f) / .4f
            Box(
                Modifier.offset { IntOffset((cos(angle) * dist * density).roundToInt(), (sin(angle) * dist * density).roundToInt()) }
                    .graphicsLayer { this.alpha = alpha; rotationZ = if (p < .6f) 0f else (p - .6f) * 400f }
                    .size(width = 46.dp, height = 30.dp).clip(RoundedCornerShape(9.dp)).background(cs.errorContainer),
                contentAlignment = Alignment.Center,
            ) { Text("AD", style = MaterialTheme.typography.labelMediumEmphasized, color = cs.onErrorContainer) }
        }
        Box(
            Modifier.size(112.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.clip(MaterialShapes.SoftBurst.toShape()).background(cs.tertiary),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Shield, null, tint = cs.onTertiary, modifier = Modifier.size(52.dp)) }
        Row(
            Modifier.align(Alignment.BottomCenter).clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainerHighest).padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Block, null, tint = cs.tertiary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("${count.toInt()} blocked", style = MaterialTheme.typography.labelLargeEmphasized, color = cs.onSurface)
        }
    }
}

/** Page 3: a mini Zen sidebar - tabs slide in, the selection moves, workspaces switch. */
@Composable
fun TabsArt() {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "tabs")
    val cycle by t.animateFloat(0f, 4f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "cycle")
    val step = cycle.toInt()
    val frac = cycle - step
    val sel = step % 4
    val ws = (step / 2) % 3
    val ease = FastOutSlowInEasing.transform((frac * 2.5f).coerceAtMost(1f))
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.size(width = 210.dp, height = 250.dp).clip(RoundedCornerShape(32.dp)).background(cs.surfaceContainerHigh).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // essentials grid
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(cs.primaryContainer, cs.tertiaryContainer, cs.secondaryContainer, cs.primaryContainer).forEach { c ->
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(16.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.primary.copy(alpha = .7f)))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(cs.outlineVariant.copy(alpha = .5f)))
            // tab rows sliding in, with a moving selected row
            Box(Modifier.fillMaxWidth().weight(1f)) {
                val rowH = 30.dp; val gap = 6.dp
                val prev = (sel + 3) % 4
                val y = (prev + (sel - prev).let { if (it < 0) it + 4 else it } * ease).let { if (it >= 4) it - 4 else it }
                Box(
                    Modifier.offset { IntOffset(0, ((rowH + gap) * y).roundToPx()) }.fillMaxWidth().height(rowH)
                        .clip(RoundedCornerShape(12.dp)).background(cs.primaryContainer),
                )
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(4) { i ->
                        val enter = ((cycle * 1.0f) - i * .15f).coerceIn(0f, 1f)
                        Row(
                            Modifier.fillMaxWidth().height(rowH).graphicsLayer { translationX = (1f - FastOutSlowInEasing.transform(enter)) * -60f; alpha = enter }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(18.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(if (i == sel) cs.primary else cs.secondaryContainer))
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.height(8.dp).width((70 + (i * 17) % 40).dp).clip(CircleShape).background(cs.onSurfaceVariant.copy(alpha = .35f)))
                        }
                    }
                }
            }
            // workspace switcher
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("leaf", "briefcase", "code").forEachIndexed { i, key ->
                    val on = i == ws
                    Box(
                        Modifier.size(34.dp).clip(if (on) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
                            .background(if (on) cs.primary else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) { Icon(ZunoIcons.get(key), null, tint = if (on) cs.onPrimary else cs.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

/** Page 4: equalizer + floating notes + a mini player pill. */
@Composable
fun PlayArt() {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "play")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "time")
    val float by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "float")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "spin")
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(232.dp).graphicsLayer { rotationZ = spin }.clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.secondaryContainer))
        // equalizer
        Row(Modifier.height(96.dp).offset(y = (-14).dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            repeat(7) { i ->
                val h = .3f + .7f * ((sin((time * 2 * PI) + i * 1.3) + 1) / 2).toFloat() * (if (i % 2 == 0) 1f else .8f)
                Box(Modifier.width(12.dp).fillMaxHeight(h).clip(CircleShape).background(if (i == 3) cs.tertiary else cs.primary))
            }
        }
        // floating notes
        repeat(3) { i ->
            val p = (float + i / 3f) % 1f
            Icon(
                Icons.Rounded.MusicNote, null, tint = cs.tertiary,
                modifier = Modifier.offset(x = (-70 + i * 70).dp, y = (60 - p * 150).dp).size((22 + i * 4).dp).graphicsLayer { alpha = 1f - p; rotationZ = -15f + p * 30f },
            )
        }
        // mini player
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).clip(RoundedCornerShape(22.dp)).background(cs.surfaceContainerHighest)
                .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZunoMark(28.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Box(Modifier.height(8.dp).width(84.dp).clip(CircleShape).background(cs.onSurface.copy(alpha = .7f)))
                Spacer(Modifier.height(5.dp))
                Box(Modifier.height(4.dp).width(84.dp).clip(CircleShape).background(cs.outlineVariant)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(time).clip(CircleShape).background(cs.primary))
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(30.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Pause, null, tint = cs.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
