package com.example.engine

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Kotlin Native FSRS Spaced Repetition Scheduler + CSAT D-Day 3-Phase Guardrail Engine.
 *
 * - Implements the Free Spaced Repetition Scheduler (FSRS) stability (S), difficulty (D),
 *   and retrievability (R) equations natively in Kotlin with zero external JS/WebView overhead.
 * - Backward-compatible with existing SM-2 `WordLearningProgress` records in DataStore:
 *   records with `fsrsStabilityDays == 0.0` and `repetition > 0` are smoothly migrated from
 *   legacy `intervalDays` and `easeFactor`.
 * - Enforces D-Day 3-Phase study policy (BUILD > 40d, CONSOLIDATE 16..40d, LOCKDOWN <= 15d).
 */

enum class StudyPhase(
    val labelKo: String,
    val shortBadgeKo: String,
    val summaryKo: String
) {
    PHASE_1_BUILD(
        labelKo = "1단계 구축기 (D-41+)",
        shortBadgeKo = "구축기",
        summaryKo = "신규 하루 25개 · Tier S→A→B 확장 · 목표 기억률 90%"
    ),
    PHASE_2_CONSOLIDATE(
        labelKo = "2단계 정리·유지기 (D-40~16)",
        shortBadgeKo = "정리유지",
        summaryKo = "신규 하루 5개 제한 · 오답 편입 + 기존 1회독 · 목표 기억률 92%"
    ),
    PHASE_3_LOCKDOWN(
        labelKo = "3단계 락다운 (D-15~1)",
        shortBadgeKo = "락다운",
        summaryKo = "신규 투입 0개 전면 차단 · 오답 압축 + 핵심 어법 집중 · 목표 기억률 95%"
    )
}

data class DDayPolicy(
    val phase: StudyPhase,
    val daysUntilCsat: Int,
    val newCardsPerDay: Int,
    val desiredRetention: Double,
    val allowedTierCodes: List<String>,
    val maxIntervalDays: Int
)

object FsrsSchedulerEngine {
    private const val TEN_MINUTES_MS = 10L * 60L * 1000L
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    const val DEFAULT_DAYS_UNTIL_CSAT = 90

    // FSRS power-law forgetting curve constants: R(t, S) = (1 + FACTOR * t / S)^DECAY
    private const val DECAY = -0.5
    private const val FACTOR = 19.0 / 81.0

    // Standard FSRS default weights (w0..w18)
    private val W = doubleArrayOf(
        0.40255, 1.18385, 3.173, 15.69105, // w0..w3: initial stability for AGAIN/HARD/GOOD/EASY
        7.1949, 0.5345, 1.4604, 0.0046,    // w4..w7: initial & updated difficulty
        1.54575, 0.1192, 1.01925,          // w8..w10: recall stability growth
        1.9395, 0.11, 0.29605, 2.2698,     // w11..w14: lapse stability
        0.2315, 2.9898,                    // w15..w16: hard penalty & easy bonus
        0.51655, 0.6621                    // w17..w18: short-term stability
    )

    fun resolveDDayPolicy(daysUntilCsat: Int): DDayPolicy {
        val safeDays = daysUntilCsat.coerceIn(1, 365)
        return when {
            safeDays > 40 -> DDayPolicy(
                phase = StudyPhase.PHASE_1_BUILD,
                daysUntilCsat = safeDays,
                newCardsPerDay = 25,
                desiredRetention = 0.90,
                allowedTierCodes = listOf("TIER_S_CORE", "TIER_A_CYCLICAL", "TIER_B_ADVANCED_NEW"),
                maxIntervalDays = max(14, safeDays)
            )
            safeDays > 15 -> DDayPolicy(
                phase = StudyPhase.PHASE_2_CONSOLIDATE,
                daysUntilCsat = safeDays,
                newCardsPerDay = 5,
                desiredRetention = 0.92,
                allowedTierCodes = listOf("TIER_S_CORE", "TIER_A_CYCLICAL"),
                maxIntervalDays = max(14, safeDays)
            )
            else -> DDayPolicy(
                phase = StudyPhase.PHASE_3_LOCKDOWN,
                daysUntilCsat = safeDays,
                newCardsPerDay = 0,
                desiredRetention = 0.95,
                allowedTierCodes = listOf("TIER_S_CORE", "TIER_A_CYCLICAL"),
                maxIntervalDays = max(14, safeDays)
            )
        }
    }

    fun isNewWordAllowed(daysUntilCsat: Int, newWordsStudiedToday: Int): Boolean {
        val policy = resolveDDayPolicy(daysUntilCsat)
        return policy.newCardsPerDay > 0 && newWordsStudiedToday < policy.newCardsPerDay
    }

    /**
     * Computes current memory retrievability R in [0.0, 1.0] for an existing progress record.
     */
    fun retrievability(progress: WordLearningProgress, nowMs: Long = System.currentTimeMillis()): Double {
        val stability = resolveStability(progress)
        if (stability <= 0.0 || progress.lastReviewedAtMs <= 0L) return 1.0
        val elapsedDays = max(0.0, (nowMs - progress.lastReviewedAtMs).toDouble() / ONE_DAY_MS.toDouble())
        return (1.0 + FACTOR * (elapsedDays / stability)).pow(DECAY).coerceIn(0.0, 1.0)
    }

    /**
     * Applies one FSRS review grade and returns an updated `WordLearningProgress`
     * with both FSRS fields (`fsrsStabilityDays`, `fsrsDifficulty`, `desiredRetention`)
     * and legacy SM-2 compatible fields (`intervalDays`, `easeFactor`, `repetition`, `dueAtMs`, `learningStatus`).
     */
    fun review(
        existing: WordLearningProgress?,
        wordId: String,
        grade: RecallGrade,
        daysUntilCsat: Int = DEFAULT_DAYS_UNTIL_CSAT,
        nowMs: Long = System.currentTimeMillis()
    ): WordLearningProgress {
        val policy = resolveDDayPolicy(daysUntilCsat)
        val cur = existing ?: WordLearningProgress(wordId = wordId)
        val rating = when (grade) {
            RecallGrade.AGAIN -> 1
            RecallGrade.HARD -> 2
            RecallGrade.GOOD -> 3
            RecallGrade.EASY -> 4
        }

        val isFirstReview = cur.repetition == 0 && cur.fsrsStabilityDays <= 0.0 && cur.correctCount == 0 && cur.wrongCount == 0
        val prevS = resolveStability(cur)
        val prevD = resolveDifficulty(cur)

        val (newS, newD) = if (isFirstReview || prevS <= 0.0) {
            val s0 = W[rating - 1].coerceAtLeast(0.1)
            val d0 = initialDifficulty(rating)
            s0 to d0
        } else {
            val elapsedDays = if (cur.lastReviewedAtMs > 0L && nowMs > cur.lastReviewedAtMs) {
                (nowMs - cur.lastReviewedAtMs).toDouble() / ONE_DAY_MS.toDouble()
            } else {
                cur.intervalDays.coerceAtLeast(1).toDouble()
            }
            val r = (1.0 + FACTOR * (elapsedDays / prevS)).pow(DECAY).coerceIn(0.01, 1.0)
            val nextD = updateDifficulty(prevD, rating)
            val nextS = if (grade == RecallGrade.AGAIN) {
                nextForgetStability(nextD, prevS, r)
            } else {
                nextRecallStability(nextD, prevS, r, grade)
            }
            nextS to nextD
        }

        val compatibleEase = (3.4 - (newD - 1.0) * 0.22).coerceIn(1.3, 3.0)

        return if (grade == RecallGrade.AGAIN) {
            cur.copy(
                learningStatus = WordLearningStatus.LEARNING,
                lastRecallGrade = grade,
                repetition = 0,
                intervalDays = 0,
                easeFactor = compatibleEase,
                dueAtMs = nowMs + TEN_MINUTES_MS,
                lastReviewedAtMs = nowMs,
                lapseCount = cur.lapseCount + 1,
                consecutiveCorrect = 0,
                wrongCount = cur.wrongCount + 1,
                masteryScore = (cur.masteryScore - 20).coerceAtLeast(0),
                fsrsStabilityDays = round2(newS),
                fsrsDifficulty = round2(newD),
                desiredRetention = policy.desiredRetention
            )
        } else {
            val nextRep = cur.repetition + 1
            val computedInterval = nextIntervalDays(
                stabilityDays = newS,
                desiredRetention = policy.desiredRetention,
                maxIntervalDays = policy.maxIntervalDays
            )
            // Enforce monotonic intuitive spacing across HARD <= GOOD <= EASY on early reviews
            val finalInterval = when (grade) {
                RecallGrade.HARD -> computedInterval.coerceIn(1, policy.maxIntervalDays)
                RecallGrade.GOOD -> max(computedInterval, if (nextRep == 1) 1 else (cur.intervalDays + 1))
                    .coerceIn(1, policy.maxIntervalDays)
                RecallGrade.EASY -> max(computedInterval, if (nextRep == 1) 3 else (cur.intervalDays + 2))
                    .coerceIn(2, policy.maxIntervalDays)
                RecallGrade.AGAIN -> 0
            }

            val nextConsecutive = cur.consecutiveCorrect + 1
            val nextMastery = (cur.masteryScore + when (grade) {
                RecallGrade.HARD -> 14
                RecallGrade.GOOD -> 24
                RecallGrade.EASY -> 34
                RecallGrade.AGAIN -> 0
            }).coerceIn(0, 100)

            val nextStatus = when {
                finalInterval >= 7 && nextConsecutive >= 3 -> WordLearningStatus.MASTERED
                nextRep >= 2 -> WordLearningStatus.REVIEW
                else -> WordLearningStatus.LEARNING
            }

            cur.copy(
                learningStatus = nextStatus,
                lastRecallGrade = grade,
                repetition = nextRep,
                intervalDays = finalInterval,
                easeFactor = compatibleEase,
                dueAtMs = nowMs + finalInterval * ONE_DAY_MS,
                lastReviewedAtMs = nowMs,
                consecutiveCorrect = nextConsecutive,
                correctCount = cur.correctCount + 1,
                masteryScore = nextMastery,
                fsrsStabilityDays = round2(newS),
                fsrsDifficulty = round2(newD),
                desiredRetention = policy.desiredRetention
            )
        }
    }

    fun previewIntervalLabel(
        existing: WordLearningProgress?,
        wordId: String,
        grade: RecallGrade,
        daysUntilCsat: Int = DEFAULT_DAYS_UNTIL_CSAT,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        val next = review(
            existing = existing,
            wordId = wordId,
            grade = grade,
            daysUntilCsat = daysUntilCsat,
            nowMs = nowMs
        )
        return if (next.intervalDays <= 0) "10분" else "${next.intervalDays}일"
    }

    private fun resolveStability(progress: WordLearningProgress): Double {
        if (progress.fsrsStabilityDays > 0.0) return progress.fsrsStabilityDays
        // Seamless migration from legacy SM-2 records
        return if (progress.intervalDays > 0) {
            progress.intervalDays.toDouble()
        } else if (progress.repetition > 0) {
            1.5
        } else {
            0.0
        }
    }

    private fun resolveDifficulty(progress: WordLearningProgress): Double {
        if (progress.fsrsStabilityDays > 0.0) return progress.fsrsDifficulty.coerceIn(1.0, 10.0)
        // Map legacy SM-2 easeFactor [1.3 .. 2.8] to FSRS difficulty [1.0 .. 10.0]
        return (11.0 - 2.4 * progress.easeFactor).coerceIn(1.0, 10.0)
    }

    private fun initialDifficulty(rating: Int): Double {
        val raw = W[4] - exp(W[5] * (rating - 1)) + 1.0
        return raw.coerceIn(1.0, 10.0)
    }

    private fun updateDifficulty(prevD: Double, rating: Int): Double {
        val d0Good = initialDifficulty(3)
        val delta = -W[6] * (rating - 3)
        val next = prevD + delta * ((10.0 - prevD) / 9.0)
        val meanReverted = W[7] * d0Good + (1.0 - W[7]) * next
        return meanReverted.coerceIn(1.0, 10.0)
    }

    private fun nextRecallStability(d: Double, s: Double, r: Double, grade: RecallGrade): Double {
        val hardPenalty = if (grade == RecallGrade.HARD) W[15] else 1.0
        val easyBonus = if (grade == RecallGrade.EASY) W[16] else 1.0
        val growth = exp(W[8]) *
            (11.0 - d) *
            s.pow(-W[9]) *
            (exp((1.0 - r) * W[10]) - 1.0) *
            hardPenalty *
            easyBonus
        return max(s + 0.1, s * (1.0 + growth)).coerceAtMost(3650.0)
    }

    private fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val postLapse = W[11] *
            d.pow(-W[12]) *
            ((s + 1.0).pow(W[13]) - 1.0) *
            exp((1.0 - r) * W[14])
        return min(s, max(0.1, postLapse))
    }

    private fun nextIntervalDays(
        stabilityDays: Double,
        desiredRetention: Double,
        maxIntervalDays: Int
    ): Int {
        val clampedR = desiredRetention.coerceIn(0.70, 0.98)
        val raw = (stabilityDays / FACTOR) * (clampedR.pow(1.0 / DECAY) - 1.0)
        val safeDays = if (raw.isNaN() || raw.isInfinite()) 1 else raw.roundToInt()
        return safeDays.coerceIn(1, max(1, maxIntervalDays))
    }

    private fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
