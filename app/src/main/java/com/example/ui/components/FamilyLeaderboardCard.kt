package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.RankedFamilyMember
import com.example.model.KakaoFriend
import com.example.model.KakaoSessionState
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCobaltSoft
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow

private val LeaderboardKakaoYellow = Color(0xFFFEE500)
private val LeaderboardKakaoBrown = Color(0xFF3C1E1E)
private val SilverMedalColor = Color(0xFF94A3B8)
private val BronzeMedalColor = Color(0xFFCD7F32)

sealed interface FamilyLeaderboardUiState {
    data object SignedOut : FamilyLeaderboardUiState

    data class Loading(
        val previousMembers: List<RankedFamilyMember> = emptyList()
    ) : FamilyLeaderboardUiState

    data class Success(
        val members: List<RankedFamilyMember>,
        val unlinkedKakaoFriends: List<KakaoFriend>,
        val shareWithFriends: Boolean,
        val syncedAtLabel: String
    ) : FamilyLeaderboardUiState

    data class Error(
        val message: String,
        val cachedMembers: List<RankedFamilyMember> = emptyList()
    ) : FamilyLeaderboardUiState
}

@Composable
fun FamilyLeaderboardCard(
    state: FamilyLeaderboardUiState,
    kakaoSession: KakaoSessionState,
    firebaseUserDisplayName: String?,
    shareWithFriends: Boolean,
    compact: Boolean = false,
    onGoogleSignIn: () -> Unit,
    onSignOutFirebase: () -> Unit,
    onConnectKakao: () -> Unit,
    onRefreshLeaderboard: () -> Unit,
    onToggleShareWithFriends: (Boolean) -> Unit,
    onInviteKakaoFriends: () -> Unit,
    onOpenFullLeaderboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Elevated3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("family_leaderboard_card"),
        accentShadow = TrophyGoldShadow.copy(alpha = 0.45f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header row
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = TrophyGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.45f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = stringResource(R.string.family_leaderboard_title),
                                tint = TrophyGoldShadow,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.family_leaderboard_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.family_leaderboard_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (state !is FamilyLeaderboardUiState.SignedOut) {
                    IconButton(
                        onClick = onRefreshLeaderboard,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("family_leaderboard_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.family_leaderboard_refresh_desc),
                            tint = StudioCobalt
                        )
                    }
                }
            }

            // Status strip when authenticated
            if (state !is FamilyLeaderboardUiState.SignedOut) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AdventureEmerald.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = AdventureEmeraldShadow,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = buildString {
                                    append("Firebase 연동")
                                    if (!firebaseUserDisplayName.isNullOrBlank()) {
                                        append(" · ${firebaseUserDisplayName.take(14)}")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AdventureEmeraldShadow
                            )
                        }
                    }

                    if (state is FamilyLeaderboardUiState.Success && state.syncedAtLabel.isNotBlank()) {
                        Text(
                            text = "동기화 ${state.syncedAtLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when (state) {
                is FamilyLeaderboardUiState.SignedOut -> {
                    SignedOutLeaderboardPanel(onGoogleSignIn = onGoogleSignIn)
                }

                is FamilyLeaderboardUiState.Loading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = StudioCobalt,
                            trackColor = StudioCobaltSoft,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "카카오톡 친구들의 일일 연속 학습 스트릭을 불러오는 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.previousMembers.isNotEmpty()) {
                            LeaderboardRowsList(
                                members = if (compact) state.previousMembers.take(3) else state.previousMembers
                            )
                        }
                    }
                }

                is FamilyLeaderboardUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioCoral.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = StudioCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = onRefreshLeaderboard,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("family_leaderboard_retry_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.family_leaderboard_retry_cta))
                            }
                        }
                    }
                    if (state.cachedMembers.isNotEmpty()) {
                        LeaderboardRowsList(
                            members = if (compact) state.cachedMembers.take(3) else state.cachedMembers
                        )
                    }
                }

                is FamilyLeaderboardUiState.Success -> {
                    // Prompt Kakao Friends connection if not yet logged in to Kakao
                    if (!kakaoSession.isLoggedIn) {
                        KakaoConnectBanner(
                            isKakaoConfigured = kakaoSession.isConfigured,
                            onConnectKakao = onConnectKakao
                        )
                    }

                    // Top-3 Podium when 2+ family/friends are synced
                    if (state.members.size >= 2) {
                        FamilyStreakPodium(members = state.members.take(3))
                    }

                    // Ranked list
                    if (state.members.isNotEmpty()) {
                        val visibleMembers = if (compact) state.members.take(3) else state.members
                        LeaderboardRowsList(members = visibleMembers)
                    }

                    // Honest empty/invite state when no connected Kakao Friends have synced yet
                    if (kakaoSession.isLoggedIn && state.members.size <= 1) {
                        UnjoinedFriendsNoticeCard(
                            unlinkedFriends = state.unlinkedKakaoFriends,
                            onInviteKakaoFriends = onInviteKakaoFriends
                        )
                    } else if (!compact && state.unlinkedKakaoFriends.isNotEmpty()) {
                        UnjoinedFriendsNoticeCard(
                            unlinkedFriends = state.unlinkedKakaoFriends,
                            onInviteKakaoFriends = onInviteKakaoFriends
                        )
                    }

                    // Compact mode footer CTA to open full Family Leaderboard
                    if (compact && onOpenFullLeaderboard != null) {
                        OutlinedButton(
                            onClick = onOpenFullLeaderboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("family_leaderboard_view_all_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.family_leaderboard_view_all),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Full mode privacy toggle + Firebase account sign-out
                    if (!compact) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.family_leaderboard_share_label),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = shareWithFriends,
                                        onCheckedChange = onToggleShareWithFriends,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AdventureEmerald
                                        ),
                                        modifier = Modifier.testTag("family_leaderboard_share_switch")
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = onSignOutFirebase,
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("family_leaderboard_firebase_signout_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Firebase 로그아웃", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignedOutLeaderboardPanel(onGoogleSignIn: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StudioCobaltSoft.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = StudioCobalt,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Firebase Google 인증이 필요해요",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.family_leaderboard_google_signin_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onGoogleSignIn,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioCobalt,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("family_leaderboard_google_signin_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.family_leaderboard_google_signin_cta),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun KakaoConnectBanner(
    isKakaoConfigured: Boolean,
    onConnectKakao: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LeaderboardKakaoYellow.copy(alpha = 0.30f),
        border = BorderStroke(1.dp, LeaderboardKakaoBrown.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isKakaoConfigured) {
                    "카카오톡 친구를 연동하면 가족·친구들의 일일 스트릭 순위가 자동으로 표시됩니다."
                } else {
                    "카카오톡 앱 키를 연동하면 카톡 친구 목록에서 가족들의 일일 스트릭을 실시간으로 불러옵니다."
                },
                style = MaterialTheme.typography.bodySmall,
                color = LeaderboardKakaoBrown
            )
            Button(
                onClick = onConnectKakao,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LeaderboardKakaoYellow,
                    contentColor = LeaderboardKakaoBrown
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("family_leaderboard_kakao_connect_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubble,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.family_leaderboard_kakao_connect_cta),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun FamilyStreakPodium(members: List<RankedFamilyMember>) {
    val first = members.getOrNull(0)
    val second = members.getOrNull(1)
    val third = members.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (second != null) {
            PodiumSlot(
                member = second,
                medalColor = SilverMedalColor,
                podiumHeightDp = 64,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (first != null) {
            PodiumSlot(
                member = first,
                medalColor = TrophyGold,
                podiumHeightDp = 82,
                isChampion = true,
                modifier = Modifier.weight(1.12f)
            )
        }

        if (third != null) {
            PodiumSlot(
                member = third,
                medalColor = BronzeMedalColor,
                podiumHeightDp = 54,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumSlot(
    member: RankedFamilyMember,
    medalColor: Color,
    podiumHeightDp: Int,
    isChampion: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isChampion) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = "1위 챔피언",
                tint = TrophyGoldShadow,
                modifier = Modifier.size(20.dp)
            )
        }
        MemberAvatarMonogram(
            displayName = member.displayName,
            ringColor = medalColor,
            sizeDp = if (isChampion) 46 else 38
        )
        Text(
            text = member.displayName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (member.isCurrentUser) FontWeight.ExtraBold else FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
            color = medalColor.copy(alpha = if (isChampion) 0.24f else 0.16f),
            border = BorderStroke(1.dp, medalColor.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeightDp.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "${member.rank}위",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = CoralOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${member.streakCount}일",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowsList(members: List<RankedFamilyMember>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        members.forEach { member ->
            val rankColor = when (member.rank) {
                1 -> TrophyGoldShadow
                2 -> SilverMedalColor
                3 -> BronzeMedalColor
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val rowBackground = if (member.isCurrentUser) {
                StudioCobaltSoft.copy(alpha = 0.50f)
            } else {
                MaterialTheme.colorScheme.surface
            }
            val rowBorder = if (member.isCurrentUser) {
                BorderStroke(1.5.dp, StudioCobalt.copy(alpha = 0.55f))
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = rowBackground,
                border = rowBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("family_leaderboard_row_${member.rank}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.width(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${member.rank}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = rankColor
                        )
                    }

                    MemberAvatarMonogram(
                        displayName = member.displayName,
                        ringColor = if (member.isKakaoLinked) LeaderboardKakaoYellow else StudioIris,
                        sizeDp = 38
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = member.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (member.isCurrentUser) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StudioCobalt
                                ) {
                                    Text(
                                        text = "나",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "오늘 ${member.todayWordsCompleted}단어 · ${member.totalXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = CoralOrange.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "연속 학습 스트릭",
                                tint = CoralOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${member.streakCount}일",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = CoralOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberAvatarMonogram(
    displayName: String,
    ringColor: Color,
    sizeDp: Int
) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        StudioCobalt.copy(alpha = 0.85f),
                        StudioIris.copy(alpha = 0.85f)
                    )
                )
            )
            .border(2.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White
        )
    }
}

@Composable
private fun UnjoinedFriendsNoticeCard(
    unlinkedFriends: List<KakaoFriend>,
    onInviteKakaoFriends: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val message = if (unlinkedFriends.isNotEmpty()) {
                val previewNames = unlinkedFriends.take(3).joinToString(", ") { it.nickname }
                val suffix = if (unlinkedFriends.size > 3) " 외 ${unlinkedFriends.size - 3}명" else ""
                "연동된 카톡 친구 중 아직 스트릭을 공유하지 않은 가족: $previewNames$suffix"
            } else {
                "아직 스트릭을 연동한 카카오톡 친구가 없어요. 가족을 초대하고 함께 연속 학습 기록을 쌓아보세요!"
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onInviteKakaoFriends,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("family_leaderboard_invite_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.GroupAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.family_leaderboard_invite_cta),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
