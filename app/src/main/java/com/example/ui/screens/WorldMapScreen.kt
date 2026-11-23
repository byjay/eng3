package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SquirrelRpgRepository
import com.example.data.TravelCurriculumRepository
import com.example.data.VocabRepository
import com.example.model.AiHumanTutor
import com.example.model.KingdomProgressStats
import com.example.model.ParentProfiles
import com.example.model.ScenarioProgress
import com.example.model.SquirrelJobHero
import com.example.model.TravelZone
import com.example.model.UserProgress
import com.example.model.VocabStudyTrack
import com.example.ui.components.DailyGoalStreakCardSection
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.GameHudBar
import com.example.ui.components.SquirrelHeroAvatar
import com.example.ui.components.SquirrelMoodState
import com.example.ui.components.Tactile3DButton
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeShadow
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalPurpleShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.SpartaRedShadow
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow

@Composable
fun WorldMapScreen(
    progress: UserProgress,
    kingdomStats: KingdomProgressStats,
    parents: ParentProfiles,
    vocabGradeTier: Int,
    masteredWordsCount: Int,
    harvestedApiCount: Int,
    harvestStatusKo: String,
    todayStudySeconds: Int,
    totalStudySeconds: Int,
    studyLogCount: Int,
    scenarioProgress: List<ScenarioProgress>,
    activeTutor: AiHumanTutor,
    activeSquirrelHero: SquirrelJobHero,
    clearedRpgChapters: Set<String>,
    unopenedChests: Int,
    onSwitchPlayer: (String) -> Unit,
    onOpenHeroPicker: () -> Unit,
    onStartRpgChapter: (Int) -> Unit,
    onOpenMysteryChest: () -> Unit,
    onStartVocabTrack: (VocabStudyTrack) -> Unit,
    onStartLevelTest: () -> Unit,
    onStartBossBattle: () -> Unit,
    onOpenTravelZone: (Int) -> Unit,
    onOpenTutorVideoCall: () -> Unit,
    onOpenFamilyGiftHub: () -> Unit,
    onHarvestDailyWords: () -> Unit,
    onRefillHearts: () -> Unit,
    onSpeakFocusEnglish: (String) -> Unit,
    onRecordFocusWordResult: (wordId: String, known: Boolean) -> Unit
) {
    var focusMood by remember { mutableStateOf(SquirrelMoodState.IDLE) }
    val missionsInCycle = kingdomStats.missionClearCount % 7
    val levelPoints = kingdomStats.previewCount + kingdomStats.reviewCount * 2 + kingdomStats.perfectScoreCount * 3
    val levelFraction = ((levelPoints % 6) / 6f).coerceIn(0.08f, 1f)
    val clearedScenariosCount = scenarioProgress.size

    val humorousFamilyQuotes = listOf(
        "${parents.dadName}: \"${parents.daughterName}! 흰색 피그미 다람쥐와 수능 챕터 돌파하자!\"",
        "${parents.momName}: \"${parents.daughterName}! 챕터 깨면 비밀 보물상자가 나타난대!\"",
        "${parents.dadName}: \"원삐·투삐 출동! 오늘도 에메랄드 성채 지키자!\"",
        "${parents.momName}: \"${parents.daughterName}, 오늘 뉴욕 친구랑 회화학습 어때?\""
    )
    val activeQuote = humorousFamilyQuotes[(kingdomStats.missionClearCount + kingdomStats.princessLevel) % humorousFamilyQuotes.size]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFDFEFE8),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 3D Game HUD Bar
        item {
            GameHudBar(
                progress = progress,
                onCoinsClick = onOpenFamilyGiftHub,
                onHeartsClick = onRefillHearts
            )
        }

        // 2. Royal Family Castle Hero Banner with New Dad+Daughters Crest Logo
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(2.5.dp, TrophyGold, RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.img_world_map_hero_1795412460420),
                    contentDescription = "가족 왕국 에메랄드 성채",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF173BCF).copy(alpha = 0.25f),
                                    Color(0xFF0A1647).copy(alpha = 0.78f)
                                )
                            )
                        )
                )
                // Top-Left Instant 원삐 / 투삐 / 손님 Switcher Pill
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("원삐", "투삐").forEach { player ->
                        val active = parents.daughterName == player
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) TrophyGold else Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.5.dp, if (active) Color.White else TrophyGold.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSwitchPlayer(player) }
                                .testTag("switch_player_$player")
                        ) {
                            Text(
                                text = player,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (active) Color(0xFF3E2C00) else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (parents.daughterName.startsWith("손님")) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AdventureEmerald,
                            border = BorderStroke(1.5.dp, Color.White)
                        ) {
                            Text(
                                text = parents.daughterName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                // Floating Original White Pygmy Squirrel Hero Portrait (GPU animated, zero lag!)
                SquirrelHeroAvatar(
                    hero = activeSquirrelHero,
                    size = 76.dp,
                    mood = focusMood,
                    borderWidth = 3.dp,
                    borderColor = TrophyGold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable(onClick = onOpenHeroPicker)
                        .testTag("home_squirrel_avatar_btn")
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = TrophyGold, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "Lv.${kingdomStats.princessLevel} ${parents.daughterName}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF3E2C00),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(color = AdventureEmerald, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "수능 ${vocabGradeTier}등급 빌드업",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "에메랄드 성채 수복률 ${kingdomStats.castleHpPercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    LinearProgressIndicator(
                        progress = { kingdomStats.castleHpPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = TrophyGold,
                        trackColor = Color.White.copy(alpha = 0.28f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "오늘 학습 ${todayStudySeconds / 60}분 ${todayStudySeconds % 60}초 · 누적 ${totalStudySeconds / 60}분 · 로그 ${studyLogCount}건",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.94f)
                    )
                }
            }
        }

        // 2-A. MAIN FOCUSED VOCABULARY STUDY MODE (수능 빈출 단어 + 실전 문장 즉시 집중 학습)
        item {
            MainFocusedVocabStudyCard(
                vocabGradeTier = vocabGradeTier,
                masteredWordsCount = masteredWordsCount,
                streakCount = progress.streakCount,
                todayWordsCompleted = progress.todayWordsCompleted,
                activeHero = activeSquirrelHero,
                onSpeakEnglish = onSpeakFocusEnglish,
                onRecordResult = { wordId, known ->
                    focusMood = if (known) SquirrelMoodState.HAPPY_CHEER else SquirrelMoodState.LISTENING_TILT
                    onRecordFocusWordResult(wordId, known)
                },
                onOpenFullVocab = { onStartVocabTrack(VocabStudyTrack.CSAT_FREQUENCY) }
            )
        }

        // 2-B. Original White Pygmy Squirrel Story RPG Hub (10 Jobs + 6 Story Chapters + Sealed Mystery Chest)
        item {
            val nextChapterNum = SquirrelRpgRepository.chapters
                .firstOrNull { it.id !in clearedRpgChapters }?.number ?: 1
            Elevated3DCard(accentShadow = Color(activeSquirrelHero.accentHex).copy(alpha = 0.4f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SquirrelHeroAvatar(
                            hero = activeSquirrelHero,
                            size = 62.dp,
                            mood = focusMood,
                            borderWidth = 2.5.dp,
                            modifier = Modifier.clickable(onClick = onOpenHeroPicker)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "흰색 피그미 다람쥐 RPG · ${clearedRpgChapters.size}/6 챕터",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(activeSquirrelHero.accentHex)
                            )
                            Text(
                                text = "${activeSquirrelHero.nameKo} (${activeSquirrelHero.jobTitleKo})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = activeSquirrelHero.passiveKo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 6 Story Chapters Horizontal Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SquirrelRpgRepository.chapters, key = { it.id }) { ch ->
                            val cleared = ch.id in clearedRpgChapters
                            val accent = Color(ch.accentHex)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (cleared) AdventureEmerald.copy(alpha = 0.16f) else accent.copy(alpha = 0.12f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (cleared) AdventureEmerald else accent.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onStartRpgChapter(ch.number) }
                                    .testTag("home_rpg_chapter_${ch.number}")
                            ) {
                                Text(
                                    text = if (cleared) "✓ Ch.${ch.number}" else "Ch.${ch.number} ${ch.locationKo.take(5)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (cleared) AdventureEmeraldShadow else accent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Tactile3DButton(
                            text = "다람쥐 10종",
                            icon = Icons.Default.Star,
                            containerColor = TrophyGold,
                            shadowColor = TrophyGoldShadow,
                            contentColor = Color(0xFF3E2C00),
                            height = 46.dp,
                            modifier = Modifier.weight(0.95f),
                            tag = "home_open_squirrel_picker",
                            onClick = onOpenHeroPicker
                        )
                        Tactile3DButton(
                            text = "Ch.$nextChapterNum 스토리 출격",
                            icon = Icons.Default.PlayArrow,
                            containerColor = AdventureEmerald,
                            shadowColor = AdventureEmeraldShadow,
                            height = 46.dp,
                            modifier = Modifier.weight(1.3f),
                            tag = "home_start_rpg_story",
                            onClick = { onStartRpgChapter(nextChapterNum) }
                        )
                    }

                    // Sealed Mystery Chest — ONLY appears when earned! Zero item spoilers!
                    if (unopenedChests > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = TrophyGold.copy(alpha = 0.22f),
                            border = BorderStroke(2.dp, TrophyGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = onOpenMysteryChest)
                                .testTag("home_mystery_chest_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Help,
                                        contentDescription = "미스터리 보물상자",
                                        tint = Color(0xFF3E2C00),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "??? 미스터리 보물상자 ${unopenedChests}개 도착!",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFF3E2C00)
                                    )
                                }
                                Text(
                                    text = "열기",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = TrophyGoldShadow
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Humorous Dad King & Mom Queen Family Speech Bubble
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = TrophyGold.copy(alpha = 0.20f),
                border = BorderStroke(1.5.dp, TrophyGoldShadow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TrophyGoldShadow,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = activeQuote,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 4. 7-Mission Difficulty Escalation & Exam Monster Boss Battle + Level-Up Card
        item {
            Elevated3DCard(
                accentShadow = if (kingdomStats.bossPending) SpartaRed.copy(alpha = 0.45f) else RoyalPurple.copy(alpha = 0.3f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (kingdomStats.bossPending) SpartaRed else RoyalPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "7회 미션 보스전 · 난이도 Tier ${kingdomStats.difficultyTier}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (kingdomStats.bossPending) SpartaRed else SkyQuestBlue.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (kingdomStats.bossPending) "보스 출현!!" else "$missionsInCycle / 7 미션",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (kingdomStats.bossPending) Color.White else SkyQuestBlueShadow,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 7-step mission dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 1..7) {
                            val filled = kingdomStats.bossPending || i <= missionsInCycle
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        when {
                                            i == 7 && filled -> SpartaRed
                                            filled -> AdventureEmerald
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }

                    // Preview / Review / Perfect Level-Up Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "레벨업 누적 · 예습 ${kingdomStats.previewCount} · 복습 ${kingdomStats.reviewCount} · 만점 ${kingdomStats.perfectScoreCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { levelFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = RoyalPurple,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    if (kingdomStats.bossPending) {
                        Tactile3DButton(
                            text = "시험 마왕 보스전 출격!",
                            icon = Icons.Default.LocalFireDepartment,
                            containerColor = SpartaRed,
                            shadowColor = SpartaRedShadow,
                            height = 48.dp,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "launch_boss_battle_button",
                            onClick = onStartBossBattle
                        )
                    } else {
                        Tactile3DButton(
                            text = "수능 1~5등급 실력 검증 테스트",
                            icon = Icons.Default.EmojiEvents,
                            containerColor = SkyQuestBlue,
                            shadowColor = SkyQuestBlueShadow,
                            height = 48.dp,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "home_level_test_button",
                            onClick = onStartLevelTest
                        )
                    }
                }
            }
        }

        // 5. Anti-ABC 5-Track Vocab + Daily Free Word API 50+ Accumulator Card
        item {
            Elevated3DCard(accentShadow = AdventureEmerald.copy(alpha = 0.3f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ABC 순서 배제! 수능 영단어 성벽 훈련",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "마스터 ${masteredWordsCount}개 · 무료API 누적 ${harvestedApiCount}단어 ($harvestStatusKo)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SkyQuestBlue.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onHarvestDailyWords)
                                .testTag("home_harvest_api_words")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "오늘의 50단어 수집",
                                    tint = SkyQuestBlueShadow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "+50수집",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SkyQuestBlueShadow
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(VocabStudyTrack.entries) { track ->
                            val (bg, shadow) = when (track) {
                                VocabStudyTrack.CSAT_FREQUENCY -> SpartaRed to SpartaRedShadow
                                VocabStudyTrack.CSAT_DIFFICULTY -> RoyalPurple to RoyalPurpleShadow
                                VocabStudyTrack.RANDOM_MIX -> AdventureEmerald to AdventureEmeraldShadow
                                VocabStudyTrack.SMART_REVIEW -> CoralOrange to CoralOrangeShadow
                                VocabStudyTrack.TOPIC_SITUATION -> SkyQuestBlue to SkyQuestBlueShadow
                            }
                            Tactile3DButton(
                                text = track.labelKo,
                                containerColor = bg,
                                shadowColor = shadow,
                                height = 44.dp,
                                tag = "home_track_${track.name}",
                                onClick = { onStartVocabTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Real Live Video-Call AI Teacher Quick Launch
        item {
            Elevated3DCard(accentShadow = Color(activeTutor.themeColorHex).copy(alpha = 0.35f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Image(
                                painter = painterResource(activeTutor.portraitRes),
                                contentDescription = activeTutor.koreanNickname,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, Color(activeTutor.themeColorHex), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(AdventureEmerald)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE 회화학습 · 투명 자막 · 실시간 교정",
                                style = MaterialTheme.typography.labelMedium,
                                color = AdventureEmeraldShadow
                            )
                            Text(
                                text = activeTutor.koreanNickname,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "카카오 페이스톡 감성 · EN/KR 음성 질문",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Tactile3DButton(
                        text = "${activeTutor.name} · LIVE 회화학습 시작",
                        icon = Icons.Default.RecordVoiceOver,
                        containerColor = AdventureEmerald,
                        shadowColor = AdventureEmeraldShadow,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "map_quick_tutor_call",
                        onClick = onOpenTutorVideoCall
                    )
                }
            }
        }

        // 7. Original 2-Column Fairytale Expedition Grid (Zero Duolingo Zigzag Nodes!)
        item {
            Column {
                Text(
                    text = "에메랄드 원정대 · 실전 회화 100상황",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "클리어 $clearedScenariosCount/100 · 문장과 함께 실전 회화 훈련",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(TravelCurriculumRepository.zones.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { zone ->
                    val zoneCleared = scenarioProgress.count { it.zoneId == zone.id }
                    FairytaleExpeditionTile(
                        zone = zone,
                        clearedCount = zoneCleared,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTravelZone(zone.id) }
                    )
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun FairytaleExpeditionTile(
    zone: TravelZone,
    clearedCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = Color(zone.accentHex)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("world_node_${zone.id}")
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (zone.id <= 2) Icons.Default.FlightTakeoff else Icons.Default.Star,
                            contentDescription = zone.titleKo,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "W${zone.id}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = accent
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "시작",
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = zone.titleKo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1
            )
            Text(
                text = "$clearedCount / 10 완료",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
        }
    }
}

/**
 * Main Screen Focused Vocabulary Study Mode (메인 화면 집중 단어 학습 모드):
 * - Instant CSAT high-frequency word + real English example sentence right on Home
 * - Native pronunciation TTS, tap-to-reveal meaning, Known / Review Again quick flow
 */
@Composable
private fun MainFocusedVocabStudyCard(
    vocabGradeTier: Int,
    masteredWordsCount: Int,
    streakCount: Int,
    todayWordsCompleted: Int,
    activeHero: SquirrelJobHero,
    onSpeakEnglish: (String) -> Unit,
    onRecordResult: (wordId: String, known: Boolean) -> Unit,
    onOpenFullVocab: () -> Unit
) {
    val focusDeck = remember(vocabGradeTier) {
        val all = VocabRepository.allWords
        val preferred = all.filter { it.csatFrequency >= 4 }
        (if (preferred.isNotEmpty()) preferred else all).take(40)
    }
    var currentIdx by remember { mutableIntStateOf(0) }
    var showMeaning by remember { mutableStateOf(false) }
    var sessionStreak by remember { mutableIntStateOf(0) }

    val word = focusDeck[currentIdx % focusDeck.size]
    val sentenceEn = word.examples.firstOrNull()?.english ?: word.exampleEn
    val sentenceKo = word.examples.firstOrNull()?.korean ?: word.exampleKo

    Elevated3DCard(accentShadow = SkyQuestBlueShadow.copy(alpha = 0.42f)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        shape = RoundedCornerShape(10.dp),
                        color = SkyQuestBlue.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = SkyQuestBlueShadow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "집중 단어 학습 모드",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = SkyQuestBlueShadow
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AdventureEmerald.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "연속 ${sessionStreak}단어",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AdventureEmeraldShadow,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "누적 ${masteredWordsCount}개 · 전체 ▸",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SkyQuestBlueShadow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenFullVocab)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("main_focus_open_full_vocab")
                )
            }

            // Daily Goal Streak Counter & 10-Word Progress Tracker
            DailyGoalStreakCardSection(
                streakCount = streakCount,
                todayWordsCompleted = todayWordsCompleted
            )

            // Word & Real CSAT Sentence Panel
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF6F2E9),
                border = BorderStroke(1.dp, Color(0xFFE2DACB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${word.ipa} · ${word.partOfSpeech} · 수능 빈출 ★${word.csatFrequency}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = SkyQuestBlue,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { onSpeakEnglish("${word.word}. $sentenceEn") }
                                .testTag("main_focus_speak_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "원어민 단어·문장 듣기",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = sentenceEn,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (showMeaning) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AdventureEmerald.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = word.meaningKo,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = AdventureEmeraldShadow
                                )
                                Text(
                                    text = sentenceKo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, SkyQuestBlue.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showMeaning = true }
                                .testTag("main_focus_reveal_meaning")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = SkyQuestBlueShadow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "터치해서 뜻 · 해석 확인하기 (${activeHero.nameKo}와 암기 중)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SkyQuestBlueShadow
                                )
                            }
                        }
                    }
                }
            }

            // Instant Action Buttons (Review vs Mastered)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Tactile3DButton(
                    text = "다시 복습",
                    icon = Icons.Default.Refresh,
                    containerColor = CoralOrange,
                    shadowColor = CoralOrangeShadow,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    tag = "main_focus_unknown_btn",
                    onClick = {
                        onRecordResult(word.id, false)
                        showMeaning = false
                        currentIdx = (currentIdx + 1) % focusDeck.size
                    }
                )
                Tactile3DButton(
                    text = "완벽 암기!",
                    icon = Icons.Default.CheckCircle,
                    containerColor = AdventureEmerald,
                    shadowColor = AdventureEmeraldShadow,
                    height = 46.dp,
                    modifier = Modifier.weight(1.2f),
                    tag = "main_focus_known_btn",
                    onClick = {
                        onRecordResult(word.id, true)
                        sessionStreak += 1
                        showMeaning = false
                        currentIdx = (currentIdx + 1) % focusDeck.size
                    }
                )
            }
        }
    }
}

