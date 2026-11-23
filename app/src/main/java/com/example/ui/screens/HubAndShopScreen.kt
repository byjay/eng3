package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HumanTutorRepository
import com.example.model.AiEngineMode
import com.example.model.CollectibleItem
import com.example.model.DadAllowanceMission
import com.example.model.GiftRequestItem
import com.example.model.KakaoSessionState
import com.example.model.KingdomProgressStats
import com.example.model.LlmSlotConfig
import com.example.model.ParentProfiles
import com.example.model.ScorecardEntry
import com.example.model.StudyScheduleItem
import com.example.model.StudyTimeLogEntry
import com.example.model.UserProgress
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.StudyStatsOverviewCards
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

private val KakaoYellow = Color(0xFFFEE500)
private val KakaoBrown = Color(0xFF3C1E1E)

@Composable
fun HubAndShopScreen(
    progress: UserProgress,
    kingdomStats: KingdomProgressStats,
    parents: ParentProfiles,
    vocabGradeTier: Int,
    masteredWordsCount: Int,
    harvestedApiCount: Int,
    giftRequests: List<GiftRequestItem>,
    dadMissions: List<DadAllowanceMission>,
    scorecards: List<ScorecardEntry>,
    schedules: List<StudyScheduleItem>,
    studyLogs: List<StudyTimeLogEntry>,
    activeStudyElapsedSec: Int,
    kakaoSession: KakaoSessionState,
    cloudflareWorkerUrl: String,
    cloudflareStatusKo: String,
    engineMode: AiEngineMode,
    llmSlots: List<LlmSlotConfig>,
    slotTestResults: Map<Int, String>,
    onSaveParentProfiles: (ParentProfiles) -> Unit,
    onCreateGiftRequest: (targetParent: String, titleKo: String, reasonKo: String) -> Unit,
    onApproveGiftWithPin: (giftId: String, pin: String) -> Boolean,
    onShareGiftToKakao: (GiftRequestItem) -> Unit,
    onAddDadMission: (titleKo: String, descKo: String, rewardWon: Int) -> Unit,
    onMarkMissionDone: (missionId: String) -> Unit,
    onApproveMissionWithPin: (missionId: String, pin: String) -> Boolean,
    onAddScorecard: (examTitle: String, subject: String, score: Int, target: Int) -> Unit,
    onAddSchedule: (dayKo: String, timeLabel: String, titleKo: String, detailKo: String) -> Unit,
    onToggleScheduleDone: (id: String) -> Unit,
    onFlushStudyLog: () -> Unit,
    onShareFamilyReportToKakao: () -> Unit,
    onLaunchKakaoLogin: () -> Unit,
    onRefreshKakaoFriends: () -> Unit,
    onLogoutKakao: () -> Unit,
    onRegisterCloudflareLevel: (String) -> Unit,
    onBuyCollectible: (CollectibleItem) -> Unit,
    onRefillHearts: () -> Unit,
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onSaveLlmSlot: (LlmSlotConfig) -> Unit,
    onTestLlmSlot: (LlmSlotConfig) -> Unit
) {
    // 0 = 아빠·선물, 1 = 통계·로그, 2 = 용돈·점수, 3 = 카카오·CF·AI
    var activeSubTab by remember { mutableIntStateOf(0) }

    // Parent PIN Dialog State (shared for Gift approval & Allowance Mission approval)
    var pinDialogTitle by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinAction by remember { mutableStateOf<((String) -> Boolean)?>(null) }

    if (pinDialogTitle != null && pinAction != null) {
        AlertDialog(
            onDismissRequest = {
                pinDialogTitle = null
                pinInput = ""
            },
            title = { Text(pinDialogTitle ?: "부모님 PIN 승인") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "등록된 아빠·엄마 4자리 PIN 번호를 입력하세요.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("부모님 PIN (기본 1234)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_pin_dialog_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = pinAction?.invoke(pinInput) == true
                        if (ok) {
                            pinDialogTitle = null
                            pinInput = ""
                        }
                    },
                    modifier = Modifier.testTag("parent_pin_confirm_button")
                ) {
                    Text("승인하기", fontWeight = FontWeight.Bold, color = AdventureEmeraldShadow)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pinDialogTitle = null
                    pinInput = ""
                }) {
                    Text("취소")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top 4 Sub-Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val tabs = listOf(
                    Triple(0, "아빠·선물", TrophyGold to TrophyGoldShadow),
                    Triple(1, "통계·로그", SkyQuestBlue to SkyQuestBlueShadow),
                    Triple(2, "용돈·점수", AdventureEmerald to AdventureEmeraldShadow),
                    Triple(3, "카카오·AI", RoyalPurple to RoyalPurpleShadow)
                )
                tabs.forEach { (idx, label, colors) ->
                    val selected = activeSubTab == idx
                    Tactile3DButton(
                        text = label,
                        containerColor = if (selected) colors.first else MaterialTheme.colorScheme.surfaceVariant,
                        shadowColor = if (selected) colors.second else Color(0xFFC5CFD9),
                        contentColor = when {
                            selected && idx == 0 -> Color(0xFF3E2C00)
                            selected -> Color.White
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                        tag = "hub_subtab_$idx",
                        onClick = { activeSubTab = idx }
                    )
                }
            }
        }

        // ==================== TAB 1: EXACT STUDY TIME LOGS & STATISTICS ====================
        if (activeSubTab == 1) {
            item {
                StudyStatsOverviewCards(
                    logs = studyLogs,
                    activeStudyElapsedSec = activeStudyElapsedSec,
                    onFlushCurrentLog = onFlushStudyLog
                )
            }
        }

        // ==================== TAB 0: PARENTS (DAD & MOM) + GIFT REQUESTS ====================
        if (activeSubTab == 0) {
            item {
                ParentRegistrationCard(
                    parents = parents,
                    onSave = onSaveParentProfiles
                )
            }

            item {
                GiftRequestComposerCard(
                    parents = parents,
                    kingdomStats = kingdomStats,
                    onSubmitGift = onCreateGiftRequest
                )
            }

            item {
                Text(
                    text = "왕실 선물 요청함 (${giftRequests.size}건)",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (giftRequests.isEmpty()) {
                item {
                    Elevated3DCard {
                        Text(
                            text = "아직 등록된 선물 요청이 없어요! 예습·복습·만점을 모아 선물 요청권을 얻고 아빠나 엄마께 신청해 보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(giftRequests, key = { it.id }) { gift ->
                    val isDad = gift.targetParent == "아빠"
                    val parentLabel = if (isDad) parents.dadName else parents.momName
                    Elevated3DCard(
                        accentShadow = if (gift.isApproved) AdventureEmeraldShadow.copy(alpha = 0.4f)
                        else TrophyGoldShadow.copy(alpha = 0.4f)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDad) SkyQuestBlue.copy(alpha = 0.16f) else CoralOrange.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "받는 분: $parentLabel (${gift.targetParent})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDad) SkyQuestBlueShadow else CoralOrangeShadow,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (gift.isApproved) AdventureEmerald else TrophyGold
                                ) {
                                    Text(
                                        text = if (gift.isApproved) "왕실 승인 완료!" else "승인 대기 중",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (gift.isApproved) Color.White else Color(0xFF3E2C00),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = gift.giftTitleKo,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "사유: ${gift.reasonKo} · 신청 Lv.${gift.requiredLevel} (${gift.createdDateLabel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Tactile3DButton(
                                    text = "카톡 요청장 전송",
                                    icon = Icons.Default.Share,
                                    containerColor = KakaoYellow,
                                    shadowColor = Color(0xFFD1BC00),
                                    contentColor = KakaoBrown,
                                    height = 44.dp,
                                    modifier = Modifier.weight(1f),
                                    tag = "gift_kakao_share_${gift.id}",
                                    onClick = { onShareGiftToKakao(gift) }
                                )
                                if (!gift.isApproved) {
                                    Tactile3DButton(
                                        text = "부모님 PIN 승인",
                                        icon = Icons.Default.LockOpen,
                                        containerColor = AdventureEmerald,
                                        shadowColor = AdventureEmeraldShadow,
                                        height = 44.dp,
                                        modifier = Modifier.weight(1f),
                                        tag = "gift_pin_approve_${gift.id}",
                                        onClick = {
                                            pinDialogTitle = "${parentLabel} 선물 승인: ${gift.giftTitleKo}"
                                            pinInput = ""
                                            pinAction = { entered -> onApproveGiftWithPin(gift.id, entered) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== TAB 2: DAD ALLOWANCE MISSIONS + SCORECARD + SCHEDULE ====================
        if (activeSubTab == 2) {
            // Kakao Family Report Banner
            item {
                Elevated3DCard(accentShadow = Color(0xFFD1BC00)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val avgScore = scorecards.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${parents.daughterName} 누적 획득 용돈",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${kingdomStats.totalAllowanceWon}원",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = AdventureEmeraldShadow
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SkyQuestBlue.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "성적 평균 ${avgScore}점",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SkyQuestBlueShadow,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Tactile3DButton(
                            text = "카카오톡으로 가족 성적·용돈 보고서 공유",
                            icon = Icons.Default.Share,
                            containerColor = KakaoYellow,
                            shadowColor = Color(0xFFD1BC00),
                            contentColor = KakaoBrown,
                            height = 46.dp,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "share_family_report_kakao",
                            onClick = onShareFamilyReportToKakao
                        )
                    }
                }
            }

            // Dad's Allowance Missions
            item {
                DadAllowanceMissionCreator(onAddMission = onAddDadMission)
            }

            items(dadMissions, key = { it.id }) { mission ->
                Elevated3DCard(
                    accentShadow = if (mission.isApprovedByDad) AdventureEmeraldShadow.copy(alpha = 0.35f)
                    else CoralOrangeShadow.copy(alpha = 0.35f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mission.titleKo,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TrophyGold.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "+${mission.rewardWon}원",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = TrophyGoldShadow,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = mission.descriptionKo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when {
                                mission.isApprovedByDad -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = AdventureEmerald.copy(alpha = 0.16f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "아빠 승인 & 용돈 지급 완료! (성채 HP +${mission.castleHpBonus}%)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = AdventureEmeraldShadow,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                !mission.isCompletedByDaughter -> {
                                    Tactile3DButton(
                                        text = "${parents.daughterName}: 미션 완료 보고하기!",
                                        icon = Icons.Default.CheckCircle,
                                        containerColor = SkyQuestBlue,
                                        shadowColor = SkyQuestBlueShadow,
                                        height = 44.dp,
                                        modifier = Modifier.fillMaxWidth(),
                                        tag = "mission_daughter_done_${mission.id}",
                                        onClick = { onMarkMissionDone(mission.id) }
                                    )
                                }
                                else -> {
                                    Tactile3DButton(
                                        text = "부모님 PIN 승인 (+${mission.rewardWon}원 지급)",
                                        icon = Icons.Default.LockOpen,
                                        containerColor = AdventureEmerald,
                                        shadowColor = AdventureEmeraldShadow,
                                        height = 44.dp,
                                        modifier = Modifier.fillMaxWidth(),
                                        tag = "mission_parent_approve_${mission.id}",
                                        onClick = {
                                            pinDialogTitle = "용돈 미션 승인 (+${mission.rewardWon}원)"
                                            pinInput = ""
                                            pinAction = { entered -> onApproveMissionWithPin(mission.id, entered) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Exam Scorecard Section
            item {
                ScorecardCreatorCard(onAddScorecard = onAddScorecard)
            }

            items(scorecards, key = { it.id }) { card ->
                val hitGoal = card.score >= card.targetScore
                Elevated3DCard(
                    accentShadow = if (hitGoal) AdventureEmeraldShadow.copy(alpha = 0.35f)
                    else SkyQuestBlueShadow.copy(alpha = 0.3f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${card.examTitleKo} (${card.subjectKo})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = card.dateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${card.score}점 / 목표 ${card.targetScore}점",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (hitGoal) AdventureEmeraldShadow else CoralOrange
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (card.score / 100f).coerceIn(0f, 1f) },
                            color = if (hitGoal) AdventureEmerald else SkyQuestBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }

            // Weekly Study Schedule Section
            item {
                ScheduleCreatorCard(onAddSchedule = onAddSchedule)
            }

            items(schedules, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { onToggleScheduleDone(item.id) },
                            colors = CheckboxDefaults.colors(checkedColor = AdventureEmerald)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[${item.dayLabelKo} ${item.timeLabel}] ${item.titleKo}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = item.detailKo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ==================== TAB 3: KAKAO LOGIN & FRIENDS + CLOUDFLARE + 3D SHOP + 10-SLOT LLM ====================
        if (activeSubTab == 3) {
            // 1. Kakao OAuth2 Login & Friends Sync Card
            item {
                Elevated3DCard(accentShadow = Color(0xFFD1BC00)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = KakaoBrown,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "카카오톡 로그인 & 카톡 친구 연동",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = kakaoSession.statusMessage,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!kakaoSession.isLoggedIn) {
                            Tactile3DButton(
                                text = "카카오톡으로 로그인 & 친구 불러오기",
                                icon = Icons.Default.VerifiedUser,
                                containerColor = KakaoYellow,
                                shadowColor = Color(0xFFD1BC00),
                                contentColor = KakaoBrown,
                                height = 48.dp,
                                modifier = Modifier.fillMaxWidth(),
                                tag = "kakao_oauth_login_button",
                                onClick = onLaunchKakaoLogin
                            )
                            Text(
                                text = "테스트 기간에는 로그인 없이도 모든 학습·미션·선물 기능을 바로 사용할 수 있으며, Secrets에 KAKAO_REST_API_KEY를 넣으면 실제 카카오 계정·친구가 연동됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = KakaoYellow.copy(alpha = 0.28f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "연동 계정: ${kakaoSession.nickname}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = KakaoBrown
                                    )
                                    Text(
                                        text = "연동된 카카오톡 친구: ${kakaoSession.friends.size}명",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KakaoBrown
                                    )
                                    kakaoSession.friends.take(5).forEach { f ->
                                        Text(
                                            text = "• ${f.nickname}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = KakaoBrown
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Tactile3DButton(
                                    text = "친구 새로고침",
                                    icon = Icons.Default.Refresh,
                                    containerColor = SkyQuestBlue,
                                    shadowColor = SkyQuestBlueShadow,
                                    height = 44.dp,
                                    modifier = Modifier.weight(1f),
                                    tag = "kakao_refresh_friends_button",
                                    onClick = onRefreshKakaoFriends
                                )
                                Tactile3DButton(
                                    text = "연동 해제",
                                    containerColor = SpartaRed,
                                    shadowColor = SpartaRedShadow,
                                    height = 44.dp,
                                    modifier = Modifier.weight(1f),
                                    tag = "kakao_logout_button",
                                    onClick = onLogoutKakao
                                )
                            }
                        }
                    }
                }
            }

            // 2. Cloudflare Workers Level Registration Card
            item {
                CloudflareLevelRegistrationCard(
                    vocabGradeTier = vocabGradeTier,
                    princessLevel = kingdomStats.princessLevel,
                    masteredWordsCount = masteredWordsCount,
                    harvestedApiCount = harvestedApiCount,
                    initialWorkerUrl = cloudflareWorkerUrl,
                    statusKo = cloudflareStatusKo,
                    onRegister = onRegisterCloudflareLevel
                )
            }

            // 3. 3D Coin Shop & Heart Refill
            item {
                Elevated3DCard(accentShadow = TrophyGoldShadow.copy(alpha = 0.4f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = TrophyGoldShadow,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text("보유 왕국 코인", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "${progress.coins} 코인",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                }
                            }
                            Tactile3DButton(
                                text = "하트 풀충전 (50)",
                                icon = Icons.Default.Favorite,
                                containerColor = SpartaRed,
                                shadowColor = SpartaRedShadow,
                                height = 44.dp,
                                enabled = progress.coins >= 50 && progress.hearts < 5,
                                tag = "shop_refill_hearts_button",
                                onClick = onRefillHearts
                            )
                        }
                        Text(
                            text = "3D 왕국 보물 상점 (구매 시 성채 HP +5%)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        HumanTutorRepository.collectibleShopItems.forEach { item ->
                            val owned = item.id in progress.unlockedItems
                            val accent = Color(item.colorHex)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (owned) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                                        contentDescription = item.titleKo,
                                        tint = accent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.titleKo,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = item.bonusLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Tactile3DButton(
                                        text = if (owned) "보유" else "${item.costCoins}",
                                        containerColor = if (owned) AdventureEmerald else accent,
                                        shadowColor = if (owned) AdventureEmeraldShadow else accent.copy(alpha = 0.7f),
                                        height = 38.dp,
                                        enabled = !owned && progress.coins >= item.costCoins,
                                        tag = "buy_item_${item.id}",
                                        onClick = { onBuyCollectible(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Hybrid AI Engine & 10-Slot LLM Rotator
            item {
                Elevated3DCard(accentShadow = RoyalPurpleShadow.copy(alpha = 0.35f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RoyalPurple,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "영상통화 AI 엔진 선택",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        AiEngineMode.entries.forEach { mode ->
                            val selected = mode == engineMode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) RoyalPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    if (selected) 1.5.dp else 1.dp,
                                    if (selected) RoyalPurple else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectEngineMode(mode) }
                                    .testTag("engine_mode_${mode.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { onSelectEngineMode(mode) },
                                        colors = RadioButtonDefaults.colors(selectedColor = RoyalPurple)
                                    )
                                    Column {
                                        Text(
                                            text = mode.labelKo,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = mode.descKo,
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

            items(llmSlots, key = { it.slotIndex }) { slot ->
                LlmSlotEditorCard(
                    slot = slot,
                    testResult = slotTestResults[slot.slotIndex],
                    onSave = onSaveLlmSlot,
                    onTest = onTestLlmSlot
                )
            }
        }
    }
}

@Composable
private fun ParentRegistrationCard(
    parents: ParentProfiles,
    onSave: (ParentProfiles) -> Unit
) {
    var dadName by remember(parents.dadName) { mutableStateOf(parents.dadName) }
    var momName by remember(parents.momName) { mutableStateOf(parents.momName) }
    var daughterName by remember(parents.daughterName) { mutableStateOf(parents.daughterName) }
    var pin by remember(parents.parentPin) { mutableStateOf(parents.parentPin) }

    Elevated3DCard(accentShadow = SkyQuestBlueShadow.copy(alpha = 0.35f)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "아빠 국왕 · 엄마 왕비 · 빅뚱·스몰뚱 등록",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            // Quick 빅뚱 / 스몰뚱 toggle chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("빅뚱", "스몰뚱").forEach { preset ->
                    val active = daughterName == preset
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) TrophyGold else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { daughterName = preset }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "$preset 선택",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (active) Color(0xFF3E2C00) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dadName,
                    onValueChange = { dadName = it },
                    label = { Text("아빠 호칭") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = momName,
                    onValueChange = { momName = it },
                    label = { Text("엄마 호칭") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = daughterName,
                    onValueChange = { daughterName = it },
                    label = { Text("도전자 이름 (빅뚱/스몰뚱)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("부모님 PIN (4자리)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Tactile3DButton(
                text = "가족 왕국 정보 저장",
                icon = Icons.Default.Save,
                containerColor = SkyQuestBlue,
                shadowColor = SkyQuestBlueShadow,
                height = 46.dp,
                modifier = Modifier.fillMaxWidth(),
                tag = "save_parent_profiles_button",
                onClick = {
                    onSave(
                        ParentProfiles(
                            dadName = dadName.ifBlank { "아빠 국왕" },
                            momName = momName.ifBlank { "엄마 왕비" },
                            daughterName = daughterName.ifBlank { "빅뚱" },
                            parentPin = pin.ifBlank { "1234" }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun GiftRequestComposerCard(
    parents: ParentProfiles,
    kingdomStats: KingdomProgressStats,
    onSubmitGift: (String, String, String) -> Unit
) {
    var targetParent by remember { mutableStateOf("아빠") }
    var giftTitle by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Elevated3DCard(accentShadow = TrophyGoldShadow.copy(alpha = 0.4f)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = TrophyGoldShadow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "아빠 · 엄마께 선물 요청하기",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TrophyGold.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "요청권 ${kingdomStats.giftTickets}장",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = TrophyGoldShadow,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("아빠" to parents.dadName, "엄마" to parents.momName).forEach { (key, label) ->
                    val active = targetParent == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) {
                            if (key == "아빠") SkyQuestBlue else CoralOrange
                        } else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { targetParent = key }
                            .testTag("gift_target_$key")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = "$label 에게 요청",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = giftTitle,
                onValueChange = { giftTitle = it },
                label = { Text("받고 싶은 선물 (예: 떡볶이 세트, 문구 쇼핑, 영화 데이트)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gift_title_input")
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("${parents.daughterName}의 당당한 사유 (예: 수능 단어 30개 만점!)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Tactile3DButton(
                text = "선물 요청장 등록하기",
                icon = Icons.Default.Add,
                containerColor = TrophyGold,
                shadowColor = TrophyGoldShadow,
                contentColor = Color(0xFF3E2C00),
                height = 48.dp,
                enabled = giftTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                tag = "submit_gift_request_button",
                onClick = {
                    onSubmitGift(targetParent, giftTitle, reason)
                    giftTitle = ""
                    reason = ""
                }
            )
        }
    }
}

@Composable
private fun DadAllowanceMissionCreator(
    onAddMission: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var rewardStr by remember { mutableStateOf("3000") }

    Elevated3DCard(accentShadow = AdventureEmeraldShadow.copy(alpha = 0.35f)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "아빠의 직접 용돈 미션 추가",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("미션 제목 (예: 고3 수능 빈출 30단어 말하기)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("응원 메모") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = rewardStr,
                    onValueChange = { rewardStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("용돈(원)") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }
            Tactile3DButton(
                text = "새 용돈 미션 등록",
                icon = Icons.Default.Add,
                containerColor = AdventureEmerald,
                shadowColor = AdventureEmeraldShadow,
                height = 46.dp,
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                tag = "add_dad_mission_button",
                onClick = {
                    onAddMission(title, desc, rewardStr.toIntOrNull() ?: 2000)
                    title = ""
                    desc = ""
                }
            )
        }
    }
}

@Composable
private fun ScorecardCreatorCard(
    onAddScorecard: (String, String, Int, Int) -> Unit
) {
    var examTitle by remember { mutableStateOf("") }
    var scoreStr by remember { mutableStateOf("92") }
    var targetStr by remember { mutableStateOf("90") }

    Elevated3DCard(accentShadow = SkyQuestBlueShadow.copy(alpha = 0.35f)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Score, contentDescription = null, tint = SkyQuestBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "성적표 · 점수표 기록",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
            OutlinedTextField(
                value = examTitle,
                onValueChange = { examTitle = it },
                label = { Text("시험명 (예: 6월 모의고사 영어)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = scoreStr,
                    onValueChange = { scoreStr = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("내 점수") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("목표 점수") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Tactile3DButton(
                text = "점수표에 기록하기",
                icon = Icons.Default.Save,
                containerColor = SkyQuestBlue,
                shadowColor = SkyQuestBlueShadow,
                height = 44.dp,
                enabled = examTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                tag = "add_scorecard_button",
                onClick = {
                    onAddScorecard(
                        examTitle,
                        "고등 영어",
                        scoreStr.toIntOrNull() ?: 85,
                        targetStr.toIntOrNull() ?: 90
                    )
                    examTitle = ""
                }
            )
        }
    }
}

@Composable
private fun ScheduleCreatorCard(
    onAddSchedule: (String, String, String, String) -> Unit
) {
    var dayKo by remember { mutableStateOf("월~금") }
    var timeLabel by remember { mutableStateOf("오후 8:30") }
    var title by remember { mutableStateOf("") }

    Elevated3DCard(accentShadow = RoyalPurpleShadow.copy(alpha = 0.35f)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "주간 학습 일정표",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dayKo,
                    onValueChange = { dayKo = it },
                    label = { Text("요일") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = timeLabel,
                    onValueChange = { timeLabel = it },
                    label = { Text("시간") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("일정 내용 (예: 원어민 영상통화 10분)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Tactile3DButton(
                text = "학습 일정 추가",
                icon = Icons.Default.Add,
                containerColor = RoyalPurple,
                shadowColor = RoyalPurpleShadow,
                height = 44.dp,
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                tag = "add_schedule_button",
                onClick = {
                    onAddSchedule(dayKo, timeLabel, title, "가족 왕국 학습 루틴")
                    title = ""
                }
            )
        }
    }
}

@Composable
private fun CloudflareLevelRegistrationCard(
    vocabGradeTier: Int,
    princessLevel: Int,
    masteredWordsCount: Int,
    harvestedApiCount: Int,
    initialWorkerUrl: String,
    statusKo: String,
    onRegister: (String) -> Unit
) {
    var urlInput by remember(initialWorkerUrl) { mutableStateOf(initialWorkerUrl) }

    Elevated3DCard(accentShadow = CoralOrangeShadow.copy(alpha = 0.4f)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = CoralOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Cloudflare 서버 레벨 등록",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "현재 수능 ${vocabGradeTier}등급 · Lv.$princessLevel · 마스터 ${masteredWordsCount}단어 · API 누적 ${harvestedApiCount}개",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Cloudflare Worker URL (예: https://my-worker.workers.dev)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloudflare_worker_url_input")
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "상태: $statusKo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Tactile3DButton(
                text = "Cloudflare 서버에 현재 등급·레벨 등록",
                icon = Icons.Default.CloudUpload,
                containerColor = CoralOrange,
                shadowColor = CoralOrangeShadow,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth(),
                tag = "register_cloudflare_level_button",
                onClick = { onRegister(urlInput) }
            )
        }
    }
}

@Composable
private fun LlmSlotEditorCard(
    slot: LlmSlotConfig,
    testResult: String?,
    onSave: (LlmSlotConfig) -> Unit,
    onTest: (LlmSlotConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var baseUrl by remember(slot.baseUrl) { mutableStateOf(slot.baseUrl) }
    var modelId by remember(slot.modelId) { mutableStateOf(slot.modelId) }
    var apiKey by remember(slot.apiKey) { mutableStateOf(slot.apiKey) }
    var enabled by remember(slot.enabled) { mutableStateOf(slot.enabled) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = if (enabled && apiKey.isNotBlank()) AdventureEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "슬롯 #${slot.slotIndex + 1} · ${slot.providerName}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (apiKey.isBlank()) "키 미입력 (${slot.modelId})" else "키 설정됨 · ${slot.lastStatus}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "슬롯 열기/닫기"
                )
            }

            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("순환 활성화", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = AdventureEmerald)
                    )
                }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text("Model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!testResult.isNullOrBlank()) {
                    Text(
                        text = testResult,
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyQuestBlueShadow
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val updated = slot.copy(
                        baseUrl = baseUrl.trim(),
                        modelId = modelId.trim(),
                        apiKey = apiKey.trim(),
                        enabled = enabled
                    )
                    Tactile3DButton(
                        text = "저장",
                        icon = Icons.Default.Save,
                        containerColor = AdventureEmerald,
                        shadowColor = AdventureEmeraldShadow,
                        height = 42.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { onSave(updated) }
                    )
                    Tactile3DButton(
                        text = "테스트",
                        icon = Icons.Default.PlayArrow,
                        containerColor = SkyQuestBlue,
                        shadowColor = SkyQuestBlueShadow,
                        height = 42.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { onTest(updated) }
                    )
                }
            }
        }
    }
}
