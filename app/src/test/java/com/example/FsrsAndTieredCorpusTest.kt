package com.example

import com.example.data.ExamCorpusRepository
import com.example.engine.ExamCorpusAnalyticsEngine
import com.example.engine.FsrsSchedulerEngine
import com.example.engine.RecallGrade
import com.example.engine.StudyPhase
import com.example.engine.WordLearningProgress
import com.example.engine.WordLearningStatus
import com.example.model.CurriculumGroup2022
import com.example.model.VocabDifficultyTier
import com.example.model.WordAnalyticsFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsAndTieredCorpusTest {

    @Test
    fun `Gries DP_norm and uniformity handle uniform, bursty, and empty distributions`() {
        val uniform = ExamCorpusAnalyticsEngine.computeGriesDp(
            countsPerPart = listOf(10, 10, 10, 10),
            tokensPerPart = listOf(1000, 1000, 1000, 1000)
        )
        assertEquals(0f, uniform.dp, 0.001f)
        assertEquals(0f, uniform.dpNorm, 0.001f)
        assertEquals(1f, uniform.uniformity, 0.001f)

        val bursty = ExamCorpusAnalyticsEngine.computeGriesDp(
            countsPerPart = listOf(40, 0, 0, 0),
            tokensPerPart = listOf(1000, 1000, 1000, 1000)
        )
        assertEquals(1f, bursty.dpNorm, 0.001f)
        assertEquals(0f, bursty.uniformity, 0.001f)

        val empty = ExamCorpusAnalyticsEngine.computeGriesDp(emptyList(), emptyList())
        assertEquals(0f, empty.uniformity, 0.001f)
    }

    @Test
    fun `recurrence metrics compute mean, median, max gap and overdue years accurately`() {
        val rec = ExamCorpusAnalyticsEngine.computeRecurrenceMetrics(
            csatYears = listOf(2019, 2021, 2024, 2027),
            latestYear = 2027
        )
        assertEquals(2.67f, rec.meanGapYr ?: -1f, 0.01f)
        assertEquals(3.0f, rec.medianGapYr ?: -1f, 0.01f)
        assertEquals(3, rec.maxGapYr)
        assertEquals(2027, rec.lastCsatYear)
        assertEquals(0, rec.overdueYr)

        val overdue = ExamCorpusAnalyticsEngine.computeRecurrenceMetrics(
            csatYears = listOf(2018, 2020, 2022),
            latestYear = 2027
        )
        assertEquals(5, overdue.overdueYr)
    }

    @Test
    fun `5-tier classifier separates Tier S to D and blocks footnote contamination into Tier S or A`() {
        val tierS = ExamCorpusAnalyticsEngine.classifyDifficultyTier(
            uniformity = 0.78f,
            csatYearsCount = 5,
            meanGapYr = 2.0f,
            mockToCsatTransferHits = 3,
            curriculumGroup = CurriculumGroup2022.G1_ELEM_800,
            flags = WordAnalyticsFlags()
        )
        assertEquals(VocabDifficultyTier.TIER_S_CORE, tierS)

        val tierA = ExamCorpusAnalyticsEngine.classifyDifficultyTier(
            uniformity = 0.55f,
            csatYearsCount = 4,
            meanGapYr = 2.5f,
            mockToCsatTransferHits = 2,
            curriculumGroup = CurriculumGroup2022.G2_MID_H1_1200,
            flags = WordAnalyticsFlags()
        )
        assertEquals(VocabDifficultyTier.TIER_A_CYCLICAL, tierA)

        val tierB = ExamCorpusAnalyticsEngine.classifyDifficultyTier(
            uniformity = 0.25f,
            csatYearsCount = 1,
            meanGapYr = null,
            mockToCsatTransferHits = 0,
            curriculumGroup = CurriculumGroup2022.OOV,
            flags = WordAnalyticsFlags(isNewRecent3Yr = true)
        )
        assertEquals(VocabDifficultyTier.TIER_B_ADVANCED_NEW, tierB)

        val tierC = ExamCorpusAnalyticsEngine.classifyDifficultyTier(
            uniformity = 0.10f,
            csatYearsCount = 1,
            meanGapYr = null,
            mockToCsatTransferHits = 0,
            curriculumGroup = CurriculumGroup2022.G3_H2_H3_1000,
            flags = WordAnalyticsFlags()
        )
        assertEquals(VocabDifficultyTier.TIER_C_BURSTY, tierC)

        // Even with high uniformity and CSAT count, a footnote word MUST be isolated to Tier D
        val tierD = ExamCorpusAnalyticsEngine.classifyDifficultyTier(
            uniformity = 0.90f,
            csatYearsCount = 8,
            meanGapYr = 1.0f,
            mockToCsatTransferHits = 6,
            curriculumGroup = CurriculumGroup2022.G1_ELEM_800,
            flags = WordAnalyticsFlags(isFootnoteEver = true)
        )
        assertEquals(VocabDifficultyTier.TIER_D_FOOTNOTE, tierD)
    }

    @Test
    fun `FTS5 query sanitizer removes special operators and prevents hyphen syntax crashes`() {
        assertEquals(
            "\"self\"* AND \"driving\"* AND \"energy\"* AND \"test\"*",
            ExamCorpusAnalyticsEngine.sanitizeFtsQuery("""self-driving "energy": (test)^~""")
        )
        assertEquals("", ExamCorpusAnalyticsEngine.sanitizeFtsQuery("   --- \"\" ()   "))
    }

    @Test
    fun `Kotlin native FSRS scheduler enforces D-Day 3-phase guardrails and migrates legacy SM-2 records`() {
        val buildPolicy = FsrsSchedulerEngine.resolveDDayPolicy(90)
        assertEquals(StudyPhase.PHASE_1_BUILD, buildPolicy.phase)
        assertEquals(25, buildPolicy.newCardsPerDay)
        assertEquals(0.90, buildPolicy.desiredRetention, 0.001)

        val consolidatePolicy = FsrsSchedulerEngine.resolveDDayPolicy(30)
        assertEquals(StudyPhase.PHASE_2_CONSOLIDATE, consolidatePolicy.phase)
        assertEquals(5, consolidatePolicy.newCardsPerDay)
        assertEquals(0.92, consolidatePolicy.desiredRetention, 0.001)

        val lockdownPolicy = FsrsSchedulerEngine.resolveDDayPolicy(10)
        assertEquals(StudyPhase.PHASE_3_LOCKDOWN, lockdownPolicy.phase)
        assertEquals(0, lockdownPolicy.newCardsPerDay)
        assertEquals(0.95, lockdownPolicy.desiredRetention, 0.001)
        assertFalse(FsrsSchedulerEngine.isNewWordAllowed(daysUntilCsat = 10, newWordsStudiedToday = 0))

        // Migrate a legacy SM-2 record (fsrsStabilityDays == 0.0, intervalDays == 6, easeFactor == 2.5)
        val t0 = 1_750_000_000_000L
        val legacySm2 = WordLearningProgress(
            wordId = "w_legacy",
            learningStatus = WordLearningStatus.REVIEW,
            repetition = 2,
            intervalDays = 6,
            easeFactor = 2.5,
            lastReviewedAtMs = t0 - 6L * 86_400_000L,
            consecutiveCorrect = 2,
            correctCount = 2,
            masteryScore = 48
        )
        val upgraded = FsrsSchedulerEngine.review(
            existing = legacySm2,
            wordId = "w_legacy",
            grade = RecallGrade.GOOD,
            daysUntilCsat = 90,
            nowMs = t0
        )
        assertTrue(upgraded.fsrsStabilityDays > 6.0)
        assertTrue(upgraded.intervalDays >= 7)
        assertEquals(WordLearningStatus.MASTERED, upgraded.learningStatus)

        // Lapse drops interval to 0 (10 minutes) and preserves stability > 0
        val lapsed = FsrsSchedulerEngine.review(
            existing = upgraded,
            wordId = "w_legacy",
            grade = RecallGrade.AGAIN,
            daysUntilCsat = 90,
            nowMs = t0 + 10L * 86_400_000L
        )
        assertEquals(0, lapsed.intervalDays)
        assertEquals(0, lapsed.repetition)
        assertTrue(lapsed.fsrsStabilityDays > 0.0)
    }

    @Test
    fun `search index hides Tier D footnote words by default and shows them when explicitly filtered`() {
        val snapshot = ExamCorpusRepository.loadSnapshot(context = null)
        val defaultVisible = snapshot.searchIndex.query(includeFootnoteTierD = false)
        assertTrue(defaultVisible.none { it.analytics.difficultyTier == VocabDifficultyTier.TIER_D_FOOTNOTE })

        val tierDOnly = snapshot.searchIndex.query(
            difficultyTierFilter = VocabDifficultyTier.TIER_D_FOOTNOTE,
            includeFootnoteTierD = true
        )
        assertTrue(tierDOnly.isNotEmpty())
        assertTrue(tierDOnly.all { it.analytics.difficultyTier == VocabDifficultyTier.TIER_D_FOOTNOTE })

        val paged = ExamCorpusRepository.queryOccurrencesPaged(
            context = null,
            entry = defaultVisible.first(),
            limit = 15,
            offset = 0
        )
        assertTrue(paged.items.isNotEmpty())
        assertNotNull(paged.sourceLabelKo)
    }
}
