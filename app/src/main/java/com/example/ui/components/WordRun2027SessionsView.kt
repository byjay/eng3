package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameJuiceAudioEngine
import com.example.engine.GrammarSpanEngine
import com.example.engine.WordRun2027Engine
import com.example.model.GrammarSpanRole
import com.example.model.WordRunEntry
import com.example.model.WordRunExamSessionResult
import com.example.model.WordRunQuizQuestion
import com.example.model.WordRunUserContext
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import kotlinx.coroutines.delay

internal val WordRunNavy = Color(0xFF101D35)
internal val WordRunLime = Color(0xFFB9F34A)
internal val WordRunLimeInk = Color(0xFF183310)
internal val WordRunAmber = Color(0xFFFFBE68)
internal val WordRunAmberInk = Color(0xFF522B00)

/**
 * Step 02: High-Contrast Flashcard Mastery Session with Morphology Chips,
 * Grammar-Highlighted Context Sentence, Local Context Editor, and 15-Second Focus Break.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordRunFlashcardSessionView(
    day: Int,
    initialQueue: List<WordRunEntry>,
    learnedIds: Set<Int>,
    studyAgainCounts: Map<Int, Int>,
    userContexts: Map<Int, WordRunUserContext>,
    onMarkKnown: (WordRunEntry) -> Unit,
    onMarkAgain: (WordRunEntry) -> Unit,
    onSaveUserContext: (Int, WordRunUserContext?) -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onComplete: () -> Unit,
    onExit: () -> Unit
) {
    var queue by remember(day, initialQueue) { mutableStateOf(initialQueue) }
    var revealMeaning by remember { mutableStateOf(true) }
    var sessionAgainCount by remember { mutableIntStateOf(0) }
    var completedInSession by remember { mutableIntStateOf(0) }
    var toastMsg by remember { mutableStateOf("모르는 단어는 뒤로 재배치되고, 아는 단어만 완료됩니다.") }
    var showBreakDialog by remember { mutableStateOf(false) }
    var showContextEditor by remember { mutableStateOf(false) }
    var showGrammarOverlay by remember { mutableStateOf(true) }

    val currentWord = queue.firstOrNull()
    if (currentWord == null) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    val userCtx = userContexts[currentWord.id]
    val activeSentenceEn = userCtx?.en?.ifBlank { currentWord.exampleEn } ?: currentWord.exampleEn
    val activeSentenceKo = userCtx?.ko?.ifBlank { currentWord.exampleKo } ?: currentWord.exampleKo
    val morphology = remember(currentWord.id, activeSentenceEn) {
        WordRun2027Engine.buildMorphologyInfo(currentWord, activeSentenceEn)
    }

    var editorEn by remember(currentWord.id, userCtx?.en) { mutableStateOf(userCtx?.en ?: "") }
    var editorKo by remember(currentWord.id, userCtx?.ko) { mutableStateOf(userCtx?.ko ?: "") }
    var editorError by remember(currentWord.id) { mutableStateOf<String?>(null) }

    val progressRatio = (learnedIds.size.toFloat() / WordRun2027Engine.WORDS_PER_DAY.toFloat()).coerceIn(0f, 1f)

    if (showBreakDialog) {
        WordRunBreathingBreakDialog(onDone = { showBreakDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wordrun_flashcard_session"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DAY ${day.toString().padStart(2, '0')} · ${WordRun2027Engine.difficultyLabelKo(day)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = StudioCobalt
                )
                Text(
                    text = "플래시카드 암기 ${learnedIds.size}/${WordRun2027Engine.WORDS_PER_DAY}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("wordrun_exit_flashcard")
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "나가기", modifier = Modifier.size(16.dp))
                Text(" 나가기")
            }
        }

        LinearProgressIndicator(
            progress = { progressRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = StudioCobalt,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SessionCountPill("남은 카드 ${queue.size}개", StudioCobalt)
            SessionCountPill("한 번 더 ${sessionAgainCount}회", CoralOrange)
            SessionCountPill("이 단어 재학습 ${studyAgainCounts[currentWord.id] ?: 0}회", RoyalPurple)
        }

        // High-Contrast Navy Flashcard
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = WordRunNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.14f)) {
                        Text(
                            text = "WORD ${currentWord.id.toString().padStart(3, '0')}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = WordRunLime.copy(alpha = 0.22f)) {
                        Text(
                            text = "${morphology.posLabelKo} · 수능 ${WordRun2027Engine.formatQuestionNumberLabel(currentWord.questionNum)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = WordRunLime,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = currentWord.word,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Tap-to-reveal Lime Gloss Pill
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (revealMeaning) WordRunLime else Color(0xFF2D3F5F),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { revealMeaning = !revealMeaning }
                        .testTag("wordrun_toggle_gloss")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = if (revealMeaning) currentWord.meaningKo else "탭하여 한국어 뜻 확인",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = if (revealMeaning) WordRunLimeInk else Color(0xFFD2DDEB),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onSpeakEnglish(currentWord.word) }
                            .testTag("wordrun_speak_word")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "발음 듣기", tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("발음 듣기", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { revealMeaning = !revealMeaning }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = if (revealMeaning) "뜻 가리기" else "뜻 보기",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Dual Large Action Buttons: Again vs Known
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = WordRunAmber,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        onMarkAgain(currentWord)
                        sessionAgainCount++
                        toastMsg = "${currentWord.word} · 큐 뒤로 재배치했습니다."
                        queue = queue.drop(1) + currentWord
                        revealMeaning = true
                    }
                    .testTag("wordrun_btn_again")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, tint = WordRunAmberInk, modifier = Modifier.size(20.dp))
                        Text("한 번 더", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = WordRunAmberInk)
                    }
                    Text("뒤로 보내고 다시 암기", style = MaterialTheme.typography.labelSmall, color = WordRunAmberInk.copy(alpha = 0.85f))
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StudioCobalt,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        onMarkKnown(currentWord)
                        completedInSession++
                        toastMsg = "${currentWord.word} · 암기 완료!"
                        val remaining = queue.drop(1)
                        queue = remaining
                        revealMeaning = true
                        if (remaining.isEmpty()) {
                            onComplete()
                        } else if (completedInSession % 10 == 0) {
                            showBreakDialog = true
                        }
                    }
                    .testTag("wordrun_btn_known")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("안다", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = Color.White)
                    }
                    Text("암기 완료 · 다음 단어", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        Text(
            text = toastMsg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Context + Grammar Spans + Morphology Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("학습 문맥 & 구문 분석", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (showGrammarOverlay) AdventureEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { showGrammarOverlay = !showGrammarOverlay }
                    ) {
                        Text(
                            text = if (showGrammarOverlay) "구문 색칠 ON" else "구문 색칠 OFF",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (showGrammarOverlay) AdventureEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                if (activeSentenceEn.isNotBlank()) {
                    val spans = remember(activeSentenceEn) { GrammarSpanEngine.detectGrammarSpans(activeSentenceEn) }
                    val targetSpan = remember(currentWord.id, activeSentenceEn) {
                        WordRun2027Engine.resolveSurfaceSpanInSentence(currentWord, activeSentenceEn)
                    }
                    val rendered = remember(activeSentenceEn, spans, targetSpan, showGrammarOverlay) {
                        val segs = GrammarSpanEngine.partitionSentenceSegments(
                            sentence = activeSentenceEn,
                            grammarSpans = spans,
                            activeRoles = GrammarSpanRole.entries.toSet(),
                            targetSpan = targetSpan,
                            showGrammarSpans = showGrammarOverlay,
                            clozeEnabled = false,
                            clozeRevealed = false
                        )
                        buildAnnotatedString {
                            segs.forEach { seg ->
                                val roleColor = when (seg.primaryGrammarRole) {
                                    GrammarSpanRole.SUBJECT_CHUNK -> StudioCobalt
                                    GrammarSpanRole.MAIN_VERB -> AdventureEmerald
                                    GrammarSpanRole.RELATIVE_CLAUSE -> RoyalPurple
                                    GrammarSpanRole.PARTICIPLE_PHRASE -> CoralOrange
                                    GrammarSpanRole.PARALLEL_UNIT -> SkyQuestBlue
                                    GrammarSpanRole.PREPOSITIONAL_MODIFIER -> SpartaRed
                                    null -> null
                                }
                                when {
                                    seg.isTargetToken -> withStyle(
                                        SpanStyle(
                                            color = WordRunLimeInk,
                                            background = WordRunLime.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    ) { append(seg.text) }
                                    roleColor != null -> withStyle(
                                        SpanStyle(
                                            color = roleColor,
                                            background = roleColor.copy(alpha = 0.09f),
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) { append(seg.text) }
                                    else -> append(seg.text)
                                }
                            }
                        }
                    }

                    Text(
                        text = userCtx?.label ?: "자체 학습 예문 (공식 시험 원문 미수록)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioCobalt
                    )
                    Text(
                        text = rendered,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
                    )
                    if (activeSentenceKo.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeSentenceKo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { onSpeakEnglish(activeSentenceEn) },
                        modifier = Modifier.heightIn(min = 42.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" 문장 듣기")
                    }
                } else {
                    Text(
                        text = "이 단어의 학습 문맥이 아직 등록되지 않았습니다. 아래에서 직접 학습 문장을 저장할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Morphology Breakdown Chips
                Text("품사 · 활용형 분석", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    morphology.formChips.forEachIndexed { idx, chip ->
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (idx == 0) WordRunLime.copy(alpha = 0.5f) else StudioCobalt.copy(alpha = 0.09f)
                        ) {
                            Text(
                                text = "${chip.labelKo} · ${chip.form}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (idx == 0) WordRunLimeInk else StudioCobalt,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                if (morphology.sentenceMatches.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        morphology.sentenceMatches.forEach { m ->
                            SessionCountPill("문장 속 형태: ${m.form} (${m.labelKo})", AdventureEmerald)
                        }
                    }
                }
                Text(
                    text = morphology.noteKo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Optional Local Context Editor
                OutlinedButton(
                    onClick = { showContextEditor = !showContextEditor },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Text(if (showContextEditor) "문맥 직접 입력 닫기" else "내 학습 문장·해석 직접 등록하기")
                }

                AnimatedVisibility(visible = showContextEditor) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editorEn,
                            onValueChange = { editorEn = it; editorError = null },
                            label = { Text("영어 문장 ('${currentWord.word}' 또는 활용형 포함)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editorKo,
                            onValueChange = { editorKo = it; editorError = null },
                            label = { Text("한국어 해석 (선택)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (editorError != null) {
                            Text(editorError!!, style = MaterialTheme.typography.labelSmall, color = SpartaRed)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val err = WordRun2027Engine.validateUserContext(currentWord, editorEn, editorKo)
                                    if (err != null) {
                                        editorError = err
                                    } else {
                                        onSaveUserContext(
                                            currentWord.id,
                                            WordRunUserContext(
                                                en = editorEn.trim(),
                                                ko = editorKo.trim(),
                                                label = "직접 등록 · 학습 문맥"
                                            )
                                        )
                                        showContextEditor = false
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                            ) { Text("저장") }
                            if (userCtx != null) {
                                OutlinedButton(
                                    onClick = {
                                        onSaveUserContext(currentWord.id, null)
                                        editorEn = ""
                                        editorKo = ""
                                        showContextEditor = false
                                    },
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) { Text("초기화") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 01 (Previous-Day Review 10Q) & Step 03 (Verification Quiz 50Q)
 * Synonym-aware 4-choice drill that re-queues missed words until 100% mastered.
 */
@Composable
fun WordRunQuizSessionView(
    day: Int,
    isPreviousDayReview: Boolean,
    initialWordIds: List<Int>,
    allWordsById: Map<Int, WordRunEntry>,
    allWords: List<WordRunEntry>,
    onWordCorrect: (Int) -> Unit,
    onAddWrongToFsrs: (WordRunEntry) -> Unit,
    onComplete: () -> Unit,
    onExit: () -> Unit
) {
    var queue by remember(day, isPreviousDayReview, initialWordIds) { mutableStateOf(initialWordIds.shuffled()) }
    var doneCount by remember(day, isPreviousDayReview) { mutableIntStateOf(0) }
    val totalCount = remember(initialWordIds) { initialWordIds.size.coerceAtLeast(1) }
    var currentQuestion by remember { mutableStateOf<WordRunQuizQuestion?>(null) }
    var chosenWordId by remember { mutableStateOf<Int?>(null) }
    var comboStreak by remember(day, isPreviousDayReview) { mutableIntStateOf(0) }
    var showBreakDialog by remember { mutableStateOf(false) }

    LaunchedEffect(queue) {
        if (queue.isEmpty()) {
            onComplete()
        } else {
            val nextTarget = allWordsById[queue.first()]
            if (nextTarget != null) {
                currentQuestion = WordRun2027Engine.buildQuizQuestion(
                    target = nextTarget,
                    allWords = allWords,
                    reverseKoreanToEnglish = (doneCount + queue.size) % 2 == 0
                )
                chosenWordId = null
            }
        }
    }

    if (showBreakDialog) {
        WordRunBreathingBreakDialog(onDone = { showBreakDialog = false })
    }

    val q = currentQuestion ?: return
    val targetWord = allWordsById[q.targetWordId] ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wordrun_quiz_session"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isPreviousDayReview) "01 · 전날 오답 우선 복습" else "03 · DAY $day 확인 퀴즈",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = StudioCobalt
                )
                Text(
                    text = "$doneCount/$totalCount 완료",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
            OutlinedButton(onClick = onExit, modifier = Modifier.heightIn(min = 44.dp)) {
                Text("나가기")
            }
        }

        LinearProgressIndicator(
            progress = { (doneCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = AdventureEmerald,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (q.reverseKoreanToEnglish) "올바른 영어 단어는?" else "올바른 한국어 뜻은?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (q.reverseKoreanToEnglish) targetWord.meaningKo else targetWord.word,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center
                )
            }
        }

        q.optionWordIds.forEachIndexed { idx, optId ->
            val optWord = allWordsById[optId] ?: return@forEachIndexed
            val answered = chosenWordId != null
            val isCorrectOpt = optId == q.targetWordId
            val isSelected = chosenWordId == optId
            val bgColor = when {
                answered && isCorrectOpt -> AdventureEmerald.copy(alpha = 0.16f)
                answered && isSelected && !isCorrectOpt -> SpartaRed.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surface
            }
            val borderColor = when {
                answered && isCorrectOpt -> AdventureEmerald
                answered && isSelected && !isCorrectOpt -> SpartaRed
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                border = BorderStroke(1.5.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !answered) {
                        chosenWordId = optId
                        if (optId == q.targetWordId) {
                            comboStreak += 1
                            if (comboStreak == 5) {
                                GameJuiceAudioEngine.playFeverTrigger()
                            } else {
                                GameJuiceAudioEngine.playComboHit(comboStreak)
                            }
                            onWordCorrect(optId)
                        } else {
                            comboStreak = 0
                            GameJuiceAudioEngine.playWrongThud()
                            onAddWrongToFsrs(targetWord)
                        }
                    }
                    .testTag("wordrun_quiz_opt_$idx")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioCobalt.copy(alpha = 0.12f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "ABCD"[idx].toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = StudioCobalt
                            )
                        }
                    }
                    Text(
                        text = if (q.reverseKoreanToEnglish) optWord.word else optWord.meaningKo,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        if (chosenWordId != null) {
            val isCorrect = chosenWordId == q.targetWordId
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = (if (isCorrect) AdventureEmerald else CoralOrange).copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isCorrect) {
                        "정답! ${targetWord.word} = ${targetWord.meaningKo}"
                    } else {
                        "오답 · 다시 출제되며 FSRS 복습함에 기록됨 (${targetWord.word} = ${targetWord.meaningKo})"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isCorrect) AdventureEmerald else SpartaRed,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Button(
                onClick = {
                    val head = queue.first()
                    val tail = queue.drop(1)
                    if (isCorrect) {
                        doneCount++
                        queue = tail
                        if (doneCount % 10 == 0 && tail.isNotEmpty()) {
                            showBreakDialog = true
                        }
                    } else {
                        queue = tail + head
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WordRunLime, contentColor = WordRunLimeInk),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("wordrun_quiz_next")
            ) {
                Text("다음 문항 →", fontWeight = FontWeight.Black)
            }
        }
    }
}

/**
 * Step 04: 20-Minute Timed 50-Question Real Exam (25 EN->KO + 25 KO->EN, >=45/50 Pass).
 */
@Composable
fun WordRunTimedExamSessionView(
    day: Int,
    dayWords: List<WordRunEntry>,
    allWords: List<WordRunEntry>,
    allWordsById: Map<Int, WordRunEntry>,
    onFinishExam: (WordRunExamSessionResult) -> Unit,
    onCancelExam: () -> Unit
) {
    val questions = remember(day, dayWords) {
        WordRun2027Engine.buildExamQuestions(dayWords = dayWords, allWords = allWords)
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var remainingSec by remember { mutableIntStateOf(WordRun2027Engine.EXAM_DURATION_SEC) }
    var submitted by remember { mutableStateOf(false) }

    fun submitNow() {
        if (submitted) return
        submitted = true
        val elapsed = WordRun2027Engine.EXAM_DURATION_SEC - remainingSec
        val result = WordRun2027Engine.gradeExam(
            day = day,
            questions = questions,
            answersByIndex = answers.toMap(),
            elapsedSec = elapsed
        )
        if (result.passed) {
            GameJuiceAudioEngine.playBossDefeated()
        }
        onFinishExam(result)
    }

    LaunchedEffect(submitted) {
        while (!submitted && remainingSec > 0) {
            delay(1000L)
            remainingSec -= 1
            if (remainingSec <= 0) {
                submitNow()
            }
        }
    }

    val q = questions.getOrNull(currentIndex) ?: return
    val targetWord = allWordsById[q.targetWordId] ?: return
    val minutes = remainingSec / 60
    val seconds = remainingSec % 60
    val timerText = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val timerUrgent = remainingSec <= 180

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wordrun_timed_exam_session"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Exam Header with Live Timer
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = WordRunNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAY ${day.toString().padStart(2, '0')} 실전 시험 · ${currentIndex + 1}/${questions.size}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "남은 시간",
                            tint = if (timerUrgent) Color(0xFFFF6B81) else WordRunLime,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (timerUrgent) Color(0xFFFF6B81) else WordRunLime
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { (answers.size.toFloat() / questions.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = WordRunLime,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "응답 완료 ${answers.size}/${questions.size} · 통과 기준 45개(90점)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "중단",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF9AA8),
                        modifier = Modifier.clickable { onCancelExam() }
                    )
                }
            }
        }

        // Question Prompt Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (q.reverseKoreanToEnglish) "Q${currentIndex + 1} · 알맞은 영어 단어를 고르세요" else "Q${currentIndex + 1} · 알맞은 한국어 뜻을 고르세요",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (q.reverseKoreanToEnglish) targetWord.meaningKo else targetWord.word,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center
                )
            }
        }

        val currentAnswer = answers[currentIndex]
        q.optionWordIds.forEachIndexed { idx, optId ->
            val optWord = allWordsById[optId] ?: return@forEachIndexed
            val selected = currentAnswer == optId
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) StudioCobalt.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (selected) 2.dp else 1.2.dp, if (selected) StudioCobalt else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        answers[currentIndex] = optId
                        if (currentIndex < questions.lastIndex) {
                            currentIndex += 1
                        }
                    }
                    .testTag("wordrun_exam_opt_$idx")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) StudioCobalt else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "ABCD"[idx].toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (q.reverseKoreanToEnglish) optWord.word else optWord.meaningKo,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Navigation + Final Submit
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (currentIndex > 0) currentIndex -= 1 },
                enabled = currentIndex > 0,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text("이전") }

            if (currentIndex < questions.lastIndex) {
                Button(
                    onClick = { currentIndex += 1 },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("다음") }
            } else {
                Button(
                    onClick = { submitNow() },
                    colors = ButtonDefaults.buttonColors(containerColor = WordRunLime, contentColor = WordRunLimeInk),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("wordrun_exam_submit")
                ) {
                    Text(
                        text = if (answers.size == questions.size) "최종 제출" else "미응답 포함 제출 (${answers.size}/${questions.size})",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun WordRunBreathingBreakDialog(onDone: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(15) }
    val infinite = rememberInfiniteTransition(label = "breath")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    AlertDialog(
        onDismissRequest = { if (secondsLeft <= 0) onDone() },
        title = { Text("15초 집중 환기", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(WordRunLime),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${secondsLeft}s",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum"
                        ),
                        color = WordRunLimeInk
                    )
                }
                Text(
                    text = "눈을 화면에서 잠시 떼고 깊게 호흡하세요.\n10단어 단위로 기억을 정착시킵니다.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDone,
                enabled = secondsLeft <= 0,
                colors = ButtonDefaults.buttonColors(containerColor = WordRunLime, contentColor = WordRunLimeInk)
            ) {
                Text(if (secondsLeft > 0) "${secondsLeft}초 후 계속" else "학습 계속하기", fontWeight = FontWeight.Black)
            }
        }
    )
}

@Composable
private fun SessionCountPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = accent,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
