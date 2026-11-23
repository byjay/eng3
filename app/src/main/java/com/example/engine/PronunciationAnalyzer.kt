package com.example.engine

import com.example.model.PronunciationReport
import com.example.model.TokenStatus
import com.example.model.WordPronunciationToken
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object PronunciationAnalyzer {

    fun analyze(
        targetText: String,
        recognizedText: String,
        durationMs: Long = 3500L
    ): PronunciationReport {
        val targetWords = tokenize(targetText)
        val spokenWords = tokenize(recognizedText)

        if (targetWords.isEmpty()) {
            return PronunciationReport(
                targetText = targetText,
                recognizedText = recognizedText,
                overallScore = 0,
                fluencyGrade = "C",
                estimatedWpm = 0,
                tokens = emptyList(),
                phoneticFeedbackKo = "목표 문장이 비어 있습니다.",
                mouthTipKo = "마이크 버튼을 누르고 또렷하게 말해 보세요.",
                passed = false
            )
        }

        if (spokenWords.isEmpty()) {
            val missed = targetWords.map { w ->
                WordPronunciationToken(
                    targetWord = w,
                    spokenWord = null,
                    status = TokenStatus.MISSED,
                    similarityPercent = 0,
                    phoneticTipKo = diagnoseWordPhonetics(w, null)
                )
            }
            return PronunciationReport(
                targetText = targetText,
                recognizedText = recognizedText,
                overallScore = 0,
                fluencyGrade = "C",
                estimatedWpm = 0,
                tokens = missed,
                phoneticFeedbackKo = "음성이 인식되지 않았습니다. 마이크에 가까이 대고 천천히 발음해 보세요.",
                mouthTipKo = "첫 음절의 강세를 또렷하게 살려 말하면 인식률이 확 올라갑니다.",
                passed = false
            )
        }

        val usedIndices = mutableSetOf<Int>()
        val tokens = targetWords.mapIndexed { tIdx, target ->
            var bestIdx = -1
            var bestSim = -1f
            val windowStart = max(0, tIdx - 3)
            val windowEnd = min(spokenWords.lastIndex, tIdx + 3)

            for (sIdx in windowStart..windowEnd) {
                if (sIdx in usedIndices) continue
                val sim = combinedWordSimilarity(target, spokenWords[sIdx])
                if (sim > bestSim) {
                    bestSim = sim
                    bestIdx = sIdx
                }
            }
            // Fallback search across entire utterance for reordered/inserted words
            if (bestSim < 0.65f) {
                for (sIdx in spokenWords.indices) {
                    if (sIdx in usedIndices) continue
                    val sim = combinedWordSimilarity(target, spokenWords[sIdx])
                    if (sim > bestSim) {
                        bestSim = sim
                        bestIdx = sIdx
                    }
                }
            }

            if (bestIdx >= 0 && bestSim >= 0.45f) {
                usedIndices.add(bestIdx)
                val spoken = spokenWords[bestIdx]
                val pct = (bestSim * 100f).roundToInt().coerceIn(0, 100)
                val status = when {
                    pct >= 86 -> TokenStatus.EXACT
                    pct >= 62 -> TokenStatus.CLOSE
                    else -> TokenStatus.MISSED
                }
                WordPronunciationToken(
                    targetWord = target,
                    spokenWord = spoken,
                    status = status,
                    similarityPercent = pct,
                    phoneticTipKo = if (status == TokenStatus.EXACT) null else diagnoseWordPhonetics(target, spoken)
                )
            } else {
                WordPronunciationToken(
                    targetWord = target,
                    spokenWord = null,
                    status = TokenStatus.MISSED,
                    similarityPercent = 0,
                    phoneticTipKo = diagnoseWordPhonetics(target, null)
                )
            }
        }

        val avgScore = tokens.map { it.similarityPercent }.average().roundToInt().coerceIn(0, 100)
        val minutes = (durationMs.coerceAtLeast(1200L)) / 60_000.0
        val wpm = (spokenWords.size / minutes).roundToInt().coerceIn(35, 220)

        val grade = when {
            avgScore >= 94 -> "S"
            avgScore >= 86 -> "A+"
            avgScore >= 76 -> "A"
            avgScore >= 60 -> "B"
            else -> "C"
        }

        val weakTokens = tokens.filter { it.status != TokenStatus.EXACT }
        val phoneticFeedbackKo = when {
            avgScore >= 94 -> "원어민 수준의 완벽한 발음과 리듬입니다! 강세와 연음이 아주 자연스러워요."
            avgScore >= 82 -> "아주 훌륭해요! '${weakTokens.firstOrNull()?.targetWord ?: ""}' 발음만 살짝 더 또렷하게 굴려주면 네이티브급입니다."
            avgScore >= 68 -> "핵심 의미는 잘 전달됐어요. 빨간/노란 표시 단어를 원어민 음성과 비교하며 한 번 더 따라 말해 보세요."
            else -> "천천히 0.8x 속도로 원어민 발음을 먼저 듣고, 강세가 있는 모음을 길게 발음해 보세요!"
        }

        val mouthTipKo = weakTokens.firstOrNull()?.phoneticTipKo
            ?: "강세 음절을 한 톤 높이고, 단어 끝 자음을 부드럽게 이어주는 연음(Linking)이 뛰어납니다!"

        return PronunciationReport(
            targetText = targetText,
            recognizedText = recognizedText,
            overallScore = avgScore,
            fluencyGrade = grade,
            estimatedWpm = wpm,
            tokens = tokens,
            phoneticFeedbackKo = phoneticFeedbackKo,
            mouthTipKo = mouthTipKo,
            passed = avgScore >= 72
        )
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace("[^a-z0-9' ]".toRegex(), " ")
            .split("\\s+".toRegex())
            .map { it.trim('\'') }
            .filter { it.isNotBlank() }
    }

    private fun combinedWordSimilarity(a: String, b: String): Float {
        if (a == b) return 1.0f
        val normA = expandContraction(a)
        val normB = expandContraction(b)
        if (normA == normB) return 0.98f

        val editSim = 1f - (levenshtein(normA, normB).toFloat() / max(normA.length, normB.length).coerceAtLeast(1))
        val soundA = phoneticKey(normA)
        val soundB = phoneticKey(normB)
        val soundSim = if (soundA == soundB) 0.92f
        else 1f - (levenshtein(soundA, soundB).toFloat() / max(soundA.length, soundB.length).coerceAtLeast(1))

        return (editSim * 0.65f + soundSim * 0.35f).coerceIn(0f, 1f)
    }

    private fun expandContraction(w: String): String = when (w) {
        "i'm" -> "iam"
        "don't" -> "dont"
        "can't" -> "cant"
        "won't" -> "wont"
        "it's" -> "its"
        "i'd" -> "id"
        "i've" -> "ive"
        "we're" -> "were"
        "you're" -> "youre"
        "that's" -> "thats"
        "there's" -> "theres"
        else -> w.replace("'", "")
    }

    private fun phoneticKey(word: String): String {
        return word
            .replace("ph", "f")
            .replace("ck", "k")
            .replace("qu", "kw")
            .replace("th", "d")
            .replace("tion", "shn")
            .replace("sion", "shn")
            .replace("gh", "")
            .replace("kn", "n")
            .replace("wr", "r")
            .replace("[aeiouy]+".toRegex(), "a")
            .replace("(.)\\1+".toRegex(), "$1")
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    private fun diagnoseWordPhonetics(target: String, spoken: String?): String {
        val w = target.lowercase()
        return when {
            "th" in w -> "'$target'의 th(/θ,ð/) 소리: 혀끝을 윗니와 아랫니 사이에 살짝 물고 바람을 빼며 발음하세요."
            "f" in w || "ph" in w -> "'$target'의 F(/f/) 소리: 윗니로 아랫입술을 가볍게 스치며 'ㅍ'이 아닌 마찰음을 내세요."
            "v" in w -> "'$target'의 V(/v/) 소리: 윗니를 아랫입술에 대고 성대를 진동시키며 'ㅂ'과 구별하세요."
            "r" in w && "l" in w -> "'$target'의 R/L 구분: R은 혀를 입천장에 닿지 않게 말고, L은 혀끝을 윗니 뒤 잇몸에 꼭 대세요."
            "r" in w -> "'$target'의 R(/r/) 소리: 혀끝을 뒤로 살짝 말아 입천장에 닿지 않게 발음하세요."
            "l" in w -> "'$target'의 L(/l/) 소리: 혀끝을 윗니 바로 뒤 잇몸에 정확히 붙였다 떼세요."
            "z" in w -> "'$target'의 Z(/z/) 소리: 'ㅈ'이 아니라 윗니 사이로 성대 진동을 길게 빼주세요."
            spoken == null -> "'$target' 단어가 누락되었습니다. 원어민 음성을 듣고 강세 음절을 또렷하게 따라 말해 보세요."
            else -> "'$target' (인식: '$spoken') — 모음 길이를 조금 더 여유 있게 잡고 끝 자음을 또렷이 마무리해 보세요."
        }
    }
}
