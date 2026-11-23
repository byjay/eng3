package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SquirrelJobHero
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow

data class DailyStudyMetric(
    val dateIso: String,
    val dayLabelKo: String,
    val isToday: Boolean,
    val knownCount: Int,
    val reviewCount: Int
) {
    val totalAttempts: Int get() = knownCount + reviewCount
    val accuracyPercent: Int
        get() = if (totalAttempts <= 0) 0 else ((knownCount * 100f) / totalAttempts).toInt().coerceIn(0, 100)
}

data class TodayWordResultEntry(
    val wordId: String,
    val word: String,
    val meaningKo: String,
    val known: Boolean,
    val dateIso: String,
    val timestampMs: Long
)

data class WeeklyStudyReportData(
    val dailyMetrics: List<DailyStudyMetric> = emptyList(),
    val todayWordResults: List<TodayWordResultEntry> = emptyList(),
    val cumulativeStreakCount: Int = 0,
    val todayWordsCompleted: Int = 0,
    val dailyGoalTarget: Int = DAILY_STREAK_WORD_STEP
) {
    val todayMetric: DailyStudyMetric? get() = dailyMetrics.lastOrNull()
    val todayKnownCount: Int get() = maxOf(todayMetric?.knownCount ?: 0, todayWordsCompleted)
    val todayReviewCount: Int get() = todayMetric?.reviewCount ?: 0
    val todayTotalAttempts: Int get() = todayKnownCount + todayReviewCount
    val todayAccuracyPercent: Int
        get() = if (todayTotalAttempts <= 0) 0 else ((todayKnownCount * 100f) / todayTotalAttempts).toInt().coerceIn(0, 100)

    val todayGradeLabel: String
        get() = when {
            todayTotalAttempts == 0 -> "도전 중"
            todayAccuracyPercent >= 95 && todayKnownCount >= 10 -> "S+"
            todayAccuracyPercent >= 90 -> "A+"
            todayAccuracyPercent >= 80 -> "A"
            todayAccuracyPercent >= 65 -> "B+"
            else -> "B"
        }

    val weeklyTotalKnown: Int get() = dailyMetrics.sumOf { it.knownCount }
    val weeklyTotalAttempts: Int get() = dailyMetrics.sumOf { it.totalAttempts }
    val weeklyAccuracyPercent: Int
        get() = if (weeklyTotalAttempts <= 0) 0 else ((weeklyTotalKnown * 100f) / weeklyTotalAttempts).toInt().coerceIn(0, 100)
    val activeDaysCount: Int get() = dailyMetrics.count { it.totalAttempts > 0 }
}

@Composable
fun WeeklyStudyReportModal(
    report: WeeklyStudyReportData,
    playerName: String,
    activeHero: SquirrelJobHero,
    onDismiss: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val animFraction by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "weekly_report_chart_anim"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFBF8F1),
            border = BorderStroke(2.dp, TrophyGold),
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 22.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("weekly_report_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0E6F52), Color(0xFF148F68), Color(0xFF1D4ED8))
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            SquirrelHeroAvatar(
                                hero = activeHero,
                                size = 48.dp,
                                mood = if (report.todayKnownCount >= 10) SquirrelMoodState.VICTORY_DANCE else SquirrelMoodState.HAPPY_CHEER,
                                borderWidth = 2.dp
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = TrophyGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "주간 학습 리포트",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White,
                                        modifier = Modifier.testTag("weekly_report_title")
                                    )
                                }
                                Text(
                                    text = "$playerName · 최근 7일 성취 및 오늘 단어 성적표",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("weekly_report_top_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "리포트 닫기",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Top 3 KPI Cards (Cumulative Streak / Today Grade / Today Words)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportKpiTile(
                            title = "누적 스트릭",
                            value = "${report.cumulativeStreakCount}회",
                            subtext = "10단어 달성",
                            accentColor = CoralOrange,
                            bgColor = Color(0xFFFFF3E8),
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("report_kpi_streak")
                        )
                        ReportKpiTile(
                            title = "오늘 성적",
                            value = report.todayGradeLabel,
                            subtext = "정답률 ${report.todayAccuracyPercent}%",
                            accentColor = SkyQuestBlueShadow,
                            bgColor = Color(0xFFEFF6FF),
                            icon = Icons.Default.EmojiEvents,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("report_kpi_grade")
                        )
                        ReportKpiTile(
                            title = "오늘 완료",
                            value = "${report.todayKnownCount}단어",
                            subtext = "주간 ${report.weeklyTotalKnown}단어",
                            accentColor = AdventureEmeraldShadow,
                            bgColor = Color(0xFFECFDF5),
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("report_kpi_today_words")
                        )
                    }

                    // 2. Today's Achievement Chart (Donut Chart + Metric Breakdown)
                    TodayAchievementDonutCard(
                        report = report,
                        animFraction = animFraction
                    )

                    // 3. 7-Day Weekly Bar Chart (Known vs Review + 10-word Daily Goal Line)
                    WeeklySevenDayBarChartCard(
                        report = report,
                        animFraction = animFraction
                    )

                    // 4. Today's Studied Words Scorecard List
                    TodayWordsScorecardSection(
                        todayWords = report.todayWordResults
                    )
                }

                // Bottom Action Footer
                Surface(
                    color = Color.White,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFFE8E0D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Tactile3DButton(
                            text = "확인 · 계속 학습하기",
                            containerColor = AdventureEmerald,
                            shadowColor = AdventureEmeraldShadow,
                            height = 48.dp,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "weekly_report_close_btn",
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportKpiTile(
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFF1B2834),
                maxLines = 1
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TodayAchievementDonutCard(
    report: WeeklyStudyReportData,
    animFraction: Float
) {
    val goalTarget = report.dailyGoalTarget.coerceAtLeast(10)
    val goalProgress = (report.todayKnownCount.toFloat() / goalTarget.toFloat()).coerceIn(0f, 1f)
    val knownCount = report.todayKnownCount
    val reviewCount = report.todayReviewCount
    val totalForDonut = (knownCount + reviewCount).coerceAtLeast(1)
    val knownSweep = if (knownCount + reviewCount == 0) 0f else (knownCount.toFloat() / totalForDonut) * 360f * animFraction
    val reviewSweep = if (knownCount + reviewCount == 0) 0f else (reviewCount.toFloat() / totalForDonut) * 360f * animFraction

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, Color(0xFFE4DCCB)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_achievement_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "오늘의 성취 차트",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF1B2834)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = AdventureEmerald.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "일일 목표 ${(goalProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = AdventureEmeraldShadow,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // GPU Donut Chart
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val strokePx = 15.dp.toPx()
                        val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
                        // Track circle
                        drawArc(
                            color = Color(0xFFEDE7DA),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = stroke
                        )
                        if (knownSweep > 0f) {
                            drawArc(
                                color = AdventureEmerald,
                                startAngle = -90f,
                                sweepAngle = knownSweep,
                                useCenter = false,
                                style = stroke
                            )
                        }
                        if (reviewSweep > 0f) {
                            drawArc(
                                color = CoralOrange,
                                startAngle = -90f + knownSweep,
                                sweepAngle = reviewSweep,
                                useCenter = false,
                                style = stroke
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${report.todayAccuracyPercent}%",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF1B2834)
                        )
                        Text(
                            text = "정답률",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Breakdown Bars
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    AchievementProgressRow(
                        label = "완벽 암기",
                        countText = "${knownCount}단어",
                        fraction = if (report.todayTotalAttempts == 0) 0f else (knownCount.toFloat() / report.todayTotalAttempts) * animFraction,
                        barColor = AdventureEmerald
                    )
                    AchievementProgressRow(
                        label = "다시 복습",
                        countText = "${reviewCount}단어",
                        fraction = if (report.todayTotalAttempts == 0) 0f else (reviewCount.toFloat() / report.todayTotalAttempts) * animFraction,
                        barColor = CoralOrange
                    )
                    AchievementProgressRow(
                        label = "10단어 목표",
                        countText = "${minOf(knownCount, goalTarget)}/${goalTarget}",
                        fraction = goalProgress * animFraction,
                        barColor = TrophyGoldShadow
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementProgressRow(
    label: String,
    countText: String,
    fraction: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1B2834)
                )
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = barColor,
            trackColor = Color(0xFFF1ECE1),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun WeeklySevenDayBarChartCard(
    report: WeeklyStudyReportData,
    animFraction: Float
) {
    val metrics = report.dailyMetrics
    val maxBarValue = maxOf(10, (metrics.maxOfOrNull { it.totalAttempts } ?: 0) + 2)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, Color(0xFFE4DCCB)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_bar_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "최근 7일 주간 학습 차트",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF1B2834)
                    )
                    Text(
                        text = "주간 학습일 ${report.activeDaysCount}/7일 · 평균 정답률 ${report.weeklyAccuracyPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = AdventureEmerald, text = "암기")
                    LegendDot(color = CoralOrange, text = "복습")
                }
            }

            // Stacked Bar Chart Canvas with 10-word Daily Goal dashed line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .padding(top = 6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = metrics.size.coerceAtLeast(1)
                    val topPad = 14.dp.toPx()
                    val bottomPad = 6.dp.toPx()
                    val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                    // 10-word Goal Reference Line
                    val goalY = topPad + chartHeight * (1f - (10f / maxBarValue.toFloat()).coerceIn(0f, 1f))
                    drawLine(
                        color = TrophyGoldShadow.copy(alpha = 0.65f),
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )

                    // Baseline
                    val baseY = topPad + chartHeight
                    drawLine(
                        color = Color(0xFFE4DCCB),
                        start = Offset(0f, baseY),
                        end = Offset(size.width, baseY),
                        strokeWidth = 1.dp.toPx()
                    )

                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.46f).coerceIn(14.dp.toPx(), 28.dp.toPx())
                    val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                    metrics.forEachIndexed { idx, day ->
                        val centerX = slotWidth * idx + slotWidth / 2f
                        val left = centerX - barWidth / 2f

                        // Background slot pill
                        drawRoundRect(
                            color = if (day.isToday) Color(0xFFFFF4DC) else Color(0xFFF6F2E9),
                            topLeft = Offset(left, topPad),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = radius
                        )

                        val totalH = (day.totalAttempts.toFloat() / maxBarValue.toFloat()).coerceIn(0f, 1f) * chartHeight * animFraction
                        val knownH = (day.knownCount.toFloat() / maxBarValue.toFloat()).coerceIn(0f, 1f) * chartHeight * animFraction

                        if (totalH > 1f) {
                            // Review cap + total bar
                            if (day.reviewCount > 0) {
                                drawRoundRect(
                                    color = CoralOrange,
                                    topLeft = Offset(left, baseY - totalH),
                                    size = Size(barWidth, totalH),
                                    cornerRadius = radius
                                )
                            }
                            if (knownH > 1f) {
                                drawRoundRect(
                                    color = if (day.isToday) AdventureEmerald else AdventureEmerald.copy(alpha = 0.78f),
                                    topLeft = Offset(left, baseY - knownH),
                                    size = Size(barWidth, knownH),
                                    cornerRadius = radius
                                )
                            }
                        }
                    }
                }

                // Goal Line Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFF8E6),
                    border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.6f)),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "점선: 10단어 목표",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = TrophyGoldShadow,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Day labels + word count row aligned with bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                metrics.forEach { day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${day.knownCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            ),
                            color = if (day.isToday) AdventureEmeraldShadow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (day.isToday) CoralOrange else Color.Transparent
                        ) {
                            Text(
                                text = if (day.isToday) "오늘" else day.dayLabelKo,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = if (day.isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayWordsScorecardSection(
    todayWords: List<TodayWordResultEntry>
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, Color(0xFFE4DCCB)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_words_scorecard_section")
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
                Text(
                    text = "오늘 학습한 단어 성적표",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF1B2834)
                )
                Text(
                    text = "총 ${todayWords.size}건",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SkyQuestBlueShadow
                )
            }

            if (todayWords.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF6F2E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "아직 오늘 기록된 단어가 없어요!\n메인 화면의 '집중 단어 학습 모드'에서 첫 단어를 학습하면 성적과 차트가 실시간으로 채워집니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                todayWords.take(20).forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (entry.known) Color(0xFFF0FDF8) else Color(0xFFFFF4EE),
                        border = BorderStroke(
                            1.dp,
                            if (entry.known) AdventureEmerald.copy(alpha = 0.28f) else CoralOrange.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.word,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFF1B2834),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = entry.meaningKo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (entry.known) AdventureEmerald else CoralOrange
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (entry.known) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (entry.known) "완벽 암기" else "다시 복습",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White
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
