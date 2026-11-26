package com.example.model

/**
 * Data models for the 2027 CSAT 500-Word 10-Day Challenge (WORD RUN 2027).
 * Kept cleanly decoupled from legacy 105-word VocabRepository tests while bridging into FSRS & SQLite.
 */
data class WordRunEntry(
    val id: Int,
    val day: Int,
    val word: String,
    val meaningKo: String,
    val questionNum: Int,
    val difficulty: Int,
    val pos: String,
    val exampleEn: String = "",
    val exampleKo: String = ""
)

data class WordRunUserContext(
    val en: String,
    val ko: String,
    val label: String = "직접 등록 · 학습 문맥"
)

data class WordRunDayProgress(
    val day: Int,
    val reviewCompleted: Boolean = false,
    val learnedIds: Set<Int> = emptySet(),
    val practicedIds: Set<Int> = emptySet(),
    val passedDateKst: String = "",
    val wrongWordIds: List<Int> = emptyList()
)

data class WordRunExamRecord(
    val id: String,
    val day: Int,
    val score: Int,
    val total: Int,
    val answered: Int,
    val passed: Boolean,
    val elapsedSec: Int,
    val timestampIso: String,
    val wrongWordIds: List<Int> = emptyList()
)

data class WordRunChallengeState(
    val selectedDay: Int = 1,
    val freePracticeUnlocked: Boolean = false,
    val dayProgress: Map<Int, WordRunDayProgress> = emptyMap(),
    val studyAgainCounts: Map<Int, Int> = emptyMap(),
    val userContexts: Map<Int, WordRunUserContext> = emptyMap(),
    val examHistory: List<WordRunExamRecord> = emptyList()
)

data class WordRunMorphologyChip(
    val labelKo: String,
    val form: String
)

data class WordRunMorphologyInfo(
    val posLabelKo: String,
    val formChips: List<WordRunMorphologyChip>,
    val sentenceMatches: List<WordRunMorphologyChip>,
    val noteKo: String
)

data class WordRunQuizQuestion(
    val targetWordId: Int,
    val reverseKoreanToEnglish: Boolean,
    val optionWordIds: List<Int>
)

data class WordRunExamSessionResult(
    val day: Int,
    val score: Int,
    val total: Int,
    val answered: Int,
    val passed: Boolean,
    val wrongWordIds: List<Int>,
    val elapsedSec: Int
)
