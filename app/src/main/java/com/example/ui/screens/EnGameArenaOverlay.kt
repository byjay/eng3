package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.instantQuizTouch
import com.example.R
import com.example.engine.EnGameMathEngine
import com.example.engine.EnGameMode
import com.example.engine.EnGamePhase
import com.example.engine.EnGameUiState
import com.example.engine.VocabMnemonicEngine
import com.example.engine.VoiceEngineState
import com.example.model.SquirrelJobHero
import com.example.model.VocabTopic
import com.example.ui.components.MaterialButtonStyle
import com.example.ui.components.MaterialLearningButton
import com.example.ui.components.SquirrelHeroAvatar
import com.example.ui.components.SquirrelMoodState
import com.example.ui.components.StitchIconBadge
import com.example.ui.components.StitchIconMetricChip
import com.example.ui.components.WordSceneIllustration
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.TrophyGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnGameArenaOverlay(
    state: EnGameUiState,
    hero: SquirrelJobHero,
    playerNickname: String,
    voiceState: VoiceEngineState,
    onClose: () -> Unit,
    onBackToHub: () -> Unit,
    onStartMode: (EnGameMode) -> Unit,
    onSubmitChoice: (String) -> Unit,
    onTapScrambledToken: (Int) -> Unit,
    onRemoveSelectedToken: (Int) -> Unit,
    onClearSentenceTokens: () -> Unit,
    onSubmitSentenceBuilder: () -> Unit,
    onListenAudio: (String) -> Unit,
    onStartVoiceBossAttack: () -> Unit,
    onStopVoice: () -> Unit,
    onTapMemoryFlipCard: (Int) -> Unit = {},
    onReviewMissedWords: () -> Unit = {}
) {
    BackHandler {
        if (state.phase == EnGamePhase.PLAYING || state.phase == EnGamePhase.RESULT) {
            onBackToHub()
        } else {
            onClose()
        }
    }

    val modeAccent = Color(state.mode.accentHex)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (state.phase == EnGamePhase.PLAYING || state.phase == EnGamePhase.RESULT) {
                                onBackToHub()
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier.testTag("engame_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                    Column {
                        Text(
                            text = if (state.phase == EnGamePhase.MODE_HUB) "EnGame 2.0 아케이드" else state.mode.titleKo,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "$playerNickname · ${EnGameMathEngine.difficultyTierLabel(state.eloRating)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StitchIconMetricChip(
                    icon = Icons.Rounded.EmojiEvents,
                    valueText = "ELO ${state.eloRating}",
                    accent = TrophyGold,
                    contentDescription = "적응형 ELO 등급"
                )
            }

            when (state.phase) {
                EnGamePhase.MODE_HUB -> {
                    EnGameModeHubContent(
                        state = state,
                        hero = hero,
                        onStartMode = onStartMode
                    )
                }

                EnGamePhase.PLAYING -> {
                    EnGamePlayingContent(
                        state = state,
                        hero = hero,
                        modeAccent = modeAccent,
                        voiceState = voiceState,
                        onSubmitChoice = onSubmitChoice,
                        onTapScrambledToken = onTapScrambledToken,
                        onRemoveSelectedToken = onRemoveSelectedToken,
                        onClearSentenceTokens = onClearSentenceTokens,
                        onSubmitSentenceBuilder = onSubmitSentenceBuilder,
                        onListenAudio = onListenAudio,
                        onStartVoiceBossAttack = onStartVoiceBossAttack,
                        onStopVoice = onStopVoice,
                        onTapMemoryFlipCard = onTapMemoryFlipCard
                    )
                }

                EnGamePhase.RESULT -> {
                    EnGameResultContent(
                        state = state,
                        hero = hero,
                        onReplay = { onStartMode(state.mode) },
                        onBackToHub = onBackToHub,
                        onListenAudio = onListenAudio,
                        onReviewMissedWords = onReviewMissedWords
                    )
                }

                EnGamePhase.CLOSED -> Unit
            }
        }
    }
}

@Composable
private fun EnGameModeHubContent(
    state: EnGameUiState,
    hero: SquirrelJobHero,
    onStartMode: (EnGameMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Arcade Banner
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, StudioCobalt.copy(alpha = 0.25f)),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E3A8A),
                                Color(0xFF2563EB),
                                Color(0xFF7C3AED)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SquirrelHeroAvatar(
                        hero = hero,
                        size = 66.dp,
                        mood = SquirrelMoodState.HAPPY_CHEER,
                        borderWidth = 2.5.dp,
                        borderColor = TrophyGold
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = TrophyGold
                            ) {
                                Text(
                                    text = "ENGAME 2.0",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFF1E1B4B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (state.highScore > 0) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "최고 ${state.highScore}점",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "8종 단어·기억 배틀 아케이드",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Text(
                            text = "적응형 ELO 난이도 · 피버 2배 콤보 · SM-2/오답 자동 연동",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }
        }

        // 4 Interactive Game Modes
        EnGameMode.entries.forEach { mode ->
            val accent = Color(mode.accentHex)
            val icon = modeIcon(mode)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, accent.copy(alpha = 0.28f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onStartMode(mode) }
                    .testTag("engame_mode_${mode.name}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StitchIconBadge(
                        icon = icon,
                        contentDescription = mode.titleKo,
                        accent = accent,
                        size = 54.dp,
                        iconSize = 26.dp,
                        filled = true
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mode.titleKo,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = accent.copy(alpha = 0.13f)
                            ) {
                                Text(
                                    text = mode.badgeKo,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = mode.subtitleKo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "${mode.titleKo} 시작",
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnGamePlayingContent(
    state: EnGameUiState,
    hero: SquirrelJobHero,
    modeAccent: Color,
    voiceState: VoiceEngineState,
    onSubmitChoice: (String) -> Unit,
    onTapScrambledToken: (Int) -> Unit,
    onRemoveSelectedToken: (Int) -> Unit,
    onClearSentenceTokens: () -> Unit,
    onSubmitSentenceBuilder: () -> Unit,
    onListenAudio: (String) -> Unit,
    onStartVoiceBossAttack: () -> Unit,
    onStopVoice: () -> Unit,
    onTapMemoryFlipCard: (Int) -> Unit
) {
    val question = state.currentQuestion ?: return
    val timerFraction = (state.timeLeftSec.toFloat() / state.totalTimeSec.coerceAtLeast(1)).coerceIn(0f, 1f)
    val timerColor by animateColorAsState(
        targetValue = when {
            timerFraction > 0.45f -> AdventureEmerald
            timerFraction > 0.22f -> TrophyGold
            else -> SpartaRed
        },
        label = "timerBarColor"
    )
    val feverProgress by animateFloatAsState(
        targetValue = (state.feverGauge / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 380f),
        label = "feverGaugeAnim"
    )

    // Particle burst animation on every correct answer
    val burstProgress = remember { Animatable(1f) }
    LaunchedEffect(state.hitPulseTick) {
        if (state.hitPulseTick > 0) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        }
    }

    val squirrelMood = when {
        state.isFeverActive -> SquirrelMoodState.SKILL_CAST
        voiceState.isListening -> SquirrelMoodState.LISTENING_TILT
        state.lastSpeedTagKo == "MISS" -> SquirrelMoodState.HURT_SHAKE
        state.combo >= 2 -> SquirrelMoodState.ATTACK_LUNGE
        else -> SquirrelMoodState.IDLE
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Live Arcade HUD (Timer, Hearts, Score, Combo, Fever Bar)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (state.isFeverActive) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = if (state.isFeverActive) 2.2.dp else 1.dp,
                color = if (state.isFeverActive) TrophyGold else MaterialTheme.colorScheme.outline
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hearts
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 1..state.maxLives) {
                            Icon(
                                imageVector = if (i <= state.lives) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "체력",
                                tint = if (i <= state.lives) SpartaRed else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Timer pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "남은 시간",
                            tint = timerColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${state.timeLeftSec}초",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = timerColor
                        )
                    }

                    // Score + Combo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (state.combo > 0) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = CoralOrange
                            ) {
                                Text(
                                    text = "${state.combo} COMBO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${state.score}점",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = modeAccent
                        )
                    }
                }

                // Countdown Timer Bar
                LinearProgressIndicator(
                    progress = { timerFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = timerColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                // Fever Gauge Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = "피버 게이지",
                        tint = if (state.isFeverActive) CoralOrange else TrophyGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (state.isFeverActive) "FEVER x2 (${state.feverTurnsLeft}턴)" else "FEVER ${state.feverGauge}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (state.isFeverActive) CoralOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { if (state.isFeverActive) 1f else feverProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = if (state.isFeverActive) CoralOrange else TrophyGold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // 2. Boss HP Bar (VOICE_BOSS_RUSH mode)
        if (state.mode == EnGameMode.VOICE_BOSS_RUSH) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpartaRed.copy(alpha = 0.09f),
                border = BorderStroke(1.2.dp, SpartaRed.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WAVE ${state.waveNumber} 보스",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = SpartaRed
                        )
                        Text(
                            text = "HP ${state.bossHp} / ${state.bossMaxHp}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SpartaRed
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (state.bossHp.toFloat() / state.bossMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = SpartaRed,
                        trackColor = SpartaRed.copy(alpha = 0.18f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // 3. Question Arena Card with Live Canvas Particle Burst + Hero Reaction
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, modeAccent.copy(alpha = 0.32f)),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SquirrelHeroAvatar(
                                hero = hero,
                                size = 44.dp,
                                mood = squirrelMood,
                                borderWidth = 2.dp
                            )
                            Column {
                                Text(
                                    text = if (question.isAntonymRound) "반의어 대결" else state.mode.badgeKo,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (question.isAntonymRound) SpartaRed else modeAccent
                                )
                                Text(
                                    text = "정답 ${state.correctCount}/${state.totalAnswered}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        MaterialLearningButton(
                            text = "듣기",
                            icon = Icons.Rounded.Headphones,
                            accent = SkyQuestBlue,
                            style = MaterialButtonStyle.TONAL,
                            tag = "engame_listen_btn",
                            onClick = { onListenAudio(question.audioTextEn) }
                        )
                    }

                    if (question.hideAudioTextUntilAnswered) {
                        // Audio-First Listening Challenge Box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = modeAccent.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, modeAccent.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onListenAudio(question.audioTextEn) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "원어민 음성 재생",
                                    tint = modeAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "탭해서 원어민 음성 듣기",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = modeAccent
                                )
                                Text(
                                    text = question.promptSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        if (question.pictureTopic != null) {
                            val targetWord = question.targetWord
                            val motifLabel = remember(targetWord?.id) {
                                targetWord?.let { VocabMnemonicEngine.visualMotifFor(it).labelKo } ?: question.pictureTopic.labelKo
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(158.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .testTag("engame_picture_match_art")
                            ) {
                                if (targetWord != null) {
                                    WordSceneIllustration(
                                        word = targetWord,
                                        styleIndex = state.totalAnswered,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val artRes = when (question.pictureTopic) {
                                        VocabTopic.TRAVEL_DAILY -> R.drawable.img_mnemonic_world_travel_1795492661880
                                        VocabTopic.EMOTION_MIND -> R.drawable.img_mnemonic_emotion_scene_1795492662342
                                        VocabTopic.SCIENCE_AI, VocabTopic.LOGIC_DEBATE -> R.drawable.img_mnemonic_action_sprint_1795492662453
                                        VocabTopic.SCHOOL_EXAM, VocabTopic.SOCIETY_ECONOMY -> R.drawable.img_mnemonic_memory_stage_1795492662379
                                    }
                                    Image(
                                        painter = painterResource(artRes),
                                        contentDescription = "${question.promptPrimary} 연상 장면",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.50f))
                                            )
                                        )
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = modeAccent,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = motifLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = question.promptPrimary,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (question.promptSecondary.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = question.promptSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = state.lastFeedbackKo.isNotBlank()) {
                        val isMiss = state.lastSpeedTagKo == "MISS"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMiss) SpartaRed.copy(alpha = 0.12f) else AdventureEmerald.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.lastFeedbackKo,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isMiss) SpartaRed else AdventureEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Live Canvas Particle Burst FX overlay on hit
            if (burstProgress.value < 1f) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(8.dp)
                ) {
                    val p = burstProgress.value
                    val center = Offset(size.width * 0.5f, size.height * 0.45f)
                    val maxRadius = size.minDimension * 0.45f
                    val colors = listOf(TrophyGold, AdventureEmerald, SkyQuestBlue, CoralOrange, RoyalPurple)
                    for (i in 0 until 12) {
                        val angle = (i * 30.0) * PI / 180.0
                        val dist = maxRadius * p
                        val x = center.x + (cos(angle) * dist).toFloat()
                        val y = center.y + (sin(angle) * dist).toFloat()
                        drawCircle(
                            color = colors[i % colors.size].copy(alpha = (1f - p).coerceIn(0f, 1f)),
                            radius = (9f * (1f - p * 0.6f)).dp.toPx().coerceAtLeast(2f),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }

        // 4. Mode-Specific Interactive Controls
        when (state.mode) {
            EnGameMode.SENTENCE_BUILDER, EnGameMode.SPELLING_TILES -> {
                val isSpelling = state.mode == EnGameMode.SPELLING_TILES
                val tileAccent = if (isSpelling) modeAccent else AdventureEmerald
                // Answer Tray (Placed Word/Letter Chips)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = tileAccent.copy(alpha = 0.08f),
                    border = BorderStroke(1.2.dp, tileAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 76.dp)
                ) {
                    if (state.selectedTokens.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSpelling) {
                                    "아래 알파벳 타일을 순서대로 눌러 단어를 완성하세요"
                                } else {
                                    "아래 영어 단어 블록을 순서대로 탭하세요"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        FlowRow(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (isSpelling) 6.dp else 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.selectedTokens.forEachIndexed { pos, tokenIdx ->
                                val rawText = question.scrambledTokens.getOrNull(tokenIdx).orEmpty()
                                val tokenText = if (isSpelling) rawText.uppercase() else rawText
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = tileAccent,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .instantQuizTouch { onRemoveSelectedToken(pos) }
                                        .testTag("engame_placed_token_$pos")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = if (isSpelling) 14.dp else 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = tokenText,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bank (Scrambled Word or Letter Chips)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isSpelling) 6.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.scrambledTokens.forEachIndexed { idx, token ->
                        val used = idx in state.selectedTokens
                        val displayToken = if (isSpelling) token.uppercase() else token
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (used) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.2.dp,
                                if (used) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else tileAccent.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .instantQuizTouch(enabled = !used) { onTapScrambledToken(idx) }
                                .testTag("engame_bank_token_$idx")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = if (isSpelling) 15.dp else 13.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = displayToken,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (used) Color.Transparent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaterialLearningButton(
                        text = "초기화",
                        icon = Icons.Rounded.Backspace,
                        accent = CoralOrange,
                        style = MaterialButtonStyle.OUTLINED,
                        tag = "engame_clear_tokens_btn",
                        modifier = Modifier.weight(0.85f),
                        onClick = onClearSentenceTokens
                    )
                    MaterialLearningButton(
                        text = if (isSpelling) "철자 제출" else "문장 제출",
                        icon = Icons.Rounded.Verified,
                        accent = tileAccent,
                        style = MaterialButtonStyle.FILLED,
                        enabled = state.selectedTokens.size == question.targetTokens.size,
                        tag = "engame_submit_sentence_btn",
                        modifier = Modifier.weight(1.35f),
                        onClick = onSubmitSentenceBuilder
                    )
                }
            }

            EnGameMode.MEMORY_FLIP -> {
                // Interactive 2-Column x 4-Row Memory Flip Match Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("engame_memory_flip_grid"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.memoryFlipCards.chunked(2).forEach { rowCards ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCards.forEach { card ->
                                val isMatched = card.pairId in state.matchedPairIds
                                val isOpen = isMatched || (card.cardIndex in state.flippedCardIndices)
                                val cardBg = when {
                                    isMatched -> AdventureEmerald.copy(alpha = 0.16f)
                                    isOpen -> RoyalPurple.copy(alpha = 0.14f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val cardBorder = when {
                                    isMatched -> AdventureEmerald
                                    isOpen -> RoyalPurple
                                    else -> modeAccent.copy(alpha = 0.32f)
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardBg,
                                    border = BorderStroke(if (isOpen) 1.8.dp else 1.1.dp, cardBorder),
                                    shadowElevation = if (isOpen) 3.dp else 1.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(82.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .instantQuizTouch(enabled = !isMatched) { onTapMemoryFlipCard(card.cardIndex) }
                                        .testTag("engame_flip_card_${card.cardIndex}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isOpen) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = if (card.isEnglish) "EN" else "뜻",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = if (isMatched) AdventureEmerald else RoyalPurple
                                                )
                                                Text(
                                                    text = card.label,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = "카드 뒤집기",
                                                    tint = modeAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "?",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                                    color = modeAccent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Live Voice Speaking Attack Button in VOICE_BOSS_RUSH
                if (state.mode == EnGameMode.VOICE_BOSS_RUSH) {
                    MaterialLearningButton(
                        text = if (voiceState.isListening) "듣는 중... 말하기 중지" else "영어로 직접 말해 크리티컬 격파!",
                        icon = if (voiceState.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        accent = if (voiceState.isListening) SpartaRed else RoyalPurple,
                        style = MaterialButtonStyle.FILLED,
                        tag = "engame_boss_voice_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (voiceState.isListening) onStopVoice() else onStartVoiceBossAttack()
                        }
                    )
                }

                // 4 Ultra-Responsive Material 3 Option Buttons (touchstart + immediate haptic + 50ms animation)
                question.options.forEachIndexed { idx, opt ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.2.dp, modeAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .instantQuizTouch { onSubmitChoice(opt) }
                            .testTag("engame_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = modeAccent.copy(alpha = 0.14f),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = modeAccent
                                    )
                                }
                            }
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EnGameResultContent(
    state: EnGameUiState,
    hero: SquirrelJobHero,
    onReplay: () -> Unit,
    onBackToHub: () -> Unit,
    onListenAudio: (String) -> Unit,
    onReviewMissedWords: () -> Unit = {}
) {
    val rank = state.rank
    val rankColor = Color(rank.accentHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, rankColor.copy(alpha = 0.55f)),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SquirrelHeroAvatar(
                    hero = hero,
                    size = 74.dp,
                    mood = if (state.accuracyPercent >= 70) SquirrelMoodState.VICTORY_DANCE else SquirrelMoodState.HAPPY_CHEER,
                    borderWidth = 3.dp,
                    borderColor = rankColor
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = rankColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.5.dp, rankColor)
                ) {
                    Text(
                        text = "RANK ${rank.label}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = rankColor,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = rank.messageKo,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${state.score}점",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Stats Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultStatPill("정답률", "${state.accuracyPercent}%", AdventureEmerald, Modifier.weight(1f))
                    ResultStatPill("최대 콤보", "${state.maxCombo}", CoralOrange, Modifier.weight(1f))
                    ResultStatPill("ELO", "${state.eloRating}", StudioCobalt, Modifier.weight(1f))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AdventureEmerald.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = AdventureEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "보상 지급: +${state.earnedCoins} 코인 · +${state.earnedXp} XP",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = AdventureEmerald
                        )
                    }
                }
            }
        }

        // Auto-logged Missed Words Review List
        if (state.missedWords.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "오답 노트 자동 저장 (${state.missedWords.size}개)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = CoralOrange
                    )
                    state.missedWords.take(6).forEach { (en, meaning) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onListenAudio(en) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = en,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = meaning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = "발음 듣기",
                                tint = SkyQuestBlue,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    MaterialLearningButton(
                        text = "취약 단어 바로 복습하기",
                        icon = Icons.Rounded.AutoAwesome,
                        accent = CoralOrange,
                        style = MaterialButtonStyle.FILLED,
                        tag = "engame_review_missed_words_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onReviewMissedWords
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaterialLearningButton(
                text = "모드 선택",
                icon = Icons.Rounded.SportsEsports,
                accent = StudioCobalt,
                style = MaterialButtonStyle.OUTLINED,
                tag = "engame_result_hub_btn",
                modifier = Modifier.weight(1f),
                onClick = onBackToHub
            )
            MaterialLearningButton(
                text = "다시 도전",
                icon = Icons.Rounded.Refresh,
                accent = AdventureEmerald,
                style = MaterialButtonStyle.FILLED,
                tag = "engame_result_replay_btn",
                modifier = Modifier.weight(1.25f),
                onClick = onReplay
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ResultStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.11f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = accent
            )
        }
    }
}

private fun modeIcon(mode: EnGameMode): ImageVector = when (mode) {
    EnGameMode.SPEED_BLITZ -> Icons.Rounded.Bolt
    EnGameMode.SENTENCE_BUILDER -> Icons.Rounded.Extension
    EnGameMode.SYNONYM_LINK -> Icons.Rounded.Link
    EnGameMode.VOICE_BOSS_RUSH -> Icons.Rounded.GraphicEq
    EnGameMode.SPELLING_TILES -> Icons.Rounded.Spellcheck
    EnGameMode.PICTURE_MATCH -> Icons.Rounded.Image
    EnGameMode.MEMORY_FLIP -> Icons.Rounded.Style
    EnGameMode.CLOZE_CONTEXT -> Icons.Rounded.ShortText
}
