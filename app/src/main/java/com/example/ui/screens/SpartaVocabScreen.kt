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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PatternSentenceRepository
import com.example.data.VocabRepository
import com.example.engine.VocabMnemonicEngine
import com.example.engine.VoiceEngineState
import com.example.model.LoopRepeatMode
import com.example.model.VocabLearningMethod
import com.example.model.VocabStudyTrack
import com.example.model.VocabTopic
import com.example.model.VocabWord
import com.example.model.WordKnowledgeStatus
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.PronunciationReportCard
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.VisualMnemonicAndChunkCard
import com.example.ui.components.VocabMemorizationDeckView
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
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow
import com.example.viewmodel.PatternStudioState
import com.example.viewmodel.SpartaDrillState
import com.example.viewmodel.SpartaPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpartaVocabScreen(
    state: SpartaDrillState,
    patternState: PatternStudioState,
    voiceState: VoiceEngineState,
    activeStudySec: Int,
    masteredWordsCount: Int,
    streakCount: Int,
    todayWordsCompleted: Int,
    wrongWords: List<VocabWord>,
    harvestedApiCount: Int,
    harvestStatusKo: String,
    isHarvestingWords: Boolean,
    onSelectGrade: (Int) -> Unit,
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
    onSpeakChunk: (String) -> Unit,
    onRecordFocusWordResult: (wordId: String, known: Boolean) -> Unit,
    onOpenWeeklyReport: () -> Unit,
    onStartVoiceShadowing: () -> Unit,
    onStopVoice: () -> Unit,
    onEvaluateTypedShadowing: (String) -> Unit,
    onNextWord: () -> Unit,
    onRestartSession: () -> Unit,
    onHarvestDaily50Words: () -> Unit,
    onSelectPatternDrill: (Int) -> Unit,
    onSwapPatternWord: (String, Int) -> Unit,
    onSetPatternLoopMode: (LoopRepeatMode) -> Unit,
    onTogglePatternKorean: () -> Unit,
    onSpeakPattern: () -> Unit,
    onStartPatternSpeech: () -> Unit,
    onEvaluateTypedPattern: (String) -> Unit
) {
    val context = LocalContext.current
    var activeSubMode by remember { mutableIntStateOf(0) } // 0 = 단어암기 카드, 1 = 퀴즈·발음, 2 = 패턴 블록
    var manualInput by remember(state.currentWord?.id, activeSubMode, patternState.activeDrillIndex) { mutableStateOf("") }
    var youtubeClipQuery by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (activeSubMode == 2) onStartPatternSpeech() else onStartVoiceShadowing()
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
                if (activeSubMode == 2) onStartPatternSpeech() else onStartVoiceShadowing()
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

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            VocabSettingsBottomSheetContent(
                state = state,
                activeStudySec = activeStudySec,
                harvestedApiCount = harvestedApiCount,
                harvestStatusKo = harvestStatusKo,
                isHarvestingWords = isHarvestingWords,
                onSelectTier1To5 = onSelectTier1To5,
                onSelectTrack = onSelectTrack,
                onSelectTopic = onSelectTopic,
                onSelectLearningMethod = onSelectLearningMethod,
                onStartLevelTest = {
                    showSettingsSheet = false
                    onStartLevelTest()
                },
                onStartPreviewMode = {
                    showSettingsSheet = false
                    onStartPreviewMode()
                },
                onHarvestDaily50Words = onHarvestDaily50Words,
                onFlushStudyLog = {
                    onFlushStudyLog()
                    showSettingsSheet = false
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Clean Learning-App Top Mode Switch: 단어암기 | 퀴즈·발음 | 패턴 블록 + 설정
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ModeSegmentTab(
                            label = "암기 카드",
                            icon = Icons.Outlined.Style,
                            selected = activeSubMode == 0,
                            accent = StudioCobalt,
                            tag = "tab_vocab_flashcards",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSubMode = 0 }
                        )
                        ModeSegmentTab(
                            label = "퀴즈·발음",
                            icon = Icons.Outlined.Quiz,
                            selected = activeSubMode == 1,
                            accent = SkyQuestBlue,
                            tag = "tab_vocab_anti_abc",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSubMode = 1 }
                        )
                        ModeSegmentTab(
                            label = "패턴 블록",
                            icon = Icons.Outlined.SwapHoriz,
                            selected = activeSubMode == 2,
                            accent = StudioIris,
                            tag = "tab_pattern_studio",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSubMode = 2 }
                        )
                    }
                }
                if (activeSubMode != 2) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showSettingsSheet = true }
                            .testTag("vocab_open_settings_sheet")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "학습 설정",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (activeSubMode == 0) {
            // === MODE 0: DEDICATED LEARNING-APP VOCABULARY MEMORIZATION FLASHCARD STUDIO ===
            item {
                VocabMemorizationDeckView(
                    selectedGrade = state.grade,
                    masteredWordsCount = masteredWordsCount,
                    streakCount = streakCount,
                    todayWordsCompleted = todayWordsCompleted,
                    wrongWords = wrongWords,
                    harvestedApiCount = harvestedApiCount,
                    onSelectGrade = onSelectGrade,
                    onSpeakEnglish = onSpeakChunk,
                    onRecordWordResult = onRecordFocusWordResult,
                    onOpenWeeklyReport = onOpenWeeklyReport,
                    onOpenYouTubeWord = { query -> youtubeClipQuery = query },
                    onStartSpeakingDrillForWord = { targetWord ->
                        val sentence = targetWord.examples.firstOrNull()?.english ?: targetWord.exampleEn
                        onSpeakChunk(sentence)
                        activeSubMode = 1
                    }
                )
            }
        } else if (activeSubMode == 1) {
            // 2. Single-Row High School Grade Deck Filter Tabs (전체 · 고1 · 고2 · 고3)
            item {
                val gradeSpecs = remember(harvestedApiCount) {
                    listOf(
                        Triple(0, "전체", StudioIris),
                        Triple(1, "고1", StudioCobalt),
                        Triple(2, "고2", SkyQuestBlue),
                        Triple(3, "고3", SpartaRed)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vocab_grade_filter_tabs"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gradeSpecs.forEach { (gradeVal, shortLabel, accentColor) ->
                        val selected = state.grade == gradeVal
                        val deckCount = remember(gradeVal, harvestedApiCount) {
                            VocabRepository.wordsForGrade(gradeVal).size +
                                if (gradeVal == 0) harvestedApiCount else 0
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) accentColor else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) accentColor else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectGrade(gradeVal) }
                                .testTag("tab_vocab_grade_$gradeVal")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = shortLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${deckCount}개",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (selected) Color.White.copy(alpha = 0.86f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 3. Minimal 1-Line Study Meta Bar (Timer + Active Deck Info + O/X Counts)
            item {
                val mins = activeStudySec / 60
                val secs = activeStudySec % 60
                val clockText = "%02d:%02d".format(mins, secs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "학습 시간",
                                tint = StudioCobalt,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = clockText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${state.selectedTier1To5}등급 · ${state.track.labelKo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "O ${state.sessionKnownCount} · X ${state.sessionUnknownCount + state.sessionFuzzyCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.lastFeedbackKo.isNotBlank()) {
                            val compactStatus = when {
                                state.lastFeedbackKo.contains("시간 초과") -> "복습 추가"
                                state.lastWasMiss -> "복습 대기"
                                else -> "완료 +XP"
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (state.lastWasMiss) CoralOrange.copy(alpha = 0.16f) else StudioCobalt.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = compactStatus,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = if (state.lastWasMiss) CoralOrange else StudioCobalt,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. ACTIVE WORD CARD — Immediately Above the Fold!
            val word = state.currentWord
            if (state.phase == SpartaPhase.COMPLETED || word == null) {
                item {
                    Elevated3DCard(accentShadow = StudioCobalt.copy(alpha = 0.35f)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = StudioCoral,
                                modifier = Modifier.size(54.dp)
                            )
                            Text("단어 학습 완료", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "+${state.sessionCoins} 코인 · +${state.sessionXp} XP",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Tactile3DButton(
                                text = "새 덱으로 다시 학습",
                                icon = Icons.Outlined.Refresh,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onRestartSession
                            )
                        }
                    }
                }
            } else {
                item {
                    Elevated3DCard(
                        accentShadow = when (state.phase) {
                            SpartaPhase.BOSS_BATTLE -> SpartaRed.copy(alpha = 0.4f)
                            SpartaPhase.LEVEL_TEST -> StudioCoral.copy(alpha = 0.4f)
                            else -> StudioCobalt.copy(alpha = 0.28f)
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Top Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StudioCoral.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "★".repeat(word.csatFrequency.coerceIn(1, 5)),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = StudioCoral,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SkyQuestBlue.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = word.topic.labelKo,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = SkyQuestBlue,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (state.phase == SpartaPhase.MEANING_QUIZ || state.phase == SpartaPhase.BOSS_BATTLE) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timer,
                                            contentDescription = null,
                                            tint = if (state.timeLeftSec <= 5) SpartaRed else SkyQuestBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "${state.timeLeftSec}s",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (state.timeLeftSec <= 5) SpartaRed else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (state.phase == SpartaPhase.BOSS_BATTLE) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SpartaRed.copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "${state.bossNameKo} (${state.bossHp}/${state.bossMaxHp})",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = SpartaRed
                                        )
                                        LinearProgressIndicator(
                                            progress = { state.bossHp.toFloat() / state.bossMaxHp.coerceAtLeast(1) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = SpartaRed,
                                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                }
                            }

                            // Word Focus Area + Audio & YouTube Segment Trigger
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = word.textbookUnitKo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${word.partOfSpeech} ${word.ipa}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SkyQuestBlue.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, SkyQuestBlue.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(onClick = onListenWord)
                                            .testTag("listen_vocab_word")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                                contentDescription = "발음 듣기",
                                                tint = SkyQuestBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "발음",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = SkyQuestBlue
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SpartaRed.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, SpartaRed.copy(alpha = 0.38f)),
                                        modifier = Modifier
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { youtubeClipQuery = word.word }
                                            .testTag("open_youtube_word_clips")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.PlayCircle,
                                                contentDescription = "유튜브 강의 구간",
                                                tint = SpartaRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "유튜브 구간",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = SpartaRed
                                            )
                                        }
                                    }
                                }
                            }

                            when (state.phase) {
                                SpartaPhase.PREVIEW_STUDY -> {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = StudioCobalt.copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(word.meaningKo, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = StudioCobalt)
                                            Text(word.pronunciationTipKo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
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
                                                Text("${idx + 1}. ${ex.labelKo}", style = MaterialTheme.typography.labelSmall, color = SkyQuestBlue)
                                                Text(ex.english, style = MaterialTheme.typography.bodyLarge)
                                                Text(ex.korean, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    Tactile3DButton(
                                        text = "퀴즈 시작",
                                        icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                        modifier = Modifier.fillMaxWidth(),
                                        tag = "complete_preview_button",
                                        onClick = onCompletePreview
                                    )
                                }

                                SpartaPhase.MEANING_QUIZ, SpartaPhase.LEVEL_TEST, SpartaPhase.BOSS_BATTLE -> {
                                    if (state.phase == SpartaPhase.MEANING_QUIZ) {
                                        val mnemonic = remember(word.id) { VocabMnemonicEngine.buildGuide(word) }
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = state.learningMethod.labelKo,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = StudioIris
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable(onClick = onToggleRevealAnswer)
                                                        .testTag("toggle_mnemonic_reveal")
                                                ) {
                                                    Text(
                                                        text = if (state.showAnswerRevealed) "힌트 숨기기" else "연상 힌트",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = SkyQuestBlue,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Tactile3DButton(
                                                    text = "알아요",
                                                    containerColor = AdventureEmerald,
                                                    shadowColor = AdventureEmeraldShadow,
                                                    height = 44.dp,
                                                    modifier = Modifier.weight(1f),
                                                    tag = "vocab_know_button",
                                                    onClick = { onMarkWordKnowledge(WordKnowledgeStatus.KNOWN) }
                                                )
                                                Tactile3DButton(
                                                    text = "애매",
                                                    containerColor = TrophyGold,
                                                    shadowColor = TrophyGoldShadow,
                                                    contentColor = Color.White,
                                                    height = 44.dp,
                                                    modifier = Modifier.weight(0.9f),
                                                    tag = "vocab_fuzzy_button",
                                                    onClick = { onMarkWordKnowledge(WordKnowledgeStatus.FUZZY) }
                                                )
                                                Tactile3DButton(
                                                    text = "몰라요",
                                                    containerColor = SpartaRed,
                                                    shadowColor = SpartaRedShadow,
                                                    height = 44.dp,
                                                    modifier = Modifier.weight(1f),
                                                    tag = "vocab_unknown_button",
                                                    onClick = { onMarkWordKnowledge(WordKnowledgeStatus.UNKNOWN) }
                                                )
                                            }

                                            if (state.showAnswerRevealed) {
                                                VisualMnemonicAndChunkCard(
                                                    word = word,
                                                    onSpeakChunk = onSpeakChunk
                                                )
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        state.options.forEachIndexed { idx, option ->
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(50.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { onSubmitAnswer(option) }
                                                    .testTag("vocab_option_$idx")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${idx + 1}. $option",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                SpartaPhase.SENTENCE_SHADOWING -> {
                                    val activeExample = word.examples.getOrNull(state.activeExampleIndex)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        word.examples.forEachIndexed { idx, _ ->
                                            val active = idx == state.activeExampleIndex
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (active) StudioCobalt else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { onSelectExampleIndex(idx) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
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
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Text(activeExample.labelKo, style = MaterialTheme.typography.labelSmall, color = SkyQuestBlue)
                                                Text(activeExample.english, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                                Text(activeExample.korean, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        LoopRepeatMode.entries.forEach { mode ->
                                            val active = state.loopMode == mode
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (active) StudioIris else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { onSetVocabLoopMode(mode) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                                    Text(
                                                        text = mode.labelKo,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                                            text = if (state.isLoopPlaying) "중지" else "예문 듣기",
                                            icon = if (state.isLoopPlaying) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                                            containerColor = SkyQuestBlue,
                                            shadowColor = SkyQuestBlueShadow,
                                            height = 46.dp,
                                            modifier = Modifier.weight(1f),
                                            onClick = { if (state.isLoopPlaying) onStopVoice() else onListenSentence() }
                                        )
                                        Tactile3DButton(
                                            text = "연속 듣기",
                                            icon = Icons.Outlined.Repeat,
                                            containerColor = StudioIris,
                                            shadowColor = RoyalPurpleShadow,
                                            height = 46.dp,
                                            modifier = Modifier.weight(1f),
                                            onClick = onListenAllExamples
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
                                            tag = "sparta_shadowing_mic"
                                        )
                                        Text(
                                            text = if (voiceState.isListening) voiceState.partialTranscript.ifBlank { "예문을 말해 보세요" } else "마이크를 눌러 따라 말하기",
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
                                            placeholder = { Text("문장 직접 입력") },
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
                                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "채점", tint = StudioCobalt)
                                        }
                                    }

                                    Tactile3DButton(
                                        text = "다음 단어",
                                        icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                        containerColor = StudioCobalt,
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
            // ==================== MODE B: 1~3 SENTENCE PATTERN WORD-SWAP STUDIO ====================
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(PatternSentenceRepository.drills) { idx, item ->
                        val selected = idx == patternState.activeDrillIndex
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) StudioIris else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (selected) StudioIris else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPatternDrill(idx) }
                                .testTag("pattern_drill_$idx")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = item.stageTagKo,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) Color.White.copy(alpha = 0.85f) else StudioIris
                                )
                                Text(
                                    text = item.titleKo,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                val drill = patternState.activeDrill
                Elevated3DCard(accentShadow = StudioIris.copy(alpha = 0.32f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(10.dp), color = StudioIris.copy(alpha = 0.14f)) {
                                Text(
                                    text = "${drill.stageTagKo} (${drill.sentenceCount}문장)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StudioIris,
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.PlayCircle, contentDescription = "유튜브 구간", tint = SpartaRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("유튜브 구간", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SpartaRed)
                                }
                            }
                        }

                        Text(
                            text = patternState.renderedEnglish,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (patternState.showKorean) {
                            Text(
                                text = patternState.renderedKorean,
                                style = MaterialTheme.typography.bodyLarge,
                                color = SkyQuestBlue
                            )
                        }

                        drill.slots.forEach { slot ->
                            val selectedOptIdx = patternState.selectedSlotIndices[slot.slotKey] ?: 0
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = slot.labelKo,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StudioIris
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    itemsIndexed(slot.options) { optIdx, (en, ko) ->
                                        val active = optIdx == selectedOptIdx
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (active) StudioCobalt else MaterialTheme.colorScheme.surfaceVariant,
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
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                                text = if (patternState.isLoopPlaying) "반복 중지" else "반복 듣기",
                                icon = if (patternState.isLoopPlaying) Icons.Outlined.Stop else Icons.Outlined.Loop,
                                containerColor = SkyQuestBlue,
                                shadowColor = SkyQuestBlueShadow,
                                height = 46.dp,
                                modifier = Modifier.weight(1f),
                                tag = "pattern_loop_play_button",
                                onClick = { if (patternState.isLoopPlaying) onStopVoice() else onSpeakPattern() }
                            )
                            Tactile3DButton(
                                text = if (patternState.showKorean) "해석 숨김" else "해석 보기",
                                containerColor = CoralOrange,
                                shadowColor = CoralOrangeShadow,
                                height = 46.dp,
                                modifier = Modifier.weight(0.75f),
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
                                text = if (voiceState.isListening) voiceState.partialTranscript.ifBlank { "바꾼 문장을 말해 보세요" } else "마이크를 눌러 따라 말하기",
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
                                placeholder = { Text("문장 직접 입력") },
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
                                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "채점", tint = StudioCobalt)
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

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ModeSegmentTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (selected) accent else Color.Transparent,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VocabSettingsBottomSheetContent(
    state: SpartaDrillState,
    activeStudySec: Int,
    harvestedApiCount: Int,
    harvestStatusKo: String,
    isHarvestingWords: Boolean,
    onSelectTier1To5: (Int) -> Unit,
    onSelectTrack: (VocabStudyTrack) -> Unit,
    onSelectTopic: (VocabTopic) -> Unit,
    onSelectLearningMethod: (VocabLearningMethod) -> Unit,
    onStartLevelTest: () -> Unit,
    onStartPreviewMode: () -> Unit,
    onHarvestDaily50Words: () -> Unit,
    onFlushStudyLog: () -> Unit
) {
    val clockText = "%02d:%02d".format(activeStudySec / 60, activeStudySec % 60)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "학습 설정",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioCobalt.copy(alpha = 0.14f),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onFlushStudyLog)
                    .testTag("flush_study_time_log_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = "시간 저장", tint = StudioCobalt, modifier = Modifier.size(15.dp))
                    Text("시간 저장 ($clockText)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = StudioCobalt)
                }
            }
        }

        // Learning Method
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("학습법", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(VocabLearningMethod.entries) { method ->
                    val active = state.learningMethod == method
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) StudioIris else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectLearningMethod(method) }
                            .testTag("vocab_method_${method.name}")
                    ) {
                        Text(
                            text = method.labelKo,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 1~5 Grade Tier
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("목표 등급", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 4, 3, 2, 1).forEach { tier ->
                    val selected = state.selectedTier1To5 == tier
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) SpartaRed else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTier1To5(tier) }
                            .testTag("vocab_tier_$tier")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${tier}등급",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Study Tracks
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("정렬 트랙", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(VocabStudyTrack.entries) { track ->
                    val active = state.track == track
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) StudioCobalt else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTrack(track) }
                            .testTag("vocab_track_${track.name}")
                    ) {
                        Text(
                            text = track.labelKo,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
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
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Tools: Level Test, Preview, +50 API Harvest
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tactile3DButton(
                text = "레벨테스트",
                containerColor = StudioCoral,
                shadowColor = CoralOrangeShadow,
                height = 44.dp,
                modifier = Modifier.weight(1f),
                tag = "start_level_test_chip",
                onClick = onStartLevelTest
            )
            Tactile3DButton(
                text = "예습 모드",
                containerColor = StudioIris,
                shadowColor = RoyalPurpleShadow,
                height = 44.dp,
                modifier = Modifier.weight(1f),
                onClick = onStartPreviewMode
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !isHarvestingWords, onClick = onHarvestDaily50Words)
                .testTag("harvest_api_50_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "확장 단어 ${harvestedApiCount}개 · $harvestStatusKo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isHarvestingWords) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = SkyQuestBlue, modifier = Modifier.size(16.dp))
                    }
                    Text("+50 수집", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = SkyQuestBlue)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
