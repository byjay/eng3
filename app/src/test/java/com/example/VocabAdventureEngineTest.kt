package com.example

import com.example.data.VocabRepository
import com.example.engine.AchievementBadgeEngine
import com.example.engine.DailyMissionEngine
import com.example.engine.EnGameMathEngine
import com.example.engine.EnGameMode
import com.example.engine.ExplorerJourneyEngine
import com.example.engine.LevelConfig
import com.example.engine.MediaReviewStatus
import com.example.engine.MissionCadence
import com.example.engine.MissionKind
import com.example.engine.MissionStatus
import com.example.engine.RecallGrade
import com.example.engine.RewardConfig
import com.example.engine.RewardEventType
import com.example.engine.RewardLedgerEngine
import com.example.engine.RewardTransaction
import com.example.engine.Sm2ReviewScheduler
import com.example.engine.TypedRecallEvaluator
import com.example.engine.TypedRecallOutcome
import com.example.engine.VocabAnalyticsTracker
import com.example.engine.VocabMediaResolver
import com.example.engine.VocabMnemonicEngine
import com.example.engine.WordLearningProgress
import com.example.engine.WordLearningStatus
import com.example.engine.WordVisualMotif
import com.example.model.VocabTopic
import com.example.model.VocabWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabAdventureEngineTest {

    private fun sampleWord(id: String, word: String, meaningKo: String) = VocabWord(
        id = id,
        grade = 2,
        textbookUnitKo = "수능 핵심",
        csatFrequency = 5,
        csatDifficulty = 3,
        topic = VocabTopic.SCHOOL_EXAM,
        word = word,
        ipa = "/sample/",
        partOfSpeech = "v.",
        meaningKo = meaningKo,
        exampleEn = "Students use $word effectively in essays.",
        exampleKo = "학생들은 에세이에서 $meaningKo 표현을 효과적으로 사용한다.",
        examples = emptyList(),
        pronunciationTipKo = "강세를 또렷하게 발음하세요.",
        distractors = listOf("오답1", "오답2", "오답3")
    )

    @Test
    fun verifiedMorphemeReturnsRealBreakdown_andUnverifiedReturnsHonestFallback() {
        val verifiedWord = sampleWord("v1", "alleviate", "완화하다")
        val verifiedGuide = VocabMnemonicEngine.buildStructuredGuide(verifiedWord)
        assertTrue(verifiedGuide.hasVerifiedMorpheme)
        assertFalse(verifiedGuide.needsHumanReview)
        assertTrue(verifiedGuide.morphemeBreakdown.contains("lev"))
        assertFalse(verifiedGuide.sceneDescriptionKo.contains("해마"))

        val unverifiedWord = sampleWord("v2", "quixotic", "공상적인")
        val unverifiedGuide = VocabMnemonicEngine.buildStructuredGuide(unverifiedWord)
        assertFalse(unverifiedGuide.hasVerifiedMorpheme)
        assertTrue(unverifiedGuide.needsHumanReview)
        assertEquals("어근 분석 자료 없음", unverifiedGuide.morphemeBreakdown)
        assertFalse(unverifiedGuide.sceneDescriptionKo.contains("해마"))
    }

    @Test
    fun sm2SchedulerGrowsIntervalAndResetsOnAgainAndFiltersDueQueue() {
        val t0 = 1_700_000_000_000L
        val step1 = Sm2ReviewScheduler.review(null, "w1", RecallGrade.GOOD, nowMs = t0)
        assertEquals(1, step1.repetition)
        assertEquals(1, step1.intervalDays)
        assertEquals(WordLearningStatus.LEARNING, step1.learningStatus)

        val step2 = Sm2ReviewScheduler.review(step1, "w1", RecallGrade.GOOD, nowMs = t0 + 86_400_000L)
        assertEquals(2, step2.repetition)
        assertEquals(3, step2.intervalDays)
        assertEquals(WordLearningStatus.REVIEW, step2.learningStatus)

        val step3 = Sm2ReviewScheduler.review(step2, "w1", RecallGrade.EASY, nowMs = t0 + 4 * 86_400_000L)
        assertEquals(3, step3.repetition)
        assertTrue(step3.intervalDays >= 7)
        assertEquals(WordLearningStatus.MASTERED, step3.learningStatus)

        // Lapse resets repetition and schedules 10 minutes later
        val lapsed = Sm2ReviewScheduler.review(step3, "w1", RecallGrade.AGAIN, nowMs = t0 + 12 * 86_400_000L)
        assertEquals(0, lapsed.repetition)
        assertEquals(0, lapsed.intervalDays)
        assertEquals(1, lapsed.lapseCount)
        assertEquals(t0 + 12 * 86_400_000L + 600_000L, lapsed.dueAtMs)

        // Due queue returns only words whose dueAtMs <= nowMs
        val due = Sm2ReviewScheduler.dueWordIds(
            progressMap = mapOf("w1" to lapsed, "w2" to step3),
            nowMs = t0 + 12 * 86_400_000L + 700_000L
        )
        assertEquals(listOf("w1"), due)
    }

    @Test
    fun idempotentRewardLedgerBlocksDuplicatesAndDetectsLevelUp() {
        val seen = mutableSetOf<String>()
        val spec = RewardConfig.specFor(RewardEventType.DAILY_MISSION_CLAIMED)
        val tx = RewardTransaction(
            transactionId = "tx1",
            eventId = "mission_claim_2026-11-24_m1",
            eventType = RewardEventType.DAILY_MISSION_CLAIMED,
            xpDelta = spec.xp,
            coinDelta = spec.coins,
            reason = "일일 미션 완료"
        )

        val first = RewardLedgerEngine.applyTransaction(seen, currentXp = 190, currentCoins = 10, transaction = tx)
        assertTrue(first.granted)
        assertFalse(first.duplicateBlocked)
        assertEquals(190 + spec.xp, first.newTotalXp)
        assertEquals(10 + spec.coins, first.newCoinBalance)
        assertNotNull(first.leveledUpTo)
        assertEquals(2, first.leveledUpTo?.level)
        seen.add(tx.eventId)

        // Exact retry with same eventId must be blocked
        val duplicate = RewardLedgerEngine.applyTransaction(seen, currentXp = first.newTotalXp, currentCoins = first.newCoinBalance, transaction = tx)
        assertFalse(duplicate.granted)
        assertTrue(duplicate.duplicateBlocked)
        assertEquals(first.newTotalXp, duplicate.newTotalXp)
        assertEquals(first.newCoinBalance, duplicate.newCoinBalance)
    }

    @Test
    fun levelConfigMapsAllSevenTiersCleanly() {
        assertEquals("단어 탐험가", LevelConfig.resolveTier(0).titleKo)
        assertEquals(1, LevelConfig.resolveTier(0).level)
        assertEquals("이미지 연상가", LevelConfig.resolveTier(600).titleKo)
        assertEquals(7, LevelConfig.resolveTier(5000).level)
        assertEquals("단어 챔피언", LevelConfig.resolveTier(5000).titleKo)
        assertEquals(1f, LevelConfig.progressToNext(5000), 0.001f)
    }

    @Test
    fun dailyMissionStateMachinePreventsDuplicateClaims() {
        val initial = DailyMissionEngine.defaultMissionsForDate("2026-11-24", dailyWordTarget = 5)
        val newWordMissionId = initial.first { it.kind == MissionKind.DAILY_NEW_WORDS }.missionId

        // Cannot claim before completed
        val (_, prematureClaim) = DailyMissionEngine.claimMission(initial, newWordMissionId)
        assertNull(prematureClaim)

        // Advance to target -> COMPLETED
        val inProg = DailyMissionEngine.recordProgress(initial, MissionKind.DAILY_NEW_WORDS, delta = 3)
        assertEquals(MissionStatus.IN_PROGRESS, inProg.first { it.missionId == newWordMissionId }.status)

        val completed = DailyMissionEngine.recordProgress(inProg, MissionKind.DAILY_NEW_WORDS, delta = 5)
        assertEquals(MissionStatus.COMPLETED, completed.first { it.missionId == newWordMissionId }.status)
        assertEquals(5, completed.first { it.missionId == newWordMissionId }.currentCount)

        // Claim once -> CLAIMED
        val (afterClaim, claimed) = DailyMissionEngine.claimMission(completed, newWordMissionId)
        assertNotNull(claimed)
        assertEquals(MissionStatus.CLAIMED, afterClaim.first { it.missionId == newWordMissionId }.status)

        // Second claim attempt must fail
        val (_, secondClaim) = DailyMissionEngine.claimMission(afterClaim, newWordMissionId)
        assertNull(secondClaim)
    }

    @Test
    fun spellingTilesModeGeneratesValidLetterAssemblyQuestion() {
        val pool = listOf(
            sampleWord("s1", "resilient", "회복력 있는"),
            sampleWord("s2", "coherent", "일관성 있는")
        )
        val q = EnGameMathEngine.generateQuestion(
            mode = EnGameMode.SPELLING_TILES,
            eloRating = 1200,
            vocabPool = pool,
            weakWordIds = emptySet(),
            travelSentences = emptyList(),
            recentIds = emptySet()
        )
        assertEquals(EnGameMode.SPELLING_TILES, q.mode)
        assertEquals(q.targetTokens.size, q.scrambledTokens.size)
        assertEquals(q.targetTokens.sorted(), q.scrambledTokens.sorted())
        assertEquals(q.targetTokens.joinToString(""), q.correctAnswer)
        assertTrue(q.promptPrimary.isNotBlank())
    }

    @Test
    fun queueSnapshotComputesDueOverdueTodayAndWeakAccurately() {
        val now = 1_700_000_000_000L
        val day = 86_400_000L
        val map = mapOf(
            "overdue_1" to WordLearningProgress("overdue_1", WordLearningStatus.REVIEW, dueAtMs = now - 2 * day),
            "due_now_1" to WordLearningProgress("due_now_1", WordLearningStatus.LEARNING, dueAtMs = now - 10_000L),
            "today_1" to WordLearningProgress("today_1", WordLearningStatus.REVIEW, dueAtMs = now + 4 * 3_600_000L),
            "future_1" to WordLearningProgress("future_1", WordLearningStatus.MASTERED, dueAtMs = now + 5 * day)
        )
        val snap = Sm2ReviewScheduler.queueSnapshot(map, weakWordIds = setOf("overdue_1", "weak_extra"), nowMs = now)
        assertEquals(2, snap.dueNowCount)
        assertEquals(1, snap.overdueCount)
        assertEquals(1, snap.todayScheduledCount)
        assertEquals(2, snap.weakCount)
    }

    @Test
    fun weeklyMissionsAndExplorerJourneyProgressCorrectly() {
        val missions = DailyMissionEngine.defaultMissionsForDate("2026-11-24")
        val weekly = missions.filter { it.cadence == MissionCadence.WEEKLY }
        assertTrue("Should include weekly missions", weekly.size >= 2)
        val progressed = DailyMissionEngine.recordProgress(missions, MissionKind.WEEKLY_PALACE_ROOMS, delta = 3)
        val palaceMission = progressed.first { it.kind == MissionKind.WEEKLY_PALACE_ROOMS }
        assertEquals(MissionStatus.COMPLETED, palaceMission.status)

        val gears = ExplorerJourneyEngine.resolveGearItems(
            totalXp = 600,
            masteredCount = 10,
            streakCount = 4,
            palaceCount = 3,
            claimedMissionsCount = 2
        )
        assertEquals(4, gears.size)
        assertTrue("All 4 gear items should unlock at high progress", gears.all { it.unlocked })

        val biomes = ExplorerJourneyEngine.resolveJourneyBiomes(totalXp = 700)
        assertEquals(3, biomes.size)
        assertTrue(biomes[0].cleared)
        assertTrue(biomes[1].isCurrent)
        assertFalse(biomes[2].cleared)
    }

    @Test
    fun mediaAuditStatsSummarizesBundledVisualsAndVerifiedMorphemes() {
        val words = listOf(
            sampleWord("a1", "alleviate", "완화하다"),
            sampleWord("a2", "resilient", "회복력이 있는"),
            sampleWord("a3", "quixotic", "공상적인")
        )
        val audit = VocabMediaResolver.auditStats(words)
        assertEquals(3, audit.totalWords)
        assertEquals(3, audit.bundledVisualCount)
        assertEquals(2, audit.verifiedMorphemeCount)
        assertEquals(1, audit.needsHumanReviewCount)
        assertEquals(100, audit.fallbackReadyPercent)

        // Manual P7 approval clears pending count
        val approvedAudit = VocabMediaResolver.auditStats(
            words = words,
            reviewOverrides = mapOf("a3" to MediaReviewStatus.APPROVED)
        )
        assertEquals(0, approvedAudit.needsHumanReviewCount)
        assertEquals(1, approvedAudit.manuallyApprovedCount)
    }

    @Test
    fun semanticVisualMotifMapsRealWordsAccuratelyAndCoversEntireRepository() {
        val crucial = VocabRepository.allWords.first { it.word.equals("crucial", ignoreCase = true) }
        assertEquals(WordVisualMotif.KEYSTONE_PIVOT, VocabMnemonicEngine.visualMotifFor(crucial))
        val crucialGuide = VocabMnemonicEngine.buildStructuredGuide(crucial)
        assertTrue("crucial should have verified morpheme", crucialGuide.hasVerifiedMorpheme)
        assertEquals(WordVisualMotif.KEYSTONE_PIVOT, crucialGuide.visualMotif)

        val fluctuate = sampleWord("f1", "fluctuate", "변동하다")
        assertEquals(WordVisualMotif.RISING_WAVES, VocabMnemonicEngine.visualMotifFor(fluctuate))

        val ambiguous = sampleWord("m1", "ambiguous", "모호한")
        assertEquals(WordVisualMotif.FORKED_CROSSROADS, VocabMnemonicEngine.visualMotifFor(ambiguous))

        // Every word in VocabRepository resolves to a valid motif and semantic vector asset
        VocabRepository.allWords.forEach { w ->
            val asset = VocabMediaResolver.resolve(w)
            assertNotNull(asset.visualMotif)
            assertTrue(asset.assetId.startsWith("semantic_"))
        }
    }

    @Test
    fun typedRecallEvaluatorAllowsOneTypoForLongWordsAndStaysStrictForShortWords() {
        val exact = TypedRecallEvaluator.evaluate("Crucial", "crucial")
        assertTrue(exact.isPassed)
        assertEquals(TypedRecallOutcome.EXACT_MATCH, exact.outcome)

        val oneTypo = TypedRecallEvaluator.evaluate("crucal", "crucial")
        assertTrue(oneTypo.isPassed)
        assertEquals(TypedRecallOutcome.TYPO_ACCEPTED, oneTypo.outcome)
        assertEquals(1, oneTypo.editDistance)

        val twoTypos = TypedRecallEvaluator.evaluate("crul", "crucial")
        assertFalse(twoTypos.isPassed)
        assertEquals(TypedRecallOutcome.INCORRECT, twoTypos.outcome)

        // Short word (<5 chars) should not pass with 1 typo
        val shortTypo = TypedRecallEvaluator.evaluate("kye", "key")
        assertFalse(shortTypo.isPassed)
        assertEquals(TypedRecallOutcome.INCORRECT, shortTypo.outcome)
    }

    @Test
    fun suspendToggleExcludesWordFromDueQueueAndSpendCoinsPreventsNegativeBalance() {
        val now = 1_700_000_000_000L
        val dueWord = WordLearningProgress("w_due", WordLearningStatus.REVIEW, repetition = 2, dueAtMs = now - 5_000L)
        val suspended = Sm2ReviewScheduler.toggleSuspended(dueWord, "w_due", nowMs = now)
        assertEquals(WordLearningStatus.SUSPENDED, suspended.learningStatus)
        assertTrue(Sm2ReviewScheduler.dueWordIds(mapOf("w_due" to suspended), nowMs = now).isEmpty())

        val restored = Sm2ReviewScheduler.toggleSuspended(suspended, "w_due", nowMs = now)
        assertEquals(WordLearningStatus.REVIEW, restored.learningStatus)
        assertEquals(listOf("w_due"), Sm2ReviewScheduler.dueWordIds(mapOf("w_due" to restored), nowMs = now))

        val (ok1, bal1) = RewardLedgerEngine.spendCoins(currentCoins = 20, cost = 15)
        assertTrue(ok1)
        assertEquals(5, bal1)
        val (ok2, bal2) = RewardLedgerEngine.spendCoins(currentCoins = 5, cost = 10)
        assertFalse(ok2)
        assertEquals(5, bal2)
    }

    @Test
    fun missionRolloverResetsDailyPreservesWeeklyAndExplorationAndAdaptsGoal() {
        val day1 = DailyMissionEngine.defaultMissionsForDate("2026-11-24", dailyWordTarget = 10)
        val progressed = DailyMissionEngine.recordProgress(
            DailyMissionEngine.recordProgress(
                DailyMissionEngine.recordProgress(day1, MissionKind.DAILY_NEW_WORDS, 4),
                MissionKind.WEEKLY_UNIQUE_WORDS,
                12
            ),
            MissionKind.STORY_VERIFIED_ROOTS,
            3
        )
        // Next day in same month with updated dailyGoal = 15
        val day2 = DailyMissionEngine.rolloverMissions(progressed, todayDateKey = "2026-11-25", dailyWordTarget = 15)
        val dailyNew = day2.first { it.kind == MissionKind.DAILY_NEW_WORDS }
        val weeklyUnique = day2.first { it.kind == MissionKind.WEEKLY_UNIQUE_WORDS }
        val storyRoots = day2.first { it.kind == MissionKind.STORY_VERIFIED_ROOTS }

        assertEquals(0, dailyNew.currentCount)
        assertEquals(15, dailyNew.targetCount)
        assertEquals(12, weeklyUnique.currentCount)
        assertEquals(3, storyRoots.currentCount)
        assertTrue(day2.any { it.cadence == MissionCadence.EXPLORATION })
    }

    @Test
    fun achievementBadgesAndAnalyticsSummaryComputeCorrectMetrics() {
        VocabAnalyticsTracker.clearForTesting()
        VocabAnalyticsTracker.log("word_card_viewed", "w1")
        VocabAnalyticsTracker.log("mnemonic_viewed", "w1")
        VocabAnalyticsTracker.log("recall_correct", "w1")
        VocabAnalyticsTracker.log("recall_correct", "w2")
        VocabAnalyticsTracker.log("recall_wrong", "w3")
        VocabAnalyticsTracker.log("typing_correct", "w1")

        val summary = VocabAnalyticsTracker.summary()
        assertEquals(1, summary.cardsViewed)
        assertEquals(1, summary.mnemonicsViewed)
        assertEquals(67, summary.recallAccuracyPercent)
        assertEquals(1, summary.typingPassed)

        val badges = AchievementBadgeEngine.resolveBadges(
            masteredCount = 25,
            streakCount = 7,
            palaceCount = 5,
            claimedMissionsCount = 5,
            totalXp = 1600,
            sm2ReviewCount = 30
        )
        assertEquals(6, badges.size)
        assertTrue(badges.all { it.unlocked })
    }
}
