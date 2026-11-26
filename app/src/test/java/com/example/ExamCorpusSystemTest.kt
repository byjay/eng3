package com.example

import com.example.data.ExamCorpusRepository
import com.example.engine.ExamCorpusAnalyticsEngine
import com.example.model.CefrLevel
import com.example.model.ExamOccurrenceRecord
import com.example.model.ExamPriorityTier
import com.example.model.ExamType
import com.example.model.GrammarCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamCorpusSystemTest {

    @Test
    fun `lemmatizer normalizes English inflections and irregular forms accurately`() {
        val snapshot = ExamCorpusRepository.loadSnapshot(context = null)
        assertEquals("crucial", snapshot.searchIndex.findByTokenOrLemma("crucially")?.lemma)
        assertEquals("analyze", snapshot.searchIndex.findByTokenOrLemma("analyzed")?.lemma)
        assertEquals("combine", snapshot.searchIndex.findByTokenOrLemma("combining")?.lemma)
        assertEquals("consistent", snapshot.searchIndex.findByTokenOrLemma("consistently")?.lemma)
        assertEquals("child", ExamCorpusAnalyticsEngine.lemmatize("children"))
        assertEquals("go", ExamCorpusAnalyticsEngine.lemmatize("went"))
    }

    @Test
    fun `10-year quantitative analytics computes dispersion, carryover, evergreen and emergent tiers`() {
        val evergreenOccurrences = listOf(
            ExamOccurrenceRecord("1", 2018, 6, ExamType.MOCK_G3, "빈칸 추론", 31, "crucial", "s1", "k1", "n1"),
            ExamOccurrenceRecord("2", 2020, 11, ExamType.CSAT, "빈칸 추론", 32, "crucial", "s2", "k2", "n2"),
            ExamOccurrenceRecord("3", 2022, 9, ExamType.MOCK_G3, "주제", 23, "crucially", "s3", "k3", "n3"),
            ExamOccurrenceRecord("4", 2024, 11, ExamType.CSAT, "어휘", 30, "crucial", "s4", "k4", "n4"),
            ExamOccurrenceRecord("5", 2026, 6, ExamType.MOCK_G3, "요지", 22, "crucial", "s5", "k5", "n5")
        )
        val evergreenStats = ExamCorpusAnalyticsEngine.computeWordAnalytics(
            occurrences = evergreenOccurrences,
            cefr = CefrLevel.B2,
            isMoeBasic = true,
            grade = 2
        )
        assertEquals(5, evergreenStats.totalFrequency)
        assertEquals(2, evergreenStats.csatFrequency)
        assertEquals(3, evergreenStats.mockFrequency)
        assertTrue(evergreenStats.hasMockToCsatCarryover)
        assertEquals(0.5f, evergreenStats.yearDispersion, 0.01f)
        assertEquals(2.0f, evergreenStats.avgRecurrenceGapYears, 0.01f)
        assertTrue(evergreenStats.isEvergreenCore)
        assertFalse(evergreenStats.isNewlyEmergent)
        assertEquals(ExamPriorityTier.MUST_KNOW_CORE, evergreenStats.priorityTier)

        val emergentOccurrences = listOf(
            ExamOccurrenceRecord("e1", 2025, 9, ExamType.MOCK_G3, "빈칸 추론", 34, "pragmatic", "s", "k", "n"),
            ExamOccurrenceRecord("e2", 2026, 11, ExamType.CSAT, "빈칸 추론", 33, "pragmatic", "s2", "k2", "n2")
        )
        val emergentStats = ExamCorpusAnalyticsEngine.computeWordAnalytics(
            occurrences = emergentOccurrences,
            cefr = CefrLevel.C1,
            isMoeBasic = false,
            grade = 3
        )
        assertTrue(emergentStats.isNewlyEmergent)
        assertEquals(ExamPriorityTier.NEWLY_EMERGENT, emergentStats.priorityTier)
    }

    @Test
    fun `corpus snapshot covers all 105 curated words and all 6 core grammar categories`() {
        val snapshot = ExamCorpusRepository.loadSnapshot(context = null)
        assertEquals(105, snapshot.entries.size)
        assertTrue(snapshot.summary.totalOccurrences >= 200)
        assertTrue(snapshot.summary.mockToCsatCarryoverPercent in 1..100)

        val coveredCategories = snapshot.grammarSentences.map { it.category }.toSet()
        GrammarCategory.entries.forEach { category ->
            assertTrue("Missing grammar category: ${category.code}", category in coveredCategories)
        }
    }

    @Test
    fun `search index filters cleanly by tier, CEFR, carryover, and inflected search query`() {
        val snapshot = ExamCorpusRepository.loadSnapshot(context = null)

        val inflectedHits = snapshot.searchIndex.query(rawQuery = "crucially")
        assertTrue(inflectedHits.isNotEmpty())
        assertEquals("crucial", inflectedHits.first().lemma)
        assertEquals("crucial", snapshot.searchIndex.resolveNormalizedLemma("crucially"))

        val c1Only = snapshot.searchIndex.query(cefrFilter = CefrLevel.C1)
        assertTrue(c1Only.isNotEmpty())
        assertTrue(c1Only.all { it.cefr == CefrLevel.C1 })

        val carryoverOnly = snapshot.searchIndex.query(onlyMockToCsatCarryover = true)
        assertTrue(carryoverOnly.isNotEmpty())
        assertTrue(carryoverOnly.all { it.analytics.hasMockToCsatCarryover })
    }

    @Test
    fun `external official JSON parser validates schema v1 safely and rejects malformed input`() {
        val validJson = """
            {
              "schemaVersion": "1.0.0",
              "entries": [
                {
                  "lemma": "crucial",
                  "occurrences": [
                    {
                      "id": "kice_2025_31",
                      "year": 2025,
                      "month": 11,
                      "examType": "CSAT",
                      "itemTypeKo": "빈칸 추론",
                      "itemNumber": 31,
                      "surfaceForm": "crucial",
                      "sentenceEn": "Accurate analysis plays a crucial role in scientific progress.",
                      "sentenceKo": "정확한 분석은 과학 발전에 결정적인 역할을 한다.",
                      "structureNoteKo": "단수 주어 수일치 + 핵심 형용사 수식",
                      "grammarTags": ["SUBJECT_VERB_AGREEMENT"],
                      "provenance": "OFFICIAL_IMPORT"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = ExamCorpusRepository.parseExternalOccurrencesJson(validJson)
        assertTrue(parsed.isSuccess)
        val records = parsed.getOrNull()?.get("crucial")
        assertNotNull(records)
        assertEquals(1, records?.size)
        assertEquals(2025, records?.first()?.year)
        assertEquals(ExamType.CSAT, records?.first()?.examType)

        assertTrue(ExamCorpusRepository.parseExternalOccurrencesJson("").isFailure)
        assertTrue(ExamCorpusRepository.parseExternalOccurrencesJson("{not-json").isFailure)
        assertTrue(ExamCorpusRepository.parseExternalOccurrencesJson("""{"schemaVersion":"1.0"}""").isFailure)
    }
}
