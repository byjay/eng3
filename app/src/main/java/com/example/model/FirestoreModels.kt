package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Explorer",
    val selectedGrade: Int = 1,
    val selectedTeacherId: String = "emma_uk",
    val dailyGoal: Int = 30,
    val streakCount: Int = 1,
    val totalXp: Int = 0,
    val coins: Int = 100,
    val hearts: Int = 5,
    val todayWordsCompleted: Int = 0,
    val lastStudyDate: String = "",
    val unlockedItems: List<String> = listOf("badge_starter"),
    val kakaoUserId: String = "",
    val connectedKakaoIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyStreakEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kakaoUserId: String = "",
    val displayName: String = "Explorer",
    val avatarUrl: String = "",
    val streakCount: Int = 0,
    val todayWordsCompleted: Int = 0,
    val totalXp: Int = 0,
    val lastStudyDate: String = "",
    val shareWithFriends: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WordMastery(
    @DocumentId val id: String = "",
    val wordId: String = "",
    val grade: Int = 1,
    val correctCount: Int = 0,
    val missCount: Int = 0,
    val speakingBestScore: Int = 0,
    val mastered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScenarioProgress(
    @DocumentId val id: String = "",
    val scenarioId: String = "",
    val zoneId: Int = 1,
    val completedSentencesCount: Int = 0,
    val twistCleared: Boolean = false,
    val bestFluencyScore: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudySessionLog(
    @DocumentId val id: String = "",
    val mode: String = "SPARTA_VOCAB",
    val teacherId: String = "emma_uk",
    val accuracyPercent: Int = 0,
    val wpm: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)
