package com.example.ui.components

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.ExamCorpusRepository
import com.example.engine.DDayPolicy
import com.example.engine.FsrsSchedulerEngine
import com.example.engine.RecallGrade
import com.example.engine.StudyPhase
import com.example.engine.WordLearningProgress
import com.example.model.CorpusRuntimeMode
import com.example.model.ExamCorpusWordEntry
import com.example.model.GrammarCategory
import com.example.model.VocabDifficultyTier
import com.example.model.VocabWord
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class StudyLabTab(val labelKo: String) {
    ALL_ACTIVE("전체(D제외)"),
    REVENGE_BOSS("보스 복수전"),
    WORD_RUN_2027("2027 10일 챌린지"),
    TIER_S("Tier S"),
    TIER_A("Tier A"),
    TIER_B("Tier B"),
    TIER_C("Tier C"),
    TIER_D("Tier D 각주"),
    NEW_3YR("최근 신출"),
    LISTENING("듣기 비중↑"),
    GRAMMAR("6대 영문법")
}

private fun difficultyTierColor(tier: VocabDifficultyTier): Color = when (tier) {
    VocabDifficultyTier.TIER_S_CORE -> SpartaRed
    VocabDifficultyTier.TIER_A_CYCLICAL -> StudioCobalt
    VocabDifficultyTier.TIER_B_ADVANCED_NEW -> RoyalPurple
    VocabDifficultyTier.TIER_C_BURSTY -> SkyQuestBlue
    VocabDifficultyTier.TIER_D_FOOTNOTE -> Color(0xFF64748B)
}

/**
 * Compact expandable corpus analytics & usage-history panel embedded inside
 * `VocabMemorizationDeckView` so learners can inspect 5-tier uniformity, surface forms,
 * and expand paginated Cloze/usage history with 1 tap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordCorpusExpandablePanel(
    word: VocabWord,
    onSpeakEnglish: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snapshot = remember(context) { ExamCorpusRepository.loadSnapshot(context) }
    val entry = remember(snapshot, word.id, word.word) {
        snapshot.searchIndex.findByWordId(word.id)
            ?: snapshot.searchIndex.findByTokenOrLemma(word.word)
    } ?: return

    var expanded by remember(word.id) { mutableStateOf(false) }
    val accent = difficultyTierColor(entry.analytics.difficultyTier)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("word_corpus_expandable_panel")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable { expanded = !expanded }
                    .testTag("toggle_word_corpus_occurrences"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(shape = RoundedCornerShape(999.dp), color = accent) {
                        Text(
                            text = entry.analytics.difficultyTier.shortBadgeKo,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = StudioCobalt.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${entry.analytics.curriculumGroup.labelKo} · 균등도 ${"%.2f".format(entry.analytics.griesDispersion.uniformity)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = StudioCobalt,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "역대 용례 (${entry.occurrences.size}건)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = "역대 용례 펼치기",
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetricMiniChip("총 ${entry.analytics.totalFrequency}회", StudioCobalt)
                MetricMiniChip("수능 ${entry.analytics.csatFrequency}회", SpartaRed)
                MetricMiniChip("주기 ${"%.1f".format(entry.analytics.avgRecurrenceGapYears)}년", SkyQuestBlue)
                MetricMiniChip("듣기 ${(entry.analytics.listeningRatio * 100).roundToInt()}%", AdventureEmerald)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CorpusExampleExpandDrawer(
                    entry = entry,
                    provenanceNoticeKo = snapshot.provenanceNoticeKo,
                    onSpeakEnglish = onSpeakEnglish,
                    onJumpToGrammarCategory = {}
                )
            }
        }
    }
}

/**
 * Full 10-Year 5-Tier Analytics, SQLite+FTS5 Search, Hybrid BYOD Import,
 * Kotlin Native FSRS + D-Day 3-Phase Guardrail & 6-Category Grammar Lab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystematicStudyLabSection(
    harvestedApiWords: List<VocabWord>,
    wordSm2Progress: Map<String, WordLearningProgress>,
    onSpeakEnglish: (String) -> Unit,
    onRecordSm2Recall: (VocabWord, RecallGrade, Boolean) -> Unit,
    csatDDayDays: Int = FsrsSchedulerEngine.DEFAULT_DAYS_UNTIL_CSAT,
    onSetCsatDDayDays: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var reloadNonce by remember { mutableIntStateOf(0) }
    val snapshot = remember(context, harvestedApiWords.size, reloadNonce) {
        ExamCorpusRepository.loadSnapshot(context, harvestedApiWords)
    }
    val dDayPolicy = remember(csatDDayDays) { FsrsSchedulerEngine.resolveDDayPolicy(csatDDayDays) }

    var activeTab by remember { mutableStateOf(StudyLabTab.ALL_ACTIVE) }
    var rawSearchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var ftsMatchedWordIds by remember { mutableStateOf<Set<String>?>(null) }
    var selectedGrammarCategory by remember { mutableStateOf<GrammarCategory?>(null) }
    var byodStatusBannerKo by remember { mutableStateOf<String?>(null) }
    var byodStatusError by remember { mutableStateOf(false) }
    val expandedWordIds = remember { mutableStateMapOf<String, Boolean>() }

    // 120ms debounced FTS5 full-text query with automatic coroutine cancellation
    LaunchedEffect(rawSearchQuery, reloadNonce) {
        delay(120L)
        debouncedQuery = rawSearchQuery.trim()
        ftsMatchedWordIds = if (debouncedQuery.length >= 2) {
            withContext(Dispatchers.IO) {
                ExamCorpusRepository.searchWordIdsByFts(context, debouncedQuery)
            }
        } else {
            null
        }
    }

    // Zero-permission Android Storage Access Framework (SAF) file picker for BYOD import
    val byodPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fileName = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull() ?: uri.lastPathSegment ?: "byod_corpus.json"

            val result = withContext(Dispatchers.IO) {
                ExamCorpusRepository.importByodFromUri(context, uri, fileName)
            }
            result.onSuccess { outcome ->
                byodStatusError = false
                byodStatusBannerKo = outcome.messageKo
                reloadNonce += 1
            }.onFailure { err ->
                byodStatusError = true
                byodStatusBannerKo = "불러오기 실패: ${err.message ?: "형식을 확인하세요"}"
            }
        }
    }

    val normalizedHint = remember(debouncedQuery, snapshot) {
        snapshot.searchIndex.resolveNormalizedLemma(debouncedQuery)
    }

    val filteredEntries = remember(snapshot, activeTab, debouncedQuery, ftsMatchedWordIds) {
        val diffTier = when (activeTab) {
            StudyLabTab.TIER_S -> VocabDifficultyTier.TIER_S_CORE
            StudyLabTab.TIER_A -> VocabDifficultyTier.TIER_A_CYCLICAL
            StudyLabTab.TIER_B -> VocabDifficultyTier.TIER_B_ADVANCED_NEW
            StudyLabTab.TIER_C -> VocabDifficultyTier.TIER_C_BURSTY
            StudyLabTab.TIER_D -> VocabDifficultyTier.TIER_D_FOOTNOTE
            else -> null
        }
        snapshot.searchIndex.query(
            rawQuery = debouncedQuery,
            difficultyTierFilter = diffTier,
            includeFootnoteTierD = activeTab == StudyLabTab.TIER_D,
            onlyRecent3YrNew = activeTab == StudyLabTab.NEW_3YR,
            onlyListeningHeavy = activeTab == StudyLabTab.LISTENING,
            ftsMatchedWordIds = ftsMatchedWordIds
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("systematic_study_lab_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. D-Day 3-Phase FSRS Guardrail Banner
        DDayFsrsGuardrailBanner(
            policy = dDayPolicy,
            onSetDays = onSetCsatDDayDays
        )

        // 2. Hybrid Copyright & Local BYOD Control Bar (SAF zero-permission import)
        HybridByodStatusCard(
            runtimeMode = snapshot.runtimeMode,
            provenanceNoticeKo = snapshot.provenanceNoticeKo,
            statusBannerKo = byodStatusBannerKo,
            isError = byodStatusError,
            onImportClick = {
                byodPicker.launch(arrayOf("application/json", "application/octet-stream", "application/gzip", "*/*"))
            },
            onRestoreCurated = {
                scope.launch {
                    withContext(Dispatchers.IO) { ExamCorpusRepository.restoreCuratedMode(context) }
                    byodStatusError = false
                    byodStatusBannerKo = "저작권 안전 창작·통계 기본 모드로 복원했습니다."
                    reloadNonce += 1
                }
            }
        )

        // 3. 5-Tier + Special Tabs (DifficultyTierTabs)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.testTag("difficulty_tier_tabs")
        ) {
            items(StudyLabTab.entries) { tab ->
                val countSuffix = when (tab) {
                    StudyLabTab.REVENGE_BOSS -> " ⚡"
                    StudyLabTab.WORD_RUN_2027 -> " (500)"
                    StudyLabTab.TIER_S -> " (${snapshot.summary.tierCounts[VocabDifficultyTier.TIER_S_CORE] ?: 0})"
                    StudyLabTab.TIER_A -> " (${snapshot.summary.tierCounts[VocabDifficultyTier.TIER_A_CYCLICAL] ?: 0})"
                    StudyLabTab.TIER_B -> " (${snapshot.summary.tierCounts[VocabDifficultyTier.TIER_B_ADVANCED_NEW] ?: 0})"
                    StudyLabTab.TIER_C -> " (${snapshot.summary.tierCounts[VocabDifficultyTier.TIER_C_BURSTY] ?: 0})"
                    StudyLabTab.TIER_D -> " (${snapshot.summary.tierCounts[VocabDifficultyTier.TIER_D_FOOTNOTE] ?: 0})"
                    StudyLabTab.GRAMMAR -> " (${snapshot.grammarSentences.size})"
                    else -> ""
                }
                val accent = when (tab) {
                    StudyLabTab.REVENGE_BOSS -> SpartaRed
                    StudyLabTab.WORD_RUN_2027 -> WordRunNavy
                    StudyLabTab.TIER_S -> SpartaRed
                    StudyLabTab.TIER_A -> StudioCobalt
                    StudyLabTab.TIER_B -> RoyalPurple
                    StudyLabTab.TIER_C -> SkyQuestBlue
                    StudyLabTab.TIER_D -> Color(0xFF64748B)
                    StudyLabTab.NEW_3YR -> CoralOrange
                    StudyLabTab.LISTENING -> AdventureEmerald
                    StudyLabTab.GRAMMAR -> StudioIris
                    StudyLabTab.ALL_ACTIVE -> StudioCobalt
                }
                val selected = activeTab == tab
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) accent else accent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (selected) accent else accent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { activeTab = tab }
                        .testTag("tier_tab_${tab.name}")
                ) {
                    Text(
                        text = "${tab.labelKo}$countSuffix",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (selected) Color.White else accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }

        if (activeTab == StudyLabTab.REVENGE_BOSS) {
            val arenaWords = remember(snapshot.entries) { snapshot.entries.map { it.sourceWord } }
            GameRevengeBossArenaView(
                candidateWords = arenaWords,
                wordProgress = wordSm2Progress,
                onSpeakEnglish = onSpeakEnglish,
                onRecordFsrsWord = onRecordSm2Recall
            )
        } else if (activeTab == StudyLabTab.WORD_RUN_2027) {
            WordRun2027ChallengeView(
                onSpeakEnglish = onSpeakEnglish,
                onRecordFsrsWord = onRecordSm2Recall
            )
        } else if (activeTab == StudyLabTab.GRAMMAR) {
            CorpusGrammarLabView(
                snapshot = snapshot,
                selectedCategory = selectedGrammarCategory,
                onSelectCategory = { selectedGrammarCategory = it },
                onSpeakEnglish = onSpeakEnglish,
                onAddWordToFsrsDeck = { word ->
                    onRecordSm2Recall(word, RecallGrade.HARD, false)
                }
            )
        } else {
            // 4. 120ms Debounced FTS5 Search Input
            OutlinedTextField(
                value = rawSearchQuery,
                onValueChange = { rawSearchQuery = it },
                singleLine = true,
                placeholder = { Text("표제어 · 변화형(crucially) · 예문 전문(FTS5) 검색") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "코퍼스 검색") },
                trailingIcon = {
                    if (rawSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { rawSearchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "검색어 지우기")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("corpus_fts_search_input")
            )

            if (normalizedHint != null) {
                Text(
                    text = "표제어 정규화 자동 매칭: '$debouncedQuery' → '$normalizedHint'",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AdventureEmerald
                )
            }

            if (activeTab == StudyLabTab.TIER_D) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF64748B).copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tier D(시험지 * 각주 전문어·예외군)는 기본 암기 덱에서 자동 숨김 처리되며 사전 확인 시에만 열람합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Text(
                text = "표시 중 ${filteredEntries.size}개 어휘 · 평균 균등도(1-DP_norm) ${"%.2f".format(snapshot.summary.avgUniformity)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 5. Collapsed WordCards + Lazy Expandable Drawer
            filteredEntries.take(40).forEach { entry ->
                val isExpanded = expandedWordIds[entry.wordId] == true
                val progress = wordSm2Progress[entry.wordId]
                TieredCorpusWordCard(
                    entry = entry,
                    progress = progress,
                    policy = dDayPolicy,
                    provenanceNoticeKo = snapshot.provenanceNoticeKo,
                    expanded = isExpanded,
                    onToggleExpand = { expandedWordIds[entry.wordId] = !isExpanded },
                    onSpeakEnglish = onSpeakEnglish,
                    onGradeFsrs = { grade -> onRecordSm2Recall(entry.sourceWord, grade, false) },
                    onJumpToGrammarCategory = { cat ->
                        selectedGrammarCategory = cat
                        activeTab = StudyLabTab.GRAMMAR
                    }
                )
            }
        }
    }
}

@Composable
private fun DDayFsrsGuardrailBanner(
    policy: DDayPolicy,
    onSetDays: (Int) -> Unit
) {
    val accent = when (policy.phase) {
        StudyPhase.PHASE_1_BUILD -> StudioCobalt
        StudyPhase.PHASE_2_CONSOLIDATE -> CoralOrange
        StudyPhase.PHASE_3_LOCKDOWN -> SpartaRed
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fsrs_dday_guardrail_banner")
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (policy.phase == StudyPhase.PHASE_3_LOCKDOWN) Icons.Outlined.Lock else Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "FSRS 망각곡선 · D-${policy.daysUntilCsat} ${policy.phase.shortBadgeKo}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = accent
                    )
                }
                Surface(shape = RoundedCornerShape(999.dp), color = accent) {
                    Text(
                        text = "기억률 ${(policy.desiredRetention * 100).roundToInt()}% · 신규 ${policy.newCardsPerDay}/일",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = policy.phase.summaryKo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Quick D-Day presets for instant phase control
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val presets = listOf(90 to "D-90 구축", 30 to "D-30 정리", 10 to "D-10 락다운")
                items(presets) { (days, label) ->
                    val active = when (days) {
                        90 -> policy.phase == StudyPhase.PHASE_1_BUILD
                        30 -> policy.phase == StudyPhase.PHASE_2_CONSOLIDATE
                        else -> policy.phase == StudyPhase.PHASE_3_LOCKDOWN
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (active) accent else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onSetDays(days) }
                            .testTag("dday_preset_$days")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (active) Color.White else accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HybridByodStatusCard(
    runtimeMode: CorpusRuntimeMode,
    provenanceNoticeKo: String,
    statusBannerKo: String?,
    isError: Boolean,
    onImportClick: () -> Unit,
    onRestoreCurated: () -> Unit
) {
    val isByod = runtimeMode != CorpusRuntimeMode.CURATED_ORIGINAL
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hybrid_byod_status_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = (if (isByod) AdventureEmerald else StudioCobalt).copy(alpha = 0.14f)
                ) {
                    Text(
                        text = runtimeMode.labelKo,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isByod) AdventureEmerald else StudioCobalt,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = StudioCobalt,
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onImportClick() }
                            .testTag("import_byod_corpus_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = "로컬 코퍼스 불러오기",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "로컬 DB·JSON",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }
                    if (isByod) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = SpartaRed.copy(alpha = 0.12f),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { onRestoreCurated() }
                                .testTag("restore_curated_mode_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Restore,
                                    contentDescription = "기본 모드 복원",
                                    tint = SpartaRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "기본 복원",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = SpartaRed
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = provenanceNoticeKo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!statusBannerKo.isNullOrBlank()) {
                Text(
                    text = statusBannerKo,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isError) SpartaRed else AdventureEmerald
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TieredCorpusWordCard(
    entry: ExamCorpusWordEntry,
    progress: WordLearningProgress?,
    policy: DDayPolicy,
    provenanceNoticeKo: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onGradeFsrs: (RecallGrade) -> Unit,
    onJumpToGrammarCategory: (GrammarCategory) -> Unit
) {
    val a = entry.analytics
    val accent = difficultyTierColor(a.difficultyTier)
    val isNewBlockedByLockdown = policy.phase == StudyPhase.PHASE_3_LOCKDOWN && progress == null

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.34f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("corpus_word_card_${entry.wordId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Lemma + Tier + Curriculum + TTS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.lemma,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(shape = RoundedCornerShape(999.dp), color = accent) {
                            Text(
                                text = a.difficultyTier.shortBadgeKo,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(999.dp), color = StudioCobalt.copy(alpha = 0.12f)) {
                            Text(
                                text = "${a.curriculumGroup.labelKo} · ${entry.cefr.code}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = StudioCobalt,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${entry.ipa} · ${entry.pos} · ${entry.meaningKo}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { onSpeakEnglish(entry.lemma) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = "${entry.lemma} 듣기",
                        tint = StudioCobalt
                    )
                }
            }

            // 5 Quantitative Statistical Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetricMiniChip("총 ${a.totalFrequency}회", StudioCobalt)
                MetricMiniChip("수능 ${a.csatFrequency}회", SpartaRed)
                MetricMiniChip("균등도 ${"%.2f".format(a.griesDispersion.uniformity)}", AdventureEmerald)
                MetricMiniChip("주기 ${"%.1f".format(a.avgRecurrenceGapYears)}년", SkyQuestBlue)
                MetricMiniChip("듣기 ${(a.listeningRatio * 100).roundToInt()}%", RoyalPurple)
                if (progress != null) {
                    val sLabel = if (progress.fsrsStabilityDays > 0.0) {
                        "FSRS S=${"%.1f".format(progress.fsrsStabilityDays)}d"
                    } else {
                        "숙련 ${progress.masteryScore}%"
                    }
                    MetricMiniChip(sLabel, TrophyGold)
                }
            }

            // Surface variant summary badges
            if (entry.surfaceVariants.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.surfaceVariants.take(4).forEach { sv ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${sv.form} (${sv.count})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // FSRS 4-Grade Review Bar (with D-15 Lockdown Guardrail for new unstudied words)
            if (isNewBlockedByLockdown) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SpartaRed.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "D-15 락다운 모드: 신규 단어 투입이 차단되었습니다. 기존 오답·복습 단어와 핵심 어법에 집중하세요.",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SpartaRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RecallGrade.entries.forEach { grade ->
                        val preview = remember(entry.wordId, progress, grade, policy.daysUntilCsat) {
                            FsrsSchedulerEngine.previewIntervalLabel(
                                existing = progress,
                                wordId = entry.wordId,
                                grade = grade,
                                daysUntilCsat = policy.daysUntilCsat
                            )
                        }
                        val gAccent = when (grade) {
                            RecallGrade.AGAIN -> SpartaRed
                            RecallGrade.HARD -> CoralOrange
                            RecallGrade.GOOD -> StudioCobalt
                            RecallGrade.EASY -> AdventureEmerald
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = gAccent.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, gAccent.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onGradeFsrs(grade) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = grade.labelKo,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = gAccent
                                )
                                Text(
                                    text = preview,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Lazy Expand Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleExpand() }
                    .testTag("expand_word_occurrences_${entry.wordId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "역대 기출·학습 용례 접기" else "역대 용례 ${entry.occurrences.size}건 모두 보기 · Cloze",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = accent
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CorpusExampleExpandDrawer(
                    entry = entry,
                    provenanceNoticeKo = provenanceNoticeKo,
                    onSpeakEnglish = onSpeakEnglish,
                    onJumpToGrammarCategory = onJumpToGrammarCategory
                )
            }
        }
    }
}

@Composable
private fun MetricMiniChip(label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.11f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = accent,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
