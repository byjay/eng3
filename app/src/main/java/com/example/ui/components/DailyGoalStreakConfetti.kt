package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.SquirrelJobHero
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

const val DAILY_STREAK_WORD_STEP = 10

data class StreakMilestoneEvent(
    val newStreakCount: Int,
    val todayWordsCompleted: Int,
    val triggeredAtMs: Long = System.currentTimeMillis()
)

@Composable
fun DailyGoalStreakCardSection(
    streakCount: Int,
    todayWordsCompleted: Int,
    modifier: Modifier = Modifier
) {
    val wordsInStep = todayWordsCompleted % DAILY_STREAK_WORD_STEP
    val displayedStepWords = if (todayWordsCompleted > 0 && wordsInStep == 0) {
        DAILY_STREAK_WORD_STEP
    } else {
        wordsInStep
    }
    val targetFraction = (displayedStepWords.toFloat() / DAILY_STREAK_WORD_STEP.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "daily_goal_streak_progress"
    )
    val remainingWords = if (wordsInStep == 0) DAILY_STREAK_WORD_STEP else (DAILY_STREAK_WORD_STEP - wordsInStep)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF8EC),
        border = BorderStroke(1.5.dp, TrophyGold.copy(alpha = 0.72f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_goal_streak_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CoralOrange.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Daily Goal Streak",
                                tint = CoralOrangeShadow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Daily Goal Streak",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = CoralOrangeShadow
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = CoralOrange,
                        modifier = Modifier
                            .testTag("daily_goal_streak_counter")
                            .semantics {
                                contentDescription = "Daily Goal Streak $streakCount"
                            }
                    ) {
                        Text(
                            text = "${streakCount}회 달성",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "$displayedStepWords/$DAILY_STREAK_WORD_STEP 단어",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TrophyGoldShadow,
                    modifier = Modifier.testTag("daily_goal_words_counter")
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .testTag("daily_goal_progress_bar"),
                color = CoralOrange,
                trackColor = TrophyGold.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (todayWordsCompleted > 0 && wordsInStep == 0) {
                        "10단어 목표 달성! 다음 스트릭까지 ${DAILY_STREAK_WORD_STEP}단어"
                    } else {
                        "다음 스트릭 마일스톤까지 ${remainingWords}단어 남았어요!"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "오늘 총 ${todayWordsCompleted}단어",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AdventureEmeraldShadow
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val startXFraction: Float,
    val startDelay: Float,
    val fallSpeed: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val swayPhase: Float,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val widthDp: Float,
    val heightDp: Float,
    val isCircle: Boolean,
    val color: Color
)

@Composable
fun StreakMilestoneConfettiOverlay(
    event: StreakMilestoneEvent,
    hero: SquirrelJobHero,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val confettiProgress = remember(event.triggeredAtMs) { Animatable(0f) }
    val cardScale = remember(event.triggeredAtMs) { Animatable(0.72f) }

    val particles = remember(event.triggeredAtMs) {
        val rng = Random(event.triggeredAtMs)
        val palette = listOf(
            Color(0xFFFF4D6D),
            Color(0xFFFFB703),
            Color(0xFF14B8A6),
            Color(0xFF3B82F6),
            Color(0xFFA855F7),
            Color(0xFF22C55E),
            Color(0xFFF97316),
            Color(0xFFEC4899)
        )
        List(76) {
            ConfettiParticle(
                startXFraction = rng.nextFloat(),
                startDelay = rng.nextFloat() * 0.28f,
                fallSpeed = 0.72f + rng.nextFloat() * 0.65f,
                swayAmplitude = 18f + rng.nextFloat() * 44f,
                swayFrequency = 1.8f + rng.nextFloat() * 3.0f,
                swayPhase = rng.nextFloat() * (2f * PI.toFloat()),
                initialRotation = rng.nextFloat() * 360f,
                rotationSpeed = (if (rng.nextBoolean()) 1f else -1f) * (240f + rng.nextFloat() * 420f),
                widthDp = 6f + rng.nextFloat() * 7f,
                heightDp = 9f + rng.nextFloat() * 11f,
                isCircle = rng.nextFloat() < 0.22f,
                color = palette[rng.nextInt(palette.size)]
            )
        }
    }

    LaunchedEffect(event.triggeredAtMs) {
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    LaunchedEffect(event.triggeredAtMs) {
        confettiProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4200, easing = LinearEasing)
        )
    }

    val milestoneBadgeText = when {
        event.newStreakCount >= 30 -> "LEGENDARY 30+ STREAK!"
        event.newStreakCount >= 10 -> "10+ STREAK CHAMPION!"
        event.newStreakCount >= 5 -> "5+ VOCAB MASTER!"
        event.newStreakCount >= 3 -> "3+ STREAK HERO!"
        else -> "10-WORD DAILY GOAL ACHIEVED!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
            .testTag("confetti_celebration_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // GPU-only Canvas Confetti — reads confettiProgress.value ONLY in draw scope (0 recompositions)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tGlobal = confettiProgress.value
            val densityScale = density
            particles.forEach { p ->
                val localT = ((tGlobal - p.startDelay) / (1f - p.startDelay)).coerceIn(0f, 1f)
                if (localT <= 0f) return@forEach

                val y = -40f * densityScale + localT * p.fallSpeed * (size.height + 160f * densityScale)
                val x = p.startXFraction * size.width +
                    sin(localT * p.swayFrequency * 2f * PI.toFloat() + p.swayPhase) * p.swayAmplitude * densityScale
                val alpha = when {
                    localT < 0.08f -> localT / 0.08f
                    localT > 0.82f -> ((1f - localT) / 0.18f).coerceIn(0f, 1f)
                    else -> 1f
                }
                val tiltScaleY = (0.35f + 0.65f * kotlin.math.abs(cos(localT * 6f * PI.toFloat() + p.swayPhase)))
                val wPx = p.widthDp * densityScale
                val hPx = p.heightDp * densityScale * tiltScaleY
                val rot = p.initialRotation + localT * p.rotationSpeed

                if (y <= size.height + 60f * densityScale && alpha > 0.01f) {
                    withTransform({
                        translate(left = x, top = y)
                        rotate(degrees = rot, pivot = Offset.Zero)
                    }) {
                        if (p.isCircle) {
                            drawCircle(
                                color = p.color.copy(alpha = alpha),
                                radius = wPx * 0.55f,
                                center = Offset.Zero
                            )
                        } else {
                            drawRect(
                                color = p.color.copy(alpha = alpha),
                                topLeft = Offset(-wPx / 2f, -hPx / 2f),
                                size = Size(wPx, hPx)
                            )
                        }
                    }
                }
            }
        }

        // Center Celebration Modal Card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFFFDF8),
            border = BorderStroke(2.5.dp, TrophyGold),
            shadowElevation = 14.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val s = cardScale.value
                    scaleX = s
                    scaleY = s
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFF7E0), Color(0xFFFFFFFF))
                        )
                    )
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = CoralOrange.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = CoralOrangeShadow,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = milestoneBadgeText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = CoralOrangeShadow
                        )
                    }
                }

                SquirrelHeroAvatar(
                    hero = hero,
                    size = 86.dp,
                    mood = SquirrelMoodState.VICTORY_DANCE,
                    borderWidth = 3.dp
                )

                Text(
                    text = "Congratulations!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF1B2834),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("confetti_congrats_title")
                )

                Text(
                    text = "10단어 일일 목표를 완벽하게 달성했어요!\n${hero.nameKo}도 함께 박수 치고 있어요!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFFF4DC),
                    border = BorderStroke(1.5.dp, TrophyGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Daily Goal Streak",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CoralOrangeShadow
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = CoralOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "${event.newStreakCount}",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = CoralOrangeShadow
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(TrophyGold.copy(alpha = 0.5f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "오늘 완료 단어",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AdventureEmeraldShadow
                            )
                            Text(
                                text = "${event.todayWordsCompleted}단어",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = AdventureEmeraldShadow
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Tactile3DButton(
                    text = "계속 학습하기",
                    containerColor = AdventureEmerald,
                    shadowColor = AdventureEmeraldShadow,
                    height = 50.dp,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "confetti_dismiss_btn",
                    onClick = onDismiss
                )
            }
        }
    }
}
