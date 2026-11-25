package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.engine.VocabMnemonicEngine
import com.example.engine.WordVisualMotif
import com.example.model.VocabWord
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 4 cohesive art palettes that the "아트" button cycles without ever breaking
 * the word's semantic illustration meaning.
 */
enum class WordArtPaletteStyle(val labelKo: String) {
    EDITORIAL("에디토리얼"),
    MIDNIGHT_NEON("미드나잇"),
    STORYBOOK("파스텔"),
    BLUEPRINT("블루프린트")
}

private data class SceneColorSet(
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val surface: Color
)

private fun resolvePalette(motif: WordVisualMotif, style: WordArtPaletteStyle): SceneColorSet {
    return when (style) {
        WordArtPaletteStyle.MIDNIGHT_NEON -> SceneColorSet(
            bgTop = Color(0xFF0F172A),
            bgMid = Color(0xFF1E1B4B),
            bgBottom = Color(0xFF312E81),
            primary = Color(0xFF38BDF8),
            secondary = Color(0xFFA78BFA),
            highlight = Color(0xFFFDE047),
            surface = Color(0xFF1E293B)
        )
        WordArtPaletteStyle.STORYBOOK -> SceneColorSet(
            bgTop = Color(0xFFFFF7ED),
            bgMid = Color(0xFFFDE68A),
            bgBottom = Color(0xFFFDBA74),
            primary = Color(0xFFEA580C),
            secondary = Color(0xFF0EA5E9),
            highlight = Color(0xFFFFFFFF),
            surface = Color(0xFF9A3412)
        )
        WordArtPaletteStyle.BLUEPRINT -> SceneColorSet(
            bgTop = Color(0xFF0C2442),
            bgMid = Color(0xFF133863),
            bgBottom = Color(0xFF1B4D89),
            primary = Color(0xFF7DD3FC),
            secondary = Color(0xFFBAE6FD),
            highlight = Color(0xFFFACC15),
            surface = Color(0xFF082F49)
        )
        WordArtPaletteStyle.EDITORIAL -> when (motif) {
            WordVisualMotif.KEYSTONE_PIVOT, WordVisualMotif.SPOTLIGHT_BEAM -> SceneColorSet(
                bgTop = Color(0xFF1E293B), bgMid = Color(0xFF312E81), bgBottom = Color(0xFF4338CA),
                primary = Color(0xFFFBBF24), secondary = Color(0xFF60A5FA), highlight = Color(0xFFFEF08A), surface = Color(0xFF0F172A)
            )
            WordVisualMotif.BOUNCING_SPRING, WordVisualMotif.SUMMIT_FLAG -> SceneColorSet(
                bgTop = Color(0xFF0F766E), bgMid = Color(0xFF0284C7), bgBottom = Color(0xFF1D4ED8),
                primary = Color(0xFF34D399), secondary = Color(0xFFFBBF24), highlight = Color(0xFFFEF3C7), surface = Color(0xFF064E3B)
            )
            WordVisualMotif.LIFTING_WEIGHT, WordVisualMotif.ECO_CYCLE -> SceneColorSet(
                bgTop = Color(0xFF065F46), bgMid = Color(0xFF059669), bgBottom = Color(0xFF0D9488),
                primary = Color(0xFFA7F3D0), secondary = Color(0xFFFDE68A), highlight = Color(0xFFFFFFFF), surface = Color(0xFF064E3B)
            )
            WordVisualMotif.RISING_WAVES, WordVisualMotif.STRAIGHT_BRIDGE -> SceneColorSet(
                bgTop = Color(0xFF0C4A6E), bgMid = Color(0xFF0369A1), bgBottom = Color(0xFF0284C7),
                primary = Color(0xFF38BDF8), secondary = Color(0xFFF97316), highlight = Color(0xFFE0F2FE), surface = Color(0xFF082F49)
            )
            WordVisualMotif.FORKED_CROSSROADS, WordVisualMotif.CLASHING_ARROWS -> SceneColorSet(
                bgTop = Color(0xFF3B0764), bgMid = Color(0xFF6D28D9), bgBottom = Color(0xFF4338CA),
                primary = Color(0xFFF472B6), secondary = Color(0xFF38BDF8), highlight = Color(0xFFFDE047), surface = Color(0xFF1E1B4B)
            )
            WordVisualMotif.BALANCE_SCALE, WordVisualMotif.STACKING_CRYSTALS -> SceneColorSet(
                bgTop = Color(0xFF1E1B4B), bgMid = Color(0xFF3730A3), bgBottom = Color(0xFF1D4ED8),
                primary = Color(0xFFF59E0B), secondary = Color(0xFF34D399), highlight = Color(0xFFFEF08A), surface = Color(0xFF0F172A)
            )
            WordVisualMotif.MAGNIFYING_DIFFERENCE, WordVisualMotif.PRISM_LENS -> SceneColorSet(
                bgTop = Color(0xFF172554), bgMid = Color(0xFF1D4ED8), bgBottom = Color(0xFF0284C7),
                primary = Color(0xFF67E8F9), secondary = Color(0xFFF472B6), highlight = Color(0xFFFFFFFF), surface = Color(0xFF0F172A)
            )
            WordVisualMotif.HOURGLASS_TIME, WordVisualMotif.WARM_BEACON -> SceneColorSet(
                bgTop = Color(0xFF431407), bgMid = Color(0xFF9A3412), bgBottom = Color(0xFFD97706),
                primary = Color(0xFFFDE68A), secondary = Color(0xFFFB7185), highlight = Color(0xFFFFFFFF), surface = Color(0xFF292524)
            )
            else -> SceneColorSet(
                bgTop = Color(0xFF1E1B4B), bgMid = Color(0xFF2563EB), bgBottom = Color(0xFF0891B2),
                primary = Color(0xFF93C5FD), secondary = Color(0xFFFBBF24), highlight = Color(0xFFFFFFFF), surface = Color(0xFF0F172A)
            )
        }
    }
}

/**
 * Meaning-matched Semantic Vector Illustration for every vocabulary word.
 * Guarantees 100% conceptual relevance (e.g., "crucial" -> Glowing Arch Keystone,
 * "fluctuate" -> Ocean Wave & Trend Curve, "ambiguous" -> Forked Misty Crossroads)
 * with zero unrelated stock/recycled jogging images!
 */
@Composable
fun WordSceneIllustration(
    word: VocabWord,
    styleIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val motif = remember(word.id, word.word) { VocabMnemonicEngine.visualMotifFor(word) }
    val artStyle = remember(styleIndex) {
        val styles = WordArtPaletteStyle.entries
        styles[Math.floorMod(styleIndex, styles.size)]
    }
    val colors = remember(motif, artStyle) { resolvePalette(motif, artStyle) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.44f
            val u = minOf(w, h) / 220f

            // Rich vertical sky/stage gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.bgTop, colors.bgMid, colors.bgBottom)
                )
            )

            // Blueprint subtle grid if BLUEPRINT style
            if (artStyle == WordArtPaletteStyle.BLUEPRINT) {
                val step = 22f * u
                var gx = 0f
                while (gx < w) {
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                    gx += step
                }
                var gy = 0f
                while (gy < h) {
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                    gy += step
                }
            }

            // Soft focal aura behind main illustration
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.highlight.copy(alpha = 0.26f), colors.primary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = 125f * u
                ),
                radius = 125f * u,
                center = Offset(cx, cy)
            )

            // Draw the word's exact semantic visual scene
            when (motif) {
                WordVisualMotif.KEYSTONE_PIVOT -> drawKeystoneArchScene(cx, cy, u, colors)
                WordVisualMotif.BOUNCING_SPRING -> drawBouncingOrbScene(cx, cy, u, colors)
                WordVisualMotif.LIFTING_WEIGHT -> drawLiftingFeathersScene(cx, cy, u, colors)
                WordVisualMotif.RISING_WAVES -> drawFluctuatingWaveScene(w, h, cx, cy, u, colors)
                WordVisualMotif.FORKED_CROSSROADS -> drawForkedCrossroadsScene(cx, cy, u, colors)
                WordVisualMotif.STRAIGHT_BRIDGE -> drawStraightBridgeScene(w, h, cx, cy, u, colors)
                WordVisualMotif.MAGNIFYING_DIFFERENCE -> drawMagnifyingGlassScene(cx, cy, u, colors)
                WordVisualMotif.SPOTLIGHT_BEAM -> drawSpotlightPillarScene(cx, cy, u, colors)
                WordVisualMotif.SUMMIT_FLAG -> drawSummitFlagScene(w, h, cx, cy, u, colors)
                WordVisualMotif.STEPPING_STONES -> drawSteppingStonesScene(w, h, cx, cy, u, colors)
                WordVisualMotif.INTERLOCKING_PUZZLE -> drawInterlockingPuzzleScene(cx, cy, u, colors)
                WordVisualMotif.BALANCE_SCALE -> drawBalanceScaleScene(cx, cy, u, colors)
                WordVisualMotif.LIGHTBULB_MIND -> drawLightbulbMindScene(cx, cy, u, colors)
                WordVisualMotif.SIGNAL_NETWORK -> drawSignalNetworkScene(cx, cy, u, colors)
                WordVisualMotif.TOOLBOX_GEARS -> drawInterlockingGearsScene(cx, cy, u, colors)
                WordVisualMotif.HOURGLASS_TIME -> drawHourglassScene(cx, cy, u, colors)
                WordVisualMotif.KEY_AND_LOCK -> drawKeyAndLockScene(cx, cy, u, colors)
                WordVisualMotif.ECO_CYCLE -> drawEcoCycleScene(cx, cy, u, colors)
                WordVisualMotif.STACKING_CRYSTALS -> drawStackingCrystalsScene(cx, cy, u, colors)
                WordVisualMotif.CLASHING_ARROWS -> drawClashingArrowsScene(cx, cy, u, colors)
                WordVisualMotif.PRISM_LENS -> drawPrismLensScene(cx, cy, u, colors)
                WordVisualMotif.GLOBE_COMPASS -> drawGlobeCompassScene(cx, cy, u, colors)
                WordVisualMotif.WARM_BEACON -> drawWarmBeaconScene(cx, cy, u, colors)
                WordVisualMotif.SORTED_SHELVES -> drawSortedCategoriesScene(cx, cy, u, colors)
            }
        }
    }
}

private fun DrawScope.drawKeystoneArchScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Stone arch blocks flanking left & right
    val archRadius = 72f * u
    val archCenterY = cy + 36f * u
    drawArc(
        color = Color.White.copy(alpha = 0.25f),
        startAngle = 195f,
        sweepAngle = 55f,
        useCenter = false,
        topLeft = Offset(cx - archRadius, archCenterY - archRadius),
        size = Size(archRadius * 2f, archRadius * 2f),
        style = Stroke(width = 24f * u, cap = StrokeCap.Butt)
    )
    drawArc(
        color = Color.White.copy(alpha = 0.25f),
        startAngle = 290f,
        sweepAngle = 55f,
        useCenter = false,
        topLeft = Offset(cx - archRadius, archCenterY - archRadius),
        size = Size(archRadius * 2f, archRadius * 2f),
        style = Stroke(width = 24f * u, cap = StrokeCap.Butt)
    )
    // Light rays radiating from the central CRUCIAL golden keystone
    for (deg in listOf(-45f, -20f, 0f, 20f, 45f)) {
        val rad = (deg - 90f) * PI.toFloat() / 180f
        drawLine(
            color = c.highlight.copy(alpha = 0.55f),
            start = Offset(cx + cos(rad) * 28f * u, cy - 30f * u + sin(rad) * 28f * u),
            end = Offset(cx + cos(rad) * 52f * u, cy - 30f * u + sin(rad) * 52f * u),
            strokeWidth = 3f * u,
            cap = StrokeCap.Round
        )
    }
    // Glowing golden trapezoid KEYSTONE locking the entire arch!
    val keystone = Path().apply {
        moveTo(cx - 24f * u, cy - 52f * u)
        lineTo(cx + 24f * u, cy - 52f * u)
        lineTo(cx + 15f * u, cy - 12f * u)
        lineTo(cx - 15f * u, cy - 12f * u)
        close()
    }
    drawPath(keystone, color = c.primary)
    drawPath(keystone, color = c.highlight, style = Stroke(width = 3f * u, join = StrokeJoin.Round))
    // Crisp star spark inside keystone
    drawCircle(color = Color.White, radius = 6f * u, center = Offset(cx, cy - 34f * u))
}

private fun DrawScope.drawBouncingOrbScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    val floorY = cy + 38f * u
    drawLine(Color.White.copy(alpha = 0.35f), Offset(cx - 105f * u, floorY), Offset(cx + 105f * u, floorY), 3f * u, StrokeCap.Round)
    // Impact ripple arcs on floor
    drawOval(c.primary.copy(alpha = 0.45f), Offset(cx - 32f * u, floorY - 6f * u), Size(64f * u, 12f * u), style = Stroke(2.5f * u))
    // Upward rebound trajectory curve
    val arc = Path().apply {
        moveTo(cx - 72f * u, cy - 14f * u)
        quadraticTo(cx - 34f * u, floorY + 6f * u, cx - 8f * u, floorY - 2f * u)
        quadraticTo(cx + 22f * u, cy - 68f * u, cx + 60f * u, cy - 42f * u)
    }
    drawPath(arc, color = c.highlight.copy(alpha = 0.85f), style = Stroke(width = 4f * u, cap = StrokeCap.Round))
    // Bright elastic sphere soaring high
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White, c.primary, c.secondary), center = Offset(cx + 52f * u, cy - 48f * u), radius = 26f * u),
        radius = 22f * u,
        center = Offset(cx + 58f * u, cy - 42f * u)
    )
}

private fun DrawScope.drawLiftingFeathersScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Heavy stone base dissolving upward
    val rock = Path().apply {
        moveTo(cx - 46f * u, cy + 42f * u)
        lineTo(cx - 34f * u, cy + 12f * u)
        lineTo(cx + 32f * u, cy + 10f * u)
        lineTo(cx + 48f * u, cy + 42f * u)
        close()
    }
    drawPath(rock, color = c.surface.copy(alpha = 0.85f))
    drawPath(rock, color = c.primary.copy(alpha = 0.45f), style = Stroke(2.5f * u))
    // Floating upward feathers / light particles
    listOf(Triple(-28f, -24f, 16f), Triple(4f, -44f, 20f), Triple(36f, -18f, 15f)).forEach { (dx, dy, r) ->
        drawOval(
            color = c.primary.copy(alpha = 0.90f),
            topLeft = Offset(cx + dx * u - r * u, cy + dy * u - r * 0.55f * u),
            size = Size(r * 2f * u, r * 1.1f * u)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(cx + dx * u - r * 0.6f * u, cy + dy * u),
            end = Offset(cx + dx * u + r * 0.6f * u, cy + dy * u),
            strokeWidth = 2f * u,
            cap = StrokeCap.Round
        )
    }
    // Gentle upward relief arrows
    for (dx in listOf(-20f, 20f)) {
        drawLine(c.highlight.copy(alpha = 0.7f), Offset(cx + dx * u, cy + 4f * u), Offset(cx + dx * u, cy - 16f * u), 2.5f * u, StrokeCap.Round)
    }
}

private fun DrawScope.drawFluctuatingWaveScene(w: Float, h: Float, cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Layered rising and falling wave fill
    val fillWave = Path().apply {
        moveTo(0f, h)
        lineTo(0f, cy + 8f * u)
        cubicTo(w * 0.18f, cy - 46f * u, w * 0.34f, cy + 42f * u, w * 0.52f, cy - 16f * u)
        cubicTo(w * 0.70f, cy - 64f * u, w * 0.85f, cy + 36f * u, w, cy - 12f * u)
        lineTo(w, h)
        close()
    }
    drawPath(fillWave, color = c.primary.copy(alpha = 0.28f))
    // Crisp glowing wave graph stroke + peak/trough dots
    val lineWave = Path().apply {
        moveTo(w * 0.08f, cy + 6f * u)
        cubicTo(w * 0.22f, cy - 52f * u, w * 0.38f, cy + 44f * u, w * 0.54f, cy - 18f * u)
        cubicTo(w * 0.70f, cy - 66f * u, w * 0.82f, cy + 32f * u, w * 0.92f, cy - 14f * u)
    }
    drawPath(lineWave, color = c.highlight, style = Stroke(width = 4.5f * u, cap = StrokeCap.Round))
    listOf(Offset(w * 0.24f, cy - 22f * u), Offset(w * 0.54f, cy - 18f * u), Offset(w * 0.72f, cy - 26f * u)).forEach { pt ->
        drawCircle(c.secondary, 6.5f * u, pt)
        drawCircle(Color.White, 3f * u, pt)
    }
}

private fun DrawScope.drawForkedCrossroadsScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Vertical trunk splitting into two opposing curved paths
    val leftBranch = Path().apply {
        moveTo(cx, cy + 48f * u)
        lineTo(cx, cy + 6f * u)
        quadraticTo(cx - 8f * u, cy - 26f * u, cx - 64f * u, cy - 40f * u)
    }
    val rightBranch = Path().apply {
        moveTo(cx, cy + 48f * u)
        lineTo(cx, cy + 6f * u)
        quadraticTo(cx + 8f * u, cy - 26f * u, cx + 64f * u, cy - 40f * u)
    }
    drawPath(leftBranch, color = c.primary, style = Stroke(width = 10f * u, cap = StrokeCap.Round))
    drawPath(rightBranch, color = c.secondary, style = Stroke(width = 10f * u, cap = StrokeCap.Round))
    // Two directional signpost flags
    drawRoundRect(c.primary, Offset(cx - 82f * u, cy - 52f * u), Size(38f * u, 20f * u), CornerRadius(6f * u, 6f * u))
    drawRoundRect(c.secondary, Offset(cx + 44f * u, cy - 52f * u), Size(38f * u, 20f * u), CornerRadius(6f * u, 6f * u))
    // Central glowing ambiguity ring
    drawCircle(Color.White.copy(alpha = 0.92f), 14f * u, Offset(cx, cy + 4f * u))
    drawCircle(c.surface, 8f * u, Offset(cx, cy + 4f * u))
}

private fun DrawScope.drawStraightBridgeScene(w: Float, h: Float, cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Glowing destination sun at horizon
    drawCircle(c.highlight.copy(alpha = 0.9f), 28f * u, Offset(cx, cy - 32f * u))
    // Canyon walls left & right
    val leftCliff = Path().apply {
        moveTo(0f, h); lineTo(0f, cy - 4f * u); lineTo(cx - 42f * u, cy + 16f * u); lineTo(cx - 72f * u, h); close()
    }
    val rightCliff = Path().apply {
        moveTo(w, h); lineTo(w, cy - 4f * u); lineTo(cx + 42f * u, cy + 16f * u); lineTo(cx + 72f * u, h); close()
    }
    drawPath(leftCliff, c.surface.copy(alpha = 0.85f))
    drawPath(rightCliff, c.surface.copy(alpha = 0.85f))
    // Single straight inevitable bridge road toward horizon
    val bridge = Path().apply {
        moveTo(cx - 12f * u, cy - 4f * u)
        lineTo(cx + 12f * u, cy - 4f * u)
        lineTo(cx + 58f * u, h)
        lineTo(cx - 58f * u, h)
        close()
    }
    drawPath(bridge, c.primary.copy(alpha = 0.92f))
    drawLine(Color.White, Offset(cx, cy + 2f * u), Offset(cx, h), 3f * u)
}

private fun DrawScope.drawMagnifyingGlassScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Two shapes being carefully distinguished (circle vs diamond)
    drawCircle(c.secondary.copy(alpha = 0.88f), 20f * u, Offset(cx - 42f * u, cy - 4f * u))
    val diamond = Path().apply {
        moveTo(cx + 42f * u, cy - 26f * u)
        lineTo(cx + 64f * u, cy - 4f * u)
        lineTo(cx + 42f * u, cy + 18f * u)
        lineTo(cx + 20f * u, cy - 4f * u)
        close()
    }
    drawPath(diamond, color = c.primary)
    // Magnifying lens over the boundary
    val lensCenter = Offset(cx + 4f * u, cy - 10f * u)
    drawCircle(Color.White.copy(alpha = 0.16f), 36f * u, lensCenter)
    drawCircle(Color.White, 36f * u, lensCenter, style = Stroke(width = 6f * u))
    drawLine(c.highlight, Offset(lensCenter.x + 25f * u, lensCenter.y + 25f * u), Offset(lensCenter.x + 56f * u, lensCenter.y + 56f * u), 10f * u, StrokeCap.Round)
}

private fun DrawScope.drawSpotlightPillarScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Diagonal warm spotlight cone from top
    val beam = Path().apply {
        moveTo(cx - 16f * u, cy - 84f * u)
        lineTo(cx + 16f * u, cy - 84f * u)
        lineTo(cx + 76f * u, cy + 56f * u)
        lineTo(cx - 76f * u, cy + 56f * u)
        close()
    }
    drawPath(beam, Brush.verticalGradient(listOf(c.highlight.copy(alpha = 0.55f), Color.Transparent)))
    // Secondary dim pillars + 1 brightly highlighted central pillar
    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(cx - 72f * u, cy + 4f * u), Size(28f * u, 42f * u), CornerRadius(6f * u, 6f * u))
    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(cx + 44f * u, cy + 4f * u), Size(28f * u, 42f * u), CornerRadius(6f * u, 6f * u))
    drawRoundRect(c.primary, Offset(cx - 22f * u, cy - 26f * u), Size(44f * u, 72f * u), CornerRadius(10f * u, 10f * u))
    drawCircle(Color.White, 8f * u, Offset(cx, cy - 6f * u))
}

private fun DrawScope.drawSummitFlagScene(w: Float, h: Float, cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Crisp layered mountain peaks
    val backPeak = Path().apply {
        moveTo(w * 0.05f, h); lineTo(cx - 54f * u, cy - 8f * u); lineTo(cx + 18f * u, h); close()
    }
    val mainPeak = Path().apply {
        moveTo(cx - 88f * u, h); lineTo(cx, cy - 36f * u); lineTo(cx + 96f * u, h); close()
    }
    drawPath(backPeak, c.surface.copy(alpha = 0.55f))
    drawPath(mainPeak, c.surface.copy(alpha = 0.90f))
    // Snow cap
    val cap = Path().apply {
        moveTo(cx - 22f * u, cy - 6f * u); lineTo(cx, cy - 36f * u); lineTo(cx + 24f * u, cy - 4f * u); close()
    }
    drawPath(cap, Color.White.copy(alpha = 0.92f))
    // Victory flag planted at summit
    drawLine(Color.White, Offset(cx, cy - 36f * u), Offset(cx, cy - 78f * u), 4f * u, StrokeCap.Round)
    val flag = Path().apply {
        moveTo(cx, cy - 78f * u); lineTo(cx + 40f * u, cy - 66f * u); lineTo(cx, cy - 52f * u); close()
    }
    drawPath(flag, c.secondary)
}

private fun DrawScope.drawSteppingStonesScene(w: Float, h: Float, cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // 5 evenly spaced rising habit stepping stones
    for (i in 0..4) {
        val sx = cx - 84f * u + i * 42f * u
        val sy = cy + 32f * u - i * 16f * u
        val alpha = 0.45f + i * 0.13f
        drawRoundRect(
            color = c.primary.copy(alpha = alpha),
            topLeft = Offset(sx - 16f * u, sy - 8f * u),
            size = Size(32f * u, 16f * u),
            cornerRadius = CornerRadius(8f * u, 8f * u)
        )
        if (i < 4) {
            drawLine(c.highlight.copy(alpha = 0.55f), Offset(sx + 16f * u, sy), Offset(sx + 26f * u, sy - 10f * u), 2.5f * u, StrokeCap.Round)
        }
    }
    drawCircle(c.highlight, 10f * u, Offset(cx + 84f * u, cy - 52f * u))
}

private fun DrawScope.drawInterlockingPuzzleScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Left puzzle block
    drawRoundRect(c.primary, Offset(cx - 64f * u, cy - 32f * u), Size(56f * u, 56f * u), CornerRadius(12f * u, 12f * u))
    // Interlocking tab from left to right
    drawCircle(c.primary, 13f * u, Offset(cx - 4f * u, cy - 4f * u))
    // Right puzzle block locking tightly
    drawRoundRect(c.secondary, Offset(cx + 4f * u, cy - 32f * u), Size(56f * u, 56f * u), CornerRadius(12f * u, 12f * u))
    drawCircle(c.primary, 11f * u, Offset(cx + 4f * u, cy - 4f * u))
    // Golden lock spark at the seam
    drawCircle(c.highlight, 7f * u, Offset(cx, cy - 44f * u))
}

private fun DrawScope.drawBalanceScaleScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Central upright pillar
    drawLine(Color.White.copy(alpha = 0.85f), Offset(cx, cy - 44f * u), Offset(cx, cy + 44f * u), 5f * u, StrokeCap.Round)
    drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(cx - 32f * u, cy + 40f * u), Size(64f * u, 10f * u), CornerRadius(5f * u, 5f * u))
    // Horizontal equilibrium beam
    drawLine(c.primary, Offset(cx - 72f * u, cy - 30f * u), Offset(cx + 72f * u, cy - 30f * u), 6f * u, StrokeCap.Round)
    drawCircle(c.highlight, 9f * u, Offset(cx, cy - 30f * u))
    // Two suspended pans
    listOf(-68f, 68f).forEach { dx ->
        drawLine(Color.White.copy(alpha = 0.65f), Offset(cx + dx * u, cy - 28f * u), Offset(cx + (dx - 16f) * u, cy + 12f * u), 2.2f * u)
        drawLine(Color.White.copy(alpha = 0.65f), Offset(cx + dx * u, cy - 28f * u), Offset(cx + (dx + 16f) * u, cy + 12f * u), 2.2f * u)
        drawArc(c.primary, 0f, 180f, true, Offset(cx + (dx - 22f) * u, cy + 2f * u), Size(44f * u, 22f * u))
    }
}

private fun DrawScope.drawLightbulbMindScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Glowing bulb glass
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White, c.highlight, c.primary), center = Offset(cx, cy - 16f * u), radius = 38f * u),
        radius = 34f * u,
        center = Offset(cx, cy - 16f * u)
    )
    // Bulb socket base
    drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(cx - 16f * u, cy + 16f * u), Size(32f * u, 20f * u), CornerRadius(6f * u, 6f * u))
    // Radiating insight rays
    for (i in 0 until 8) {
        val angle = (i * 45f) * PI.toFloat() / 180f
        drawLine(
            color = c.highlight.copy(alpha = 0.78f),
            start = Offset(cx + cos(angle) * 44f * u, cy - 16f * u + sin(angle) * 44f * u),
            end = Offset(cx + cos(angle) * 58f * u, cy - 16f * u + sin(angle) * 58f * u),
            strokeWidth = 3.5f * u,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSignalNetworkScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Concentric broadcast rings
    listOf(28f, 50f, 72f).forEachIndexed { idx, r ->
        drawCircle(c.primary.copy(alpha = 0.55f - idx * 0.14f), r * u, Offset(cx, cy - 6f * u), style = Stroke(3f * u))
    }
    // Connected satellite nodes
    val nodes = listOf(Offset(cx - 66f * u, cy - 36f * u), Offset(cx + 68f * u, cy - 28f * u), Offset(cx - 56f * u, cy + 32f * u), Offset(cx + 58f * u, cy + 28f * u))
    nodes.forEach { n ->
        drawLine(Color.White.copy(alpha = 0.35f), Offset(cx, cy - 6f * u), n, 2.2f * u)
        drawCircle(c.secondary, 9f * u, n)
        drawCircle(Color.White, 4f * u, n)
    }
    drawCircle(c.highlight, 14f * u, Offset(cx, cy - 6f * u))
}

private fun DrawScope.drawInterlockingGearsScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    fun drawGear(gx: Float, gy: Float, r: Float, teeth: Int, col: Color) {
        for (i in 0 until teeth) {
            val a = (i * 360f / teeth) * PI.toFloat() / 180f
            drawLine(col, Offset(gx + cos(a) * r * 0.8f, gy + sin(a) * r * 0.8f), Offset(gx + cos(a) * r * 1.25f, gy + sin(a) * r * 1.25f), 7f * u, StrokeCap.Round)
        }
        drawCircle(col, r, Offset(gx, gy))
        drawCircle(c.bgMid, r * 0.42f, Offset(gx, gy))
    }
    drawGear(cx - 24f * u, cy - 6f * u, 28f * u, 8, c.primary)
    drawGear(cx + 30f * u, cy + 12f * u, 21f * u, 6, c.secondary)
}

private fun DrawScope.drawHourglassScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    val topTri = Path().apply {
        moveTo(cx - 34f * u, cy - 48f * u); lineTo(cx + 34f * u, cy - 48f * u); lineTo(cx, cy - 4f * u); close()
    }
    val botTri = Path().apply {
        moveTo(cx, cy - 4f * u); lineTo(cx + 34f * u, cy + 40f * u); lineTo(cx - 34f * u, cy + 40f * u); close()
    }
    drawPath(topTri, Color.White.copy(alpha = 0.22f))
    drawPath(botTri, c.primary.copy(alpha = 0.65f))
    drawPath(topTri, Color.White, style = Stroke(3.5f * u, join = StrokeJoin.Round))
    drawPath(botTri, Color.White, style = Stroke(3.5f * u, join = StrokeJoin.Round))
    drawRoundRect(c.highlight, Offset(cx - 40f * u, cy - 54f * u), Size(80f * u, 8f * u), CornerRadius(4f * u, 4f * u))
    drawRoundRect(c.highlight, Offset(cx - 40f * u, cy + 38f * u), Size(80f * u, 8f * u), CornerRadius(4f * u, 4f * u))
}

private fun DrawScope.drawKeyAndLockScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Modern glowing padlock on right
    drawArc(Color.White, 180f, 180f, false, Offset(cx + 12f * u, cy - 44f * u), Size(38f * u, 38f * u), style = Stroke(6f * u, cap = StrokeCap.Round))
    drawRoundRect(c.secondary, Offset(cx + 4f * u, cy - 22f * u), Size(54f * u, 48f * u), CornerRadius(12f * u, 12f * u))
    drawCircle(c.surface, 7f * u, Offset(cx + 31f * u, cy))
    // Golden matching key on left
    drawCircle(c.highlight, 18f * u, Offset(cx - 50f * u, cy + 2f * u), style = Stroke(6f * u))
    drawLine(c.highlight, Offset(cx - 32f * u, cy + 2f * u), Offset(cx - 4f * u, cy + 2f * u), 6f * u, StrokeCap.Round)
    drawLine(c.highlight, Offset(cx - 14f * u, cy + 2f * u), Offset(cx - 14f * u, cy + 14f * u), 5f * u, StrokeCap.Round)
}

private fun DrawScope.drawEcoCycleScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Circular sustainable cycle ring
    drawCircle(c.primary.copy(alpha = 0.45f), 54f * u, Offset(cx, cy - 4f * u), style = Stroke(5f * u))
    // Vibrant stylized tree / leaf in center
    drawLine(Color(0xFF92400E), Offset(cx, cy + 32f * u), Offset(cx, cy - 6f * u), 8f * u, StrokeCap.Round)
    drawCircle(c.primary, 24f * u, Offset(cx, cy - 22f * u))
    drawCircle(c.secondary, 14f * u, Offset(cx + 40f * u, cy - 44f * u))
}

private fun DrawScope.drawStackingCrystalsScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Rising accumulated blocks / bars
    val heights = listOf(22f, 38f, 54f, 74f)
    heights.forEachIndexed { idx, bh ->
        val bx = cx - 64f * u + idx * 36f * u
        drawRoundRect(
            color = if (idx == 3) c.highlight else c.primary.copy(alpha = 0.55f + idx * 0.14f),
            topLeft = Offset(bx, cy + 36f * u - bh * u),
            size = Size(26f * u, bh * u),
            cornerRadius = CornerRadius(7f * u, 7f * u)
        )
    }
}

private fun DrawScope.drawClashingArrowsScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Opposing arrows meeting in tension
    drawLine(c.primary, Offset(cx - 78f * u, cy - 4f * u), Offset(cx - 14f * u, cy - 4f * u), 9f * u, StrokeCap.Round)
    drawLine(c.secondary, Offset(cx + 78f * u, cy - 4f * u), Offset(cx + 14f * u, cy - 4f * u), 9f * u, StrokeCap.Round)
    // Clash spark star in center
    for (i in 0 until 6) {
        val a = (i * 60f) * PI.toFloat() / 180f
        drawLine(c.highlight, Offset(cx, cy - 4f * u), Offset(cx + cos(a) * 20f * u, cy - 4f * u + sin(a) * 20f * u), 3.5f * u, StrokeCap.Round)
    }
}

private fun DrawScope.drawPrismLensScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Incoming white beam -> crystal prism -> 3 distinct viewpoint rays
    drawLine(Color.White, Offset(cx - 94f * u, cy + 8f * u), Offset(cx - 16f * u, cy - 4f * u), 5f * u, StrokeCap.Round)
    listOf(Color(0xFFF472B6) to -28f, Color(0xFFFACC15) to -4f, Color(0xFF38BDF8) to 20f).forEach { (col, dy) ->
        drawLine(col, Offset(cx + 16f * u, cy - 4f * u), Offset(cx + 94f * u, cy + dy * u), 5f * u, StrokeCap.Round)
    }
    val prism = Path().apply {
        moveTo(cx, cy - 46f * u); lineTo(cx + 38f * u, cy + 28f * u); lineTo(cx - 38f * u, cy + 28f * u); close()
    }
    drawPath(prism, Color.White.copy(alpha = 0.24f))
    drawPath(prism, Color.White, style = Stroke(3.5f * u, join = StrokeJoin.Round))
}

private fun DrawScope.drawGlobeCompassScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    drawCircle(c.primary.copy(alpha = 0.26f), 48f * u, Offset(cx, cy - 4f * u))
    drawCircle(Color.White, 48f * u, Offset(cx, cy - 4f * u), style = Stroke(4f * u))
    // Compass needle pointing NE
    val north = Path().apply {
        moveTo(cx, cy - 38f * u); lineTo(cx + 10f * u, cy - 4f * u); lineTo(cx - 10f * u, cy - 4f * u); close()
    }
    val south = Path().apply {
        moveTo(cx, cy + 30f * u); lineTo(cx + 10f * u, cy - 4f * u); lineTo(cx - 10f * u, cy - 4f * u); close()
    }
    drawPath(north, c.secondary)
    drawPath(south, Color.White.copy(alpha = 0.85f))
    drawCircle(c.highlight, 6f * u, Offset(cx, cy - 4f * u))
}

private fun DrawScope.drawWarmBeaconScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    // Rising radiant sun over calm horizon
    drawCircle(c.highlight, 30f * u, Offset(cx, cy + 4f * u))
    for (i in 0..4) {
        val a = (200f + i * 35f) * PI.toFloat() / 180f
        drawLine(c.primary, Offset(cx + cos(a) * 38f * u, cy + 4f * u + sin(a) * 38f * u), Offset(cx + cos(a) * 56f * u, cy + 4f * u + sin(a) * 56f * u), 4f * u, StrokeCap.Round)
    }
    drawRoundRect(c.surface.copy(alpha = 0.78f), Offset(cx - 90f * u, cy + 16f * u), Size(180f * u, 28f * u), CornerRadius(12f * u, 12f * u))
}

private fun DrawScope.drawSortedCategoriesScene(cx: Float, cy: Float, u: Float, c: SceneColorSet) {
    val cols = listOf(c.primary, c.secondary, c.highlight)
    cols.forEachIndexed { idx, col ->
        val bx = cx - 72f * u + idx * 52f * u
        drawRoundRect(col.copy(alpha = 0.88f), Offset(bx, cy - 30f * u), Size(40f * u, 56f * u), CornerRadius(10f * u, 10f * u))
        drawLine(Color.White.copy(alpha = 0.8f), Offset(bx + 8f * u, cy - 12f * u), Offset(bx + 32f * u, cy - 12f * u), 3f * u, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.55f), Offset(bx + 8f * u, cy + 2f * u), Offset(bx + 26f * u, cy + 2f * u), 3f * u, StrokeCap.Round)
    }
}
