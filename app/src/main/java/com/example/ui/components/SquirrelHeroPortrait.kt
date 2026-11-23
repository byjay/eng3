package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.SquirrelJobHero
import kotlin.math.PI
import kotlin.math.sin

/**
 * Context-aware situational animation moods for the White Pygmy Squirrel protagonist.
 */
enum class SquirrelMoodState {
    IDLE,
    HAPPY_CHEER,
    ATTACK_LUNGE,
    SKILL_CAST,
    GUARD_SHIELD,
    HURT_SHAKE,
    VICTORY_DANCE,
    LISTENING_TILT
}

/**
 * Renders an exact cropped portrait for one of the 10 adorable White Pygmy Squirrel
 * fantasy job heroes with GPU-only contextual animations (zero recomposition lag!).
 */
@Composable
fun SquirrelHeroAvatar(
    hero: SquirrelJobHero,
    size: Dp,
    modifier: Modifier = Modifier,
    mood: SquirrelMoodState = SquirrelMoodState.IDLE,
    shape: Shape = CircleShape,
    borderWidth: Dp = 2.5.dp,
    borderColor: Color = Color(hero.accentHex),
    animateIdle: Boolean = true
) {
    val sheetBitmap = ImageBitmap.imageResource(id = hero.sheetRes)
    val cellMetrics = remember(sheetBitmap.width, sheetBitmap.height, hero.columnIndex) {
        val totalW = sheetBitmap.width
        val totalH = sheetBitmap.height
        val colW = totalW / 5
        val trimX = (colW * 0.035f).toInt()
        val srcX = (hero.columnIndex * colW + trimX).coerceIn(0, totalW - 1)
        val srcW = (colW - trimX * 2).coerceAtLeast(1)
        val topTrim = (totalH * 0.14f).toInt()
        val bottomTrim = (totalH * 0.12f).toInt()
        val srcH = (totalH - topTrim - bottomTrim).coerceAtLeast(1)
        QuadCrop(
            srcOffset = IntOffset(srcX, topTrim),
            srcSize = IntSize(srcW, srcH)
        )
    }

    // GPU-only breathing & lively motion (never read inside composition body!)
    val infinite = rememberInfiniteTransition(label = "squirrelMoodLoop")
    val phaseState = infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "squirrelBreath"
    )

    val targetScale = when (mood) {
        SquirrelMoodState.ATTACK_LUNGE -> 1.12f
        SquirrelMoodState.SKILL_CAST -> 1.15f
        SquirrelMoodState.VICTORY_DANCE -> 1.10f
        SquirrelMoodState.HAPPY_CHEER -> 1.07f
        SquirrelMoodState.HURT_SHAKE -> 0.92f
        SquirrelMoodState.GUARD_SHIELD -> 1.04f
        SquirrelMoodState.LISTENING_TILT -> 1.06f
        SquirrelMoodState.IDLE -> 1.0f
    }
    val targetOffsetX = when (mood) {
        SquirrelMoodState.ATTACK_LUNGE -> 18f
        SquirrelMoodState.HURT_SHAKE -> -14f
        else -> 0f
    }
    val targetTilt = when (mood) {
        SquirrelMoodState.ATTACK_LUNGE -> 9f
        SquirrelMoodState.SKILL_CAST -> -7f
        SquirrelMoodState.HURT_SHAKE -> -11f
        SquirrelMoodState.LISTENING_TILT -> 7f
        SquirrelMoodState.HAPPY_CHEER -> -5f
        else -> 0f
    }

    val animScaleState = animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 320f),
        label = "sqScale"
    )
    val animOffsetXState = animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 360f),
        label = "sqOffsetX"
    )
    val animTiltState = animateFloatAsState(
        targetValue = targetTilt,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 280f),
        label = "sqTilt"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val p = if (animateIdle) phaseState.value else 0f
                val hopAmp = when (mood) {
                    SquirrelMoodState.VICTORY_DANCE -> 9.5f
                    SquirrelMoodState.HAPPY_CHEER -> 6.5f
                    SquirrelMoodState.SKILL_CAST -> 5.5f
                    SquirrelMoodState.IDLE -> 3.0f
                    else -> 2.0f
                }
                val freq = if (mood == SquirrelMoodState.VICTORY_DANCE) 2.2f else 1f
                val wave = sin(p * freq)
                translationX = animOffsetXState.value
                translationY = -wave * hopAmp
                val squash = if (animateIdle) 1f + wave * 0.025f else 1f
                scaleX = animScaleState.value * (2f - squash)
                scaleY = animScaleState.value * squash
                rotationZ = animTiltState.value + if (animateIdle) sin(p * 0.5f) * 2.2f else 0f
            }
            .clip(shape)
            .background(Color(0xFFFDF3DF))
            .border(borderWidth, borderColor, shape)
            .semantics {
                contentDescription = "${hero.nameKo} ${hero.jobTitleKo} 흰색 피그미 다람쥐"
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dstW = this.size.width.toInt().coerceAtLeast(1)
            val dstH = this.size.height.toInt().coerceAtLeast(1)
            val srcAspect = cellMetrics.srcSize.width.toFloat() / cellMetrics.srcSize.height.toFloat()
            val dstAspect = dstW.toFloat() / dstH.toFloat()

            val (cropOffset, cropSize) = if (srcAspect > dstAspect) {
                val targetW = (cellMetrics.srcSize.height * dstAspect).toInt().coerceAtLeast(1)
                val dx = (cellMetrics.srcSize.width - targetW) / 2
                IntOffset(cellMetrics.srcOffset.x + dx, cellMetrics.srcOffset.y) to
                    IntSize(targetW, cellMetrics.srcSize.height)
            } else {
                val targetH = (cellMetrics.srcSize.width / dstAspect).toInt().coerceAtLeast(1)
                val dy = (cellMetrics.srcSize.height - targetH) / 2
                IntOffset(cellMetrics.srcOffset.x, cellMetrics.srcOffset.y + dy) to
                    IntSize(cellMetrics.srcSize.width, targetH)
            }

            drawImage(
                image = sheetBitmap,
                srcOffset = cropOffset,
                srcSize = cropSize,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(dstW, dstH)
            )
        }
    }
}

@Composable
fun SquirrelHeroCardPortrait(
    hero: SquirrelJobHero,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val sheetBitmap = ImageBitmap.imageResource(id = hero.sheetRes)
    val cellMetrics = remember(sheetBitmap.width, sheetBitmap.height, hero.columnIndex) {
        val totalW = sheetBitmap.width
        val totalH = sheetBitmap.height
        val colW = totalW / 5
        val trimX = (colW * 0.04f).toInt()
        val srcX = (hero.columnIndex * colW + trimX).coerceIn(0, totalW - 1)
        val srcW = (colW - trimX * 2).coerceAtLeast(1)
        val topTrim = (totalH * 0.10f).toInt()
        val bottomTrim = (totalH * 0.10f).toInt()
        val srcH = (totalH - topTrim - bottomTrim).coerceAtLeast(1)
        QuadCrop(IntOffset(srcX, topTrim), IntSize(srcW, srcH))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFFDF3DF))
            .semantics {
                contentDescription = "${hero.nameKo} ${hero.jobTitleKo}"
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dstW = this.size.width.toInt().coerceAtLeast(1)
            val dstH = this.size.height.toInt().coerceAtLeast(1)
            val srcAspect = cellMetrics.srcSize.width.toFloat() / cellMetrics.srcSize.height.toFloat()
            val dstAspect = dstW.toFloat() / dstH.toFloat()

            val (cropOffset, cropSize) = if (srcAspect > dstAspect) {
                val targetW = (cellMetrics.srcSize.height * dstAspect).toInt().coerceAtLeast(1)
                val dx = (cellMetrics.srcSize.width - targetW) / 2
                IntOffset(cellMetrics.srcOffset.x + dx, cellMetrics.srcOffset.y) to
                    IntSize(targetW, cellMetrics.srcSize.height)
            } else {
                val targetH = (cellMetrics.srcSize.width / dstAspect).toInt().coerceAtLeast(1)
                val dy = (cellMetrics.srcSize.height - targetH) / 2
                IntOffset(cellMetrics.srcOffset.x, cellMetrics.srcOffset.y + dy) to
                    IntSize(cellMetrics.srcSize.width, targetH)
            }

            drawImage(
                image = sheetBitmap,
                srcOffset = cropOffset,
                srcSize = cropSize,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(dstW, dstH)
            )
        }
    }
}

private data class QuadCrop(
    val srcOffset: IntOffset,
    val srcSize: IntSize
)
