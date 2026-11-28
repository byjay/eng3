package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Looks3
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.LooksTwo
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ExamCorpusRepository
import com.example.data.VocabRepository
import com.example.data.WordTranslationOverrideStore
import com.example.engine.HybridAiCoachEngine
import com.example.engine.MemoryPalaceEntry
import com.example.engine.MemoryPalaceRoom
import com.example.engine.RecallGrade
import com.example.engine.Sm2ReviewScheduler
import com.example.engine.TypedRecallResult
import com.example.engine.VocabAnalyticsTracker
import com.example.engine.VocabMnemonicEngine
import com.example.engine.VoiceEngineState
import com.example.engine.WordLearningProgress
import com.example.engine.WordLearningStatus
import com.example.model.PronunciationReport
import com.example.model.VocabExample
import com.example.model.VocabWord
import com.example.model.WrongNoteRecord
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCobaltDeep
import com.example.ui.theme.StudioCobaltSoft
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCoralDeep
import com.example.ui.theme.StudioCoralSoft
import com.example.ui.theme.StudioIris
import com.example.ui.theme.StudioIrisSoft
import com.example.ui.theme.TrophyGold

private enum class VocabDeckMode(val labelKo: String, val icon: ImageVector, val tag: String) {
    PRECISION_CARD("정밀", Icons.Outlined.Style, "vocab_mode_precision"),
    SPEED_RAPID("속성", Icons.Outlined.Bolt, "vocab_mode_speed")
}

private enum class PrecisionCardTab(val labelKo: String, val icon: ImageVector, val tag: String) {
    WORD_MEANING("뜻", Icons.Outlined.Translate, "vocab_inner_tab_meaning"),
    EXAMPLES_SPEAK("예문", Icons.Outlined.RecordVoiceOver, "vocab_inner_tab_examples"),
    MNEMONIC_BLOCKS("연상", Icons.Outlined.Lightbulb, "vocab_inner_tab_mnemonic")
}

private enum class CardStudyMode(val labelKo: String, val icon: ImageVector, val tag: String) {
    LEARN("학습", Icons.Outlined.Style, "card_study_mode_learn"),
    RECALL("회상", Icons.Rounded.Psychology, "card_study_mode_recall"),
    LISTEN("듣기", Icons.Rounded.GraphicEq, "card_study_mode_listen"),
    CLOZE("문장", Icons.Rounded.ShortText, "card_study_mode_cloze")
}

/**
 * Google Stitch Icon-First Dual-Mode Vocabulary Memorization Studio:
 * - Layered squircle StitchIconButtons & StitchIconPillTabs replace cluttered text controls
 * - Mode 1 (정밀 카드): 3 crisp icon tabs — 뜻(답·동의어/반의어) | 예문(듣기·0.75x·해석·따라 말하기) | 연상(해마·3단블록)
 * - Mode 2 (속성 암기): Distraction-free ultra-fast flashcard-only mode with Auto-Speak ON/OFF icon toggle
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabMemorizationDeckView(
    selectedGrade: Int,
    masteredWordsCount: Int,
    streakCount: Int,
    todayWordsCompleted: Int,
    wrongWords: List<VocabWord>,
    harvestedApiWords: List<VocabWord>,
    harvestedApiCount: Int,
    voiceState: VoiceEngineState,
    vocabExamplePronunciation: PronunciationReport?,
    vocabExampleTarget: String,
    onSelectGrade: (Int) -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onSpeakSlowSentence: (String) -> Unit,
    onStartExampleShadowing: (String) -> Unit,
    onClearExamplePronunciation: () -> Unit,
    onStopVoice: () -> Unit,
    onRecordWordResult: (wordId: String, known: Boolean) -> Unit,
    onOpenWeeklyReport: () -> Unit,
    onOpenYouTubeWord: (String) -> Unit,
    onStartSpeakingDrillForWord: (VocabWord) -> Unit,
    modifier: Modifier = Modifier,
    wordSm2Progress: Map<String, WordLearningProgress> = emptyMap(),
    memoryPalaceEntries: Map<String, MemoryPalaceEntry> = emptyMap(),
    isDueReviewMode: Boolean = false,
    onExitDueReviewMode: () -> Unit = {},
    onRecordSm2Recall: ((VocabWord, RecallGrade, Boolean) -> Unit)? = null,
    onSaveMemoryPalace: ((String, MemoryPalaceRoom, String) -> Unit)? = null,
    onLogAudioListen: (() -> Unit)? = null,
    dailyGoalTarget: Int = 10,
    onSubmitTypedRecall: ((VocabWord, String) -> TypedRecallResult)? = null,
    onToggleSuspendWord: ((String, String) -> Unit)? = null,
    wrongNoteRecords: Map<String, WrongNoteRecord> = emptyMap()
) {
    val context = LocalContext.current
    var deckMode by remember { mutableStateOf(VocabDeckMode.PRECISION_CARD) }
    var precisionTab by remember { mutableStateOf(PrecisionCardTab.WORD_MEANING) }
    var cardStudyMode by remember { mutableStateOf(CardStudyMode.LEARN) }
    var isWrongReviewMode by remember { mutableStateOf(false) }
    var reviewAllClearNotice by remember { mutableStateOf(false) }
    var autoSpeakInSpeedMode by remember { mutableStateOf(true) }
    var speedPassedSessionCount by remember { mutableIntStateOf(0) }
    var pendingShadowSentence by remember { mutableStateOf<String?>(null) }
    var palaceAssignWord by remember { mutableStateOf<VocabWord?>(null) }
    var lastGradeTapMs by remember { mutableLongStateOf(0L) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingShadowSentence
        pendingShadowSentence = null
        if (granted && !pending.isNullOrBlank()) {
            onStartExampleShadowing(pending)
        }
    }

    fun triggerExampleShadowing(sentence: String) {
        if (voiceState.isListening && vocabExampleTarget == sentence) {
            onStopVoice()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onStartExampleShadowing(sentence)
        } else {
            pendingShadowSentence = sentence
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (isWrongReviewMode && wrongWords.isEmpty()) {
        isWrongReviewMode = false
        reviewAllClearNotice = true
    }

    val allCombinedWords = remember(harvestedApiWords) {
        (VocabRepository.allWords + harvestedApiWords).distinctBy { it.id }
    }
    val dueWordsDeck = remember(wordSm2Progress, allCombinedWords) {
        val dueIds = Sm2ReviewScheduler.dueWordIds(wordSm2Progress).toSet()
        allCombinedWords.filter { it.id in dueIds }
    }
    if (isDueReviewMode && dueWordsDeck.isEmpty()) {
        LaunchedEffect(Unit) { onExitDueReviewMode() }
    }

    val normalDeck = remember(selectedGrade, harvestedApiWords) {
        val base = VocabRepository.wordsForGrade(selectedGrade).ifEmpty { VocabRepository.allWords }
        val apiFiltered = if (selectedGrade == 0) {
            harvestedApiWords
        } else {
            harvestedApiWords.filter { it.grade == selectedGrade }
        }
        (base + apiFiltered).distinctBy { it.id }
    }
    val baseActiveDeck = when {
        isDueReviewMode && dueWordsDeck.isNotEmpty() -> dueWordsDeck
        isWrongReviewMode && wrongWords.isNotEmpty() -> wrongWords
        else -> normalDeck
    }
    var shuffleSeed by remember(isDueReviewMode, isWrongReviewMode, selectedGrade) { mutableIntStateOf(0) }
    var artVariantIndex by remember { mutableIntStateOf(0) }
    val activeDeck = remember(baseActiveDeck, shuffleSeed) {
        if (shuffleSeed == 0) baseActiveDeck else baseActiveDeck.shuffled(kotlin.random.Random(shuffleSeed))
    }
    var cardIndex by remember(isDueReviewMode, isWrongReviewMode, selectedGrade, deckMode, shuffleSeed) { mutableIntStateOf(0) }
    var isMeaningRevealed by remember(isDueReviewMode, isWrongReviewMode, selectedGrade, deckMode, cardIndex, cardStudyMode) {
        mutableStateOf(false)
    }
    val revealedExampleKoMap = remember(isDueReviewMode, isWrongReviewMode, selectedGrade, cardIndex) {
        mutableStateMapOf<Int, Boolean>()
    }

    val translationOverrides by WordTranslationOverrideStore.overrides.collectAsState()
    LaunchedEffect(context) {
        WordTranslationOverrideStore.ensureLoaded(context)
    }
    var editingWordForTranslation by remember { mutableStateOf<VocabWord?>(null) }
    val aiTranslationCoach = remember { HybridAiCoachEngine() }

    val safeIndex = cardIndex % activeDeck.size.coerceAtLeast(1)
    val rawCurrentWord = activeDeck[safeIndex]
    val currentWord = remember(rawCurrentWord, translationOverrides) {
        WordTranslationOverrideStore.applyTo(rawCurrentWord, translationOverrides)
    }
    val currentSm2 = wordSm2Progress[currentWord.id]
    val isCurrentWordSuspended = currentSm2?.learningStatus == WordLearningStatus.SUSPENDED
    val structuredGuide = remember(currentWord.id) { VocabMnemonicEngine.buildStructuredGuide(currentWord) }
    val activeArtStyle = remember(artVariantIndex) {
        val styles = WordArtPaletteStyle.entries
        styles[Math.floorMod(artVariantIndex, styles.size)]
    }
    var typedRecallInput by remember(currentWord.id, cardStudyMode) { mutableStateOf("") }
    var typedRecallResult by remember(currentWord.id, cardStudyMode) { mutableStateOf<TypedRecallResult?>(null) }
    val examplesList = remember(currentWord.id, currentWord.exampleEn, currentWord.exampleKo, currentWord.examples) {
        currentWord.examples.ifEmpty {
            listOf(
                VocabExample(
                    labelKo = "핵심 예문",
                    english = currentWord.exampleEn,
                    korean = currentWord.exampleKo,
                    ipaHint = currentWord.ipa
                )
            )
        }
    }

    editingWordForTranslation?.let { targetWord ->
        WordTranslationEditorSheet(
            word = targetWord,
            hasCustomOverride = translationOverrides.containsKey(targetWord.id),
            onDismiss = { editingWordForTranslation = null },
            onAiPolish = { w, pos, mKo, exEn, exKo ->
                aiTranslationCoach.polishWordTranslationWithAi(w, pos, mKo, exEn, exKo)
            },
            onSave = { newMeaning, newExEn, newExKo, newTip ->
                WordTranslationOverrideStore.saveOverride(
                    context = context,
                    wordId = targetWord.id,
                    word = targetWord.word,
                    meaningKo = newMeaning,
                    exampleEn = newExEn,
                    exampleKo = newExKo,
                    pronunciationTipKo = newTip
                )
                ExamCorpusRepository.invalidateCache()
            },
            onResetToDefault = {
                WordTranslationOverrideStore.removeOverride(context, targetWord.id)
                ExamCorpusRepository.invalidateCache()
            }
        )
    }
    val safeDailyGoal = dailyGoalTarget.coerceIn(5, 30)
    val dailyGoalFraction = (todayWordsCompleted.coerceAtMost(safeDailyGoal).toFloat() / safeDailyGoal.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(currentWord.id) {
        VocabAnalyticsTracker.log("word_card_viewed", wordId = currentWord.id, meta = currentWord.word)
    }

    LaunchedEffect(precisionTab, currentWord.id) {
        if (precisionTab == PrecisionCardTab.MNEMONIC_BLOCKS) {
            VocabAnalyticsTracker.log("mnemonic_viewed", wordId = currentWord.id, meta = structuredGuide.visualMotif.name)
        }
    }

    LaunchedEffect(deckMode, cardStudyMode, currentWord.id, autoSpeakInSpeedMode) {
        if ((deckMode == VocabDeckMode.SPEED_RAPID && autoSpeakInSpeedMode) ||
            (deckMode == VocabDeckMode.PRECISION_CARD && cardStudyMode == CardStudyMode.LISTEN)
        ) {
            onSpeakEnglish(currentWord.word)
        }
    }

    fun advanceCard(known: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastGradeTapMs < 300L) return
        lastGradeTapMs = now
        val grade = if (known) RecallGrade.GOOD else RecallGrade.AGAIN
        if (onRecordSm2Recall != null) {
            onRecordSm2Recall(currentWord, grade, precisionTab == PrecisionCardTab.MNEMONIC_BLOCKS)
        } else {
            onRecordWordResult(currentWord.id, known)
        }
        onClearExamplePronunciation()
        if (deckMode == VocabDeckMode.SPEED_RAPID && known) {
            speedPassedSessionCount += 1
        }
        cardIndex += 1
        isMeaningRevealed = false
        if (deckMode == VocabDeckMode.PRECISION_CARD) {
            precisionTab = PrecisionCardTab.WORD_MEANING
        }
    }

    fun advanceWithSm2Grade(grade: RecallGrade) {
        val now = System.currentTimeMillis()
        if (now - lastGradeTapMs < 300L) return
        lastGradeTapMs = now
        if (onRecordSm2Recall != null) {
            onRecordSm2Recall(currentWord, grade, precisionTab == PrecisionCardTab.MNEMONIC_BLOCKS || cardStudyMode == CardStudyMode.RECALL)
        } else {
            onRecordWordResult(currentWord.id, grade == RecallGrade.GOOD || grade == RecallGrade.EASY)
        }
        onClearExamplePronunciation()
        cardIndex += 1
        isMeaningRevealed = false
        precisionTab = PrecisionCardTab.WORD_MEANING
    }

    palaceAssignWord?.let { targetWord ->
        if (onSaveMemoryPalace != null) {
            MemoryPalaceAssignDialog(
                word = targetWord,
                existingEntry = memoryPalaceEntries[targetWord.id],
                onDismiss = { palaceAssignWord = null },
                onSave = { room, memo ->
                    onSaveMemoryPalace(targetWord.id, room, memo)
                }
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Google Stitch Icon Dock: [정밀 | 속성] + [오답 배지 아이콘] + [주간 리포트 아이콘]
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
                    VocabDeckMode.entries.forEach { mode ->
                        StitchIconPillTab(
                            icon = mode.icon,
                            label = mode.labelKo,
                            selected = deckMode == mode,
                            accent = if (mode == VocabDeckMode.PRECISION_CARD) StudioCobalt else StudioCoral,
                            tag = mode.tag,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                deckMode = mode
                                isMeaningRevealed = false
                            }
                        )
                    }
                }
            }

            StitchIconButton(
                icon = Icons.Outlined.BookmarkBorder,
                contentDescription = "오답 복습",
                accent = StudioCoral,
                filled = isWrongReviewMode,
                size = 52.dp,
                iconSize = 22.dp,
                badgeCount = wrongWords.size,
                tag = "vocab_toggle_wrong_review",
                onClick = {
                    if (wrongWords.isNotEmpty()) {
                        reviewAllClearNotice = false
                        isWrongReviewMode = !isWrongReviewMode
                    }
                }
            )

            StitchIconButton(
                icon = Icons.Outlined.BarChart,
                contentDescription = "주간 리포트",
                accent = StudioCobalt,
                filled = false,
                size = 52.dp,
                iconSize = 22.dp,
                tag = "vocab_open_weekly_report",
                onClick = onOpenWeeklyReport
            )
        }

        // 2. Compact Stitch Icon Metric Strip + Sleek Daily Goal Line
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vocab_daily_goal_strip"),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StitchIconMetricChip(
                        icon = Icons.Outlined.LocalFireDepartment,
                        valueText = "$streakCount",
                        accent = StudioCoral,
                        contentDescription = "연속 학습"
                    )
                    StitchIconMetricChip(
                        icon = Icons.Outlined.CheckCircle,
                        valueText = "$todayWordsCompleted/$safeDailyGoal",
                        accent = StudioCobalt,
                        contentDescription = "오늘 목표"
                    )
                }
                StitchIconMetricChip(
                    icon = Icons.Outlined.CloudDone,
                    valueText = "$masteredWordsCount · +$harvestedApiCount",
                    accent = StudioIris,
                    contentDescription = "누적 마스터 및 R2 API 수집"
                )
            }
            LinearProgressIndicator(
                progress = { dailyGoalFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (deckMode == VocabDeckMode.SPEED_RAPID) StudioCoral else StudioCobalt,
                trackColor = StudioCobaltSoft,
                strokeCap = StrokeCap.Round
            )
        }

        // 3. Google Stitch Iconified Grade Dock (전체 · 고1 · 고2 · 고3)
        if (!isWrongReviewMode) {
            val gradeSpecs = remember {
                listOf(
                    Triple(0, Icons.Outlined.AllInclusive to "전체", StudioIris),
                    Triple(1, Icons.Outlined.LooksOne to "고1", StudioCobalt),
                    Triple(2, Icons.Outlined.LooksTwo to "고2", GlacierCyan),
                    Triple(3, Icons.Outlined.Looks3 to "고3", RoseCrimson)
                )
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vocab_memo_grade_selector")
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    gradeSpecs.forEach { (gradeVal, iconAndLabel, accent) ->
                        val count = remember(gradeVal, harvestedApiWords) {
                            val baseSize = VocabRepository.wordsForGrade(gradeVal).size
                            val apiSize = if (gradeVal == 0) harvestedApiWords.size else harvestedApiWords.count { it.grade == gradeVal }
                            baseSize + apiSize
                        }
                        StitchIconPillTab(
                            icon = iconAndLabel.first,
                            label = iconAndLabel.second,
                            selected = selectedGrade == gradeVal,
                            accent = accent,
                            badgeText = "$count",
                            tag = "vocab_memo_grade_$gradeVal",
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectGrade(gradeVal) }
                        )
                    }
                }
            }
        }

        if (reviewAllClearNotice) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StudioCobaltSoft,
                border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = StudioCobalt, modifier = Modifier.size(18.dp))
                        Text(
                            text = "오답 정복 완료!",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = StudioCobalt
                        )
                    }
                    Text(
                        text = "닫기",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudioCobalt,
                        modifier = Modifier.clickable { reviewAllClearNotice = false }
                    )
                }
            }
        }

        // 4. MAIN FLOATING HERO CARD (Stitch Icon-First Precision vs Speed Rapid)
        AnimatedContent(
            targetState = deckMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "vocab_deck_mode_transition"
        ) { activeMode ->
            if (activeMode == VocabDeckMode.SPEED_RAPID) {
                // === MODE B: 속성 암기 (Ultra-Fast Pure Icon-First Speed Card) ===
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.2.dp, StudioCoral.copy(alpha = 0.32f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vocab_speed_flashcard")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StitchIconMetricChip(
                                icon = Icons.Outlined.Bolt,
                                valueText = "$speedPassedSessionCount · ${safeIndex + 1}/${activeDeck.size}",
                                accent = StudioCoral,
                                contentDescription = "속성 진행"
                            )
                            StitchIconButton(
                                icon = if (autoSpeakInSpeedMode) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                                contentDescription = if (autoSpeakInSpeedMode) "자동 발음 켜짐" else "자동 발음 꺼짐",
                                accent = if (autoSpeakInSpeedMode) StudioCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                                filled = autoSpeakInSpeedMode,
                                size = 44.dp,
                                iconSize = 20.dp,
                                tag = "vocab_speed_autospeak_toggle",
                                onClick = { autoSpeakInSpeedMode = !autoSpeakInSpeedMode }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isMeaningRevealed) StudioCobaltSoft.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (isMeaningRevealed) StudioCobalt.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 210.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { isMeaningRevealed = !isMeaningRevealed }
                                .testTag("vocab_speed_tap_flip_area")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentWord.word,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.6).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${currentWord.ipa} · ${currentWord.partOfSpeech}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StitchIconButton(
                                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = "발음 듣기",
                                        accent = StudioCobalt,
                                        filled = true,
                                        size = 50.dp,
                                        iconSize = 24.dp,
                                        tag = "vocab_speed_audio_btn",
                                        onClick = { onSpeakEnglish(currentWord.word) }
                                    )
                                    StitchIconButton(
                                        icon = Icons.Outlined.Edit,
                                        contentDescription = "단어 및 한글 번역 수정",
                                        accent = StudioIris,
                                        size = 46.dp,
                                        iconSize = 20.dp,
                                        tag = "vocab_speed_edit_translation_btn",
                                        onClick = { editingWordForTranslation = currentWord }
                                    )
                                }

                                Spacer(Modifier.height(14.dp))

                                if (isMeaningRevealed) {
                                    Text(
                                        text = currentWord.meaningKo,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = StudioCobalt,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    StitchIconBadge(
                                        icon = Icons.Outlined.Visibility,
                                        contentDescription = "뜻 보기",
                                        accent = StudioCobalt,
                                        size = 38.dp,
                                        iconSize = 18.dp
                                    )
                                }
                            }
                        }

                        // Material 3 Learning Mode Buttons: 오답 노트 / 완벽 암기 + 듣기
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MaterialLearningButton(
                                text = "오답 노트",
                                icon = Icons.Rounded.BookmarkAdd,
                                accent = StudioCoral,
                                style = MaterialButtonStyle.OUTLINED,
                                modifier = Modifier.weight(1f),
                                tag = "vocab_speed_unknown_btn",
                                onClick = { advanceCard(known = false) }
                            )
                            MaterialLearningButton(
                                text = "완벽 암기",
                                icon = Icons.Rounded.Verified,
                                accent = StudioCobalt,
                                style = MaterialButtonStyle.FILLED,
                                modifier = Modifier.weight(1f),
                                tag = "vocab_speed_known_btn",
                                onClick = { advanceCard(known = true) }
                            )
                        }
                    }
                }
            } else {
                // === MODE A: 정밀 카드 (Stitch 3-Icon Tab Studio: 뜻 | 예문 | 연상) ===
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (isWrongReviewMode) StudioCoral.copy(alpha = 0.45f) else StudioCobalt.copy(alpha = 0.22f)
                    ),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vocab_hero_flashcard")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Compact Badge Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(8.dp), color = StudioCobaltSoft) {
                                    Text(
                                        text = "고${currentWord.grade}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = StudioCobalt,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = StudioCoralSoft) {
                                    Text(
                                        text = "★${currentWord.csatFrequency}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StudioCoral,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                                if (currentWord.synonymGroupId.isNotBlank()) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = StudioIrisSoft) {
                                        Text(
                                            text = currentWord.synonymGroupId,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = StudioIris,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onToggleSuspendWord != null) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isCurrentWordSuspended) RoseCrimson.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { onToggleSuspendWord(currentWord.id, currentWord.word) }
                                            .testTag("vocab_card_suspend_toggle")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Block,
                                                contentDescription = if (isCurrentWordSuspended) "복습 제외 해제" else "복습에서 제외",
                                                tint = if (isCurrentWordSuspended) RoseCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = if (isCurrentWordSuspended) "제외됨" else "숨김",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isCurrentWordSuspended) RoseCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${safeIndex + 1} / ${activeDeck.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Full-Bleed Semantic Art-First 3D-Flip Flashcard Centerpiece (Word-Matched Vector Scene)
                        val flipAngle by animateFloatAsState(
                            targetValue = if (isMeaningRevealed) 180f else 0f,
                            animationSpec = tween(durationMillis = 420),
                            label = "vocab_art_card_flip"
                        )
                        val showBackFace = flipAngle > 90f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(215.dp)
                                .graphicsLayer {
                                    rotationY = flipAngle
                                    cameraDistance = 12f * density
                                }
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { isMeaningRevealed = !isMeaningRevealed }
                                .testTag("vocab_image_first_header")
                        ) {
                            if (!showBackFace) {
                                // === FRONT FACE: Word-Matched Semantic Scene Illustration + Frosted-Glass Pill ===
                                WordSceneIllustration(
                                    word = currentWord,
                                    styleIndex = artVariantIndex,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF0F172A).copy(alpha = 0.24f),
                                                    Color.Transparent,
                                                    Color(0xFF0F172A).copy(alpha = 0.54f)
                                                )
                                            )
                                        )
                                )

                                // Top-left semantic motif & due badge + Top-right Memory Palace quick pin
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopStart)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isDueReviewMode) StudioCoral else Color(0xFF0F172A).copy(alpha = 0.68f)
                                    ) {
                                        Text(
                                            text = if (isDueReviewMode) {
                                                "SM-2 복습 · ${structuredGuide.visualMotif.labelKo}"
                                            } else {
                                                "${structuredGuide.visualMotif.labelKo} · ${activeArtStyle.labelKo}"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (onSaveMemoryPalace != null) {
                                        val savedRoom = memoryPalaceEntries[currentWord.id]?.room
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (savedRoom != null) Color(savedRoom.accentHex) else Color.White.copy(alpha = 0.92f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .clickable { palaceAssignWord = currentWord }
                                                .testTag("vocab_card_palace_pin_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Map,
                                                    contentDescription = "기억의 궁전 배치",
                                                    tint = if (savedRoom != null) Color.White else StudioIris,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = savedRoom?.titleKo?.substringBefore(" ·") ?: "궁전 배치",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = if (savedRoom != null) Color.White else StudioIris
                                                )
                                            }
                                        }
                                    }
                                }

                                // Floating Frosted-Glass Overlay Pill at Bottom (Reference Image 2: [🔊] ? / word [✨])
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.88f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.95f)),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 11.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = StudioCobalt.copy(alpha = 0.14f),
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    onSpeakEnglish(currentWord.word)
                                                    onLogAudioListen?.invoke()
                                                }
                                                .testTag("vocab_art_pill_audio_btn")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                                    contentDescription = "발음 듣기",
                                                    tint = StudioCobalt,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = if (cardStudyMode == CardStudyMode.LEARN) "?" else "탭해서 뒤집기",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = when (cardStudyMode) {
                                                    CardStudyMode.RECALL -> currentWord.meaningKo
                                                    CardStudyMode.LISTEN -> "소리를 듣고 맞혀보세요"
                                                    else -> currentWord.word
                                                },
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = StudioCobalt,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = RoyalPurple.copy(alpha = 0.14f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = "카드 뒤집기 힌트",
                                                    tint = RoyalPurple,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // === BACK FACE (counter-rotated 180° for crisp readability) ===
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4C1D95))
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = TrophyGold.copy(alpha = 0.22f)
                                            ) {
                                                Text(
                                                    text = structuredGuide.sceneTitleKo,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = TrophyGold,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = "탭해서 앞면으로",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFC7D2FE)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = currentWord.word,
                                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                                color = Color.White
                                            )
                                            Text(
                                                text = currentWord.meaningKo,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = TrophyGold
                                            )
                                            Text(
                                                text = structuredGuide.sceneDescriptionKo,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFE0E7FF),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White.copy(alpha = 0.12f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (structuredGuide.hasVerifiedMorpheme) {
                                                    "검증 어근: ${structuredGuide.morphemeBreakdown}"
                                                } else {
                                                    "예문: ${examplesList.firstOrNull()?.english ?: currentWord.exampleEn}"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Progress Dots Indicator + Tactile SHUFFLE / FLIP / ART Control Row (Reference Image 2)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val dotCount = activeDeck.size.coerceIn(1, 7)
                            val activeDot = safeIndex % dotCount
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(dotCount) { idx ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (idx == activeDot) 9.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (idx == activeDot) RoyalPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                            )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vocab_art_action_bar"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MaterialLearningButton(
                                    text = "셔플",
                                    icon = Icons.Rounded.Shuffle,
                                    accent = SkyQuestBlue,
                                    style = MaterialButtonStyle.FILLED,
                                    tag = "vocab_art_shuffle_btn",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        shuffleSeed += 1
                                        isMeaningRevealed = false
                                    }
                                )
                                MaterialLearningButton(
                                    text = "뒤집기",
                                    icon = Icons.Rounded.Flip,
                                    accent = RoyalPurple,
                                    style = MaterialButtonStyle.FILLED,
                                    tag = "vocab_art_flip_btn",
                                    modifier = Modifier.weight(1.1f),
                                    onClick = { isMeaningRevealed = !isMeaningRevealed }
                                )
                                MaterialLearningButton(
                                    text = "아트",
                                    icon = Icons.Rounded.Palette,
                                    accent = CoralOrange,
                                    style = MaterialButtonStyle.FILLED,
                                    tag = "vocab_art_style_btn",
                                    modifier = Modifier.weight(0.95f),
                                    onClick = {
                                        artVariantIndex = (artVariantIndex + 1) % WordArtPaletteStyle.entries.size
                                    }
                                )
                            }
                        }

                        // 4 Card Study Modes: 학습 | 회상 | 듣기 | 문장 (Spec Section 3.3)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vocab_card_study_mode_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                CardStudyMode.entries.forEach { mode ->
                                    val selected = cardStudyMode == mode
                                    val modeColor = when (mode) {
                                        CardStudyMode.LEARN -> StudioCobalt
                                        CardStudyMode.RECALL -> StudioIris
                                        CardStudyMode.LISTEN -> GlacierCyan
                                        CardStudyMode.CLOZE -> StudioCoral
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) modeColor else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                cardStudyMode = mode
                                                isMeaningRevealed = false
                                                precisionTab = PrecisionCardTab.WORD_MEANING
                                            }
                                            .testTag(mode.tag)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = mode.icon,
                                                contentDescription = mode.labelKo,
                                                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = mode.labelKo,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Word Centerpiece + Pure Google Stitch Icon Actions (Slow · YouTube · Audio)
                        val hideWordOnFront = (cardStudyMode == CardStudyMode.RECALL || cardStudyMode == CardStudyMode.LISTEN) && !isMeaningRevealed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = when {
                                        hideWordOnFront && cardStudyMode == CardStudyMode.RECALL -> currentWord.meaningKo
                                        hideWordOnFront && cardStudyMode == CardStudyMode.LISTEN -> "소리를 듣고 맞혀보세요"
                                        else -> currentWord.word
                                    },
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontSize = if (hideWordOnFront) 24.sp else 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = if (hideWordOnFront) StudioIris else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hideWordOnFront) {
                                        "${currentWord.partOfSpeech} · 아래 카드를 눌러 정답 확인"
                                    } else {
                                        "${currentWord.ipa} · ${currentWord.partOfSpeech}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StitchIconButton(
                                    icon = Icons.Outlined.Edit,
                                    contentDescription = "단어 및 한글 번역 수정",
                                    accent = StudioIris,
                                    size = 44.dp,
                                    iconSize = 20.dp,
                                    tag = "vocab_card_edit_translation_btn",
                                    onClick = { editingWordForTranslation = currentWord }
                                )
                                StitchIconButton(
                                    icon = Icons.Outlined.PlayCircle,
                                    contentDescription = "유튜브 실전 발음",
                                    accent = RoseCrimson,
                                    size = 44.dp,
                                    iconSize = 21.dp,
                                    tag = "vocab_card_youtube_btn",
                                    onClick = { onOpenYouTubeWord(currentWord.word) }
                                )
                                StitchIconButton(
                                    icon = Icons.Outlined.SlowMotionVideo,
                                    contentDescription = "느리게 발음 듣기",
                                    accent = GlacierCyan,
                                    size = 44.dp,
                                    iconSize = 21.dp,
                                    tag = "vocab_card_slow_btn",
                                    onClick = { onSpeakSlowSentence(currentWord.word) }
                                )
                                StitchIconButton(
                                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                    contentDescription = "단어 발음 듣기",
                                    accent = StudioCobalt,
                                    filled = true,
                                    size = 44.dp,
                                    iconSize = 21.dp,
                                    tag = "vocab_card_audio_btn",
                                    onClick = {
                                        onSpeakEnglish(currentWord.word)
                                        onLogAudioListen?.invoke()
                                    }
                                )
                            }
                        }

                        // 3 Google Stitch Expressive Icon Tabs inside Card: 뜻 | 예문 | 연상
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PrecisionCardTab.entries.forEach { tab ->
                                    StitchIconPillTab(
                                        icon = tab.icon,
                                        label = tab.labelKo,
                                        selected = precisionTab == tab,
                                        accent = when (tab) {
                                            PrecisionCardTab.WORD_MEANING -> StudioCobalt
                                            PrecisionCardTab.EXAMPLES_SPEAK -> StudioCoral
                                            PrecisionCardTab.MNEMONIC_BLOCKS -> StudioIris
                                        },
                                        tag = tab.tag,
                                        modifier = Modifier.weight(1f),
                                        onClick = { precisionTab = tab }
                                    )
                                }
                            }
                        }

                        // Inner Tab Body
                        when (precisionTab) {
                            PrecisionCardTab.WORD_MEANING -> {
                                val firstExample = examplesList.firstOrNull()
                                val clozeSentence = remember(currentWord.id, firstExample?.english) {
                                    val base = firstExample?.english ?: currentWord.exampleEn
                                    val regex = Regex("\\b${Regex.escape(currentWord.word)}\\w*\\b", RegexOption.IGNORE_CASE)
                                    if (regex.containsMatchIn(base)) regex.replace(base, "_____") else "$base (_____)"
                                }
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isMeaningRevealed) StudioCobaltSoft.copy(alpha = 0.62f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isMeaningRevealed) StudioCobalt.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 114.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { isMeaningRevealed = !isMeaningRevealed }
                                        .testTag("vocab_card_reveal_toggle")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = if (isMeaningRevealed || cardStudyMode == CardStudyMode.CLOZE) Alignment.Start else Alignment.CenterHorizontally
                                    ) {
                                        if (!isMeaningRevealed) {
                                            when (cardStudyMode) {
                                                CardStudyMode.CLOZE -> {
                                                    Text(
                                                        text = "빈칸 문장 추론",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = StudioCoral
                                                    )
                                                    Text(
                                                        text = clozeSentence,
                                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "터치해서 빈칸 정답과 해석 확인",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = StudioCobalt
                                                    )
                                                }
                                                CardStudyMode.RECALL -> {
                                                    StitchIconBadge(
                                                        icon = Icons.Rounded.Psychology,
                                                        contentDescription = "영어 단어 회상",
                                                        accent = StudioIris,
                                                        size = 42.dp,
                                                        iconSize = 21.dp
                                                    )
                                                    Text(
                                                        text = structuredGuide.memoryQuestion,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "터치 · 영어 철자 & 발음 확인",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = StudioIris
                                                    )
                                                }
                                                CardStudyMode.LISTEN -> {
                                                    StitchIconBadge(
                                                        icon = Icons.Rounded.GraphicEq,
                                                        contentDescription = "듣고 맞히기",
                                                        accent = GlacierCyan,
                                                        size = 44.dp,
                                                        iconSize = 22.dp
                                                    )
                                                    Text(
                                                        text = "발음을 듣고 단어와 뜻을 떠올린 뒤 터치하세요",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = GlacierCyan,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                                CardStudyMode.LEARN -> {
                                                    Spacer(Modifier.height(4.dp))
                                                    StitchIconBadge(
                                                        icon = Icons.Outlined.Visibility,
                                                        contentDescription = "뜻 보기",
                                                        accent = StudioCobalt,
                                                        size = 44.dp,
                                                        iconSize = 22.dp
                                                    )
                                                    Text(
                                                        text = "터치 · 뜻 & 장면 해석 확인",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = StudioCobalt
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (cardStudyMode == CardStudyMode.RECALL || cardStudyMode == CardStudyMode.LISTEN) {
                                                        "${currentWord.word} — ${currentWord.meaningKo}"
                                                    } else {
                                                        currentWord.meaningKo
                                                    },
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = Icons.Outlined.VisibilityOff,
                                                    contentDescription = "숨기기",
                                                    tint = StudioCobalt,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            if (cardStudyMode == CardStudyMode.CLOZE && firstExample != null) {
                                                Text(
                                                    text = firstExample.english,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = StudioCobalt
                                                )
                                                Text(
                                                    text = firstExample.korean,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else if (currentWord.pronunciationTipKo.isNotBlank()) {
                                                Text(
                                                    text = currentWord.pronunciationTipKo,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Inline Typed Active Recall Input (Spec Section 3.3 & 12: 1-typo tolerance for >=5 letters)
                                if (cardStudyMode != CardStudyMode.LEARN && onSubmitTypedRecall != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("vocab_typed_recall_panel"),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = typedRecallInput,
                                                onValueChange = { typedRecallInput = it },
                                                placeholder = { Text("영어 철자 직접 입력") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("vocab_typed_recall_input")
                                            )
                                            Button(
                                                onClick = {
                                                    val res = onSubmitTypedRecall(currentWord, typedRecallInput)
                                                    typedRecallResult = res
                                                    if (res.isPassed) {
                                                        isMeaningRevealed = true
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = StudioIris,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .height(52.dp)
                                                    .testTag("vocab_typed_recall_submit_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                                    contentDescription = "철자 채점",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "확인",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                                )
                                            }
                                        }
                                        typedRecallResult?.let { res ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (res.isPassed) AdventureEmerald.copy(alpha = 0.14f) else RoseCrimson.copy(alpha = 0.12f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = res.feedbackKo,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (res.isPassed) AdventureEmerald else RoseCrimson,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (currentWord.synonyms.isNotEmpty() || currentWord.antonyms.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.testTag("vocab_syn_ant_chips")
                                    ) {
                                        if (currentWord.synonyms.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(shape = RoundedCornerShape(8.dp), color = StudioIrisSoft) {
                                                    Text(
                                                        text = "≈",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = StudioIris,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                currentWord.synonyms.take(5).forEach { syn ->
                                                    Surface(
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(1.dp, StudioIris.copy(alpha = 0.35f)),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .clickable { onSpeakEnglish(syn) }
                                                    ) {
                                                        Text(
                                                            text = syn,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (currentWord.antonyms.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(shape = RoundedCornerShape(8.dp), color = StudioCoralSoft) {
                                                    Text(
                                                        text = "≠",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = StudioCoral,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                currentWord.antonyms.take(4).forEach { ant ->
                                                    Surface(
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.35f)),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .clickable { onSpeakEnglish(ant) }
                                                    ) {
                                                        Text(
                                                            text = ant,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            PrecisionCardTab.EXAMPLES_SPEAK -> {
                                // Pure Stitch Icon-Driven Interactive Example Cards
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.testTag("vocab_examples_tab_panel")
                                ) {
                                    examplesList.forEachIndexed { idx, ex ->
                                        val koVisible = revealedExampleKoMap[idx] == true
                                        val isRecordingThis = voiceState.isListening && vocabExampleTarget == ex.english
                                        val reportForThis = if (vocabExampleTarget == ex.english) vocabExamplePronunciation else null

                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (isRecordingThis) StudioCoralSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                                            border = BorderStroke(
                                                1.2.dp,
                                                if (isRecordingThis) StudioCoral else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("vocab_example_card_$idx")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(13.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = ex.english,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        lineHeight = 23.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onSpeakEnglish(ex.english) }
                                                )

                                                AnimatedVisibility(visible = koVisible) {
                                                    Text(
                                                        text = ex.korean,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                // Clean Material 3 Icon Buttons: 듣기 · 0.75x · 해석 · 말하기
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    MaterialLearningButton(
                                                        text = "듣기",
                                                        icon = Icons.Rounded.Headphones,
                                                        accent = StudioCobalt,
                                                        style = MaterialButtonStyle.TONAL,
                                                        tag = "vocab_ex_listen_$idx",
                                                        modifier = Modifier.weight(1f),
                                                        onClick = { onSpeakEnglish(ex.english) }
                                                    )
                                                    StitchIconButton(
                                                        icon = Icons.Outlined.SlowMotionVideo,
                                                        contentDescription = "느리게 듣기",
                                                        accent = GlacierCyan,
                                                        size = 46.dp,
                                                        iconSize = 20.dp,
                                                        tag = "vocab_ex_slow_$idx",
                                                        onClick = { onSpeakSlowSentence(ex.english) }
                                                    )
                                                    StitchIconButton(
                                                        icon = Icons.Outlined.Translate,
                                                        contentDescription = "해석 보기 토글",
                                                        accent = StudioIris,
                                                        filled = koVisible,
                                                        size = 46.dp,
                                                        iconSize = 20.dp,
                                                        tag = "vocab_example_ko_toggle_$idx",
                                                        onClick = { revealedExampleKoMap[idx] = !koVisible }
                                                    )
                                                    MaterialLearningButton(
                                                        text = if (isRecordingThis) "중지" else "말하기",
                                                        icon = if (isRecordingThis) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                                        accent = if (isRecordingThis) RoseCrimson else StudioCoral,
                                                        style = MaterialButtonStyle.FILLED,
                                                        tag = "vocab_ex_shadow_$idx",
                                                        modifier = Modifier.weight(1.1f),
                                                        onClick = { triggerExampleShadowing(ex.english) }
                                                    )
                                                }

                                                if (isRecordingThis) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = voiceState.partialTranscript.ifBlank {
                                                                "듣는 중... 예문을 따라 말해 보세요"
                                                            },
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            color = RoseCrimson,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }

                                                if (reportForThis != null) {
                                                    PronunciationReportCard(
                                                        report = reportForThis,
                                                        onCompareNativeClick = { onSpeakEnglish(ex.english) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            PrecisionCardTab.MNEMONIC_BLOCKS -> {
                                VisualMnemonicAndChunkCard(
                                    word = currentWord,
                                    onSpeakChunk = onSpeakEnglish,
                                    onStartChunkSpeakingDrill = { onStartSpeakingDrillForWord(currentWord) }
                                )
                            }
                        }

                        // Expandable Usage History, Priority Tier & Linked Grammar Drawer
                        WordCorpusExpandablePanel(
                            word = currentWord,
                            onSpeakEnglish = onSpeakEnglish
                        )

                        // SM-2 4-Step Spaced Repetition Recall Bar (Spec Section 5.1)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vocab_sm2_recall_bar"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "간격 반복 자가 평가 (FSRS)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (currentSm2 != null) {
                                    Text(
                                        text = "숙련도 ${currentSm2.masteryScore}% · ${currentSm2.repetition}회 성공",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AdventureEmerald
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RecallGrade.entries.forEach { grade ->
                                    val intervalLabel = remember(currentWord.id, currentSm2, grade) {
                                        com.example.engine.FsrsSchedulerEngine.previewIntervalLabel(currentSm2, currentWord.id, grade)
                                    }
                                    val accent = when (grade) {
                                        RecallGrade.AGAIN -> RoseCrimson
                                        RecallGrade.HARD -> CoralOrange
                                        RecallGrade.GOOD -> StudioCobalt
                                        RecallGrade.EASY -> AdventureEmerald
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = accent.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { advanceWithSm2Grade(grade) }
                                            .testTag("sm2_grade_${grade.name}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = grade.labelKo,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = accent,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = intervalLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Clean Material Design 3 4-Mode Action Grid: 듣기 · 말하기 · 오답 노트 · 완벽 암기
                        val primarySentence = examplesList.firstOrNull()?.english ?: currentWord.exampleEn
                        val isListeningPrimary = voiceState.isListening && vocabExampleTarget == primarySentence
                        LearningModeMaterialButtonGrid(
                            listenAccent = StudioCobalt,
                            speakAccent = StudioCoral,
                            wrongAccent = RoseCrimson,
                            masterAccent = StudioCobalt,
                            isSpeakingActive = isListeningPrimary,
                            isWrongNoteMode = isWrongReviewMode,
                            wrongCount = wrongWords.size,
                            onListenClick = {
                                if (precisionTab == PrecisionCardTab.EXAMPLES_SPEAK) {
                                    onSpeakEnglish(primarySentence)
                                } else {
                                    onSpeakEnglish(currentWord.word)
                                }
                                onLogAudioListen?.invoke()
                            },
                            onSpeakClick = {
                                precisionTab = PrecisionCardTab.EXAMPLES_SPEAK
                                triggerExampleShadowing(primarySentence)
                            },
                            onWrongNoteClick = { advanceCard(known = false) },
                            onMasteredClick = { advanceCard(known = true) }
                        )
                    }
                }
            }
        }

        // 5. Wrong Answer Notebook with Filtering Tabs (전체 / 아직 학습 필요 / 완벽 암기) & Learning Dates
        WrongAnswerNotebookSection(
            allWords = allCombinedWords,
            wrongNoteRecords = wrongNoteRecords,
            isWrongReviewMode = isWrongReviewMode,
            onToggleWrongReviewMode = {
                if (wrongWords.isNotEmpty() || isWrongReviewMode) {
                    isWrongReviewMode = !isWrongReviewMode
                }
            },
            onSpeakWord = onSpeakEnglish,
            onMarkWordResult = onRecordWordResult
        )
    }
}
