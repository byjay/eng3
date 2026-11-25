package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.VocabExample
import com.example.model.VocabTopic
import com.example.model.VocabWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val TAG = "DailyWordHarvester"

data class DailyHarvestResult(
    val newlyAddedWords: List<VocabWord>,
    val sourceLabel: String,
    val topicLabelKo: String,
    val synonymGroupCount: Int = 0
)

data class SynonymClusterSummary(
    val groupId: String,
    val members: List<String>,
    val antonymGroupIds: List<String>
)

/**
 * Deterministic Union-Find Disjoint Set for Synonym & Antonym Clustering (SYN-0001, SYN-0002...).
 * Guarantees identical cluster IDs across Android local fallback and Cloudflare R2 Worker.
 */
internal class SynonymUnionFind {
    private val parent = LinkedHashMap<String, String>()

    fun add(word: String) {
        val norm = normalizeWord(word)
        if (norm.isNotEmpty() && !parent.containsKey(norm)) {
            parent[norm] = norm
        }
    }

    fun find(word: String): String {
        val norm = normalizeWord(word)
        if (norm.isEmpty()) return ""
        add(norm)
        var root = norm
        while (parent[root] != root) {
            root = parent[root] ?: root
        }
        var cur = norm
        while (cur != root) {
            val next = parent[cur] ?: root
            parent[cur] = root
            cur = next
        }
        return root
    }

    fun union(a: String, b: String) {
        val ra = find(a)
        val rb = find(b)
        if (ra.isEmpty() || rb.isEmpty() || ra == rb) return
        if (ra < rb) parent[rb] = ra else parent[ra] = rb
    }

    fun buildClusters(antonymPairs: List<Pair<String, String>>): Pair<Map<String, String>, List<SynonymClusterSummary>> {
        val rootToMembers = LinkedHashMap<String, MutableSet<String>>()
        for (w in parent.keys) {
            val r = find(w)
            rootToMembers.getOrPut(r) { linkedSetOf() }.add(w)
        }

        val sortedRoots = rootToMembers.keys.sorted()
        val wordToGroupId = LinkedHashMap<String, String>()
        val groupAntonyms = LinkedHashMap<String, MutableSet<String>>()
        val groupMembers = LinkedHashMap<String, List<String>>()

        sortedRoots.forEachIndexed { idx, root ->
            val groupId = "SYN-%04d".format(idx + 1)
            val members = rootToMembers[root].orEmpty().sorted()
            groupMembers[groupId] = members
            groupAntonyms[groupId] = linkedSetOf()
            members.forEach { wordToGroupId[it] = groupId }
        }

        for ((rawA, rawB) in antonymPairs) {
            val ga = wordToGroupId[normalizeWord(rawA)] ?: continue
            val gb = wordToGroupId[normalizeWord(rawB)] ?: continue
            if (ga != gb) {
                groupAntonyms[ga]?.add(gb)
                groupAntonyms[gb]?.add(ga)
            }
        }

        val summaries = groupMembers.map { (gid, members) ->
            SynonymClusterSummary(
                groupId = gid,
                members = members,
                antonymGroupIds = groupAntonyms[gid].orEmpty().sorted()
            )
        }
        return wordToGroupId to summaries
    }

    companion object {
        fun normalizeWord(raw: String): String {
            val cleaned = raw.trim().lowercase()
            if (cleaned.length !in 4..20) return ""
            if (!cleaned.all { it in 'a'..'z' }) return ""
            return cleaned
        }
    }
}

/**
 * Strengthened Vocabulary Collector Engine:
 * 1) Primary: Cloudflare R2 Worker sync (GET /api/vocab/latest?limit=50) with Bearer token support
 * 2) Automatic Fallback: Local 3-Free-API Pipeline (Datamuse + Free Dictionary + MyMemory Korean Translation)
 *    with exponential backoff retry, rate-limit (429) defense, duplicate/quality filtering,
 *    and deterministic Union-Find SYN-xxxx synonym & antonym clustering.
 */
object DailyWordHarvesterService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(16, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val mapAdapter by lazy {
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any?>>(type)
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
        preferredTopic: VocabTopic? = null,
        customCloudflareWorkerUrl: String = ""
    ): Result<DailyHarvestResult> = withContext(Dispatchers.IO) {
        // 1. Primary path: Try Cloudflare R2 Worker latest shard sync first!
        val workerBaseUrl = CloudflareLevelSyncService.resolveWorkerUrl(customCloudflareWorkerUrl)
        if (workerBaseUrl.isNotBlank()) {
            val r2Result = fetchLatestFromR2Worker(
                workerBaseUrl = workerBaseUrl,
                existingWordLowerSet = existingWordLowerSet,
                limit = 50
            )
            if (r2Result.isSuccess) {
                val synced = r2Result.getOrNull()
                if (synced != null && synced.newlyAddedWords.isNotEmpty()) {
                    return@withContext Result.success(synced)
                }
            } else {
                Log.w(TAG, "R2 Worker sync fallback to local 3-API pipeline: ${r2Result.exceptionOrNull()?.message}")
            }
        }

        // 2. Fallback path: Local 3 Free APIs (Datamuse + Free Dictionary + MyMemory Korean) + Union-Find Clustering
        harvestFromLocalThreeFreeApis(
            existingWordLowerSet = existingWordLowerSet,
            daySeed = daySeed,
            preferredTopic = preferredTopic
        )
    }

    private suspend fun fetchLatestFromR2Worker(
        workerBaseUrl: String,
        existingWordLowerSet: Set<String>,
        limit: Int
    ): Result<DailyHarvestResult> = runCatching {
        val url = "$workerBaseUrl/api/vocab/latest?limit=$limit"
        val reqBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()

        val token = BuildConfig.CLOUDFLARE_API_TOKEN.trim()
        if (token.isNotBlank() && token != "YOUR_CLOUDFLARE_API_TOKEN") {
            reqBuilder.header("Authorization", "Bearer $token")
        }

        val rawBody = executeGetWithRetry(reqBuilder.build(), maxRetries = 2)
            ?: throw IOException("Cloudflare R2 Worker 응답 없음")

        val parsedWords = parseR2LatestJson(rawBody, existingWordLowerSet)
        if (parsedWords.isEmpty()) {
            throw IllegalStateException("R2 샤드에 신규 미등록 단어가 없습니다.")
        }

        val (clusteredWords, clusters) = applyUnionFindClustering(parsedWords)
        DailyHarvestResult(
            newlyAddedWords = clusteredWords,
            sourceLabel = "Cloudflare R2 샤드 동기화",
            topicLabelKo = "R2 일일 자동누적 · 동의어 ${clusters.size}그룹",
            synonymGroupCount = clusters.size
        )
    }

    private suspend fun harvestFromLocalThreeFreeApis(
        existingWordLowerSet: Set<String>,
        daySeed: Int,
        preferredTopic: VocabTopic?
    ): Result<DailyHarvestResult> = runCatching {
        val primaryPair = if (preferredTopic != null) {
            topicQueryMap.first { it.first == preferredTopic }
        } else {
            topicQueryMap[Math.floorMod(daySeed, topicQueryMap.size)]
        }
        val secondaryPair = topicQueryMap[(topicQueryMap.indexOf(primaryPair) + 1) % topicQueryMap.size]

        val harvested = mutableListOf<VocabWord>()
        val seen = existingWordLowerSet.map { it.lowercase() }.toMutableSet()

        listOf(primaryPair, secondaryPair).forEach { (topic, queryTopics) ->
            val encodedTopics = URLEncoder.encode(queryTopics, "UTF-8")
            val url = "https://api.datamuse.com/words?topics=$encodedTopics&md=dfp&max=65"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()

            val raw = executeGetWithRetry(req, maxRetries = 2) ?: return@forEach
            val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return@forEach

            for (i in 0 until arr.length()) {
                if (harvested.size >= 50) break
                val obj = arr.optJSONObject(i) ?: continue
                val wordText = obj.optString("word").trim()
                val lower = SynonymUnionFind.normalizeWord(wordText)
                if (lower.isEmpty() || lower in seen) continue

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
                    lower.length >= 10 || freqScore < 5.0 -> 3
                    lower.length >= 7 || freqScore < 20.0 -> 2
                    else -> 1
                }

                val sentences = buildContextualSentences(lower, pos, engDef, topic)
                seen.add(lower)
                harvested.add(
                    VocabWord(
                        id = "api_$lower",
                        grade = csatDiff.coerceIn(1, 3),
                        textbookUnitKo = "3종API 일일수집 · ${topic.labelKo}",
                        csatFrequency = csatFreq,
                        csatDifficulty = csatDiff,
                        topic = topic,
                        word = lower,
                        ipa = "/$lower/",
                        partOfSpeech = pos,
                        meaningKo = "${topic.labelKo} · ${engDef.take(52)}",
                        exampleEn = sentences.first().english,
                        exampleKo = sentences.first().korean,
                        examples = sentences,
                        pronunciationTipKo = "강세를 살려 '$lower'를 또박또박 읽고 예문을 소리 내어 따라 말해보세요.",
                        distractors = emptyList()
                    )
                )
            }
        }

        if (harvested.isEmpty()) {
            throw IllegalStateException("새로 수집할 단어가 없거나 네트워크가 오프라인입니다.")
        }

        // Enrich top 8 words with authentic IPA, real dictionary examples, synonyms/antonyms, and Korean meaning via MyMemory!
        val enriched = harvested.shuffled().mapIndexed { index, word ->
            if (index < 8) {
                enrichWordWithDictSynAndKorean(word)
            } else {
                word
            }
        }

        // Fill realistic non-ABC distractors after Korean translations are applied
        val allMeanings = enriched.map { it.meaningKo }
        val withDistractors = enriched.map { w ->
            val dist = allMeanings.filter { it != w.meaningKo }.shuffled().take(3)
            w.copy(distractors = dist)
        }

        val (clusteredWords, clusters) = applyUnionFindClustering(withDistractors)

        DailyHarvestResult(
            newlyAddedWords = clusteredWords,
            sourceLabel = "Datamuse + FreeDict + MyMemory(한글)",
            topicLabelKo = "${primaryPair.first.labelKo} · ${secondaryPair.first.labelKo}",
            synonymGroupCount = clusters.size
        )
    }.onFailure { err ->
        Log.w(TAG, "Daily free word API harvest failed: ${err.message}")
    }

    private suspend fun enrichWordWithDictSynAndKorean(word: VocabWord): VocabWord {
        var updated = word
        val synSet = linkedSetOf<String>()
        val antSet = linkedSetOf<String>()

        // 1) Free Dictionary API (IPA, native example, synonyms/antonyms)
        val dictRaw = executeGetWithRetry(
            Request.Builder().url("https://api.dictionaryapi.dev/api/v2/entries/en/${word.word}").get().build(),
            maxRetries = 1
        )
        if (!dictRaw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(dictRaw)
                val first = arr.optJSONObject(0)
                if (first != null) {
                    val phonetic = first.optString("phonetic").takeIf { it.isNotBlank() } ?: updated.ipa
                    var dictExample: String? = null
                    val meanings = first.optJSONArray("meanings")
                    if (meanings != null) {
                        for (m in 0 until meanings.length()) {
                            val mObj = meanings.getJSONObject(m)
                            mObj.optJSONArray("synonyms")?.let { sArr ->
                                for (k in 0 until sArr.length()) {
                                    val ns = SynonymUnionFind.normalizeWord(sArr.optString(k))
                                    if (ns.isNotEmpty() && ns != word.word) synSet.add(ns)
                                }
                            }
                            mObj.optJSONArray("antonyms")?.let { aArr ->
                                for (k in 0 until aArr.length()) {
                                    val na = SynonymUnionFind.normalizeWord(aArr.optString(k))
                                    if (na.isNotEmpty() && na != word.word) antSet.add(na)
                                }
                            }
                            val defs = mObj.optJSONArray("definitions") ?: continue
                            for (d in 0 until defs.length()) {
                                val ex = defs.getJSONObject(d).optString("example")
                                if (dictExample == null && ex.isNotBlank()) dictExample = ex
                            }
                        }
                    }

                    val updatedExamples = if (!dictExample.isNullOrBlank()) {
                        listOf(
                            VocabExample(
                                labelKo = "영영사전 실전 예문",
                                english = dictExample,
                                korean = "원어민 사전 실전 예문 — '${word.word}'의 쓰임을 소리 내어 따라 말해보세요.",
                                ipaHint = phonetic
                            )
                        ) + updated.examples.drop(1)
                    } else {
                        updated.examples
                    }

                    updated = updated.copy(
                        ipa = phonetic,
                        exampleEn = updatedExamples.first().english,
                        examples = updatedExamples
                    )
                }
            }
        }

        // 2) Datamuse Synonym & Antonym cross-fetch
        val synRaw = executeGetWithRetry(
            Request.Builder().url("https://api.datamuse.com/words?rel_syn=${word.word}&max=6").get().build(),
            maxRetries = 1
        )
        if (!synRaw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(synRaw)
                for (i in 0 until arr.length()) {
                    val ns = SynonymUnionFind.normalizeWord(arr.optJSONObject(i)?.optString("word").orEmpty())
                    if (ns.isNotEmpty() && ns != word.word) synSet.add(ns)
                }
            }
        }
        val antRaw = executeGetWithRetry(
            Request.Builder().url("https://api.datamuse.com/words?rel_ant=${word.word}&max=4").get().build(),
            maxRetries = 1
        )
        if (!antRaw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(antRaw)
                for (i in 0 until arr.length()) {
                    val na = SynonymUnionFind.normalizeWord(arr.optJSONObject(i)?.optString("word").orEmpty())
                    if (na.isNotEmpty() && na != word.word) antSet.add(na)
                }
            }
        }

        // 3) MyMemory Korean Translation (meaning + first example)
        val koMeaning = translateEnglishToKoreanSafe(word.word)
        val koExample = translateEnglishToKoreanSafe(updated.exampleEn)
        val finalMeaning = if (koMeaning.isNotBlank()) koMeaning else updated.meaningKo
        val finalExamples = if (koExample.isNotBlank() && updated.examples.isNotEmpty()) {
            listOf(updated.examples.first().copy(korean = koExample)) + updated.examples.drop(1)
        } else {
            updated.examples
        }

        return updated.copy(
            meaningKo = finalMeaning,
            exampleKo = finalExamples.firstOrNull()?.korean ?: updated.exampleKo,
            examples = finalExamples,
            synonyms = synSet.take(6),
            antonyms = antSet.take(4)
        )
    }

    private suspend fun translateEnglishToKoreanSafe(englishText: String): String {
        val clipped = englishText.trim().take(160)
        if (clipped.length < 2) return ""
        val encoded = URLEncoder.encode(clipped, "UTF-8")
        val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=en|ko"
        val raw = executeGetWithRetry(Request.Builder().url(url).get().build(), maxRetries = 1) ?: return ""
        return parseMyMemoryTranslation(raw)
    }

    private suspend fun executeGetWithRetry(request: Request, maxRetries: Int): String? {
        var attempt = 0
        while (attempt <= maxRetries) {
            try {
                httpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        return resp.body?.string()
                    }
                    if (resp.code == 429 || resp.code >= 500) {
                        if (attempt < maxRetries) {
                            delay(420L * (attempt + 1))
                        }
                    } else {
                        return null
                    }
                }
            } catch (_: IOException) {
                if (attempt < maxRetries) {
                    delay(380L * (attempt + 1))
                }
            }
            attempt += 1
        }
        return null
    }

    // === Pure, Testable Validation & Parsing Functions ===

    internal fun parseMyMemoryTranslation(rawJson: String): String {
        return runCatching {
            val root = mapAdapter.fromJson(rawJson) ?: return ""
            val responseData = root["responseData"] as? Map<*, *> ?: return ""
            val translated = (responseData["translatedText"] as? String)
                .orEmpty()
                .replace("&#39;", "'")
                .replace("&quot;", "\"")
                .trim()
            if (translated.isBlank() ||
                translated.contains("MYMEMORY WARNING", ignoreCase = true) ||
                translated.contains("QUERY LENGTH LIMIT", ignoreCase = true)
            ) {
                ""
            } else {
                translated
            }
        }.getOrDefault("")
    }

    internal fun parseR2LatestJson(
        rawJson: String,
        existingWordLowerSet: Set<String>
    ): List<VocabWord> {
        val seen = existingWordLowerSet.map { it.lowercase() }.toMutableSet()
        val result = mutableListOf<VocabWord>()
        val root = runCatching { mapAdapter.fromJson(rawJson) }.getOrNull() ?: return emptyList()
        val arr = root["words"] as? List<*> ?: return emptyList()

        for (item in arr) {
            val obj = item as? Map<*, *> ?: continue
            val wordText = SynonymUnionFind.normalizeWord(obj["word"] as? String ?: "")
            if (wordText.isEmpty() || wordText in seen) continue

            val meaningKo = (obj["meaningKo"] as? String)?.trim().orEmpty()
                .ifBlank { (obj["englishDef"] as? String)?.trim().orEmpty() }
            if (meaningKo.isBlank()) continue

            val topicName = (obj["topic"] as? String)?.trim().orEmpty()
            val topic = VocabTopic.entries.firstOrNull { it.name == topicName } ?: VocabTopic.SCHOOL_EXAM
            val pos = (obj["partOfSpeech"] as? String)?.trim().orEmpty().ifBlank { "n." }
            val ipa = (obj["ipa"] as? String)?.trim().orEmpty().ifBlank { "/$wordText/" }
            val grade = ((obj["grade"] as? Number)?.toInt() ?: 2).coerceIn(1, 3)
            val csatFreq = ((obj["csatFrequency"] as? Number)?.toInt() ?: 4).coerceIn(3, 5)
            val csatDiff = ((obj["csatDifficulty"] as? Number)?.toInt() ?: grade).coerceIn(1, 3)

            val examples = mutableListOf<VocabExample>()
            (obj["examples"] as? List<*>)?.forEach { exItem ->
                val exObj = exItem as? Map<*, *> ?: return@forEach
                val en = (exObj["english"] as? String)?.trim().orEmpty()
                val ko = (exObj["korean"] as? String)?.trim().orEmpty()
                if (en.isNotBlank()) {
                    examples.add(
                        VocabExample(
                            labelKo = (exObj["labelKo"] as? String)?.trim().orEmpty().ifBlank { "R2 실전 예문" },
                            english = en,
                            korean = ko.ifBlank { "'$wordText' 실전 문장 연습" },
                            ipaHint = (exObj["ipaHint"] as? String)?.trim().orEmpty().ifBlank { ipa }
                        )
                    )
                }
            }
            if (examples.isEmpty()) {
                val fallbackEn = (obj["exampleEn"] as? String)?.trim().orEmpty().ifBlank {
                    "We practiced '$wordText' in a real sentence today."
                }
                val fallbackKo = (obj["exampleKo"] as? String)?.trim().orEmpty().ifBlank {
                    "오늘 실전 문장에서 '$wordText'($meaningKo)를 연습했어요."
                }
                examples.add(VocabExample("R2 실전 예문", fallbackEn, fallbackKo, ipa))
            }

            val synonyms = mutableListOf<String>()
            (obj["synonyms"] as? List<*>)?.forEach { sItem ->
                val ns = SynonymUnionFind.normalizeWord(sItem as? String ?: "")
                if (ns.isNotEmpty() && ns != wordText) synonyms.add(ns)
            }
            val antonyms = mutableListOf<String>()
            (obj["antonyms"] as? List<*>)?.forEach { aItem ->
                val na = SynonymUnionFind.normalizeWord(aItem as? String ?: "")
                if (na.isNotEmpty() && na != wordText) antonyms.add(na)
            }

            seen.add(wordText)
            result.add(
                VocabWord(
                    id = (obj["id"] as? String)?.trim().orEmpty().ifBlank { "r2_$wordText" },
                    grade = grade,
                    textbookUnitKo = "Cloudflare R2 자동수집 · ${topic.labelKo}",
                    csatFrequency = csatFreq,
                    csatDifficulty = csatDiff,
                    topic = topic,
                    word = wordText,
                    ipa = ipa,
                    partOfSpeech = pos,
                    meaningKo = meaningKo,
                    exampleEn = examples.first().english,
                    exampleKo = examples.first().korean,
                    examples = examples,
                    pronunciationTipKo = "R2 자동 수집 어휘 — '$wordText' 강세를 살려 예문과 함께 말해 보세요.",
                    distractors = emptyList(),
                    synonymGroupId = (obj["synGroupId"] as? String)?.trim().orEmpty(),
                    synonyms = synonyms.distinct().take(6),
                    antonyms = antonyms.distinct().take(4)
                )
            )
        }

        val allMeanings = result.map { it.meaningKo }
        return result.map { w ->
            w.copy(distractors = allMeanings.filter { it != w.meaningKo }.take(3))
        }
    }

    internal fun applyUnionFindClustering(words: List<VocabWord>): Pair<List<VocabWord>, List<SynonymClusterSummary>> {
        val uf = SynonymUnionFind()
        val antonymPairs = mutableListOf<Pair<String, String>>()

        words.forEach { w ->
            uf.add(w.word)
            w.synonyms.forEach { syn ->
                uf.union(w.word, syn)
            }
            w.antonyms.forEach { ant ->
                uf.add(ant)
                antonymPairs.add(w.word to ant)
            }
        }

        val (wordToGroupId, clusters) = uf.buildClusters(antonymPairs)
        val stamped = words.map { w ->
            val resolvedId = w.synonymGroupId.ifBlank { wordToGroupId[SynonymUnionFind.normalizeWord(w.word)].orEmpty() }
            w.copy(synonymGroupId = resolvedId)
        }
        return stamped to clusters
    }

    internal fun replenishQueueIfLow(
        currentQueue: List<String>,
        existingWords: Set<String>,
        discoveredRelatedWords: List<String>,
        minThreshold: Int = 50
    ): List<String> {
        val existingNorm = existingWords.mapNotNull { SynonymUnionFind.normalizeWord(it).takeIf(String::isNotEmpty) }.toSet()
        val queueSet = LinkedHashSet<String>()
        currentQueue.forEach { raw ->
            val nw = SynonymUnionFind.normalizeWord(raw)
            if (nw.isNotEmpty() && nw !in existingNorm) queueSet.add(nw)
        }
        if (queueSet.size < minThreshold) {
            discoveredRelatedWords.forEach { raw ->
                val nw = SynonymUnionFind.normalizeWord(raw)
                if (nw.isNotEmpty() && nw !in existingNorm) queueSet.add(nw)
            }
        }
        return queueSet.toList()
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
                VocabExample("실전 스피킹", "Our team will $word every challenge together today!", "우리 팀은 오늘 모든 도전을 함께 '$word' 할 거야!")
            )
            "adj." -> listOf(
                VocabExample("상황 핵심 회화", "Your idea sounds really $word and helpful for our trip!", "네 아이디어가 우리 여행에 정말 '$word($shortDef)' 하고 도움 되겠다!"),
                VocabExample("수능 독해 지문", "The passage highlights why a $word approach leads to better scores.", "지문은 왜 '$word' 접근법이 더 좋은 성적으로 이어지는지 강조한다."),
                VocabExample("원어민 친구 수다", "Honestly, I feel much more $word after practicing with you.", "솔직히 너랑 연습하고 나니까 훨씬 '$word' 느낌이 들어."),
                VocabExample("실전 스피킹", "Staying $word helps us solve difficult problems calmly!", "'$word' 상태를 유지하면 어려운 문제도 차분히 풀 수 있어!")
            )
            else -> listOf(
                VocabExample("상황 핵심 회화", "We talked about this $word during dinner last night.", "어젯밤 저녁 식사 때 이 '$word($shortDef)'에 대해 이야기했어."),
                VocabExample("수능 독해 지문", "Understanding the role of $word is essential in modern society.", "현대 사회에서 '$word'의 역할을 이해하는 것은 필수적이다."),
                VocabExample("원어민 친구 수다", "Can you explain this $word using a simple everyday example?", "이 '$word'를 쉬운 일상 예문으로 설명해 줄래?"),
                VocabExample("실전 스피킹", "Mastering this $word makes reading English much easier!", "이 '$word'를 마스터하면 영어 읽기가 훨씬 쉬워져!")
            )
        }
    }
}
