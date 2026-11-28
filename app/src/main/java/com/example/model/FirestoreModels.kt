package com.example.model

import androidx.annotation.Keep

@Keep
data class UserProgress(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "Explorer",
    val selectedGrade: Int = 1,
    val selectedTeacherId: String = "emma_nyc",
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
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
data class FamilyStreakEntry(
    val id: String = "",
    val userId: String = "",
    val kakaoUserId: String = "",
    val displayName: String = "Explorer",
    val avatarUrl: String = "",
    val streakCount: Int = 0,
    val todayWordsCompleted: Int = 0,
    val totalXp: Int = 0,
    val lastStudyDate: String = "",
    val shareWithFriends: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
data class WordMastery(
    val id: String = "",
    val wordId: String = "",
    val grade: Int = 1,
    val correctCount: Int = 0,
    val missCount: Int = 0,
    val speakingBestScore: Int = 0,
    val mastered: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
data class ScenarioProgress(
    val id: String = "",
    val scenarioId: String = "",
    val zoneId: Int = 1,
    val completedSentencesCount: Int = 0,
    val twistCleared: Boolean = false,
    val bestFluencyScore: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
data class StudySessionLog(
    val id: String = "",
    val mode: String = "SPARTA_VOCAB",
    val teacherId: String = "emma_nyc",
    val accuracyPercent: Int = 0,
    val wpm: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0,
    val createdAt: Long = 0L
)
