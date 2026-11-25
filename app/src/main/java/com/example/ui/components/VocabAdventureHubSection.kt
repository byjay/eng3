package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalAirport
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.SquirrelRpgRepository
import com.example.engine.AchievementBadge
import com.example.engine.AchievementBadgeEngine
import com.example.engine.ExplorerGearItem
import com.example.engine.ExplorerJourneyEngine
import com.example.engine.JourneyBiomeNode
import com.example.engine.LearningMission
import com.example.engine.LevelConfig
import com.example.engine.MemoryPalaceEntry
import com.example.engine.MemoryPalaceRoom
import com.example.engine.MissionCadence
import com.example.engine.MissionStatus
import com.example.engine.QuestCelebrationEvent
import com.example.engine.RewardTransaction
import com.example.engine.Sm2ReviewScheduler
import com.example.engine.WordLearningProgress
import com.example.model.SquirrelJobHero
import com.example.model.UserProgress
import com.example.model.VocabWord
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold
import kotlin.math.cos
import kotlin.math.sin

private val MidnightDeep = Color(0xFF0E132B)
private val MidnightIndigo = Color(0xFF1A2148)
private val MidnightViolet = Color(0xFF2B235A)
private val MidnightCardSurface = Color(0xFF232852)

/**
 * Adventurer RPG Vocab Hub (Reference Image 1 + v2 Master Spec P3–P6):
 * - Cosmic Midnight-Indigo Adventurer Banner with Squirrel Hero, Lv/XP bar, Shield & Coins
 * - 4-Stat SM-2 Review Queue Strip (지금 복습 · 오늘 예정 · 기한 초과 · 취약 단어)
 * - Daily & Weekly Quest Drawer with colorful gradient quest cards + claim separation
 * - "나의 여정" Sheet (Explorer Gear Grid + 3-Biome Adventure Map) & 2D Memory Palace Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabAdventureHubSection(
    progress: UserProgress,
    wordSm2Progress: Map<String, WordLearningProgress>,
    dailyMissions: List<LearningMission>,
    memoryPalaceEntries: Map<String, MemoryPalaceEntry>,
    allWordsPool: List<VocabWord>,
    isDueReviewActive: Boolean,
    onToggleDueReview: () -> Unit,
    onClaimMission: (String) -> Unit,
    onOpenEnGameHub: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onSaveMemoryPalace: (wordId: String, room: MemoryPalaceRoom, memoKo: String) -> Unit,
    modifier: Modifier = Modifier,
    hero: SquirrelJobHero = SquirrelRpgRepository.findHeroById(""),
    playerNickname: String = "모험가",
    weakWordIds: Set<String> = emptySet(),
    masteredWordsCount: Int = 0,
    rewardLedger: List<RewardTransaction> = emptyList()
) {
    val tier = remember(progress.totalXp) { LevelConfig.resolveTier(progress.totalXp) }
    val levelFraction = remember(progress.totalXp) { LevelConfig.progressToNext(progress.totalXp) }
    val queueSnap = remember(wordSm2Progress, weakWordIds) {
        Sm2ReviewScheduler.queueSnapshot(wordSm2Progress, weakWordIds)
    }
    val claimableCount = remember(dailyMissions) { dailyMissions.count { it.status == MissionStatus.COMPLETED } }
    val claimedCount = remember(dailyMissions) { dailyMissions.count { it.status == MissionStatus.CLAIMED } }
    var missionsExpanded by remember(claimableCount) { mutableStateOf(claimableCount > 0) }
    var selectedCadence by remember { mutableStateOf(MissionCadence.DAILY) }
    var showPalaceSheet by remember { mutableStateOf(false) }
    var showJourneySheet by remember { mutableStateOf(false) }

    if (showPalaceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPalaceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MemoryPalaceSheetContent(
                entries = memoryPalaceEntries,
                allWordsPool = allWordsPool,
                onSpeakEnglish = onSpeakEnglish,
                onSaveMemoryPalace = onSaveMemoryPalace,
                onClose = { showPalaceSheet = false }
            )
        }
    }

    if (showJourneySheet) {
        ModalBottomSheet(
            onDismissRequest = { showJourneySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MidnightDeep
        ) {
            YourJourneySheetContent(
                progress = progress,
                hero = hero,
                playerNickname = playerNickname,
                masteredWordsCount = masteredWordsCount,
                palaceCount = memoryPalaceEntries.size,
                claimedMissionsCount = claimedCount,
                sm2ReviewCount = wordSm2Progress.values.sumOf { it.correctCount + it.wrongCount },
                rewardLedger = rewardLedger,
                onOpenEnGameHub = {
                    showJourneySheet = false
                    onOpenEnGameHub()
                },
                onClose = { showJourneySheet = false }
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MidnightDeep,
        border = BorderStroke(1.4.dp, Color(0xFF4F46E5).copy(alpha = 0.48f)),
        shadowElevation = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("vocab_adventure_hub_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(MidnightDeep, MidnightIndigo, MidnightViolet)
                    )
                )
        ) {
            // Subtle starry cosmic sparkle canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val starOffsets = listOf(
                    0.12f to 0.14f, 0.34f to 0.08f, 0.58f to 0.18f,
                    0.82f to 0.10f, 0.91f to 0.32f, 0.22f to 0.42f, 0.72f to 0.28f
                )
                starOffsets.forEachIndexed { idx, (fx, fy) ->
                    drawCircle(
                        color = if (idx % 2 == 0) TrophyGold.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.28f),
                        radius = if (idx % 3 == 0) 2.4.dp.toPx() else 1.5.dp.toPx(),
                        center = Offset(size.width * fx, size.height * fy)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Adventurer Greeting + Squirrel Hero Avatar + Level Shield Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "준비됐나요, ${playerNickname} 모험가님?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "오늘도 전설의 단어로 채워 보세요 · 연속 ${progress.streakCount}일",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC7D2FE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    SquirrelHeroAvatar(
                        hero = hero,
                        size = 54.dp,
                        mood = if (claimableCount > 0) SquirrelMoodState.HAPPY_CHEER else SquirrelMoodState.IDLE,
                        borderWidth = 2.2.dp,
                        borderColor = TrophyGold
                    )
                }

                // Level Progress & Shield Card (matching Reference Image 1 Left)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MidnightCardSurface.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    Text(
                                        text = "LVL ${tier.level}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = TrophyGold
                                    )
                                    Text(
                                        text = tier.titleKo,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MonetizationOn,
                                        contentDescription = "보유 코인",
                                        tint = TrophyGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${progress.coins}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = TrophyGold
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { levelFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = TrophyGold,
                                trackColor = Color.White.copy(alpha = 0.14f),
                                strokeCap = StrokeCap.Round
                            )
                            Text(
                                text = "${progress.totalXp} / ${tier.nextLevelXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        // Golden Explorer Crest Shield
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TrophyGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.2.dp, TrophyGold.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showJourneySheet = true }
                                .testTag("vocab_hub_shield_journey_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = "나의 여정 열기",
                                    tint = TrophyGold,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                    }
                }

                // 4-Stat SM-2 Review Queue Strip (Spec Section 9.4)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vocab_sm2_queue_snapshot")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QueueMiniStat("지금 복습", "${queueSnap.dueNowCount}", StudioCoral, Modifier.weight(1f))
                        QueueMiniStat("오늘 예정", "${queueSnap.todayScheduledCount}", SkyQuestBlue, Modifier.weight(1f))
                        QueueMiniStat("기한 초과", "${queueSnap.overdueCount}", TrophyGold, Modifier.weight(1f))
                        QueueMiniStat("취약 단어", "${queueSnap.weakCount}", Color(0xFFF472B6), Modifier.weight(1f))

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isDueReviewActive) StudioCoral else if (queueSnap.dueNowCount > 0) StudioCoral.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(enabled = queueSnap.dueNowCount > 0 || isDueReviewActive) { onToggleDueReview() }
                                .testTag("vocab_due_review_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = "오늘 복습 큐",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isDueReviewActive) "복습 중" else "시작",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 4-Action Quick Dock (퀘스트 | 나의 여정 | 기억 궁전 | 8종 게임)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CosmicActionChip(
                        icon = Icons.Rounded.Flag,
                        label = if (claimableCount > 0) "퀘스트 $claimableCount!" else "퀘스트",
                        trailingIcon = if (missionsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        accent = if (claimableCount > 0) AdventureEmerald else Color(0xFFA78BFA),
                        highlighted = claimableCount > 0 || missionsExpanded,
                        tag = "vocab_hub_missions_toggle",
                        modifier = Modifier.weight(1.1f),
                        onClick = { missionsExpanded = !missionsExpanded }
                    )
                    CosmicActionChip(
                        icon = Icons.Rounded.Explore,
                        label = "여정",
                        accent = TrophyGold,
                        highlighted = false,
                        tag = "vocab_hub_journey_btn",
                        modifier = Modifier.weight(0.85f),
                        onClick = { showJourneySheet = true }
                    )
                    CosmicActionChip(
                        icon = Icons.Rounded.Map,
                        label = "궁전 ${memoryPalaceEntries.size}",
                        accent = Color(0xFF38BDF8),
                        highlighted = false,
                        tag = "vocab_hub_palace_btn",
                        modifier = Modifier.weight(0.95f),
                        onClick = { showPalaceSheet = true }
                    )
                    CosmicActionChip(
                        icon = Icons.Rounded.SportsEsports,
                        label = "게임 8종",
                        accent = CoralOrange,
                        highlighted = true,
                        tag = "vocab_hub_engame_btn",
                        modifier = Modifier.weight(1.0f),
                        onClick = onOpenEnGameHub
                    )
                }

                // Expandable Daily & Weekly Quests (vibrant gradient quest cards from Reference Image 1)
                AnimatedVisibility(visible = missionsExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .testTag("vocab_daily_missions_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MissionCadence.entries.forEach { cadence ->
                                val selected = selectedCadence == cadence
                                val cadenceClaimable = dailyMissions.count {
                                    it.cadence == cadence && it.status == MissionStatus.COMPLETED
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedCadence = cadence }
                                        .testTag("mission_cadence_${cadence.name}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = if (cadenceClaimable > 0) "${cadence.labelKo} ($cadenceClaimable)" else cadence.labelKo,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = if (selected) Color.White else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }

                        val filtered = remember(dailyMissions, selectedCadence) {
                            dailyMissions.filter { it.cadence == selectedCadence }
                        }
                        filtered.forEachIndexed { idx, mission ->
                            AdventurerGradientQuestRow(
                                mission = mission,
                                colorIndex = idx,
                                onClaim = { onClaimMission(mission.missionId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueMiniStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = Color(0xFFCBD5E1),
            maxLines = 1
        )
    }
}

@Composable
private fun CosmicActionChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    highlighted: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = if (highlighted) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, if (highlighted) accent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.16f)),
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun AdventurerGradientQuestRow(
    mission: LearningMission,
    colorIndex: Int,
    onClaim: () -> Unit
) {
    val isClaimable = mission.status == MissionStatus.COMPLETED
    val isClaimed = mission.status == MissionStatus.CLAIMED
    val gradientStops = when (colorIndex % 4) {
        0 -> listOf(Color(0xFF5B21B6), Color(0xFF7C3AED)) // Royal Purple
        1 -> listOf(Color(0xFF1D4ED8), Color(0xFF2563EB)) // Cobalt Blue
        2 -> listOf(Color(0xFF0F766E), Color(0xFF0D9488)) // Ocean Teal
        else -> listOf(Color(0xFFB45309), Color(0xFFD97706)) // Quest Amber
    }

    Surface(
        shape = RoundedCornerShape(15.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isClaimable) 1.8.dp else 1.dp,
            color = if (isClaimable) TrophyGold else Color.White.copy(alpha = 0.18f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mission_row_${mission.missionId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        if (isClaimed) gradientStops.map { it.copy(alpha = 0.42f) } else gradientStops
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mission.titleKo,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { mission.progressFraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = TrophyGold,
                        trackColor = Color.White.copy(alpha = 0.22f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${mission.currentCount}/${mission.targetCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                }
                Text(
                    text = "+${mission.rewardXp} XP · +${mission.rewardCoins} 코인",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFEF08A)
                )
            }

            when {
                isClaimable -> {
                    Button(
                        onClick = onClaim,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrophyGold,
                            contentColor = Color(0xFF1E1B4B)
                        ),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .testTag("claim_mission_${mission.missionId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Redeem,
                            contentDescription = "보상 수령",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "수령",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
                isClaimed -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AdventureEmerald.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "수령 완료",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "완료",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }
                }
                else -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "진행 중",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quest Complete Celebration Dialog (Reference Image 1 Center Screen):
 * Starry midnight sky + animated Canvas confetti + Golden Crest + Huge +XP + Squirrel Hero + Glowing CTA
 */
@Composable
fun QuestCompleteCelebrationDialog(
    event: QuestCelebrationEvent,
    hero: SquirrelJobHero,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quest_confetti")
    val confettiPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_phase"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MidnightDeep,
            border = BorderStroke(2.dp, TrophyGold.copy(alpha = 0.72f)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .testTag("quest_complete_celebration_dialog")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0B1026), Color(0xFF1E1B4B), Color(0xFF312E81))
                        )
                    )
            ) {
                // Live Animated Confetti & Starlight Canvas
                Canvas(modifier = Modifier.matchParentSize()) {
                    val palette = listOf(TrophyGold, CoralOrange, SkyQuestBlue, AdventureEmerald, Color(0xFFC084FC))
                    for (i in 0 until 26) {
                        val seedX = ((i * 37) % 100) / 100f
                        val seedY = (((i * 53) % 100) / 100f + confettiPhase) % 1f
                        val sway = sin((confettiPhase * 6.28f + i).toDouble()).toFloat() * 14f
                        drawCircle(
                            color = palette[i % palette.size].copy(alpha = 0.78f),
                            radius = (if (i % 3 == 0) 4.5f else 2.8f).dp.toPx(),
                            center = Offset(size.width * seedX + sway, size.height * seedY)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Golden Crest Ribbon
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = TrophyGold.copy(alpha = 0.22f),
                        border = BorderStroke(1.5.dp, TrophyGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = TrophyGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "QUEST COMPLETE!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = event.titleKo,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE0E7FF),
                        textAlign = TextAlign.Center
                    )

                    // Huge Golden +XP Headline
                    Text(
                        text = "+${event.xpEarned} XP",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = TrophyGold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (event.coinsEarned > 0) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "+${event.coinsEarned} 코인",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = TrophyGold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CoralOrange.copy(alpha = 0.24f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = CoralOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "연속 ${event.streakCount}일 달성!",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (!event.leveledUpTitleKo.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AdventureEmerald.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, AdventureEmerald)
                        ) {
                            Text(
                                text = "승급 완료: ${event.leveledUpTitleKo}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    SquirrelHeroAvatar(
                        hero = hero,
                        size = 94.dp,
                        mood = SquirrelMoodState.VICTORY_DANCE,
                        borderWidth = 3.dp,
                        borderColor = TrophyGold
                    )

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("quest_celebration_awesome_btn")
                    ) {
                        Text(
                            text = "멋져요! 계속 모험하기",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Your Journey Sheet (Reference Image 1 Right Screen):
 * Level Overview + 4-Item Explorer Gear Grid + 3-Biome Adventure Map with Glowing Trail
 */
@Composable
private fun YourJourneySheetContent(
    progress: UserProgress,
    hero: SquirrelJobHero,
    playerNickname: String,
    masteredWordsCount: Int,
    palaceCount: Int,
    claimedMissionsCount: Int,
    sm2ReviewCount: Int,
    rewardLedger: List<RewardTransaction>,
    onOpenEnGameHub: () -> Unit,
    onClose: () -> Unit
) {
    val tier = remember(progress.totalXp) { LevelConfig.resolveTier(progress.totalXp) }
    val levelFraction = remember(progress.totalXp) { LevelConfig.progressToNext(progress.totalXp) }
    val gearItems = remember(progress.totalXp, masteredWordsCount, progress.streakCount, palaceCount, claimedMissionsCount) {
        ExplorerJourneyEngine.resolveGearItems(
            totalXp = progress.totalXp,
            masteredCount = masteredWordsCount,
            streakCount = progress.streakCount,
            palaceCount = palaceCount,
            claimedMissionsCount = claimedMissionsCount
        )
    }
    val badges = remember(masteredWordsCount, progress.streakCount, palaceCount, claimedMissionsCount, progress.totalXp, sm2ReviewCount) {
        AchievementBadgeEngine.resolveBadges(
            masteredCount = masteredWordsCount,
            streakCount = progress.streakCount,
            palaceCount = palaceCount,
            claimedMissionsCount = claimedMissionsCount,
            totalXp = progress.totalXp,
            sm2ReviewCount = sm2ReviewCount
        )
    }
    val biomes = remember(progress.totalXp) {
        ExplorerJourneyEngine.resolveJourneyBiomes(progress.totalXp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .testTag("your_journey_sheet"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "나의 어휘 여정",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
            TextButton(onClick = onClose) {
                Text("닫기", color = TrophyGold)
            }
        }

        // Hero & Level Banner Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MidnightCardSurface,
            border = BorderStroke(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF312E81), Color(0xFF1E3A8A)))
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SquirrelHeroAvatar(
                    hero = hero,
                    size = 64.dp,
                    mood = SquirrelMoodState.HAPPY_CHEER,
                    borderWidth = 2.5.dp,
                    borderColor = TrophyGold
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Level ${tier.level} · ${tier.titleKo}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Text(
                        text = "$playerNickname · 암기 ${masteredWordsCount}단어 · 궁전 ${palaceCount}개",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC7D2FE)
                    )
                    LinearProgressIndicator(
                        progress = { levelFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = TrophyGold,
                        trackColor = Color.White.copy(alpha = 0.16f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${progress.totalXp} / ${tier.nextLevelXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TrophyGold
                    )
                }
            }
        }

        // Explorer Gear Grid (4 Collectible Gear Cards)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "모험가 장비 (Gear)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Text(
                    text = "${gearItems.count { it.unlocked }} / ${gearItems.size} 해금",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TrophyGold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gearItems.forEach { gear ->
                    ExplorerGearCard(gear = gear, modifier = Modifier.weight(1f))
                }
            }
        }

        // 3-Biome Adventure Journey Map (설산 -> 피라미드 사막 -> 어둠의 보스 성채)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "탐험 여정 지도 (Journey Map)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MidnightCardSurface,
                border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.img_world_map_hero_1795412460420),
                        contentDescription = "3개 바이옴 모험 지도",
                        contentScale = ContentScale.Crop,
                        alpha = 0.32f,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Glowing Dotted Trail Path across 3 Biomes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val y = size.height * 0.5f
                                val p1 = Offset(size.width * 0.16f, y + 8.dp.toPx())
                                val p2 = Offset(size.width * 0.50f, y - 10.dp.toPx())
                                val p3 = Offset(size.width * 0.84f, y + 6.dp.toPx())
                                val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                drawLine(
                                    color = TrophyGold.copy(alpha = 0.85f),
                                    start = p1,
                                    end = p2,
                                    strokeWidth = 3.dp.toPx(),
                                    pathEffect = dash,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color(0xFFA78BFA).copy(alpha = 0.85f),
                                    start = p2,
                                    end = p3,
                                    strokeWidth = 3.dp.toPx(),
                                    pathEffect = dash,
                                    cap = StrokeCap.Round
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                biomes.forEach { biome ->
                                    val accent = Color(biome.accentHex)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (biome.cleared || biome.isCurrent) accent else Color(0xFF334155),
                                        border = BorderStroke(
                                            width = if (biome.isCurrent) 2.5.dp else 1.2.dp,
                                            color = if (biome.isCurrent) TrophyGold else Color.White.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when {
                                                    biome.cleared -> Icons.Rounded.CheckCircle
                                                    biome.biomeId == "biome_frost" -> Icons.Rounded.Terrain
                                                    biome.biomeId == "biome_desert" -> Icons.Rounded.Whatshot
                                                    else -> Icons.Rounded.Shield
                                                },
                                                contentDescription = biome.titleKo,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        biomes.forEach { biome ->
                            JourneyBiomeRow(biome = biome)
                        }

                        Button(
                            onClick = onOpenEnGameHub,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralOrange,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "8종 미니게임으로 다음 바이옴 개척하기",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }
        }

        // 6 Study Achievement Badges (Spec Section 10.1)
        Column(
            modifier = Modifier.testTag("your_journey_badges_section"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "학습 업적 배지 (Badges)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Text(
                    text = "${badges.count { it.unlocked }} / ${badges.size} 달성",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TrophyGold
                )
            }
            badges.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { badge ->
                        val accent = Color(badge.accentHex)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (badge.unlocked) accent.copy(alpha = 0.22f) else MidnightCardSurface,
                            border = BorderStroke(
                                1.1.dp,
                                if (badge.unlocked) accent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (badge.unlocked) accent else Color.White.copy(alpha = 0.10f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (badge.unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                                            contentDescription = badge.titleKo,
                                            tint = if (badge.unlocked) Color.White else Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = badge.titleKo,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = badge.criteriaKo,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (badge.unlocked) "획득 완료" else badge.progressLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = if (badge.unlocked) TrophyGold else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reward Ledger History + Non-Cash Policy Notice (Spec Section 6.4)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MidnightCardSurface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("your_journey_reward_ledger_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "보상 원장 (최근 내역)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Text(
                        text = "총 ${rewardLedger.size}건 기록",
                        style = MaterialTheme.typography.labelSmall,
                        color = TrophyGold
                    )
                }
                if (rewardLedger.isEmpty()) {
                    Text(
                        text = "단어를 학습하거나 퀘스트를 완료하면 중복 방지 원장에 자동 기록됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                } else {
                    rewardLedger.take(6).forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tx.reason.ifBlank { tx.eventType.name },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "+${tx.xpDelta}XP · +${tx.coinDelta}C",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = TrophyGold
                            )
                        }
                    }
                }
                Text(
                    text = "※ XP와 코인은 앱 내 학습 보상이며 현금 가치가 없습니다.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = Color(0xFF94A3B8)
                )
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun ExplorerGearCard(
    gear: ExplorerGearItem,
    modifier: Modifier = Modifier
) {
    val accent = Color(gear.accentHex)
    val icon = when (gear.id) {
        "gear_hood" -> Icons.Rounded.Shield
        "gear_satchel" -> Icons.Rounded.Backpack
        "gear_lantern" -> Icons.Rounded.Lightbulb
        else -> Icons.Rounded.Explore
    }
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = if (gear.unlocked) accent.copy(alpha = 0.22f) else MidnightCardSurface,
        border = BorderStroke(
            1.2.dp,
            if (gear.unlocked) accent.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.14f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (gear.unlocked) accent else Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (gear.unlocked) icon else Icons.Rounded.Lock,
                        contentDescription = gear.titleKo,
                        tint = if (gear.unlocked) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = gear.titleKo,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (gear.unlocked) "장착됨" else gear.progressText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                color = if (gear.unlocked) TrophyGold else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun JourneyBiomeRow(biome: JourneyBiomeNode) {
    val accent = Color(biome.accentHex)
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = if (biome.isCurrent) accent.copy(alpha = 0.24f) else Color(0xFF0F172A).copy(alpha = 0.65f),
        border = BorderStroke(
            1.dp,
            if (biome.isCurrent) TrophyGold else accent.copy(alpha = 0.35f)
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
                    text = biome.titleKo,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Text(
                    text = biome.subtitleKo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = when {
                    biome.cleared -> AdventureEmerald
                    biome.isCurrent -> TrophyGold
                    else -> Color.White.copy(alpha = 0.14f)
                }
            ) {
                Text(
                    text = when {
                        biome.cleared -> "정복"
                        biome.isCurrent -> "탐험 중"
                        else -> "${biome.requiredXp} XP"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (biome.isCurrent) Color(0xFF1E1B4B) else Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun MemoryPalaceSheetContent(
    entries: Map<String, MemoryPalaceEntry>,
    allWordsPool: List<VocabWord>,
    onSpeakEnglish: (String) -> Unit,
    onSaveMemoryPalace: (wordId: String, room: MemoryPalaceRoom, memoKo: String) -> Unit,
    onClose: () -> Unit
) {
    var selectedRoom by remember { mutableStateOf(MemoryPalaceRoom.HOME_ROOM) }
    val revealedMap = remember(selectedRoom) { mutableStateMapOf<String, Boolean>() }
    val wordLookup = remember(allWordsPool) { allWordsPool.associateBy { it.id } }
    val roomItems = remember(entries, selectedRoom) {
        entries.values.filter { it.room == selectedRoom }.sortedByDescending { it.updatedAtMs }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "2D 기억의 궁전",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "공간에 배치한 단어를 떠올리며 회상해 보세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClose) {
                Text("닫기")
            }
        }

        // 4 Room Cards (2x2 Grid)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MemoryPalaceRoom.entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { room ->
                        val accent = Color(room.accentHex)
                        val count = entries.values.count { it.room == room }
                        val selected = selectedRoom == room
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(if (selected) 1.6.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedRoom = room }
                                .testTag("palace_room_${room.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = accent.copy(alpha = 0.18f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = roomIcon(room),
                                            contentDescription = room.titleKo,
                                            tint = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = room.titleKo.substringBefore(" ·"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${count}단어 배치",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(selectedRoom.accentHex).copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${selectedRoom.titleKo} — ${selectedRoom.subtitleKo}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(selectedRoom.accentHex),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }

        if (roomItems.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = StudioIris,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "아직 이 공간에 배치된 단어가 없어요",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "단어 카드의 '궁전 배치' 버튼을 눌러 나만의 연상 메모와 함께 저장해 보세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            roomItems.forEach { entry ->
                val word = wordLookup[entry.wordId]
                val revealed = revealedMap[entry.wordId] == true
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(selectedRoom.accentHex).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { revealedMap[entry.wordId] = !revealed }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = word?.word ?: entry.wordId,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            if (entry.personalMemoKo.isNotBlank()) {
                                Text(
                                    text = "내 메모: ${entry.personalMemoKo}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StudioIris
                                )
                            }
                            if (revealed) {
                                Text(
                                    text = word?.meaningKo ?: "뜻 확인됨",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AdventureEmerald
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = "뜻 떠올리기",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "탭해서 뜻 회상 확인",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (word != null) {
                            StitchIconButton(
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "발음 듣기",
                                accent = StudioCobalt,
                                size = 40.dp,
                                iconSize = 19.dp,
                                onClick = { onSpeakEnglish(word.word) }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryPalaceAssignDialog(
    word: VocabWord,
    existingEntry: MemoryPalaceEntry?,
    onDismiss: () -> Unit,
    onSave: (MemoryPalaceRoom, String) -> Unit
) {
    var chosenRoom by remember(word.id) { mutableStateOf(existingEntry?.room ?: MemoryPalaceRoom.SCHOOL_LIBRARY) }
    var memoText by remember(word.id) { mutableStateOf(existingEntry?.personalMemoKo.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "'${word.word}' 기억의 궁전 배치",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "이 단어가 잘 떠오르는 공간을 고르고 한 줄 연상 메모를 남겨 보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MemoryPalaceRoom.entries.forEach { room ->
                        val accent = Color(room.accentHex)
                        val selected = chosenRoom == room
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { chosenRoom = room }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = roomIcon(room),
                                    contentDescription = room.titleKo,
                                    tint = if (selected) Color.White else accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = room.titleKo.substringBefore(" ·"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it.take(120) },
                    label = { Text("나만의 연상 메모 (선택)") },
                    placeholder = { Text("예: 책상 위 무거운 가방을 내려놓는 장면") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_palace_memo_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(chosenRoom, memoText)
                    onDismiss()
                },
                modifier = Modifier.testTag("memory_palace_save_btn")
            ) {
                Text("공간에 배치")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private fun roomIcon(room: MemoryPalaceRoom): ImageVector = when (room) {
    MemoryPalaceRoom.HOME_ROOM -> Icons.Rounded.Home
    MemoryPalaceRoom.SCHOOL_LIBRARY -> Icons.Rounded.School
    MemoryPalaceRoom.TRAVEL_AIRPORT -> Icons.Rounded.LocalAirport
    MemoryPalaceRoom.NATURE_FOREST -> Icons.Rounded.Landscape
}
