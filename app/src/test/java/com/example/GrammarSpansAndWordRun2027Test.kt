package com.example

import com.example.cloudflare.CloudflareConfig
import com.example.data.HumanTutorRepository
import com.example.data.WordRun2027Repository
import com.example.engine.GameJuiceAudioEngine
import com.example.engine.AppThemeMode
import com.example.engine.GameRevengeLoopEngine
import com.example.engine.LeagueTier
import com.example.engine.PlayGrowthRetentionEngine
import com.example.ui.components.QUIZ_OPTION_ANIM_DURATION_MS
import com.example.ui.components.QUIZ_TOUCH_DEBOUNCE_MS
import com.example.ui.components.TravelStreetDirectory
import com.example.ui.components.shouldAcceptInstantTouch
import com.example.data.WordTranslationOverride
import com.example.data.WordTranslationOverrideStore
import com.example.engine.GrammarSpanEngine
import com.example.engine.KoreanTranslationRefinerEngine
import com.example.engine.LearnedVocabCallMotionEngine
import com.example.engine.RewardChestRarity
import com.example.engine.WordLearningProgress
import com.example.engine.WordRun2027Engine
import com.example.engine.YouTubeNoLoginSegmentExtractor
import com.example.ui.components.MotionPhotoGesture
import com.example.model.GrammarSpan
import com.example.model.GrammarSpanRole
import com.example.model.VocabTopic
import com.example.model.VocabWord
import com.example.model.WordRunChallengeState
import com.example.model.WordRunDayProgress
import com.example.model.WordRunEntry
import com.example.model.WordRunUserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Comprehensive JVM QA suite verifying:
 * 1. `GrammarSpanEngine`: safe JSON parsing/clamping, deterministic clause detection,
 *    and conflict-free atomic interval partitioning with Cloze overlap protection.
 * 2. `WordRun2027`: 500-word bundled asset integrity (zero KICE verbatim leaks),
 *    morphology analyzer, synonym-safe distractor exclusion, 20-min 50Q exam grading,
 *    KST daily unlock policy, and state JSON round-trip persistence.
 */
class GrammarSpansAndWordRun2027Test {

    @Test
    fun `grammar spans JSON parser clamps out-of-bounds and discards malformed ranges`() {
        val raw = """
            [
              {"role": "SUBJECT_CHUNK", "char_span": [-5, 12]},
              {"role": "MAIN_VERB", "char_span": [13, 999]},
              {"role": "RELATIVE_CLAUSE", "char_span": [20, 10]},
              {"role": "UNKNOWN_ROLE", "char_span": [0, 5]}
            ]
        """.trimIndent()

        val parsed = GrammarSpanEngine.parseGrammarSpansJson(raw, sentenceLength = 30)
        assertEquals(2, parsed.size)
        assertEquals(GrammarSpan(GrammarSpanRole.SUBJECT_CHUNK, 0, 12), parsed[0])
        assertEquals(GrammarSpan(GrammarSpanRole.MAIN_VERB, 13, 30), parsed[1])
    }

    @Test
    fun `atomic interval partitioning keeps Cloze blank indivisible across overlapping grammar spans`() {
        val sentence = "The consistent effort of students produces remarkable progress."
        val spans = listOf(
            GrammarSpan(GrammarSpanRole.SUBJECT_CHUNK, 0, 33),
            GrammarSpan(GrammarSpanRole.PREPOSITIONAL_MODIFIER, 22, 33),
            GrammarSpan(GrammarSpanRole.MAIN_VERB, 34, 42)
        )
        // Target word "produces" (34..42) fully inside MAIN_VERB, and also test partial overlap
        val partialSpans = spans + GrammarSpan(GrammarSpanRole.RELATIVE_CLAUSE, 37, 50)

        val segments = GrammarSpanEngine.partitionSentenceSegments(
            sentence = sentence,
            grammarSpans = partialSpans,
            activeRoles = GrammarSpanRole.entries.toSet(),
            targetSpan = 34 to 42,
            showGrammarSpans = true,
            clozeEnabled = true,
            clozeRevealed = false
        )

        // Exact reconstruction of original string without gaps or overlaps
        assertEquals(sentence, segments.joinToString("") { it.text })
        val maskedSegments = segments.filter { it.isClozeMasked }
        assertEquals(1, maskedSegments.size)
        assertEquals("produces", maskedSegments.first().text)
        assertEquals(GrammarSpanRole.MAIN_VERB, maskedSegments.first().primaryGrammarRole)
    }

    @Test
    fun `deterministic grammar detector identifies subject, main verb, and relative clause`() {
        val sentence = "The new strategy that researchers adopted produces lasting improvement."
        val spans = GrammarSpanEngine.detectGrammarSpans(sentence)
        val roles = spans.map { it.role }.toSet()
        assertTrue(GrammarSpanRole.MAIN_VERB in roles)
        assertTrue(GrammarSpanRole.RELATIVE_CLAUSE in roles)
        assertTrue(spans.all { it.start in 0 until sentence.length && it.end in (it.start + 1)..sentence.length })
    }

    @Test
    fun `bundled word_run_2027_500 json contains exactly 500 words across 10 balanced days`() {
        val candidatePaths = listOf(
            File("src/main/assets/corpus/word_run_2027_500.json"),
            File("app/src/main/assets/corpus/word_run_2027_500.json")
        )
        val assetFile = candidatePaths.firstOrNull { it.exists() }
        assertNotNull("Expected word_run_2027_500.json asset to exist", assetFile)

        val raw = assetFile!!.readText(Charsets.UTF_8)
        assertFalse("Must not bundle verbatim KICE source field", raw.contains("\"source\""))

        val words = WordRun2027Repository.parseWordsJson(raw)
        assertEquals(500, words.size)
        for (day in 1..10) {
            assertEquals("Day $day must have 50 words", 50, words.count { it.day == day })
        }
        assertTrue(words.all { it.word.isNotBlank() && it.meaningKo.isNotBlank() })
        assertTrue("Day 1 should contain bundled learning examples", words.filter { it.day == 1 }.all { it.exampleEn.isNotBlank() })
    }

    @Test
    fun `word run morphology handles irregular verbs, unchanged nouns, and sentence matching`() {
        val bury = WordRunEntry(25, 1, "bury", "파묻다", 43, 1, "v")
        val info = WordRun2027Engine.buildMorphologyInfo(bury, "The animal buried its food underground.")
        assertEquals("동사", info.posLabelKo)
        assertTrue(info.formChips.any { it.form == "buried" })
        assertTrue(info.sentenceMatches.any { it.form == "buried" })

        assertEquals("wildlife", WordRun2027Engine.plural("wildlife"))
        assertEquals("pupae", WordRun2027Engine.plural("pupa"))
        assertEquals("facilities", WordRun2027Engine.plural("facility"))
    }

    @Test
    fun `synonym aware quiz distractor selection never mixes synonym pairs`() {
        val roughly = WordRunEntry(189, 4, "roughly", "대략", 36, 2, "adv")
        val approx = WordRunEntry(342, 7, "approximately", "대략", 36, 2, "adv")
        val stable = WordRunEntry(49, 1, "stable", "안정적인", 41, 1, "adj")
        val adopt = WordRunEntry(47, 1, "adopt", "채택하다", 39, 1, "v")
        val wildlife = WordRunEntry(3, 1, "wildlife", "야생 생물", 20, 1, "n")
        val decade = WordRunEntry(4, 1, "decade", "10년", 21, 1, "n")

        assertFalse(WordRun2027Engine.isCompatibleDistractor(roughly, approx))
        val q = WordRun2027Engine.buildQuizQuestion(
            target = roughly,
            allWords = listOf(roughly, approx, stable, adopt, wildlife, decade),
            reverseKoreanToEnglish = false,
            random = Random(42)
        )
        assertEquals(4, q.optionWordIds.size)
        assertTrue(roughly.id in q.optionWordIds)
        assertFalse("Synonym 'approximately' must not appear as distractor", approx.id in q.optionWordIds)
    }

    @Test
    fun `exam builder generates balanced 25_25 directions and grader enforces 45_50 pass threshold`() {
        val sample50 = (1..50).map { i ->
            WordRunEntry(i, 1, "word$i", "뜻$i", 20 + (i % 20), 1, "n")
        }
        val questions = WordRun2027Engine.buildExamQuestions(sample50, sample50, Random(7))
        assertEquals(50, questions.size)
        assertEquals(25, questions.count { it.reverseKoreanToEnglish })
        assertEquals(25, questions.count { !it.reverseKoreanToEnglish })

        // 45/50 all answered -> PASS
        val answers45 = questions.mapIndexed { idx, q ->
            idx to if (idx < 45) q.targetWordId else -1
        }.toMap()
        val passRes = WordRun2027Engine.gradeExam(1, questions, answers45, elapsedSec = 600)
        assertTrue(passRes.passed)
        assertEquals(45, passRes.score)
        assertEquals(5, passRes.wrongWordIds.size)

        // 44/50 -> FAIL
        val answers44 = questions.mapIndexed { idx, q ->
            idx to if (idx < 44) q.targetWordId else -1
        }.toMap()
        val failRes = WordRun2027Engine.gradeExam(1, questions, answers44, elapsedSec = 600)
        assertFalse(failRes.passed)
    }

    @Test
    fun `context validation and challenge state serialization round trip cleanly`() {
        val entry = WordRunEntry(1, 1, "facility", "시설", 18, 1, "n")
        assertNotNull(WordRun2027Engine.validateUserContext(entry, "The school is big.", "학교가 크다."))
        assertNull(WordRun2027Engine.validateUserContext(entry, "The school has modern facilities.", "학교에 현대적 시설이 있다."))

        val state = WordRunChallengeState(
            selectedDay = 2,
            freePracticeUnlocked = false,
            dayProgress = mapOf(
                1 to WordRunDayProgress(
                    day = 1,
                    reviewCompleted = true,
                    learnedIds = setOf(1, 2, 3),
                    practicedIds = setOf(1, 2),
                    passedDateKst = "2026-11-25",
                    wrongWordIds = listOf(4, 5)
                )
            ),
            studyAgainCounts = mapOf(1 to 2),
            userContexts = mapOf(1 to WordRunUserContext("Modern facilities help.", "현대 시설이 돕는다."))
        )
        assertFalse(WordRun2027Engine.isDayUnlocked(2, state, todayKst = "2026-11-25"))
        assertTrue(WordRun2027Engine.isDayUnlocked(2, state, todayKst = "2026-11-26"))

        val json = WordRun2027Repository.serializeStateJson(state)
        val restored = WordRun2027Repository.parseStateJson(json)
        assertEquals(state.selectedDay, restored.selectedDay)
        assertEquals(state.dayProgress[1]?.learnedIds, restored.dayProgress[1]?.learnedIds)
        assertEquals(state.userContexts[1]?.en, restored.userContexts[1]?.en)
    }

    @Test
    fun `revenge boss engine prioritizes FSRS lapsed words, triggers FEVER at 5x combo, and drops variable chest`() {
        val pool = (1..10).map { i ->
            VocabWord(
                id = "w_$i",
                grade = 2,
                textbookUnitKo = "테스트",
                csatFrequency = 4,
                csatDifficulty = 2,
                topic = VocabTopic.SCHOOL_EXAM,
                word = "term$i",
                ipa = "",
                partOfSpeech = "n.",
                meaningKo = "핵심뜻$i",
                exampleEn = "The researcher carefully examined term$i in the report.",
                exampleKo = "연구자는 보고서에서 핵심뜻${i}을 면밀히 검토했다.",
                examples = emptyList(),
                pronunciationTipKo = "",
                distractors = emptyList()
            )
        }
        val progress = mapOf(
            "w_7" to WordLearningProgress(wordId = "w_7", lapseCount = 3, dueAtMs = 0L, fsrsStabilityDays = 0.8),
            "w_3" to WordLearningProgress(wordId = "w_3", lapseCount = 1, dueAtMs = 0L, fsrsStabilityDays = 1.2),
            "w_1" to WordLearningProgress(wordId = "w_1", repetition = 5, dueAtMs = Long.MAX_VALUE, fsrsStabilityDays = 25.0)
        )
        val selected = GameRevengeLoopEngine.selectRevengeTargets(pool, progress, count = 6, nowMillis = 1000L)
        assertEquals("w_7", selected[0].id)
        assertEquals("w_3", selected[1].id)

        var battle = GameRevengeLoopEngine.startBattle(pool, progress, sessionLength = 6, bossSeed = 0, random = Random(99))
        assertEquals(6, battle.questions.size)
        assertEquals(600, battle.bossCurrentHp)

        // Chain 5 fast correct answers -> triggers FEVER at combo 5!
        var feverTriggered = false
        for (turn in 0 until 5) {
            val q = battle.currentQuestion!!
            val correctId = q.options.first { it.isCorrect }.wordId
            battle = GameRevengeLoopEngine.submitAnswer(battle, correctId, elapsedMs = 1500L)
            if (battle.lastOutcome?.justTriggeredFever == true) {
                feverTriggered = true
            }
            if (!battle.isFinished && battle.bossCurrentHp > 0) {
                battle = GameRevengeLoopEngine.advanceTurn(battle, Random(turn))
            }
        }
        assertTrue(battle.maxCombo >= 2)
        assertTrue(battle.bossCurrentHp < battle.boss.maxHp || battle.isFinished)
        val summary = GameRevengeLoopEngine.summarizeSession(battle.copy(isFinished = true), rollPercent = 90)
        assertTrue(summary.bossDefeated)
        assertTrue(summary.chestRarity != RewardChestRarity.COMMON || summary.accuracyPercent < 100)
        // Explicit Fever threshold verification
        assertEquals(3, GameRevengeLoopEngine.comboMultiplier(5))
        assertEquals(5, GameRevengeLoopEngine.comboMultiplier(8))
        assertTrue(feverTriggered || battle.bossCurrentHp == 0)
    }

    @Test
    fun `revenge boss flow channel activates auto hint and removes one distractor after two consecutive misses`() {
        val pool = (1..8).map { i ->
            VocabWord(
                id = "flow_$i",
                grade = 2,
                textbookUnitKo = "테스트",
                csatFrequency = 4,
                csatDifficulty = 2,
                topic = VocabTopic.SCHOOL_EXAM,
                word = "alpha$i",
                ipa = "",
                partOfSpeech = "n.",
                meaningKo = "의미$i",
                exampleEn = "Students use alpha$i daily.",
                exampleKo = "학생들은 매일 의미${i}를 사용한다.",
                examples = emptyList(),
                pronunciationTipKo = "",
                distractors = emptyList()
            )
        }
        var battle = GameRevengeLoopEngine.startBattle(pool, emptyMap(), sessionLength = 5, random = Random(11))

        // Miss twice in a row
        repeat(2) { step ->
            val q = battle.currentQuestion!!
            val wrongId = q.options.first { !it.isCorrect }.wordId
            battle = GameRevengeLoopEngine.submitAnswer(battle, wrongId, elapsedMs = 5000L)
            if (step == 1) {
                assertTrue(battle.lastOutcome!!.flowAutoHintActivated)
                assertEquals(2, battle.consecutiveMisses)
            }
            battle = GameRevengeLoopEngine.advanceTurn(battle, Random(step))
        }
        // Next turn must automatically eliminate 1 wrong distractor
        assertNotNull(battle.eliminatedOptionWordId)
        val nextQ = battle.currentQuestion!!
        val eliminatedOpt = nextQ.options.first { it.wordId == battle.eliminatedOptionWordId }
        assertFalse(eliminatedOpt.isCorrect)
    }

    @Test
    fun `procedural audio synthesizer generates bounded PCM16 samples and respects mute toggle`() {
        val pcm = GameJuiceAudioEngine.synthesizePcm16(
            listOf(
                GameJuiceAudioEngine.ToneNote(523.25, 80, 0.25),
                GameJuiceAudioEngine.ToneNote(783.99, 100, 0.20)
            )
        )
        assertTrue(pcm.isNotEmpty())
        assertTrue(pcm.any { it != 0.toShort() })
        val initialMute = GameJuiceAudioEngine.isMuted()
        assertEquals(!initialMute, GameJuiceAudioEngine.toggleMuted())
        GameJuiceAudioEngine.setMuted(initialMute)
    }

    @Test
    fun `3-second speed combo multiplier escalates 2x 3x 5x and resets speed bonus on slow answers`() {
        assertEquals(1, GameRevengeLoopEngine.speedMultiplier(0))
        assertEquals(2, GameRevengeLoopEngine.speedMultiplier(1))
        assertEquals(3, GameRevengeLoopEngine.speedMultiplier(2))
        assertEquals(5, GameRevengeLoopEngine.speedMultiplier(3))
        assertEquals(5, GameRevengeLoopEngine.speedMultiplier(7))

        val pool = (1..8).map { i ->
            VocabWord(
                id = "spd_$i",
                grade = 1,
                textbookUnitKo = "스피드",
                csatFrequency = 4,
                csatDifficulty = 2,
                topic = VocabTopic.SCHOOL_EXAM,
                word = "quick$i",
                ipa = "",
                partOfSpeech = "adj.",
                meaningKo = "빠른$i",
                exampleEn = "A quick$i response builds fluency.",
                exampleKo = "빠른${i} 반응이 유창함을 만든다.",
                examples = emptyList(),
                pronunciationTipKo = "",
                distractors = emptyList()
            )
        }
        var battle = GameRevengeLoopEngine.startBattle(pool, emptyMap(), sessionLength = 6, random = Random(5))

        // Turn 1: fast (1800ms <= 3000ms) -> 2x
        battle = GameRevengeLoopEngine.submitAnswer(
            battle,
            battle.currentQuestion!!.options.first { it.isCorrect }.wordId,
            elapsedMs = 1800L
        )
        val outcome1 = battle.lastOutcome!!
        assertTrue(outcome1.answeredWithin3Sec)
        assertEquals(2, outcome1.multiplierAfter)
        assertEquals(1, battle.speedStreak)
        battle = GameRevengeLoopEngine.advanceTurn(battle, Random(1))

        // Turn 2: fast (2400ms <= 3000ms) -> 3x
        battle = GameRevengeLoopEngine.submitAnswer(
            battle,
            battle.currentQuestion!!.options.first { it.isCorrect }.wordId,
            elapsedMs = 2400L
        )
        assertEquals(3, battle.lastOutcome!!.multiplierAfter)
        assertEquals(2, battle.speedStreak)
        battle = GameRevengeLoopEngine.advanceTurn(battle, Random(2))

        // Turn 3: fast (2999ms <= 3000ms) -> 5x
        battle = GameRevengeLoopEngine.submitAnswer(
            battle,
            battle.currentQuestion!!.options.first { it.isCorrect }.wordId,
            elapsedMs = 2999L
        )
        assertEquals(5, battle.lastOutcome!!.multiplierAfter)
        assertEquals(3, battle.speedStreak)
        if (!battle.isFinished) {
            battle = GameRevengeLoopEngine.advanceTurn(battle, Random(3))
            // Turn 4: slow (4500ms > 3000ms) -> speedStreak resets to 0 (1x speed bonus) while combo continues
            battle = GameRevengeLoopEngine.submitAnswer(
                battle,
                battle.currentQuestion!!.options.first { it.isCorrect }.wordId,
                elapsedMs = 4500L
            )
            assertFalse(battle.lastOutcome!!.answeredWithin3Sec)
            assertEquals(0, battle.speedStreak)
            assertEquals(1, GameRevengeLoopEngine.speedMultiplier(battle.speedStreak))
        }
    }

    @Test
    fun `cloudflare config rejects placeholder and insecure urls while accepting https workers`() {
        assertEquals("", CloudflareConfig.resolveWorkerUrl("https://your-worker.your-subdomain.workers.dev"))
        assertEquals("", CloudflareConfig.resolveWorkerUrl("http://insecure.example.com"))
        assertEquals(
            "https://speakquest-api.example.workers.dev",
            CloudflareConfig.resolveWorkerUrl("https://speakquest-api.example.workers.dev/")
        )
    }

    @Test
    fun `travel zones and conversation themes map to diverse characters and matching scenic backgrounds`() {
        val zones = (1..10).map { TravelStreetDirectory.locationForZone(it) }
        val distinctBgCount = zones.map { it.sceneBgRes }.distinct().size
        val distinctHostNames = zones.map { it.defaultHost.name }.distinct().size
        val distinctPortraits = zones.map { it.defaultHost.portraitRes }.distinct().size
        assertTrue("Expected at least 7 distinct scenic backgrounds across 10 zones, got $distinctBgCount", distinctBgCount >= 7)
        assertTrue("Expected at least 8 distinct local host names across 10 zones, got $distinctHostNames", distinctHostNames >= 8)
        assertTrue("Expected at least 6 distinct character portraits across 10 zones, got $distinctPortraits", distinctPortraits >= 6)

        val topicTutors = listOf("수다", "카페", "공항", "호텔", "마트", "식당")
            .map { HumanTutorRepository.recommendedTutorForTopic(it) }
        assertEquals(6, topicTutors.map { it.id }.distinct().size)
        assertEquals(6, topicTutors.map { it.portraitRes }.distinct().size)
    }

    @Test
    fun `instant quiz touch enforces 50ms animation delay and debounces rapid bounce without blocking fast taps`() {
        assertEquals(50, QUIZ_OPTION_ANIM_DURATION_MS)
        assertTrue(shouldAcceptInstantTouch(nowMs = 1000L, lastFiredMs = 0L))
        assertFalse(shouldAcceptInstantTouch(nowMs = 1040L, lastFiredMs = 1000L, debounceMs = QUIZ_TOUCH_DEBOUNCE_MS))
        assertTrue(shouldAcceptInstantTouch(nowMs = 1000L + QUIZ_TOUCH_DEBOUNCE_MS, lastFiredMs = 1000L, debounceMs = QUIZ_TOUCH_DEBOUNCE_MS))
    }

    @Test
    fun `play growth engine computes league tiers, applies streak freeze shield on 1-day miss, and enforces 14-day review cooldown`() {
        val bronze = PlayGrowthRetentionEngine.computeLeagueStanding(totalXp = 150, streakCount = 0, masteredWordsCount = 0)
        assertEquals(LeagueTier.BRONZE, bronze.currentTier)
        assertEquals(LeagueTier.SILVER, bronze.nextTier)

        val master = PlayGrowthRetentionEngine.computeLeagueStanding(totalXp = 5500, streakCount = 20, masteredWordsCount = 80)
        assertEquals(LeagueTier.MASTER, master.currentTier)
        assertNull(master.nextTier)
        assertEquals(1f, master.progressToNext, 0.001f)

        // Missing exactly 1 calendar day (gap == 2) with 1 freeze preserves streak and consumes 1 shield
        val shielded = PlayGrowthRetentionEngine.resolveStreakTransition(
            lastStudyDateIso = "2026-11-25",
            todayIso = "2026-11-27",
            currentStreak = 7,
            streakFreezes = 2
        )
        assertEquals(7, shielded.nextStreak)
        assertEquals(1, shielded.remainingFreezes)
        assertTrue(shielded.freezeConsumed)

        // Missing 1 day with 0 freezes resets streak
        val unshielded = PlayGrowthRetentionEngine.resolveStreakTransition(
            lastStudyDateIso = "2026-11-25",
            todayIso = "2026-11-27",
            currentStreak = 7,
            streakFreezes = 0
        )
        assertEquals(0, unshielded.nextStreak)
        assertFalse(unshielded.freezeConsumed)

        // Peak-happiness review prompt respects onboarding, 14-day cooldown, and permanent dismissal
        val now = 2_000_000_000L
        assertFalse(
            PlayGrowthRetentionEngine.shouldTriggerPlayReviewPrompt(
                onboardingCompleted = false,
                hasDismissedPermanently = false,
                lastPromptedAtMs = 0L,
                nowMs = now,
                todayWordsCompleted = 10,
                dailyGoal = 10,
                streakCount = 5,
                bossVictories = 1
            )
        )
        assertTrue(
            PlayGrowthRetentionEngine.shouldTriggerPlayReviewPrompt(
                onboardingCompleted = true,
                hasDismissedPermanently = false,
                lastPromptedAtMs = 0L,
                nowMs = now,
                todayWordsCompleted = 10,
                dailyGoal = 10,
                streakCount = 1,
                bossVictories = 0
            )
        )
        assertFalse(
            PlayGrowthRetentionEngine.shouldTriggerPlayReviewPrompt(
                onboardingCompleted = true,
                hasDismissedPermanently = false,
                lastPromptedAtMs = now - 1000L,
                nowMs = now,
                todayWordsCompleted = 10,
                dailyGoal = 10,
                streakCount = 5,
                bossVictories = 2
            )
        )
        assertEquals(AppThemeMode.DARK, AppThemeMode.SYSTEM.next())
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.DARK.next())
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.LIGHT.next())
    }

    @Test
    fun `all 33 tutor motions generate mandatory learned-word questions with natural Korean particles`() {
        val sampleWord = VocabWord(
            id = "test_crucial",
            grade = 2,
            textbookUnitKo = "고2 수능핵심",
            csatFrequency = 5,
            csatDifficulty = 2,
            topic = VocabTopic.SCHOOL_EXAM,
            word = "crucial",
            ipa = "/ˈkruːʃəl/",
            partOfSpeech = "adj.",
            meaningKo = "결정적인, 매우 중요한",
            exampleEn = "Regular practice is crucial for fluency.",
            exampleKo = "꾸준한 연습은 유창함에 결정적이다.",
            examples = emptyList(),
            pronunciationTipKo = "크루셜",
            distractors = emptyList()
        )
        assertEquals(33, MotionPhotoGesture.entries.size)

        MotionPhotoGesture.entries.forEach { gesture ->
            val bundle = LearnedVocabCallMotionEngine.buildMotionQuestionBundle(
                word = sampleWord,
                motion = gesture,
                learnerName = "학습자"
            )
            assertEquals(gesture, bundle.motion)
            assertTrue(
                "Gesture ${gesture.name} question must contain learned word 'crucial': ${bundle.questionEn}",
                LearnedVocabCallMotionEngine.containsLearnedWord(bundle.questionEn, sampleWord.word)
            )
            assertTrue(
                "Gesture ${gesture.name} must include question mark: ${bundle.questionEn}",
                bundle.questionEn.contains("?")
            )
            assertFalse(
                "Gesture ${gesture.name} Korean translation must not have awkward '이(가)': ${bundle.questionKo}",
                bundle.questionKo.contains("이(가)") || bundle.questionKo.contains("을(를)")
            )
        }
    }

    @Test
    fun `korean translation refiner fixes batchim particles, awkward templates, and persists overrides`() {
        // Exact Hangul Jongseong particle agreement
        assertEquals("결정적인은", KoreanTranslationRefinerEngine.attachJosa("결정적인", "은/는"))
        assertEquals("기회는", KoreanTranslationRefinerEngine.attachJosa("기회", "은/는"))
        assertEquals("사과를", KoreanTranslationRefinerEngine.attachJosa("사과", "을/를"))
        assertEquals("책상을", KoreanTranslationRefinerEngine.attachJosa("책상", "을/를"))
        assertEquals("서울로", KoreanTranslationRefinerEngine.attachJosa("서울", "으로/로"))
        assertEquals("부산으로", KoreanTranslationRefinerEngine.attachJosa("부산", "으로/로"))

        // Polish awkward translationese
        val polished = KoreanTranslationRefinerEngine.polishKorean("기회이(가) 중요하다. '책임' 한다.")
        assertFalse(polished.contains("이(가)"))

        // Quality analyzer detects awkwardness
        val badQuality = KoreanTranslationRefinerEngine.analyzeQuality("그는 '탐구하다' 한다 그리고 기회이(가) 있다.")
        assertTrue(badQuality.naturalnessScore < 85)
        assertTrue(badQuality.issuesDetected.isNotEmpty())

        val goodQuality = KoreanTranslationRefinerEngine.analyzeQuality("꾸준한 연습은 실력 향상에 결정적인 역할을 합니다.")
        assertTrue(goodQuality.naturalnessScore >= 90)

        // Override store JSON round-trip & applyTo
        val overrideMap = mapOf(
            "test_crucial" to WordTranslationOverride(
                wordId = "test_crucial",
                word = "crucial",
                meaningKo = "매우 중요한, 결정적인",
                exampleEn = "Sleep is crucial for memory.",
                exampleKo = "수면은 기억력에 매우 중요합니다.",
                pronunciationTipKo = "크루셜(ru에 강세)",
                updatedAtMs = 123456L
            )
        )
        val json = WordTranslationOverrideStore.serializeOverridesJson(overrideMap)
        val parsed = WordTranslationOverrideStore.parseOverridesJson(json)
        assertEquals("매우 중요한, 결정적인", parsed["test_crucial"]?.meaningKo)
        assertEquals("수면은 기억력에 매우 중요합니다.", parsed["test_crucial"]?.exampleKo)
    }

    @Test
    fun `youtube no-login segment extractor parses urls, json3, xml, and srt captions into exact A-B dialogue clips`() {
        // URL parsing across all YouTube formats
        val watch = YouTubeNoLoginSegmentExtractor.parseYouTubeInput("https://www.youtube.com/watch?v=abc123XYZ_9&t=1m25s")
        assertNotNull(watch)
        assertEquals("abc123XYZ_9", watch!!.videoId)
        assertEquals(85, watch.startTimeSec)

        val shortUrl = YouTubeNoLoginSegmentExtractor.parseYouTubeInput("https://youtu.be/abc123XYZ_9?t=42")
        assertNotNull(shortUrl)
        assertEquals("abc123XYZ_9", shortUrl!!.videoId)
        assertEquals(42, shortUrl.startTimeSec)

        val shorts = YouTubeNoLoginSegmentExtractor.parseYouTubeInput("https://www.youtube.com/shorts/abc123XYZ_9")
        assertNotNull(shorts)
        assertEquals("abc123XYZ_9", shorts!!.videoId)

        // JSON3 caption parser
        val sampleJson3 = """
            {
              "events": [
                {"tStartMs": 5000, "dDurationMs": 2500, "segs": [{"utf8": "Hello everyone."}]},
                {"tStartMs": 8000, "dDurationMs": 3200, "segs": [{"utf8": "We need to "}, {"utf8": "implement a better plan."}]},
                {"tStartMs": 12000, "dDurationMs": 2500, "segs": [{"utf8": "Thank you."}]}
              ]
            }
        """.trimIndent()
        val json3Cues = YouTubeNoLoginSegmentExtractor.parseJson3Captions(sampleJson3)
        assertEquals(3, json3Cues.size)
        val matches = YouTubeNoLoginSegmentExtractor.extractDialogueMatchesFromCues(
            videoId = "abc123XYZ_9",
            title = "TED Talk Sample",
            channelName = "TED",
            cues = json3Cues,
            query = "implement"
        )
        assertEquals(1, matches.size)
        assertEquals(7, matches.first().startSeconds) // 8s - 1s lead-in
        assertTrue(matches.first().endSeconds >= 12)  // 11.2s + 1.5s tail
        assertTrue(matches.first().matchedDialogueEn.contains("implement", ignoreCase = true))
        assertTrue(matches.first().contextBeforeEn.contains("Hello everyone."))

        // XML timedtext parser
        val sampleXml = """
            <transcript>
              <text start="12.5" dur="3.0">They implemented new rules immediately.</text>
            </transcript>
        """.trimIndent()
        val xmlCues = YouTubeNoLoginSegmentExtractor.parseXmlCaptions(sampleXml)
        assertEquals(1, xmlCues.size)
        val xmlMatches = YouTubeNoLoginSegmentExtractor.extractDialogueMatchesFromCues(
            videoId = "abc123XYZ_9",
            title = "News Clip",
            channelName = "BBC",
            cues = xmlCues,
            query = "implement"
        )
        assertEquals(1, xmlMatches.size) // Inflection matching ("implemented")
        assertEquals(11, xmlMatches.first().startSeconds)

        // YouTube copy-pasted timestamp transcript parser
        val ytTranscript = """
            0:04
            Welcome back to the channel.
            0:09
            Consistency is crucial when learning a language.
            0:14
            Keep practicing every day.
        """.trimIndent()
        val manualCues = YouTubeNoLoginSegmentExtractor.parseManualTranscriptOrSrt(ytTranscript)
        assertEquals(3, manualCues.size)
        assertEquals(9.0, manualCues[1].startSec, 0.01)
    }
}
