package com.example.data

import android.content.Context
import com.example.cloudflare.CloudflareApiClient
import com.example.cloudflare.CloudflareAuthManager
import com.example.cloudflare.CloudflareConfig
import com.example.cloudflare.CloudflareUser
import com.example.model.ScenarioProgress
import com.example.model.UserProgress
import com.example.model.WordMastery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure Cloudflare Workers + D1 backed UserProgressRepository.
 * - Maintains instant zero-latency StateFlow caches for UI responsiveness.
 * - Synchronizes reads/writes asynchronously with Cloudflare D1 endpoints when configured.
 */
class UserProgressRepository(
    context: Context
) {
    private val appContext = context.applicationContext

    init {
        CloudflareAuthManager.init(appContext)
    }

    private fun requireUser(): CloudflareUser {
        return CloudflareAuthManager.currentUser
            ?: throw IllegalStateException("Google 로그인 후 Cloudflare 클라우드 동기화를 사용할 수 있습니다.")
    }

    private fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun observeUserProgress(userId: String): Flow<UserProgress?> =
        cachedProgressByUser.map { it[userId] }

    fun observeWordMastery(userId: String): Flow<List<WordMastery>> =
        cachedMasteryByUser.map { it[userId].orEmpty() }

    fun observeScenarioProgress(userId: String): Flow<List<ScenarioProgress>> =
        cachedScenarioByUser.map { it[userId].orEmpty() }

    suspend fun ensureUserProfileInitialized(displayName: String?): Result<Unit> {
        val user = CloudflareAuthManager.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val today = todayIsoDate()
            val safeName = (displayName?.trim()?.takeIf { it.isNotBlank() } ?: user.displayName).take(80)

            val remote = if (CloudflareConfig.isConfigured()) {
                CloudflareApiClient.fetchUserState(user).getOrNull()
            } else {
                null
            }

            val baseProgress = remote?.first ?: cachedProgressByUser.value[user.uid] ?: UserProgress(
                id = user.uid,
                userId = user.uid,
                displayName = safeName,
                selectedGrade = 1,
                selectedTeacherId = "emma_nyc",
                dailyGoal = 30,
                streakCount = 1,
                totalXp = 0,
                coins = 150,
                hearts = 5,
                todayWordsCompleted = 0,
                lastStudyDate = today,
                unlockedItems = listOf("badge_starter"),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val updatedProgress = if (baseProgress.lastStudyDate != today) {
                baseProgress.copy(
                    displayName = safeName,
                    todayWordsCompleted = 0,
                    hearts = 5,
                    streakCount = (baseProgress.streakCount + 1).coerceAtMost(10000),
                    lastStudyDate = today,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                baseProgress.copy(displayName = safeName, updatedAt = System.currentTimeMillis())
            }

            cachedProgressByUser.value = cachedProgressByUser.value + (user.uid to updatedProgress)
            if (remote != null) {
                cachedMasteryByUser.value = cachedMasteryByUser.value + (user.uid to remote.second)
                cachedScenarioByUser.value = cachedScenarioByUser.value + (user.uid to remote.third)
            }

            if (CloudflareConfig.isConfigured()) {
                CloudflareApiClient.upsertUserProgress(user, updatedProgress)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        selectedGrade: Int? = null,
        selectedTeacherId: String? = null,
        dailyGoal: Int? = null
    ): Result<Unit> {
        return try {
            val user = requireUser()
            val cur = cachedProgressByUser.value[user.uid] ?: UserProgress(id = user.uid, userId = user.uid, displayName = user.displayName)
            val next = cur.copy(
                selectedGrade = selectedGrade?.coerceIn(1, 3) ?: cur.selectedGrade,
                selectedTeacherId = selectedTeacherId?.takeIf { it.isNotBlank() }?.take(40) ?: cur.selectedTeacherId,
                dailyGoal = dailyGoal?.coerceIn(30, 120) ?: cur.dailyGoal,
                updatedAt = System.currentTimeMillis()
            )
            cachedProgressByUser.value = cachedProgressByUser.value + (user.uid to next)
            CloudflareApiClient.upsertUserProgress(user, next)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun awardMissionRewards(
        xpDelta: Int,
        coinsDelta: Int,
        wordsCompletedDelta: Int = 0,
        newHearts: Int? = null
    ): Result<Unit> {
        return try {
            val user = requireUser()
            val cur = cachedProgressByUser.value[user.uid] ?: UserProgress(id = user.uid, userId = user.uid, displayName = user.displayName)
            val next = cur.copy(
                totalXp = (cur.totalXp + xpDelta).coerceIn(0, 10_000_000),
                coins = (cur.coins + coinsDelta).coerceIn(0, 10_000_000),
                todayWordsCompleted = (cur.todayWordsCompleted + wordsCompletedDelta).coerceIn(0, 500),
                hearts = newHearts?.coerceIn(0, 5) ?: cur.hearts,
                lastStudyDate = todayIsoDate(),
                updatedAt = System.currentTimeMillis()
            )
            cachedProgressByUser.value = cachedProgressByUser.value + (user.uid to next)
            CloudflareApiClient.upsertUserProgress(user, next)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purchaseCollectibleItem(itemId: String, costCoins: Int): Result<Unit> {
        return try {
            val user = requireUser()
            require(itemId.isNotBlank())
            val cur = cachedProgressByUser.value[user.uid] ?: UserProgress(id = user.uid, userId = user.uid, displayName = user.displayName)
            if (itemId in cur.unlockedItems) return Result.success(Unit)
            if (cur.coins < costCoins) {
                return Result.failure(IllegalStateException("코인이 부족합니다! 미션을 완료해 코인을 모아보세요."))
            }
            val next = cur.copy(
                coins = (cur.coins - costCoins).coerceAtLeast(0),
                unlockedItems = (cur.unlockedItems + itemId).distinct().take(30),
                updatedAt = System.currentTimeMillis()
            )
            cachedProgressByUser.value = cachedProgressByUser.value + (user.uid to next)
            CloudflareApiClient.upsertUserProgress(user, next)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refillHeartsWithCoins(costCoins: Int = 50): Result<Unit> {
        return try {
            val user = requireUser()
            val cur = cachedProgressByUser.value[user.uid] ?: UserProgress(id = user.uid, userId = user.uid, displayName = user.displayName)
            if (cur.coins < costCoins) {
                return Result.failure(IllegalStateException("하트 충전에 ${costCoins} 코인이 필요합니다."))
            }
            val next = cur.copy(
                coins = (cur.coins - costCoins).coerceAtLeast(0),
                hearts = 5,
                updatedAt = System.currentTimeMillis()
            )
            cachedProgressByUser.value = cachedProgressByUser.value + (user.uid to next)
            CloudflareApiClient.upsertUserProgress(user, next)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordWordMastery(
        wordId: String,
        grade: Int,
        passed: Boolean,
        speakingScore: Int
    ): Result<Unit> {
        return try {
            val user = requireUser()
            require(wordId.isNotBlank())
            val curList = cachedMasteryByUser.value[user.uid].orEmpty()
            val existing = curList.firstOrNull { it.wordId == wordId }
            val clampedScore = speakingScore.coerceIn(0, 100)
            val clampedGrade = grade.coerceIn(1, 3)
            val now = System.currentTimeMillis()
            val updated = if (existing == null) {
                WordMastery(
                    id = wordId,
                    wordId = wordId,
                    grade = clampedGrade,
                    correctCount = if (passed) 1 else 0,
                    missCount = if (passed) 0 else 1,
                    speakingBestScore = clampedScore,
                    mastered = passed && clampedScore >= 75,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                existing.copy(
                    grade = clampedGrade,
                    correctCount = (existing.correctCount + if (passed) 1 else 0).coerceAtMost(10000),
                    missCount = (existing.missCount + if (passed) 0 else 1).coerceAtMost(10000),
                    speakingBestScore = maxOf(existing.speakingBestScore, clampedScore),
                    mastered = existing.mastered || (passed && clampedScore >= 75),
                    updatedAt = now
                )
            }
            val nextList = curList.filterNot { it.wordId == wordId } + updated
            cachedMasteryByUser.value = cachedMasteryByUser.value + (user.uid to nextList)
            CloudflareApiClient.upsertWordMastery(user, updated)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordScenarioProgress(
        scenarioId: String,
        zoneId: Int,
        completedSentencesCount: Int,
        twistCleared: Boolean,
        fluencyScore: Int
    ): Result<Unit> {
        return try {
            val user = requireUser()
            require(scenarioId.isNotBlank())
            val curList = cachedScenarioByUser.value[user.uid].orEmpty()
            val existing = curList.firstOrNull { it.scenarioId == scenarioId }
            val safeZone = zoneId.coerceIn(1, 10)
            val safeSentences = completedSentencesCount.coerceIn(0, 30)
            val safeScore = fluencyScore.coerceIn(0, 100)
            val now = System.currentTimeMillis()
            val updated = if (existing == null) {
                ScenarioProgress(
                    id = scenarioId,
                    scenarioId = scenarioId,
                    zoneId = safeZone,
                    completedSentencesCount = safeSentences,
                    twistCleared = twistCleared,
                    bestFluencyScore = safeScore,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                existing.copy(
                    zoneId = safeZone,
                    completedSentencesCount = maxOf(existing.completedSentencesCount, safeSentences),
                    twistCleared = existing.twistCleared || twistCleared,
                    bestFluencyScore = maxOf(existing.bestFluencyScore, safeScore),
                    updatedAt = now
                )
            }
            val nextList = curList.filterNot { it.scenarioId == scenarioId } + updated
            cachedScenarioByUser.value = cachedScenarioByUser.value + (user.uid to nextList)
            CloudflareApiClient.upsertScenarioProgress(user, updated)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logStudySession(
        mode: String,
        teacherId: String,
        accuracyPercent: Int,
        wpm: Int,
        xpEarned: Int,
        coinsEarned: Int
    ): Result<Unit> {
        return try {
            val user = requireUser()
            val safeMode = if (mode in setOf("SPARTA_VOCAB", "TRAVEL_100", "AI_TUTOR")) mode else "AI_TUTOR"
            CloudflareApiClient.logStudySession(
                user = user,
                mode = safeMode,
                teacherId = teacherId.take(40).ifBlank { "emma_nyc" },
                accuracyPercent = accuracyPercent.coerceIn(0, 100),
                wpm = wpm.coerceIn(0, 400),
                xpEarned = xpEarned.coerceIn(0, 10000),
                coinsEarned = coinsEarned.coerceIn(0, 10000)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserCloudData(): Result<Unit> {
        val user = CloudflareAuthManager.currentUser ?: return Result.success(Unit)
        cachedProgressByUser.value = cachedProgressByUser.value - user.uid
        cachedMasteryByUser.value = cachedMasteryByUser.value - user.uid
        cachedScenarioByUser.value = cachedScenarioByUser.value - user.uid
        return CloudflareApiClient.deleteAllUserData(user)
    }

    companion object {
        private val cachedProgressByUser = MutableStateFlow<Map<String, UserProgress>>(emptyMap())
        private val cachedMasteryByUser = MutableStateFlow<Map<String, List<WordMastery>>>(emptyMap())
        private val cachedScenarioByUser = MutableStateFlow<Map<String, List<ScenarioProgress>>>(emptyMap())
    }
}
