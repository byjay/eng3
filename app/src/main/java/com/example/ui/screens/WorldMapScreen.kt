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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.TravelCurriculumRepository
import com.example.model.AiHumanTutor
import com.example.model.KingdomProgressStats
import com.example.model.ParentProfiles
import com.example.model.ScenarioProgress
import com.example.model.TravelZone
import com.example.model.UserProgress
import com.example.model.VocabStudyTrack
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.GameHudBar
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
    onSwitchPlayer: (String) -> Unit,
    onStartVocabTrack: (VocabStudyTrack) -> Unit,
    onStartLevelTest: () -> Unit,
    onStartBossBattle: () -> Unit,
    onOpenTravelZone: (Int) -> Unit,
    onOpenTutorVideoCall: () -> Unit,
    onOpenFamilyGiftHub: () -> Unit,
    onHarvestDailyWords: () -> Unit,
    onRefillHearts: () -> Unit
) {
    val floatAnim = rememberInfiniteTransition(label = "crestFloat")
    val bobOffset by floatAnim.animateFloat(
        initialValue = -4f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobY"
    )

    val missionsInCycle = kingdomStats.missionClearCount % 7
    val levelPoints = kingdomStats.previewCount + kingdomStats.reviewCount * 2 + kingdomStats.perfectScoreCount * 3
    val levelFraction = ((levelPoints % 6) / 6f).coerceIn(0.08f, 1f)
    val clearedScenariosCount = scenarioProgress.size

    val humorousFamilyQuotes = listOf(
        "${parents.dadName}: \"${parents.daughterName}! 오늘 수능 빈출 단어 뚫으면 치킨 성벽이 뚝딱 세워집니다!\"",
        "${parents.momName}: \"${parents.daughterName}! 예습·복습 쌓아서 레벨업하면 엄마한테 선물 요청장 바로 날려줘!\"",
        "${parents.dadName}: \"긴급 칙령! 시험 마왕이 단어장을 훔쳐갔소! 빅뚱·스몰뚱 출동해서 보스전으로 혼내주시오!\"",
        "${parents.momName}: \"${parents.daughterName}, 오늘 영상통화로 원어민 선생님이랑 수다 떨면 왕국 통신탑이 반짝반짝 빛난대요!\""
    )
    val activeQuote = humorousFamilyQuotes[(kingdomStats.missionClearCount + kingdomStats.princessLevel) % humorousFamilyQuotes.size]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFDDE7FF),
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
                // Top-Left Instant 빅뚱 / 스몰뚱 Player Switcher Pill
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("빅뚱", "스몰뚱").forEach { player ->
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
                }
                // Floating New Castle Crest Emblem
                Image(
                    painter = painterResource(R.drawable.dad_kingdom_logo_1795414187994),
                    contentDescription = "아빠가딸들을 위한 학습도구 왕국 문장",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .offset(y = bobOffset.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.dp, TrophyGold, CircleShape)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (kingdomStats.bossPending) {
                            Tactile3DButton(
                                text = "시험 마왕 보스전 출격!",
                                icon = Icons.Default.LocalFireDepartment,
                                containerColor = SpartaRed,
                                shadowColor = SpartaRedShadow,
                                height = 48.dp,
                                modifier = Modifier.weight(1f),
                                tag = "launch_boss_battle_button",
                                onClick = onStartBossBattle
                            )
                        } else {
                            Tactile3DButton(
                                text = "1~5등급 레벨테스트",
                                icon = Icons.Default.EmojiEvents,
                                containerColor = SkyQuestBlue,
                                shadowColor = SkyQuestBlueShadow,
                                height = 48.dp,
                                modifier = Modifier.weight(1f),
                                tag = "home_level_test_button",
                                onClick = onStartLevelTest
                            )
                        }
                        Tactile3DButton(
                            text = "부모님 선물요청 (${kingdomStats.giftTickets})",
                            icon = Icons.Default.CardGiftcard,
                            containerColor = TrophyGold,
                            shadowColor = TrophyGoldShadow,
                            contentColor = Color(0xFF3E2C00),
                            height = 48.dp,
                            modifier = Modifier.weight(1f),
                            tag = "home_gift_request_button",
                            onClick = onOpenFamilyGiftHub
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
                                text = "진짜 영상통화 · 자막·실시간 교정·음성 설정",
                                style = MaterialTheme.typography.labelMedium,
                                color = AdventureEmeraldShadow
                            )
                            Text(
                                text = activeTutor.koreanNickname,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "\"마트에서 이거 빼고 현금으로 할게요\" 한국어로 물어도 바로 알려드려요!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Tactile3DButton(
                        text = "${activeTutor.name} 선생님과 LIVE 영상통화 걸기",
                        icon = Icons.Default.Videocam,
                        containerColor = AdventureEmerald,
                        shadowColor = AdventureEmeraldShadow,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "map_quick_tutor_call",
                        onClick = onOpenTutorVideoCall
                    )
                }
            }
        }

        // 7. 3D Winding Adventure Worlds (100 Travel Scenarios)
        item {
            Column {
                Text(
                    text = "왕국 해외 원정대 맵 (100대 여행 × 30문장)",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "클리어한 여행 상황 $clearedScenariosCount/100 · 돌발 변수를 깨고 성채 코인을 모으세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        itemsIndexed(TravelCurriculumRepository.zones) { index, zone ->
            val zoneCleared = scenarioProgress.count { it.zoneId == zone.id }
            val offsetDp = when (index % 4) {
                0 -> (-30).dp
                1 -> 16.dp
                2 -> 34.dp
                else -> (-10).dp
            }
            AdventureMapNodeRow(
                zone = zone,
                clearedCount = zoneCleared,
                horizontalOffset = offsetDp,
                onClick = { onOpenTravelZone(zone.id) }
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun AdventureMapNodeRow(
    zone: TravelZone,
    clearedCount: Int,
    horizontalOffset: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val accent = Color(zone.accentHex)
    val shadow = Color(zone.shadowHex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .offset(x = horizontalOffset)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onClick)
                .testTag("world_node_${zone.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = 5.dp)
                        .clip(CircleShape)
                        .background(shadow)
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.92f), accent)))
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (zone.id <= 2) Icons.Default.FlightTakeoff else Icons.Default.Star,
                            contentDescription = zone.titleKo,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "W${zone.id}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.5f)),
                modifier = Modifier.width(214.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = zone.titleKo, style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Default.PlayArrow, contentDescription = "시작", tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = zone.subtitleKo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "진행 $clearedCount / 10 상황 · +400 코인",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
            }
        }
    }
}
