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
import androidx.compose.material.icons.outlined.Analytics
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
import com.example.data.SquirrelRpgRepository
import com.example.data.VocabRepository
import com.example.engine.LearningMission
import com.example.engine.MediaReviewStatus
import com.example.engine.MemoryPalaceEntry
import com.example.engine.MemoryPalaceRoom
import com.example.engine.RecallGrade
import com.example.engine.RewardTransaction
import com.example.engine.TypedRecallResult
import com.example.engine.VocabMediaResolver
import com.example.engine.VocabMnemonicEngine
import com.example.engine.VoiceEngineState
import com.example.ui.components.instantQuizTouch
import com.example.engine.WordLearningProgress
import com.example.model.LoopRepeatMode
import com.example.model.PronunciationReport
import com.example.model.SquirrelJobHero
import com.example.model.UserProgress
import com.example.model.VocabLearningMethod
import com.example.model.VocabStudyTrack
import com.example.model.VocabTopic
import com.example.model.VocabWord
import com.example.model.WordKnowledgeStatus
import com.example.model.WrongNoteRecord
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.MaterialButtonStyle
import com.example.ui.components.MaterialLearningButton
import com.example.ui.components.MediaAuditAndAnalyticsSheetContent
import com.example.ui.components.PronunciationReportCard
import com.example.ui.components.StitchIconButton
import com.example.ui.components.StitchIconPillTab
import com.example.ui.components.SystematicStudyLabSection
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.VisualMnemonicAndChunkCard
import com.example.ui.components.VocabAdventureHubSection
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
    harvestedApiWords: List<VocabWord>,
    harvestedApiCount: Int,
    harvestStatusKo: String,
    isHarvestingWords: Boolean,
    vocabExamplePronunciation: PronunciationReport?,
    vocabExampleTarget: String,
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
    onSpeakSlowSentence: (String) -> Unit,
    onStartExampleShadowing: (String) -> Unit,
    onClearExamplePronunciation: () -> Unit,
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
    onEvaluateTypedPattern: (String) -> Unit,
    userProgress: UserProgress = UserProgress(),
    wordSm2Progress: Map<String, WordLearningProgress> = emptyMap(),
    dailyVocabMissions: List<LearningMission> = emptyList(),
    memoryPalaceEntries: Map<String, MemoryPalaceEntry> = emptyMap(),
    onRecordSm2Recall: (VocabWord, RecallGrade, Boolean) -> Unit = { w, g, _ ->
        onRecordFocusWordResult(w.id, g == RecallGrade.GOOD || g == RecallGrade.EASY)
    },
    onClaimMission: (String) -> Unit = {},
    onSaveMemoryPalace: (String, MemoryPalaceRoom, String) -> Unit = { _, _, _ -> },
    onOpenEnGameHub: () -> Unit = {},
    onLogAudioListen: () -> Unit = {},
    activeSquirrelHero: SquirrelJobHero = SquirrelRpgRepository.findHeroById(""),
    playerNickname: String = "모험가",
    weakWordIds: Set<String> = emptySet(),
    dailyGoalTarget: Int = 10,
    rewardLedger: List<RewardTransaction> = emptyList(),
    mediaReviewOverrides: Map<String, MediaReviewStatus> = emptyMap(),
    onSetDailyWordGoal: (Int) -> Unit = {},
    onToggleSuspendWord: (String, String) -> Unit = { _, _ -> },
    onSubmitTypedRecall: ((VocabWord, String) -> TypedRecallResult)? = null,
    onSetMediaReviewStatus: (String, MediaReviewStatus) -> Unit = { _, _ -> },
    dueReviewLaunchNonce: Int = 0,
    wrongNoteRecords: Map<String, WrongNoteRecord> = emptyMap(),
    csatDDayDays: Int = com.example.engine.FsrsSchedulerEngine.DEFAULT_DAYS_UNTIL_CSAT,
    onSetCsatDDayDays: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var activeSubMode by remember { mutableIntStateOf(0) } // 0 = 단어암기 카드, 1 = 퀴즈·발음, 2 = 패턴 블록
    var isDueReviewMode by remember { mutableStateOf(false) }
    var manualInput by remember(state.currentWord?.id, activeSubMode, patternState.activeDrillIndex) { mutableStateOf("") }
    var youtubeClipQuery by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAuditSheet by remember { mutableStateOf(false) }

    LaunchedEffect(dueReviewLaunchNonce) {
        if (dueReviewLaunchNonce > 0) {
            activeSubMode = 0
            isDueReviewMode = true
        }
    }

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
                dailyGoalTarget = dailyGoalTarget,
                harvestedApiWords = harvestedApiWords,
                harvestedApiCount = harvestedApiCount,
                harvestStatusKo = harvestStatusKo,
                isHarvestingWords = isHarvestingWords,
                mediaReviewOverrides = mediaReviewOverrides,
                onSelectDailyWordGoal = onSetDailyWordGoal,
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
                },
                onOpenAuditAndAnalytics = {
                    showSettingsSheet = false
                    showAuditSheet = true
                }
            )
        }
    }

    if (showAuditSheet) {
        val combinedAuditWords = remember(harvestedApiWords) {
            (VocabRepository.allWords + harvestedApiWords).distinctBy { it.id }
        }
        ModalBottomSheet(
            onDismissRequest = { showAuditSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MediaAuditAndAnalyticsSheetContent(
                allWords = combinedAuditWords,
                reviewOverrides = mediaReviewOverrides,
                rewardLedger = rewardLedger,
                onSetMediaReviewStatus = onSetMediaReviewStatus,
                onClose = { showAuditSheet = false }
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
        // 1. Google Stitch Icon-First Mode Dock: 암기 | 퀴즈 | 패턴 + 설정 아이콘
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StitchIconPillTab(
                            icon = Icons.Outlined.Style,
                            label = "암기",
                            selected = activeSubMode == 0,
                            accent = StudioCobalt,
                            tag = "tab_vocab_flashcards",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSubMode = 0 }
                        )
                        StitchIconPillTab(
                            icon = Icons.Outlined.Quiz,
                            label = "퀴즈",
                            selected = activeSubMode == 1,
                            accent = SkyQuestBlue,
                            tag = "tab_vocab_anti_abc",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSubMode = 1 }
                        )
                        StitchIconPillTab(
                            icon = Icons.Outlined.SwapHoriz,
                            label = "패턴",
                            selected = activeSubMode == 2,
                            accent = StudioIris,
                            tag = "tab_pattern_studio",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSubMode = 2 }
                        )
                        StitchIconPillTab(
                            icon = Icons.Outlined.Analytics,
                            label = "분석·문법",
                            selected = activeSubMode == 3,
                            accent = AdventureEmerald,
                            tag = "tab_systematic_lab",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSubMode = 3 }
                        )
                    }
                }
                if (activeSubMode == 0 || activeSubMode == 1) {
                    StitchIconButton(
                        icon = Icons.Outlined.Tune,
                        contentDescription = "학습 설정",
                        accent = StudioCobalt,
                        size = 52.dp,
                        iconSize = 22.dp,
                        tag = "vocab_open_settings_sheet",
                        onClick = { showSettingsSheet = true }
                    )
                }
            }
        }

        if (activeSubMode == 0) {
            // === MODE 0: v2 VOCAB ADVENTURE HUB + IMAGE-FIRST MEMORIZATION STUDIO ===
            item {
                val combinedPool = remember(harvestedApiWords) {
                    (VocabRepository.allWords + harvestedApiWords).distinctBy { it.id }
                }
                VocabAdventureHubSection(
                    progress = userProgress,
                    wordSm2Progress = wordSm2Progress,
                    dailyMissions = dailyVocabMissions,
                    memoryPalaceEntries = memoryPalaceEntries,
                    allWordsPool = combinedPool,
                    isDueReviewActive = isDueReviewMode,
                    onToggleDueReview = { isDueReviewMode = !isDueReviewMode },
                    onClaimMission = onClaimMission,
                    onOpenEnGameHub = onOpenEnGameHub,
                    onSpeakEnglish = onSpeakChunk,
                    onSaveMemoryPalace = onSaveMemoryPalace,
                    hero = activeSquirrelHero,
                    playerNickname = playerNickname,
                    weakWordIds = weakWordIds,
                    masteredWordsCount = masteredWordsCount,
                    rewardLedger = rewardLedger
                )
            }
            item {
                VocabMemorizationDeckView(
                    selectedGrade = state.grade,
                    masteredWordsCount = masteredWordsCount,
                    streakCount = streakCount,
                    todayWordsCompleted = todayWordsCompleted,
                    wrongWords = wrongWords,
                    harvestedApiWords = harvestedApiWords,
                    harvestedApiCount = harvestedApiCount,
                    voiceState = voiceState,
                    vocabExamplePronunciation = vocabExamplePronunciation,
                    vocabExampleTarget = vocabExampleTarget,
                    onSelectGrade = onSelectGrade,
                    onSpeakEnglish = onSpeakChunk,
                    onSpeakSlowSentence = onSpeakSlowSentence,
                    onStartExampleShadowing = onStartExampleShadowing,
                    onClearExamplePronunciation = onClearExamplePronunciation,
                    onStopVoice = onStopVoice,
                    onRecordWordResult = onRecordFocusWordResult,
                    onOpenWeeklyReport = onOpenWeeklyReport,
                    onOpenYouTubeWord = { query -> youtubeClipQuery = query },
                    onStartSpeakingDrillForWord = { targetWord ->
                        val sentence = targetWord.examples.firstOrNull()?.english ?: targetWord.exampleEn
                        onSpeakChunk(sentence)
                        activeSubMode = 1
                    },
                    wordSm2Progress = wordSm2Progress,
                    memoryPalaceEntries = memoryPalaceEntries,
                    isDueReviewMode = isDueReviewMode,
                    onExitDueReviewMode = { isDueReviewMode = false },
                    onRecordSm2Recall = onRecordSm2Recall,
                    onSaveMemoryPalace = onSaveMemoryPalace,
                    onLogAudioListen = onLogAudioListen,
                    dailyGoalTarget = dailyGoalTarget,
                    onSubmitTypedRecall = onSubmitTypedRecall,
                    onToggleSuspendWord = onToggleSuspendWord,
                    wrongNoteRecords = wrongNoteRecords
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
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                MaterialLearningButton(
                                                    text = "완벽 암기",
                                                    icon = Icons.Rounded.Verified,
                                                    accent = StudioCobalt,
                                                    style = MaterialButtonStyle.FILLED,
                                                    modifier = Modifier.weight(1.1f),
                                                    tag = "vocab_know_button",
                                                    onClick = { onMarkWordKnowledge(WordKnowledgeStatus.KNOWN) }
                                                )
                                                MaterialLearningButton(
                                                    text = "애매",
                                                    icon = Icons.Rounded.HelpOutline,
                                                    accent = TrophyGoldShadow,
                                                    style = MaterialButtonStyle.TONAL,
                                                    modifier = Modifier.weight(0.85f),
                                                    tag = "vocab_fuzzy_button",
                                                    onClick = { onMarkWordKnowledge(WordKnowledgeStatus.FUZZY) }
                                                )
                                                MaterialLearningButton(
                                                    text = "오답 노트",
                                                    icon = Icons.Rounded.BookmarkAdd,
                                                    accent = SpartaRed,
                                                    style = MaterialButtonStyle.OUTLINED,
                                                    modifier = Modifier.weight(1.1f),
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
                                                    .instantQuizTouch { onSubmitAnswer(option) }
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
                                        MaterialLearningButton(
                                            text = if (state.isLoopPlaying) "중지" else "듣기",
                                            icon = if (state.isLoopPlaying) Icons.Outlined.Stop else Icons.Rounded.Headphones,
                                            accent = StudioCobalt,
                                            style = MaterialButtonStyle.FILLED,
                                            modifier = Modifier.weight(1f),
                                            onClick = { if (state.isLoopPlaying) onStopVoice() else onListenSentence() }
                                        )
                                        MaterialLearningButton(
                                            text = "연속 듣기",
                                            icon = Icons.Outlined.Repeat,
                                            accent = StudioIris,
                                            style = MaterialButtonStyle.TONAL,
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
        } else if (activeSubMode == 3) {
            // ==================== MODE 3: SYSTEMATIC CORPUS ANALYTICS & 6-CATEGORY GRAMMAR LAB ====================
            item {
                SystematicStudyLabSection(
                    harvestedApiWords = harvestedApiWords,
                    wordSm2Progress = wordSm2Progress,
                    onSpeakEnglish = onSpeakChunk,
                    onRecordSm2Recall = onRecordSm2Recall,
                    csatDDayDays = csatDDayDays,
                    onSetCsatDDayDays = onSetCsatDDayDays
                )
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
                            MaterialLearningButton(
                                text = if (patternState.isLoopPlaying) "반복 중지" else "반복 듣기",
                                icon = if (patternState.isLoopPlaying) Icons.Outlined.Stop else Icons.Outlined.Loop,
                                accent = StudioCobalt,
                                style = MaterialButtonStyle.FILLED,
                                modifier = Modifier.weight(1f),
                                tag = "pattern_loop_play_button",
                                onClick = { if (patternState.isLoopPlaying) onStopVoice() else onSpeakPattern() }
                            )
                            MaterialLearningButton(
                                text = if (patternState.showKorean) "해석 숨김" else "해석 보기",
                                icon = Icons.Rounded.Translate,
                                accent = StudioCoral,
                                style = MaterialButtonStyle.TONAL,
                                modifier = Modifier.weight(0.85f),
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
    dailyGoalTarget: Int,
    harvestedApiWords: List<VocabWord>,
    harvestedApiCount: Int,
    harvestStatusKo: String,
    isHarvestingWords: Boolean,
    mediaReviewOverrides: Map<String, MediaReviewStatus>,
    onSelectDailyWordGoal: (Int) -> Unit,
    onSelectTier1To5: (Int) -> Unit,
    onSelectTrack: (VocabStudyTrack) -> Unit,
    onSelectTopic: (VocabTopic) -> Unit,
    onSelectLearningMethod: (VocabLearningMethod) -> Unit,
    onStartLevelTest: () -> Unit,
    onStartPreviewMode: () -> Unit,
    onHarvestDaily50Words: () -> Unit,
    onFlushStudyLog: () -> Unit,
    onOpenAuditAndAnalytics: () -> Unit
) {
    val clockText = "%02d:%02d".format(activeStudySec / 60, activeStudySec % 60)
    val auditSummary = remember(harvestedApiWords, mediaReviewOverrides) {
        VocabMediaResolver.auditStats(
            words = (VocabRepository.allWords + harvestedApiWords).distinctBy { it.id },
            reviewOverrides = mediaReviewOverrides
        )
    }

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

        // Learning Method with M3 Visual Icons
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("학습법", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VocabLearningMethod.entries) { method ->
                    val active = state.learningMethod == method
                    val methodIcon = when (method) {
                        VocabLearningMethod.HIPPOCAMPUS -> Icons.Rounded.Lightbulb
                        VocabLearningMethod.WORD_ASSOCIATION -> Icons.Rounded.Extension
                        VocabLearningMethod.EBBINGHAUS_SM2 -> Icons.Rounded.Replay
                        VocabLearningMethod.ACTIVE_RECALL -> Icons.Rounded.Visibility
                        VocabLearningMethod.CONTEXT_SPEAKING -> Icons.Rounded.RecordVoiceOver
                    }
                    MaterialLearningButton(
                        text = method.labelKo,
                        icon = methodIcon,
                        accent = StudioIris,
                        style = if (active) MaterialButtonStyle.FILLED else MaterialButtonStyle.TONAL,
                        tag = "vocab_method_${method.name}",
                        onClick = { onSelectLearningMethod(method) }
                    )
                }
            }
        }

        // Daily Word Goal Customization (5 / 10 / 15 / 20 words)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "하루 단어 목표 (${dailyGoalTarget}개)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 10, 15, 20).forEach { goalVal ->
                    val selected = dailyGoalTarget == goalVal
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) AdventureEmerald else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectDailyWordGoal(goalVal) }
                            .testTag("vocab_daily_goal_$goalVal")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${goalVal}단어",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
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

        // Quick Tools: Level Test, Preview (Material 3 Icon Buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaterialLearningButton(
                text = "레벨테스트",
                icon = Icons.Rounded.School,
                accent = StudioCoral,
                style = MaterialButtonStyle.FILLED,
                modifier = Modifier.weight(1f),
                tag = "start_level_test_chip",
                onClick = onStartLevelTest
            )
            MaterialLearningButton(
                text = "예습 모드",
                icon = Icons.Rounded.Visibility,
                accent = StudioIris,
                style = MaterialButtonStyle.TONAL,
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

        // P7 Media & Mnemonic Audit Status Dashboard + Section 13 Analytics Launcher
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AdventureEmerald.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, AdventureEmerald.copy(alpha = 0.32f)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenAuditAndAnalytics)
                .testTag("vocab_media_audit_card")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "이미지·연상 검수 & 학습 분석 (P7)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = AdventureEmerald
                    )
                    Text(
                        text = "상세 열기 >",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioCobalt
                    )
                }
                Text(
                    text = "총 ${auditSummary.totalWords}단어 · 의미 일러스트 ${auditSummary.bundledVisualCount}개 · 검증 어근 ${auditSummary.verifiedMorphemeCount}개 · 검수 대기 ${auditSummary.needsHumanReviewCount}개",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "라이선스: ${auditSummary.approvedLicenseLabel}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
