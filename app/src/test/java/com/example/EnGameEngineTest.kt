package com.example

import com.example.engine.EnGameMathEngine
import com.example.engine.EnGameMode
import com.example.engine.EnGamePhase
import com.example.engine.EnGameRank
import com.example.engine.EnGameUiState
import com.example.model.TravelSentence
import com.example.model.VocabTopic
import com.example.model.VocabWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnGameEngineTest {

    private val samplePool = listOf(
        VocabWord(
            id = "w1",
            grade = 2,
            textbookUnitKo = "고2 수능",
            csatFrequency = 5,
            csatDifficulty = 2,
            topic = VocabTopic.EMOTION_MIND,
            word = "resilient",
            ipa = "/rɪˈzɪliənt/",
            partOfSpeech = "형용사",
            meaningKo = "회복력이 있는",
            exampleEn = "Stay resilient under heavy pressure.",
            exampleKo = "강한 압박 속에서도 회복력을 유지하세요.",
            examples = emptyList(),
            pronunciationTipKo = "리질리언트",
            distractors = listOf("취약한", "일시적인", "무관심한"),
            synonymGroupId = "SYN-0101",
            synonyms = listOf("adaptable", "tough"),
            antonyms = listOf("fragile")
        ),
        VocabWord(
            id = "w2",
            grade = 3,
            textbookUnitKo = "고3 수능",
            csatFrequency = 5,
            csatDifficulty = 3,
            topic = VocabTopic.LOGIC_DEBATE,
            word = "inevitable",
            ipa = "/ɪnˈevɪtəbl/",
            partOfSpeech = "형용사",
            meaningKo = "피할 수 없는",
            exampleEn = "Change is inevitable in modern technology.",
            exampleKo = "현대 기술에서 변화는 피할 수 없다.",
            examples = emptyList(),
            pronunciationTipKo = "이네비터블",
            distractors = listOf("우연한", "일시적인", "모호한"),
            synonymGroupId = "SYN-0102",
            synonyms = listOf("unavoidable", "certain"),
            antonyms = listOf("avoidable")
        ),
        VocabWord(
            id = "w3",
            grade = 1,
            textbookUnitKo = "고1 공통",
            csatFrequency = 4,
            csatDifficulty = 1,
            topic = VocabTopic.SCHOOL_EXAM,
            word = "coherent",
            ipa = "/koʊˈhɪrənt/",
            partOfSpeech = "형용사",
            meaningKo = "일관성 있는",
            exampleEn = "She gave a coherent explanation today.",
            exampleKo = "그녀는 오늘 일관된 설명을 했다.",
            examples = emptyList(),
            pronunciationTipKo = "코히어런트",
            distractors = listOf("혼란스러운", "충동적인", "지연된"),
            synonymGroupId = "SYN-0103",
            synonyms = listOf("logical", "consistent"),
            antonyms = listOf("inconsistent")
        ),
        VocabWord(
            id = "w4",
            grade = 2,
            textbookUnitKo = "고2 수능",
            csatFrequency = 5,
            csatDifficulty = 2,
            topic = VocabTopic.SOCIETY_ECONOMY,
            word = "substantial",
            ipa = "/səbˈstænʃl/",
            partOfSpeech = "형용사",
            meaningKo = "상당한",
            exampleEn = "We made substantial progress this week.",
            exampleKo = "우리는 이번 주에 상당한 진전을 이뤘다.",
            examples = emptyList(),
            pronunciationTipKo = "섭스탠셜",
            distractors = listOf("사소한", "추상적인", "불안정한"),
            synonymGroupId = "SYN-0104",
            synonyms = listOf("significant", "considerable"),
            antonyms = listOf("minor")
        )
    )

    private val sampleSentences = listOf(
        TravelSentence(
            index = 1,
            phaseKo = "주문",
            english = "Could I have an iced latte please?",
            korean = "아이스 라떼 한 잔 주시겠어요?",
            ipaHint = "/kʊd aɪ hæv æn aɪst ˈlɑːteɪ pliːz/",
            speakingTipKo = "정중한 요청 표현"
        ),
        TravelSentence(
            index = 2,
            phaseKo = "길찾기",
            english = "Where is the nearest subway station?",
            korean = "가장 가까운 지하철역이 어디인가요?",
            ipaHint = "/wer ɪz ðə ˈnɪrɪst ˈsʌbweɪ ˈsteɪʃn/",
            speakingTipKo = "길 묻기 핵심"
        ),
        TravelSentence(
            index = 3,
            phaseKo = "호텔",
            english = "I would like to check in now.",
            korean = "지금 체크인하고 싶습니다.",
            ipaHint = "/aɪ wʊd laɪk tuː tʃek ɪn naʊ/",
            speakingTipKo = "호텔 체크인"
        ),
        TravelSentence(
            index = 4,
            phaseKo = "결제",
            english = "Can I pay by credit card here?",
            korean = "여기서 신용카드로 결제할 수 있나요?",
            ipaHint = "/kæn aɪ peɪ baɪ ˈkredɪt kɑːrd hɪr/",
            speakingTipKo = "결제 표현"
        )
    )

    @Test
    fun eloRating_risesOnCorrect_fallsOnWrong_andClampsWithinBounds() {
        val up = EnGameMathEngine.updateElo(1200, 1250, isCorrect = true)
        val down = EnGameMathEngine.updateElo(1200, 1250, isCorrect = false)
        assertTrue("ELO should increase on correct answer", up > 1200)
        assertTrue("ELO should decrease on wrong answer", down < 1200)
        assertEquals(800, EnGameMathEngine.updateElo(800, 1600, isCorrect = false))
        assertEquals(2400, EnGameMathEngine.updateElo(2400, 1200, isCorrect = true))
    }

    @Test
    fun evaluateAnswer_appliesSpeedAndComboAndTriggersFeverAt100() {
        val baseState = EnGameUiState(
            phase = EnGamePhase.PLAYING,
            mode = EnGameMode.SPEED_BLITZ,
            eloRating = 1200,
            combo = 3,
            feverGauge = 80,
            feverTurnsLeft = 0
        )
        val fastEval = EnGameMathEngine.evaluateAnswer(
            state = baseState,
            isCorrect = true,
            elapsedMs = 1800L,
            correctLabel = "회복력이 있는"
        )
        assertTrue(fastEval.isCorrect)
        assertEquals(4, fastEval.nextCombo)
        assertTrue("Fever should trigger when gauge crosses 100", fastEval.feverJustActivated)
        assertTrue(fastEval.isFeverActive)
        // base 100 * comboMult(1 + 4*0.12 = 1.48) * speedMult(1.6) * feverMult(2.0) = 474
        assertEquals(474, fastEval.scoreDelta)

        val missEval = EnGameMathEngine.evaluateAnswer(
            state = baseState,
            isCorrect = false,
            elapsedMs = 4000L,
            correctLabel = "회복력이 있는"
        )
        assertFalse(missEval.isCorrect)
        assertEquals(0, missEval.nextCombo)
        assertEquals(0, missEval.scoreDelta)
        assertEquals(60, missEval.nextFeverGauge)
    }

    @Test
    fun sentenceBuilder_tokenizesAndScramblesWithoutIdentityOrder() {
        val tokens = EnGameMathEngine.tokenizeSentence("Could I have an iced latte please?")
        assertEquals(listOf("Could", "I", "have", "an", "iced", "latte", "please"), tokens)

        val scrambled = EnGameMathEngine.scrambleTokens(tokens, seed = 42)
        assertEquals(tokens.sorted(), scrambled.sorted())
        assertNotEquals("Scrambled order must differ from target order", tokens, scrambled)
    }

    @Test
    fun generateQuestion_producesValidQuestionsForAll8EnGameModes() {
        assertEquals(8, EnGameMode.entries.size)
        EnGameMode.entries.forEach { mode ->
            val q = EnGameMathEngine.generateQuestion(
                mode = mode,
                eloRating = 1250,
                vocabPool = samplePool,
                weakWordIds = setOf("w1"),
                travelSentences = sampleSentences
            )
            assertEquals(mode, q.mode)
            assertTrue(q.audioTextEn.isNotBlank())
            assertTrue(q.correctAnswer.isNotBlank())
            when (mode) {
                EnGameMode.SENTENCE_BUILDER, EnGameMode.SPELLING_TILES -> {
                    assertTrue(q.targetTokens.size >= 3)
                    assertEquals(q.targetTokens.size, q.scrambledTokens.size)
                }
                EnGameMode.MEMORY_FLIP -> {
                    assertEquals(8, q.memoryFlipCards.size)
                    assertEquals(4, q.memoryFlipCards.map { it.pairId }.distinct().size)
                }
                else -> {
                    assertTrue("Multiple choice options must contain correct answer for $mode", q.correctAnswer in q.options)
                    assertEquals("Should provide 4 options for $mode", 4, q.options.size)
                    if (mode == EnGameMode.PICTURE_MATCH) {
                        assertTrue("PICTURE_MATCH must include pictureTopic", q.pictureTopic != null)
                        assertTrue("PICTURE_MATCH must include targetWord for semantic illustration", q.targetWord != null)
                    }
                    if (mode == EnGameMode.CLOZE_CONTEXT) {
                        assertTrue("CLOZE_CONTEXT must include blank marker", q.promptPrimary.contains("_____"))
                    }
                }
            }
        }
    }

    @Test
    fun computeRank_gradesAccuratelyAcrossTiers() {
        assertEquals(EnGameRank.S_PLUS, EnGameMathEngine.computeRank(accuracyPercent = 96, maxCombo = 7, score = 1200))
        assertEquals(EnGameRank.S, EnGameMathEngine.computeRank(accuracyPercent = 90, maxCombo = 4, score = 700))
        assertEquals(EnGameRank.A, EnGameMathEngine.computeRank(accuracyPercent = 80, maxCombo = 3, score = 420))
        assertEquals(EnGameRank.B, EnGameMathEngine.computeRank(accuracyPercent = 65, maxCombo = 2, score = 200))
        assertEquals(EnGameRank.C, EnGameMathEngine.computeRank(accuracyPercent = 40, maxCombo = 1, score = 100))
    }
}
