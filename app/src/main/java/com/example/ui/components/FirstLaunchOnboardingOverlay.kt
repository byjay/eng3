package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameJuiceAudioEngine
import com.example.engine.LearnerGoalTrack
import com.example.engine.PlayGrowthRetentionEngine
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.TrophyGold

/**
 * 15-second First-Launch Interactive Onboarding designed for Google Play Day-0 retention:
 * - Step 1: Learner nickname + goal track selection
 * - Step 2: Daily word goal commitment (5 / 10 / 20 words)
 * - Step 3: 1-question instant touchstart warm-up quiz for an immediate +50 XP / +30 Coins first win!
 * Always includes a 1-tap skip button so zero users feel trapped.
 */
@Composable
fun FirstLaunchOnboardingOverlay(
    defaultNickname: String,
    onComplete: (nickname: String, goalTrack: LearnerGoalTrack, dailyGoal: Int, earnedWarmupBonus: Boolean) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var nickname by remember { mutableStateOf(defaultNickname.ifBlank { "학습자" }) }
    var selectedTrack by remember { mutableStateOf(LearnerGoalTrack.DAILY_HABIT) }
    var selectedDailyGoal by remember { mutableIntStateOf(10) }
    var warmupAnswered by remember { mutableStateOf(false) }
    var warmupCorrect by remember { mutableStateOf(false) }

    val warmupOptions = remember {
        listOf(
            "결정적인 · 매우 중요한" to true,
            "우연한 · 일시적인" to false,
            "평범한 · 지루한" to false,
            "복잡한 · 혼란스러운" to false
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E3A8A),
                        Color(0xFF172554)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("first_launch_onboarding_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Progress + Instant Skip
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = TrophyGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "15초 맞춤 설정 · ${step + 1}/3",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }

                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("onboarding_skip_btn")
                    ) {
                        Text(
                            text = "바로 시작",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (step + 1) / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = TrophyGold,
                    trackColor = Color.White.copy(alpha = 0.18f),
                    strokeCap = StrokeCap.Round
                )
            }

            // Center Step Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboarding_step_transition"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "어떤 영어 목표부터\n시작해 볼까요?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 34.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "닉네임과 학습 목표를 고르면 매일 3분 맞춤 루틴과 스트릭 보호막 1개를 무료로 드려요.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.82f)
                                )

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "학습자 닉네임",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFBFDBFE)
                                        )
                                        OutlinedTextField(
                                            value = nickname,
                                            onValueChange = { if (it.length <= 12) nickname = it },
                                            singleLine = true,
                                            placeholder = { Text("닉네임 입력 (예: 지민, Alex)") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("onboarding_nickname_input")
                                        )
                                    }
                                }

                                LearnerGoalTrack.entries.forEach { track ->
                                    val selected = selectedTrack == track
                                    val accent = Color(track.accentHex)
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (selected) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) accent else Color.White.copy(alpha = 0.18f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 64.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { selectedTrack = track }
                                            .testTag("onboarding_track_${track.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.titleKo,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = track.subtitleKo,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.78f)
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "선택됨",
                                                    tint = TrophyGold,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "하루 목표를 정하면\n꾸준함이 3배 쉬워져요",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 34.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "부담 없이 시작하고 언제든 바꿀 수 있어요. 하루를 놓쳐도 무료 스트릭 보호막이 기록을 지켜줍니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.82f)
                                )

                                val goalChoices = listOf(
                                    Triple(5, "가볍게 시작 · 하루 5단어", "약 2분 · 습관 만들기 입문"),
                                    Triple(10, "가장 인기 · 하루 10단어 (추천)", "약 3분 · 일일 스트릭 자동 달성"),
                                    Triple(20, "스파르타 집중 · 하루 20단어", "약 6분 · 빠른 등급 상승")
                                )
                                goalChoices.forEach { (goalCount, title, desc) ->
                                    val selected = selectedDailyGoal == goalCount
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (selected) AdventureEmerald.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) AdventureEmerald else Color.White.copy(alpha = 0.18f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 68.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { selectedDailyGoal = goalCount }
                                            .testTag("onboarding_goal_$goalCount")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "선택됨",
                                                    tint = AdventureEmerald,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = GlacierCyan.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, GlacierCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shield,
                                            contentDescription = null,
                                            tint = GlacierCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "웰컴 혜택: 스트릭 보호막 1개 자동 지급!",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FlashOn,
                                        contentDescription = null,
                                        tint = TrophyGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "초고속 터치 퀴즈 1문제 체험!",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TrophyGold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.5.dp, TrophyGold.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "수능·회화 1순위 필수 단어",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFBFDBFE)
                                        )
                                        Text(
                                            text = "crucial",
                                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "\"Daily habits play a crucial role in fluency.\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                warmupOptions.forEachIndexed { idx, (label, isCorrect) ->
                                    val bg = when {
                                        warmupAnswered && isCorrect -> AdventureEmerald.copy(alpha = 0.34f)
                                        warmupAnswered && !isCorrect -> Color.White.copy(alpha = 0.06f)
                                        else -> Color.White.copy(alpha = 0.12f)
                                    }
                                    val border = when {
                                        warmupAnswered && isCorrect -> AdventureEmerald
                                        else -> Color.White.copy(alpha = 0.25f)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = bg,
                                        border = BorderStroke(1.5.dp, border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .instantQuizTouch(enabled = !warmupAnswered) {
                                                warmupAnswered = true
                                                warmupCorrect = isCorrect
                                                if (isCorrect) {
                                                    GameJuiceAudioEngine.playComboHit(2)
                                                } else {
                                                    GameJuiceAudioEngine.playWrongThud()
                                                }
                                            }
                                            .testTag("onboarding_warmup_opt_$idx")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White.copy(alpha = 0.18f),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${idx + 1}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = warmupAnswered) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = (if (warmupCorrect) AdventureEmerald else StudioCoral).copy(alpha = 0.22f),
                                        border = BorderStroke(
                                            1.2.dp,
                                            if (warmupCorrect) AdventureEmerald else StudioCoral
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (warmupCorrect) {
                                                "완벽해요! 첫 승리 보너스 +${PlayGrowthRetentionEngine.ONBOARDING_BONUS_XP} XP · +${PlayGrowthRetentionEngine.ONBOARDING_BONUS_COINS} 코인 획득!"
                                            } else {
                                                "crucial = 결정적인 · 매우 중요한! 첫 도전 보너스 +${PlayGrowthRetentionEngine.ONBOARDING_BONUS_XP} XP를 드릴게요!"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Primary CTA
            Button(
                onClick = {
                    if (step < 2) {
                        step += 1
                    } else {
                        onComplete(
                            nickname.trim().ifBlank { "학습자" },
                            selectedTrack,
                            selectedDailyGoal,
                            warmupAnswered
                        )
                    }
                },
                enabled = step < 2 || warmupAnswered,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (step == 2) AdventureEmerald else TrophyGold,
                    contentColor = Color(0xFF0F172A),
                    disabledContainerColor = Color.White.copy(alpha = 0.2f),
                    disabledContentColor = Color.White.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("onboarding_next_btn")
            ) {
                Text(
                    text = when (step) {
                        0 -> "다음 단계 →"
                        1 -> "1문제 체험하고 보너스 받기 →"
                        else -> "보너스 받고 바로 학습 시작!"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
            }
        }
    }
}
