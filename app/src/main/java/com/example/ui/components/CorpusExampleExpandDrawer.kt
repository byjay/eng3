package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExamCorpusRepository
import com.example.engine.ExamCorpusAnalyticsEngine
import com.example.engine.GrammarSpanEngine
import com.example.model.ExamCorpusWordEntry
import com.example.model.ExamOccurrenceRecord
import com.example.model.ExamSection
import com.example.model.ExamType
import com.example.model.GrammarCategory
import com.example.model.GrammarSpanRole
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_SIZE = 15

private fun grammarRoleColor(role: GrammarSpanRole): Color = when (role) {
    GrammarSpanRole.SUBJECT_CHUNK -> StudioCobalt
    GrammarSpanRole.MAIN_VERB -> AdventureEmerald
    GrammarSpanRole.RELATIVE_CLAUSE -> RoyalPurple
    GrammarSpanRole.PARTICIPLE_PHRASE -> CoralOrange
    GrammarSpanRole.PARALLEL_UNIT -> SkyQuestBlue
    GrammarSpanRole.PREPOSITIONAL_MODIFIER -> SpartaRed
}

/**
 * Production-grade Lazy-Loaded Historical Example Expand Drawer (15 items per SQLite page).
 *
 * Features:
 * - Non-blocking async pagination (`LIMIT 15 OFFSET ?`) on Dispatchers.IO with race-safe sequence IDs
 * - Multi-filter bar: 전체 · 수능만 · 평가원 모평 · 학평 · 독해 · 듣기 · 문법 핵심만 · 변화형별
 * - Cloze (빈칸 채우기) mode toggle with per-sentence tap-to-reveal
 * - `grammar_spans` role-colored underline + soft tint background + interactive 6-role legend toggle
 * - Atomic interval partitioning ensuring zero crashes or split blanks when Cloze and grammar_spans overlap
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
    val scope = rememberCoroutineScope()

    var examFilter by remember(entry.wordId) { mutableStateOf<ExamType?>(null) }
    var sectionFilter by remember(entry.wordId) { mutableStateOf<ExamSection?>(null) }
    var surfaceFilter by remember(entry.wordId) { mutableStateOf<String?>(null) }
    var onlyGrammarCore by remember(entry.wordId) { mutableStateOf(false) }

    var clozeMode by remember(entry.wordId) { mutableStateOf(false) }
    var showGrammarSpans by remember(entry.wordId) { mutableStateOf(true) }
    var activeRoles by remember(entry.wordId) { mutableStateOf(GrammarSpanRole.entries.toSet()) }
    val revealedClozeIds = remember(entry.wordId, clozeMode) { mutableStateMapOf<String, Boolean>() }

    val loadedItems = remember(entry.wordId) { mutableStateListOf<ExamOccurrenceRecord>() }
    var totalCount by remember(entry.wordId) { mutableIntStateOf(0) }
    var hasMore by remember(entry.wordId) { mutableStateOf(false) }
    var sourceLabelKo by remember(entry.wordId) { mutableStateOf("SQLite 로컬 코퍼스") }
    var isLoadingInitial by remember(entry.wordId) { mutableStateOf(true) }
    var isLoadingMore by remember(entry.wordId) { mutableStateOf(false) }
    var requestSeq by remember(entry.wordId) { mutableIntStateOf(0) }

    // Fetch first 15 items asynchronously whenever word or filter changes
    LaunchedEffect(entry.wordId, examFilter, sectionFilter, surfaceFilter, onlyGrammarCore) {
        val currentSeq = ++requestSeq
        isLoadingInitial = true
        isLoadingMore = false
        val page = withContext(Dispatchers.IO) {
            ExamCorpusRepository.queryOccurrencesPaged(
                context = context,
                entry = entry,
                examTypeFilter = examFilter,
                sectionFilter = sectionFilter,
                surfaceFormFilter = surfaceFilter,
                onlyGrammarCore = onlyGrammarCore,
                limit = PAGE_SIZE,
                offset = 0
            )
        }
        if (currentSeq == requestSeq) {
            loadedItems.clear()
            loadedItems.addAll(page.items)
            totalCount = page.totalCount
            hasMore = page.hasMore
            sourceLabelKo = page.sourceLabelKo
            isLoadingInitial = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("corpus_example_expand_drawer_${entry.wordId}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header: provenance count + dual learning mode toggles (Cloze & grammar_spans)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "$sourceLabelKo · 조건 일치 ${totalCount}건 (15건 단위 로딩)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (clozeMode) StudioIris else StudioIris.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, StudioIris.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            clozeMode = !clozeMode
                            revealedClozeIds.clear()
                        }
                        .testTag("toggle_cloze_mode_${entry.wordId}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (clozeMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "빈칸 모드 전환",
                            tint = if (clozeMode) Color.White else StudioIris,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (clozeMode) " 빈칸(Cloze) ON" else " 빈칸(Cloze)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (clozeMode) Color.White else StudioIris
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (showGrammarSpans) AdventureEmerald else AdventureEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AdventureEmerald.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { showGrammarSpans = !showGrammarSpans }
                        .testTag("toggle_grammar_spans_${entry.wordId}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "구문 하이라이트 전환",
                            tint = if (showGrammarSpans) Color.White else AdventureEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (showGrammarSpans) " 구문 분석 (${activeRoles.size}/6)" else " 구문 분석 OFF",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (showGrammarSpans) Color.White else AdventureEmerald
                        )
                    }
                }
            }
        }

        // Interactive 6-Role Grammar Legend Filter Chips
        AnimatedVisibility(
            visible = showGrammarSpans,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.testTag("grammar_span_legend_${entry.wordId}")
            ) {
                GrammarSpanRole.entries.forEach { role ->
                    val accent = grammarRoleColor(role)
                    val selected = role in activeRoles
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (selected) 1.4.dp else 1.dp,
                            color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                activeRoles = if (selected) activeRoles - role else activeRoles + role
                            }
                            .testTag("legend_role_${role.code}")
                    ) {
                        Text(
                            text = role.shortLabelKo,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                textDecoration = if (selected) TextDecoration.Underline else TextDecoration.None
                            ),
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
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

        // Loading / Empty / Loaded list
        when {
            isLoadingInitial -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = StudioCobalt
                    )
                    Text(
                        text = "  SQLite에서 예문 15건을 불러오는 중...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            loadedItems.isEmpty() -> {
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
            }
            else -> {
                loadedItems.forEach { occ ->
                    val isClozeRevealed = revealedClozeIds[occ.id] == true
                    ExpandedOccurrenceSentenceCard(
                        occurrence = occ,
                        clozeMode = clozeMode,
                        isClozeRevealed = isClozeRevealed,
                        showGrammarSpans = showGrammarSpans,
                        activeRoles = activeRoles,
                        onToggleClozeReveal = { revealedClozeIds[occ.id] = !isClozeRevealed },
                        onSpeakEnglish = onSpeakEnglish,
                        onJumpToGrammarCategory = onJumpToGrammarCategory
                    )
                }
            }
        }

        // Incremental 15-Item Lazy Load Footer
        if (hasMore) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StudioCobalt.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoadingMore) {
                        val currentSeq = requestSeq
                        val nextOffset = loadedItems.size
                        isLoadingMore = true
                        scope.launch {
                            val nextPage = withContext(Dispatchers.IO) {
                                ExamCorpusRepository.queryOccurrencesPaged(
                                    context = context,
                                    entry = entry,
                                    examTypeFilter = examFilter,
                                    sectionFilter = sectionFilter,
                                    surfaceFormFilter = surfaceFilter,
                                    onlyGrammarCore = onlyGrammarCore,
                                    limit = PAGE_SIZE,
                                    offset = nextOffset
                                )
                            }
                            if (currentSeq == requestSeq) {
                                val existingIds = loadedItems.map { it.id }.toHashSet()
                                loadedItems.addAll(nextPage.items.filterNot { it.id in existingIds })
                                totalCount = nextPage.totalCount
                                hasMore = nextPage.hasMore
                                isLoadingMore = false
                            }
                        }
                    }
                    .testTag("load_more_occurrences_${entry.wordId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = StudioCobalt
                        )
                        Text(
                            text = "  추가 예문 불러오는 중...",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = StudioCobalt
                        )
                    } else {
                        Text(
                            text = "기출 용례 15건 더 보기 (${loadedItems.size}/$totalCount)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = StudioCobalt
                        )
                    }
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
    showGrammarSpans: Boolean,
    activeRoles: Set<GrammarSpanRole>,
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

    val effectiveSpans = remember(occurrence.sentenceEn, occurrence.grammarSpans) {
        GrammarSpanEngine.resolveEffectiveSpans(occurrence.sentenceEn, occurrence.grammarSpans)
    }

    val formattedSentence = remember(
        occurrence.sentenceEn,
        occurrence.surfaceForm,
        occurrence.charSpanStart,
        occurrence.charSpanEnd,
        effectiveSpans,
        activeRoles,
        showGrammarSpans,
        clozeMode,
        isClozeRevealed,
        occurrence.isUnderlined
    ) {
        val text = occurrence.sentenceEn
        val targetSpan = if (
            occurrence.charSpanStart in text.indices &&
            occurrence.charSpanEnd in (occurrence.charSpanStart + 1)..text.length
        ) {
            occurrence.charSpanStart to occurrence.charSpanEnd
        } else {
            ExamCorpusAnalyticsEngine.resolveCharSpan(text, occurrence.surfaceForm)
        }

        val segments = GrammarSpanEngine.partitionSentenceSegments(
            sentence = text,
            grammarSpans = effectiveSpans,
            activeRoles = activeRoles,
            targetSpan = targetSpan,
            showGrammarSpans = showGrammarSpans,
            clozeEnabled = clozeMode,
            clozeRevealed = isClozeRevealed
        )

        buildAnnotatedString {
            segments.forEach { seg ->
                val roleAccent = seg.primaryGrammarRole?.let { grammarRoleColor(it) }
                when {
                    seg.isClozeMasked -> {
                        withStyle(
                            SpanStyle(
                                color = StudioIris,
                                fontWeight = FontWeight.ExtraBold,
                                background = StudioIris.copy(alpha = 0.16f),
                                textDecoration = if (roleAccent != null) TextDecoration.Underline else TextDecoration.None
                            )
                        ) {
                            append(" [ ______ ] ")
                        }
                    }
                    seg.isClozeRevealed -> {
                        withStyle(
                            SpanStyle(
                                color = AdventureEmerald,
                                fontWeight = FontWeight.ExtraBold,
                                background = AdventureEmerald.copy(alpha = 0.18f),
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(seg.text)
                        }
                    }
                    seg.isTargetToken -> {
                        withStyle(
                            SpanStyle(
                                color = StudioCobalt,
                                fontWeight = FontWeight.ExtraBold,
                                background = (roleAccent ?: StudioCobalt).copy(alpha = 0.18f),
                                textDecoration = if (occurrence.isUnderlined || roleAccent != null) {
                                    TextDecoration.Underline
                                } else {
                                    TextDecoration.None
                                }
                            )
                        ) {
                            append(seg.text)
                        }
                    }
                    roleAccent != null -> {
                        withStyle(
                            SpanStyle(
                                color = roleAccent,
                                fontWeight = FontWeight.SemiBold,
                                background = roleAccent.copy(alpha = 0.10f),
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(seg.text)
                        }
                    }
                    else -> append(seg.text)
                }
            }
        }
    }

    val detectedRolesInSentence = remember(effectiveSpans, activeRoles) {
        effectiveSpans.map { it.role }.filter { it in activeRoles }.distinct()
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (showMasked) StudioIris.copy(alpha = 0.50f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clozeMode) Modifier.clickable { onToggleClozeReveal() } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = "문장 듣기",
                        tint = StudioCobalt,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Text(
                text = formattedSentence,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (occurrence.sentenceKo.isNotBlank()) {
                Text(
                    text = occurrence.sentenceKo,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (clozeMode) {
                Text(
                    text = if (isClozeRevealed) {
                        "정답 어형: ${occurrence.surfaceForm} (카드를 탭하여 다시 가리기)"
                    } else {
                        "빈칸에 들어갈 어형을 떠올린 뒤 카드를 탭해 정답을 확인하세요."
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isClozeRevealed) AdventureEmerald else StudioIris
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showGrammarSpans) {
                    detectedRolesInSentence.forEach { role ->
                        val rColor = grammarRoleColor(role)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = rColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = role.shortLabelKo,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                ),
                                color = rColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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
            .heightIn(min = 36.dp)
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
