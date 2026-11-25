package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VocabWord
import com.example.model.WrongNoteRecord
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WrongNoteFilterTab(
    val labelKo: String,
    val tag: String
) {
    ALL("전체", "wrong_note_tab_all"),
    NEEDS_STUDY("아직 학습 필요", "wrong_note_tab_needs_study"),
    MASTERED("완벽 암기", "wrong_note_tab_mastered")
}

data class WrongNoteDisplayItem(
    val word: VocabWord,
    val record: WrongNoteRecord
)

fun buildWrongNoteDisplayItems(
    allWords: List<VocabWord>,
    records: Map<String, WrongNoteRecord>
): List<WrongNoteDisplayItem> {
    if (records.isEmpty()) return emptyList()
    val wordsById = allWords.associateBy { it.id }
    return records.values
        .mapNotNull { record ->
            val word = wordsById[record.wordId] ?: return@mapNotNull null
            WrongNoteDisplayItem(word = word, record = record)
        }
        .sortedWith(
            compareByDescending<WrongNoteDisplayItem> { it.record.lastStudiedAtMs }
                .thenByDescending { it.record.lastStudiedDateIso }
                .thenBy { it.word.word.lowercase(Locale.ROOT) }
        )
}

fun filterWrongNoteItems(
    items: List<WrongNoteDisplayItem>,
    tab: WrongNoteFilterTab
): List<WrongNoteDisplayItem> {
    return when (tab) {
        WrongNoteFilterTab.ALL -> items
        WrongNoteFilterTab.NEEDS_STUDY -> items.filter { !it.record.isMastered }
        WrongNoteFilterTab.MASTERED -> items.filter { it.record.isMastered }
    }
}

fun formatWrongNoteStudyDate(
    dateIso: String,
    todayIso: String
): String {
    val clean = dateIso.trim()
    if (clean.isBlank()) return "학습일 기록 없음"
    val dotted = clean.replace('-', '.')
    return if (clean == todayIso) {
        "오늘 학습 · $dotted"
    } else {
        "학습일 $dotted"
    }
}

@Composable
fun WrongAnswerNotebookSection(
    allWords: List<VocabWord>,
    wrongNoteRecords: Map<String, WrongNoteRecord>,
    isWrongReviewMode: Boolean,
    onToggleWrongReviewMode: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onMarkWordResult: (wordId: String, known: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var selectedTab by rememberSaveable { mutableStateOf(WrongNoteFilterTab.ALL) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val allItems = remember(allWords, wrongNoteRecords) {
        buildWrongNoteDisplayItems(allWords = allWords, records = wrongNoteRecords)
    }
    val needsStudyCount = remember(allItems) { allItems.count { !it.record.isMastered } }
    val masteredCount = remember(allItems) { allItems.count { it.record.isMastered } }
    val filteredItems = remember(allItems, selectedTab) {
        filterWrongNoteItems(allItems, selectedTab)
    }
    val visibleItems = if (expanded) filteredItems else filteredItems.take(5)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.28f)),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("vocab_wrong_notebook_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    StitchIconBadge(
                        icon = Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.wrong_note_title),
                        accent = StudioCoral,
                        size = 38.dp,
                        iconSize = 20.dp,
                        cornerBadgeText = "${allItems.size}"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.wrong_note_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.wrong_note_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (needsStudyCount > 0 || isWrongReviewMode) {
                    StitchIconMetricChip(
                        icon = Icons.Outlined.Refresh,
                        valueText = if (isWrongReviewMode) "전체 덱" else "오답 집중",
                        accent = StudioCoral,
                        contentDescription = "오답 집중 모드 전환",
                        onClick = onToggleWrongReviewMode
                    )
                }
            }

            // Filtering Tabs Row: 전체 / 아직 학습 필요 / 완벽 암기
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wrong_note_filter_tabs"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabSpecs = listOf(
                    Triple(WrongNoteFilterTab.ALL, allItems.size, StudioIris),
                    Triple(WrongNoteFilterTab.NEEDS_STUDY, needsStudyCount, StudioCoral),
                    Triple(WrongNoteFilterTab.MASTERED, masteredCount, AdventureEmerald)
                )
                tabSpecs.forEach { (tab, count, accent) ->
                    val selected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(if (tab == WrongNoteFilterTab.ALL) 0.78f else 1.11f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedTab = tab
                                expanded = false
                            }
                            .testTag(tab.tag)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tab.labelKo,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                ),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${count}개",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (selected) Color.White.copy(alpha = 0.92f) else accent
                            )
                        }
                    }
                }
            }

            // Filtered Word List or Contextual Empty State
            if (filteredItems.isEmpty()) {
                val emptyText = when (selectedTab) {
                    WrongNoteFilterTab.ALL ->
                        "아직 오답 노트에 보관된 단어가 없어요. 단어 카드에서 '오답 노트'를 누르면 학습 날짜와 함께 저장됩니다."
                    WrongNoteFilterTab.NEEDS_STUDY ->
                        if (masteredCount > 0) {
                            "아직 학습이 필요한 오답을 모두 정복했어요! '완벽 암기' 탭에서 기록을 확인해 보세요."
                        } else {
                            "학습이 필요한 오답 단어가 없습니다."
                        }
                    WrongNoteFilterTab.MASTERED ->
                        "아직 완벽 암기한 오답 단어가 없어요. 오답을 복습한 뒤 체크 버튼을 눌러 정복해 보세요."
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wrong_note_empty_state")
                ) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleItems.forEach { item ->
                        WrongNoteWordRow(
                            item = item,
                            todayIso = todayIso,
                            onSpeakWord = onSpeakWord,
                            onMarkWordResult = onMarkWordResult
                        )
                    }
                }

                if (filteredItems.size > 5) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { expanded = !expanded }
                            .testTag("wrong_note_expand_toggle")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (expanded) {
                                    "오답 목록 접기 ▴"
                                } else {
                                    "보관된 단어 ${filteredItems.size}개 모두 보기 ▾"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StudioCobalt
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WrongNoteWordRow(
    item: WrongNoteDisplayItem,
    todayIso: String,
    onSpeakWord: (String) -> Unit,
    onMarkWordResult: (wordId: String, known: Boolean) -> Unit
) {
    val w = item.word
    val mastered = item.record.isMastered
    val statusAccent = if (mastered) AdventureEmerald else StudioCoral
    val statusAccentText = if (mastered) AdventureEmeraldShadow else StudioCoral
    val dateLabel = formatWrongNoteStudyDate(item.record.lastStudiedDateIso, todayIso)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (mastered) {
            AdventureEmerald.copy(alpha = 0.07f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSpeakWord(w.word) }
            .testTag("wrong_note_item_${w.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = w.word,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusAccent.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (mastered) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = statusAccentText,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (mastered) R.string.wrong_note_badge_mastered
                                    else R.string.wrong_note_badge_needs_study
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = statusAccentText
                            )
                        }
                    }
                }

                Text(
                    text = w.meaningKo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Learning Date Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.testTag("wrong_note_date_${w.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "학습 날짜",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StitchIconButton(
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = "${w.word} 발음 듣기",
                    accent = StudioCobalt,
                    size = 42.dp,
                    iconSize = 18.dp,
                    tag = "wrong_note_speak_${w.id}",
                    onClick = { onSpeakWord(w.word) }
                )
                if (mastered) {
                    StitchIconButton(
                        icon = Icons.Rounded.Replay,
                        contentDescription = stringResource(R.string.wrong_note_action_relearn),
                        accent = StudioCoral,
                        filled = false,
                        size = 42.dp,
                        iconSize = 18.dp,
                        tag = "wrong_note_relearn_${w.id}",
                        onClick = { onMarkWordResult(w.id, false) }
                    )
                } else {
                    StitchIconButton(
                        icon = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.wrong_note_action_mark_mastered),
                        accent = AdventureEmerald,
                        filled = true,
                        size = 42.dp,
                        iconSize = 18.dp,
                        tag = "wrong_note_master_${w.id}",
                        onClick = { onMarkWordResult(w.id, true) }
                    )
                }
            }
        }
    }
}
