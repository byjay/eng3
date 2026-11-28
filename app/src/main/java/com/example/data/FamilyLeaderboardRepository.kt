package com.example.data

import android.content.Context
import com.example.cloudflare.CloudflareApiClient
import com.example.cloudflare.CloudflareAuthManager
import com.example.cloudflare.CloudflareConfig
import com.example.cloudflare.CloudflareUser
import com.example.model.FamilyStreakEntry
import com.example.model.KakaoFriend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RankedFamilyMember(
    val rank: Int,
    val entryId: String,
    val displayName: String,
    val avatarUrl: String,
    val streakCount: Int,
    val todayWordsCompleted: Int,
    val totalXp: Int,
    val lastStudyDate: String,
    val isCurrentUser: Boolean,
    val isKakaoLinked: Boolean
)

data class FamilyLeaderboardSnapshot(
    val rankedMembers: List<RankedFamilyMember>,
    val unlinkedKakaoFriends: List<KakaoFriend>,
    val ownEntryId: String,
    val syncedAtLabel: String
)

fun rankFamilyLeaderboardEntries(
    entries: List<FamilyStreakEntry>,
    currentUserUid: String,
    friendNicknamesByKakaoId: Map<String, KakaoFriend> = emptyMap()
): List<RankedFamilyMember> {
    val dedupedByOwner = entries
        .groupBy { it.userId.ifBlank { it.id } }
        .values
        .map { group ->
            group.maxWithOrNull(
                compareBy<FamilyStreakEntry>(
                    { it.kakaoUserId.isNotBlank() },
                    { it.streakCount },
                    { it.todayWordsCompleted },
                    { it.totalXp }
                )
            ) ?: group.first()
        }

    val sorted = dedupedByOwner.sortedWith(
        compareByDescending<FamilyStreakEntry> { it.streakCount }
            .thenByDescending { it.todayWordsCompleted }
            .thenByDescending { it.totalXp }
            .thenBy { it.displayName.lowercase(Locale.ROOT) }
    )

    return sorted.mapIndexed { index, entry ->
        val kakaoFriendMeta = friendNicknamesByKakaoId[entry.kakaoUserId]
        val resolvedName = entry.displayName.ifBlank {
            kakaoFriendMeta?.nickname.orEmpty().ifBlank { "가족 멤버" }
        }
        val resolvedAvatar = KakaoAuthAndFriendsService.sanitizeHttpsUrl(
            entry.avatarUrl.ifBlank { kakaoFriendMeta?.thumbnailUrl.orEmpty() }
        )
        RankedFamilyMember(
            rank = index + 1,
            entryId = entry.id.ifBlank { entry.kakaoUserId.ifBlank { "uid_${entry.userId}" } },
            displayName = resolvedName,
            avatarUrl = resolvedAvatar,
            streakCount = entry.streakCount.coerceAtLeast(0),
            todayWordsCompleted = entry.todayWordsCompleted.coerceAtLeast(0),
            totalXp = entry.totalXp.coerceAtLeast(0),
            lastStudyDate = entry.lastStudyDate,
            isCurrentUser = entry.userId == currentUserUid && currentUserUid.isNotBlank(),
            isKakaoLinked = entry.kakaoUserId.startsWith("kakao_")
        )
    }
}

/**
 * Pure Cloudflare Workers + D1 backed Family Leaderboard Repository.
 */
class FamilyLeaderboardRepository(
    context: Context
) {
    private val appContext = context.applicationContext

    init {
        CloudflareAuthManager.init(appContext)
    }

    private fun requireUser(): CloudflareUser {
        return CloudflareAuthManager.currentUser
            ?: throw IllegalStateException("Google 로그인 후 Cloudflare 가족 리더보드를 이용할 수 있습니다.")
    }

    private fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun nowTimeLabel(): String {
        return SimpleDateFormat("HH:mm", Locale.KOREA).format(Date())
    }

    fun observeOwnStreakEntry(entryId: String): Flow<FamilyStreakEntry?> =
        localEntries.map { it[entryId] }

    suspend fun syncUserStreakAndKakaoFriends(
        displayName: String,
        avatarUrl: String,
        streakCount: Int,
        todayWordsCompleted: Int,
        totalXp: Int,
        coins: Int,
        hearts: Int,
        dailyGoal: Int,
        selectedGrade: Int,
        selectedTeacherId: String,
        lastStudyDate: String,
        kakaoUserId: String,
        connectedFriends: List<KakaoFriend>,
        shareWithFriends: Boolean
    ): Result<FamilyStreakEntry> {
        return try {
            val user = requireUser()
            val normalizedOwnKakaoId = KakaoAuthAndFriendsService.normalizeKakaoId(kakaoUserId)
            val normalizedFriendIds = connectedFriends
                .map { KakaoAuthAndFriendsService.normalizeKakaoId(it.kakaoId.ifBlank { it.uuid }) }
                .filter { it.startsWith("kakao_") && it != normalizedOwnKakaoId }
                .distinct()
                .take(30)

            val safeDisplayName = displayName.trim().ifBlank {
                user.displayName.trim().ifBlank { "가족 수호기사" }
            }.take(40)
            val safeAvatarUrl = KakaoAuthAndFriendsService.sanitizeHttpsUrl(avatarUrl)
            val ownEntryId = normalizedOwnKakaoId.ifBlank { "uid_${user.uid}" }
            val now = System.currentTimeMillis()

            val entry = FamilyStreakEntry(
                id = ownEntryId,
                userId = user.uid,
                kakaoUserId = normalizedOwnKakaoId,
                displayName = safeDisplayName,
                avatarUrl = safeAvatarUrl,
                streakCount = streakCount.coerceIn(0, 10000),
                todayWordsCompleted = todayWordsCompleted.coerceIn(0, 500),
                totalXp = totalXp.coerceIn(0, 10_000_000),
                lastStudyDate = lastStudyDate.trim().ifBlank { todayIsoDate() }.take(20),
                shareWithFriends = shareWithFriends,
                createdAt = now,
                updatedAt = now
            )

            localEntries.value = localEntries.value + (ownEntryId to entry)

            if (CloudflareConfig.isConfigured()) {
                CloudflareApiClient.syncFamilyStreak(
                    user = user,
                    entry = entry,
                    connectedKakaoIds = normalizedFriendIds
                )
            }

            Result.success(entry)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchKakaoFriendsLeaderboard(
        ownKakaoUserId: String,
        connectedFriends: List<KakaoFriend>,
        fallbackOwnEntry: FamilyStreakEntry? = null
    ): Result<FamilyLeaderboardSnapshot> {
        return try {
            val user = requireUser()
            val normalizedOwnKakaoId = KakaoAuthAndFriendsService.normalizeKakaoId(ownKakaoUserId)
            val ownEntryId = normalizedOwnKakaoId.ifBlank { "uid_${user.uid}" }

            val normalizedFriends = connectedFriends
                .mapNotNull { friend ->
                    val normalizedId = KakaoAuthAndFriendsService.normalizeKakaoId(
                        friend.kakaoId.ifBlank { friend.uuid }
                    )
                    if (normalizedId.startsWith("kakao_") && normalizedId != normalizedOwnKakaoId) {
                        friend.copy(kakaoId = normalizedId)
                    } else {
                        null
                    }
                }
                .distinctBy { it.kakaoId }
                .take(30)

            val friendMap = normalizedFriends.associateBy { it.kakaoId }
            val friendIds = normalizedFriends.map { it.kakaoId }

            val remoteEntries = if (CloudflareConfig.isConfigured()) {
                CloudflareApiClient.fetchFamilyLeaderboard(
                    user = user,
                    ownEntryId = ownEntryId,
                    friendKakaoIds = friendIds
                ).getOrNull()
            } else {
                null
            }

            val collectedEntries = mutableListOf<FamilyStreakEntry>()
            if (remoteEntries != null && remoteEntries.isNotEmpty()) {
                collectedEntries.addAll(remoteEntries)
            } else {
                val ownEntry = localEntries.value[ownEntryId] ?: fallbackOwnEntry
                if (ownEntry != null) {
                    collectedEntries.add(ownEntry)
                }
            }

            val linkedFriendIds = collectedEntries.map { it.kakaoUserId }.toSet()
            val unlinkedFriends = normalizedFriends.filterNot { it.kakaoId in linkedFriendIds }

            val ranked = rankFamilyLeaderboardEntries(
                entries = collectedEntries,
                currentUserUid = user.uid,
                friendNicknamesByKakaoId = friendMap
            )

            Result.success(
                FamilyLeaderboardSnapshot(
                    rankedMembers = ranked,
                    unlinkedKakaoFriends = unlinkedFriends,
                    ownEntryId = ownEntryId,
                    syncedAtLabel = nowTimeLabel()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOwnFamilyStreakEntries(ownKakaoUserId: String): Result<Unit> {
        val user = CloudflareAuthManager.currentUser ?: return Result.success(Unit)
        val normalized = KakaoAuthAndFriendsService.normalizeKakaoId(ownKakaoUserId)
        val keysToRemove = buildSet {
            add("uid_${user.uid}")
            if (normalized.startsWith("kakao_")) add(normalized)
        }
        localEntries.value = localEntries.value - keysToRemove
        return CloudflareApiClient.deleteAllUserData(user)
    }

    companion object {
        private val localEntries = MutableStateFlow<Map<String, FamilyStreakEntry>>(emptyMap())
    }
}
