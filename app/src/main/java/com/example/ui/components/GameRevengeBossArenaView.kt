package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameJuiceAudioEngine
import com.example.engine.GameRevengeLoopEngine
import com.example.engine.RecallGrade
import com.example.engine.RevengeBattleState
import com.example.engine.RevengeQuestionMode
import com.example.engine.RevengeSessionSummary
import com.example.engine.RewardChestRarity
import com.example.engine.WordLearningProgress
import com.example.model.VocabWord
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.TrophyGold
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Game-First + FSRS + Hooked Micro-Session Arena ("오답 몬스터 복수전"):
 * - 3-second instant play CTA prioritizing FSRS due/lapsed vocabulary
 * - 4-mode sensory rotation (Speed Match / Reverse / Cloze Context / Listening)
 * - Rising-pitch procedural audio synth, Canvas particle burst, Fever multiplier,
 *   Dynamic Flow Channel auto-hint & distractor removal, and Variable Reward Chests.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameRevengeBossArenaView(
    candidateWords: List<VocabWord>,
    wordProgress: Map<String, WordLearningProgress>,
    onSpeakEnglish: (String) -> Unit,
    onRecordFsrsWord: (VocabWord, RecallGrade, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var battleState by remember { mutableStateOf<RevengeBattleState?>(null) }
    var sessionSummary by remember { mutableStateOf<RevengeSessionSummary?>(null) }
    var chestOpened by remember { mutableStateOf(false) }
    var bossSeed by remember { mutableIntStateOf(0) }
    var sessionsClearedToday by remember { mutableIntStateOf(0) }
    var bestComboToday by remember { mutableIntStateOf(0) }
    var audioMuted by remember { mutableStateOf(GameJuiceAudioEngine.isMuted()) }
    var questionShownAtMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var particleBurstNonce by remember { mutableIntStateOf(0) }

    val shakeOffsetX = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(1f) }

    LaunchedEffect(particleBurstNonce) {
        if (particleBurstNonce > 0) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        }
    }

    val dueOrWeakCount = remember(candidateWords, wordProgress) {
        val now = System.currentTimeMillis()
        candidateWords.count { word ->
            val p = wordProgress[word.id]
            p != null && (p.lapseCount > 0 || p.dueAtMs <= now || p.fsrsStabilityDays < 3.0)
        }
    }
    val masteryCodexPercent = remember(candidateWords.size, wordProgress) {
        GameRevengeLoopEngine.computeMasteryCodexPercent(candidateWords.size, wordProgress)
    }
    val masteredCount = remember(wordProgress) {
        wordProgress.values.count { it.repetition >= 2 || it.fsrsStabilityDays >= 4.0 }
    }
    val dailyMissions = remember(sessionsClearedToday, bestComboToday, masteredCount) {
        GameRevengeLoopEngine.buildDailyMicroMissions(
            sessionsClearedToday = sessionsClearedToday,
            bestComboToday = bestComboToday,
            masteredWordCount = masteredCount
        )
    }

    fun launchNewBattle() {
        if (candidateWords.size < 4) return
        sessionSummary = null
        chestOpened = false
        val started = GameRevengeLoopEngine.startBattle(
            candidateWords = candidateWords,
            progressByWordId = wordProgress,
            sessionLength = GameRevengeLoopEngine.DEFAULT_SESSION_LENGTH,
            bossSeed = bossSeed
        )
        bossSeed += 1
        questionShownAtMs = System.currentTimeMillis()
        battleState = started
    }

    // Trigger soft shake animation when an answer is missed
    val currentBattle = battleState
    LaunchedEffect(currentBattle?.lastOutcome) {
        val outcome = currentBattle?.lastOutcome ?: return@LaunchedEffect
        if (!outcome.isCorrect) {
            shakeOffsetX.snapTo(0f)
            listOf(-14f, 14f, -10f, 10f, -4f, 0f).forEach { target ->
                shakeOffsetX.animateTo(target, animationSpec = tween(36))
            }
        }
    }

    // Auto-speak target word on LISTENING_SENSE turns
    LaunchedEffect(currentBattle?.currentIndex, currentBattle?.isFinished) {
        val q = currentBattle?.currentQuestion ?: return@LaunchedEffect
        if (!currentBattle.isFinished) {
            questionShownAtMs = System.currentTimeMillis()
            if (q.mode == RevengeQuestionMode.LISTENING_SENSE) {
                delay(180L)
                onSpeakEnglish(q.targetWord.word)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("game_revenge_boss_arena"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sound Toggle + Header Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = SpartaRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "오답 몬스터 복수전 · Game-First SRS",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { audioMuted = GameJuiceAudioEngine.toggleMuted() }
                    .testTag("revenge_audio_mute_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (audioMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = if (audioMuted) "효과음 음소거됨" else "효과음 켜짐",
                        tint = if (audioMuted) MaterialTheme.colorScheme.onSurfaceVariant else StudioCobalt,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (audioMuted) "효과음 OFF" else "효과음 ON",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        when {
            sessionSummary != null -> {
                val summary = sessionSummary!!
                RevengeVictoryChestCard(
                    summary = summary,
                    chestOpened = chestOpened,
                    onOpenChest = {
                        if (!chestOpened) {
                            chestOpened = true
                            particleBurstNonce += 1
                            GameJuiceAudioEngine.playChestUnlock(summary.chestRarity.tierIndex)
                        }
                    },
                    onPlayAgain = { launchNewBattle() },
                    onBackToHub = {
                        sessionSummary = null
                        battleState = null
                    }
                )
            }

            currentBattle != null && !currentBattle.isFinished -> {
                val question = currentBattle.currentQuestion
                if (question != null) {
                    val animatedHp by animateFloatAsState(
                        targetValue = currentBattle.hpRatio,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "bossHpAnim"
                    )
                    val answered = currentBattle.lastOutcome != null

                    // Boss HUD + Particle Burst Canvas
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (currentBattle.feverActive) Color(0xFF28102A) else WordRunNavy,
                            border = BorderStroke(
                                1.5.dp,
                                if (currentBattle.feverActive) TrophyGold else StudioCobalt.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(shakeOffsetX.value.roundToInt(), 0) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${currentBattle.boss.nameKo} · ${currentBattle.currentIndex + 1}/${currentBattle.questions.size}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = Color.White
                                        )
                                        Text(
                                            text = currentBattle.boss.titleKo,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (currentBattle.combo > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = if (currentBattle.feverActive) TrophyGold else CoralOrange
                                            ) {
                                                Text(
                                                    text = if (currentBattle.feverActive) {
                                                        "FEVER ${currentBattle.combo}x!"
                                                    } else {
                                                        "${currentBattle.combo} COMBO (${currentBattle.multiplier}x)"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                                    color = if (currentBattle.feverActive) WordRunAmberInk else Color.White,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color.White.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = "+${currentBattle.totalXp} XP",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = WordRunLime,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Boss HP Bar
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "BOSS HP",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color(0xFFFCA5A5)
                                        )
                                        Text(
                                            text = "${currentBattle.bossCurrentHp} / ${currentBattle.boss.maxHp}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { animatedHp },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(999.dp)),
                                        color = if (currentBattle.feverActive) TrophyGold else SpartaRed,
                                        trackColor = Color.White.copy(alpha = 0.18f)
                                    )
                                }

                                // Mode Badge & Listening Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = GlacierCyan.copy(alpha = 0.20f)
                                    ) {
                                        Text(
                                            text = "${question.mode.badgeKo} · ${question.mode.instructionKo}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GlacierCyan,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onSpeakEnglish(question.targetWord.word) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                            contentDescription = "단어 발음 듣기",
                                            tint = WordRunLime
                                        )
                                    }
                                }

                                // Main Prompt Card
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = question.promptPrimary,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    question.promptSecondary?.let { sec ->
                                        Text(
                                            text = sec,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFBAE6FD),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Dynamic Flow Channel Auto-Hint Banner (after 2 consecutive misses)
                                if (currentBattle.consecutiveMisses >= GameRevengeLoopEngine.FLOW_HINT_MISS_THRESHOLD) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = TrophyGold.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.65f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("revenge_flow_hint_banner")
                                    ) {
                                        Text(
                                            text = "${question.hintTextKo} (오답 보기 1개 자동 제거됨!)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TrophyGold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Lightweight Canvas Particle Spark Burst on Hit
                        if (burstProgress.value < 0.99f) {
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                            ) {
                                val p = burstProgress.value
                                val center = Offset(size.width * 0.5f, size.height * 0.45f)
                                val radiusMax = size.minDimension * 0.55f
                                val colors = listOf(WordRunLime, TrophyGold, GlacierCyan, CoralOrange)
                                for (i in 0 until 14) {
                                    val angle = (2.0 * PI * i / 14.0)
                                    val dist = radiusMax * p
                                    val x = center.x + (cos(angle) * dist).toFloat()
                                    val y = center.y + (sin(angle) * dist).toFloat()
                                    drawCircle(
                                        color = colors[i % colors.size].copy(alpha = (1f - p).coerceIn(0f, 1f)),
                                        radius = (8f * (1f - p * 0.6f)).coerceAtLeast(2f),
                                        center = Offset(x, y)
                                    )
                                }
                            }
                        }
                    }

                    // 4 Answer Options
                    question.options.forEachIndexed { idx, opt ->
                        val isEliminated = currentBattle.eliminatedOptionWordId == opt.wordId && !answered
                        val bg = when {
                            answered && opt.isCorrect -> AdventureEmerald.copy(alpha = 0.18f)
                            answered && !opt.isCorrect -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            isEliminated -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val border = when {
                            answered && opt.isCorrect -> AdventureEmerald
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = bg,
                            border = BorderStroke(1.5.dp, border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .alpha(if (isEliminated) 0.42f else 1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !answered && !isEliminated) {
                                    val elapsed = System.currentTimeMillis() - questionShownAtMs
                                    val nextState = GameRevengeLoopEngine.submitAnswer(
                                        state = currentBattle,
                                        selectedWordId = opt.wordId,
                                        elapsedMs = elapsed
                                    )
                                    battleState = nextState
                                    val outcome = nextState.lastOutcome
                                    if (outcome != null) {
                                        if (outcome.isCorrect) {
                                            particleBurstNonce += 1
                                            bestComboToday = maxOf(bestComboToday, outcome.comboAfter)
                                            if (outcome.justTriggeredFever) {
                                                GameJuiceAudioEngine.playFeverTrigger()
                                            } else {
                                                GameJuiceAudioEngine.playComboHit(outcome.comboAfter)
                                            }
                                            onRecordFsrsWord(question.targetWord, RecallGrade.GOOD, false)
                                        } else {
                                            GameJuiceAudioEngine.playWrongThud()
                                            onRecordFsrsWord(question.targetWord, RecallGrade.AGAIN, false)
                                        }
                                    }
                                }
                                .testTag("revenge_opt_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = StudioCobalt.copy(alpha = 0.14f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                            color = StudioCobalt
                                        )
                                    }
                                }
                                Text(
                                    text = opt.displayText,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // Instant Feedback & Next Turn Button
                    AnimatedVisibility(visible = answered) {
                        val outcome = currentBattle.lastOutcome
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (outcome != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = (if (outcome.isCorrect) AdventureEmerald else SpartaRed).copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = outcome.feedbackKo,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (outcome.isCorrect) AdventureEmerald else SpartaRed,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { battleState = null },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) { Text("중단") }
                                Button(
                                    onClick = {
                                        val advanced = GameRevengeLoopEngine.advanceTurn(currentBattle)
                                        if (advanced.isFinished) {
                                            val summary = GameRevengeLoopEngine.summarizeSession(advanced)
                                            if (summary.bossDefeated) {
                                                sessionsClearedToday += 1
                                                GameJuiceAudioEngine.playBossDefeated()
                                            }
                                            sessionSummary = summary
                                            battleState = null
                                        } else {
                                            battleState = advanced
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WordRunLime,
                                        contentColor = WordRunLimeInk
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("revenge_next_turn_btn")
                                ) {
                                    Text(
                                        text = if (currentBattle.bossCurrentHp <= 0 || currentBattle.currentIndex + 1 >= currentBattle.questions.size) {
                                            "보상 상자 열기 →"
                                        } else {
                                            "다음 공격 →"
                                        },
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // 3-Second Instant Play Launcher Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = WordRunNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SpartaRed.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "3초 즉시 출격 · 8문항 2분 컷",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFFFCA5A5),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = WordRunLime.copy(alpha = 0.20f)
                            ) {
                                Text(
                                    text = "도감 숙련 $masteryCodexPercent%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = WordRunLime,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "FSRS 오답·망각 임박 단어로\n보스를 격파하고 상자를 여세요",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ArenaStatPill("복수전 대상 ${dueOrWeakCount}단어", SpartaRed)
                            ArenaStatPill("전체 풀 ${candidateWords.size}단어", SkyQuestBlue)
                            ArenaStatPill("4모드 감각 전환", RoyalPurple)
                            ArenaStatPill("연속오답 자동힌트", TrophyGold)
                        }

                        Button(
                            onClick = { launchNewBattle() },
                            enabled = candidateWords.size >= 4,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WordRunLime,
                                contentColor = WordRunLimeInk
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("revenge_instant_start_btn")
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = "오답 보스 복수전 바로 시작",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }

                // Daily 3-Minute Micro-Missions Checklist
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = TrophyGold, modifier = Modifier.size(18.dp))
                            Text(
                                text = "오늘의 3분 마이크로 미션",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                        dailyMissions.forEach { mission ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (mission.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = if (mission.isCompleted) AdventureEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = mission.titleKo,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (mission.isCompleted) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                }
                                Text(
                                    text = "${mission.current}/${mission.target}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (mission.isCompleted) AdventureEmerald else StudioCobalt
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevengeVictoryChestCard(
    summary: RevengeSessionSummary,
    chestOpened: Boolean,
    onOpenChest: () -> Unit,
    onPlayAgain: () -> Unit,
    onBackToHub: () -> Unit
) {
    val rarityAccent = when (summary.chestRarity) {
        RewardChestRarity.COMMON -> SkyQuestBlue
        RewardChestRarity.RARE -> StudioCobalt
        RewardChestRarity.EPIC -> RoyalPurple
        RewardChestRarity.LEGENDARY -> TrophyGold
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = WordRunNavy,
        border = BorderStroke(1.5.dp, rarityAccent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("revenge_victory_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (summary.bossDefeated) Icons.Outlined.EmojiEvents else Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = TrophyGold,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = if (summary.bossDefeated) "${summary.boss.nameKo} 격파 성공!" else "복수전 완료 · 다시 도전하세요!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "정답 ${summary.correctCount}/${summary.totalCount} (${summary.accuracyPercent}%) · 최고 ${summary.maxCombo}콤보",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = GlacierCyan
            )

            // Interactive Variable Reward Chest
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (chestOpened) rarityAccent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, rarityAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !chestOpened) { onOpenChest() }
                    .testTag("revenge_open_chest_btn")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CardGiftcard,
                        contentDescription = "보상 상자 열기",
                        tint = rarityAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = if (chestOpened) {
                            "${summary.chestRarity.labelKo} 획득! (+${summary.chestBonusXp} 보너스 XP)"
                        } else {
                            "탭하여 랜덤 보상 상자 열기"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (chestOpened) {
                        Text(
                            text = "총 획득 경험치 +${summary.totalXp} XP · 모든 결과가 FSRS 복습 스케줄에 반영됨",
                            style = MaterialTheme.typography.labelSmall,
                            color = WordRunLime,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (summary.missedWords.isNotEmpty()) {
                Text(
                    text = "FSRS 집중 복습 예약: ${summary.missedWords.joinToString(", ") { it.word }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFCA5A5),
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToHub,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("로비", color = Color.White)
                }
                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WordRunLime,
                        contentColor = WordRunLimeInk
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("revenge_play_again_btn")
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("딱 한 판만 더!", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ArenaStatPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
