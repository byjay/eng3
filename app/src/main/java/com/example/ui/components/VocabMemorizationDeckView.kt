package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VocabRepository
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

/**
 * Modern bright language-learning-app Vocabulary Memorization Deck:
 * - Clean daily progress bar & streak pill
 * - Dedicated Wrong-Answer Review toggle + Grade deck selector (전체 · 고1 · 고2 · 고3)
 * - Spacious elevated white Flashcard with instant audio, YouTube context,
 *   경선식 해마 그림 연상 & 시원스쿨 3단 블록 결합 (VisualMnemonicAndChunkCard)
 * - Crisp full-width "다시 복습" / "암기 완료" mastery actions + inline Wrong Answer Notebook
 */
@Composable
fun VocabMemorizationDeckView(
    selectedGrade: Int,
    masteredWordsCount: Int,
    streakCount: Int,
    todayWordsCompleted: Int,
    wrongWords: List<VocabWord>,
    harvestedApiCount: Int,
    onSelectGrade: (Int) -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onRecordWordResult: (wordId: String, known: Boolean) -> Unit,
    onOpenWeeklyReport: () -> Unit,
    onOpenYouTubeWord: (String) -> Unit,
    onStartSpeakingDrillForWord: (VocabWord) -> Unit,
    modifier: Modifier = Modifier
) {
    var isWrongReviewMode by remember { mutableStateOf(false) }
    var reviewAllClearNotice by remember { mutableStateOf(false) }

    if (isWrongReviewMode && wrongWords.isEmpty()) {
        isWrongReviewMode = false
        reviewAllClearNotice = true
    }

    val normalDeck = remember(selectedGrade) {
        VocabRepository.wordsForGrade(selectedGrade).ifEmpty { VocabRepository.allWords }
    }
    val activeDeck = if (isWrongReviewMode && wrongWords.isNotEmpty()) wrongWords else normalDeck
    var cardIndex by remember(isWrongReviewMode, selectedGrade) { mutableIntStateOf(0) }
    var isMeaningRevealed by remember(isWrongReviewMode, selectedGrade, cardIndex) { mutableStateOf(false) }

    val safeIndex = cardIndex % activeDeck.size
    val currentWord = activeDeck[safeIndex]
    val primaryExampleEn = currentWord.examples.firstOrNull()?.english ?: currentWord.exampleEn
    val primaryExampleKo = currentWord.examples.firstOrNull()?.korean ?: currentWord.exampleKo
    val dailyGoalFraction = (todayWordsCompleted.coerceAtMost(10) / 10f).coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clean Compact Daily Learning Goal Strip (Like Speak / Malhaeboca)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vocab_daily_goal_strip")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = StudioCoralSoft
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalFireDepartment,
                                    contentDescription = "연속 학습",
                                    tint = StudioCoral,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "연속 ${streakCount}회",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = StudioCoral
                                )
                            }
                        }
                        Text(
                            text = "오늘 암기 ${todayWordsCompleted}/10",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isWrongReviewMode) StudioCoral else StudioCoralSoft,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    if (wrongWords.isNotEmpty()) {
                                        reviewAllClearNotice = false
                                        isWrongReviewMode = !isWrongReviewMode
                                    }
                                }
                                .testTag("vocab_toggle_wrong_review")
                        ) {
                            Text(
                                text = if (isWrongReviewMode) "오답 집중 중 (${wrongWords.size})" else "오답 복습 (${wrongWords.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isWrongReviewMode) Color.White else StudioCoral,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = StudioCobaltSoft,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenWeeklyReport)
                                .testTag("vocab_open_weekly_report")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.BarChart,
                                    contentDescription = "주간 리포트",
                                    tint = StudioCobalt,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { dailyGoalFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = StudioCobalt,
                    trackColor = StudioCobaltSoft,
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (todayWordsCompleted >= 10) "오늘 10단어 목표 달성!" else "목표까지 ${(10 - todayWordsCompleted).coerceAtLeast(0)}단어 남았어요",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "누적 마스터 ${masteredWordsCount}단어",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StudioCobalt
                    )
                }
            }
        }

        // 2. Clean Single-Row Grade Deck Selector (전체 · 고1 · 고2 · 고3)
        if (!isWrongReviewMode) {
            val gradeSpecs = remember(harvestedApiCount) {
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
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                gradeSpecs.forEach { (gradeVal, label, accent) ->
                    val selected = selectedGrade == gradeVal
                    val count = remember(gradeVal, harvestedApiCount) {
                        VocabRepository.wordsForGrade(gradeVal).size +
                            if (gradeVal == 0) harvestedApiCount else 0
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) accent else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) accent else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectGrade(gradeVal) }
                            .testTag("vocab_memo_grade_$gradeVal")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${count}개",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (selected) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant
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
                border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.35f)),
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

        // 3. HERO FLASHCARD (Bright, Clean, High-Contrast Learning App Card)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isWrongReviewMode) StudioCoral.copy(alpha = 0.45f) else StudioCobalt.copy(alpha = 0.22f)
            ),
            shadowElevation = 5.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vocab_hero_flashcard")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card Top Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
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

                    Text(
                        text = "${safeIndex + 1} / ${activeDeck.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Word Centerpiece + Circular Audio & YouTube Triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = RoseCrimsonSoft,
                            border = BorderStroke(1.dp, RoseCrimson.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onOpenYouTubeWord(currentWord.word) }
                                .testTag("vocab_card_youtube_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayCircle,
                                    contentDescription = "유튜브 실전 발음",
                                    tint = RoseCrimson,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = StudioCobalt,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onSpeakEnglish(currentWord.word) }
                                .testTag("vocab_card_audio_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                    contentDescription = "단어 발음 듣기",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Primary Example Sentence Preview Box (Tap to listen!)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSpeakEnglish(primaryExampleEn) }
                        .testTag("vocab_card_example_box")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = primaryExampleEn,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AnimatedVisibility(visible = isMeaningRevealed) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                                Text(
                                    text = currentWord.meaningKo,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = StudioCobalt
                                )
                                Text(
                                    text = primaryExampleKo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Crisp Tap-to-Reveal Meaning Toggle Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isMeaningRevealed) StudioCobaltSoft else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.2.dp,
                        if (isMeaningRevealed) StudioCobalt else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isMeaningRevealed = !isMeaningRevealed }
                        .testTag("vocab_card_reveal_toggle")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMeaningRevealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "뜻 보기 토글",
                            tint = StudioCobalt,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = if (isMeaningRevealed) "뜻 · 예문 해석 숨기기" else "터치해서 뜻 · 해석 확인하기",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = StudioCobalt
                        )
                    }
                }

                // Visual Memory Engine: 경선식 해마 그림 연상 | 시원스쿨 3단 블록 결합
                VisualMnemonicAndChunkCard(
                    word = currentWord,
                    onSpeakChunk = onSpeakEnglish,
                    onStartChunkSpeakingDrill = { onStartSpeakingDrillForWord(currentWord) }
                )

                // Clean Spacious Bottom 2-Button Mastery Bar (Zero text clipping!)
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
                        onClick = {
                            onRecordWordResult(currentWord.id, false)
                            cardIndex += 1
                            isMeaningRevealed = false
                        }
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
                        onClick = {
                            onRecordWordResult(currentWord.id, true)
                            cardIndex += 1
                            isMeaningRevealed = false
                        }
                    )
                }
            }
        }

        // 4. Clean Compact Wrong-Answer Notebook Drawer right inside Vocab Memorization Tab
        if (wrongWords.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.28f)),
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
