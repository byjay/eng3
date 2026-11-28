package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.AppThemeMode
import com.example.engine.LearnerGoalTrack
import com.example.engine.LeagueStanding
import com.example.engine.PlayGrowthRetentionEngine
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.TrophyGold

/**
 * Above-the-fold Top-Chart EdTech Primary Action Card:
 * - Shows Weekly League Tier + Streak Freeze Shield status (with 1-tap coin shield recharge)
 * - Provides 1-tap Theme switcher (System / Dark / Light)
 * - Unmistakable single primary CTA ("오늘의 3분 챌린지 바로 시작") + Viral Share Card button
 */
@Composable
fun PlayStoreHeroQuickStartCard(
    learnerName: String,
    goalTrack: LearnerGoalTrack,
    leagueStanding: LeagueStanding,
    streakFreezes: Int,
    todayWordsCompleted: Int,
    dailyGoal: Int,
    themeMode: AppThemeMode,
    onStartDailyChallenge: () -> Unit,
    onShareAchievementCard: () -> Unit,
    onBuyStreakFreeze: () -> Unit,
    onCycleThemeMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leagueColor = Color(leagueStanding.currentTier.accentHex)
    val safeGoal = dailyGoal.coerceAtLeast(5)
    val goalFraction = (todayWordsCompleted.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val themeIcon = when (themeMode) {
        AppThemeMode.SYSTEM -> Icons.Rounded.Brightness4
        AppThemeMode.DARK -> Icons.Rounded.DarkMode
        AppThemeMode.LIGHT -> Icons.Rounded.LightMode
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, leagueColor.copy(alpha = 0.45f)),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("play_growth_hero_card")
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            leagueColor.copy(alpha = 0.11f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: League Tier Pill + Streak Freeze Shield + Theme Toggle
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
                        color = leagueColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = leagueStanding.currentTier.titleKo,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }

                    // Streak Freeze Shield Pill (tappable to equip up to 3 shields)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = GlacierCyan.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, GlacierCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onBuyStreakFreeze)
                            .testTag("home_streak_freeze_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = "스트릭 보호막",
                                tint = GlacierCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "보호막 ${streakFreezes}/${PlayGrowthRetentionEngine.MAX_STREAK_FREEZES}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 1-Tap Dark / Light / System Theme Switcher
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onCycleThemeMode)
                        .testTag("home_theme_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "화면 테마 변경",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = themeMode.labelKo,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Middle: Personalized Goal Title + Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${learnerName}의 ${goalTrack.badgeKo} · 오늘 $todayWordsCompleted/$safeGoal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (leagueStanding.nextTier != null) {
                            "다음 승급 -${leagueStanding.pointsRemainingToNext}pt"
                        } else {
                            "최고 리그 달성"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = leagueColor
                    )
                }

                LinearProgressIndicator(
                    progress = { goalFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (goalFraction >= 1f) AdventureEmerald else StudioCobalt,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            // Bottom Row: Unmistakable Primary 3-Minute CTA + Viral PNG Share Card CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartDailyChallenge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioCobalt,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.45f)
                        .heightIn(min = 50.dp)
                        .testTag("home_instant_3min_cta")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (todayWordsCompleted >= safeGoal) "보너스 스피드 퀴즈" else "3분 챌린지 바로 시작",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                    )
                }

                OutlinedButton(
                    onClick = onShareAchievementCard,
                    border = BorderStroke(1.3.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .heightIn(min = 50.dp)
                        .testTag("home_share_card_cta")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "성적표 카드 공유",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "카드 공유",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * Play-Policy compliant Peak-Happiness Review & Feedback Prompt:
 * - Never gates rewards or forces 5-star ratings.
 * - Separates happy learners (Google Play Store review) from users with suggestions (quick feedback).
 */
@Composable
fun PlayReviewPromptDialog(
    learnerName: String,
    streakCount: Int,
    onOpenPlayStoreReview: () -> Unit,
    onSendFeedback: (String) -> Unit,
    onSnooze: () -> Unit,
    onDismissPermanently: () -> Unit
) {
    var showFeedbackComposer by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onSnooze,
        icon = {
            Surface(
                shape = CircleShape,
                color = TrophyGold.copy(alpha = 0.16f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = TrophyGold,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = if (showFeedbackComposer) "더 좋은 앱을 위한 의견" else "${learnerName}님, 학습이 즐거우셨나요?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!showFeedbackComposer) {
                    Text(
                        text = if (streakCount > 0) {
                            "벌써 ${streakCount}회 연속 스트릭을 달성하셨어요! 짧은 응원의 리뷰가 더 많은 학습자에게 큰 힘이 됩니다."
                        } else {
                            "오늘 목표 달성을 축하해요! 사용해보신 소감을 들려주시면 앱을 더 빠르게 발전시킬 수 있어요."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onOpenPlayStoreReview,
                        colors = ButtonDefaults.buttonColors(containerColor = AdventureEmerald),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("play_review_open_store_btn")
                    ) {
                        Text("Google Play 리뷰 남기기", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showFeedbackComposer = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("play_review_feedback_btn")
                    ) {
                        Text("아쉬운 점 의견 보내기", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text(
                        text = "불편했거나 추가되었으면 하는 기능을 적어주시면 다음 업데이트에 최우선 반영하겠습니다.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { if (it.length <= 400) feedbackText = it },
                        minLines = 3,
                        maxLines = 5,
                        placeholder = { Text("예: 발음 속도 조절 버튼이 더 컸으면 좋겠어요") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("play_feedback_text_input")
                    )
                }
            }
        },
        confirmButton = {
            if (showFeedbackComposer) {
                Button(
                    onClick = { onSendFeedback(feedbackText) },
                    enabled = feedbackText.isNotBlank(),
                    modifier = Modifier.testTag("play_feedback_submit_btn")
                ) {
                    Text("의견 전달")
                }
            } else {
                TextButton(
                    onClick = onSnooze,
                    modifier = Modifier.testTag("play_review_snooze_btn")
                ) {
                    Text("나중에")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissPermanently,
                modifier = Modifier.testTag("play_review_never_btn")
            ) {
                Text("다시 보지 않기")
            }
        }
    )
}
