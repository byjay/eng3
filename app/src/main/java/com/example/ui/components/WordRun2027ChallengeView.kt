package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WordRun2027Repository
import com.example.engine.RecallGrade
import com.example.engine.WordRun2027Engine
import com.example.model.VocabWord
import com.example.model.WordRunChallengeState
import com.example.model.WordRunDayProgress
import com.example.model.WordRunEntry
import com.example.model.WordRunExamRecord
import com.example.model.WordRunExamSessionResult
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WordRunSubScreen {
    HUB,
    DAY_MISSION,
    WORD_LIST,
    FLASHCARD,
    PREV_REVIEW,
    VERIFY_QUIZ,
    TIMED_EXAM,
    EXAM_RESULT
}

/**
 * WORD RUN 2027 · 500-Word 10-Day CSAT Challenge Hub & Curriculum Orchestrator.
 * Integrates seamlessly into SystematicStudyLabSection and bridges wrong answers into FSRS.
 */
@Composable
fun WordRun2027ChallengeView(
    onSpeakEnglish: (String) -> Unit,
    onRecordFsrsWord: (VocabWord, RecallGrade, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allWords by remember { mutableStateOf<List<WordRunEntry>>(emptyList()) }
    var state by remember { mutableStateOf(WordRunChallengeState()) }
    var subScreen by remember { mutableStateOf(WordRunSubScreen.HUB) }
    var lastExamResult by remember { mutableStateOf<WordRunExamSessionResult?>(null) }
    var statusToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        val loadedWords = withContext(Dispatchers.IO) { WordRun2027Repository.loadAllWords(context) }
        val loadedState = withContext(Dispatchers.IO) { WordRun2027Repository.loadState(context) }
        allWords = loadedWords
        val activeDay = WordRun2027Engine.resolveCurrentDay(loadedState)
        state = loadedState.copy(selectedDay = activeDay)
    }

    fun updateState(transform: (WordRunChallengeState) -> WordRunChallengeState) {
        val updated = transform(state)
        state = updated
        scope.launch(Dispatchers.IO) {
            WordRun2027Repository.saveState(context, updated)
        }
    }

    val allWordsById = remember(allWords) { allWords.associateBy { it.id } }
    val selectedDay = state.selectedDay.coerceIn(1, WordRun2027Engine.TOTAL_DAYS)
    val dayWords = remember(allWords, selectedDay) { allWords.filter { it.day == selectedDay } }
    val dayProgress = state.dayProgress[selectedDay] ?: WordRunDayProgress(day = selectedDay)

    if (allWords.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = "2027 수능 500단어 챌린지 데이터를 불러오는 중입니다...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wordrun_2027_challenge_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!statusToast.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AdventureEmerald.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusToast!!,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AdventureEmerald,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        when (subScreen) {
            WordRunSubScreen.HUB -> {
                WordRunHubView(
                    state = state,
                    totalWords = allWords.size,
                    onStartToday = {
                        val todayDay = WordRun2027Engine.resolveCurrentDay(state)
                        updateState { it.copy(selectedDay = todayDay) }
                        subScreen = WordRunSubScreen.DAY_MISSION
                    },
                    onSelectDay = { day ->
                        updateState { it.copy(selectedDay = day) }
                        subScreen = WordRunSubScreen.DAY_MISSION
                    },
                    onToggleFreePractice = { enabled ->
                        updateState { it.copy(freePracticeUnlocked = enabled) }
                    }
                )
            }

            WordRunSubScreen.DAY_MISSION -> {
                val reviewDone = selectedDay == 1 || dayProgress.reviewCompleted
                val learnDone = dayProgress.learnedIds.size >= dayWords.size && dayWords.isNotEmpty()
                val practiceDone = dayProgress.practicedIds.size >= dayWords.size && dayWords.isNotEmpty()
                val examReady = reviewDone && learnDone && practiceDone

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { subScreen = WordRunSubScreen.HUB },
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "챌린지 홈", modifier = Modifier.size(16.dp))
                        Text(" 10일 로드맵")
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = WordRunLime) {
                        Text(
                            text = "DAY ${selectedDay.toString().padStart(2, '0')} · ${WordRun2027Engine.difficultyLabelKo(selectedDay)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = WordRunLimeInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Step 01: Previous Day Review
                MissionStepCard(
                    stepLabel = "01 · 전날 오답 우선 복습",
                    badge = if (reviewDone) "완료" else "필수",
                    badgeGood = reviewDone,
                    subtitle = if (selectedDay == 1) "DAY 01은 전날 복습이 면제됩니다." else "전날 오답 우선 10문항 · 모두 맞혀야 완료",
                    progressText = null,
                    actionLabel = if (selectedDay > 1) "전날 복습 10문항 시작" else null,
                    actionEnabled = selectedDay > 1,
                    primaryAccent = StudioCobalt,
                    onAction = { subScreen = WordRunSubScreen.PREV_REVIEW }
                )

                // Step 02: Flashcard Memorization
                MissionStepCard(
                    stepLabel = "02 · 플래시카드 암기 & 형태소 분석",
                    badge = "${dayProgress.learnedIds.size}/${dayWords.size}",
                    badgeGood = learnDone,
                    subtitle = "품사·활용형·구문 하이라이트 확인 · 10개마다 15초 호흡 환기",
                    progressText = null,
                    progressFraction = (dayProgress.learnedIds.size.toFloat() / dayWords.size.coerceAtLeast(1).toFloat()),
                    actionLabel = if (learnDone) "50개 카드 다시 복습" else "카드 암기 시작 →",
                    actionEnabled = reviewDone || state.freePracticeUnlocked,
                    primaryAccent = WordRunLime,
                    onPrimaryColor = WordRunLimeInk,
                    onAction = { subScreen = WordRunSubScreen.FLASHCARD }
                )

                // Step 03: Verification Quiz
                MissionStepCard(
                    stepLabel = "03 · 유의어 변별 확인 퀴즈",
                    badge = "${dayProgress.practicedIds.size}/${dayWords.size}",
                    badgeGood = practiceDone,
                    subtitle = "틀린 단어는 큐 끝으로 재출제 · 50개 전부 정답 처리",
                    progressText = null,
                    progressFraction = (dayProgress.practicedIds.size.toFloat() / dayWords.size.coerceAtLeast(1).toFloat()),
                    actionLabel = "확인 퀴즈 시작",
                    actionEnabled = (reviewDone && learnDone) || state.freePracticeUnlocked,
                    primaryAccent = StudioCobalt,
                    onAction = { subScreen = WordRunSubScreen.VERIFY_QUIZ }
                )

                // Step 04: 20-Min Real Exam
                MissionStepCard(
                    stepLabel = "04 · 20분 50문항 실전 시험",
                    badge = if (dayProgress.passedDateKst.isNotBlank()) "통과 (${dayProgress.passedDateKst})" else if (examReady || state.freePracticeUnlocked) "응시 가능" else "잠김",
                    badgeGood = dayProgress.passedDateKst.isNotBlank() || examReady,
                    subtitle = "영→한 25 + 한→영 25 혼합 · 20분 제한 · 45개(90점) 이상 통과",
                    progressText = null,
                    actionLabel = "실전 시험 시작",
                    actionEnabled = examReady || state.freePracticeUnlocked,
                    primaryAccent = WordRunNavy,
                    onAction = { subScreen = WordRunSubScreen.TIMED_EXAM }
                )

                OutlinedButton(
                    onClick = { subScreen = WordRunSubScreen.WORD_LIST },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("wordrun_preview_words_btn")
                ) {
                    Text("DAY ${selectedDay.toString().padStart(2, '0')} 단어 50개 전체 목록 보기")
                }
            }

            WordRunSubScreen.WORD_LIST -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAY ${selectedDay.toString().padStart(2, '0')} · ${dayWords.size}개 전체 단어",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    OutlinedButton(onClick = { subScreen = WordRunSubScreen.DAY_MISSION }, modifier = Modifier.heightIn(min = 44.dp)) {
                        Text("미션으로")
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayWords.forEach { entry ->
                            val isLearned = entry.id in dayProgress.learnedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(entry.word, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold))
                                        if (isLearned) {
                                            Icon(Icons.Outlined.CheckCircle, contentDescription = "암기 완료", tint = AdventureEmerald, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    Text(
                                        text = "${WordRun2027Engine.posLabelKo(entry.pos)} · 수능 ${WordRun2027Engine.formatQuestionNumberLabel(entry.questionNum)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = entry.meaningKo,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StudioCobalt
                                )
                                IconButton(onClick = { onSpeakEnglish(entry.word) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "${entry.word} 발음", tint = StudioCobalt, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            WordRunSubScreen.FLASHCARD -> {
                val pending = dayWords.filterNot { it.id in dayProgress.learnedIds }.ifEmpty { dayWords }
                WordRunFlashcardSessionView(
                    day = selectedDay,
                    initialQueue = pending,
                    learnedIds = dayProgress.learnedIds,
                    studyAgainCounts = state.studyAgainCounts,
                    userContexts = state.userContexts,
                    onMarkKnown = { entry ->
                        updateState { st ->
                            val cur = st.dayProgress[selectedDay] ?: WordRunDayProgress(selectedDay)
                            st.copy(dayProgress = st.dayProgress + (selectedDay to cur.copy(learnedIds = cur.learnedIds + entry.id)))
                        }
                    },
                    onMarkAgain = { entry ->
                        updateState { st ->
                            val nextCnt = (st.studyAgainCounts[entry.id] ?: 0) + 1
                            st.copy(studyAgainCounts = st.studyAgainCounts + (entry.id to nextCnt))
                        }
                    },
                    onSaveUserContext = { wordId, ctx ->
                        updateState { st ->
                            val nextMap = if (ctx == null) st.userContexts - wordId else st.userContexts + (wordId to ctx)
                            st.copy(userContexts = nextMap)
                        }
                    },
                    onSpeakEnglish = onSpeakEnglish,
                    onComplete = {
                        statusToast = "DAY $selectedDay 플래시카드 암기 완료! 이제 확인 퀴즈로 검증하세요."
                        subScreen = WordRunSubScreen.DAY_MISSION
                    },
                    onExit = { subScreen = WordRunSubScreen.DAY_MISSION }
                )
            }

            WordRunSubScreen.PREV_REVIEW -> {
                val prevProg = state.dayProgress[selectedDay - 1]
                val pool = remember(selectedDay, allWords, prevProg) {
                    WordRun2027Engine.buildPreviousDayReviewPool(selectedDay, allWords, prevProg).map { it.id }
                }
                WordRunQuizSessionView(
                    day = selectedDay,
                    isPreviousDayReview = true,
                    initialWordIds = pool,
                    allWordsById = allWordsById,
                    allWords = allWords,
                    onWordCorrect = {},
                    onAddWrongToFsrs = { entry -> onRecordFsrsWord(WordRun2027Engine.toVocabWord(entry), RecallGrade.AGAIN, false) },
                    onComplete = {
                        updateState { st ->
                            val cur = st.dayProgress[selectedDay] ?: WordRunDayProgress(selectedDay)
                            st.copy(dayProgress = st.dayProgress + (selectedDay to cur.copy(reviewCompleted = true)))
                        }
                        statusToast = "전날 복습 10문항 완료! 오늘의 플래시카드가 열렸습니다."
                        subScreen = WordRunSubScreen.DAY_MISSION
                    },
                    onExit = { subScreen = WordRunSubScreen.DAY_MISSION }
                )
            }

            WordRunSubScreen.VERIFY_QUIZ -> {
                val pendingIds = dayWords.filterNot { it.id in dayProgress.practicedIds }
                    .ifEmpty { dayWords }
                    .map { it.id }
                WordRunQuizSessionView(
                    day = selectedDay,
                    isPreviousDayReview = false,
                    initialWordIds = pendingIds,
                    allWordsById = allWordsById,
                    allWords = allWords,
                    onWordCorrect = { id ->
                        updateState { st ->
                            val cur = st.dayProgress[selectedDay] ?: WordRunDayProgress(selectedDay)
                            st.copy(dayProgress = st.dayProgress + (selectedDay to cur.copy(practicedIds = cur.practicedIds + id)))
                        }
                    },
                    onAddWrongToFsrs = { entry -> onRecordFsrsWord(WordRun2027Engine.toVocabWord(entry), RecallGrade.AGAIN, false) },
                    onComplete = {
                        statusToast = "DAY $selectedDay 확인 퀴즈 50/50 완주! 실전 시험에 응시할 수 있습니다."
                        subScreen = WordRunSubScreen.DAY_MISSION
                    },
                    onExit = { subScreen = WordRunSubScreen.DAY_MISSION }
                )
            }

            WordRunSubScreen.TIMED_EXAM -> {
                WordRunTimedExamSessionView(
                    day = selectedDay,
                    dayWords = dayWords,
                    allWords = allWords,
                    allWordsById = allWordsById,
                    onFinishExam = { res ->
                        lastExamResult = res
                        val todayKst = WordRun2027Engine.todayKstIso()
                        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                        updateState { st ->
                            val cur = st.dayProgress[selectedDay] ?: WordRunDayProgress(selectedDay)
                            val updatedProg = cur.copy(
                                passedDateKst = if (res.passed) cur.passedDateKst.ifBlank { todayKst } else cur.passedDateKst,
                                wrongWordIds = res.wrongWordIds
                            )
                            val rec = WordRunExamRecord(
                                id = "wr_exam_${System.currentTimeMillis()}",
                                day = selectedDay,
                                score = res.score,
                                total = res.total,
                                answered = res.answered,
                                passed = res.passed,
                                elapsedSec = res.elapsedSec,
                                timestampIso = ts,
                                wrongWordIds = res.wrongWordIds
                            )
                            st.copy(
                                dayProgress = st.dayProgress + (selectedDay to updatedProg),
                                examHistory = (listOf(rec) + st.examHistory).take(40)
                            )
                        }
                        subScreen = WordRunSubScreen.EXAM_RESULT
                    },
                    onCancelExam = { subScreen = WordRunSubScreen.DAY_MISSION }
                )
            }

            WordRunSubScreen.EXAM_RESULT -> {
                val res = lastExamResult
                if (res != null) {
                    WordRunExamResultCard(
                        result = res,
                        allWordsById = allWordsById,
                        onBulkSendWrongToFsrs = {
                            res.wrongWordIds.mapNotNull { allWordsById[it] }.forEach { entry ->
                                onRecordFsrsWord(WordRun2027Engine.toVocabWord(entry), RecallGrade.AGAIN, false)
                            }
                            statusToast = "오답 ${res.wrongWordIds.size}개를 FSRS 간격 반복 복습 대기열에 등록했습니다."
                        },
                        onBackToMission = { subScreen = WordRunSubScreen.DAY_MISSION },
                        onBackToHub = { subScreen = WordRunSubScreen.HUB }
                    )
                } else {
                    subScreen = WordRunSubScreen.DAY_MISSION
                }
            }
        }
    }
}

@Composable
private fun WordRunHubView(
    state: WordRunChallengeState,
    totalWords: Int,
    onStartToday: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onToggleFreePractice: (Boolean) -> Unit
) {
    val passedCount = state.dayProgress.values.count { it.passedDateKst.isNotBlank() }
    val learnedCount = state.dayProgress.values.sumOf { it.learnedIds.size }
    val currentDay = WordRun2027Engine.resolveCurrentDay(state)

    // Hero Card
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = WordRunNavy,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wordrun_hero_card")
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "WORD RUN · 2027 CSAT · 10 DAY CHALLENGE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                color = WordRunLime
            )
            Text(
                text = "하루 50개.\n수능 단어 ${totalWords}개 완주.",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, lineHeight = 30.sp),
                color = Color.White
            )
            Text(
                text = "전날 복습 → 플래시카드·형태소 → 확인 퀴즈 → 20분 실전 시험",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFADBED6)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("전체 통과 진행률", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8C8DE))
                Text("$passedCount / 10 DAY · 누적 암기 $learnedCount/$totalWords", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), color = WordRunLime)
            }
            LinearProgressIndicator(
                progress = { (passedCount / 10f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = WordRunLime,
                trackColor = Color(0xFF34445C)
            )
            Button(
                onClick = onStartToday,
                colors = ButtonDefaults.buttonColors(containerColor = WordRunLime, contentColor = WordRunLimeInk),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("wordrun_start_today_btn")
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" DAY ${currentDay.toString().padStart(2, '0')} (${WordRun2027Engine.difficultyLabelKo(currentDay)}) 오늘 학습 시작", fontWeight = FontWeight.Black)
            }
        }
    }

    // Free Practice Mode Toggle (commercial UX flexibility)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("자유 연습 잠금 해제", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                Text(
                    text = "기본은 이전 DAY 통과 후 다음 날(KST) 해금됩니다. 켜면 모든 DAY를 자유 탐색합니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.freePracticeUnlocked,
                onCheckedChange = onToggleFreePractice,
                modifier = Modifier.testTag("wordrun_free_practice_switch")
            )
        }
    }

    // 10-Day Curriculum Roadmap
    Text("10일 커리큘럼 로드맵", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (d in 1..WordRun2027Engine.TOTAL_DAYS) {
            val prog = state.dayProgress[d] ?: WordRunDayProgress(d)
            val unlocked = WordRun2027Engine.isDayUnlocked(d, state)
            val isCurrent = d == currentDay
            val passed = prog.passedDateKst.isNotBlank()

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) StudioCobalt else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = unlocked) { onSelectDay(d) }
                    .testTag("wordrun_day_row_$d")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) StudioCobalt else StudioCobalt.copy(alpha = 0.10f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = d.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = if (isCurrent) Color.White else StudioCobalt
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${WordRun2027Engine.difficultyLabelKo(d)} 어휘 (50단어)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "${prog.learnedIds.size}/50 암기 · ${prog.practicedIds.size}/50 확인",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = when {
                            passed -> AdventureEmerald.copy(alpha = 0.16f)
                            unlocked -> StudioCobalt.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (passed) Icons.Outlined.CheckCircle else if (unlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (passed) AdventureEmerald else if (unlocked) StudioCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (passed) "통과" else if (unlocked) "학습 가능" else "잠김",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (passed) AdventureEmerald else if (unlocked) StudioCobalt else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.examHistory.isNotEmpty()) {
        Text("최근 실전 시험 기록", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
        state.examHistory.take(5).forEach { rec ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAY ${rec.day.toString().padStart(2, '0')} · ${rec.score}/${rec.total} (${rec.elapsedSec / 60}분 ${rec.elapsedSec % 60}초)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (rec.passed) "통과" else "재도전",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (rec.passed) AdventureEmerald else SpartaRed
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionStepCard(
    stepLabel: String,
    badge: String,
    badgeGood: Boolean,
    subtitle: String,
    progressText: String?,
    progressFraction: Float? = null,
    actionLabel: String?,
    actionEnabled: Boolean,
    primaryAccent: Color,
    onPrimaryColor: Color = Color.White,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stepLabel, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = (if (badgeGood) AdventureEmerald else StudioCobalt).copy(alpha = 0.14f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (badgeGood) AdventureEmerald else StudioCobalt,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (progressFraction != null) {
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = StudioCobalt,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            if (actionLabel != null) {
                Button(
                    onClick = onAction,
                    enabled = actionEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun WordRunExamResultCard(
    result: WordRunExamSessionResult,
    allWordsById: Map<Int, WordRunEntry>,
    onBulkSendWrongToFsrs: () -> Unit,
    onBackToMission: () -> Unit,
    onBackToHub: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, if (result.passed) AdventureEmerald else SpartaRed),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wordrun_exam_result_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DAY ${result.day.toString().padStart(2, '0')} 실전 시험 결과",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = (if (result.passed) AdventureEmerald else SpartaRed).copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (result.passed) "통과 (PASS)" else "재도전 필요",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (result.passed) AdventureEmerald else SpartaRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${result.score} / ${result.total} 정답 (응답 ${result.answered}문항 · 소요 ${result.elapsedSec / 60}분 ${result.elapsedSec % 60}초)",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = if (result.passed) AdventureEmerald else CoralOrange
            )

            if (result.wrongWordIds.isNotEmpty()) {
                Text(
                    text = "오답 단어 (${result.wrongWordIds.size}개)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                result.wrongWordIds.take(15).mapNotNull { allWordsById[it] }.forEach { w ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(w.word, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(w.meaningKo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = onBulkSendWrongToFsrs,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCobalt, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("wordrun_send_wrong_fsrs_btn")
                ) {
                    Text("오답 ${result.wrongWordIds.size}개 FSRS 복습함에 일괄 등록", fontWeight = FontWeight.ExtraBold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBackToMission, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("DAY 미션")
                }
                Button(
                    onClick = onBackToHub,
                    colors = ButtonDefaults.buttonColors(containerColor = WordRunLime, contentColor = WordRunLimeInk),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("10일 로드맵", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
