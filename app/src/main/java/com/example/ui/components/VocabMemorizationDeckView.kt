package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.VocabRepository
import com.example.engine.VoiceEngineState
import com.example.model.PronunciationReport
import com.example.model.VocabExample
import com.example.model.VocabWord
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.GlacierCyanSoft
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.RoseCrimsonSoft
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCobaltDeep
import com.example.ui.theme.StudioCobaltSoft
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCoralDeep
import com.example.ui.theme.StudioCoralSoft
import com.example.ui.theme.StudioIris
import com.example.ui.theme.StudioIrisSoft

private enum class VocabDeckMode(val labelKo: String, val icon: ImageVector, val tag: String) {
    PRECISION_CARD("카드 정밀", Icons.Outlined.Style, "vocab_mode_precision"),
    SPEED_RAPID("속성 암기", Icons.Outlined.Bolt, "vocab_mode_speed")
}

private enum class PrecisionCardTab(val labelKo: String, val icon: ImageVector, val tag: String) {
    WORD_MEANING("단어·뜻", Icons.Outlined.Translate, "vocab_inner_tab_meaning"),
    EXAMPLES_SPEAK("예문·말하기", Icons.Outlined.RecordVoiceOver, "vocab_inner_tab_examples"),
    MNEMONIC_BLOCKS("해마·3단블록", Icons.Outlined.Lightbulb, "vocab_inner_tab_mnemonic")
}

/**
 * Top-30 EdTech-Inspired Dual-Mode Vocabulary Memorization Studio:
 * - Mode 1 (카드 정밀): Airy single Hero Card with 3 dedicated inner tabs:
 *   1) 단어·뜻 (tap-to-reveal answer/meaning, IPA, audio, clickable SYN-xxxx synonym & antonym chips)
 *   2) 예문·말하기 (dedicated tappable Example tab: Listen, 0.75x Slow, Korean translation toggle,
 *      and live Mic 따라 말하기 with real-time transcript & PronunciationReport)
 *   3) 해마·3단블록 (VisualMnemonicAndChunkCard)
 * - Mode 2 (속성 암기): Distraction-free ultra-fast flashcard-only mode with Auto-Speak ON/OFF,
 *   1-tap center reveal, and thumb-zone 몰라요(복습) / 알아요(다음) instant advance.
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var deckMode by remember { mutableStateOf(VocabDeckMode.PRECISION_CARD) }
    var precisionTab by remember { mutableStateOf(PrecisionCardTab.WORD_MEANING) }
    var isWrongReviewMode by remember { mutableStateOf(false) }
    var reviewAllClearNotice by remember { mutableStateOf(false) }
    var autoSpeakInSpeedMode by remember { mutableStateOf(true) }
    var speedPassedSessionCount by remember { mutableIntStateOf(0) }
    var pendingShadowSentence by remember { mutableStateOf<String?>(null) }

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

    val normalDeck = remember(selectedGrade, harvestedApiWords) {
        val base = VocabRepository.wordsForGrade(selectedGrade).ifEmpty { VocabRepository.allWords }
        val apiFiltered = if (selectedGrade == 0) {
            harvestedApiWords
        } else {
            harvestedApiWords.filter { it.grade == selectedGrade }
        }
        (base + apiFiltered).distinctBy { it.id }
    }
    val activeDeck = if (isWrongReviewMode && wrongWords.isNotEmpty()) wrongWords else normalDeck
    var cardIndex by remember(isWrongReviewMode, selectedGrade, deckMode) { mutableIntStateOf(0) }
    var isMeaningRevealed by remember(isWrongReviewMode, selectedGrade, deckMode, cardIndex) { mutableStateOf(false) }
    val revealedExampleKoMap = remember(isWrongReviewMode, selectedGrade, cardIndex) {
        mutableStateMapOf<Int, Boolean>()
    }

    val safeIndex = cardIndex % activeDeck.size.coerceAtLeast(1)
    val currentWord = activeDeck[safeIndex]
    val examplesList = remember(currentWord.id) {
        currentWord.examples.ifEmpty {
            listOf(
                VocabExample(
                    labelKo = "핵심 실전 예문",
                    english = currentWord.exampleEn,
                    korean = currentWord.exampleKo,
                    ipaHint = currentWord.ipa
                )
            )
        }
    }
    val dailyGoalFraction = (todayWordsCompleted.coerceAtMost(10) / 10f).coerceIn(0f, 1f)

    // Auto-speak word immediately when advancing cards in Speed Rapid mode (if toggled ON)
    LaunchedEffect(deckMode, currentWord.id, autoSpeakInSpeedMode) {
        if (deckMode == VocabDeckMode.SPEED_RAPID && autoSpeakInSpeedMode) {
            onSpeakEnglish(currentWord.word)
        }
    }

    fun advanceCard(known: Boolean) {
        onRecordWordResult(currentWord.id, known)
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Compact Mode Capsule (카드 정밀 vs 속성 암기) + Wrong Review & Weekly Report
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VocabDeckMode.entries.forEach { mode ->
                        val selected = deckMode == mode
                        val accent = if (mode == VocabDeckMode.PRECISION_CARD) StudioCobalt else StudioCoral
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) accent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    deckMode = mode
                                    isMeaningRevealed = false
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = mode.labelKo,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Wrong-answer quick chip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isWrongReviewMode) StudioCoral else StudioCoralSoft,
                border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.25f)),
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (wrongWords.isNotEmpty()) {
                            reviewAllClearNotice = false
                            isWrongReviewMode = !isWrongReviewMode
                        }
                    }
                    .testTag("vocab_toggle_wrong_review")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isWrongReviewMode) "오답중" else "오답 ${wrongWords.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isWrongReviewMode) Color.White else StudioCoral
                    )
                }
            }

            // Report icon button (48dp touch target)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioCobaltSoft,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenWeeklyReport)
                    .testTag("vocab_open_weekly_report")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = "주간 리포트",
                        tint = StudioCobalt,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Ultra-Slim Single-Line Progress Strip (No bulky card wrapper!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .testTag("vocab_daily_goal_strip"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = "연속 학습",
                        tint = StudioCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "연속 ${streakCount}회 · 오늘 ${todayWordsCompleted}/10",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "마스터 ${masteredWordsCount} · R2/API +${harvestedApiCount}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = StudioCobalt
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

        // 3. Airy Single-Row Grade Pills (전체 · 고1 · 고2 · 고3)
        if (!isWrongReviewMode) {
            val gradeSpecs = remember {
                listOf(
                    Triple(0, "전체", StudioIris),
                    Triple(1, "고1", StudioCobalt),
                    Triple(2, "고2", GlacierCyan),
                    Triple(3, "고3", RoseCrimson)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vocab_memo_grade_selector"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                gradeSpecs.forEach { (gradeVal, label, accent) ->
                    val selected = selectedGrade == gradeVal
                    val count = remember(gradeVal, harvestedApiWords) {
                        val baseSize = VocabRepository.wordsForGrade(gradeVal).size
                        val apiSize = if (gradeVal == 0) harvestedApiWords.size else harvestedApiWords.count { it.grade == gradeVal }
                        baseSize + apiSize
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) accent else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) accent else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectGrade(gradeVal) }
                            .testTag("vocab_memo_grade_$gradeVal")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (selected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (reviewAllClearNotice) {
            Surface(
                shape = RoundedCornerShape(14.dp),
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
                            text = "오답 노트 단어를 모두 정복했어요!",
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

        // 4. MAIN FLOATING HERO CARD (Switches cleanly between Precision Card & Speed Rapid Mode)
        AnimatedContent(
            targetState = deckMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "vocab_deck_mode_transition"
        ) { activeMode ->
            if (activeMode == VocabDeckMode.SPEED_RAPID) {
                // =====================================================================
                // MODE B: 속성 암기 (Ultra-Fast Pure Vocabulary Speed Flashcard)
                // =====================================================================
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, StudioCoral.copy(alpha = 0.35f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vocab_speed_flashcard")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top Speed Row: Counter + Auto-Speak Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioCoralSoft
                                ) {
                                    Text(
                                        text = "속성 ${speedPassedSessionCount}단어 통과",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = StudioCoral,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${safeIndex + 1} / ${activeDeck.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (autoSpeakInSpeedMode) StudioCobaltSoft else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (autoSpeakInSpeedMode) StudioCobalt.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { autoSpeakInSpeedMode = !autoSpeakInSpeedMode }
                                    .testTag("vocab_speed_autospeak_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = "자동 발음",
                                        tint = if (autoSpeakInSpeedMode) StudioCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (autoSpeakInSpeedMode) "자동발음 ON" else "자동발음 OFF",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (autoSpeakInSpeedMode) StudioCobalt else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Center Speed Flip Pad (Tap anywhere to flip meaning immediately!)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isMeaningRevealed) StudioCobaltSoft.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (isMeaningRevealed) StudioCobalt.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 215.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { isMeaningRevealed = !isMeaningRevealed }
                                .testTag("vocab_speed_tap_flip_area")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "고${currentWord.grade} · ${currentWord.partOfSpeech}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StudioCobalt
                                )
                                Spacer(Modifier.height(6.dp))
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
                                    text = currentWord.ipa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(14.dp))

                                // Quick Audio Button (does not flip card)
                                Surface(
                                    shape = CircleShape,
                                    color = StudioCobalt,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { onSpeakEnglish(currentWord.word) }
                                        .testTag("vocab_speed_audio_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                            contentDescription = "속성 발음 듣기",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                if (isMeaningRevealed) {
                                    Text(
                                        text = currentWord.meaningKo,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = StudioCobalt,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        text = "카드를 터치해서 정답 뜻 바로 확인",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Instant Thumb-Zone Rapid Decision Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Tactile3DButton(
                                text = "몰라요 (복습)",
                                icon = Icons.Outlined.Refresh,
                                containerColor = StudioCoral,
                                shadowColor = StudioCoralDeep,
                                contentColor = Color.White,
                                height = 54.dp,
                                modifier = Modifier.weight(1f),
                                tag = "vocab_speed_unknown_btn",
                                onClick = { advanceCard(known = false) }
                            )
                            Tactile3DButton(
                                text = "알아요 (다음)",
                                icon = Icons.Outlined.CheckCircle,
                                containerColor = StudioCobalt,
                                shadowColor = StudioCobaltDeep,
                                contentColor = Color.White,
                                height = 54.dp,
                                modifier = Modifier.weight(1f),
                                tag = "vocab_speed_known_btn",
                                onClick = { advanceCard(known = true) }
                            )
                        }
                    }
                }
            } else {
                // =====================================================================
                // MODE A: 카드 정밀 (3 Inner Card Tabs: 단어·뜻 | 예문·말하기 | 해마·3단블록)
                // =====================================================================
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
                        // Top Metadata Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioCobaltSoft
                                ) {
                                    Text(
                                        text = "고${currentWord.grade}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = StudioCobalt,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioCoralSoft
                                ) {
                                    Text(
                                        text = "★${currentWord.csatFrequency}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StudioCoral,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                                if (currentWord.synonymGroupId.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StudioIrisSoft
                                    ) {
                                        Text(
                                            text = currentWord.synonymGroupId,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = StudioIris,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GlacierCyanSoft
                                    ) {
                                        Text(
                                            text = currentWord.topic.labelKo,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = GlacierCyan,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
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

                        // Word Header + Audio / Slow / YouTube Circular Actions
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
                                    text = currentWord.word,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentWord.ipa}  ·  ${currentWord.partOfSpeech}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = RoseCrimsonSoft,
                                    border = BorderStroke(1.dp, RoseCrimson.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .clickable { onOpenYouTubeWord(currentWord.word) }
                                        .testTag("vocab_card_youtube_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.PlayCircle,
                                            contentDescription = "유튜브 실전 발음",
                                            tint = RoseCrimson,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = StudioCobalt,
                                    shadowElevation = 3.dp,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .clickable { onSpeakEnglish(currentWord.word) }
                                        .testTag("vocab_card_audio_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                            contentDescription = "단어 발음 듣기",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3 Crisp Inner Card Tabs: 단어·뜻 | 예문·말하기 | 해마·3단블록
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PrecisionCardTab.entries.forEach { tab ->
                                    val active = precisionTab == tab
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        shadowElevation = if (active) 2.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { precisionTab = tab }
                                            .testTag(tab.tag)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.labelKo,
                                                tint = if (active) StudioCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = tab.labelKo,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold
                                                ),
                                                color = if (active) StudioCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Inner Tab Body
                        when (precisionTab) {
                            PrecisionCardTab.WORD_MEANING -> {
                                // Tap-to-Reveal Answer & Meaning Pad
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isMeaningRevealed) StudioCobaltSoft.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        1.2.dp,
                                        if (isMeaningRevealed) StudioCobalt.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 122.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { isMeaningRevealed = !isMeaningRevealed }
                                        .testTag("vocab_card_reveal_toggle")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = if (isMeaningRevealed) Alignment.Start else Alignment.CenterHorizontally
                                    ) {
                                        if (!isMeaningRevealed) {
                                            Spacer(Modifier.height(10.dp))
                                            Icon(
                                                imageVector = Icons.Outlined.Visibility,
                                                contentDescription = "답과 뜻 보기",
                                                tint = StudioCobalt,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "터치해서 정답 뜻 · 발음 팁 확인하기",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = StudioCobalt
                                            )
                                            Text(
                                                text = "예문 듣기·따라 말하기는 상단 [예문·말하기] 탭을 누르세요",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "정답 뜻",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = StudioCobalt
                                                )
                                                Icon(
                                                    imageVector = Icons.Outlined.VisibilityOff,
                                                    contentDescription = "숨기기",
                                                    tint = StudioCobalt.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = currentWord.meaningKo,
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (currentWord.pronunciationTipKo.isNotBlank()) {
                                                Text(
                                                    text = currentWord.pronunciationTipKo,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Clickable Synonym / Antonym Group Chips (when available from R2 / 3-API Harvester)
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
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = StudioIrisSoft
                                                ) {
                                                    Text(
                                                        text = "유의어",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = StudioIris,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
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
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = StudioCoralSoft
                                                ) {
                                                    Text(
                                                        text = "반의어",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = StudioCoral,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
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
                                // Dedicated Interactive Example Cards (Listen · 0.75x Slow · Translation · Mic Shadowing!)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.testTag("vocab_examples_tab_panel")
                                ) {
                                    examplesList.forEachIndexed { idx, ex ->
                                        val koVisible = revealedExampleKoMap[idx] == true
                                        val isRecordingThis = voiceState.isListening && vocabExampleTarget == ex.english
                                        val reportForThis = if (vocabExampleTarget == ex.english) vocabExamplePronunciation else null

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isRecordingThis) StudioCoralSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
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
                                                verticalArrangement = Arrangement.spacedBy(9.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(7.dp),
                                                        color = StudioCobaltSoft
                                                    ) {
                                                        Text(
                                                            text = "${idx + 1}. ${ex.labelKo}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = StudioCobalt,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = if (koVisible) "해석 숨김" else "해석 보기",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = StudioIris,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { revealedExampleKoMap[idx] = !koVisible }
                                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                                            .testTag("vocab_example_ko_toggle_$idx")
                                                    )
                                                }

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

                                                // Listen · 0.75x Slow · Follow-Along Mic Shadowing Bar
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    ExampleMiniActionChip(
                                                        label = "듣기",
                                                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                                        accent = StudioCobalt,
                                                        bg = StudioCobaltSoft,
                                                        tag = "vocab_ex_listen_$idx",
                                                        modifier = Modifier.weight(0.9f),
                                                        onClick = { onSpeakEnglish(ex.english) }
                                                    )
                                                    ExampleMiniActionChip(
                                                        label = "0.75x",
                                                        icon = Icons.Outlined.SlowMotionVideo,
                                                        accent = GlacierCyan,
                                                        bg = GlacierCyanSoft,
                                                        tag = "vocab_ex_slow_$idx",
                                                        modifier = Modifier.weight(0.9f),
                                                        onClick = { onSpeakSlowSentence(ex.english) }
                                                    )
                                                    ExampleMiniActionChip(
                                                        label = if (isRecordingThis) "중지" else "따라 말하기",
                                                        icon = if (isRecordingThis) Icons.Outlined.Stop else Icons.Outlined.MicNone,
                                                        accent = if (isRecordingThis) Color.White else Color.White,
                                                        bg = if (isRecordingThis) RoseCrimson else StudioCoral,
                                                        tag = "vocab_ex_shadow_$idx",
                                                        modifier = Modifier.weight(1.3f),
                                                        onClick = { triggerExampleShadowing(ex.english) }
                                                    )
                                                }

                                                // Live partial transcript while speaking
                                                if (isRecordingThis) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = voiceState.partialTranscript.ifBlank {
                                                                "마이크 듣는 중... 위 예문을 소리 내어 따라 읽어 보세요!"
                                                            },
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            color = RoseCrimson,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }

                                                // Instant inline pronunciation evaluation report
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

                        // Spacious Bottom 2-Button Mastery Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Tactile3DButton(
                                text = "다시 복습",
                                icon = Icons.Outlined.Refresh,
                                containerColor = StudioCoral,
                                shadowColor = StudioCoralDeep,
                                contentColor = Color.White,
                                height = 52.dp,
                                modifier = Modifier.weight(1f),
                                tag = "vocab_memo_again_btn",
                                onClick = { advanceCard(known = false) }
                            )
                            Tactile3DButton(
                                text = "암기 완료",
                                icon = Icons.Outlined.CheckCircle,
                                containerColor = StudioCobalt,
                                shadowColor = StudioCobaltDeep,
                                contentColor = Color.White,
                                height = 52.dp,
                                modifier = Modifier.weight(1f),
                                tag = "vocab_memo_mastered_btn",
                                onClick = { advanceCard(known = true) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Compact Wrong-Answer Notebook Drawer
        if (wrongWords.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vocab_wrong_notebook_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = "오답 노트",
                                tint = StudioCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "오답 노트 (${wrongWords.size}단어)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (isWrongReviewMode) "전체 덱 보기" else "오답만 집중 복습",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StudioCoral,
                            modifier = Modifier.clickable {
                                isWrongReviewMode = !isWrongReviewMode
                            }
                        )
                    }

                    wrongWords.take(4).forEach { w ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .clickable { onSpeakEnglish(w.word) }
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = w.word,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = w.meaningKo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                    contentDescription = "듣기",
                                    tint = StudioCobalt,
                                    modifier = Modifier.size(18.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioCobaltSoft,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onRecordWordResult(w.id, true) }
                                ) {
                                    Text(
                                        text = "암기",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StudioCobalt,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
private fun ExampleMiniActionChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    bg: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = bg,
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = accent,
                maxLines = 1
            )
        }
    }
}
