package com.example.engine

import com.example.model.VocabExample
import com.example.model.VocabTopic
import com.example.model.VocabWord
import com.example.model.WordRunChallengeState
import com.example.model.WordRunDayProgress
import com.example.model.WordRunEntry
import com.example.model.WordRunExamSessionResult
import com.example.model.WordRunMorphologyChip
import com.example.model.WordRunMorphologyInfo
import com.example.model.WordRunQuizQuestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Pure-Kotlin Domain Engine for WORD RUN 2027 (500 CSAT Words · 10-Day Intensive Challenge).
 *
 * Implements:
 * - Morphological inflection breakdown (irregular verbs, plurals, unchanged uncountables, sentence form matcher)
 * - Synonym-group & Levenshtein-distance distractor selection (eliminating ambiguous choices)
 * - 10-Day KST sequential unlock policy + optional free-practice override
 * - 20-minute (1,200s) 50-question mixed (25 EN->KO + 25 KO->EN) exam builder & 90% (45/50) grader
 * - Direct bridge to FSRS VocabWord for wrong-word spaced repetition
 */
object WordRun2027Engine {

    const val TOTAL_DAYS = 10
    const val WORDS_PER_DAY = 50
    const val EXAM_DURATION_SEC = 20 * 60 // 1,200 seconds
    const val EXAM_PASS_SCORE = 45        // 90% (45/50)
    const val REVIEW_QUESTION_COUNT = 10
    const val MAX_CONTEXT_LENGTH = 1800

    private val POS_LABELS_KO = mapOf(
        "n" to "명사",
        "v" to "동사",
        "adj" to "형용사",
        "adv" to "부사"
    )

    private val IRREGULAR_VERBS: Map<String, List<String>> = mapOf(
        "bury" to listOf("buries", "buried", "buried", "burying"),
        "stare" to listOf("stares", "stared", "stared", "staring"),
        "recognize" to listOf("recognizes", "recognized", "recognized", "recognizing"),
        "arise" to listOf("arises", "arose", "arisen", "arising"),
        "rise" to listOf("rises", "rose", "risen", "rising"),
        "underlie" to listOf("underlies", "underlay", "underlain", "underlying"),
        "compete" to listOf("competes", "competed", "competed", "competing"),
        "thrive" to listOf("thrives", "thrived", "thrived", "thriving"),
        "admit" to listOf("admits", "admitted", "admitted", "admitting"),
        "prefer" to listOf("prefers", "preferred", "preferred", "preferring"),
        "regret" to listOf("regrets", "regretted", "regretted", "regretting"),
        "occur" to listOf("occurs", "occurred", "occurred", "occurring"),
        "refer" to listOf("refers", "referred", "referred", "referring"),
        "rely" to listOf("relies", "relied", "relied", "relying"),
        "blame" to listOf("blames", "blamed", "blamed", "blaming"),
        "agree" to listOf("agrees", "agreed", "agreed", "agreeing"),
        "focus" to listOf("focuses", "focused", "focused", "focusing"),
        "infer" to listOf("infers", "inferred", "inferred", "inferring"),
        "sustain" to listOf("sustains", "sustained", "sustained", "sustaining"),
        "overstep" to listOf("oversteps", "overstepped", "overstepped", "overstepping"),
        "travel" to listOf("travels", "traveled", "traveled", "traveling"),
        "host" to listOf("hosts", "hosted", "hosted", "hosting")
    )

    private val UNCHANGED_NOUNS: Set<String> = setOf(
        "wildlife", "research", "evidence", "literature", "food", "clothing",
        "information", "staff", "species", "offspring", "aircraft", "biomass",
        "antitrust", "globalization", "memorization", "sustainment"
    )

    private val SYNONYM_GROUPS: List<Set<String>> = listOf(
        setOf("roughly", "approximately"),
        setOf("largely", "broadly", "primarily", "predominantly"),
        setOf("obvious", "evidently", "obviously"),
        setOf("precise", "exact", "definite"),
        setOf("capability", "capacity"),
        setOf("result", "consequence"),
        setOf("goal", "objective", "purpose"),
        setOf("convey", "deliver"),
        setOf("represent", "index", "display"),
        setOf("arrange", "compose"),
        setOf("proportion", "ratio"),
        setOf("evolution", "development"),
        setOf("protect", "safeguard"),
        setOf("separate", "inseparable"),
        setOf("available", "access"),
        setOf("skill", "capability"),
        setOf("viewpoint", "perspective"),
        setOf("notion", "principle"),
        setOf("thrive", "prosper"),
        setOf("conceal", "hidden"),
        setOf("globalization", "global"),
        setOf("rely", "trust"),
        setOf("maintain", "sustain"),
        setOf("belief", "assumption"),
        setOf("assume", "adopt"),
        setOf("adjust", "modify"),
        setOf("temporary", "temporarily"),
        setOf("attention", "interest"),
        setOf("emerge", "appear"),
        setOf("recognize", "realize"),
        setOf("distributor", "distribution"),
        setOf("participation", "participant"),
        setOf("comfort", "reassure"),
        setOf("challenge", "obstacle"),
        setOf("attack", "threat"),
        setOf("consumption", "consume"),
        setOf("interaction", "collaboration"),
        setOf("influence", "impact"),
        setOf("selectively", "select"),
        setOf("convention", "norm"),
        setOf("emotional", "emotion"),
        setOf("accidental", "accidentally"),
        setOf("significant", "momentous"),
        setOf("prospect", "projection"),
        setOf("precise", "precision"),
        setOf("observation", "observe"),
        setOf("competitive", "competition"),
        setOf("reflectively", "critically"),
        setOf("rare", "exotic"),
        setOf("repetition", "reoccurring"),
        setOf("pre-registration", "registration"),
        setOf("belonging", "identity"),
        setOf("unbalanced", "imbalanced"),
        setOf("contradict", "differ"),
        setOf("efficiently", "effectively"),
        setOf("securely", "safely")
    )

    fun posLabelKo(pos: String): String = POS_LABELS_KO[pos.trim().lowercase(Locale.US)] ?: "어휘"

    fun difficultyLabelKo(day: Int): String = when {
        day <= 3 -> "기초"
        day <= 8 -> "중급"
        else -> "심화"
    }

    fun formatQuestionNumberLabel(questionNum: Int): String = when {
        questionNum in 41..42 -> "41~42번"
        questionNum >= 43 -> "43~45번"
        else -> "${questionNum}번"
    }

    fun todayKstIso(nowMs: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return fmt.format(Date(nowMs))
    }

    fun isDayUnlocked(
        day: Int,
        state: WordRunChallengeState,
        todayKst: String = todayKstIso()
    ): Boolean {
        if (day <= 1) return true
        if (state.freePracticeUnlocked) return true
        val prev = state.dayProgress[day - 1] ?: return false
        return prev.passedDateKst.isNotBlank() && prev.passedDateKst < todayKst
    }

    fun resolveCurrentDay(
        state: WordRunChallengeState,
        todayKst: String = todayKstIso()
    ): Int {
        for (d in 1..TOTAL_DAYS) {
            val prog = state.dayProgress[d]
            if ((prog == null || prog.passedDateKst.isBlank()) && isDayUnlocked(d, state, todayKst)) {
                return d
            }
        }
        for (d in TOTAL_DAYS downTo 1) {
            if (isDayUnlocked(d, state, todayKst)) return d
        }
        return 1
    }

    fun plural(word: String): String {
        val w = word.trim().lowercase(Locale.US)
        if (w == "pupa") return "pupae"
        if (w in UNCHANGED_NOUNS) return w
        return when {
            Regex("[^aeiou]y$", RegexOption.IGNORE_CASE).containsMatchIn(w) -> w.dropLast(1) + "ies"
            Regex("(s|x|z|ch|sh)$", RegexOption.IGNORE_CASE).containsMatchIn(w) -> w + "es"
            else -> w + "s"
        }
    }

    fun verbForms(word: String): List<String> {
        val w = word.trim().lowercase(Locale.US)
        IRREGULAR_VERBS[w]?.let { return it }
        val third = plural(w)
        val past = when {
            Regex("[^aeiou]y$").containsMatchIn(w) -> w.dropLast(1) + "ied"
            w.endsWith("e") -> w + "d"
            else -> w + "ed"
        }
        val ing = when {
            w.endsWith("ie") -> w.dropLast(2) + "ying"
            w.endsWith("e") && !Regex("(ee|ye|oe)$").containsMatchIn(w) -> w.dropLast(1) + "ing"
            else -> w + "ing"
        }
        return listOf(third, past, past, ing)
    }

    fun variants(word: String): List<String> {
        val base = word.trim().lowercase(Locale.US)
        if (base.isBlank()) return emptyList()
        val set = linkedSetOf<String>()
        set += base
        set += plural(base)
        set.addAll(verbForms(base))
        if (base.endsWith("ise")) {
            val ize = base.dropLast(3) + "ize"
            set += listOf(ize, ize + "d", ize.dropLast(1) + "ing")
        }
        if (base.endsWith("ize")) {
            val ise = base.dropLast(3) + "ise"
            set += listOf(ise, ise + "d", ise.dropLast(1) + "ing")
        }
        if (base.contains("-")) {
            set += base.replace("-", " ")
        }
        if (base == "focus") set += listOf("focussed", "focussing")
        if (base == "recognize") set += listOf("recognise", "recognised", "recognising")
        return set.sortedByDescending { it.length }
    }

    fun findMatchedForms(entry: WordRunEntry, sentence: String): List<String> {
        if (sentence.isBlank()) return emptyList()
        val pattern = variants(entry.word).joinToString("|") { Regex.escape(it) }
        if (pattern.isBlank()) return emptyList()
        val rx = Regex("\\b(?:$pattern)\\b", RegexOption.IGNORE_CASE)
        return rx.findAll(sentence)
            .map { it.value.lowercase(Locale.US) }
            .distinct()
            .toList()
    }

    fun resolveSurfaceSpanInSentence(entry: WordRunEntry, sentence: String): Pair<Int, Int> {
        if (sentence.isBlank()) return -1 to -1
        val pattern = variants(entry.word).joinToString("|") { Regex.escape(it) }
        if (pattern.isBlank()) return -1 to -1
        val match = Regex("\\b(?:$pattern)\\b", RegexOption.IGNORE_CASE).find(sentence)
        return if (match != null) match.range.first to (match.range.last + 1) else -1 to -1
    }

    fun buildMorphologyInfo(entry: WordRunEntry, sentence: String): WordRunMorphologyInfo {
        val base = entry.word.trim().lowercase(Locale.US)
        val forms = verbForms(base)
        val matched = findMatchedForms(entry, sentence)

        val chips: List<WordRunMorphologyChip>
        val noteKo: String
        when (entry.pos.trim().lowercase(Locale.US)) {
            "v" -> {
                chips = listOf(
                    WordRunMorphologyChip("원형", entry.word),
                    WordRunMorphologyChip("3인칭 단수", forms[0]),
                    WordRunMorphologyChip("과거", forms[1]),
                    WordRunMorphologyChip("과거분사", forms[2]),
                    WordRunMorphologyChip("현재분사·동명사", forms[3])
                )
                noteKo = "과거형은 시제, 과거분사는 완료·수동·수식, -ing는 진행·동명사·분사 역할을 합니다."
            }
            "n" -> {
                val pl = plural(base)
                chips = buildList {
                    add(WordRunMorphologyChip("기본형", entry.word))
                    if (base !in UNCHANGED_NOUNS && pl != base) {
                        add(WordRunMorphologyChip("복수형 후보", pl))
                    }
                }
                noteKo = "명사는 주어·목적어·보어로 쓰이며, 가산성·단복수 수일치를 함께 확인하세요."
            }
            "adj" -> {
                chips = listOf(WordRunMorphologyChip("형용사", entry.word))
                noteKo = if (base.endsWith("ed") || base.endsWith("ing")) {
                    "분사형 형용사입니다. -ed는 감정·상태를, -ing는 유발·성질을 나타내는 경우가 많습니다."
                } else {
                    "명사를 앞뒤에서 수식하거나 보어 자리에서 주어·목적어의 상태를 설명합니다."
                }
            }
            else -> {
                chips = listOf(WordRunMorphologyChip("부사", entry.word))
                noteKo = "동사·형용사·다른 부사 또는 문장 전체의 뉘앙스를 수식합니다."
            }
        }

        val sentenceMatches = matched.map { token ->
            val label = when {
                token == base -> "기본형"
                entry.pos == "v" && token == forms[1] && token == forms[2] -> "과거형 / 과거분사"
                entry.pos == "v" && token == forms[1] -> "과거형"
                entry.pos == "v" && token == forms[2] -> "과거분사"
                entry.pos == "v" && token == forms[3] -> "현재분사 / 동명사"
                entry.pos == "v" && token == forms[0] -> "3인칭 단수"
                entry.pos == "n" && token == plural(base) -> "복수형"
                else -> "활용형"
            }
            WordRunMorphologyChip(label, token)
        }

        return WordRunMorphologyInfo(
            posLabelKo = posLabelKo(entry.pos),
            formChips = chips,
            sentenceMatches = sentenceMatches,
            noteKo = noteKo
        )
    }

    fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }

    fun isCompatibleDistractor(a: WordRunEntry, b: WordRunEntry): Boolean {
        if (a.id == b.id) return false
        if (a.meaningKo.trim() == b.meaningKo.trim()) return false
        val aw = a.word.lowercase(Locale.US)
        val bw = b.word.lowercase(Locale.US)
        if (SYNONYM_GROUPS.any { aw in it && bw in it }) return false
        if (a.meaningKo.length > 2 && b.meaningKo.contains(a.meaningKo)) return false
        if (b.meaningKo.length > 2 && a.meaningKo.contains(b.meaningKo)) return false
        return true
    }

    fun buildQuizQuestion(
        target: WordRunEntry,
        allWords: List<WordRunEntry>,
        reverseKoreanToEnglish: Boolean,
        random: Random = Random.Default
    ): WordRunQuizQuestion {
        val candidates = allWords
            .filter { isCompatibleDistractor(target, it) }
            .shuffled(random)
            .map { cand ->
                val posScore = if (cand.pos == target.pos) 7.0 else 0.0
                val dayScore = when {
                    cand.day == target.day -> 5.0
                    cand.difficulty == target.difficulty -> 3.0
                    else -> 0.0
                }
                val editScore = if (reverseKoreanToEnglish) {
                    max(0, 4 - levenshteinDistance(target.word, cand.word)).toDouble()
                } else {
                    0.0
                }
                cand to (posScore + dayScore + editScore + random.nextDouble() * 3.0)
            }
            .sortedByDescending { it.second }
            .map { it.first }

        val picks = mutableListOf(target)
        for (cand in candidates) {
            if (picks.all { existing -> isCompatibleDistractor(existing, cand) }) {
                picks += cand
                if (picks.size == 4) break
            }
        }
        // Fallback fill if pool is very small in unit tests
        if (picks.size < 4) {
            allWords.filter { cand -> picks.none { it.id == cand.id } }
                .take(4 - picks.size)
                .forEach { picks += it }
        }
        return WordRunQuizQuestion(
            targetWordId = target.id,
            reverseKoreanToEnglish = reverseKoreanToEnglish,
            optionWordIds = picks.map { it.id }.shuffled(random)
        )
    }

    /**
     * Generates a balanced 50-question exam (25 EN->KO + 25 KO->EN shuffled) for a given day.
     */
    fun buildExamQuestions(
        dayWords: List<WordRunEntry>,
        allWords: List<WordRunEntry>,
        random: Random = Random.Default
    ): List<WordRunQuizQuestion> {
        val shuffledWords = dayWords.shuffled(random)
        val half = shuffledWords.size / 2
        val directions = List(shuffledWords.size) { idx -> idx < half }.shuffled(random)
        return shuffledWords.mapIndexed { idx, entry ->
            buildQuizQuestion(
                target = entry,
                allWords = allWords,
                reverseKoreanToEnglish = directions[idx],
                random = random
            )
        }
    }

    fun buildPreviousDayReviewPool(
        day: Int,
        allWords: List<WordRunEntry>,
        previousProgress: WordRunDayProgress?,
        random: Random = Random.Default
    ): List<WordRunEntry> {
        if (day <= 1) return emptyList()
        val prevDayWords = allWords.filter { it.day == day - 1 }
        val byId = prevDayWords.associateBy { it.id }
        val priorWrong = previousProgress?.wrongWordIds.orEmpty().mapNotNull { byId[it] }.distinctBy { it.id }
        val wrongIdSet = priorWrong.map { it.id }.toSet()
        val fillers = prevDayWords.filterNot { it.id in wrongIdSet }.shuffled(random)
        return (priorWrong + fillers).take(REVIEW_QUESTION_COUNT)
    }

    fun gradeExam(
        day: Int,
        questions: List<WordRunQuizQuestion>,
        answersByIndex: Map<Int, Int>,
        elapsedSec: Int
    ): WordRunExamSessionResult {
        var score = 0
        val wrongIds = mutableListOf<Int>()
        questions.forEachIndexed { idx, q ->
            val chosen = answersByIndex[idx]
            if (chosen == q.targetWordId) {
                score++
            } else {
                wrongIds += q.targetWordId
            }
        }
        val answered = answersByIndex.size
        val passThreshold = if (questions.size == WORDS_PER_DAY) EXAM_PASS_SCORE else (questions.size * 0.9).toInt()
        val passed = answered == questions.size && score >= passThreshold
        return WordRunExamSessionResult(
            day = day,
            score = score,
            total = questions.size,
            answered = answered,
            passed = passed,
            wrongWordIds = wrongIds.distinct(),
            elapsedSec = elapsedSec.coerceIn(0, EXAM_DURATION_SEC)
        )
    }

    fun validateUserContext(entry: WordRunEntry, en: String, ko: String): String? {
        val cleanEn = en.trim()
        if (cleanEn.isBlank()) return "영어 문장을 입력해 주세요."
        if (cleanEn.length > MAX_CONTEXT_LENGTH) return "영어 문장은 ${MAX_CONTEXT_LENGTH}자 이하로 입력하세요."
        if (ko.trim().length > MAX_CONTEXT_LENGTH) return "한국어 해석은 ${MAX_CONTEXT_LENGTH}자 이하로 입력하세요."
        if (findMatchedForms(entry, cleanEn).isEmpty()) {
            return "문장에 대상 단어('${entry.word}') 또는 활용형이 포함되어야 합니다."
        }
        return null
    }

    /**
     * Converts any 2027 Word Run entry into a standard `VocabWord` so missed words
     * seamlessly enter the app's native FSRS spaced-repetition deck.
     */
    fun toVocabWord(entry: WordRunEntry): VocabWord {
        val posText = when (entry.pos.lowercase(Locale.US)) {
            "n" -> "n."
            "v" -> "v."
            "adj" -> "adj."
            "adv" -> "adv."
            else -> "n."
        }
        val defaultEn = entry.exampleEn.ifBlank {
            "Students carefully review the meaning of ${entry.word} in context."
        }
        val defaultKo = entry.exampleKo.ifBlank {
            "학생들은 문맥 속에서 ${entry.word}(${entry.meaningKo})의 의미를 꼼꼼히 복습한다."
        }
        return VocabWord(
            id = "wr2027_${entry.id}",
            grade = entry.difficulty.coerceIn(1, 3),
            textbookUnitKo = "WORD RUN 2027 · DAY ${entry.day} (${difficultyLabelKo(entry.day)})",
            csatFrequency = (6 - entry.difficulty).coerceIn(3, 5),
            csatDifficulty = entry.difficulty.coerceIn(1, 3),
            topic = VocabTopic.SCHOOL_EXAM,
            word = entry.word,
            ipa = "",
            partOfSpeech = posText,
            meaningKo = entry.meaningKo,
            exampleEn = defaultEn,
            exampleKo = defaultKo,
            examples = listOf(
                VocabExample(
                    labelKo = "2027 챌린지 예문",
                    english = defaultEn,
                    korean = defaultKo
                )
            ),
            pronunciationTipKo = "2027 수능 ${formatQuestionNumberLabel(entry.questionNum)} · DAY ${entry.day} 핵심 어휘",
            distractors = emptyList()
        )
    }
}
