package com.example.engine

import com.example.model.VocabWord
import com.example.ui.components.MotionPhotoGesture
import java.util.Locale

data class MotionVocabQuestionBundle(
    val word: VocabWord,
    val motion: MotionPhotoGesture,
    val questionEn: String,
    val questionKo: String,
    val hintReplyEn: String,
    val hintReplyKo: String,
    val promptDirectiveEn: String
)

/**
 * Synchronizes all 33 video-call tutor motions (`MotionPhotoGesture`) with mandatory
 * learned-vocabulary questions:
 * 1. Selects the learner's studied/mastered/review vocabulary words (with graceful starter fallback).
 * 2. Maps ALL 33 `MotionPhotoGesture` expressions to tailored spoken question cues.
 * 3. Guarantees via deterministic post-enforcement that EVERY tutor reply during a video call
 *    strictly contains the target learned word inside a question (`?`), even if an LLM omits it
 *    or offline fallback is used.
 * 4. Detects when the learner actively uses the target word in their reply so the tutor celebrates
 *    with energetic positive motions.
 */
object LearnedVocabCallMotionEngine {

    private val positiveCelebrationMotions = listOf(
        MotionPhotoGesture.CLAPPING_BRAVO,
        MotionPhotoGesture.EXCITED_BOUNCE,
        MotionPhotoGesture.HIGH_FIVE_LEAN,
        MotionPhotoGesture.THUMBS_UP_COOL,
        MotionPhotoGesture.VICTORY_DANCE,
        MotionPhotoGesture.AMAZED_SPARKLE,
        MotionPhotoGesture.PROUD_CHEER,
        MotionPhotoGesture.RAINBOW_CHEER
    )

    private val conversationalQuestionMotions = listOf(
        MotionPhotoGesture.CURIOUS_TILT,
        MotionPhotoGesture.SPEAK_ANIMATED,
        MotionPhotoGesture.THOUGHTFUL_MUSE,
        MotionPhotoGesture.WARM_SMILE,
        MotionPhotoGesture.PLAYFUL_WINK,
        MotionPhotoGesture.ENTHUSIASTIC_NOD,
        MotionPhotoGesture.DOUBLE_NOD,
        MotionPhotoGesture.LISTEN_LEAN_IN,
        MotionPhotoGesture.SURPRISED_WOW,
        MotionPhotoGesture.CONFIDENT_SMIRK,
        MotionPhotoGesture.SALUTE_READY,
        MotionPhotoGesture.PEACE_SIGN,
        MotionPhotoGesture.LAUGHING_JOY,
        MotionPhotoGesture.HEART_EYES,
        MotionPhotoGesture.PLAYFUL_TEASE,
        MotionPhotoGesture.DRAMATIC_GASP,
        MotionPhotoGesture.MUSIC_SWAY,
        MotionPhotoGesture.SHY_BLUSH,
        MotionPhotoGesture.GENTLE_COMFORT,
        MotionPhotoGesture.SUNSET_DREAM
    )

    /**
     * Selects the next learned vocabulary word to weave into the tutor's spoken question.
     * Priority:
     *  1) Words the learner actually studied today / mastered / reviewed via SM-2
     *  2) Words in the learner's weak/review list
     *  3) Starter fallback from the available vocabulary catalog (never returns null if pool is non-empty)
     */
    fun selectTargetLearnedWord(
        allWords: List<VocabWord>,
        masteredWordIds: Set<String>,
        weakWordIds: Set<String>,
        sm2StudiedWordIds: Set<String>,
        todayStudiedWordIds: Set<String>,
        recentlyAskedWordIds: Set<String>,
        turnSeed: Int
    ): VocabWord? {
        if (allWords.isEmpty()) return null
        val byId = allWords.associateBy { it.id }

        val learnedIds = LinkedHashSet<String>().apply {
            addAll(todayStudiedWordIds)
            addAll(masteredWordIds)
            addAll(sm2StudiedWordIds)
            addAll(weakWordIds)
        }
        val learnedPool = learnedIds.mapNotNull { byId[it] }
        val activePool = if (learnedPool.isNotEmpty()) learnedPool else allWords

        val freshCandidates = activePool.filterNot { it.id in recentlyAskedWordIds }
        val finalPool = if (freshCandidates.isNotEmpty()) freshCandidates else activePool
        val idx = Math.floorMod(turnSeed, finalPool.size)
        return finalPool[idx]
    }

    /**
     * Picks a synchronized motion from the 33-motion catalog for the current video-call turn.
     */
    fun selectSyncedMotion(
        activeTopicTag: String,
        turnCount: Int,
        isOpening: Boolean,
        learnerUsedPreviousWord: Boolean,
        explicitGesture: MotionPhotoGesture? = null
    ): MotionPhotoGesture {
        if (explicitGesture != null) return explicitGesture
        if (learnerUsedPreviousWord) {
            return positiveCelebrationMotions[Math.floorMod(turnCount, positiveCelebrationMotions.size)]
        }
        if (isOpening) {
            return when {
                activeTopicTag.contains("카페") -> MotionPhotoGesture.COFFEE_CHILL
                activeTopicTag.contains("괌") || activeTopicTag.contains("바다") -> MotionPhotoGesture.OCEAN_BREEZE
                activeTopicTag.contains("뉴욕") || activeTopicTag.contains("시내") -> MotionPhotoGesture.CITY_VIBE
                else -> MotionPhotoGesture.FRIENDLY_WAVE
            }
        }
        val topicBoost = when {
            activeTopicTag.contains("카페") && turnCount % 3 == 0 -> MotionPhotoGesture.COFFEE_CHILL
            (activeTopicTag.contains("괌") || activeTopicTag.contains("바다")) && turnCount % 3 == 0 -> MotionPhotoGesture.OCEAN_BREEZE
            (activeTopicTag.contains("뉴욕") || activeTopicTag.contains("시내")) && turnCount % 3 == 0 -> MotionPhotoGesture.CITY_VIBE
            else -> null
        }
        if (topicBoost != null) return topicBoost
        return conversationalQuestionMotions[Math.floorMod(turnCount, conversationalQuestionMotions.size)]
    }

    /**
     * Checks whether the text includes the learned word (case-insensitive whole word or regular inflection).
     */
    fun containsLearnedWord(text: String, targetWord: String): Boolean {
        val cleanWord = targetWord.trim().lowercase(Locale.US)
        if (cleanWord.isBlank() || text.isBlank()) return false
        val escaped = Regex.escape(cleanWord)
        // Matches exact word or standard English inflections (s/es/ed/ing/d/er/ly)
        val stem = if (cleanWord.length >= 4 && cleanWord.endsWith("e")) {
            Regex.escape(cleanWord.dropLast(1))
        } else {
            escaped
        }
        val pattern = Regex("\\b($escaped(?:s|es|ed|ing|d|ly|er)?|$stem(?:ing|ed))\\b", RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(text)
    }

    /**
     * Detects whether the learner actively used the previous target learned word in their spoken/typed answer.
     */
    fun didLearnerUseTargetWord(learnerText: String, targetWord: VocabWord?): Boolean {
        if (targetWord == null || learnerText.isBlank()) return false
        return containsLearnedWord(learnerText, targetWord.word)
    }

    /**
     * Exhaustively builds a motion-synchronized question bundle for EVERY ONE of the 33 motions.
     */
    fun buildMotionQuestionBundle(
        word: VocabWord,
        motion: MotionPhotoGesture,
        learnerName: String
    ): MotionVocabQuestionBundle {
        val w = word.word.trim()
        val ko = KoreanTranslationRefinerEngine.normalizeMeaningKo(word.meaningKo)
        val enName = learnerName.trim().ifBlank { "friend" }
        val koName = learnerName.trim().ifBlank { "학습자" }
        val token = "'$w'($ko)"
        val tokenIga = KoreanTranslationRefinerEngine.attachJosa(token, "이/가")
        val tokenEul = KoreanTranslationRefinerEngine.attachJosa(token, "을/를")
        val tokenGwa = KoreanTranslationRefinerEngine.attachJosa(token, "과/와")
        val tokenEuro = KoreanTranslationRefinerEngine.attachJosa(token, "으로/로")

        val (qEn, rawQKo) = when (motion) {
            MotionPhotoGesture.WARM_SMILE ->
                "Hey $enName, when do you think \"$w\" matters most in your daily life?" to
                    "${koName}님, 일상에서 $tokenIga 가장 중요할 때는 언제라고 생각해요?"
            MotionPhotoGesture.FRIENDLY_WAVE ->
                "Hi $enName! Can you kick off our chat with a short sentence using \"$w\"?" to
                    "${koName}님 안녕하세요! 오늘 배운 $tokenEul 넣어서 첫 문장을 말해 볼래요?"
            MotionPhotoGesture.ENTHUSIASTIC_NOD ->
                "I totally agree! Could you describe a real moment that felt \"$w\" to you?" to
                    "완전 공감해요! $tokenGwa 딱 어울렸던 순간을 영어로 말해 줄래요?"
            MotionPhotoGesture.DOUBLE_NOD ->
                "Yes, that's the spirit! How would you use \"$w\" when talking with a friend?" to
                    "좋아요, 그 느낌이에요! 친구랑 얘기할 때 $tokenEul 어떻게 써 볼까요?"
            MotionPhotoGesture.CURIOUS_TILT ->
                "Ooh I'm curious, $enName—what comes to your mind first when you hear \"$w\"?" to
                    "궁금해요 ${koName}님! $token 단어를 들으면 뭐가 제일 먼저 떠올라요?"
            MotionPhotoGesture.EXCITED_BOUNCE ->
                "Yay! Ready for a fun challenge using today's word \"$w\" in your answer?" to
                    "좋아요! 오늘의 단어 $tokenEul 넣어서 재미있게 답해 볼 준비 됐나요?"
            MotionPhotoGesture.PLAYFUL_WINK ->
                "Secret quiz time, $enName! Can you sneak the word \"$w\" into your next reply?" to
                    "깜짝 미션이에요 ${koName}님! 다음 대답에 $tokenEul 자연스럽게 넣어 볼 수 있어요?"
            MotionPhotoGesture.LAUGHING_JOY ->
                "Haha I love chatting with you! What's a funny story connected to \"$w\"?" to
                    "하하 대화 정말 재밌어요! $tokenGwa 관련된 재밌는 얘기 하나 있을까요?"
            MotionPhotoGesture.LISTEN_LEAN_IN ->
                "I'm all ears, $enName—can you explain \"$w\" in your own simple English?" to
                    "귀 기울여 듣고 있어요 ${koName}님! $tokenEul 나만의 쉬운 영어로 말해 볼까요?"
            MotionPhotoGesture.PROUD_CHEER ->
                "You're doing great! Can you level up by answering me with the word \"$w\"?" to
                    "정말 잘하고 있어요! 이번엔 $tokenEul 써서 한 단계 더 멋지게 답해 볼래요?"
            MotionPhotoGesture.SURPRISED_WOW ->
                "Whoa, really? What is something surprising you'd connect with \"$w\"?" to
                    "우와 정말요? $tokenGwa 연결되는 깜짝 놀랄 만한 예시가 있을까요?"
            MotionPhotoGesture.THOUGHTFUL_MUSE ->
                "Hmm let's think together—why do you think \"$w\" is so useful to know?" to
                    "음 같이 생각해 봐요! $token 표현을 알아두면 왜 유용할까요?"
            MotionPhotoGesture.SHY_BLUSH ->
                "Even if it feels a little shy, would you try saying one line with \"$w\"?" to
                    "조금 쑥스러워도 괜찮아요! $tokenEul 넣어서 한 문장만 편하게 말해 볼래요?"
            MotionPhotoGesture.CLAPPING_BRAVO ->
                "Bravo, $enName! Now can you show off with another sentence using \"$w\"?" to
                    "브라보 ${koName}님! 이번엔 $tokenEul 넣은 새 문장도 멋지게 뽐내 볼까요?"
            MotionPhotoGesture.HIGH_FIVE_LEAN ->
                "High five! Ready to team up and make a quick example with \"$w\"?" to
                    "하이파이브! 같이 $tokenEuro 짧은 예문 하나 만들어 볼 준비 됐나요?"
            MotionPhotoGesture.THUMBS_UP_COOL ->
                "Awesome job! How would you use \"$w\" to describe your week so far?" to
                    "최고예요! 이번 주를 설명할 때 $tokenEul 어떻게 써 볼 수 있을까요?"
            MotionPhotoGesture.COFFEE_CHILL ->
                "Imagine we're chilling at a cafe—how would you bring up \"$w\" in conversation?" to
                    "카페에서 여유롭게 대화한다고 상상해 봐요! $tokenEul 써서 뭐라고 말해 볼까요?"
            MotionPhotoGesture.OCEAN_BREEZE ->
                "With this sunny ocean breeze, what travel memory reminds you of \"$w\"?" to
                    "시원한 바닷바람 맞으면서, $tokenIga 떠오르는 여행 기억이 있나요?"
            MotionPhotoGesture.CITY_VIBE ->
                "Right here in the bustling city, where might you hear the word \"$w\"?" to
                    "활기찬 거리에서 $token 표현을 어디서 들을 수 있을까요?"
            MotionPhotoGesture.HEART_EYES ->
                "I love your energy! What's something you really care about involving \"$w\"?" to
                    "에너지가 정말 좋아요! $tokenGwa 관련해서 가장 좋아하는 게 뭐예요?"
            MotionPhotoGesture.AMAZED_SPARKLE ->
                "That's sparkling! Can you share one bold idea using the word \"$w\"?" to
                    "반짝반짝 멋져요! $tokenEul 넣어서 멋진 아이디어를 하나 말해 줄래요?"
            MotionPhotoGesture.GENTLE_COMFORT ->
                "No pressure at all, $enName—how would you gently use \"$w\" in a short phrase?" to
                    "전혀 부담 갖지 마세요 ${koName}님! $tokenEuro 편하게 짧은 문장 하나 말해 볼까요?"
            MotionPhotoGesture.PLAYFUL_TEASE ->
                "Bet you can't beat this round—can you answer me using \"$w\" right now?" to
                    "이번 라운드 도전해 볼래요? 지금 바로 $tokenEul 넣어서 대답할 수 있을까요?"
            MotionPhotoGesture.VICTORY_DANCE ->
                "Victory dance time! Want to seal the win with a sentence containing \"$w\"?" to
                    "승리 댄스 타임! $tokenEul 넣은 문장으로 완벽하게 마무리해 볼까요?"
            MotionPhotoGesture.CONFIDENT_SMIRK ->
                "Look how confident you are—can you drop \"$w\" like a true native speaker?" to
                    "자신감 최고예요! 원어민처럼 자연스럽게 $tokenEul 써서 말해 볼래요?"
            MotionPhotoGesture.SPEAK_ANIMATED ->
                "Let's keep the flow going! What do you think about \"$w\" in today's topic?" to
                    "대화 흐름 그대로 이어가요! 오늘 주제에서 ${token}에 대해 어떻게 생각해요?"
            MotionPhotoGesture.DRAMATIC_GASP ->
                "Wait, no way! Could you imagine a dramatic situation involving \"$w\"?" to
                    "헉 진짜요?! $tokenIga 등장하는 드라마틱한 상황을 상상해 볼 수 있나요?"
            MotionPhotoGesture.PEACE_SIGN ->
                "Peace and good vibes! What positive plan can you share using \"$w\"?" to
                    "브이! $tokenEul 넣어서 오늘의 기분 좋은 계획을 하나 말해 줄래요?"
            MotionPhotoGesture.SALUTE_READY ->
                "Mission ready, $enName! Can you report back with a sentence using \"$w\"?" to
                    "미션 준비 완료 ${koName}님! $tokenEul 넣어서 영어로 말해 줄 수 있나요?"
            MotionPhotoGesture.MUSIC_SWAY ->
                "Catch this rhythm! Can you smoothly say a sentence featuring \"$w\"?" to
                    "리듬에 맞춰서! $tokenIga 들어간 문장을 자연스럽게 말해 볼래요?"
            MotionPhotoGesture.RAINBOW_CHEER ->
                "Cheering for you! What exciting goal connects to \"$w\" for you?" to
                    "응원할게요! $tokenGwa 연결되는 신나는 목표가 있나요?"
            MotionPhotoGesture.SUNSET_DREAM ->
                "Looking at the golden sunset, what dream comes to mind with \"$w\"?" to
                    "노을을 보면서 생각해 봐요! $tokenGwa 어울리는 꿈이나 바람이 있나요?"
            MotionPhotoGesture.BYE_WAVE ->
                "Before we wrap up this round, can you give me one parting line with \"$w\"?" to
                    "이번 라운드 마무리하기 전에 $tokenEul 넣어서 한마디 남겨 줄래요?"
        }
        val qKo = KoreanTranslationRefinerEngine.polishKorean(rawQKo)

        val hintEn = buildSuggestedAnswerUsingWord(word)
        val hintKo = KoreanTranslationRefinerEngine.polishKorean("$token 단어를 활용한 자연스러운 답변")
        val directiveEn = """
            MANDATORY LEARNED-VOCABULARY & 33-MOTION SYNC RULE (STRICT):
            - Active 33-Motion Expression: ${motion.name} (${motion.labelKo}) — "${motion.nanoBananaPromptEn}".
            - Target Learned Word: "$w" (Korean meaning: "$ko", POS: ${word.partOfSpeech}).
            - Your REPLY_EN MUST be spoken in a tone matching ${motion.name}, MUST include the exact word "$w", AND MUST end with an engaging question ("?").
            - Make HINT1 naturally include "$w" so the learner can immediately practice speaking it back.
            - ${KoreanTranslationRefinerEngine.NATURAL_KOREAN_PROMPT_RULES}
        """.trimIndent()

        return MotionVocabQuestionBundle(
            word = word,
            motion = motion,
            questionEn = qEn,
            questionKo = qKo,
            hintReplyEn = hintEn,
            hintReplyKo = hintKo,
            promptDirectiveEn = directiveEn
        )
    }

    /**
     * Deterministic post-enforcement on ANY AI or offline reply:
     * Guarantees 100% that `replyEn` contains the target learned word AND asks a question (`?`).
     */
    fun enforceLearnedWordQuestion(
        reply: AiCoachReply,
        word: VocabWord,
        motion: MotionPhotoGesture,
        learnerName: String
    ): AiCoachReply {
        val bundle = buildMotionQuestionBundle(word, motion, learnerName)
        val baseEn = reply.replyEn.trim()
        val hasWord = containsLearnedWord(baseEn, word.word)
        val hasQuestion = baseEn.contains("?")

        val finalReplyEn = when {
            hasWord && hasQuestion -> baseEn
            hasWord && !hasQuestion -> "$baseEn What do you think about \"${word.word}\"?"
            baseEn.isBlank() -> bundle.questionEn
            else -> {
                val trimmedBase = baseEn.trimEnd('?', '!', '.').trim()
                val separator = if (baseEn.endsWith("!")) "! " else ". "
                "$trimmedBase$separator${bundle.questionEn}"
            }
        }

        val finalTranslationKo = KoreanTranslationRefinerEngine.polishKorean(
            when {
                hasWord && hasQuestion && reply.translationKo.isNotBlank() -> reply.translationKo
                reply.translationKo.isBlank() -> bundle.questionKo
                reply.translationKo.contains(word.word, ignoreCase = true) -> reply.translationKo
                else -> "${reply.translationKo.trim()} ${bundle.questionKo}"
            }
        )

        val vocabHint = SuggestedReplyOption(
            english = bundle.hintReplyEn,
            korean = bundle.hintReplyKo
        )
        val mergedHints = buildList {
            add(vocabHint)
            reply.suggestedReplies.forEach { hint ->
                if (!hint.english.equals(vocabHint.english, ignoreCase = true)) {
                    add(hint.copy(korean = KoreanTranslationRefinerEngine.polishKorean(hint.korean)))
                }
            }
        }.take(3)

        val motionTipPrefix = "[${motion.labelKo} · 학습단어 '${word.word}'(${word.meaningKo})] "
        val enrichedTip = KoreanTranslationRefinerEngine.polishKorean(
            if (reply.coachingTipKo.contains(word.word, ignoreCase = true)) {
                reply.coachingTipKo
            } else {
                motionTipPrefix + reply.coachingTipKo.ifBlank { "질문 속 '${word.word}' 단어를 넣어 영어로 대답해 보세요!" }
            }
        )

        return reply.copy(
            replyEn = finalReplyEn,
            translationKo = finalTranslationKo,
            coachingTipKo = enrichedTip,
            suggestedReplies = mergedHints
        )
    }

    private fun buildSuggestedAnswerUsingWord(word: VocabWord): String {
        val w = word.word.trim()
        val pos = word.partOfSpeech.lowercase(Locale.US)
        return when {
            pos.startsWith("v") -> "I try to $w every single day!"
            pos.startsWith("adj") -> "I think it is really $w for my goal!"
            pos.startsWith("adv") -> "I will practice $w starting today!"
            else -> "To me, $w is super important in daily life!"
        }
    }
}
