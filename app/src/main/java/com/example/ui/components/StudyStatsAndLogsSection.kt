package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudyTimeLogEntry
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalPurpleShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatDurationKo(totalSec: Int): String {
    val safe = totalSec.coerceAtLeast(0)
    val hrs = safe / 3600
    val mins = (safe % 3600) / 60
    val secs = safe % 60
    return when {
        hrs > 0 -> "${hrs}시간 ${mins}분 ${secs}초"
        mins > 0 -> "${mins}분 ${secs}초"
        else -> "${secs}초"
    }
}

@Composable
fun StudyStatsOverviewCards(
    logs: List<StudyTimeLogEntry>,
    activeStudyElapsedSec: Int,
    onFlushCurrentLog: () -> Unit
) {
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val totalSec = logs.sumOf { it.durationSec } + activeStudyElapsedSec
    val todaySec = logs.filter { it.dateIso == todayIso }.sumOf { it.durationSec } + activeStudyElapsedSec
    val totalKnown = logs.sumOf { it.knownCount }
    val totalUnknown = logs.sumOf { it.unknownCount }
    val totalChecked = (totalKnown + totalUnknown).coerceAtLeast(1)
    val knownRatio = (totalKnown.toFloat() / totalChecked.toFloat()).coerceIn(0f, 1f)
    val knownPct = (knownRatio * 100).toInt()

    val bigTtungSec = logs.filter { it.playerName.contains("빅뚱") }.sumOf { it.durationSec }
    val smallTtungSec = logs.filter { it.playerName.contains("스몰뚱") }.sumOf { it.durationSec }

    // Last 7 days bar chart data
    val last7Days = remember(logs, activeStudyElapsedSec) {
        val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("M/d", Locale.KOREA)
        val cal = Calendar.getInstance()
        (6 downTo 0).map { offset ->
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val iso = isoFmt.format(cal.time)
            val label = labelFmt.format(cal.time)
            val daySec = logs.filter { it.dateIso == iso }.sumOf { it.durationSec } +
                if (iso == todayIso) activeStudyElapsedSec else 0
            Triple(iso, label, daySec)
        }
    }
    val maxDaySec = (last7Days.maxOfOrNull { it.third } ?: 60).coerceAtLeast(60)

    // Method stats breakdown
    val methodStats = remember(logs) {
        logs.groupBy { it.methodKo }
            .map { (method, entries) ->
                Triple(method, entries.sumOf { it.durationSec }, entries.sumOf { it.wordsStudied })
            }
            .sortedByDescending { it.second }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Live Exact Study Time Summary + Save Now Button
        Elevated3DCard(accentShadow = SkyQuestBlueShadow.copy(alpha = 0.35f)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = SkyQuestBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "정확한 학습 시간 통계",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AdventureEmerald.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "기록 ${logs.size}건",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AdventureEmeraldShadow,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMiniTile(
                        title = "오늘 학습 시간",
                        value = formatDurationKo(todaySec),
                        accent = AdventureEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniTile(
                        title = "총 누적 학습 시간",
                        value = formatDurationKo(totalSec),
                        accent = SkyQuestBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMiniTile(
                        title = "빅뚱 누적 시간",
                        value = formatDurationKo(bigTtungSec),
                        accent = TrophyGoldShadow,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniTile(
                        title = "스몰뚱 누적 시간",
                        value = formatDurationKo(smallTtungSec),
                        accent = CoralOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Tactile3DButton(
                    text = "현재 세션 시간 (${formatDurationKo(activeStudyElapsedSec)}) 지금 로그 저장",
                    icon = Icons.Default.Save,
                    containerColor = SkyQuestBlue,
                    shadowColor = SkyQuestBlueShadow,
                    height = 44.dp,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "stats_flush_study_log_button",
                    onClick = onFlushCurrentLog
                )
            }
        }

        // 2. Know vs Don't Know Diagnostic Accuracy Card
        Elevated3DCard(accentShadow = AdventureEmeraldShadow.copy(alpha = 0.35f)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "단어 알아요 / 몰라요 메타인지 진단 통계",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = if (totalKnown + totalUnknown == 0) "측정 대기" else "알고 있는 비율 $knownPct%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = AdventureEmeraldShadow
                    )
                }
                LinearProgressIndicator(
                    progress = { if (totalKnown + totalUnknown == 0) 0.5f else knownRatio },
                    color = AdventureEmerald,
                    trackColor = SpartaRed.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "완전 알아요 (O): ${totalKnown}회",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AdventureEmeraldShadow
                    )
                    Text(
                        text = "애매·몰라요 (△/X): ${totalUnknown}회",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SpartaRed
                    )
                }
            }
        }

        // 3. Last 7 Days Daily Study Time Bar Chart
        Elevated3DCard(accentShadow = RoyalPurpleShadow.copy(alpha = 0.35f)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "최근 7일 일별 학습 시간 그래프",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    last7Days.forEach { (iso, label, daySec) ->
                        val frac = (daySec.toFloat() / maxDaySec.toFloat()).coerceIn(0.08f, 1f)
                        val isToday = iso == todayIso
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = if (daySec >= 60) "${daySec / 60}분" else "${daySec}초",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height((76 * frac).dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(if (isToday) AdventureEmerald else RoyalPurple.copy(alpha = 0.72f))
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isToday) "오늘" else label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (isToday) AdventureEmeraldShadow else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 4. Learning Method Breakdown (해마학습법 · 단어연상법 · 망각곡선 등)
        if (methodStats.isNotEmpty()) {
            Elevated3DCard(accentShadow = TrophyGoldShadow.copy(alpha = 0.35f)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "단어 학습법별 누적 활용 통계",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    val maxMethodSec = methodStats.maxOf { it.second }.coerceAtLeast(1)
                    methodStats.forEach { (methodName, sec, words) ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = methodName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${formatDurationKo(sec)} · ${words}단어",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (sec.toFloat() / maxMethodSec.toFloat()).coerceIn(0.08f, 1f) },
                                color = TrophyGoldShadow,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }

        // 5. Exact Timestamped Study Time Logs Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "정확한 학습 시간 상세 로그 (${logs.size}건)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
        }

        if (logs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "단어·패턴·영상통화 탭에서 학습 후 [시간 로그 저장]을 누르거나 탭을 이동하면 시작~종료 시각과 정확한 소요 시간(분/초)이 자동 기록됩니다!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            logs.take(25).forEach { entry ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${entry.playerName} · ${entry.activityTypeKo} (${entry.methodKo})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SkyQuestBlue.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = formatDurationKo(entry.durationSec),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SkyQuestBlueShadow,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "${entry.dateIso}  ${entry.startTimeLabel} ~ ${entry.endTimeLabel}  ·  알아요 ${entry.knownCount} / 몰라요 ${entry.unknownCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = accent
            )
        }
    }
}
