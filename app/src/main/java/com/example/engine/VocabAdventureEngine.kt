package com.example.engine

import kotlin.math.roundToInt

/**
 * Core v2 Master Spec Engine:
 * - P3: SM-2 Spaced Repetition Scheduler (AGAIN / HARD / GOOD / EASY)
 * - P4: Centralized RewardConfig, LevelConfig (Lv.1..Lv.7), and Idempotent Reward Ledger
 * - P5: Rule-Based Daily & Weekly Mission State Machine (IN_PROGRESS -> COMPLETED -> CLAIMED)
 * - Section 5.3: 2D Memory Palace Rooms (집 · 학교 · 여행 · 자연)
 * - Section 13: Privacy-safe Learning Analytics Event Ring Buffer
 */

enum class WordLearningStatus {
    NEW,
    LEARNING,
    REVIEW,
    MASTERED,
    SUSPENDED
}

enum class RecallGrade(val labelKo: String, val shortHintKo: String) {
    AGAIN("다시", "10분 뒤"),
    HARD("어려움", "1일 뒤"),
    GOOD("좋음", "간격 확장"),
    EASY("쉬움", "장기 복습")
}

data class WordLearningProgress(
    val wordId: String,
    val learningStatus: WordLearningStatus = WordLearningStatus.NEW,
    val lastRecallGrade: RecallGrade? = null,
    val repetition: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val dueAtMs: Long = 0L,
    val lastReviewedAtMs: Long = 0L,
    val lapseCount: Int = 0,
    val consecutiveCorrect: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val masteryScore: Int = 0 // 0..100
)

object Sm2ReviewScheduler {
    private const val TEN_MINUTES_MS = 10L * 60L * 1000L
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val MIN_EASE = 1.3

    fun review(
        existing: WordLearningProgress?,
        wordId: String,
        grade: RecallGrade,
        nowMs: Long = System.currentTimeMillis()
    ): WordLearningProgress {
        val cur = existing ?: WordLearningProgress(wordId = wordId)
        val quality = when (grade) {
            RecallGrade.AGAIN -> 1
            RecallGrade.HARD -> 3
            RecallGrade.GOOD -> 4
            RecallGrade.EASY -> 5
        }

        val nextEase = (cur.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))
            .coerceAtLeast(MIN_EASE)

        return if (grade == RecallGrade.AGAIN) {
            cur.copy(
                learningStatus = WordLearningStatus.LEARNING,
                lastRecallGrade = grade,
                repetition = 0,
                intervalDays = 0,
                easeFactor = (cur.easeFactor - 0.2).coerceAtLeast(MIN_EASE),
                dueAtMs = nowMs + TEN_MINUTES_MS,
                lastReviewedAtMs = nowMs,
                lapseCount = cur.lapseCount + 1,
                consecutiveCorrect = 0,
                wrongCount = cur.wrongCount + 1,
                masteryScore = (cur.masteryScore - 20).coerceAtLeast(0)
            )
        } else {
            val nextRep = cur.repetition + 1
            val rawInterval = when {
                nextRep == 1 -> if (grade == RecallGrade.EASY) 3 else 1
                nextRep == 2 -> when (grade) {
                    RecallGrade.HARD -> 2
                    RecallGrade.GOOD -> 3
                    RecallGrade.EASY -> 7
                    else -> 1
                }
                else -> {
                    val mult = when (grade) {
                        RecallGrade.HARD -> 1.2
                        RecallGrade.GOOD -> nextEase
                        RecallGrade.EASY -> nextEase * 1.3
                        else -> 1.0
                    }
                    (cur.intervalDays.coerceAtLeast(1) * mult).roundToInt().coerceAtLeast(cur.intervalDays + 1)
                }
            }.coerceAtMost(180)

            val nextConsecutive = cur.consecutiveCorrect + 1
            val nextMastery = (cur.masteryScore + when (grade) {
                RecallGrade.HARD -> 14
                RecallGrade.GOOD -> 24
                RecallGrade.EASY -> 34
                else -> 0
            }).coerceIn(0, 100)

            val nextStatus = when {
                nextIntervalMastered(rawInterval, nextConsecutive) -> WordLearningStatus.MASTERED
                nextRep >= 2 -> WordLearningStatus.REVIEW
                else -> WordLearningStatus.LEARNING
            }

            cur.copy(
                learningStatus = nextStatus,
                lastRecallGrade = grade,
                repetition = nextRep,
                intervalDays = rawInterval,
                easeFactor = nextEase,
                dueAtMs = nowMs + rawInterval * ONE_DAY_MS,
                lastReviewedAtMs = nowMs,
                consecutiveCorrect = nextConsecutive,
                correctCount = cur.correctCount + 1,
                masteryScore = nextMastery
            )
        }
    }

    private fun nextIntervalMastered(intervalDays: Int, consecutiveCorrect: Int): Boolean =
        intervalDays >= 7 && consecutiveCorrect >= 3

    fun previewIntervalLabel(existing: WordLearningProgress?, wordId: String, grade: RecallGrade): String {
        val next = review(existing, wordId, grade, nowMs = 0L)
        return if (next.intervalDays <= 0) "10분" else "${next.intervalDays}일"
    }

    fun dueWordIds(progressMap: Map<String, WordLearningProgress>, nowMs: Long = System.currentTimeMillis()): List<String> =
        progressMap.values
            .filter { it.learningStatus != WordLearningStatus.SUSPENDED && it.dueAtMs > 0L && it.dueAtMs <= nowMs }
            .sortedBy { it.dueAtMs }
            .map { it.wordId }

    fun queueSnapshot(
        progressMap: Map<String, WordLearningProgress>,
        weakWordIds: Set<String>,
        nowMs: Long = System.currentTimeMillis()
    ): ReviewQueueSnapshot {
        val endOfDayMs = nowMs + ONE_DAY_MS
        var dueNow = 0
        var overdue = 0
        var todayScheduled = 0
        progressMap.values.forEach { p ->
            if (p.learningStatus == WordLearningStatus.SUSPENDED || p.dueAtMs <= 0L) return@forEach
            if (p.dueAtMs <= nowMs) {
                dueNow += 1
                if (nowMs - p.dueAtMs >= ONE_DAY_MS) overdue += 1
            } else if (p.dueAtMs <= endOfDayMs) {
                todayScheduled += 1
            }
        }
        return ReviewQueueSnapshot(
            dueNowCount = dueNow,
            overdueCount = overdue,
            todayScheduledCount = todayScheduled,
            weakCount = weakWordIds.size
        )
    }

    /**
     * Toggles a word between SUSPENDED (excluded from due reviews) and REVIEW/NEW (Spec Section 9.1).
     */
    fun toggleSuspended(
        existing: WordLearningProgress?,
        wordId: String,
        nowMs: Long = System.currentTimeMillis()
    ): WordLearningProgress {
        val cur = existing ?: WordLearningProgress(wordId = wordId)
        return if (cur.learningStatus == WordLearningStatus.SUSPENDED) {
            cur.copy(
                learningStatus = if (cur.repetition >= 2) WordLearningStatus.REVIEW else WordLearningStatus.LEARNING,
                lastReviewedAtMs = nowMs
            )
        } else {
            cur.copy(
                learningStatus = WordLearningStatus.SUSPENDED,
                lastReviewedAtMs = nowMs
            )
        }
    }
}

// ==================== SECTION 3.3 & 12: TYPED ACTIVE RECALL WITH TYPO TOLERANCE ====================

enum class TypedRecallOutcome {
    EXACT_MATCH,
    TYPO_ACCEPTED,
    INCORRECT
}

data class TypedRecallResult(
    val outcome: TypedRecallOutcome,
    val isPassed: Boolean,
    val editDistance: Int,
    val feedbackKo: String
)

object TypedRecallEvaluator {
    fun evaluate(rawInput: String, targetWord: String): TypedRecallResult {
        val input = rawInput.trim().lowercase().replace(Regex("[^a-z'-]"), "")
        val target = targetWord.trim().lowercase().replace(Regex("[^a-z'-]"), "")
        if (input.isEmpty() || target.isEmpty()) {
            return TypedRecallResult(
                outcome = TypedRecallOutcome.INCORRECT,
                isPassed = false,
                editDistance = target.length,
                feedbackKo = "영단어 철자를 입력해 주세요."
            )
        }
        if (input == target) {
            return TypedRecallResult(
                outcome = TypedRecallOutcome.EXACT_MATCH,
                isPassed = true,
                editDistance = 0,
                feedbackKo = "완벽한 철자입니다! '$targetWord' (+8 XP · +3 코인)"
            )
        }
        val dist = levenshtein(input, target)
        // Allow 1-character typo tolerance only for words of length >= 5 (Spec Section 12)
        if (target.length >= 5 && dist == 1) {
            return TypedRecallResult(
                outcome = TypedRecallOutcome.TYPO_ACCEPTED,
                isPassed = true,
                editDistance = 1,
                feedbackKo = "거의 완벽해요! (오타 1자 허용 · 정답: $targetWord)"
            )
        }
        return TypedRecallResult(
            outcome = TypedRecallOutcome.INCORRECT,
            isPassed = false,
            editDistance = dist,
            feedbackKo = "아쉬워요! 첫 글자 '${target.first().uppercase()}' (${target.length}글자)를 떠올려 보세요."
        )
    }

    internal fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost
                )
            }
            System.arraycopy(curr, 0, prev, 0, b.length + 1)
        }
        return prev[b.length]
    }
}

data class ReviewQueueSnapshot(
    val dueNowCount: Int = 0,
    val overdueCount: Int = 0,
    val todayScheduledCount: Int = 0,
    val weakCount: Int = 0
)

// ==================== P4: REWARD CONFIG, LEVEL CONFIG & IDEMPOTENT LEDGER ====================

enum class RewardEventType {
    NEW_WORD_LEARNED,
    MNEMONIC_RECALLED,
    ACTIVE_RECALL_CORRECT,
    TYPING_SPELLING_CORRECT,
    LISTENING_QUIZ_CORRECT,
    DAILY_REVIEW_COMPLETED,
    TEN_WORD_SESSION_COMPLETED,
    DAILY_MISSION_CLAIMED,
    WEEKLY_MISSION_CLAIMED,
    MINIGAME_COMPLETED
}

data class RewardSpec(val xp: Int, val coins: Int)

/**
 * Single centralized balancing table (Spec Section 6.2).
 */
object RewardConfig {
    private val table: Map<RewardEventType, RewardSpec> = mapOf(
        RewardEventType.NEW_WORD_LEARNED to RewardSpec(xp = 5, coins = 1),
        RewardEventType.MNEMONIC_RECALLED to RewardSpec(xp = 2, coins = 0),
        RewardEventType.ACTIVE_RECALL_CORRECT to RewardSpec(xp = 5, coins = 2),
        RewardEventType.TYPING_SPELLING_CORRECT to RewardSpec(xp = 8, coins = 3),
        RewardEventType.LISTENING_QUIZ_CORRECT to RewardSpec(xp = 6, coins = 2),
        RewardEventType.DAILY_REVIEW_COMPLETED to RewardSpec(xp = 10, coins = 4),
        RewardEventType.TEN_WORD_SESSION_COMPLETED to RewardSpec(xp = 15, coins = 5),
        RewardEventType.DAILY_MISSION_CLAIMED to RewardSpec(xp = 20, coins = 8),
        RewardEventType.WEEKLY_MISSION_CLAIMED to RewardSpec(xp = 60, coins = 20),
        RewardEventType.MINIGAME_COMPLETED to RewardSpec(xp = 12, coins = 5)
    )

    fun specFor(type: RewardEventType): RewardSpec = table[type] ?: RewardSpec(0, 0)
}

data class VocabLevelTier(
    val level: Int,
    val titleKo: String,
    val minXp: Int,
    val nextLevelXp: Int
)

object LevelConfig {
    val tiers: List<VocabLevelTier> = listOf(
        VocabLevelTier(1, "단어 탐험가", 0, 200),
        VocabLevelTier(2, "기억 수집가", 200, 500),
        VocabLevelTier(3, "이미지 연상가", 500, 950),
        VocabLevelTier(4, "어휘 사냥꾼", 950, 1550),
        VocabLevelTier(5, "문장 마스터", 1550, 2350),
        VocabLevelTier(6, "기억의 건축가", 2350, 3400),
        VocabLevelTier(7, "단어 챔피언", 3400, 5000)
    )

    fun resolveTier(totalXp: Int): VocabLevelTier {
        val safeXp = totalXp.coerceAtLeast(0)
        return tiers.lastOrNull { safeXp >= it.minXp } ?: tiers.first()
    }

    fun progressToNext(totalXp: Int): Float {
        val tier = resolveTier(totalXp)
        if (tier.level >= tiers.last().level) return 1f
        val span = (tier.nextLevelXp - tier.minXp).coerceAtLeast(1)
        return ((totalXp - tier.minXp).toFloat() / span).coerceIn(0f, 1f)
    }
}

data class RewardTransaction(
    val transactionId: String,
    val eventId: String,
    val eventType: RewardEventType,
    val xpDelta: Int,
    val coinDelta: Int,
    val reason: String,
    val wordId: String = "",
    val missionId: String = "",
    val sessionId: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

data class LedgerGrantResult(
    val granted: Boolean,
    val duplicateBlocked: Boolean,
    val newTotalXp: Int,
    val newCoinBalance: Int,
    val leveledUpTo: VocabLevelTier? = null,
    val transaction: RewardTransaction? = null
)

object RewardLedgerEngine {
    /**
     * Pure deterministic check ensuring idempotency by eventId and non-negative balances.
     */
    fun applyTransaction(
        processedEventIds: Set<String>,
        currentXp: Int,
        currentCoins: Int,
        transaction: RewardTransaction
    ): LedgerGrantResult {
        if (transaction.eventId.isBlank() || transaction.eventId in processedEventIds) {
            return LedgerGrantResult(
                granted = false,
                duplicateBlocked = true,
                newTotalXp = currentXp,
                newCoinBalance = currentCoins
            )
        }
        val nextXp = (currentXp + transaction.xpDelta).coerceAtLeast(0)
        val nextCoins = (currentCoins + transaction.coinDelta).coerceAtLeast(0)
        val oldLevel = LevelConfig.resolveTier(currentXp)
        val newLevel = LevelConfig.resolveTier(nextXp)
        return LedgerGrantResult(
            granted = true,
            duplicateBlocked = false,
            newTotalXp = nextXp,
            newCoinBalance = nextCoins,
            leveledUpTo = if (newLevel.level > oldLevel.level) newLevel else null,
            transaction = transaction
        )
    }

    /**
     * Safe coin spend guard preventing negative balances (Spec Section 6.4 & 15).
     */
    fun spendCoins(currentCoins: Int, cost: Int): Pair<Boolean, Int> {
        if (cost < 0 || currentCoins < cost) return false to currentCoins.coerceAtLeast(0)
        return true to (currentCoins - cost).coerceAtLeast(0)
    }
}

// ==================== P5: DAILY & WEEKLY MISSION STATE MACHINE ====================

enum class MissionKind {
    DAILY_NEW_WORDS,
    DAILY_MNEMONIC_RECALL,
    DAILY_WRONG_RETRY,
    DAILY_LISTENING_SPEAK,
    DAILY_REVIEW_DUE,
    WEEKLY_UNIQUE_WORDS,
    WEEKLY_LISTENING_QUIZ,
    WEEKLY_PALACE_ROOMS,
    STORY_TRAVEL_WORDS,
    STORY_EMOTION_WORDS,
    STORY_VERIFIED_ROOTS
}

enum class MissionCadence(val labelKo: String) {
    DAILY("오늘의 퀘스트"),
    WEEKLY("주간 탐험"),
    EXPLORATION("테마 탐험")
}

enum class MissionStatus {
    LOCKED,
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED,
    CLAIMED,
    EXPIRED
}

data class LearningMission(
    val missionId: String,
    val kind: MissionKind,
    val titleKo: String,
    val descriptionKo: String,
    val targetCount: Int,
    val currentCount: Int = 0,
    val rewardXp: Int,
    val rewardCoins: Int,
    val status: MissionStatus = MissionStatus.AVAILABLE,
    val dateKey: String = "",
    val claimedAtMs: Long = 0L,
    val cadence: MissionCadence = MissionCadence.DAILY
) {
    val progressFraction: Float
        get() = (currentCount.toFloat() / targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)
}

object DailyMissionEngine {

    fun defaultMissionsForDate(dateKey: String, dailyWordTarget: Int = 10): List<LearningMission> = listOf(
        LearningMission(
            missionId = "m_daily_new_$dateKey",
            kind = MissionKind.DAILY_NEW_WORDS,
            titleKo = "오늘 새 단어 ${dailyWordTarget}개 배우기",
            descriptionKo = "이미지 연상 카드로 새 단어를 학습하고 회상해요",
            targetCount = dailyWordTarget.coerceIn(5, 30),
            rewardXp = 20,
            rewardCoins = 8,
            status = MissionStatus.AVAILABLE,
            dateKey = dateKey,
            cadence = MissionCadence.DAILY
        ),
        LearningMission(
            missionId = "m_daily_mnemonic_$dateKey",
            kind = MissionKind.DAILY_MNEMONIC_RECALL,
            titleKo = "기억 장면 5개 확인 후 회상",
            descriptionKo = "연상 장면을 보고 뜻을 가린 채 떠올려 보세요",
            targetCount = 5,
            rewardXp = 15,
            rewardCoins = 5,
            status = MissionStatus.AVAILABLE,
            dateKey = dateKey,
            cadence = MissionCadence.DAILY
        ),
        LearningMission(
            missionId = "m_daily_review_$dateKey",
            kind = MissionKind.DAILY_REVIEW_DUE,
            titleKo = "복습·오답 단어 5개 재도전",
            descriptionKo = "기억이 흐릿해진 단어를 다시 꺼내 정복해요",
            targetCount = 5,
            rewardXp = 18,
            rewardCoins = 6,
            status = MissionStatus.AVAILABLE,
            dateKey = dateKey,
            cadence = MissionCadence.DAILY
        ),
        LearningMission(
            missionId = "m_daily_audio_$dateKey",
            kind = MissionKind.DAILY_LISTENING_SPEAK,
            titleKo = "영어 발음 5회 듣고 말하기",
            descriptionKo = "원어민 음성을 듣고 따라 말하며 소리로 기억해요",
            targetCount = 5,
            rewardXp = 15,
            rewardCoins = 5,
            status = MissionStatus.AVAILABLE,
            dateKey = dateKey,
            cadence = MissionCadence.DAILY
        ),
        LearningMission(
            missionId = "m_weekly_words_${dateKey.take(7)}",
            kind = MissionKind.WEEKLY_UNIQUE_WORDS,
            titleKo = "주간 목표: 단어 30회 정복",
            descriptionKo = "이번 주 누적 단어 회상과 미니게임으로 탐험을 완주해요",
            targetCount = 30,
            rewardXp = 60,
            rewardCoins = 20,
            status = MissionStatus.AVAILABLE,
            dateKey = dateKey.take(7),
            cadence = MissionCadence.WEEKLY
        ),
        LearningMission(
            missionId = "m_weekly_palace_${dateKey.take(7)}",
            kind = MissionKind.WEEKLY_PALACE_ROOMS,
            titleKo = "기억의 궁전 3단어 배치",
            descriptionKo = "집·학교·여행·자연 공간에 나만의 연상 메모를 남겨요",
            targetCount = 3,
            rewardXp = 40,
            rewardCoins = 12,
            status = MissionStatus.AVAILABLE,
            dateKey = dateKey.take(7),
            cadence = MissionCadence.WEEKLY
        ),
        LearningMission(
            missionId = "m_story_travel_pack",
            kind = MissionKind.STORY_TRAVEL_WORDS,
            titleKo = "테마 탐험: 여행·일상 핵심 5단어",
            descriptionKo = "해외 공항·호텔·카페 상황 단어를 회상으로 정복해요",
            targetCount = 5,
            rewardXp = 35,
            rewardCoins = 10,
            status = MissionStatus.AVAILABLE,
            dateKey = "story",
            cadence = MissionCadence.EXPLORATION
        ),
        LearningMission(
            missionId = "m_story_emotion_pack",
            kind = MissionKind.STORY_EMOTION_WORDS,
            titleKo = "테마 탐험: 감정·마음 표현 5단어",
            descriptionKo = "감정·심리 어휘를 장면 연상으로 생생하게 익혀요",
            targetCount = 5,
            rewardXp = 35,
            rewardCoins = 10,
            status = MissionStatus.AVAILABLE,
            dateKey = "story",
            cadence = MissionCadence.EXPLORATION
        ),
        LearningMission(
            missionId = "m_story_roots_pack",
            kind = MissionKind.STORY_VERIFIED_ROOTS,
            titleKo = "어근 탐험: 검증 어근 단어 5개 정복",
            descriptionKo = "접두사·어근 구조가 검증된 필수 어휘 5개를 마스터해요",
            targetCount = 5,
            rewardXp = 45,
            rewardCoins = 14,
            status = MissionStatus.AVAILABLE,
            dateKey = "story",
            cadence = MissionCadence.EXPLORATION
        )
    )

    /**
     * Rolls over DAILY missions on a new date while preserving WEEKLY (same YYYY-MM)
     * and EXPLORATION (persistent story) mission progress, and adapts daily word goal.
     */
    fun rolloverMissions(
        existing: List<LearningMission>,
        todayDateKey: String,
        dailyWordTarget: Int = 10
    ): List<LearningMission> {
        val freshDefaults = defaultMissionsForDate(todayDateKey, dailyWordTarget)
        if (existing.isEmpty()) return freshDefaults
        val monthKey = todayDateKey.take(7)
        val existingByKind = existing.associateBy { it.kind }

        return freshDefaults.map { template ->
            val prev = existingByKind[template.kind]
            when {
                prev == null -> template
                template.cadence == MissionCadence.DAILY && prev.dateKey == todayDateKey -> {
                    if (template.kind == MissionKind.DAILY_NEW_WORDS && prev.targetCount != template.targetCount) {
                        val safeCurrent = prev.currentCount.coerceAtMost(template.targetCount)
                        val updatedStatus = when {
                            prev.status == MissionStatus.CLAIMED -> MissionStatus.CLAIMED
                            safeCurrent >= template.targetCount -> MissionStatus.COMPLETED
                            safeCurrent > 0 -> MissionStatus.IN_PROGRESS
                            else -> MissionStatus.AVAILABLE
                        }
                        prev.copy(
                            titleKo = template.titleKo,
                            targetCount = template.targetCount,
                            currentCount = safeCurrent,
                            status = updatedStatus
                        )
                    } else {
                        prev
                    }
                }
                template.cadence == MissionCadence.WEEKLY && prev.dateKey == monthKey -> prev
                template.cadence == MissionCadence.EXPLORATION -> prev
                else -> template
            }
        }
    }

    fun recordProgress(
        missions: List<LearningMission>,
        kind: MissionKind,
        delta: Int = 1
    ): List<LearningMission> {
        if (delta <= 0) return missions
        return missions.map { m ->
            if (m.kind != kind || m.status == MissionStatus.CLAIMED || m.status == MissionStatus.EXPIRED || m.status == MissionStatus.LOCKED) {
                m
            } else {
                val nextCount = (m.currentCount + delta).coerceAtMost(m.targetCount)
                val nextStatus = if (nextCount >= m.targetCount) MissionStatus.COMPLETED else MissionStatus.IN_PROGRESS
                m.copy(currentCount = nextCount, status = nextStatus)
            }
        }
    }

    /**
     * Separates COMPLETED from CLAIMED and prevents duplicate reward claims.
     */
    fun claimMission(
        missions: List<LearningMission>,
        missionId: String,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<List<LearningMission>, LearningMission?> {
        var claimed: LearningMission? = null
        val updated = missions.map { m ->
            if (m.missionId == missionId && m.status == MissionStatus.COMPLETED) {
                val done = m.copy(status = MissionStatus.CLAIMED, claimedAtMs = nowMs)
                claimed = done
                done
            } else {
                m
            }
        }
        return updated to claimed
    }
}

// ==================== SECTION 5.3: 2D MEMORY PALACE ====================

enum class MemoryPalaceRoom(val titleKo: String, val subtitleKo: String, val accentHex: Long) {
    HOME_ROOM("집 · 방과 현관", "일상과 감정 단어를 배치하는 아늑한 공간", 0xFFEA580C),
    SCHOOL_LIBRARY("학교 · 도서관", "수능·논리 핵심 단어를 정리하는 서가", 0xFF2563EB),
    TRAVEL_AIRPORT("여행 · 공항과 카페", "여행 회화·상황 단어를 기억하는 게이트", 0xFF0D9488),
    NATURE_FOREST("자연 · 숲과 바다", "변화·지속·과학 개념을 떠올리는 풍경", 0xFF148F68)
}

data class MemoryPalaceEntry(
    val wordId: String,
    val room: MemoryPalaceRoom,
    val personalMemoKo: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

// ==================== SECTION 13: LEARNING ANALYTICS EVENTS ====================

data class VocabAnalyticsRecord(
    val eventName: String,
    val wordId: String = "",
    val meta: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)

data class VocabAnalyticsSummary(
    val cardsViewed: Int,
    val mnemonicsViewed: Int,
    val pronunciationPlayed: Int,
    val recallCorrect: Int,
    val recallWrong: Int,
    val typingPassed: Int,
    val missionsClaimed: Int,
    val duplicateRewardsBlocked: Int
) {
    val recallAccuracyPercent: Int
        get() {
            val total = recallCorrect + recallWrong
            return if (total <= 0) 0 else ((recallCorrect * 100f) / total).roundToInt()
        }
}

object VocabAnalyticsTracker {
    private val records = ArrayDeque<VocabAnalyticsRecord>()
    private const val MAX_RECORDS = 200

    @Synchronized
    fun log(eventName: String, wordId: String = "", meta: String = "") {
        records.addLast(VocabAnalyticsRecord(eventName = eventName, wordId = wordId, meta = meta))
        while (records.size > MAX_RECORDS) {
            records.removeFirst()
        }
    }

    @Synchronized
    fun countByEvent(eventName: String): Int = records.count { it.eventName == eventName }

    @Synchronized
    fun recent(limit: Int = 20): List<VocabAnalyticsRecord> = records.takeLast(limit)

    @Synchronized
    fun summary(): VocabAnalyticsSummary = VocabAnalyticsSummary(
        cardsViewed = records.count { it.eventName == "word_card_viewed" },
        mnemonicsViewed = records.count { it.eventName == "mnemonic_viewed" },
        pronunciationPlayed = records.count { it.eventName == "pronunciation_played" },
        recallCorrect = records.count { it.eventName == "recall_correct" },
        recallWrong = records.count { it.eventName == "recall_wrong" },
        typingPassed = records.count { it.eventName == "typing_correct" },
        missionsClaimed = records.count { it.eventName == "mission_claimed" },
        duplicateRewardsBlocked = records.count { it.eventName == "duplicate_reward_blocked" }
    )

    @Synchronized
    fun clearForTesting() {
        records.clear()
    }
}

// ==================== SECTION 10.1: ACHIEVEMENT BADGES ====================

data class AchievementBadge(
    val id: String,
    val titleKo: String,
    val criteriaKo: String,
    val unlocked: Boolean,
    val progressLabel: String,
    val accentHex: Long
)

object AchievementBadgeEngine {
    fun resolveBadges(
        masteredCount: Int,
        streakCount: Int,
        palaceCount: Int,
        claimedMissionsCount: Int,
        totalXp: Int,
        sm2ReviewCount: Int
    ): List<AchievementBadge> = listOf(
        AchievementBadge(
            id = "badge_first_10",
            titleKo = "첫 10단어 정복",
            criteriaKo = "단어 10개 누적 마스터",
            unlocked = masteredCount >= 10,
            progressLabel = "${masteredCount.coerceAtMost(10)}/10",
            accentHex = 0xFF2563EB
        ),
        AchievementBadge(
            id = "badge_sm2_guardian",
            titleKo = "간격 복습 수호자",
            criteriaKo = "SM-2 단어 복습 15회",
            unlocked = sm2ReviewCount >= 15,
            progressLabel = "${sm2ReviewCount.coerceAtMost(15)}/15",
            accentHex = 0xFF0D9488
        ),
        AchievementBadge(
            id = "badge_palace_architect",
            titleKo = "기억의 궁전 건축가",
            criteriaKo = "기억의 궁전에 5단어 배치",
            unlocked = palaceCount >= 5,
            progressLabel = "${palaceCount.coerceAtMost(5)}/5",
            accentHex = 0xFF7C3AED
        ),
        AchievementBadge(
            id = "badge_quest_hunter",
            titleKo = "퀘스트 완주자",
            criteriaKo = "일일·주간·탐험 미션 3회 수령",
            unlocked = claimedMissionsCount >= 3,
            progressLabel = "${claimedMissionsCount.coerceAtMost(3)}/3",
            accentHex = 0xFFEA580C
        ),
        AchievementBadge(
            id = "badge_streak_7",
            titleKo = "7일 연속 학습 전설",
            criteriaKo = "연속 학습 스트릭 7일 달성",
            unlocked = streakCount >= 7,
            progressLabel = "${streakCount.coerceAtMost(7)}/7일",
            accentHex = 0xFFE11D48
        ),
        AchievementBadge(
            id = "badge_xp_champion",
            titleKo = "어휘 마스터 Lv.5",
            criteriaKo = "누적 XP 1,550 달성",
            unlocked = totalXp >= 1550,
            progressLabel = "${totalXp.coerceAtMost(1550)}/1550",
            accentHex = 0xFFEAB308
        )
    )
}

// ==================== ADVENTURER GEAR, ACHIEVEMENT BADGES & 3-BIOME JOURNEY MAP ====================

data class ExplorerGearItem(
    val id: String,
    val titleKo: String,
    val unlockHintKo: String,
    val unlocked: Boolean,
    val progressText: String,
    val accentHex: Long
)

data class JourneyBiomeNode(
    val biomeId: String,
    val titleKo: String,
    val subtitleKo: String,
    val requiredXp: Int,
    val cleared: Boolean,
    val isCurrent: Boolean,
    val accentHex: Long
)

data class QuestCelebrationEvent(
    val titleKo: String,
    val xpEarned: Int,
    val coinsEarned: Int,
    val streakCount: Int,
    val leveledUpTitleKo: String? = null
)

object ExplorerJourneyEngine {
    fun resolveGearItems(
        totalXp: Int,
        masteredCount: Int,
        streakCount: Int,
        palaceCount: Int,
        claimedMissionsCount: Int
    ): List<ExplorerGearItem> = listOf(
        ExplorerGearItem(
            id = "gear_hood",
            titleKo = "모험가 후드",
            unlockHintKo = "단어 5개 암기 달성",
            unlocked = masteredCount >= 5 || totalXp >= 200,
            progressText = "${masteredCount.coerceAtMost(5)}/5",
            accentHex = 0xFF148F68
        ),
        ExplorerGearItem(
            id = "gear_satchel",
            titleKo = "여행자 배낭",
            unlockHintKo = "오늘 미션 1회 완료",
            unlocked = claimedMissionsCount >= 1 || totalXp >= 350,
            progressText = "${claimedMissionsCount.coerceAtMost(1)}/1",
            accentHex = 0xFFEA580C
        ),
        ExplorerGearItem(
            id = "gear_lantern",
            titleKo = "기억의 등불",
            unlockHintKo = "기억의 궁전 3단어 배치",
            unlocked = palaceCount >= 3,
            progressText = "${palaceCount.coerceAtMost(3)}/3",
            accentHex = 0xFFEAB308
        ),
        ExplorerGearItem(
            id = "gear_compass",
            titleKo = "어휘 나침반",
            unlockHintKo = "연속 학습 3일 또는 Lv.3",
            unlocked = streakCount >= 3 || totalXp >= 500,
            progressText = if (streakCount >= 3 || totalXp >= 500) "해금됨" else "${streakCount}/3일",
            accentHex = 0xFF2563EB
        )
    )

    fun resolveJourneyBiomes(totalXp: Int): List<JourneyBiomeNode> {
        val specs = listOf(
            Triple("biome_frost", "설산 어휘 봉우리" to "기초·이미지 연상 캠프", 0 to 0xFF0EA5E9),
            Triple("biome_desert", "피라미드 기억 사막" to "어근·문맥 회상 오아시스", 500 to 0xFFD97706),
            Triple("biome_citadel", "어둠의 보스 성채" to "수능·회화 마스터 최종 결전", 1550 to 0xFF7C3AED)
        )
        val currentIdx = specs.indexOfLast { totalXp >= it.second.first.let { _ -> it.third.first } }.coerceAtLeast(0)
        return specs.mapIndexed { idx, (id, labels, reqAndAccent) ->
            JourneyBiomeNode(
                biomeId = id,
                titleKo = labels.first,
                subtitleKo = labels.second,
                requiredXp = reqAndAccent.first,
                cleared = idx < currentIdx || (idx == specs.lastIndex && totalXp >= 3400),
                isCurrent = idx == currentIdx,
                accentHex = reqAndAccent.second
            )
        }
    }
}
