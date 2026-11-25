package com.example

import com.example.model.VocabTopic
import com.example.model.VocabWord
import com.example.model.WrongNoteRecord
import com.example.ui.components.WrongNoteFilterTab
import com.example.ui.components.buildWrongNoteDisplayItems
import com.example.ui.components.filterWrongNoteItems
import com.example.ui.components.formatWrongNoteStudyDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WrongNoteFilterAndDateTest {

    private fun sampleWord(id: String, word: String): VocabWord = VocabWord(
        id = id,
        grade = 1,
        textbookUnitKo = "고1 핵심",
        csatFrequency = 5,
        csatDifficulty = 2,
        topic = VocabTopic.SCHOOL_EXAM,
        word = word,
        ipa = "/test/",
        partOfSpeech = "n.",
        meaningKo = "테스트 뜻",
        exampleEn = "Example sentence.",
        exampleKo = "예문 해석.",
        examples = emptyList(),
        pronunciationTipKo = "발음 팁",
        distractors = listOf("오답1", "오답2", "오답3")
    )

    @Test
    fun `buildWrongNoteDisplayItems sorts newest studied first and filters cleanly by mastered status`() {
        val words = listOf(
            sampleWord("w1", "resilient"),
            sampleWord("w2", "crucial"),
            sampleWord("w3", "abundant")
        )
        val records = mapOf(
            "w1" to WrongNoteRecord(
                wordId = "w1",
                savedDateIso = "2026-11-20",
                lastStudiedDateIso = "2026-11-22",
                lastStudiedAtMs = 1_000L,
                isMastered = true
            ),
            "w2" to WrongNoteRecord(
                wordId = "w2",
                savedDateIso = "2026-11-24",
                lastStudiedDateIso = "2026-11-24",
                lastStudiedAtMs = 3_000L,
                isMastered = false
            ),
            "w3" to WrongNoteRecord(
                wordId = "w3",
                savedDateIso = "2026-11-21",
                lastStudiedDateIso = "2026-11-23",
                lastStudiedAtMs = 2_000L,
                isMastered = true
            )
        )

        val all = buildWrongNoteDisplayItems(words, records)
        assertEquals(listOf("w2", "w3", "w1"), all.map { it.word.id })

        val needsStudy = filterWrongNoteItems(all, WrongNoteFilterTab.NEEDS_STUDY)
        assertEquals(listOf("w2"), needsStudy.map { it.word.id })

        val mastered = filterWrongNoteItems(all, WrongNoteFilterTab.MASTERED)
        assertEquals(listOf("w3", "w1"), mastered.map { it.word.id })

        val allTab = filterWrongNoteItems(all, WrongNoteFilterTab.ALL)
        assertEquals(3, allTab.size)
    }

    @Test
    fun `formatWrongNoteStudyDate formats today and past ISO dates clearly`() {
        assertEquals(
            "오늘 학습 · 2026.11.24",
            formatWrongNoteStudyDate("2026-11-24", todayIso = "2026-11-24")
        )
        assertEquals(
            "학습일 2026.11.21",
            formatWrongNoteStudyDate("2026-11-21", todayIso = "2026-11-24")
        )
        assertEquals(
            "학습일 기록 없음",
            formatWrongNoteStudyDate("   ", todayIso = "2026-11-24")
        )
    }
}
