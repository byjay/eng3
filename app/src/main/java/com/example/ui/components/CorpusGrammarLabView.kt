package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExamCorpusSnapshot
import com.example.engine.RecallGrade
import com.example.model.ExamCorpusWordEntry
import com.example.model.GrammarCategory
import com.example.model.GrammarFocusSentence
import com.example.model.VocabWord
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold
import kotlin.math.roundToInt

/**
 * 6-Category KICE Core English Grammar Focus Lab with Bidirectional Word 10-Year Stats Popover.
 *
 * - Displays evaluation share % per category (관계사 36.7%, 준동사 30.0%, 병렬 10.0%, etc.)
 * - Interactive 2-choice grammar drill with explanation & trap tip
 * - Tapping any linked/sentence word opens a 10-year quantitative stats popover with 1-tap
 *   addition into the FSRS review deck.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorpusGrammarLabView(
    snapshot: ExamCorpusSnapshot,
    selectedCategory: GrammarCategory?,
    onSelectCategory: (GrammarCategory?) -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onAddWordToFsrsDeck: (VocabWord) -> Unit,
    modifier: Modifier = Modifier
) {
    val grammarAnswers = remember { mutableStateMapOf<String, String>() }
    var popoverEntry by remember { mutableStateOf<ExamCorpusWordEntry?>(null) }

    val filteredGrammar = remember(snapshot.grammarSentences, selectedCategory) {
        if (selectedCategory == null) snapshot.grammarSentences
        else snapshot.grammarSentences.filter { it.category == selectedCategory }
    }

    if (popoverEntry != null) {
        WordStatsPopoverDialog(
            entry = popoverEntry!!,
            onSpeakEnglish = onSpeakEnglish,
            onAddToFsrsDeck = {
                onAddWordToFsrsDeck(popoverEntry!!.sourceWord)
                popoverEntry = null
            },
            onDismiss = { popoverEntry = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("corpus_grammar_lab_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 6-Category Filter Chips with KICE evaluation weight percentages
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                GrammarCategoryChip(
                    label = "전체 6대 영역 (${snapshot.grammarSentences.size})",
                    selected = selectedCategory == null,
                    accent = StudioIris,
                    onClick = { onSelectCategory(null) }
                )
            }
            items(GrammarCategory.entries) { cat ->
                val count = snapshot.grammarSentences.count { it.category == cat }
                GrammarCategoryChip(
                    label = "${cat.shortLabelKo} ${cat.kiceSharePercent}% ($count)",
                    selected = selectedCategory == cat,
                    accent = StudioCobalt,
                    onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) }
                )
            }
        }

        selectedCategory?.let { cat ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioIris.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, StudioIris.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${cat.labelKo} · 평가원 어법 비중 ${cat.kiceSharePercent}% (${cat.udRuleCode})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = StudioIris
                    )
                    Text(
                        text = cat.focusSummaryKo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        filteredGrammar.forEach { sentence ->
            GrammarDrillCardWithWordPopover(
                sentence = sentence,
                snapshot = snapshot,
                chosenOption = grammarAnswers[sentence.id],
                onChooseOption = { grammarAnswers[sentence.id] = it },
                onSpeakEnglish = onSpeakEnglish,
                onTapWordEntry = { popoverEntry = it }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrammarDrillCardWithWordPopover(
    sentence: GrammarFocusSentence,
    snapshot: ExamCorpusSnapshot,
    chosenOption: String?,
    onChooseOption: (String) -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onTapWordEntry: (ExamCorpusWordEntry) -> Unit
) {
    val options = remember(sentence.id) {
        if (sentence.id.hashCode() % 2 == 0) {
            listOf(sentence.targetSegment, sentence.distractorSegment)
        } else {
            listOf(sentence.distractorSegment, sentence.targetSegment)
        }
    }
    val isAnswered = chosenOption != null
    val isCorrect = chosenOption == sentence.targetSegment

    // Extract tappable known corpus words inside the grammar sentence + explicit linkedLemmas
    val tappableEntries = remember(sentence.id, snapshot) {
        val fromLinked = sentence.linkedLemmas.mapNotNull { snapshot.searchIndex.findByTokenOrLemma(it) }
        val fromTokens = sentence.completeSentenceEn
            .split(Regex("[^A-Za-z]+"))
            .filter { it.length >= 4 }
            .mapNotNull { snapshot.searchIndex.findByTokenOrLemma(it) }
        (fromLinked + fromTokens).distinctBy { it.wordId }.take(6)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.2.dp,
            when {
                !isAnswered -> MaterialTheme.colorScheme.outline
                isCorrect -> AdventureEmerald.copy(alpha = 0.6f)
                else -> SpartaRed.copy(alpha = 0.6f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grammar_drill_card_${sentence.id}")
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(shape = RoundedCornerShape(999.dp), color = StudioIris.copy(alpha = 0.14f)) {
                        Text(
                            text = "${sentence.category.shortLabelKo} ${sentence.category.kiceSharePercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = StudioIris,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = sentence.titleKo,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = { onSpeakEnglish(sentence.completeSentenceEn) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = "문법 예문 듣기",
                        tint = StudioCobalt,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioCobalt.copy(alpha = 0.08f)
            ) {
                Text(
                    text = "공식: ${sentence.patternFormula}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioCobalt,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }

            Text(
                text = if (isAnswered) sentence.completeSentenceEn else sentence.sentenceWithBlankEn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = sentence.sentenceKo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 2-choice grammar button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { opt ->
                    val chosen = chosenOption == opt
                    val rightOption = opt == sentence.targetSegment
                    val bg = when {
                        !isAnswered -> StudioCobalt.copy(alpha = 0.09f)
                        rightOption -> AdventureEmerald.copy(alpha = 0.18f)
                        chosen -> SpartaRed.copy(alpha = 0.16f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                    val border = when {
                        !isAnswered -> StudioCobalt.copy(alpha = 0.35f)
                        rightOption -> AdventureEmerald
                        chosen -> SpartaRed
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bg,
                        border = BorderStroke(1.2.dp, border),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onChooseOption(opt) }
                            .testTag("grammar_opt_${sentence.id}_$opt")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (isAnswered) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (isCorrect) AdventureEmerald else CoralOrange).copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, (if (isCorrect) AdventureEmerald else CoralOrange).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Outlined.CheckCircle else Icons.Outlined.Close,
                                contentDescription = null,
                                tint = if (isCorrect) AdventureEmerald else SpartaRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isCorrect) "정답입니다!" else "정답: ${sentence.targetSegment}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isCorrect) AdventureEmerald else SpartaRed
                            )
                        }
                        Text(
                            text = sentence.explanationKo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (sentence.trapTipKo.isNotBlank()) {
                            Text(
                                text = "함정 포인트: ${sentence.trapTipKo}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = RoyalPurple
                            )
                        }
                    }
                }
            }

            // Bidirectional word popover chips
            if (tappableEntries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "문장 핵심 어휘 탭 → 10개년 통계 팝오버 · FSRS 덱 추가",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tappableEntries.forEach { wordEntry ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SkyQuestBlue.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, SkyQuestBlue.copy(alpha = 0.38f)),
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onTapWordEntry(wordEntry) }
                                    .testTag("grammar_word_popover_${wordEntry.lemma}")
                            ) {
                                Text(
                                    text = "${wordEntry.lemma} · ${wordEntry.analytics.difficultyTier.shortBadgeKo}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = SkyQuestBlue,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordStatsPopoverDialog(
    entry: ExamCorpusWordEntry,
    onSpeakEnglish: (String) -> Unit,
    onAddToFsrsDeck: () -> Unit,
    onDismiss: () -> Unit
) {
    val a = entry.analytics
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.lemma,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "${entry.ipa} · ${entry.pos} · ${a.difficultyTier.labelKo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onSpeakEnglish(entry.lemma) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = "단어 발음 듣기",
                        tint = StudioCobalt
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = entry.meaningKo,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PopoverStatChip("총 ${a.totalFrequency}회", StudioCobalt)
                    PopoverStatChip("수능 ${a.csatFrequency}회", SpartaRed)
                    PopoverStatChip("균등도 ${"%.2f".format(a.griesDispersion.uniformity)}", AdventureEmerald)
                    PopoverStatChip("주기 ${"%.1f".format(a.avgRecurrenceGapYears)}년", SkyQuestBlue)
                    PopoverStatChip("듣기 ${(a.listeningRatio * 100).roundToInt()}%", RoyalPurple)
                    PopoverStatChip(a.curriculumGroup.labelKo, TrophyGold)
                }
                if (entry.surfaceVariants.isNotEmpty()) {
                    Text(
                        text = "기출 변화형: ${entry.surfaceVariants.joinToString(", ") { "${it.form}(${it.count})" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddToFsrsDeck,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCobalt),
                modifier = Modifier.testTag("popover_add_fsrs_deck")
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddTask,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(" FSRS 복습 덱 추가", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

@Composable
private fun PopoverStatChip(label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun GrammarCategoryChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accent else accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (selected) accent else accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = if (selected) Color.White else accent,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
        )
    }
}
