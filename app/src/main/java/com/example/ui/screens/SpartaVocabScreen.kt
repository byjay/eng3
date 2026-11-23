package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PatternSentenceRepository
import com.example.engine.VocabMnemonicEngine
import com.example.engine.VoiceEngineState
import com.example.model.LoopRepeatMode
import com.example.model.VocabLearningMethod
import com.example.model.VocabStudyTrack
import com.example.model.VocabTopic
import com.example.model.WordKnowledgeStatus
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.PronunciationReportCard
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.VoiceWaveformOrb3D
import com.example.ui.components.YouTubeWordContextDialog
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeShadow
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalPurpleShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.SpartaRedShadow
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow
import com.example.viewmodel.PatternStudioState
import com.example.viewmodel.SpartaDrillState
import com.example.viewmodel.SpartaPhase

@Composable
fun SpartaVocabScreen(
    state: SpartaDrillState,
    patternState: PatternStudioState,
    voiceState: VoiceEngineState,
    activeStudySec: Int,
    harvestedApiCount: Int,
    harvestStatusKo: String,
    isHarvestingWords: Boolean,
    onSelectTier1To5: (Int) -> Unit,
    onStartLevelTest: () -> Unit,
    onSelectTrack: (VocabStudyTrack) -> Unit,
    onSelectTopic: (VocabTopic) -> Unit,
    onSelectLearningMethod: (VocabLearningMethod) -> Unit,
    onToggleRevealAnswer: () -> Unit,
    onMarkWordKnowledge: (WordKnowledgeStatus) -> Unit,
    onFlushStudyLog: () -> Unit,
    onStartPreviewMode: () -> Unit,
    onCompletePreview: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onSelectExampleIndex: (Int) -> Unit,
    onSetVocabLoopMode: (LoopRepeatMode) -> Unit,
    onListenWord: () -> Unit,
    onListenSentence: () -> Unit,
    onListenAllExamples: () -> Unit,
    onStartVoiceShadowing: () -> Unit,
    onStopVoice: () -> Unit,
    onEvaluateTypedShadowing: (String) -> Unit,
    onNextWord: () -> Unit,
    onRestartSession: () -> Unit,
    onHarvestDaily50Words: () -> Unit,
    // 1~3 Sentence Pattern Word-Swap Studio callbacks
    onSelectPatternDrill: (Int) -> Unit,
    onSwapPatternWord: (String, Int) -> Unit,
    onSetPatternLoopMode: (LoopRepeatMode) -> Unit,
    onTogglePatternKorean: () -> Unit,
    onSpeakPattern: () -> Unit,
    onStartPatternSpeech: () -> Unit,
    onEvaluateTypedPattern: (String) -> Unit
) {
    val context = LocalContext.current
    var activeSubMode by remember { mutableIntStateOf(0) } // 0 = Vocab (Anti-ABC 5 Tracks), 1 = 1~3 Sentence Pattern Swap Studio
    var manualInput by remember(state.currentWord?.id, activeSubMode, patternState.activeDrillIndex) { mutableStateOf("") }
    var youtubeClipQuery by remember { mutableStateOf<String?>(null) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (activeSubMode == 0) onStartVoiceShadowing() else onStartPatternSpeech()
        }
    }

    fun triggerMic() {
        if (voiceState.isListening) {
            onStopVoice()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                if (activeSubMode == 0) onStartVoiceShadowing() else onStartPatternSpeech()
            } else {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    if (youtubeClipQuery != null) {
        YouTubeWordContextDialog(
            wordOrPhrase = youtubeClipQuery!!,
            onDismiss = { youtubeClipQuery = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Mode Switcher: Anti-ABC Vocab vs 1~3 Sentence Pattern Swap Studio
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Tactile3DButton(
                    text = "교과 영단어 (1~5등급)",
                    icon = Icons.Default.AutoStories,
                    containerColor = if (activeSubMode == 0) SpartaRed else MaterialTheme.colorScheme.surfaceVariant,
                    shadowColor = if (activeSubMode == 0) SpartaRedShadow else Color(0xFFC5CFD9),
                    contentColor = if (activeSubMode == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    tag = "tab_vocab_anti_abc",
                    onClick = { activeSubMode = 0 }
                )
                Tactile3DButton(
                    text = "1~3문장 패턴교체",
                    icon = Icons.Default.SwapHoriz,
                    containerColor = if (activeSubMode == 1) RoyalPurple else MaterialTheme.colorScheme.surfaceVariant,
                    shadowColor = if (activeSubMode == 1) RoyalPurpleShadow else Color(0xFFC5CFD9),
                    contentColor = if (activeSubMode == 1) Color.White else MaterialTheme.colorScheme.onSurface,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    tag = "tab_pattern_studio",
                    onClick = { activeSubMode = 1 }
                )
            }
        }

        if (activeSubMode == 0) {
            // 0. Exact Study Stopwatch + Multi-Method Learning Selector (해마학습법 · 단어연상법 · 망각곡선 · 능동회상 · 맥락스피킹)
            item {
                val mins = activeStudySec / 60
                val secs = activeStudySec % 60
                val clockText = "%02d:%02d".format(mins, secs)
                Elevated3DCard(accentShadow = RoyalPurpleShadow.copy(alpha = 0.3f), contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = "실시간 학습 타이머", tint = RoyalPurple, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "정확한 학습시간 $clockText",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = RoyalPurpleShadow
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "알아요 ${state.sessionKnownCount} · 몰라요 ${state.sessionUnknownCount + state.sessionFuzzyCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AdventureEmerald,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onFlushStudyLog)
                                    .testTag("flush_study_time_log_button")
                            ) {
                                Text(
                                    text = "시간 로그 저장",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // 5 Evidence-Based Learning Method Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(VocabLearningMethod.entries) { method ->
                                val active = state.learningMethod == method
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (active) RoyalPurple else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectLearningMethod(method) }
                                        .testTag("vocab_method_${method.name}")
                                ) {
                                    Text(
                                        text = method.labelKo,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = state.learningMethod.descKo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. 1~5 Grade Tier Build-Up Bar + Quick Level Test + Daily Free API 50+ Words Button
            item {
                Elevated3DCard(accentShadow = SkyQuestBlue.copy(alpha = 0.25f), contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1~5등급 난이도 빌드업 (ABC 순서 배제)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TrophyGold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onStartLevelTest)
                                    .testTag("start_level_test_chip")
                            ) {
                                Text(
                                    text = "레벨테스트",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF3E2C00),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 4, 3, 2, 1).forEach { tier ->
                                val selected = state.selectedTier1To5 == tier
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) SpartaRed else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectTier1To5(tier) }
                                        .testTag("vocab_tier_$tier")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${tier}등급",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // 5 Anti-ABC Study Tracks
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(VocabStudyTrack.entries) { track ->
                                val active = state.track == track
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (active) AdventureEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectTrack(track) }
                                        .testTag("vocab_track_${track.name}")
                                ) {
                                    Text(
                                        text = track.labelKo,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        if (state.track == VocabStudyTrack.TOPIC_SITUATION) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(VocabTopic.entries) { topic ->
                                    val active = state.selectedTopic == topic
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (active) SkyQuestBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSelectTopic(topic) }
                                    ) {
                                        Text(
                                            text = "#${topic.labelKo}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Daily Free Word API 50+ Accumulator Strip + Preview Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "무료API 누적 ${harvestedApiCount}단어 · $harvestStatusKo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Spacer(Modifier.width(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = RoyalPurple.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onStartPreviewMode)
                                ) {
                                    Text(
                                        text = "예습",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = RoyalPurpleShadow,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SkyQuestBlue.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isHarvestingWords, onClick = onHarvestDaily50Words)
                                        .testTag("harvest_api_50_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isHarvestingWords) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SkyQuestBlueShadow, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "+50수집",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SkyQuestBlueShadow
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Status Banner (Timer / Combo / Feedback)
            if (state.lastFeedbackKo.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (state.lastWasMiss) SpartaRed.copy(alpha = 0.14f) else AdventureEmerald.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.lastFeedbackKo,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (state.lastWasMiss) SpartaRedShadow else AdventureEmeraldShadow,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            val word = state.currentWord
            if (state.phase == SpartaPhase.COMPLETED || word == null) {
                item {
                    Elevated3DCard(accentShadow = TrophyGoldShadow.copy(alpha = 0.45f)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TrophyGoldShadow, modifier = Modifier.size(60.dp))
                            Text("왕국 성벽 단어 미션 완료!", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                text = "획득 +${state.sessionCoins} 코인 · +${state.sessionXp} XP · 예습·복습·만점 누적 반영!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Tactile3DButton(
                                text = "새 랜덤 덱으로 다시 도전",
                                icon = Icons.Default.Refresh,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onRestartSession
                            )
                        }
                    }
                }
            } else {
                // Active Word Card (Supports LEVEL_TEST, BOSS_BATTLE, PREVIEW_STUDY, MEANING_QUIZ, SENTENCE_SHADOWING)
                item {
                    Elevated3DCard(
                        accentShadow = when (state.phase) {
                            SpartaPhase.BOSS_BATTLE -> SpartaRed.copy(alpha = 0.45f)
                            SpartaPhase.LEVEL_TEST -> TrophyGoldShadow.copy(alpha = 0.45f)
                            else -> SkyQuestBlue.copy(alpha = 0.3f)
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Top Badges (Frequency Stars, Difficulty, Topic, Timer)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = TrophyGold.copy(alpha = 0.25f)) {
                                        Text(
                                            text = "빈출 ${"★".repeat(word.csatFrequency.coerceIn(1, 5))}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF7A5600),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = SkyQuestBlue.copy(alpha = 0.14f)) {
                                        Text(
                                            text = word.topic.labelKo,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SkyQuestBlueShadow,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (state.phase == SpartaPhase.MEANING_QUIZ || state.phase == SpartaPhase.BOSS_BATTLE) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Timer, contentDescription = null, tint = if (state.timeLeftSec <= 5) SpartaRed else AdventureEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("${state.timeLeftSec}s", style = MaterialTheme.typography.labelLarge, color = if (state.timeLeftSec <= 5) SpartaRed else AdventureEmeraldShadow)
                                    }
                                }
                            }

                            if (state.phase == SpartaPhase.BOSS_BATTLE) {
                                Surface(shape = RoundedCornerShape(12.dp), color = SpartaRed.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "보스전: ${state.bossNameKo} (체력 ${state.bossHp}/${state.bossMaxHp})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SpartaRedShadow
                                        )
                                        LinearProgressIndicator(
                                            progress = { state.bossHp.toFloat() / state.bossMaxHp.coerceAtLeast(1) },
                                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                            color = SpartaRed,
                                            trackColor = Color.White,
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                }
                            }

                            // Word Title + IPA + Audio + YouTube Real Conversation Clip Button!
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = word.textbookUnitKo,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${word.partOfSpeech} ${word.ipa}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SkyQuestBlue.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(onClick = onListenWord)
                                            .testTag("listen_vocab_word")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "발음 듣기", tint = SkyQuestBlue, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("발음 듣기", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = SkyQuestBlueShadow)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SpartaRed.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { youtubeClipQuery = word.word }
                                            .testTag("open_youtube_word_clips")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = "유튜브 실제 회화", tint = SpartaRed, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("유튜브 회화", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = SpartaRedShadow)
                                        }
                                    }
                                }
                            }

                            when (state.phase) {
                                SpartaPhase.PREVIEW_STUDY -> {
                                    Surface(shape = RoundedCornerShape(14.dp), color = AdventureEmerald.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("뜻: ${word.meaningKo}", style = MaterialTheme.typography.titleMedium, color = AdventureEmeraldShadow)
                                            Text("발음 팁: ${word.pronunciationTipKo}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text("단어별 4개 예문 미리 읽기 & 듣기", style = MaterialTheme.typography.titleMedium)
                                    word.examples.forEachIndexed { idx, ex ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onSelectExampleIndex(idx) }
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("${idx + 1}. ${ex.labelKo}", style = MaterialTheme.typography.labelMedium, color = SkyQuestBlueShadow)
                                                Text(ex.english, style = MaterialTheme.typography.bodyLarge)
                                                Text(ex.korean, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    Tactile3DButton(
                                        text = "예습 완료! 퀴즈 & 말하기 시작",
                                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                                        modifier = Modifier.fillMaxWidth(),
                                        tag = "complete_preview_button",
                                        onClick = onCompletePreview
                                    )
                                }

                                SpartaPhase.MEANING_QUIZ, SpartaPhase.LEVEL_TEST, SpartaPhase.BOSS_BATTLE -> {
                                    // Instant Know / Fuzzy / Don't Know Diagnostic + Hippocampus & Word Association Guide
                                    if (state.phase == SpartaPhase.MEANING_QUIZ) {
                                        val mnemonic = remember(word.id) { VocabMnemonicEngine.buildGuide(word) }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = RoyalPurple.copy(alpha = 0.09f),
                                            border = BorderStroke(1.dp, RoyalPurple.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "이 단어 아나요? 모르나요? (${state.learningMethod.labelKo})",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = RoyalPurpleShadow
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable(onClick = onToggleRevealAnswer)
                                                            .testTag("toggle_mnemonic_reveal")
                                                    ) {
                                                        Text(
                                                            text = if (state.showAnswerRevealed) "힌트 숨기기" else "해마·연상 보기",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = SkyQuestBlueShadow,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Tactile3DButton(
                                                        text = "알아요 (O)",
                                                        containerColor = AdventureEmerald,
                                                        shadowColor = AdventureEmeraldShadow,
                                                        height = 42.dp,
                                                        modifier = Modifier.weight(1f),
                                                        tag = "vocab_know_button",
                                                        onClick = { onMarkWordKnowledge(WordKnowledgeStatus.KNOWN) }
                                                    )
                                                    Tactile3DButton(
                                                        text = "애매 (△)",
                                                        containerColor = TrophyGold,
                                                        shadowColor = TrophyGoldShadow,
                                                        contentColor = Color(0xFF3E2C00),
                                                        height = 42.dp,
                                                        modifier = Modifier.weight(0.95f),
                                                        tag = "vocab_fuzzy_button",
                                                        onClick = { onMarkWordKnowledge(WordKnowledgeStatus.FUZZY) }
                                                    )
                                                    Tactile3DButton(
                                                        text = "몰라요 (X)",
                                                        containerColor = SpartaRed,
                                                        shadowColor = SpartaRedShadow,
                                                        height = 42.dp,
                                                        modifier = Modifier.weight(1f),
                                                        tag = "vocab_unknown_button",
                                                        onClick = { onMarkWordKnowledge(WordKnowledgeStatus.UNKNOWN) }
                                                    )
                                                }

                                                if (state.showAnswerRevealed) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                                        ) {
                                                            Text(
                                                                text = "정답 뜻: ${word.meaningKo}",
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                                color = AdventureEmeraldShadow
                                                            )
                                                            Text(
                                                                text = "• ${mnemonic.rootBreakdownKo}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "• ${mnemonic.phoneticHookKo}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = SkyQuestBlueShadow
                                                            )
                                                            Text(
                                                                text = "• ${mnemonic.hippocampusStoryKo}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = RoyalPurpleShadow
                                                            )
                                                            Text(
                                                                text = mnemonic.reviewTimingKo,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = CoralOrangeShadow
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        state.options.forEachIndexed { idx, option ->
                                            Tactile3DButton(
                                                text = "${idx + 1}. $option",
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                shadowColor = Color(0xFFC5CFD9),
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                                height = 50.dp,
                                                modifier = Modifier.fillMaxWidth(),
                                                tag = "vocab_option_$idx",
                                                onClick = { onSubmitAnswer(option) }
                                            )
                                        }
                                    }
                                }

                                SpartaPhase.SENTENCE_SHADOWING -> {
                                    val activeExample = word.examples.getOrNull(state.activeExampleIndex)
                                    // 4 Sentences Tabs (Read, Listen & Speak!)
                                    Text("STEP 2 · 4개 예문 읽고 · 듣고 · 말하기", style = MaterialTheme.typography.titleMedium, color = AdventureEmeraldShadow)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        word.examples.forEachIndexed { idx, _ ->
                                            val active = idx == state.activeExampleIndex
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (active) AdventureEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { onSelectExampleIndex(idx) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                                                    Text(
                                                        text = "예문 ${idx + 1}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (activeExample != null) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = SkyQuestBlue.copy(alpha = 0.10f),
                                            border = BorderStroke(1.dp, SkyQuestBlue.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(activeExample.labelKo, style = MaterialTheme.typography.labelMedium, color = SkyQuestBlueShadow)
                                                Text(activeExample.english, style = MaterialTheme.typography.titleLarge)
                                                Text(activeExample.korean, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("발음 코칭: ${word.pronunciationTipKo}", style = MaterialTheme.typography.labelMedium, color = AdventureEmeraldShadow)
                                            }
                                        }
                                    }

                                    // Loop Listening Controls (1x, 3x, 5x, Infinite + Play All 4)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        LoopRepeatMode.entries.forEach { mode ->
                                            val active = state.loopMode == mode
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (active) RoyalPurple else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { onSetVocabLoopMode(mode) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                                    Text(
                                                        text = mode.labelKo,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Tactile3DButton(
                                            text = if (state.isLoopPlaying) "반복 중지" else "예문 듣기",
                                            icon = if (state.isLoopPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                            containerColor = SkyQuestBlue,
                                            shadowColor = SkyQuestBlueShadow,
                                            height = 46.dp,
                                            modifier = Modifier.weight(1f),
                                            onClick = { if (state.isLoopPlaying) onStopVoice() else onListenSentence() }
                                        )
                                        Tactile3DButton(
                                            text = "4문장 연속듣기",
                                            icon = Icons.Default.Repeat,
                                            containerColor = RoyalPurple,
                                            shadowColor = RoyalPurpleShadow,
                                            height = 46.dp,
                                            modifier = Modifier.weight(1f),
                                            onClick = onListenAllExamples
                                        )
                                    }

                                    // Voice Speaking Orb + Text Fallback
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        VoiceWaveformOrb3D(
                                            isListening = voiceState.isListening,
                                            isSpeaking = voiceState.isSpeaking,
                                            rmsDb = voiceState.rmsDb,
                                            onClick = { triggerMic() },
                                            tag = "sparta_shadowing_mic"
                                        )
                                        Text(
                                            text = if (voiceState.isListening) voiceState.partialTranscript.ifBlank { "예문을 원어민처럼 말하세요..." } else "마이크를 눌러 예문을 따라 말하세요!",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = manualInput,
                                            onValueChange = { manualInput = it },
                                            placeholder = { Text("직접 예문 입력 테스트...") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (manualInput.isNotBlank()) {
                                                    onEvaluateTypedShadowing(manualInput)
                                                    manualInput = ""
                                                }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "채점", tint = AdventureEmerald)
                                        }
                                    }

                                    Tactile3DButton(
                                        text = "다음 단어로 넘어가기",
                                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                                        containerColor = AdventureEmerald,
                                        shadowColor = AdventureEmeraldShadow,
                                        height = 46.dp,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = onNextWord
                                    )
                                }

                                SpartaPhase.COMPLETED -> Unit
                            }
                        }
                    }
                }

                if (state.latestPronunciation != null) {
                    item {
                        PronunciationReportCard(
                            report = state.latestPronunciation,
                            onCompareNativeClick = onListenSentence
                        )
                    }
                }
            }
        } else {
            // ==================== MODE B: SELF-INTRO & 1~3 SENTENCE PATTERN WORD-SWAP & LOOP STUDIO ====================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "자기소개 & 1~3문장 집중 패턴 교체 도장",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "문장 속 컬러 단어를 바꿔가며 패턴을 익히고, 귀에 익숙해지도록 반복 듣기·말하기를 훈련하세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(PatternSentenceRepository.drills) { idx, item ->
                            val selected = idx == patternState.activeDrillIndex
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) RoyalPurple else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, if (selected) RoyalPurple else MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectPatternDrill(idx) }
                                    .testTag("pattern_drill_$idx")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = item.stageTagKo,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) TrophyGold else RoyalPurple
                                    )
                                    Text(
                                        text = item.titleKo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                val drill = patternState.activeDrill
                Elevated3DCard(accentShadow = RoyalPurple.copy(alpha = 0.35f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(10.dp), color = RoyalPurple.copy(alpha = 0.14f)) {
                                Text(
                                    text = "${drill.stageTagKo} (${drill.sentenceCount}문장)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = RoyalPurpleShadow,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SpartaRed.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { youtubeClipQuery = patternState.renderedEnglish.split(" ").take(4).joinToString(" ") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = SpartaRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("유튜브 클립", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = SpartaRedShadow)
                                }
                            }
                        }

                        Text(
                            text = patternState.renderedEnglish,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (patternState.showKorean) {
                            Text(
                                text = patternState.renderedKorean,
                                style = MaterialTheme.typography.bodyLarge,
                                color = SkyQuestBlueShadow
                            )
                        }

                        // Interactive Word-Swap Slot Chips!
                        drill.slots.forEach { slot ->
                            val selectedOptIdx = patternState.selectedSlotIndices[slot.slotKey] ?: 0
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "단어 교체: ${slot.labelKo}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = RoyalPurpleShadow
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    itemsIndexed(slot.options) { optIdx, (en, ko) ->
                                        val active = optIdx == selectedOptIdx
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (active) AdventureEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onSwapPatternWord(slot.slotKey, optIdx) }
                                        ) {
                                            Text(
                                                text = "$en ($ko)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Surface(shape = RoundedCornerShape(12.dp), color = TrophyGold.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "청킹 팁: ${drill.coachingTipKo}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF5A4000),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        // Loop Repeat Controls (3x / 5x / Infinite Ear-Training!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LoopRepeatMode.entries.forEach { mode ->
                                val active = patternState.loopMode == mode
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) SkyQuestBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSetPatternLoopMode(mode) }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                                        Text(
                                            text = mode.labelKo,
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Tactile3DButton(
                                text = if (patternState.isLoopPlaying) "반복 중지" else "귀 트이기 반복듣기",
                                icon = if (patternState.isLoopPlaying) Icons.Default.Stop else Icons.Default.Loop,
                                containerColor = SkyQuestBlue,
                                shadowColor = SkyQuestBlueShadow,
                                height = 48.dp,
                                modifier = Modifier.weight(1f),
                                tag = "pattern_loop_play_button",
                                onClick = { if (patternState.isLoopPlaying) onStopVoice() else onSpeakPattern() }
                            )
                            Tactile3DButton(
                                text = if (patternState.showKorean) "해석 숨기기" else "해석 보기",
                                containerColor = CoralOrange,
                                shadowColor = CoralOrangeShadow,
                                height = 48.dp,
                                modifier = Modifier.weight(0.65f),
                                onClick = onTogglePatternKorean
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VoiceWaveformOrb3D(
                                isListening = voiceState.isListening,
                                isSpeaking = voiceState.isSpeaking,
                                rmsDb = voiceState.rmsDb,
                                onClick = { triggerMic() },
                                tag = "pattern_mic_button"
                            )
                            Text(
                                text = if (voiceState.isListening) voiceState.partialTranscript.ifBlank { "바꾼 문장을 소리 내어 말하세요..." } else "마이크를 눌러 바꾼 패턴 문장을 따라 말해보세요!",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualInput,
                                onValueChange = { manualInput = it },
                                placeholder = { Text("문장 발화 입력 테스트...") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (manualInput.isNotBlank()) {
                                        onEvaluateTypedPattern(manualInput)
                                        manualInput = ""
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "채점", tint = AdventureEmerald)
                            }
                        }
                    }
                }
            }

            if (patternState.latestPronunciation != null) {
                item {
                    PronunciationReportCard(
                        report = patternState.latestPronunciation,
                        onCompareNativeClick = onSpeakPattern
                    )
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}
