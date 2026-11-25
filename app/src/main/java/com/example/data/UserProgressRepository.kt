package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.ScenarioProgress
import com.example.model.StudySessionLog
import com.example.model.UserProgress
import com.example.model.WordMastery
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserProgressRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun observeUserProgress(userId: String): Flow<UserProgress?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProgress::class.java,
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
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeWordMastery(userId: String): Flow<List<WordMastery>> = flow {
        val path = "users/$userId/wordMastery"
        emitAll(
            db.collection("users").document(userId).collection("wordMastery")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WordMastery::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeScenarioProgress(userId: String): Flow<List<ScenarioProgress>> = flow {
        val path = "users/$userId/scenarioProgress"
        emitAll(
            db.collection("users").document(userId).collection("scenarioProgress")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ScenarioProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfileInitialized(displayName: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val safeName = (displayName?.trim()?.takeIf { it.isNotBlank() } ?: "Explorer").take(80)
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "selectedGrade" to 1,
                    "selectedTeacherId" to "emma_uk",
                    "dailyGoal" to 30,
                    "streakCount" to 1,
                    "totalXp" to 0,
                    "coins" to 150,
                    "hearts" to 5,
                    "todayWordsCompleted" to 0,
                    "lastStudyDate" to todayIsoDate(),
                    "unlockedItems" to listOf("badge_starter"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val lastDate = snap.getString("lastStudyDate") ?: ""
                val today = todayIsoDate()
                if (lastDate != today) {
                    val prevStreak = (snap.getLong("streakCount") ?: 0L).toInt()
                    docRef.update(
                        mapOf(
                            "todayWordsCompleted" to 0,
                            "hearts" to 5,
                            "streakCount" to (prevStreak + 1).coerceAtMost(10000),
                            "lastStudyDate" to today,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        selectedGrade: Int? = null,
        selectedTeacherId: String? = null,
        dailyGoal: Int? = null
    ): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (selectedGrade != null) {
                updates["selectedGrade"] = selectedGrade.coerceIn(1, 3)
            }
            if (selectedTeacherId != null && selectedTeacherId.isNotBlank()) {
                updates["selectedTeacherId"] = selectedTeacherId.take(40)
            }
            if (dailyGoal != null) {
                updates["dailyGoal"] = dailyGoal.coerceIn(30, 120)
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun awardMissionRewards(
        xpDelta: Int,
        coinsDelta: Int,
        wordsCompletedDelta: Int = 0,
        newHearts: Int? = null
    ): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val curXp = (snap.getLong("totalXp") ?: 0L).toInt()
            val curCoins = (snap.getLong("coins") ?: 0L).toInt()
            val curWords = (snap.getLong("todayWordsCompleted") ?: 0L).toInt()
            val updates = mutableMapOf<String, Any>(
                "totalXp" to (curXp + xpDelta).coerceIn(0, 10_000_000),
                "coins" to (curCoins + coinsDelta).coerceIn(0, 10_000_000),
                "todayWordsCompleted" to (curWords + wordsCompletedDelta).coerceIn(0, 500),
                "lastStudyDate" to todayIsoDate(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newHearts != null) {
                updates["hearts"] = newHearts.coerceIn(0, 5)
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun purchaseCollectibleItem(itemId: String, costCoins: Int): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            require(itemId.isNotBlank())
            path = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val currentCoins = (snap.getLong("coins") ?: 0L).toInt()
            @Suppress("UNCHECKED_CAST")
            val currentItems = (snap.get("unlockedItems") as? List<*>)
                ?.filterIsInstance<String>()
                ?: emptyList()
            if (itemId in currentItems) return Result.success(Unit)
            if (currentCoins < costCoins) {
                return Result.failure(IllegalStateException("코인이 부족합니다! 미션을 완료해 코인을 모아보세요."))
            }
            val updatedItems = (currentItems + itemId).distinct().take(30)
            docRef.update(
                mapOf(
                    "coins" to (currentCoins - costCoins).coerceAtLeast(0),
                    "unlockedItems" to updatedItems,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun refillHeartsWithCoins(costCoins: Int = 50): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val currentCoins = (snap.getLong("coins") ?: 0L).toInt()
            if (currentCoins < costCoins) {
                return Result.failure(IllegalStateException("하트 충전에 ${costCoins} 코인이 필요합니다."))
            }
            docRef.update(
                mapOf(
                    "coins" to (currentCoins - costCoins).coerceAtLeast(0),
                    "hearts" to 5,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordWordMastery(
        wordId: String,
        grade: Int,
        passed: Boolean,
        speakingScore: Int
    ): Result<Unit> {
        var path = "users/unauthenticated/wordMastery/$wordId"
        return try {
            val uid = requireUserId()
            require(wordId.isNotBlank())
            val docRef = db.collection("users").document(uid)
                .collection("wordMastery").document(wordId)
            path = docRef.path
            val snap = docRef.get().await()
            val clampedScore = speakingScore.coerceIn(0, 100)
            val clampedGrade = grade.coerceIn(1, 3)
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "wordId" to wordId,
                        "grade" to clampedGrade,
                        "correctCount" to if (passed) 1 else 0,
                        "missCount" to if (passed) 0 else 1,
                        "speakingBestScore" to clampedScore,
                        "mastered" to (passed && clampedScore >= 75),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val prevCorrect = (snap.getLong("correctCount") ?: 0L).toInt()
                val prevMiss = (snap.getLong("missCount") ?: 0L).toInt()
                val prevBest = (snap.getLong("speakingBestScore") ?: 0L).toInt()
                val prevMastered = snap.getBoolean("mastered") ?: false
                docRef.update(
                    mapOf(
                        "grade" to clampedGrade,
                        "correctCount" to (prevCorrect + if (passed) 1 else 0).coerceAtMost(10000),
                        "missCount" to (prevMiss + if (passed) 0 else 1).coerceAtMost(10000),
                        "speakingBestScore" to maxOf(prevBest, clampedScore),
                        "mastered" to (prevMastered || (passed && clampedScore >= 75)),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
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
        var path = "users/unauthenticated/scenarioProgress/$scenarioId"
        return try {
            val uid = requireUserId()
            require(scenarioId.isNotBlank())
            val docRef = db.collection("users").document(uid)
                .collection("scenarioProgress").document(scenarioId)
            path = docRef.path
            val snap = docRef.get().await()
            val safeZone = zoneId.coerceIn(1, 10)
            val safeSentences = completedSentencesCount.coerceIn(0, 30)
            val safeScore = fluencyScore.coerceIn(0, 100)
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "scenarioId" to scenarioId,
                        "zoneId" to safeZone,
                        "completedSentencesCount" to safeSentences,
                        "twistCleared" to twistCleared,
                        "bestFluencyScore" to safeScore,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val prevCount = (snap.getLong("completedSentencesCount") ?: 0L).toInt()
                val prevTwist = snap.getBoolean("twistCleared") ?: false
                val prevBest = (snap.getLong("bestFluencyScore") ?: 0L).toInt()
                docRef.update(
                    mapOf(
                        "zoneId" to safeZone,
                        "completedSentencesCount" to maxOf(prevCount, safeSentences),
                        "twistCleared" to (prevTwist || twistCleared),
                        "bestFluencyScore" to maxOf(prevBest, safeScore),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
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
        var path = "users/unauthenticated/sessions"
        return try {
            val uid = requireUserId()
            val safeMode = if (mode in setOf("SPARTA_VOCAB", "TRAVEL_100", "AI_TUTOR")) mode else "AI_TUTOR"
            val docRef = db.collection("users").document(uid).collection("sessions").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "mode" to safeMode,
                    "teacherId" to teacherId.take(40).ifBlank { "emma_uk" },
                    "accuracyPercent" to accuracyPercent.coerceIn(0, 100),
                    "wpm" to wpm.coerceIn(0, 400),
                    "xpEarned" to xpEarned.coerceIn(0, 10000),
                    "coinsEarned" to coinsEarned.coerceIn(0, 10000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserCloudData(): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            val userDocRef = db.collection("users").document(uid)
            path = userDocRef.path

            val subCollections = listOf("wordMastery", "scenarioProgress", "sessions")
            for (subName in subCollections) {
                val snap = userDocRef.collection(subName).get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            userDocRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
