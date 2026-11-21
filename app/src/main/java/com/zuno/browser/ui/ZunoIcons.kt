package com.zuno.browser.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Zuno's own icon set for workspaces: hand-written SVG path data on a 24×24 grid, drawn as
 * round-capped strokes. Icons are tinted by `Icon(tint = …)`, so the stroke colour here is
 * irrelevant. Keys are persisted - never rename one.
 */
object ZunoIcons {
    private fun stroke(name: String, d: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()

    private val paths = linkedMapOf(
        "leaf" to "M5 19C5 11 10 5 19 5C19 14 13 19 5 19Z M5 19L13 11",
        "briefcase" to "M4 8H20V19H4Z M9 8V5H15V8 M4 13H20",
        "code" to "M8 7L3 12L8 17 M16 7L21 12L16 17 M14 4L10 20",
        "book" to "M4 5C7 4 10 4 12 6C14 4 17 4 20 5V19C17 18 14 18 12 20C10 18 7 18 4 19Z M12 6V20",
        "palette" to "M12 3A9 9 0 1 0 12 21C13.5 21 14 20 14 19C14 17.5 15 17 16.5 17H18A3 3 0 0 0 21 14C21 8 17 3 12 3Z " +
            "M8.6 11A1 1 0 1 1 6.6 11A1 1 0 1 1 8.6 11Z M12.5 7.5A1 1 0 1 1 10.5 7.5A1 1 0 1 1 12.5 7.5Z M17 10A1 1 0 1 1 15 10A1 1 0 1 1 17 10Z",
        "game" to "M7 8H17A4 4 0 0 1 21 12V14A3 3 0 0 1 16 16L15 15H9L8 16A3 3 0 0 1 3 14V12A4 4 0 0 1 7 8Z M7 10.5V13.5 M5.5 12H8.5 M15.5 11H15.6 M17.5 13H17.6",
        "music" to "M9 18V5L20 3V16 M9 18A3 3 0 1 1 3 18A3 3 0 1 1 9 18Z M20 16A3 3 0 1 1 14 16A3 3 0 1 1 20 16Z",
        "cart" to "M3 4H6L8 15H18L20 8H7 M10 20A1 1 0 1 1 8 20A1 1 0 1 1 10 20Z M18 20A1 1 0 1 1 16 20A1 1 0 1 1 18 20Z",
        "plane" to "M2.5 12.5L21.5 4L17 20L11.5 15L2.5 12.5Z M11.5 15L21.5 4 M11.5 15V19.5L14 17.3",
        "cup" to "M5 8H16V14A5 5 0 0 1 11 19H10A5 5 0 0 1 5 14Z M16 10H18A2 2 0 0 1 18 14H16 M8.5 3V5 M12.5 3V5",
        "bulb" to "M9 18H15 M10 21H14 M12 3A6 6 0 0 0 8 13.5C9 14.5 9 15.5 9 16H15C15 15.5 15 14.5 16 13.5A6 6 0 0 0 12 3Z",
        "flask" to "M9 3H15 M10 3V9L4.5 18.5A1.7 1.7 0 0 0 6 21H18A1.7 1.7 0 0 0 19.5 18.5L14 9V3 M7 15H17",
        "news" to "M4 5H17V19H6A2 2 0 0 1 4 17Z M17 9H20V17A2 2 0 0 1 18 19 M7 9H14 M7 12H14 M7 15H11",
        "coin" to "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12Z M14.5 9C14 8 13 7.5 12 7.5C10.5 7.5 9.5 8.3 9.5 9.5C9.5 12 14.5 11 14.5 14C14.5 15.3 13.4 16.5 12 16.5C10.8 16.5 9.8 15.8 9.5 15 M12 6V7.5 M12 16.5V18",
        "home" to "M3 11L12 3L21 11 M5 9.5V20H19V9.5 M10 20V14H14V20",
        "film" to "M3 5H21V19H3Z M7 5V19 M17 5V19 M3 9.5H7 M3 14.5H7 M17 9.5H21 M17 14.5H21",
        "mountain" to "M2 20L9 7L13 14L16 10L22 20Z M7.3 10.2L9 12L10.7 10.2",
        "moon" to "M20 14.5A8 8 0 1 1 9.5 4A6.5 6.5 0 0 0 20 14.5Z",
        "flame" to "M12 3C13 7 17 9 17 14A5 5 0 0 1 7 14C7 11 9 10 9 7C10.5 8 11 9.5 11 11C12.5 9.5 12.5 6 12 3Z",
        "wave" to "M2 9C4.5 6 7 6 9.5 9S14.5 12 17 9S20.5 6 22 7 M2 15C4.5 12 7 12 9.5 15S14.5 18 17 15S20.5 12 22 13",
        "rocket" to "M12 3C15.5 5.5 17 9 16 14H8C7 9 8.5 5.5 12 3Z M8 14L5 18H9 M16 14L19 18H15 M10 18L12 21L14 18 M13.5 9A1.5 1.5 0 1 1 10.5 9A1.5 1.5 0 1 1 13.5 9Z",
        "heart" to "M12 20C5 15 3 11.5 3 8.5A4.5 4.5 0 0 1 12 6.5A4.5 4.5 0 0 1 21 8.5C21 11.5 19 15 12 20Z",
        "star" to "M12 3L14.6 9L21 9.5L16 13.8L17.6 20.5L12 17L6.4 20.5L8 13.8L3 9.5L9.4 9Z",
        "spiral" to "M12 12C12 11 13 10.5 13.8 11.2C14.8 12 14.3 14 12.5 14.5C10.5 15 9 13.5 9 11.5C9 9.2 11 7.5 13.5 7.8C16.5 8.2 18 11 17.5 14C17 17 14 19.2 10.8 18.8C7 18.3 4.8 15 5.2 11.3C5.7 7 9.5 4 14 4.5",
    )

    private val cache = HashMap<String, ImageVector>()

    /** Every workspace icon key, in picker order. */
    val keys: List<String> = paths.keys.toList()

    const val DEFAULT = "spiral"

    /**
     * Traditional incognito mark: a hat (crown + brim) over round glasses. Filled; the lenses are
     * hollow rings via even-odd fill. Drawn on a 24x24 grid.
     */
    val Incognito: ImageVector by lazy {
        val hat = "M7.6,4.6 C7.85,3.55 8.95,3 9.95,3.4 L12,4.15 L14.05,3.4 C15.05,3 16.15,3.55 16.4,4.6 L17.55,9.2 H6.45 Z " +
            "M3.6,9.9 H20.4 A1.05,1.05 0 0,1 20.4,12 H3.6 A1.05,1.05 0 0,1 3.6,9.9 Z"
        fun ring(cx: Float, cy: Float, ro: Float, ri: Float) =
            "M${cx - ro},$cy A$ro,$ro 0 1,0 ${cx + ro},$cy A$ro,$ro 0 1,0 ${cx - ro},$cy Z " +
                "M${cx - ri},$cy A$ri,$ri 0 1,0 ${cx + ri},$cy A$ri,$ri 0 1,0 ${cx - ri},$cy Z"
        val glasses = ring(7.4f, 16.9f, 3.35f, 1.95f) + " " + ring(16.6f, 16.9f, 3.35f, 1.95f) +
            " M10.55,16.05 Q12,15.1 13.45,16.05 L13.45,17.35 Q12,16.55 10.55,17.35 Z"
        ImageVector.Builder("incognito", 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = addPathNodes(hat), fill = SolidColor(Color.Black))
            addPath(pathData = addPathNodes(glasses), fill = SolidColor(Color.Black), pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd)
        }.build()
    }

    fun get(key: String): ImageVector {
        val k = if (key in paths) key else DEFAULT
        return cache.getOrPut(k) { stroke(k, paths.getValue(k)) }
    }
}
