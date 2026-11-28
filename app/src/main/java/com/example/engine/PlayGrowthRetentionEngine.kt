package com.example.engine

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Commercial Google Play growth, retention, and personalization engine:
 * - Weekly League Tiers (Bronze -> Master) for competitive progression
 * - Streak Freeze Shield protection (prevents Day-1 miss churn)
 * - Peak-happiness Google Play rating prompt eligibility (14-day cooldown, Play-policy compliant)
 * - First-launch learner goal tracks & system/light/dark theme modes
 */
enum class AppThemeMode(val labelKo: String) {
    SYSTEM("시스템"),
    LIGHT("라이트"),
    DARK("다크");

    fun next(): AppThemeMode = when (this) {
        SYSTEM -> DARK
        DARK -> LIGHT
        LIGHT -> SYSTEM
    }

    companion object {
        fun fromStored(raw: String?): AppThemeMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}

enum class LearnerGoalTrack(
    val titleKo: String,
    val subtitleKo: String,
    val badgeKo: String,
    val accentHex: Long
) {
    CSAT_EXAM(
        titleKo = "수능·내신 1등급 단어",
        subtitleKo = "빈출 어휘 + 3초 스피드 콤보 정복",
        badgeKo = "수능·내신",
        accentHex = 0xFF4F6EF7L
    ),
    TRAVEL_SPEAKING(
        titleKo = "해외여행 실전 회화",
        subtitleKo = "360° 현지 거리뷰 + AI 원어민 대화",
        badgeKo = "여행 회화",
        accentHex = 0xFF0EA5E9L
    ),
    DAILY_HABIT(
        titleKo = "매일 3분 영어 습관",
        subtitleKo = "출퇴근·등하교 3분 퀴즈로 스트릭 유지",
        badgeKo = "3분 루틴",
        accentHex = 0xFF10B981L
    ),
    FAMILY_CHALLENGE(
        titleKo = "가족·친구 스트릭 대결",
        subtitleKo = "함께 성장하는 리더보드 & 보상 챌린지",
        badgeKo = "가족 챌린지",
        accentHex = 0xFFF59E0BL
    );

    companion object {
        fun fromStored(raw: String?): LearnerGoalTrack =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY_HABIT
    }
}

enum class LeagueTier(
    val rankIndex: Int,
    val titleKo: String,
    val minScore: Int,
    val accentHex: Long,
    val badgeSymbol: String
) {
    BRONZE(0, "브론즈 리그", 0, 0xFFB45309L, "B"),
    SILVER(1, "실버 리그", 400, 0xFF64748BL, "S"),
    GOLD(2, "골드 리그", 1000, 0xFFD97706L, "G"),
    EMERALD(3, "에메랄드 리그", 2000, 0xFF059669L, "E"),
    DIAMOND(4, "다이아몬드 리그", 3600, 0xFF0284C7L, "D"),
    MASTER(5, "마스터 리그", 6000, 0xFF7C3AEDL, "M")
}

data class LeagueStanding(
    val currentTier: LeagueTier,
    val nextTier: LeagueTier?,
    val leagueScore: Int,
    val progressToNext: Float,
    val pointsRemainingToNext: Int
)

data class StreakTransitionResult(
    val nextStreak: Int,
    val remainingFreezes: Int,
    val freezeConsumed: Boolean,
    val gapDays: Int
)

object PlayGrowthRetentionEngine {
    const val STREAK_FREEZE_COST_COINS: Int = 150
    const val MAX_STREAK_FREEZES: Int = 3
    const val REVIEW_COOLDOWN_MS: Long = 14L * 24L * 60L * 60L * 1000L
    const val ONBOARDING_BONUS_XP: Int = 50
    const val ONBOARDING_BONUS_COINS: Int = 30

    /**
     * Computes deterministic league tier & progress from user XP, streak, and mastered word count.
     */
    fun computeLeagueStanding(
        totalXp: Int,
        streakCount: Int,
        masteredWordsCount: Int
    ): LeagueStanding {
        val safeXp = totalXp.coerceAtLeast(0)
        val safeStreak = streakCount.coerceAtLeast(0)
        val safeMastered = masteredWordsCount.coerceAtLeast(0)
        val score = safeXp + (safeStreak * 45) + (safeMastered * 12)

        val tiers = LeagueTier.entries
        val current = tiers.lastOrNull { score >= it.minScore } ?: LeagueTier.BRONZE
        val next = tiers.getOrNull(current.rankIndex + 1)

        return if (next == null) {
            LeagueStanding(
                currentTier = current,
                nextTier = null,
                leagueScore = score,
                progressToNext = 1f,
                pointsRemainingToNext = 0
            )
        } else {
            val span = (next.minScore - current.minScore).coerceAtLeast(1)
            val earned = (score - current.minScore).coerceIn(0, span)
            LeagueStanding(
                currentTier = current,
                nextTier = next,
                leagueScore = score,
                progressToNext = (earned.toFloat() / span.toFloat()).coerceIn(0f, 1f),
                pointsRemainingToNext = (next.minScore - score).coerceAtLeast(0)
            )
        }
    }

    /**
     * Evaluates daily streak rollover with Streak Freeze Shield auto-protection:
     * - Gap <= 1 day: streak stays intact.
     * - Gap == 2 days (missed exactly 1 calendar day) and freeze >= 1: consumes 1 freeze & preserves streak!
     * - Gap >= 2 days without freeze (or gap > 2): resets streak to 0 gracefully.
     */
    fun resolveStreakTransition(
        lastStudyDateIso: String,
        todayIso: String,
        currentStreak: Int,
        streakFreezes: Int
    ): StreakTransitionResult {
        val safeStreak = currentStreak.coerceAtLeast(0)
        val safeFreezes = streakFreezes.coerceIn(0, MAX_STREAK_FREEZES)
        val gap = daysBetweenIso(lastStudyDateIso, todayIso)

        if (safeStreak == 0 || gap <= 1) {
            return StreakTransitionResult(
                nextStreak = safeStreak,
                remainingFreezes = safeFreezes,
                freezeConsumed = false,
                gapDays = gap
            )
        }
        if (gap == 2 && safeFreezes > 0) {
            return StreakTransitionResult(
                nextStreak = safeStreak,
                remainingFreezes = safeFreezes - 1,
                freezeConsumed = true,
                gapDays = gap
            )
        }
        return StreakTransitionResult(
            nextStreak = 0,
            remainingFreezes = safeFreezes,
            freezeConsumed = false,
            gapDays = gap
        )
    }

    /**
     * Play-policy compliant peak-happiness rating prompt trigger:
     * Only prompts after real learning achievement + 14-day cooldown, never on first launch.
     */
    fun shouldTriggerPlayReviewPrompt(
        onboardingCompleted: Boolean,
        hasDismissedPermanently: Boolean,
        lastPromptedAtMs: Long,
        nowMs: Long,
        todayWordsCompleted: Int,
        dailyGoal: Int,
        streakCount: Int,
        bossVictories: Int
    ): Boolean {
        if (!onboardingCompleted || hasDismissedPermanently) return false
        if (lastPromptedAtMs > 0L && (nowMs - lastPromptedAtMs) < REVIEW_COOLDOWN_MS) return false
        val safeGoal = dailyGoal.coerceAtLeast(5)
        val achievedPeakMoment = todayWordsCompleted >= safeGoal || streakCount >= 3 || bossVictories >= 1
        return achievedPeakMoment
    }

    fun daysBetweenIso(startIso: String, endIso: String): Int {
        if (startIso.isBlank() || endIso.isBlank() || startIso == endIso) return 0
        return runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            val startMs = fmt.parse(startIso)?.time ?: return 0
            val endMs = fmt.parse(endIso)?.time ?: return 0
            val diffDays = ((endMs - startMs) / (24L * 60L * 60L * 1000L)).toInt()
            if (diffDays < 0) 0 else abs(diffDays)
        }.getOrDefault(0)
    }
}
