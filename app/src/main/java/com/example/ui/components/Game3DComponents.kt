package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PronunciationReport
import com.example.model.TokenStatus
import com.example.model.UserProgress
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.SpartaRedShadow
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow

@Composable
fun Tactile3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AdventureEmerald,
    shadowColor: Color = AdventureEmeraldShadow,
    contentColor: Color = Color.White,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    tag: String = "tactile_3d_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val lipHeight = 5.dp
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) lipHeight else 0.dp,
        animationSpec = spring(stiffness = 700f),
        label = "btn3dOffset"
    )

    val actualBg = if (enabled) containerColor else Color(0xFFBDC5CE)
    val actualShadow = if (enabled) shadowColor else Color(0xFF9AA3AD)

    Box(
        modifier = modifier
            .height(height + lipHeight)
            .testTag(tag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // 3D Extruded Bottom Lip Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(18.dp))
                .background(actualShadow)
        )
        // Tactile Top Face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = pressOffset)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            actualBg.copy(alpha = 0.96f),
                            actualBg
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Elevated3DCard(
    modifier: Modifier = Modifier,
    accentShadow: Color = Color(0xFFD5DFEA),
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    tiltDegrees: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.graphicsLayer {
            cameraDistance = 14f * density
            rotationX = tiltDegrees
        }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(accentShadow)
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

@Composable
fun GameHudBar(
    progress: UserProgress,
    onCoinsClick: () -> Unit,
    onHeartsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudBadgeItem(
                icon = Icons.Default.LocalFireDepartment,
                tint = CoralOrange,
                value = "${progress.streakCount}일",
                label = "스트릭",
                onClick = {}
            )
            HudBadgeItem(
                icon = Icons.Default.MonetizationOn,
                tint = TrophyGoldShadow,
                value = "${progress.coins}",
                label = "코인",
                onClick = onCoinsClick,
                tag = "hud_coins_button"
            )
            HudBadgeItem(
                icon = Icons.Default.Bolt,
                tint = SkyQuestBlue,
                value = "${progress.totalXp}",
                label = "XP",
                onClick = {}
            )
            HudBadgeItem(
                icon = Icons.Default.Favorite,
                tint = SpartaRed,
                value = "${progress.hearts}/5",
                label = "하트",
                onClick = onHeartsClick,
                tag = "hud_hearts_button"
            )
        }
    }
}

@Composable
private fun HudBadgeItem(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String,
    onClick: () -> Unit,
    tag: String = "hud_item"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(4.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun VoiceWaveformOrb3D(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "voice_mic_orb"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceOrbPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val dynamicScale = when {
        isListening -> (1.0f + (rmsDb / 28f)).coerceIn(1.0f, 1.32f)
        isSpeaking -> pulse
        else -> 1.0f
    }
    val mainColor = when {
        isListening -> SpartaRed
        isSpeaking -> SkyQuestBlue
        else -> AdventureEmerald
    }
    val lipColor = when {
        isListening -> SpartaRedShadow
        isSpeaking -> SkyQuestBlueShadow
        else -> AdventureEmeraldShadow
    }

    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .scale(dynamicScale)
                    .clip(CircleShape)
                    .background(mainColor.copy(alpha = 0.22f))
            )
        }
        Box(
            modifier = Modifier
                .size(68.dp)
                .offset(y = 4.dp)
                .clip(CircleShape)
                .background(lipColor)
        )
        Box(
            modifier = Modifier
                .size(68.dp)
                .testTag(tag)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(mainColor.copy(alpha = 0.9f), mainColor)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "녹음 중지" else "말하기 시작",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PronunciationReportCard(
    report: PronunciationReport,
    onCompareNativeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradeColor = when {
        report.overallScore >= 86 -> AdventureEmerald
        report.overallScore >= 72 -> TrophyGoldShadow
        else -> SpartaRed
    }

    Elevated3DCard(
        modifier = modifier.fillMaxWidth(),
        accentShadow = gradeColor.copy(alpha = 0.35f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(gradeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = report.fluencyGrade,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "실시간 발음 정밀 분석 ${report.overallScore}점",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "유창성 속도 ${report.estimatedWpm} WPM · ${if (report.passed) "통과!" else "교정 필요"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SkyQuestBlue.copy(alpha = 0.14f),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCompareNativeClick)
                        .testTag("compare_native_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "원어민 발음 비교 듣기",
                            tint = SkyQuestBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "원어민 비교",
                            style = MaterialTheme.typography.labelMedium,
                            color = SkyQuestBlue
                        )
                    }
                }
            }

            if (report.tokens.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    report.tokens.forEach { token ->
                        val (bg, fg) = when (token.status) {
                            TokenStatus.EXACT -> AdventureEmerald.copy(alpha = 0.16f) to AdventureEmeraldShadow
                            TokenStatus.CLOSE -> TrophyGold.copy(alpha = 0.25f) to Color(0xFF8A6200)
                            TokenStatus.MISSED -> SpartaRed.copy(alpha = 0.16f) to SpartaRedShadow
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bg
                        ) {
                            Text(
                                text = "${token.targetWord} ${token.similarityPercent}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = fg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "내 인식 발화: \"${report.recognizedText.ifBlank { "(음성 없음)" }}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = report.phoneticFeedbackKo,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "발음 교정 팁: ${report.mouthTipKo}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyQuestBlueShadow
                    )
                }
            }
        }
    }
}
