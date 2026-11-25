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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
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
import com.example.model.KakaoSessionState
import com.example.model.KingdomProgressStats
import com.example.model.ParentProfiles
import com.example.model.ScenarioProgress
import com.example.model.SquirrelJobHero
import com.example.model.TravelZone
import com.example.model.UserProgress
import com.example.model.VocabStudyTrack
import com.example.model.VocabWord
import com.example.ui.components.DailyGoalStreakCardSection
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.FamilyLeaderboardCard
import com.example.ui.components.FamilyLeaderboardUiState
import com.example.ui.components.GameHudBar
import com.example.ui.components.SquirrelHeroAvatar
import com.example.ui.components.SquirrelMoodState
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.YouTubeWordContextDialog
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeShadow
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.GlacierCyanSoft
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalPurpleShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.SpartaRedShadow
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCobaltSoft
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCoralSoft
import com.example.ui.theme.StudioIris
import com.example.ui.theme.StudioIrisSoft
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stars
import com.example.ui.components.MaterialButtonStyle
import com.example.ui.components.MaterialLearningButton
import com.example.ui.components.StitchIconBadge
import com.example.ui.components.StitchIconButton
import com.example.ui.components.travelZoneStitchIcon

@Composable
fun WorldMapScreen(
    progress: UserProgress,
    kingdomStats: KingdomProgressStats,
    parents: ParentProfiles,
    vocabGradeTier: Int,
    masteredWordsCount: Int,
    weakWordIds: Set<String>,
    harvestedApiWords: List<VocabWord>,
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
    onOpenEnGameHub: () -> Unit,
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
    onRecordFocusWordResult: (wordId: String, known: Boolean) -> Unit,
    onOpenWeeklyReport: () -> Unit,
    familyLeaderboardState: FamilyLeaderboardUiState = FamilyLeaderboardUiState.SignedOut,
    kakaoSession: KakaoSessionState = KakaoSessionState(),
    firebaseDisplayName: String? = null,
    shareStreakWithFriends: Boolean = false,
    onGoogleSignIn: () -> Unit = {},
    onSignOutFirebase: () -> Unit = {},
    onConnectKakao: () -> Unit = {},
    onRefreshFamilyLeaderboard: () -> Unit = {},
    onToggleShareStreakWithFriends: (Boolean) -> Unit = {},
    onInviteKakaoFriends: () -> Unit = {},
    onOpenFullFamilyLeaderboard: () -> Unit = onOpenFamilyGiftHub
) {
    var focusMood by remember { mutableStateOf(SquirrelMoodState.IDLE) }
    var isWrongReviewMode by remember { mutableStateOf(false) }
    var reviewAllClearBanner by remember { mutableStateOf(false) }
    val wrongWords = remember(weakWordIds, harvestedApiWords) {
        (VocabRepository.allWords + harvestedApiWords)
            .distinctBy { it.id }
            .filter { it.id in weakWordIds }
    }
    if (isWrongReviewMode && wrongWords.isEmpty()) {
        isWrongReviewMode = false
        reviewAllClearBanner = true
    }
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
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 3D Game HUD Bar
        item {
            GameHudBar(
                progress = progress,
                onCoinsClick = onOpenFamilyGiftHub,
                onHeartsClick = onRefillHearts,
                onStreakClick = onOpenWeeklyReport
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
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
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
                                color = Color.White,
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

        // 2-A. Top-30 EdTech Compact Daily Streak Strip + Clean 2x2 Quick-Launch Action Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        DailyGoalStreakCardSection(
                            todayWordsCompleted = progress.todayWordsCompleted,
                            streakCount = progress.streakCount,
                            onOpenWeeklyReport = onOpenWeeklyReport
                        )
                    }
                }

                // 2-B. Family Leaderboard (Kakao Friends Daily Streak Counts via Firebase Auth)
                FamilyLeaderboardCard(
                    state = familyLeaderboardState,
                    kakaoSession = kakaoSession,
                    firebaseUserDisplayName = firebaseDisplayName,
                    shareWithFriends = shareStreakWithFriends,
                    compact = true,
                    onGoogleSignIn = onGoogleSignIn,
                    onSignOutFirebase = onSignOutFirebase,
                    onConnectKakao = onConnectKakao,
                    onRefreshLeaderboard = onRefreshFamilyLeaderboard,
                    onToggleShareWithFriends = onToggleShareStreakWithFriends,
                    onInviteKakaoFriends = onInviteKakaoFriends,
                    onOpenFullLeaderboard = onOpenFullFamilyLeaderboard
                )

                // 2x2 Modern Learning-App Action Grid (Zero nested box clutter!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_quick_launch_row_1"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeQuickActionTile(
                        title = "단어암기",
                        subtitle = "카드·속성 · 마스터 ${masteredWordsCount}",
                        badge = "오늘 ${progress.todayWordsCompleted}/10",
                        icon = Icons.Outlined.MenuBook,
                        accent = StudioCobalt,
                        softBg = StudioCobaltSoft,
                        tag = "home_launch_vocab_tab",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartVocabTrack(VocabStudyTrack.CSAT_FREQUENCY) }
                    )
                    HomeQuickActionTile(
                        title = "오답 집중 복습",
                        subtitle = if (wrongWords.isEmpty()) "현재 오답 모두 정복!" else "${wrongWords.size}단어 집중 복습",
                        badge = if (wrongWords.isEmpty()) "ALL CLEAR" else "복습 ${wrongWords.size}",
                        icon = Icons.Outlined.Refresh,
                        accent = StudioCoral,
                        softBg = StudioCoralSoft,
                        tag = "home_launch_wrong_review_tab",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartVocabTrack(VocabStudyTrack.SMART_REVIEW) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_quick_launch_row_2"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeQuickActionTile(
                        title = "360° 거리뷰 여행",
                        subtitle = "구글 스트리트뷰 실전 회화",
                        badge = "$clearedScenariosCount/100 클리어",
                        icon = Icons.Outlined.Flight,
                        accent = GlacierCyan,
                        softBg = GlacierCyanSoft,
                        tag = "home_launch_travel_grid",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTravelZone(1) }
                    )
                    HomeQuickActionTile(
                        title = "LIVE 원어민 회화",
                        subtitle = "${activeTutor.name} · 33종 모션 리깅",
                        badge = "실시간 립싱크",
                        icon = Icons.Outlined.MicNone,
                        accent = StudioIris,
                        softBg = StudioIrisSoft,
                        tag = "home_launch_tutor_grid",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTutorVideoCall
                    )
                }
            }
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
                        MaterialLearningButton(
                            text = "다람쥐 10종",
                            icon = Icons.Rounded.Stars,
                            accent = TrophyGold,
                            style = MaterialButtonStyle.TONAL,
                            modifier = Modifier.weight(0.95f),
                            tag = "home_open_squirrel_picker",
                            onClick = onOpenHeroPicker
                        )
                        MaterialLearningButton(
                            text = "Ch.$nextChapterNum 스토리",
                            icon = Icons.Rounded.PlayArrow,
                            accent = AdventureEmerald,
                            style = MaterialButtonStyle.FILLED,
                            modifier = Modifier.weight(1.25f),
                            tag = "home_start_rpg_story",
                            onClick = { onStartRpgChapter(nextChapterNum) }
                        )
                    }

                    // EnGame 2.0 4-Mode Arcade Launch Button
                    MaterialLearningButton(
                        text = "EnGame 2.0 아케이드 (블리츠·문장조립·동의어·보스)",
                        icon = Icons.Rounded.SportsEsports,
                        accent = StudioCobalt,
                        style = MaterialButtonStyle.FILLED,
                        badgeText = "4종",
                        modifier = Modifier.fillMaxWidth(),
                        tag = "home_open_engame_hub",
                        onClick = onOpenEnGameHub
                    )

                    // Sealed Mystery Chest — ONLY appears when earned! Zero item spoilers!
                    if (unopenedChests > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = TrophyGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.5.dp, TrophyGold.copy(alpha = 0.65f)),
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
                                        imageVector = Icons.Outlined.HelpOutline,
                                        contentDescription = "미스터리 보물상자",
                                        tint = TrophyGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "미스터리 보물상자 ${unopenedChests}개 도착",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "열기",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TrophyGold
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
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = TrophyGold,
                        modifier = Modifier.size(22.dp)
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
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = if (kingdomStats.bossPending) SpartaRed else RoyalPurple,
                                modifier = Modifier.size(22.dp)
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
                            icon = Icons.Outlined.LocalFireDepartment,
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
                            icon = Icons.Outlined.EmojiEvents,
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

        // 5. Google Stitch Iconified Anti-ABC 5-Track Vocab + Daily +50 R2/API Accumulator
        item {
            Elevated3DCard(accentShadow = StudioCobalt.copy(alpha = 0.22f)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "수능 영단어 5트랙",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "마스터 ${masteredWordsCount} · R2/API +${harvestedApiCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StitchIconButton(
                            icon = Icons.Outlined.CloudDownload,
                            contentDescription = "오늘의 50단어 수집",
                            accent = SkyQuestBlue,
                            filled = true,
                            size = 46.dp,
                            iconSize = 21.dp,
                            badgeCount = 50,
                            tag = "home_harvest_api_words",
                            onClick = onHarvestDailyWords
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VocabStudyTrack.entries.forEach { track ->
                            val (icon, shortLabel, accent) = when (track) {
                                VocabStudyTrack.CSAT_FREQUENCY -> Triple(Icons.Outlined.LocalFireDepartment, "빈출", SpartaRed)
                                VocabStudyTrack.CSAT_DIFFICULTY -> Triple(Icons.Outlined.TrendingUp, "난도", StudioIris)
                                VocabStudyTrack.RANDOM_MIX -> Triple(Icons.Outlined.Shuffle, "랜덤", StudioCobalt)
                                VocabStudyTrack.SMART_REVIEW -> Triple(Icons.Outlined.Replay, "오답", StudioCoral)
                                VocabStudyTrack.TOPIC_SITUATION -> Triple(Icons.Outlined.Category, "주제", GlacierCyan)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onStartVocabTrack(track) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("home_track_${track.name}")
                            ) {
                                StitchIconBadge(
                                    icon = icon,
                                    contentDescription = track.labelKo,
                                    accent = accent,
                                    size = 48.dp,
                                    iconSize = 22.dp
                                )
                                Text(
                                    text = shortLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                                text = "LIVE 회화 · 3D 립싱크 · 실시간 교정",
                                style = MaterialTheme.typography.labelMedium,
                                color = AdventureEmerald
                            )
                            Text(
                                text = activeTutor.koreanNickname,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "실시간 AI 튜터 · EN/KR 음성 질문",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Tactile3DButton(
                        text = "${activeTutor.name} · LIVE 회화 시작",
                        icon = Icons.Outlined.MicNone,
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
private fun HomeQuickActionTile(
    title: String,
    subtitle: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    softBg: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        shadowElevation = 3.dp,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                StitchIconBadge(
                    icon = icon,
                    contentDescription = title,
                    accent = accent,
                    size = 48.dp,
                    iconSize = 23.dp,
                    filled = true
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = softBg
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("world_node_${zone.id}")
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
                StitchIconBadge(
                    icon = travelZoneStitchIcon(zone.id),
                    contentDescription = zone.titleKo,
                    accent = accent,
                    size = 42.dp,
                    iconSize = 20.dp,
                    cornerBadgeText = "${zone.id}"
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "$clearedCount/10",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = zone.titleKo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1
            )
        }
    }
}

/**
 * Main Screen Focused Vocabulary Study Mode (메인 화면 집중 단어 학습 모드):
 * - Instant CSAT high-frequency word + real English example sentence right on Home
 * - Dedicated Wrong-Answer Review Mode ('복습 모드') serving ONLY archived wrong words!
 */
@Composable
private fun MainFocusedVocabStudyCard(
    vocabGradeTier: Int,
    masteredWordsCount: Int,
    streakCount: Int,
    todayWordsCompleted: Int,
    activeHero: SquirrelJobHero,
    wrongWords: List<VocabWord>,
    isReviewMode: Boolean,
    reviewAllClearBanner: Boolean,
    onToggleReviewMode: (Boolean) -> Unit,
    onDismissAllClear: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onRecordResult: (wordId: String, known: Boolean) -> Unit,
    onOpenFullVocab: () -> Unit,
    onOpenWeeklyReport: () -> Unit
) {
    var selectedGrade by remember { mutableIntStateOf(0) } // 0 = 전체, 1 = 고1, 2 = 고2, 3 = 고3
    val normalDeck = remember(vocabGradeTier, selectedGrade) {
        val pool = VocabRepository.wordsForGrade(selectedGrade)
        if (selectedGrade != 0) {
            pool
        } else {
            val preferred = pool.filter { it.csatFrequency >= 4 }
            (if (preferred.isNotEmpty()) preferred else pool).take(45)
        }
    }
    val activeDeck = if (isReviewMode && wrongWords.isNotEmpty()) wrongWords else normalDeck
    var currentIdx by remember(isReviewMode, selectedGrade) { mutableIntStateOf(0) }
    var showMeaning by remember(selectedGrade) { mutableStateOf(false) }
    var sessionStreak by remember { mutableIntStateOf(0) }
    var youtubeWordQuery by remember { mutableStateOf<String?>(null) }

    val word = activeDeck[currentIdx % activeDeck.size]
    val sentenceEn = word.examples.firstOrNull()?.english ?: word.exampleEn
    val sentenceKo = word.examples.firstOrNull()?.korean ?: word.exampleKo

    if (youtubeWordQuery != null) {
        YouTubeWordContextDialog(
            wordOrPhrase = youtubeWordQuery!!,
            onDismiss = { youtubeWordQuery = null }
        )
    }

    Elevated3DCard(
        accentShadow = if (isReviewMode) CoralOrangeShadow.copy(alpha = 0.42f) else SkyQuestBlueShadow.copy(alpha = 0.36f)
    ) {
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
                        color = if (isReviewMode) CoralOrange.copy(alpha = 0.16f) else SkyQuestBlue.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isReviewMode) Icons.Outlined.Refresh else Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = if (isReviewMode) CoralOrange else SkyQuestBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isReviewMode) "오답 복습" else "집중 단어",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isReviewMode) CoralOrange else SkyQuestBlue
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isReviewMode) CoralOrange else AdventureEmerald.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (isReviewMode) onToggleReviewMode(false)
                                else if (wrongWords.isNotEmpty()) onToggleReviewMode(true)
                            }
                            .testTag("main_focus_review_mode_toggle")
                    ) {
                        Text(
                            text = if (isReviewMode) "오답 ${wrongWords.size} · 해제" else if (wrongWords.isNotEmpty()) "복습 (${wrongWords.size})" else "연속 ${sessionStreak}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isReviewMode) Color.White else if (wrongWords.isNotEmpty()) CoralOrange else AdventureEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "누적 ${masteredWordsCount} · 전체 ▸",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SkyQuestBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenFullVocab)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("main_focus_open_full_vocab")
                )
            }

            // Top High School Difficulty Grade Deck Filter Tabs (전체 · 고1 · 고2 · 고3) — Dark-Mode Safe!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_focus_grade_tabs"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val gradeTabs = listOf(
                    Triple(0, "전체", RoyalPurple),
                    Triple(1, "고1", AdventureEmerald),
                    Triple(2, "고2", SkyQuestBlue),
                    Triple(3, "고3", SpartaRed)
                )
                gradeTabs.forEach { (gradeVal, label, accentColor) ->
                    val selected = !isReviewMode && selectedGrade == gradeVal
                    val gradeCount = remember(gradeVal) { VocabRepository.wordsForGrade(gradeVal).size }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) accentColor else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedGrade = gradeVal
                                currentIdx = 0
                                showMeaning = false
                                if (isReviewMode) onToggleReviewMode(false)
                            }
                            .testTag("main_focus_grade_tab_$gradeVal")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "${gradeCount}단어",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                color = if (selected) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (reviewAllClearBanner) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AdventureEmerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AdventureEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismissAllClear)
                        .testTag("wrong_note_all_clear_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "오답 노트 올클리어!",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AdventureEmerald
                        )
                        Text(
                            text = "확인",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AdventureEmerald
                        )
                    }
                }
            }

            // Daily Goal Streak Counter & 10-Word Progress Tracker
            DailyGoalStreakCardSection(
                streakCount = streakCount,
                todayWordsCompleted = todayWordsCompleted,
                onOpenWeeklyReport = onOpenWeeklyReport
            )

            // Word & Real CSAT Sentence Panel — Dark-Mode Safe!
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isReviewMode) CoralOrange.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (isReviewMode) CoralOrange.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val wordGradeColor = when (word.grade) {
                                    1 -> AdventureEmerald
                                    2 -> SkyQuestBlue
                                    else -> SpartaRed
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = wordGradeColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "고${word.grade}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = wordGradeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isReviewMode) {
                                    "오답 복습 ${(currentIdx % activeDeck.size) + 1}/${activeDeck.size} · ${word.ipa}"
                                } else {
                                    "${word.ipa} · ${word.partOfSpeech} · ★${word.csatFrequency}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReviewMode) CoralOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = SpartaRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, SpartaRed.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { youtubeWordQuery = word.word }
                                    .testTag("main_focus_youtube_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayCircle,
                                        contentDescription = "유튜브 강의 구간 재생",
                                        tint = SpartaRed,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isReviewMode) CoralOrange else SkyQuestBlue,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { onSpeakEnglish("${word.word}. $sentenceEn") }
                                    .testTag("main_focus_speak_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = "원어민 단어·문장 듣기",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = sentenceEn,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (showMeaning) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AdventureEmerald.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = word.meaningKo,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AdventureEmerald
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
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = SkyQuestBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "뜻 · 예문 해석 보기",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SkyQuestBlue
                                )
                            }
                        }
                    }
                }
            }

            // Instant Action Buttons (Zero truncation!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Tactile3DButton(
                    text = "다시 복습",
                    icon = Icons.Outlined.Refresh,
                    containerColor = CoralOrange,
                    shadowColor = CoralOrangeShadow,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    tag = "main_focus_unknown_btn",
                    onClick = {
                        onRecordResult(word.id, false)
                        showMeaning = false
                        currentIdx = (currentIdx + 1) % activeDeck.size
                    }
                )
                Tactile3DButton(
                    text = if (isReviewMode) "오답 정복" else "암기 완료",
                    icon = Icons.Outlined.CheckCircle,
                    containerColor = AdventureEmerald,
                    shadowColor = AdventureEmeraldShadow,
                    height = 46.dp,
                    modifier = Modifier.weight(1.1f),
                    tag = "main_focus_known_btn",
                    onClick = {
                        onRecordResult(word.id, true)
                        sessionStreak += 1
                        showMeaning = false
                        currentIdx = if (isReviewMode) 0 else (currentIdx + 1) % activeDeck.size
                    }
                )
            }
        }
    }
}

/**
 * Dedicated Wrong Answer Notebook Section ('오답 노트') + Review Mode ('복습 모드') button:
 * - Automatically stores words marked wrong / '다시 복습' in Focused Vocabulary Study Mode
 * - Lets learner launch focused Review Mode to study ONLY wrong words until all are mastered
 */
@Composable
private fun WrongAnswerNotebookCard(
    wrongWords: List<VocabWord>,
    isReviewModeActive: Boolean,
    onEnterReviewMode: () -> Unit,
    onStartSpartaWeakReview: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onMarkMastered: (String) -> Unit
) {
    var showAllWords by remember { mutableStateOf(false) }
    val visibleWords = if (showAllWords) wrongWords else wrongWords.take(4)

    Elevated3DCard(accentShadow = CoralOrangeShadow.copy(alpha = 0.36f)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CoralOrange.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "오답 노트",
                            tint = CoralOrange,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "오답 노트",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "틀린 단어가 자동 보관됩니다",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (wrongWords.isNotEmpty()) CoralOrange else AdventureEmerald.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (wrongWords.isNotEmpty()) "${wrongWords.size}단어" else "0단어",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (wrongWords.isNotEmpty()) Color.White else AdventureEmerald,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            if (wrongWords.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "보관된 오답이 없습니다. '다시 복습'을 누른 단어가 여기에 모입니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tactile3DButton(
                        text = if (isReviewModeActive) "복습 중 ✓" else "복습 모드 (${wrongWords.size})",
                        icon = Icons.Outlined.Refresh,
                        containerColor = if (isReviewModeActive) AdventureEmerald else CoralOrange,
                        shadowColor = if (isReviewModeActive) AdventureEmeraldShadow else CoralOrangeShadow,
                        height = 46.dp,
                        modifier = Modifier.weight(1.15f),
                        tag = "wrong_note_review_mode_btn",
                        onClick = onEnterReviewMode
                    )
                    Tactile3DButton(
                        text = "약점 집중",
                        icon = Icons.Outlined.LocalFireDepartment,
                        containerColor = RoyalPurple,
                        shadowColor = RoyalPurpleShadow,
                        height = 46.dp,
                        modifier = Modifier.weight(0.95f),
                        tag = "wrong_note_sparta_review_btn",
                        onClick = onStartSpartaWeakReview
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    visibleWords.forEach { w ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wrong_note_item_${w.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = w.word,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = w.ipa,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = w.meaningKo,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = CoralOrange
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = SkyQuestBlue.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable { onSpeakWord("${w.word}. ${w.exampleEn}") }
                                        .testTag("wrong_note_speak_${w.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                            contentDescription = "${w.word} 발음 듣기",
                                            tint = SkyQuestBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AdventureEmerald,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onMarkMastered(w.id) }
                                        .testTag("wrong_note_clear_${w.id}")
                                ) {
                                    Text(
                                        text = "암기 완료",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (wrongWords.size > 4) {
                    Text(
                        text = if (showAllWords) "오답 목록 접기 ▴" else "보관된 오답 ${wrongWords.size}개 모두 보기 ▾",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CoralOrangeShadow,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAllWords = !showAllWords }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("wrong_note_expand_toggle")
                    )
                }
            }
        }
    }
}

