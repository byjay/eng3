package com.example.engine

import com.example.model.VocabExample
import com.example.model.VocabWord
import java.util.Locale

data class TranslationQualityReport(
    val original: String,
    val polished: String,
    val naturalnessScore: Int,
    val issuesDetected: List<String>,
    val wasModified: Boolean
)

/**
 * Korean Translation Naturalization & Grammar Polishing Engine.
 *
 * Eliminates stiff literal English-ese ("번역투"), broken template concatenations
 * (e.g. `'적응하다' 한다`, `'일관된' 하고`), and parenthetical placeholder particles
 * (`이(가)`, `을(를)`, `은(는)`, `과(와)`, `(으)로`) using exact Hangul Unicode Jongseong
 * (받침) rules and predicate conjugation heuristics.
 */
object KoreanTranslationRefinerEngine {

    const val NATURAL_KOREAN_PROMPT_RULES: String =
        "Korean translation quality rules (CRITICAL): " +
            "Write natural, idiomatic modern Korean that real Koreans actually say. " +
            "NEVER use stiff literal translationese (번역투) such as '당신은 ~을 가지고 있다', '그것은 ~하는 것이다', or '이(가)/을(를)'. " +
            "Match Korean particles (이/가, 을/를, 은/는, 과/와, 으로/로) accurately to the final syllable batchim, " +
            "and use warm conversational endings (~해요/~할까요/~이에요) for video-call subtitles."

    private val parentheticalParticleRegex = Regex(
        """([^\s(\[]+(?:\([^)]*\))?['"”’]?)(이\(가\)|가\(이\)|을\(를\)|를\(을\)|은\(는\)|는\(은\)|과\(와\)|와\(과\)|\(으\)로|으로\(로\)|로\(으로\))"""
    )

    private val mechanicalVerbTemplateRegex = Regex(
        """['"]([^'"]+?)하다['"]\s*(한다|할 거야|할 것이다|해볼 수 있을까|해보자|할 수 있다)"""
    )

    private val mechanicalAdjTemplateRegex = Regex(
        """['"]([^'"]+?)['"]\s*(하고|한 상태|개념을)"""
    )

    private val multiSpaceRegex = Regex("""[ \t]{2,}""")
    private val duplicateParticleRegex = Regex("""(을\s+를|를\s+을|이\s+가|가\s+이|은\s+는|는\s+은)""")

    private val translationeseReplacements: List<Pair<Regex, String>> = listOf(
        Regex("""당신은\s+(.+?)을\s+가지고\s+있다""") to "$1을 갖추고 있다",
        Regex("""당신은\s+(.+?)를\s+가지고\s+있다""") to "$1를 갖추고 있다",
        Regex("""그것은\s+(.+?)하는\s+것이다""") to "$1하는 셈이다",
        Regex("""매우\s+(.+?)한\s+것처럼\s+보인다""") to "꽤 $1해 보인다",
        Regex("""~의\s+탓으로\s+돌리다""") to "원인으로 보다",
        Regex("""할\s+수\s+있는\s+능력이\s+충분하다""") to "충분히 해낼 수 있다",
        Regex("""에\s+대해\s+감사하다""") to "에 고마워하다",
        Regex("""을\s+만들어낸다""") to "을 만든다",
        Regex("""를\s+만들어낸다""") to "를 만든다"
    )

    /**
     * Checks whether the final meaningful character of [text] has a Korean 받침 (Jongseong).
     * Handles Hangul syllables, trailing closing quotes/parentheses, digits, and English letters.
     */
    fun hasBatchim(text: String): Boolean {
        val target = extractLastMeaningfulChar(text) ?: return false
        return when {
            target in '\uAC00'..'\uD7A3' -> ((target.code - 0xAC00) % 28) != 0
            target in '0'..'9' -> target in setOf('0', '1', '3', '6', '7', '8')
            target.lowercaseChar() in 'a'..'z' -> {
                val lower = target.lowercaseChar()
                lower in setOf('l', 'm', 'n', 'r', 'b', 'c', 'k', 'p', 't')
            }
            else -> false
        }
    }

    /**
     * Checks whether the final Hangul syllable has ㄹ 받침 (jongseong index 8),
     * which takes '로' instead of '으로'.
     */
    fun hasRieulBatchim(text: String): Boolean {
        val target = extractLastMeaningfulChar(text) ?: return false
        return target in '\uAC00'..'\uD7A3' && ((target.code - 0xAC00) % 28) == 8
    }

    private fun extractLastMeaningfulChar(raw: String): Char? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        // If text ends with parenthetical Korean meaning like `'consistent'(일관된)`, inspect inside parens first
        val trailingParen = Regex("""\(([^()]+)\)['"”’\s]*$""").find(cleaned)
        val probe = trailingParen?.groupValues?.getOrNull(1)?.trim().takeIf { !it.isNullOrBlank() } ?: cleaned
        for (i in probe.length - 1 downTo 0) {
            val ch = probe[i]
            if (ch in '\uAC00'..'\uD7A3' || ch.isLetterOrDigit()) {
                return ch
            }
        }
        return null
    }

    /**
     * Selects the grammatically correct Korean particle for [precedingText].
     * Supported pairs: "이/가", "을/를", "은/는", "과/와", "으로/로", "이나/나", "이랑/랑".
     */
    fun josa(precedingText: String, pair: String): String {
        val batchim = hasBatchim(precedingText)
        return when (pair.trim()) {
            "이/가", "가/이" -> if (batchim) "이" else "가"
            "을/를", "를/을" -> if (batchim) "을" else "를"
            "은/는", "는/은" -> if (batchim) "은" else "는"
            "과/와", "와/과" -> if (batchim) "과" else "와"
            "으로/로", "로/으로" -> if (!batchim || hasRieulBatchim(precedingText)) "로" else "으로"
            "이나/나", "나/이나" -> if (batchim) "이나" else "나"
            "이랑/랑", "랑/이랑" -> if (batchim) "이랑" else "랑"
            else -> pair
        }
    }

    /**
     * Attaches the exact 받침-matched Korean particle directly onto [nounPhrase].
     */
    fun attachJosa(nounPhrase: String, pair: String): String {
        val clean = nounPhrase.trim()
        if (clean.isEmpty()) return clean
        return clean + josa(clean, pair)
    }

    /**
     * Normalizes a single Korean vocabulary meaning so it reads cleanly and concisely.
     */
    fun normalizeMeaningKo(rawMeaning: String): String {
        val cleaned = rawMeaning
            .replace(";", ",")
            .replace(" / ", ", ")
            .replace(multiSpaceRegex, " ")
            .trim()
            .trimEnd('.', ',')
        if (cleaned.isBlank()) return rawMeaning.trim()
        return cleaned
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
    }

    /**
     * Polishes a Korean sentence or subtitle by fixing parenthetical particles,
     * mechanical template artifacts, duplicated particles, and stiff translationese.
     */
    fun polishKorean(rawText: String): String {
        return analyzeQuality(rawText).polished
    }

    /**
     * Evaluates a Korean translation and returns both a 0-100 naturalness score
     * and the automatically refined text with human-readable fix notes.
     */
    fun analyzeQuality(rawText: String): TranslationQualityReport {
        val original = rawText.trim()
        if (original.isBlank()) {
            return TranslationQualityReport(
                original = original,
                polished = original,
                naturalnessScore = 0,
                issuesDetected = listOf("번역 문장이 비어 있습니다."),
                wasModified = false
            )
        }

        var current = original
        val issues = mutableListOf<String>()
        var penalty = 0

        // 1. Resolve placeholder particles like 이(가), 을(를), 은(는), 와(과), (으)로
        if (parentheticalParticleRegex.containsMatchIn(current)) {
            issues += "어색한 괄호 조사(이(가)·을(를) 등) 자동 교정"
            penalty += 18
            current = parentheticalParticleRegex.replace(current) { match ->
                val head = match.groupValues[1]
                val rawParticle = match.groupValues[2]
                val pair = when {
                    rawParticle.contains("이") && rawParticle.contains("가") -> "이/가"
                    rawParticle.contains("을") && rawParticle.contains("를") -> "을/를"
                    rawParticle.contains("은") && rawParticle.contains("는") -> "은/는"
                    rawParticle.contains("과") && rawParticle.contains("와") -> "과/와"
                    else -> "으로/로"
                }
                head + josa(head, pair)
            }
        }

        // 2. Fix mechanical verb templates like "'적응하다' 한다" -> "적응한다"
        if (mechanicalVerbTemplateRegex.containsMatchIn(current)) {
            issues += "기계식 동사 번역 템플릿('~하다' 한다) 자연화"
            penalty += 25
            current = mechanicalVerbTemplateRegex.replace(current) { match ->
                val stem = match.groupValues[1].trim()
                val ending = match.groupValues[2].trim()
                when (ending) {
                    "한다" -> "${stem}한다"
                    "할 거야" -> "${stem}할 거야"
                    "할 것이다" -> "${stem}할 것이다"
                    "해볼 수 있을까" -> "${stem}해 볼 수 있을까"
                    "해보자" -> "${stem}해 보자"
                    "할 수 있다" -> "${stem}할 수 있다"
                    else -> "${stem}${ending}"
                }
            }
        }

        // 3. Fix mechanical adjective/noun quote templates like "'일관된' 하고"
        if (mechanicalAdjTemplateRegex.containsMatchIn(current)) {
            issues += "어색한 따옴표 결합 표현 자연화"
            penalty += 20
            current = mechanicalAdjTemplateRegex.replace(current) { match ->
                val inner = match.groupValues[1].trim()
                val tail = match.groupValues[2].trim()
                when (tail) {
                    "하고" -> conjugateAdjectiveConnective(inner)
                    "한 상태" -> "${inner.removeSuffix("한")}한 상태"
                    "개념을" -> "$inner 개념을"
                    else -> "$inner $tail"
                }
            }
        }

        // 4. Fix duplicated particles
        if (duplicateParticleRegex.containsMatchIn(current)) {
            issues += "중복 조사 정리"
            penalty += 15
            current = duplicateParticleRegex.replace(current) { m ->
                m.value.split(Regex("\\s+")).firstOrNull().orEmpty()
            }
        }

        // 5. Refine stiff literal English-ese ("번역투")
        for ((pattern, replacement) in translationeseReplacements) {
            if (pattern.containsMatchIn(current)) {
                issues += "직역 번역투 표현을 자연스러운 한국어로 다듬음"
                penalty += 12
                current = pattern.replace(current, replacement)
            }
        }

        // 6. Clean double spaces and spacing before punctuation
        val collapsed = current
            .replace(multiSpaceRegex, " ")
            .replace(Regex("""\s+([,.?!])"""), "$1")
            .trim()
        if (collapsed != current) {
            issues += "불필요한 공백 정리"
            penalty += 5
            current = collapsed
        }

        // Check residual awkwardness indicators for transparent scoring
        if (current.contains("  ")) penalty += 5
        if (Regex("""'[가-힣\s]+'\s*(한다|하고|할)""").containsMatchIn(current)) {
            issues += "기계식 결합 어투 감지(직접 수정 권장)"
            penalty += 18
        }

        val score = (100 - penalty).coerceIn(25, 100)
        return TranslationQualityReport(
            original = original,
            polished = current,
            naturalnessScore = score,
            issuesDetected = issues.distinct(),
            wasModified = current != original
        )
    }

    /**
     * Converts a Korean adjective/modifier meaning like "일관된", "정확한", "적절한", "효율적인"
     * into a natural connective form for spoken Korean (e.g., "일관되고", "정확하고", "효율적이고").
     */
    fun conjugateAdjectiveConnective(rawAdj: String): String {
        val base = rawAdj.substringBefore(",").trim()
        return when {
            base.endsWith("적인") -> base.removeSuffix("적인") + "적이고"
            base.endsWith("스러운") -> base.removeSuffix("스러운") + "스럽고"
            base.endsWith("로운") -> base.removeSuffix("로운") + "롭고"
            base.endsWith("한") -> base.removeSuffix("한") + "하고"
            base.endsWith("된") -> base.removeSuffix("된") + "되고"
            base.endsWith("있는") -> base.removeSuffix("있는") + "있고"
            base.endsWith("없는") -> base.removeSuffix("없는") + "없고"
            base.endsWith("운") -> base.removeSuffix("운") + "우면서"
            base.endsWith("인") -> base.removeSuffix("인") + "이면서"
            base.endsWith("의") -> base.removeSuffix("의") + " 성격이 뚜렷하고"
            else -> "$base 느낌이 살아 있고"
        }
    }

    /**
     * Conjugates a Korean dictionary-form verb like "적응하다", "향상시키다", "줄이다", "깨닫다"
     * into a natural declarative/conversational phrase instead of raw `'~하다' 한다`.
     */
    fun conjugateVerbPresent(rawVerb: String): String {
        val base = rawVerb.substringBefore(",").trim()
        return when {
            base.endsWith("하다") -> base.removeSuffix("하다") + "한다"
            base.endsWith("되다") -> base.removeSuffix("되다") + "된다"
            base.endsWith("시키다") -> base.removeSuffix("시키다") + "시킨다"
            base.endsWith("이다") -> base.removeSuffix("이다") + "인다"
            base.endsWith("지다") -> base.removeSuffix("지다") + "진다"
            base.endsWith("나다") -> base.removeSuffix("나다") + "난다"
            base.endsWith("가다") -> base.removeSuffix("가다") + "간다"
            base.endsWith("오다") -> base.removeSuffix("오다") + "온다"
            base.endsWith("주다") -> base.removeSuffix("주다") + "준다"
            base.endsWith("내다") -> base.removeSuffix("내다") + "낸다"
            base.endsWith("치다") -> base.removeSuffix("치다") + "친다"
            base.endsWith("리다") -> base.removeSuffix("리다") + "린다"
            base.endsWith("기다") -> base.removeSuffix("기다") + "긴다"
            base.endsWith("다") -> base.removeSuffix("다") + "는 경향이 있다"
            else -> "$base 역할을 한다"
        }
    }

    /**
     * Builds natural, idiomatic Korean & English contextual sentences for any vocabulary word
     * without awkward mechanical templates or personal family lore.
     */
    fun buildNaturalVocabExamples(
        word: String,
        ipa: String,
        pos: String,
        meaningKo: String,
        coreEn: String,
        coreKo: String
    ): List<VocabExample> {
        val w = word.trim()
        val primaryMeaning = meaningKo.substringBefore(",").trim().ifBlank { meaningKo.trim() }
        val lowerPos = pos.trim().lowercase(Locale.US)
        val isVerb = lowerPos.startsWith("v")
        val isAdj = lowerPos.startsWith("adj")
        val isAdv = lowerPos.startsWith("adv")

        val readingSentence = when {
            isVerb -> VocabExample(
                labelKo = "수능 독해 용례",
                english = "In academic passages, researchers frequently use \"$w\" to explain how key factors interact.",
                korean = "수능 독해 지문에서는 핵심 요인들이 어떻게 작용하는지 설명할 때 '$w'($primaryMeaning) 표현이 자주 쓰인다.",
                ipaHint = ipa
            )
            isAdj -> VocabExample(
                labelKo = "수능 독해 용례",
                english = "The author highlights why a $w approach leads to stronger long-term results.",
                korean = "필자는 $primaryMeaning 접근 방식이 왜 더 탄탄한 장기 성과로 이어지는지 강조한다.",
                ipaHint = ipa
            )
            isAdv -> VocabExample(
                labelKo = "수능 독해 용례",
                english = "The study shows that small habits can $w reshape everyday performance.",
                korean = "이 연구는 작은 습관이 일상 성과를 $primaryMeaning 바꿀 수 있음을 보여 준다.",
                ipaHint = ipa
            )
            else -> VocabExample(
                labelKo = "수능 독해 용례",
                english = "Grasping the exact role of $w makes complex exam passages much easier to follow.",
                korean = "${attachJosa(primaryMeaning, "의".takeIf { false } ?: "이/가")} 문맥에서 맡는 역할을 정확히 알면 복잡한 독해 지문도 훨씬 쉽게 풀린다.",
                ipaHint = ipa
            )
        }

        val conversationSentence = when {
            isVerb -> VocabExample(
                labelKo = "원어민 일상 회화",
                english = "Let's practice using \"$w\" naturally when we talk about today's topic.",
                korean = "오늘 주제로 대화할 때 '$w'($primaryMeaning) 표현을 자연스럽게 써 보자.",
                ipaHint = ipa
            )
            isAdj -> VocabExample(
                labelKo = "원어민 일상 회화",
                english = "Honestly, your suggestion sounds really $w and practical!",
                korean = "솔직히 네 제안이 정말 ${conjugateAdjectiveConnective(primaryMeaning)} 실용적으로 들려!",
                ipaHint = ipa
            )
            isAdv -> VocabExample(
                labelKo = "원어민 일상 회화",
                english = "Once you notice the rhythm, you can speak much more $w.",
                korean = "리듬을 한 번 익히면 훨씬 더 $primaryMeaning 말할 수 있어.",
                ipaHint = ipa
            )
            else -> VocabExample(
                labelKo = "원어민 일상 회화",
                english = "Did you catch how the speaker explained that $w just now?",
                korean = "방금 화자가 그 ${attachJosa(primaryMeaning, "을/를")} 어떻게 설명했는지 들었어?",
                ipaHint = ipa
            )
        }

        val speakingChallengeSentence = when {
            isVerb -> VocabExample(
                labelKo = "실전 스피킹 응용",
                english = "Using \"$w\" in a short personal story helps lock the meaning into long-term memory.",
                korean = "짧은 내 이야기에 '$w'($primaryMeaning) 표현을 넣어 말해 보면 장기 기억에 오래 남는다.",
                ipaHint = ipa
            )
            isAdj -> VocabExample(
                labelKo = "실전 스피킹 응용",
                english = "Try describing a recent experience that felt genuinely $w to you.",
                korean = "최근 겪었던 일 중에서 정말 $primaryMeaning 느낌이 들었던 순간을 영어로 말해 보자.",
                ipaHint = ipa
            )
            isAdv -> VocabExample(
                labelKo = "실전 스피킹 응용",
                english = "Say this sentence out loud $w while keeping a steady natural pace.",
                korean = "일정한 속도를 유지하면서 이 문장을 $primaryMeaning 소리 내어 말해 보자.",
                ipaHint = ipa
            )
            else -> VocabExample(
                labelKo = "실전 스피킹 응용",
                english = "Clear examples make ${attachEnglishArticle(w)} easy to remember in real conversations.",
                korean = "구체적인 예문과 함께 익히면 실제 회화에서도 ${attachJosa(primaryMeaning, "이/가")} 바로 떠오른다.",
                ipaHint = ipa
            )
        }

        val cleanCoreEn = coreEn.trim().ifBlank {
            when {
                isVerb -> "Practicing daily helps learners $w new ideas with confidence."
                isAdj -> "A $w habit makes a noticeable difference over time."
                isAdv -> "Consistent learners improve $w through short daily sessions."
                else -> "Understanding $w helps you read academic passages accurately."
            }
        }
        val cleanCoreKo = polishKorean(
            coreKo.trim().ifBlank {
                when {
                    isVerb -> "매일 연습하면 새로운 내용을 자신 있게 ${conjugateVerbPresent(primaryMeaning)}."
                    isAdj -> "$primaryMeaning 습관은 시간이 지날수록 뚜렷한 차이를 만든다."
                    isAdv -> "꾸준히 공부하는 학습자는 매일 짧은 연습으로 $primaryMeaning 성장한다."
                    else -> "${attachJosa(primaryMeaning, "을/를")} 정확히 이해하면 영어 지문을 훨씬 또렷하게 읽을 수 있다."
                }
            }
        )

        return listOf(
            VocabExample(
                labelKo = "핵심 대표 예문",
                english = cleanCoreEn,
                korean = cleanCoreKo,
                ipaHint = ipa
            ),
            readingSentence.copy(korean = polishKorean(readingSentence.korean)),
            conversationSentence.copy(korean = polishKorean(conversationSentence.korean)),
            speakingChallengeSentence.copy(korean = polishKorean(speakingChallengeSentence.korean))
        )
    }

    private fun attachEnglishArticle(word: String): String {
        val clean = word.trim()
        if (clean.isBlank()) return "the word"
        return "\"$clean\""
    }

    /**
     * Automatically polishes a [VocabWord]'s Korean meaning, example translation, and all example sentences.
     */
    fun polishVocabWord(word: VocabWord): VocabWord {
        val refinedMeaning = normalizeMeaningKo(word.meaningKo)
        val refinedExampleKo = polishKorean(word.exampleKo)
        val refinedExamples = word.examples.map { ex ->
            ex.copy(korean = polishKorean(ex.korean))
        }
        val refinedTip = polishKorean(word.pronunciationTipKo)
        return word.copy(
            meaningKo = refinedMeaning,
            exampleKo = refinedExampleKo,
            examples = refinedExamples,
            pronunciationTipKo = refinedTip
        )
    }
}
