package com.example.engine

import com.example.model.TravelSentence
import com.example.model.VocabTopic
import com.example.model.VocabWord
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * EnGame 2.0 — 4-Mode Gamified English Arcade Engine:
 * 1. SPEED_BLITZ: 60-second timed vocabulary survival with Speed & Fever multipliers.
 * 2. SENTENCE_BUILDER: Interactive English word-chip scramble/unscramble puzzle.
 * 3. SYNONYM_LINK: Synonym & Antonym semantic chain battle powered by SYN-xxxx clusters.
 * 4. VOICE_BOSS_RUSH: Audio-first listening & live pronunciation speaking boss wave.
 */
enum class EnGameMode(
    val titleKo: String,
    val subtitleKo: String,
    val badgeKo: String,
    val accentHex: Long,
    val durationSec: Int
) {
    SPEED_BLITZ(
        titleKo = "타임어택 블리츠",
        subtitleKo = "60초 초고속 어휘 생존 · 콤보 피버",
        badgeKo = "60초 스피드",
        accentHex = 0xFF2563EB,
        durationSec = 60
    ),
    SENTENCE_BUILDER(
        titleKo = "문장 블록 퍼즐",
        subtitleKo = "실전 영어 문장을 순서대로 조립",
        badgeKo = "어순 마스터",
        accentHex = 0xFF148F68,
        durationSec = 90
    ),
    SYNONYM_LINK(
        titleKo = "동의어·반의어 체인",
        subtitleKo = "SYN 클러스터 의미 연결 콤보",
        badgeKo = "어휘 확장",
        accentHex = 0xFF7C3AED,
        durationSec = 75
    ),
    VOICE_BOSS_RUSH(
        titleKo = "리스닝·스피킹 보스",
        subtitleKo = "원어민 음성 청취 & 직접 말하기 격파",
        badgeKo = "보스 러시",
        accentHex = 0xFFE11D48,
        durationSec = 90
    ),
    SPELLING_TILES(
        titleKo = "철자 타일 조립",
        subtitleKo = "알파벳 타일을 순서대로 배치해 영단어 완성",
        badgeKo = "철자 마스터",
        accentHex = 0xFFEA580C,
        durationSec = 80
    ),
    PICTURE_MATCH(
        titleKo = "그림 장면 맞히기",
        subtitleKo = "연상 일러스트와 장면을 보고 영단어 선택",
        badgeKo = "이미지 퀴즈",
        accentHex = 0xFF0D9488,
        durationSec = 65
    ),
    MEMORY_FLIP(
        titleKo = "기억 뒤집기 퍼즐",
        subtitleKo = "영어 카드와 뜻 카드를 뒤집어 짝맞추기",
        badgeKo = "기억 매칭",
        accentHex = 0xFF8B5CF6,
        durationSec = 75
    ),
    CLOZE_CONTEXT(
        titleKo = "문장 속 단어 찾기",
        subtitleKo = "실전 영어 예문 빈칸에 들어갈 핵심 단어 추론",
        badgeKo = "문맥 빈칸",
        accentHex = 0xFFD97706,
        durationSec = 75
    )
}

enum class EnGamePhase {
    CLOSED,
    MODE_HUB,
    PLAYING,
    RESULT
}

enum class EnGameRank(val label: String, val messageKo: String, val accentHex: Long) {
    S_PLUS("S+", "완벽한 원어민급 반응속도와 정확도!", 0xFFEAB308),
    S("S", "최상위 마스터 등급 달성!", 0xFF148F68),
    A("A", "훌륭한 실력! 조금만 더 하면 S등급!", 0xFF2563EB),
    B("B", "안정적인 성장세! 오답 복습으로 업그레이드!", 0xFF7C3AED),
    C("C", "좋은 워밍업! 틀린 단어가 오답 노트에 저장됐어요.", 0xFFEA580C)
}

data class MemoryFlipCard(
    val cardIndex: Int,
    val pairId: String,
    val label: String,
    val isEnglish: Boolean,
    val wordId: String
)

data class EnGameQuestion(
    val id: String,
    val mode: EnGameMode,
    val promptPrimary: String,
    val promptSecondary: String,
    val audioTextEn: String,
    val correctAnswer: String,
    val options: List<String> = emptyList(),
    val wordId: String? = null,
    val scrambledTokens: List<String> = emptyList(),
    val targetTokens: List<String> = emptyList(),
    val memoryFlipCards: List<MemoryFlipCard> = emptyList(),
    val pictureTopic: VocabTopic? = null,
    val targetWord: VocabWord? = null,
    val isAntonymRound: Boolean = false,
    val hideAudioTextUntilAnswered: Boolean = false,
    val difficultyRating: Int = 1200,
    val startedAtMs: Long = System.currentTimeMillis()
)

data class EnGameAnswerEvaluation(
    val isCorrect: Boolean,
    val scoreDelta: Int,
    val nextCombo: Int,
    val nextFeverGauge: Int,
    val feverJustActivated: Boolean,
    val isFeverActive: Boolean,
    val speedTagKo: String,
    val nextElo: Int,
    val bossDamage: Int,
    val feedbackKo: String
)

data class EnGameUiState(
    val phase: EnGamePhase = EnGamePhase.CLOSED,
    val mode: EnGameMode = EnGameMode.SPEED_BLITZ,
    val eloRating: Int = 1200,
    val score: Int = 0,
    val highScore: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val lives: Int = 3,
    val maxLives: Int = 3,
    val timeLeftSec: Int = 60,
    val totalTimeSec: Int = 60,
    val feverGauge: Int = 0, // 0..100
    val feverTurnsLeft: Int = 0,
    val waveNumber: Int = 1,
    val bossHp: Int = 100,
    val bossMaxHp: Int = 100,
    val totalAnswered: Int = 0,
    val correctCount: Int = 0,
    val currentQuestion: EnGameQuestion? = null,
    val selectedTokens: List<Int> = emptyList(), // Indices into scrambledTokens for SENTENCE_BUILDER/SPELLING_TILES
    val flippedCardIndices: List<Int> = emptyList(), // Up to 2 open indices in MEMORY_FLIP
    val matchedPairIds: Set<String> = emptySet(),
    val lastFeedbackKo: String = "",
    val lastSpeedTagKo: String = "",
    val lastScoreDelta: Int = 0,
    val hitPulseTick: Int = 0,
    val missPulseTick: Int = 0,
    val missedWords: List<Pair<String, String>> = emptyList(),
    val earnedCoins: Int = 0,
    val earnedXp: Int = 0
) {
    val accuracyPercent: Int
        get() = if (totalAnswered <= 0) 0 else ((correctCount * 100f) / totalAnswered).roundToInt()

    val isFeverActive: Boolean
        get() = feverTurnsLeft > 0

    val rank: EnGameRank
        get() = EnGameMathEngine.computeRank(accuracyPercent = accuracyPercent, maxCombo = maxCombo, score = score)
}

/**
 * Pure mathematical core for EnGame 2.0:
 * - ELO adaptive difficulty update
 * - Speed + Combo + Fever score formula
 * - Sentence tokenization & non-identity deterministic/random scrambling
 * - Multi-mode question generator backed by real VocabWord & TravelSentence pools
 */
object EnGameMathEngine {

    private const val MIN_ELO = 800
    private const val MAX_ELO = 2400
    private const val ELO_K_FACTOR = 32.0

    fun updateElo(currentElo: Int, itemDifficulty: Int, isCorrect: Boolean): Int {
        val expected = 1.0 / (1.0 + 10.0.pow((itemDifficulty - currentElo) / 400.0))
        val actual = if (isCorrect) 1.0 else 0.0
        val updated = (currentElo + ELO_K_FACTOR * (actual - expected)).roundToInt()
        return updated.coerceIn(MIN_ELO, MAX_ELO)
    }

    fun difficultyTierLabel(elo: Int): String = when {
        elo >= 1650 -> "수능 1등급 마스터"
        elo >= 1400 -> "수능 2등급 챌린저"
        elo >= 1180 -> "수능 3등급 도전자"
        elo >= 1000 -> "기초 완성 실버"
        else -> "입문 브론즈"
    }

    fun computeRank(accuracyPercent: Int, maxCombo: Int, score: Int): EnGameRank = when {
        accuracyPercent >= 95 && maxCombo >= 6 && score >= 900 -> EnGameRank.S_PLUS
        accuracyPercent >= 88 && score >= 600 -> EnGameRank.S
        accuracyPercent >= 75 && score >= 350 -> EnGameRank.A
        accuracyPercent >= 58 -> EnGameRank.B
        else -> EnGameRank.C
    }

    fun evaluateAnswer(
        state: EnGameUiState,
        isCorrect: Boolean,
        elapsedMs: Long,
        isVoiceCritical: Boolean = false,
        correctLabel: String
    ): EnGameAnswerEvaluation {
        val itemDiff = state.currentQuestion?.difficultyRating ?: 1200
        val nextElo = updateElo(state.eloRating, itemDiff, isCorrect)

        if (!isCorrect) {
            return EnGameAnswerEvaluation(
                isCorrect = false,
                scoreDelta = 0,
                nextCombo = 0,
                nextFeverGauge = (state.feverGauge - 20).coerceAtLeast(0),
                feverJustActivated = false,
                isFeverActive = false,
                speedTagKo = "MISS",
                nextElo = nextElo,
                bossDamage = 0,
                feedbackKo = "정답: $correctLabel (오답 노트에 자동 저장!)"
            )
        }

        val nextCombo = state.combo + 1
        val (speedMult, speedTag) = when {
            isVoiceCritical -> 1.75 to "VOICE CRITICAL!"
            elapsedMs <= 2500L -> 1.60 to "FLASH x1.6!"
            elapsedMs <= 5000L -> 1.25 to "QUICK x1.25"
            else -> 1.00 to "NICE!"
        }
        val comboMult = 1.0 + (nextCombo.coerceAtMost(15) * 0.12)
        val currentlyFever = state.feverTurnsLeft > 0
        val addedGauge = if (currentlyFever) 0 else (24 + (if (elapsedMs <= 3000L) 10 else 0))
        val rawGauge = (state.feverGauge + addedGauge).coerceAtMost(100)
        val feverJustActivated = !currentlyFever && rawGauge >= 100
        val isFeverNow = currentlyFever || feverJustActivated
        val feverMult = if (isFeverNow) 2.0 else 1.0

        val basePoints = when (state.mode) {
            EnGameMode.SPEED_BLITZ -> 100
            EnGameMode.SENTENCE_BUILDER -> 160
            EnGameMode.SYNONYM_LINK -> 130
            EnGameMode.VOICE_BOSS_RUSH -> 150
            EnGameMode.SPELLING_TILES -> 170
            EnGameMode.PICTURE_MATCH -> 120
            EnGameMode.MEMORY_FLIP -> 140
            EnGameMode.CLOZE_CONTEXT -> 150
        }
        val scoreDelta = (basePoints * comboMult * speedMult * feverMult).roundToInt()
        val bossDamage = (28 * speedMult * (if (isFeverNow) 1.5 else 1.0) + nextCombo * 2).roundToInt()

        return EnGameAnswerEvaluation(
            isCorrect = true,
            scoreDelta = scoreDelta,
            nextCombo = nextCombo,
            nextFeverGauge = if (feverJustActivated) 100 else rawGauge,
            feverJustActivated = feverJustActivated,
            isFeverActive = isFeverNow,
            speedTagKo = if (feverJustActivated) "FEVER MODE x2!" else speedTag,
            nextElo = nextElo,
            bossDamage = bossDamage,
            feedbackKo = if (feverJustActivated) {
                "FEVER 발동! +$scoreDelta 점 · 모든 보상 2배!"
            } else {
                "+$scoreDelta 점 ($speedTag · ${nextCombo}콤보)"
            }
        )
    }

    /**
     * Tokenizes an English sentence into clean word chips and ensures the scrambled
     * ordering differs from the original whenever there are >= 3 distinct tokens.
     */
    fun tokenizeSentence(sentence: String): List<String> =
        sentence.trim()
            .replace(Regex("[.!?]+$"), "")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun scrambleTokens(tokens: List<String>, seed: Int = 0): List<String> {
        if (tokens.size <= 1) return tokens
        val shuffled = tokens.toMutableList()
        if (seed != 0) {
            val rnd = kotlin.random.Random(seed)
            shuffled.shuffle(rnd)
        } else {
            shuffled.shuffle()
        }
        // Guarantee non-identity order if possible
        if (shuffled == tokens && tokens.distinct().size > 1) {
            val first = shuffled.removeAt(0)
            shuffled.add(first)
        }
        return shuffled
    }

    fun generateQuestion(
        mode: EnGameMode,
        eloRating: Int,
        vocabPool: List<VocabWord>,
        weakWordIds: Set<String>,
        travelSentences: List<TravelSentence>,
        recentIds: Set<String> = emptySet()
    ): EnGameQuestion {
        val safeVocab = vocabPool.ifEmpty {
            listOf(
                VocabWord(
                    id = "fallback_1",
                    grade = 2,
                    textbookUnitKo = "고2 수능 핵심",
                    csatFrequency = 5,
                    csatDifficulty = 2,
                    topic = VocabTopic.EMOTION_MIND,
                    word = "resilient",
                    ipa = "/rɪˈzɪliənt/",
                    partOfSpeech = "형용사",
                    meaningKo = "회복력이 있는, 탄력적인",
                    exampleEn = "Stay resilient and keep moving forward.",
                    exampleKo = "회복력을 유지하고 계속 나아가세요.",
                    examples = emptyList(),
                    pronunciationTipKo = "리질리언트(zil 강세)",
                    distractors = listOf("일시적인", "무관심한", "취약한"),
                    synonymGroupId = "SYN-0001",
                    synonyms = listOf("strong", "adaptable", "tough"),
                    antonyms = listOf("fragile", "weak")
                )
            )
        }

        // Prioritize weak words (35% chance when available) + adaptive difficulty band
        val unseenPool = safeVocab.filter { it.id !in recentIds }.ifEmpty { safeVocab }
        val weakCandidates = unseenPool.filter { it.id in weakWordIds }
        val targetWord = when {
            weakCandidates.isNotEmpty() && kotlin.random.Random.nextFloat() < 0.38f -> weakCandidates.random()
            else -> pickAdaptiveWord(unseenPool, eloRating)
        }

        val itemDifficulty = when (targetWord.grade) {
            1 -> 1050
            2 -> 1250
            else -> 1480
        }

        return when (mode) {
            EnGameMode.SPEED_BLITZ -> {
                val distractors = safeVocab
                    .filter { it.meaningKo != targetWord.meaningKo }
                    .shuffled()
                    .take(3)
                    .map { it.meaningKo }
                val options = (distractors + targetWord.meaningKo).shuffled()
                EnGameQuestion(
                    id = "blitz_${targetWord.id}_${System.nanoTime()}",
                    mode = mode,
                    promptPrimary = targetWord.word,
                    promptSecondary = targetWord.exampleEn.ifBlank { targetWord.ipa },
                    audioTextEn = targetWord.word,
                    correctAnswer = targetWord.meaningKo,
                    options = options,
                    wordId = targetWord.id,
                    difficultyRating = itemDifficulty
                )
            }

            EnGameMode.SENTENCE_BUILDER -> {
                val sentencePair: Pair<String, String> = if (travelSentences.isNotEmpty() && kotlin.random.Random.nextBoolean()) {
                    val s = travelSentences.filter { tokenizeSentence(it.english).size in 4..8 }
                        .ifEmpty { travelSentences }
                        .random()
                    s.english to s.korean
                } else {
                    val ex = targetWord.examples.firstOrNull { tokenizeSentence(it.english).size in 4..8 }
                    if (ex != null) {
                        ex.english to ex.korean
                    } else {
                        val rawEn = targetWord.exampleEn.ifBlank { "We use ${targetWord.word} every single day." }
                        val rawKo = targetWord.exampleKo.ifBlank { "${targetWord.meaningKo} — 실전 문장을 완성해 보세요." }
                        rawEn to rawKo
                    }
                }
                val tokens = tokenizeSentence(sentencePair.first).take(9)
                val scrambled = scrambleTokens(tokens)
                EnGameQuestion(
                    id = "builder_${System.nanoTime()}",
                    mode = mode,
                    promptPrimary = sentencePair.second,
                    promptSecondary = "영어 단어 블록을 순서대로 눌러 문장을 완성하세요",
                    audioTextEn = sentencePair.first,
                    correctAnswer = tokens.joinToString(" "),
                    scrambledTokens = scrambled,
                    targetTokens = tokens,
                    wordId = targetWord.id,
                    difficultyRating = itemDifficulty + 40
                )
            }

            EnGameMode.SYNONYM_LINK -> {
                val synPool = safeVocab.filter { it.synonyms.isNotEmpty() || it.antonyms.isNotEmpty() }
                val chosen = synPool.filter { it.id !in recentIds }.ifEmpty { synPool }.randomOrNull() ?: targetWord
                val useAntonym = chosen.antonyms.isNotEmpty() && (chosen.synonyms.isEmpty() || kotlin.random.Random.nextBoolean())
                val answer = if (useAntonym) {
                    chosen.antonyms.first()
                } else {
                    chosen.synonyms.firstOrNull() ?: chosen.meaningKo
                }
                val allRelWords = (safeVocab.flatMap { it.synonyms + it.antonyms } + safeVocab.map { it.word })
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.equals(answer, ignoreCase = true) && !it.equals(chosen.word, ignoreCase = true) }
                    .distinct()
                val distractors = allRelWords.shuffled().take(3)
                val options = (distractors + answer).shuffled()
                val relationLabel = if (useAntonym) "반의어(ANTONYM) 찾기" else "동의어(SYNONYM) 연결"
                val clusterTag = if (chosen.synonymGroupId.isNotBlank()) " · ${chosen.synonymGroupId}" else ""
                EnGameQuestion(
                    id = "syn_${chosen.id}_${System.nanoTime()}",
                    mode = mode,
                    promptPrimary = chosen.word,
                    promptSecondary = "${chosen.meaningKo} · $relationLabel$clusterTag",
                    audioTextEn = chosen.word,
                    correctAnswer = answer,
                    options = options,
                    wordId = chosen.id,
                    isAntonymRound = useAntonym,
                    difficultyRating = itemDifficulty + 60
                )
            }

            EnGameMode.VOICE_BOSS_RUSH -> {
                val useSentence = travelSentences.isNotEmpty() && kotlin.random.Random.nextFloat() < 0.55f
                if (useSentence) {
                    val s = travelSentences.random()
                    val distractors = travelSentences
                        .filter { it.korean != s.korean }
                        .shuffled()
                        .take(3)
                        .map { it.korean }
                    val options = (distractors + s.korean).shuffled()
                    EnGameQuestion(
                        id = "boss_s_${System.nanoTime()}",
                        mode = mode,
                        promptPrimary = s.english,
                        promptSecondary = "보스 청취 라운드 · 원어민 음성을 듣고 뜻을 맞히거나 직접 말해 격파!",
                        audioTextEn = s.english,
                        correctAnswer = s.korean,
                        options = options,
                        hideAudioTextUntilAnswered = true,
                        difficultyRating = itemDifficulty + 80
                    )
                } else {
                    val distractors = safeVocab
                        .filter { it.meaningKo != targetWord.meaningKo }
                        .sortedByDescending { if (it.partOfSpeech == targetWord.partOfSpeech) 1 else 0 }
                        .take(8)
                        .shuffled()
                        .take(3)
                        .map { it.meaningKo }
                    EnGameQuestion(
                        id = "boss_w_${targetWord.id}_${System.nanoTime()}",
                        mode = mode,
                        promptPrimary = targetWord.word,
                        promptSecondary = "보스 어휘 라운드 · 음성을 듣고 뜻을 고르거나 직접 발음해 크리티컬!",
                        audioTextEn = targetWord.word,
                        correctAnswer = targetWord.meaningKo,
                        options = (distractors + targetWord.meaningKo).shuffled(),
                        wordId = targetWord.id,
                        targetWord = targetWord,
                        hideAudioTextUntilAnswered = true,
                        difficultyRating = itemDifficulty + 80
                    )
                }
            }

            EnGameMode.SPELLING_TILES -> {
                val spellingPool = safeVocab.filter { w ->
                    val cleaned = w.word.trim()
                    cleaned.length in 4..10 && cleaned.all { it.isLetter() }
                }.ifEmpty { safeVocab }
                val chosen = spellingPool.filter { it.id !in recentIds }.ifEmpty { spellingPool }.randomOrNull() ?: targetWord
                val letters = chosen.word.lowercase()
                    .filter { it.isLetter() }
                    .ifEmpty { "study" }
                    .map { it.toString() }
                val scrambled = scrambleTokens(letters)
                val firstHint = letters.firstOrNull().orEmpty().uppercase()
                EnGameQuestion(
                    id = "spell_${chosen.id}_${System.nanoTime()}",
                    mode = mode,
                    promptPrimary = chosen.meaningKo,
                    promptSecondary = "${chosen.ipa} · 첫 글자 '$firstHint' (${letters.size}글자) · 알파벳 타일을 순서대로 조립하세요",
                    audioTextEn = chosen.word,
                    correctAnswer = letters.joinToString(""),
                    scrambledTokens = scrambled,
                    targetTokens = letters,
                    wordId = chosen.id,
                    difficultyRating = itemDifficulty + 35
                )
            }

            EnGameMode.PICTURE_MATCH -> {
                val guide = VocabMnemonicEngine.buildStructuredGuide(targetWord)
                val samePosPool = safeVocab.filter {
                    !it.word.equals(targetWord.word, ignoreCase = true) &&
                        it.partOfSpeech.equals(targetWord.partOfSpeech, ignoreCase = true)
                }
                val fallbackPool = safeVocab.filter { !it.word.equals(targetWord.word, ignoreCase = true) }
                val distractors = (samePosPool.shuffled() + fallbackPool.shuffled())
                    .distinctBy { it.word.lowercase() }
                    .take(3)
                    .map { it.word }
                val options = (distractors + targetWord.word).shuffled()
                EnGameQuestion(
                    id = "pic_${targetWord.id}_${System.nanoTime()}",
                    mode = mode,
                    promptPrimary = guide.sceneTitleKo,
                    promptSecondary = "${guide.sceneDescriptionKo} (${targetWord.meaningKo})",
                    audioTextEn = targetWord.word,
                    correctAnswer = targetWord.word,
                    options = options,
                    wordId = targetWord.id,
                    pictureTopic = targetWord.topic,
                    targetWord = targetWord,
                    difficultyRating = itemDifficulty + 20
                )
            }

            EnGameMode.MEMORY_FLIP -> {
                val pairWords = (listOf(targetWord) + safeVocab.filter { it.id != targetWord.id }.shuffled())
                    .distinctBy { it.word.lowercase() }
                    .take(4)
                val rawCards = pairWords.flatMap { w ->
                    listOf(
                        MemoryFlipCard(0, w.id, w.word, isEnglish = true, wordId = w.id),
                        MemoryFlipCard(0, w.id, w.meaningKo, isEnglish = false, wordId = w.id)
                    )
                }.shuffled().mapIndexed { idx, card -> card.copy(cardIndex = idx) }
                EnGameQuestion(
                    id = "flip_${System.nanoTime()}",
                    mode = mode,
                    promptPrimary = "기억 뒤집기 · ${pairWords.size}쌍 짝맞추기",
                    promptSecondary = "같은 의미의 영어 카드와 한국어 뜻 카드를 찾아 연결하세요",
                    audioTextEn = targetWord.word,
                    correctAnswer = targetWord.word,
                    wordId = targetWord.id,
                    targetWord = targetWord,
                    memoryFlipCards = rawCards,
                    difficultyRating = itemDifficulty + 25
                )
            }

            EnGameMode.CLOZE_CONTEXT -> {
                val baseSentence = targetWord.examples.firstOrNull()?.english?.ifBlank { targetWord.exampleEn } ?: targetWord.exampleEn
                val regex = Regex("\\b${Regex.escape(targetWord.word)}\\w*\\b", RegexOption.IGNORE_CASE)
                val clozeEn = if (regex.containsMatchIn(baseSentence)) {
                    regex.replace(baseSentence, "_____")
                } else {
                    "$baseSentence (_____)"
                }
                val koHint = targetWord.examples.firstOrNull()?.korean?.ifBlank { targetWord.exampleKo } ?: targetWord.exampleKo
                val samePosPool = safeVocab.filter {
                    !it.word.equals(targetWord.word, ignoreCase = true) &&
                        it.partOfSpeech.equals(targetWord.partOfSpeech, ignoreCase = true)
                }
                val fallbackPool = safeVocab.filter { !it.word.equals(targetWord.word, ignoreCase = true) }
                val distractors = (samePosPool.shuffled() + fallbackPool.shuffled())
                    .distinctBy { it.word.lowercase() }
                    .take(3)
                    .map { it.word }
                val options = (distractors + targetWord.word).shuffled()
                EnGameQuestion(
                    id = "cloze_${targetWord.id}_${System.nanoTime()}",
                    mode = mode,
                    promptPrimary = clozeEn,
                    promptSecondary = "힌트: $koHint",
                    audioTextEn = baseSentence,
                    correctAnswer = targetWord.word,
                    options = options,
                    wordId = targetWord.id,
                    targetWord = targetWord,
                    difficultyRating = itemDifficulty + 45
                )
            }
        }
    }

    private fun pickAdaptiveWord(pool: List<VocabWord>, elo: Int): VocabWord {
        val targetGrade = when {
            elo >= 1450 -> 3
            elo >= 1150 -> 2
            else -> 1
        }
        val matched = pool.filter { it.grade == targetGrade }
        return if (matched.isNotEmpty() && kotlin.random.Random.nextFloat() < 0.72f) {
            matched.random()
        } else {
            pool.random()
        }
    }
}
