package com.example.engine

import com.example.model.VocabWord
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 4-mode sensory variation inside each Boss Revenge micro-session to eliminate quiz monotony.
 */
enum class RevengeQuestionMode(val badgeKo: String, val instructionKo: String) {
    SPEED_MEANING("스피드 매칭", "영단어의 정확한 한국어 뜻을 고르세요"),
    REVERSE_WORD("역방향 영작", "한국어 뜻에 맞는 영단어를 선택하세요"),
    CLOZE_CONTEXT("문맥 빈칸", "문맥 속 빈칸(_____)에 들어갈 단어를 고르세요"),
    LISTENING_SENSE("리스닝 감각", "발음을 듣고 알맞은 뜻을 빠르게 맞히세요")
}

enum class RewardChestRarity(
    val labelKo: String,
    val bonusXp: Int,
    val tierIndex: Int
) {
    COMMON("브론즈 보급함", 40, 0),
    RARE("실버 아케인 상자", 85, 1),
    EPIC("골드 챔피언 상자", 150, 2),
    LEGENDARY("다이아 레전드 보물함", 260, 3)
}

data class RevengeBossProfile(
    val id: String,
    val nameKo: String,
    val titleKo: String,
    val maxHp: Int
)

data class RevengeEncounterQuestion(
    val index: Int,
    val mode: RevengeQuestionMode,
    val targetWord: VocabWord,
    val promptPrimary: String,
    val promptSecondary: String?,
    val options: List<RevengeAnswerOption>,
    val hintTextKo: String
)

data class RevengeAnswerOption(
    val wordId: String,
    val displayText: String,
    val isCorrect: Boolean
)

data class RevengeAnswerOutcome(
    val isCorrect: Boolean,
    val isCriticalSpeedHit: Boolean,
    val answeredWithin3Sec: Boolean = false,
    val damageDealt: Int,
    val xpEarned: Int,
    val comboAfter: Int,
    val speedStreakAfter: Int = 0,
    val multiplierAfter: Int,
    val justTriggeredFever: Boolean,
    val flowAutoHintActivated: Boolean,
    val feedbackKo: String
)

data class DailyMicroMission(
    val id: String,
    val titleKo: String,
    val current: Int,
    val target: Int
) {
    val isCompleted: Boolean get() = current >= target
}

data class RevengeSessionSummary(
    val boss: RevengeBossProfile,
    val bossDefeated: Boolean,
    val correctCount: Int,
    val totalCount: Int,
    val accuracyPercent: Int,
    val maxCombo: Int,
    val totalXp: Int,
    val chestRarity: RewardChestRarity,
    val chestBonusXp: Int,
    val missedWords: List<VocabWord>
)

data class RevengeBattleState(
    val boss: RevengeBossProfile,
    val bossCurrentHp: Int,
    val questions: List<RevengeEncounterQuestion>,
    val currentIndex: Int = 0,
    val combo: Int = 0,
    val speedStreak: Int = 0,
    val maxCombo: Int = 0,
    val consecutiveMisses: Int = 0,
    val correctCount: Int = 0,
    val totalXp: Int = 0,
    val feverActive: Boolean = false,
    val eliminatedOptionWordId: String? = null,
    val missedWords: List<VocabWord> = emptyList(),
    val lastOutcome: RevengeAnswerOutcome? = null,
    val isFinished: Boolean = false
) {
    val currentQuestion: RevengeEncounterQuestion?
        get() = questions.getOrNull(currentIndex)

    val multiplier: Int
        get() = maxOf(
            GameRevengeLoopEngine.speedMultiplier(speedStreak),
            GameRevengeLoopEngine.comboMultiplier(combo)
        )

    val hpRatio: Float
        get() = if (boss.maxHp <= 0) 0f else (bossCurrentHp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
}

/**
 * Pure-Kotlin Game-First + SRS + Hooked micro-session engine ("오답 몬스터 복수전").
 * - Prioritizes FSRS due/lapsed/low-stability words first so gameplay directly powers memory retention.
 * - 3-second Speed Combo Multiplier: consecutive answers within 3.0s scale 2x -> 3x -> 5x!
 * - Dynamic Flow Channel: auto-activates hint & eliminates 1 distractor after 2 consecutive misses.
 * - Variable Reward Chests & Daily 3-Minute Micro-Missions.
 */
object GameRevengeLoopEngine {
    const val DEFAULT_SESSION_LENGTH = 8
    const val FAST_COMBO_WINDOW_MS = 3000L
    const val FAST_CRITICAL_MS = FAST_COMBO_WINDOW_MS
    const val FEVER_COMBO_THRESHOLD = 5
    const val FLOW_HINT_MISS_THRESHOLD = 2
    private const val BASE_DAMAGE = 100
    private const val BASE_XP = 18

    private val bossRoster = listOf(
        RevengeBossProfile("revenge_dragon", "망각 드래곤", "오답 기억을 삼키는 수능 수호룡", 800),
        RevengeBossProfile("cloze_guardian", "빈칸 가디언", "문맥 추론을 시험하는 고대 골렘", 800),
        RevengeBossProfile("polysemy_titan", "다의어 타이탄", "함정 선지를 뿌리는 심연의 거인", 800)
    )

    /** Speed-accuracy multiplier for consecutive answers within 3 seconds: 1x -> 2x -> 3x -> 5x. */
    fun speedMultiplier(speedStreak: Int): Int = when {
        speedStreak >= 3 -> 5
        speedStreak == 2 -> 3
        speedStreak == 1 -> 2
        else -> 1
    }

    fun comboMultiplier(combo: Int): Int = when {
        combo >= 8 -> 5
        combo >= FEVER_COMBO_THRESHOLD -> 3
        combo >= 2 -> 2
        else -> 1
    }

    /**
     * Prioritizes FSRS weak/due words (`lapses > 0`, low stability, or due now) and fills
     * the remainder from the candidate vocabulary pool so users can play immediately.
     */
    fun selectRevengeTargets(
        candidateWords: List<VocabWord>,
        progressByWordId: Map<String, WordLearningProgress>,
        count: Int = DEFAULT_SESSION_LENGTH,
        nowMillis: Long = System.currentTimeMillis()
    ): List<VocabWord> {
        val uniquePool = candidateWords.distinctBy { it.id }
        if (uniquePool.isEmpty()) return emptyList()

        val scored = uniquePool.sortedWith(
            compareByDescending<VocabWord> { word ->
                val p = progressByWordId[word.id]
                when {
                    p == null -> 10
                    p.lapseCount > 0 && p.dueAtMs <= nowMillis -> 100 + p.lapseCount * 10
                    p.lapseCount > 0 -> 80 + p.lapseCount * 8
                    p.dueAtMs <= nowMillis -> 65
                    p.fsrsStabilityDays < 3.0 -> 45
                    else -> 5
                }
            }.thenBy { word ->
                progressByWordId[word.id]?.fsrsStabilityDays ?: 1.0
            }
        )
        return scored.take(count.coerceIn(1, uniquePool.size))
    }

    fun startBattle(
        candidateWords: List<VocabWord>,
        progressByWordId: Map<String, WordLearningProgress>,
        sessionLength: Int = DEFAULT_SESSION_LENGTH,
        bossSeed: Int = 0,
        random: Random = Random.Default
    ): RevengeBattleState {
        val pool = candidateWords.distinctBy { it.id }
        val targets = selectRevengeTargets(pool, progressByWordId, sessionLength)
        val bossTemplate = bossRoster[Math.floorMod(bossSeed, bossRoster.size)]
        val totalHp = (targets.size.coerceAtLeast(1)) * BASE_DAMAGE
        val boss = bossTemplate.copy(maxHp = totalHp)

        val modes = RevengeQuestionMode.entries
        val questions = targets.mapIndexed { idx, target ->
            val mode = modes[idx % modes.size]
            buildQuestion(
                index = idx,
                mode = mode,
                target = target,
                distractorPool = pool,
                random = random
            )
        }
        return RevengeBattleState(
            boss = boss,
            bossCurrentHp = totalHp,
            questions = questions,
            isFinished = questions.isEmpty()
        )
    }

    fun buildQuestion(
        index: Int,
        mode: RevengeQuestionMode,
        target: VocabWord,
        distractorPool: List<VocabWord>,
        random: Random = Random.Default
    ): RevengeEncounterQuestion {
        val distractors = distractorPool
            .asSequence()
            .filter { it.id != target.id && !it.meaningKo.equals(target.meaningKo, ignoreCase = true) }
            .toList()
            .shuffled(random)
            .take(3)

        val choiceWords = (listOf(target) + distractors).shuffled(random)
        val useEnglishOptions = mode == RevengeQuestionMode.REVERSE_WORD || mode == RevengeQuestionMode.CLOZE_CONTEXT

        val options = choiceWords.map { word ->
            RevengeAnswerOption(
                wordId = word.id,
                displayText = if (useEnglishOptions) word.word else word.meaningKo,
                isCorrect = word.id == target.id
            )
        }

        val blankedExample = buildClozeExample(target.exampleEn, target.word)
        val (primary, secondary) = when (mode) {
            RevengeQuestionMode.SPEED_MEANING -> target.word to target.ipa.takeIf { it.isNotBlank() }
            RevengeQuestionMode.REVERSE_WORD -> target.meaningKo to "철자 ${target.word.length}글자 · 첫 글자 '${target.word.firstOrNull()?.uppercaseChar() ?: '?'}'"
            RevengeQuestionMode.CLOZE_CONTEXT -> blankedExample to target.exampleKo.takeIf { it.isNotBlank() }
            RevengeQuestionMode.LISTENING_SENSE -> "발음을 듣고 뜻을 맞히세요" to "힌트: ${target.word.take(2)}... (${target.word.length}글자)"
        }

        val firstChar = target.word.firstOrNull()?.uppercaseChar() ?: '?'
        val hintKo = "플로우 힌트: '${firstChar}'로 시작하는 ${target.word.length}글자 · 핵심 뜻 '${target.meaningKo.take(8)}'"

        return RevengeEncounterQuestion(
            index = index,
            mode = mode,
            targetWord = target,
            promptPrimary = primary,
            promptSecondary = secondary,
            options = options,
            hintTextKo = hintKo
        )
    }

    /**
     * Submits an answer for the active question and returns the updated immutable battle state.
     * Guards against double-tap or out-of-bounds submissions.
     */
    fun submitAnswer(
        state: RevengeBattleState,
        selectedWordId: String,
        elapsedMs: Long
    ): RevengeBattleState {
        if (state.isFinished) return state
        val question = state.currentQuestion ?: return state.copy(isFinished = true)
        val selected = question.options.firstOrNull { it.wordId == selectedWordId } ?: return state

        return if (selected.isCorrect) {
            val nextCombo = state.combo + 1
            val within3Sec = elapsedMs in 0..FAST_COMBO_WINDOW_MS
            val nextSpeedStreak = if (within3Sec) state.speedStreak + 1 else 0
            val nextSpeedMult = speedMultiplier(nextSpeedStreak)
            val nextMultiplier = maxOf(nextSpeedMult, comboMultiplier(nextCombo))
            val isFastCritical = within3Sec
            val damage = when {
                nextMultiplier >= 5 -> 260
                nextMultiplier == 3 -> 180
                nextMultiplier == 2 -> 120
                else -> BASE_DAMAGE
            }
            val xp = (BASE_XP * nextMultiplier) + if (within3Sec) 12 * nextSpeedMult else 0
            val wasFever = state.feverActive
            val nowFever = nextMultiplier >= 5 || nextCombo >= FEVER_COMBO_THRESHOLD
            val nextHp = (state.bossCurrentHp - damage).coerceAtLeast(0)

            val feedback = buildString {
                if (nextMultiplier >= 5) append("5x 초고속 FEVER! ")
                else if (within3Sec) append("3초 스피드 ${nextMultiplier}x 콤보! ")
                else if (nowFever && !wasFever) append("FEVER 발동! ")
                append("-$damage HP · +$xp XP")
            }

            state.copy(
                bossCurrentHp = nextHp,
                combo = nextCombo,
                speedStreak = nextSpeedStreak,
                maxCombo = maxOf(state.maxCombo, nextCombo),
                consecutiveMisses = 0,
                correctCount = state.correctCount + 1,
                totalXp = state.totalXp + xp,
                feverActive = nowFever,
                lastOutcome = RevengeAnswerOutcome(
                    isCorrect = true,
                    isCriticalSpeedHit = isFastCritical,
                    answeredWithin3Sec = within3Sec,
                    damageDealt = damage,
                    xpEarned = xp,
                    comboAfter = nextCombo,
                    speedStreakAfter = nextSpeedStreak,
                    multiplierAfter = nextMultiplier,
                    justTriggeredFever = nowFever && !wasFever,
                    flowAutoHintActivated = false,
                    feedbackKo = feedback
                )
            )
        } else {
            val nextMissStreak = state.consecutiveMisses + 1
            val activateFlowHint = nextMissStreak >= FLOW_HINT_MISS_THRESHOLD
            val updatedMissed = if (state.missedWords.any { it.id == question.targetWord.id }) {
                state.missedWords
            } else {
                state.missedWords + question.targetWord
            }

            state.copy(
                combo = 0,
                speedStreak = 0,
                consecutiveMisses = nextMissStreak,
                feverActive = false,
                missedWords = updatedMissed,
                lastOutcome = RevengeAnswerOutcome(
                    isCorrect = false,
                    isCriticalSpeedHit = false,
                    answeredWithin3Sec = false,
                    damageDealt = 0,
                    xpEarned = 0,
                    comboAfter = 0,
                    speedStreakAfter = 0,
                    multiplierAfter = 1,
                    justTriggeredFever = false,
                    flowAutoHintActivated = activateFlowHint,
                    feedbackKo = "정답: ${question.targetWord.word} = ${question.targetWord.meaningKo}"
                )
            )
        }
    }

    /** Advances to the next question or finishes the battle; applies Flow Channel distractor removal when needed. */
    fun advanceTurn(state: RevengeBattleState, random: Random = Random.Default): RevengeBattleState {
        if (state.isFinished) return state
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size || state.bossCurrentHp <= 0) {
            return state.copy(isFinished = true)
        }
        val nextQuestion = state.questions[nextIndex]
        val eliminatedId = if (state.consecutiveMisses >= FLOW_HINT_MISS_THRESHOLD) {
            nextQuestion.options.filter { !it.isCorrect }.shuffled(random).firstOrNull()?.wordId
        } else {
            null
        }
        return state.copy(
            currentIndex = nextIndex,
            eliminatedOptionWordId = eliminatedId,
            lastOutcome = null
        )
    }

    /** Computes end-of-session victory report & variable reward chest rarity. */
    fun summarizeSession(
        state: RevengeBattleState,
        rollPercent: Int = Random.nextInt(100)
    ): RevengeSessionSummary {
        val total = state.questions.size.coerceAtLeast(1)
        val accuracy = ((state.correctCount.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        val defeated = state.bossCurrentHp <= 0 || accuracy >= 75
        val clampedRoll = rollPercent.coerceIn(0, 99)

        // Variable Ratio Reward Chest influenced by mastery performance (Hooked model)
        val rarity = when {
            defeated && accuracy == 100 && clampedRoll >= 55 -> RewardChestRarity.LEGENDARY
            defeated && accuracy >= 85 && clampedRoll >= 45 -> RewardChestRarity.EPIC
            defeated && clampedRoll >= 30 -> RewardChestRarity.RARE
            accuracy >= 60 && clampedRoll >= 75 -> RewardChestRarity.RARE
            else -> RewardChestRarity.COMMON
        }

        return RevengeSessionSummary(
            boss = state.boss,
            bossDefeated = defeated,
            correctCount = state.correctCount,
            totalCount = state.questions.size,
            accuracyPercent = accuracy,
            maxCombo = state.maxCombo,
            totalXp = state.totalXp + rarity.bonusXp,
            chestRarity = rarity,
            chestBonusXp = rarity.bonusXp,
            missedWords = state.missedWords
        )
    }

    /** Builds Daily 3-Minute Micro-Missions from live session & FSRS progress. */
    fun buildDailyMicroMissions(
        sessionsClearedToday: Int,
        bestComboToday: Int,
        masteredWordCount: Int
    ): List<DailyMicroMission> = listOf(
        DailyMicroMission(
            id = "daily_revenge_clear",
            titleKo = "오답 보스 1판 격파 (2분 컷)",
            current = sessionsClearedToday.coerceAtMost(1),
            target = 1
        ),
        DailyMicroMission(
            id = "daily_fever_combo",
            titleKo = "5연속 콤보 FEVER 발동",
            current = bestComboToday.coerceAtMost(FEVER_COMBO_THRESHOLD),
            target = FEVER_COMBO_THRESHOLD
        ),
        DailyMicroMission(
            id = "daily_codex_mastery",
            titleKo = "도감 숙련 단어 10개 돌파",
            current = masteredWordCount.coerceAtMost(10),
            target = 10
        )
    )

    fun computeMasteryCodexPercent(
        totalPoolSize: Int,
        progressByWordId: Map<String, WordLearningProgress>
    ): Int {
        if (totalPoolSize <= 0) return 0
        val mastered = progressByWordId.values.count { it.repetition >= 2 || it.fsrsStabilityDays >= 4.0 }
        return ((mastered.toDouble() / totalPoolSize.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    }

    private fun buildClozeExample(sentence: String, word: String): String {
        val cleanSentence = sentence.trim()
        val cleanWord = word.trim()
        if (cleanSentence.isEmpty() || cleanWord.isEmpty()) {
            return "Choose the best word to fill in _____."
        }
        val regex = Regex("\\b${Regex.escape(cleanWord)}[a-zA-Z]*\\b", RegexOption.IGNORE_CASE)
        return if (regex.containsMatchIn(cleanSentence)) {
            regex.replaceFirst(cleanSentence, "_____")
        } else {
            "$cleanSentence (_____)"
        }
    }
}
