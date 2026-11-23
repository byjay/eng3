package com.example.data

import android.util.Log
import com.example.model.VocabExample
import com.example.model.VocabTopic
import com.example.model.VocabWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val TAG = "DailyWordHarvester"

data class DailyHarvestResult(
    val newlyAddedWords: List<VocabWord>,
    val sourceLabel: String,
    val topicLabelKo: String
)

/**
 * Real Free Word API Harvester (Zero API key required):
 * - Primary: Datamuse Free Word API (https://api.datamuse.com/words) — returns frequency-scored,
 *   part-of-speech tagged, definition-rich English words by situational topic (50+ per fetch).
 * - Enricher: Free Dictionary API (https://api.dictionaryapi.dev/api/v2/entries/en/{word}) —
 *   fetches real IPA phonetics and native example sentences when available.
 */
object DailyWordHarvesterService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val topicQueryMap: List<Pair<VocabTopic, String>> = listOf(
        VocabTopic.SCHOOL_EXAM to "education,exam,academic,study,learning",
        VocabTopic.TRAVEL_DAILY to "travel,airport,restaurant,shopping,hotel",
        VocabTopic.SCIENCE_AI to "science,technology,research,nature,future",
        VocabTopic.SOCIETY_ECONOMY to "economy,society,culture,community,global",
        VocabTopic.EMOTION_MIND to "emotion,confidence,psychology,family,motivation",
        VocabTopic.LOGIC_DEBATE to "argument,logic,opinion,evidence,solution"
    )

    suspend fun harvestDaily50PlusWords(
        existingWordLowerSet: Set<String>,
        daySeed: Int,
        preferredTopic: VocabTopic? = null
    ): Result<DailyHarvestResult> = withContext(Dispatchers.IO) {
        runCatching {
            val primaryPair = if (preferredTopic != null) {
                topicQueryMap.first { it.first == preferredTopic }
            } else {
                topicQueryMap[daySeed % topicQueryMap.size]
            }
            val secondaryPair = topicQueryMap[(topicQueryMap.indexOf(primaryPair) + 1) % topicQueryMap.size]

            val harvested = mutableListOf<VocabWord>()
            val seen = existingWordLowerSet.toMutableSet()

            // Fetch 2 situational batches (~45 each) so we comfortably accumulate 50+ unique words!
            listOf(primaryPair, secondaryPair).forEachIndexed { batchIdx, (topic, queryTopics) ->
                val encodedTopics = URLEncoder.encode(queryTopics, "UTF-8")
                val url = "https://api.datamuse.com/words?topics=$encodedTopics&md=dfp&max=65"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("무료 단어 API 응답 오류 (${response.code})")
                    }
                    val raw = response.body?.string().orEmpty()
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        if (harvested.size >= 55) break
                        val obj = arr.getJSONObject(i)
                        val wordText = obj.optString("word").trim()
                        if (wordText.length < 4 || wordText.contains(" ") || wordText.contains("-")) continue
                        val lower = wordText.lowercase()
                        if (lower in seen) continue
                        if (!lower.all { it in 'a'..'z' }) continue

                        val defs = obj.optJSONArray("defs")
                        if (defs == null || defs.length() == 0) continue
                        val firstDefRaw = defs.optString(0).trim()
                        val tabSplit = firstDefRaw.split("\t", limit = 2)
                        val rawPos = tabSplit.getOrNull(0)?.trim().orEmpty()
                        val engDef = tabSplit.getOrNull(1)?.trim().orEmpty()
                        if (engDef.length < 8) continue

                        val pos = when (rawPos) {
                            "v" -> "v."
                            "adj" -> "adj."
                            "adv" -> "adv."
                            else -> "n."
                        }

                        // Parse real corpus frequency from Datamuse tags ("f:42.5")
                        val tags = obj.optJSONArray("tags")
                        var freqScore = 10.0
                        if (tags != null) {
                            for (t in 0 until tags.length()) {
                                val tagStr = tags.optString(t)
                                if (tagStr.startsWith("f:")) {
                                    freqScore = tagStr.removePrefix("f:").toDoubleOrNull() ?: 10.0
                                }
                            }
                        }

                        val csatFreq = when {
                            freqScore >= 25.0 -> 5
                            freqScore >= 8.0 -> 4
                            else -> 3
                        }
                        val csatDiff = when {
                            wordText.length >= 10 || freqScore < 5.0 -> 3
                            wordText.length >= 7 || freqScore < 20.0 -> 2
                            else -> 1
                        }
                        val assignedGrade = csatDiff.coerceIn(1, 3)

                        val cleanMeaning = "${topic.labelKo} 핵심 · ${engDef.take(52)}"
                        val sentences = buildContextualSentences(wordText, pos, engDef, topic)

                        seen.add(lower)
                        harvested.add(
                            VocabWord(
                                id = "api_${lower}",
                                grade = assignedGrade,
                                textbookUnitKo = "무료API 일일수집 · ${topic.labelKo}",
                                csatFrequency = csatFreq,
                                csatDifficulty = csatDiff,
                                topic = topic,
                                word = wordText,
                                ipa = "/$lower/",
                                partOfSpeech = pos,
                                meaningKo = cleanMeaning,
                                exampleEn = sentences.first().english,
                                exampleKo = sentences.first().korean,
                                examples = sentences,
                                pronunciationTipKo = "강세를 살려 '$wordText'를 또박또박 읽고, 예문 4개를 원어민처럼 이어 말해보세요!",
                                distractors = emptyList()
                            )
                        )
                    }
                }
            }

            if (harvested.isEmpty()) {
                throw IllegalStateException("새로 수집할 단어가 없거나 네트워크가 오프라인입니다.")
            }

            // Fill realistic distractors from the harvested batch itself (non-alphabetical!)
            val allMeanings = harvested.map { it.meaningKo }
            val finalized = harvested.shuffled().map { w ->
                val dist = allMeanings.filter { it != w.meaningKo }.shuffled().take(3)
                w.copy(distractors = dist)
            }

            // Enrich top 3 words with authentic IPA & real dictionary example via Free Dictionary API
            val enriched = finalized.mapIndexed { index, word ->
                if (index < 3) enrichWithFreeDictionaryApi(word) else word
            }

            DailyHarvestResult(
                newlyAddedWords = enriched,
                sourceLabel = "Datamuse + Free Dictionary API",
                topicLabelKo = "${primaryPair.first.labelKo} · ${secondaryPair.first.labelKo}"
            )
        }.onFailure { err ->
            Log.w(TAG, "Daily free word API harvest failed: ${err.message}")
        }
    }

    private fun enrichWithFreeDictionaryApi(word: VocabWord): VocabWord {
        return runCatching {
            val req = Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/${word.word.lowercase()}")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return word
                val arr = JSONArray(resp.body?.string().orEmpty())
                val first = arr.optJSONObject(0) ?: return word
                val phonetic = first.optString("phonetic").takeIf { it.isNotBlank() } ?: word.ipa

                var dictExample: String? = null
                val meanings = first.optJSONArray("meanings")
                if (meanings != null) {
                    outer@ for (m in 0 until meanings.length()) {
                        val defs = meanings.getJSONObject(m).optJSONArray("definitions") ?: continue
                        for (d in 0 until defs.length()) {
                            val ex = defs.getJSONObject(d).optString("example")
                            if (ex.isNotBlank()) {
                                dictExample = ex
                                break@outer
                            }
                        }
                    }
                }

                val updatedExamples = if (!dictExample.isNullOrBlank()) {
                    listOf(
                        VocabExample(
                            labelKo = "영영사전 실전 예문",
                            english = dictExample,
                            korean = "원어민 사전 예문 — '${word.word}'의 실제 쓰임을 소리 내어 따라 말해보세요.",
                            ipaHint = phonetic
                        )
                    ) + word.examples.drop(1)
                } else {
                    word.examples
                }

                word.copy(
                    ipa = phonetic,
                    exampleEn = updatedExamples.first().english,
                    examples = updatedExamples
                )
            }
        }.getOrDefault(word)
    }

    private fun buildContextualSentences(
        word: String,
        pos: String,
        engDef: String,
        topic: VocabTopic
    ): List<VocabExample> {
        val shortDef = engDef.take(46)
        return when (pos) {
            "v." -> listOf(
                VocabExample("상황 핵심 회화", "Let's $word this carefully before we make our final decision.", "최종 결정을 내리기 전에 이것을 신중하게 '$word($shortDef)' 해보자."),
                VocabExample("수능 독해 지문", "Experts often $word new strategies to solve complex ${topic.labelKo} issues.", "전문가들은 복잡한 문제를 풀기 위해 새 전략을 자주 '$word' 한다."),
                VocabExample("원어민 친구 수다", "Could you show me how to $word that more naturally?", "그걸 좀 더 자연스럽게 '$word' 하는 법 알려줄 수 있어?"),
                VocabExample("왕국 수호 미션", "Our family team will $word every challenge together today!", "우리 가족 팀은 오늘 모든 도전을 함께 '$word' 할 거야!")
            )
            "adj." -> listOf(
                VocabExample("상황 핵심 회화", "Your idea sounds really $word and helpful for our trip!", "네 아이디어가 우리 여행에 정말 '$word($shortDef)' 하고 도움 되겠다!"),
                VocabExample("수능 독해 지문", "The passage highlights why a $word approach leads to better scores.", "지문은 왜 '$word' 접근법이 더 좋은 성적으로 이어지는지 강조한다."),
                VocabExample("원어민 친구 수다", "Honestly, I feel much more $word after practicing with you.", "솔직히 너랑 연습하고 나니까 훨씬 '$word' 느낌이 들어."),
                VocabExample("왕국 수호 미션", "A $word Big-Ttung and Small-Ttung team always wins the seven-mission boss battle!", "'$word' 빅뚱·스몰뚱 팀은 언제나 7회 보스전에서 승리해!")
            )
            else -> listOf(
                VocabExample("상황 핵심 회화", "We talked about this $word during our family dinner last night.", "어젯밤 가족 저녁 식사 때 이 '$word($shortDef)'에 대해 이야기했어."),
                VocabExample("수능 독해 지문", "Understanding the role of $word is essential in modern society.", "현대 사회에서 '$word'의 역할을 이해하는 것은 필수적이다."),
                VocabExample("원어민 친구 수다", "Can you explain this $word using a simple everyday example?", "이 '$word'를 쉬운 일상 예문으로 설명해 줄래?"),
                VocabExample("왕국 수호 미션", "Mastering this $word repairs our emerald castle walls right away!", "이 '$word'를 마스터하면 에메랄드 성벽이 즉시 수복돼!")
            )
        }
    }
}
