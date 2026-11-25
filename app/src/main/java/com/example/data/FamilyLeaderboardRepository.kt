package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.FamilyStreakEntry
import com.example.model.KakaoFriend
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
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

class FamilyLeaderboardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google 로그인 후 가족 리더보드를 이용할 수 있습니다.")
    }

    private fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun nowTimeLabel(): String {
        return SimpleDateFormat("HH:mm", Locale.KOREA).format(Date())
    }

    fun observeOwnStreakEntry(entryId: String): Flow<FamilyStreakEntry?> = flow {
        val uid = auth.currentUser?.uid ?: run {
            emit(null)
            return@flow
        }
        val path = "familyStreaks/$entryId"
        emitAll(
            db.collection("familyStreaks").document(entryId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FamilyStreakEntry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

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
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val normalizedOwnKakaoId = KakaoAuthAndFriendsService.normalizeKakaoId(kakaoUserId)
            val normalizedFriendIds = connectedFriends
                .map { KakaoAuthAndFriendsService.normalizeKakaoId(it.kakaoId.ifBlank { it.uuid }) }
                .filter { it.startsWith("kakao_") && it != normalizedOwnKakaoId }
                .distinct()
                .take(30)

            val safeDisplayName = displayName.trim().ifBlank {
                auth.currentUser?.displayName?.trim().orEmpty().ifBlank { "가족 수호기사" }
            }.take(40)
            val safeAvatarUrl = KakaoAuthAndFriendsService.sanitizeHttpsUrl(avatarUrl)
            val safeStreak = streakCount.coerceIn(0, 10000)
            val safeTodayWords = todayWordsCompleted.coerceIn(0, 500)
            val safeTotalXp = totalXp.coerceIn(0, 10_000_000)
            val safeCoins = coins.coerceIn(0, 10_000_000)
            val safeHearts = hearts.coerceIn(0, 5)
            val safeDailyGoal = dailyGoal.coerceIn(30, 120)
            val safeGrade = selectedGrade.coerceIn(1, 3)
            val safeTeacher = selectedTeacherId.trim().ifBlank { "emma_uk" }.take(40)
            val safeLastDate = lastStudyDate.trim().ifBlank { todayIsoDate() }.take(20)

            // 1. Ensure /users/{uid} exists and stores normalized Kakao user + connected friend IDs
            val userDocRef = db.collection("users").document(uid)
            path = userDocRef.path
            val userSnap = userDocRef.get().await()
            if (!userSnap.exists()) {
                val createUserPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeDisplayName,
                    "selectedGrade" to safeGrade,
                    "selectedTeacherId" to safeTeacher,
                    "dailyGoal" to safeDailyGoal,
                    "streakCount" to safeStreak,
                    "totalXp" to safeTotalXp,
                    "coins" to safeCoins,
                    "hearts" to safeHearts,
                    "todayWordsCompleted" to safeTodayWords,
                    "lastStudyDate" to safeLastDate,
                    "unlockedItems" to listOf("badge_starter"),
                    "kakaoUserId" to normalizedOwnKakaoId,
                    "connectedKakaoIds" to normalizedFriendIds,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(createUserPayload).await()
            } else {
                val updateUserPayload = mapOf(
                    "displayName" to safeDisplayName,
                    "streakCount" to safeStreak,
                    "totalXp" to safeTotalXp,
                    "todayWordsCompleted" to safeTodayWords,
                    "lastStudyDate" to safeLastDate,
                    "kakaoUserId" to normalizedOwnKakaoId,
                    "connectedKakaoIds" to normalizedFriendIds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.update(updateUserPayload).await()
            }

            // 2. Upsert /familyStreaks/{ownEntryId} preserving immutable userId & createdAt
            val ownEntryId = normalizedOwnKakaoId.ifBlank { "uid_$uid" }
            val entryDocRef = db.collection("familyStreaks").document(ownEntryId)
            path = entryDocRef.path
            val existingEntrySnap = entryDocRef.get().await()

            if (!existingEntrySnap.exists()) {
                val createEntryPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "kakaoUserId" to normalizedOwnKakaoId,
                    "displayName" to safeDisplayName,
                    "streakCount" to safeStreak,
                    "todayWordsCompleted" to safeTodayWords,
                    "totalXp" to safeTotalXp,
                    "lastStudyDate" to safeLastDate,
                    "shareWithFriends" to shareWithFriends,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeAvatarUrl.isNotBlank()) {
                    createEntryPayload["avatarUrl"] = safeAvatarUrl
                }
                entryDocRef.set(createEntryPayload).await()
            } else {
                val updateEntryPayload = mapOf(
                    "kakaoUserId" to normalizedOwnKakaoId,
                    "displayName" to safeDisplayName,
                    "avatarUrl" to safeAvatarUrl,
                    "streakCount" to safeStreak,
                    "todayWordsCompleted" to safeTodayWords,
                    "totalXp" to safeTotalXp,
                    "lastStudyDate" to safeLastDate,
                    "shareWithFriends" to shareWithFriends,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                entryDocRef.update(updateEntryPayload).await()
            }

            Result.success(
                FamilyStreakEntry(
                    id = ownEntryId,
                    userId = uid,
                    kakaoUserId = normalizedOwnKakaoId,
                    displayName = safeDisplayName,
                    avatarUrl = safeAvatarUrl,
                    streakCount = safeStreak,
                    todayWordsCompleted = safeTodayWords,
                    totalXp = safeTotalXp,
                    lastStudyDate = safeLastDate,
                    shareWithFriends = shareWithFriends
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchKakaoFriendsLeaderboard(
        ownKakaoUserId: String,
        connectedFriends: List<KakaoFriend>,
        fallbackOwnEntry: FamilyStreakEntry? = null
    ): Result<FamilyLeaderboardSnapshot> {
        var path = "familyStreaks/unauthenticated"
        return try {
            val uid = requireUserId()
            val normalizedOwnKakaoId = KakaoAuthAndFriendsService.normalizeKakaoId(ownKakaoUserId)
            val ownEntryId = normalizedOwnKakaoId.ifBlank { "uid_$uid" }

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
            val collectedEntries = mutableListOf<FamilyStreakEntry>()
            val unlinkedFriends = mutableListOf<KakaoFriend>()

            // Fetch own entry
            path = "familyStreaks/$ownEntryId"
            val ownSnap = db.collection("familyStreaks").document(ownEntryId).get().await()
            val ownEntry = if (ownSnap.exists()) {
                ownSnap.toObject(
                    FamilyStreakEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                fallbackOwnEntry
            }
            if (ownEntry != null) {
                collectedEntries.add(ownEntry)
            }

            // Fetch connected Kakao friends' entries concurrently via strict point reads
            val friendResults = coroutineScope {
                normalizedFriends.map { friend ->
                    async {
                        val friendPath = "familyStreaks/${friend.kakaoId}"
                        try {
                            val snap = db.collection("familyStreaks")
                                .document(friend.kakaoId)
                                .get()
                                .await()
                            if (snap.exists()) {
                                friend to snap.toObject(
                                    FamilyStreakEntry::class.java,
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )
                            } else {
                                friend to null
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // Friend may have opted out (shareWithFriends == false); log and skip gracefully
                            handleFirestoreError(e, OperationType.GET, friendPath)
                            friend to null
                        }
                    }
                }.awaitAll()
            }

            friendResults.forEach { (friend, entry) ->
                if (entry != null) {
                    collectedEntries.add(entry)
                } else {
                    unlinkedFriends.add(friend)
                }
            }

            val ranked = rankFamilyLeaderboardEntries(
                entries = collectedEntries,
                currentUserUid = uid,
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
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun deleteOwnFamilyStreakEntries(ownKakaoUserId: String): Result<Unit> {
        var path = "familyStreaks/unauthenticated"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            val candidateIds = buildSet {
                add("uid_$uid")
                val normalized = KakaoAuthAndFriendsService.normalizeKakaoId(ownKakaoUserId)
                if (normalized.startsWith("kakao_")) add(normalized)
            }
            for (entryId in candidateIds) {
                path = "familyStreaks/$entryId"
                val ref = db.collection("familyStreaks").document(entryId)
                val snap = ref.get().await()
                if (snap.exists() && snap.getString("userId") == uid) {
                    ref.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
