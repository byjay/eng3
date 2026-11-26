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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExamCorpusRepository
import com.example.model.ExamCorpusWordEntry
import com.example.model.ExamOccurrenceRecord
import com.example.model.ExamSection
import com.example.model.ExamType
import com.example.model.GrammarCategory
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold

/**
 * Lazy-loaded Historical Example Expand Drawer (15-item pagination via SQLite/FTS5 store).
 *
 * Features:
 * - Multi-filter bar: 전체 · 수능만 · 평가원 모평 · 학평 · 독해 · 듣기 · 문법 핵심만 · 변화형별
 * - Span-highlighted surface form display & underlined exam marker
 * - Cloze (문맥 빈칸) learning mode toggle with per-sentence tap-to-reveal answer
 * - Bidirectional jump from any grammar badge directly to GrammarLabView
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorpusExampleExpandDrawer(
    entry: ExamCorpusWordEntry,
    provenanceNoticeKo: String,
    onSpeakEnglish: (String) -> Unit,
    onJumpToGrammarCategory: (GrammarCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var examFilter by remember(entry.wordId) { mutableStateOf<ExamType?>(null) }
    var sectionFilter by remember(entry.wordId) { mutableStateOf<ExamSection?>(null) }
    var surfaceFilter by remember(entry.wordId) { mutableStateOf<String?>(null) }
    var onlyGrammarCore by remember(entry.wordId) { mutableStateOf(false) }
    var clozeMode by remember(entry.wordId) { mutableStateOf(false) }
    var loadedLimit by remember(entry.wordId, examFilter, sectionFilter, surfaceFilter, onlyGrammarCore) {
        mutableIntStateOf(15)
    }
    val revealedClozeIds = remember(entry.wordId, clozeMode) { mutableStateMapOf<String, Boolean>() }

    val pagedResult = remember(
        context, entry.wordId, examFilter, sectionFilter, surfaceFilter, onlyGrammarCore, loadedLimit
    ) {
        ExamCorpusRepository.queryOccurrencesPaged(
            context = context,
            entry = entry,
            examTypeFilter = examFilter,
            sectionFilter = sectionFilter,
            surfaceFormFilter = surfaceFilter,
            onlyGrammarCore = onlyGrammarCore,
            limit = loadedLimit,
            offset = 0
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("corpus_example_expand_drawer_${entry.wordId}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Provenance + Cloze toggle header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pagedResult.sourceLabelKo} · 조건 일치 ${pagedResult.totalCount}건",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (clozeMode) StudioIris else StudioIris.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, StudioIris.copy(alpha = 0.5f)),
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { clozeMode = !clozeMode }
                    .testTag("toggle_cloze_mode_${entry.wordId}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (clozeMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "빈칸 모드 전환",
                        tint = if (clozeMode) Color.White else StudioIris,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (clozeMode) "빈칸 모드 ON" else "빈칸(Cloze)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (clozeMode) Color.White else StudioIris
                    )
                }
            }
        }

        // Multi-filter bar: ExamType + Section + Grammar Core
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                DrawerFilterChip(
                    label = "전체",
                    selected = examFilter == null && sectionFilter == null && !onlyGrammarCore && surfaceFilter == null,
                    accent = StudioCobalt,
                    onClick = {
                        examFilter = null
                        sectionFilter = null
                        surfaceFilter = null
                        onlyGrammarCore = false
                    }
                )
            }
            item {
                DrawerFilterChip(
                    label = "수능만",
                    selected = examFilter == ExamType.CSAT,
                    accent = SpartaRed,
                    onClick = { examFilter = if (examFilter == ExamType.CSAT) null else ExamType.CSAT }
                )
            }
            item {
                DrawerFilterChip(
                    label = "평가원 모평",
                    selected = examFilter == ExamType.KICE_MOCK,
                    accent = RoyalPurple,
                    onClick = { examFilter = if (examFilter == ExamType.KICE_MOCK) null else ExamType.KICE_MOCK }
                )
            }
            item {
                DrawerFilterChip(
                    label = "학평(고1~3)",
                    selected = examFilter == ExamType.MOCK_G3 || examFilter == ExamType.MOCK_G2 || examFilter == ExamType.MOCK_G1,
                    accent = SkyQuestBlue,
                    onClick = { examFilter = if (examFilter == ExamType.MOCK_G3) null else ExamType.MOCK_G3 }
                )
            }
            item {
                DrawerFilterChip(
                    label = "독해",
                    selected = sectionFilter == ExamSection.READING,
                    accent = StudioCobalt,
                    onClick = { sectionFilter = if (sectionFilter == ExamSection.READING) null else ExamSection.READING }
                )
            }
            item {
                DrawerFilterChip(
                    label = "듣기",
                    selected = sectionFilter == ExamSection.LISTENING,
                    accent = AdventureEmerald,
                    onClick = { sectionFilter = if (sectionFilter == ExamSection.LISTENING) null else ExamSection.LISTENING }
                )
            }
            item {
                DrawerFilterChip(
                    label = "문법 핵심만",
                    selected = onlyGrammarCore,
                    accent = TrophyGold,
                    onClick = { onlyGrammarCore = !onlyGrammarCore }
                )
            }
        }

        // Surface variant chips for quick inflection-level filtering
        if (entry.surfaceVariants.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entry.surfaceVariants.take(5).forEach { variant ->
                    val selected = surfaceFilter.equals(variant.form, ignoreCase = true)
                    DrawerFilterChip(
                        label = "${variant.form} (${variant.count})",
                        selected = selected,
                        accent = StudioIris,
                        onClick = {
                            surfaceFilter = if (selected) null else variant.form
                        }
                    )
                }
            }
        }

        if (pagedResult.items.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "선택한 필터 조건에 해당하는 용례가 없습니다. 필터를 '전체'로 초기화해 보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            pagedResult.items.forEach { occ ->
                val isClozeRevealed = revealedClozeIds[occ.id] == true
                ExpandedOccurrenceSentenceCard(
                    occurrence = occ,
                    clozeMode = clozeMode,
                    isClozeRevealed = isClozeRevealed,
                    onToggleClozeReveal = { revealedClozeIds[occ.id] = !isClozeRevealed },
                    onSpeakEnglish = onSpeakEnglish,
                    onJumpToGrammarCategory = onJumpToGrammarCategory
                )
            }
        }

        if (pagedResult.hasMore) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StudioCobalt.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { loadedLimit += 15 }
                    .testTag("load_more_occurrences_${entry.wordId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "기출 용례 15건 더 보기 (${pagedResult.items.size}/${pagedResult.totalCount})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = StudioCobalt
                    )
                }
            }
        }

        Text(
            text = provenanceNoticeKo,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandedOccurrenceSentenceCard(
    occurrence: ExamOccurrenceRecord,
    clozeMode: Boolean,
    isClozeRevealed: Boolean,
    onToggleClozeReveal: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onJumpToGrammarCategory: (GrammarCategory) -> Unit
) {
    val examAccent = when (occurrence.examType) {
        ExamType.CSAT -> SpartaRed
        ExamType.KICE_MOCK -> RoyalPurple
        ExamType.MOCK_G3 -> StudioCobalt
        ExamType.MOCK_G2 -> SkyQuestBlue
        ExamType.MOCK_G1 -> AdventureEmerald
    }
    val showMasked = clozeMode && !isClozeRevealed

    val formattedSentence = remember(
        occurrence.sentenceEn,
        occurrence.surfaceForm,
        occurrence.charSpanStart,
        occurrence.charSpanEnd,
        showMasked,
        isClozeRevealed,
        occurrence.isUnderlined
    ) {
        val text = occurrence.sentenceEn
        val startIdx = if (occurrence.charSpanStart in text.indices && occurrence.charSpanEnd > occurrence.charSpanStart) {
            occurrence.charSpanStart
        } else {
            text.indexOf(occurrence.surfaceForm, ignoreCase = true)
        }
        val endIdx = if (occurrence.charSpanStart in text.indices && occurrence.charSpanEnd <= text.length && occurrence.charSpanEnd > occurrence.charSpanStart) {
            occurrence.charSpanEnd
        } else if (startIdx >= 0) {
            (startIdx + occurrence.surfaceForm.length).coerceAtMost(text.length)
        } else {
            -1
        }

        buildAnnotatedString {
            if (startIdx >= 0 && endIdx > startIdx) {
                append(text.substring(0, startIdx))
                if (showMasked) {
                    withStyle(
                        SpanStyle(
                            color = StudioIris,
                            fontWeight = FontWeight.ExtraBold,
                            background = StudioIris.copy(alpha = 0.14f)
                        )
                    ) {
                        append(" [ _________ ] ")
                    }
                } else {
                    withStyle(
                        SpanStyle(
                            color = if (isClozeRevealed) AdventureEmerald else StudioCobalt,
                            fontWeight = FontWeight.ExtraBold,
                            background = (if (isClozeRevealed) AdventureEmerald else StudioCobalt).copy(alpha = 0.13f),
                            textDecoration = if (occurrence.isUnderlined) TextDecoration.Underline else TextDecoration.None
                        )
                    ) {
                        append(text.substring(startIdx, endIdx))
                    }
                }
                append(text.substring(endIdx))
            } else {
                append(text)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (showMasked) StudioIris.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (clozeMode) Modifier.clickable { onToggleClozeReveal() } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(shape = RoundedCornerShape(999.dp), color = examAccent.copy(alpha = 0.14f)) {
                        Text(
                            text = "${occurrence.year} ${occurrence.month}월 ${occurrence.examType.labelKo}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = examAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = (if (occurrence.section == ExamSection.LISTENING) AdventureEmerald else SkyQuestBlue).copy(alpha = 0.13f)
                    ) {
                        Text(
                            text = "${occurrence.section.labelKo} ${occurrence.itemNumber}번 · ${occurrence.itemTypeKo}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (occurrence.section == ExamSection.LISTENING) AdventureEmerald else SkyQuestBlue,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { onSpeakEnglish(occurrence.sentenceEn) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = "문장 듣기",
                        tint = StudioCobalt,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = formattedSentence,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (occurrence.sentenceKo.isNotBlank()) {
                Text(
                    text = occurrence.sentenceKo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (clozeMode) {
                Text(
                    text = if (isClozeRevealed) {
                        "정답 어형: ${occurrence.surfaceForm} (탭하여 다시 가리기)"
                    } else {
                        "빈칸을 채울 어형을 떠올린 뒤 카드를 탭해 정답을 확인하세요."
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isClozeRevealed) AdventureEmerald else StudioIris
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (occurrence.isUnderlined) {
                    Surface(shape = RoundedCornerShape(999.dp), color = CoralOrange.copy(alpha = 0.14f)) {
                        Text(
                            text = "밑줄 어형 출제",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = CoralOrange,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                occurrence.grammarCategories.forEach { cat ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = StudioIris.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, StudioIris.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onJumpToGrammarCategory(cat) }
                            .testTag("jump_grammar_${cat.code}")
                    ) {
                        Text(
                            text = "${cat.shortLabelKo} 훈련 →",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = StudioIris,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accent else accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (selected) accent else accent.copy(alpha = 0.32f)),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = if (selected) Color.White else accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
