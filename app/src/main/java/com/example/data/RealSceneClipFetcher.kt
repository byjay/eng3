package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor

data class RealDialogueClip(
    val id: String,
    val mp4Url: String,
    val dialogueLineEn: String,
    val sourceTitle: String,
    val providerLabel: String
)

data class YouTubeDialogueMatch(
    val videoId: String,
    val title: String,
    val channelName: String,
    val durationLabel: String,
    val thumbnailUrl: String,
    val matchedDialogueEn: String,
    val contextBeforeEn: String = "",
    val contextAfterEn: String = "",
    val startSeconds: Int = 0,
    val endSeconds: Int = 0,
    val rawStartSec: Double = 0.0,
    val rawEndSec: Double = 0.0,
    val isExactCaptionMatch: Boolean = false,
    val fromCache: Boolean = false,
    val captionTypeLabel: String = "YouTube",
    val sourceBadge: String = "YouTube 자막",
    val rateLimited: Boolean = false
) {
    val formattedTimestamp: String
        get() = formatSec(startSeconds)

    val formattedWindow: String
        get() = if (endSeconds > startSeconds) {
            "${formatSec(startSeconds)} ~ ${formatSec(endSeconds)}"
        } else {
            formatSec(startSeconds)
        }

    val watchUrlWithTimestamp: String
        get() = if (startSeconds > 0) {
            "https://www.youtube.com/watch?v=$videoId&t=${startSeconds}s"
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }

    private fun formatSec(totalSec: Int): String {
        val safe = totalSec.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return "%02d:%02d".format(mins, secs)
    }
}

private data class CaptionSegment(
    val startSec: Double,
    val durationSec: Double,
    val text: String
) {
    val endSec: Double get() = startSec + durationSec.coerceAtLeast(1.4)
}

private data class CachedTranscript(
    val segments: List<CaptionSegment>,
    val captionTypeLabel: String,
    val savedAtMs: Long,
    val fromDiskOrMemoryCache: Boolean
)

private data class YouTubeVideoCandidate(
    val videoId: String,
    val title: String,
    val channelName: String,
    val durationLabel: String
)

private data class HttpResult(
    val body: String?,
    val statusCode: Int
)

/**
 * Lightweight Kotlin YouTube Lecture & Dialogue Segment Engine:
 * - Distills core algorithms from Youtube-Word-Finder / yttr-search / find-youtube without heavy deps
 * - Stage 1: Full-transcript fragment merging with character-offset reverse timestamp mapping
 * - Defensive buffers: [max(0, rawStart - 1.0s) .. rawEnd + 1.5s] so spoken phrases never clip
 * - 24h local disk + memory JSON caption cache to prevent YouTube HTTP 429 IP throttling
 * - Graceful fallback chain: Manual EN -> Auto-generated ASR (a.en) -> Watch captionTracks -> Filmot / YouGlish
 */
object RealSceneClipFetcher {

    private const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    private const val MAX_CACHE_FILES = 80
    private const val BUFFER_BEFORE_SEC = 1.0
    private const val BUFFER_AFTER_SEC = 1.5
    private const val RATE_LIMIT_COOLDOWN_MS = 90_000L

    private val memoryTranscriptCache = ConcurrentHashMap<String, CachedTranscript>()
    @Volatile
    private var rateLimitedUntilMs: Long = 0L

    private val uuidRegex = Regex("""/yarn-clip/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})""", RegexOption.IGNORE_CASE)
    private val transcriptRegex = Regex("""class="[^"]*transcript[^"]*"[^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ogDescRegex = Regex("""<meta[^>]+property=["']og:description["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val ogTitleRegex = Regex("""<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val htmlTagRegex = Regex("""<[^>]+>""")

    // YouTube XML caption patterns preserving both start and duration
    private val timedTextTagRegex = Regex("""<text([^>]*)>(.*?)</text>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val srv3TagRegex = Regex("""<p([^>]*)>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val startAttrRegex = Regex("""start="([0-9.]+)"""")
    private val durAttrRegex = Regex("""dur="([0-9.]+)"""")
    private val tAttrRegex = Regex("""t="([0-9]+)"""")
    private val dAttrRegex = Regex("""d="([0-9]+)"""")

    private val videoRendererIdRegex = Regex(""""videoRenderer":\{"videoId":"([a-zA-Z0-9_-]{11})"""")
    private val titleRunsRegex = Regex(""""title":\{"runs":\[\{"text":"((?:\\.|[^"\\])*)"""")
    private val ownerRunsRegex = Regex(""""ownerText":\{"runs":\[\{"text":"((?:\\.|[^"\\])*)"""")
    private val lengthSimpleRegex = Regex(""""lengthText":\{[^}]*"simpleText":"([^"]+)"""")
    private val captionTrackUrlRegex = Regex(""""captionTracks":\[(.*?)\]""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val baseUrlRegex = Regex(""""baseUrl":"((?:\\.|[^"\\])+)"""")

    private val stopWords = setOf(
        "the", "and", "for", "with", "that", "this", "your", "have", "from", "just",
        "very", "into", "about", "there", "here", "what", "when", "where", "which",
        "will", "would", "could", "should", "they", "them", "their", "then", "than",
        "are", "was", "were", "been", "being", "does", "did", "done", "you", "our", "out"
    )

    fun isCurrentlyRateLimited(): Boolean = System.currentTimeMillis() < rateLimitedUntilMs

    fun buildFilmotSearchUrl(rawQuery: String): String {
        val clean = sanitizeQuery(rawQuery).ifBlank { "hello" }
        val encoded = URLEncoder.encode("\"$clean\"", "UTF-8")
        return "https://filmot.com/search/$encoded/1?lang=en"
    }

    fun sanitizeQuery(raw: String): String {
        return raw.trim()
            .replace(Regex("[^a-zA-Z0-9\\s'-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(60)
    }

    /**
     * Converts a long conversational sentence into a crisp, high-hit-rate YouTube dialogue search phrase
     * (2~4 words) while keeping short words/idioms untouched.
     */
    fun extractBestSearchPhrase(raw: String): String {
        val cleaned = sanitizeQuery(raw)
        if (cleaned.isBlank()) return "how are you"
        val words = cleaned.split(" ").filter { it.isNotBlank() }
        if (words.size <= 4) return cleaned

        val clauses = raw.split(Regex("[.!?\\n]+"))
            .map { sanitizeQuery(it) }
            .filter { it.split(" ").size in 2..5 }
        if (clauses.isNotEmpty()) {
            return clauses.last()
        }

        val anchorIdx = words.indices
            .filter { words[it].lowercase() !in stopWords && words[it].length >= 4 }
            .maxByOrNull { words[it].length }
            ?: 0
        val start = (anchorIdx - 1).coerceAtLeast(0)
        val end = (start + 3).coerceAtMost(words.size)
        return words.subList(start, end).joinToString(" ")
    }

    /**
     * Searches real YouTube English lecture & conversation videos with subtitles,
     * checks 24h local JSON transcript cache first, merges fragmented captions,
     * and returns buffered [start - 1.0s .. end + 1.5s] segments ready for IFrame A-B looping.
     */
    suspend fun searchYouTubeDialogues(
        rawQuery: String,
        cacheDir: File? = null
    ): List<YouTubeDialogueMatch> = withContext(Dispatchers.IO) {
        val clean = sanitizeQuery(rawQuery)
        if (clean.isBlank()) return@withContext emptyList()

        // 1. Live YouTube Search with CC/Subtitles filter (`sp=EgIoAQ%3D%3D`) targeting lectures & conversations
        val ccCandidates = runCatching {
            fetchYouTubeSearchCandidates("\"$clean\" english lesson conversation", subtitlesOnly = true)
        }.getOrDefault(emptyList())

        val broadCandidates = if (ccCandidates.size < 4) {
            runCatching {
                fetchYouTubeSearchCandidates("$clean english dialogue example", subtitlesOnly = false)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val pipedCandidates = if (ccCandidates.isEmpty() && broadCandidates.isEmpty()) {
            runCatching { fetchPipedSearchCandidates("$clean english lesson") }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val mergedCandidates = (ccCandidates + broadCandidates + pipedCandidates)
            .distinctBy { it.videoId }
            .take(7)

        if (mergedCandidates.isEmpty()) {
            return@withContext emptyList()
        }

        // 2. Fetch or read cached English transcripts in parallel and run fragment-merged matching
        val enriched = coroutineScope {
            mergedCandidates.take(6).map { candidate ->
                async {
                    val captionMatch = withTimeoutOrNull(4800L) {
                        findMatchingCaptionInVideo(candidate.videoId, clean, cacheDir)
                    }
                    if (captionMatch != null) {
                        YouTubeDialogueMatch(
                            videoId = candidate.videoId,
                            title = candidate.title,
                            channelName = candidate.channelName,
                            durationLabel = candidate.durationLabel,
                            thumbnailUrl = "https://i.ytimg.com/vi/${candidate.videoId}/hqdefault.jpg",
                            matchedDialogueEn = captionMatch.matchedLine,
                            contextBeforeEn = captionMatch.beforeLine,
                            contextAfterEn = captionMatch.afterLine,
                            startSeconds = captionMatch.bufferedStartSec,
                            endSeconds = captionMatch.bufferedEndSec,
                            rawStartSec = captionMatch.rawStartSec,
                            rawEndSec = captionMatch.rawEndSec,
                            isExactCaptionMatch = true,
                            fromCache = captionMatch.fromCache,
                            captionTypeLabel = captionMatch.captionTypeLabel,
                            sourceBadge = if (captionMatch.fromCache) "캐시 · ${captionMatch.captionTypeLabel}" else captionMatch.captionTypeLabel,
                            rateLimited = isCurrentlyRateLimited()
                        )
                    } else {
                        YouTubeDialogueMatch(
                            videoId = candidate.videoId,
                            title = candidate.title,
                            channelName = candidate.channelName,
                            durationLabel = candidate.durationLabel,
                            thumbnailUrl = "https://i.ytimg.com/vi/${candidate.videoId}/hqdefault.jpg",
                            matchedDialogueEn = "\"$clean\" — ${candidate.title}",
                            startSeconds = 0,
                            endSeconds = 12,
                            isExactCaptionMatch = false,
                            fromCache = false,
                            captionTypeLabel = "자막 미제공",
                            sourceBadge = "YouTube 영상",
                            rateLimited = isCurrentlyRateLimited()
                        )
                    }
                }
            }.awaitAll()
        }

        enriched.sortedByDescending { it.isExactCaptionMatch }
    }

    private data class CaptionMatchResult(
        val bufferedStartSec: Int,
        val bufferedEndSec: Int,
        val rawStartSec: Double,
        val rawEndSec: Double,
        val matchedLine: String,
        val beforeLine: String,
        val afterLine: String,
        val captionTypeLabel: String,
        val fromCache: Boolean
    )

    private data class SegmentCharSpan(
        val segmentIndex: Int,
        val startChar: Int,
        val endChar: Int
    )

    /**
     * Core Fragment-Merged Caption Matching Algorithm:
     * Joins all caption fragments with spaces while maintaining exact character-offset spans,
     * finds the phrase even when split across 2~4 timestamps, reverse-maps the start & end
     * segments, and applies [start - 1.0s .. end + 1.5s] buffers.
     */
    private fun findMatchingCaptionInVideo(
        videoId: String,
        query: String,
        cacheDir: File?
    ): CaptionMatchResult? {
        val transcript = loadOrFetchEnglishTranscript(videoId, cacheDir) ?: return null
        val segments = transcript.segments
        if (segments.isEmpty()) return null

        val mergedBuilder = StringBuilder()
        val spans = ArrayList<SegmentCharSpan>(segments.size)
        segments.forEachIndexed { idx, seg ->
            if (mergedBuilder.isNotEmpty()) mergedBuilder.append(' ')
            val startChar = mergedBuilder.length
            mergedBuilder.append(seg.text)
            val endChar = mergedBuilder.length
            spans.add(SegmentCharSpan(idx, startChar, endChar))
        }
        val mergedLower = mergedBuilder.toString().lowercase()
        val lowerQuery = query.lowercase().trim()

        // Pass 1: Whole-word / exact-phrase match across the ENTIRE merged transcript
        val boundaryRegex = runCatching {
            Regex("""\b${Regex.escape(lowerQuery)}\b""")
        }.getOrNull()
        val boundaryMatch = boundaryRegex?.find(mergedLower)
        if (boundaryMatch != null) {
            return buildMergedResult(
                segments = segments,
                spans = spans,
                matchCharStart = boundaryMatch.range.first,
                matchCharEnd = boundaryMatch.range.last + 1,
                transcript = transcript
            )
        }

        // Pass 2: Substring match across merged transcript (handles plural/inflected forms)
        val rawIdx = mergedLower.indexOf(lowerQuery)
        if (rawIdx >= 0) {
            return buildMergedResult(
                segments = segments,
                spans = spans,
                matchCharStart = rawIdx,
                matchCharEnd = rawIdx + lowerQuery.length,
                transcript = transcript
            )
        }

        // Pass 3: Strongest multi-token or distinctive content word match
        val queryTokens = lowerQuery.split(" ")
            .map { it.replace(Regex("[^a-z0-9']"), "") }
            .filter { it.length >= 2 }
        val distinctiveTokens = queryTokens.filter { it !in stopWords && it.length >= 3 }
            .ifEmpty { queryTokens }
        if (distinctiveTokens.isEmpty()) return null

        var bestIdx = -1
        var bestScore = 0
        segments.forEachIndexed { idx, seg ->
            val lowerLine = seg.text.lowercase()
            val hits = distinctiveTokens.count { token -> lowerLine.contains(token) }
            if (hits > bestScore) {
                bestScore = hits
                bestIdx = idx
            }
        }
        val requiredHits = if (distinctiveTokens.size >= 2) 2 else 1
        return if (bestIdx >= 0 && bestScore >= requiredHits) {
            buildRangeResult(segments, bestIdx, bestIdx, transcript)
        } else {
            null
        }
    }

    private fun buildMergedResult(
        segments: List<CaptionSegment>,
        spans: List<SegmentCharSpan>,
        matchCharStart: Int,
        matchCharEnd: Int,
        transcript: CachedTranscript
    ): CaptionMatchResult {
        val firstSegIdx = spans.firstOrNull { it.endChar > matchCharStart }?.segmentIndex ?: 0
        val lastSegIdx = spans.lastOrNull { it.startChar < matchCharEnd }?.segmentIndex
            ?.coerceAtLeast(firstSegIdx) ?: firstSegIdx
        return buildRangeResult(segments, firstSegIdx, lastSegIdx, transcript)
    }

    private fun buildRangeResult(
        segments: List<CaptionSegment>,
        firstIdx: Int,
        lastIdx: Int,
        transcript: CachedTranscript
    ): CaptionMatchResult {
        val rawStart = segments[firstIdx].startSec
        val rawEnd = segments[lastIdx].endSec.coerceAtLeast(rawStart + 2.0)
        val bufferedStart = floor((rawStart - BUFFER_BEFORE_SEC).coerceAtLeast(0.0)).toInt()
        val bufferedEnd = ceil((rawEnd + BUFFER_AFTER_SEC).coerceAtLeast(bufferedStart + 3.5)).toInt()
        val joinedLine = segments.subList(firstIdx, (lastIdx + 1).coerceAtMost(segments.size))
            .joinToString(" ") { it.text }
            .trim()
        return CaptionMatchResult(
            bufferedStartSec = bufferedStart,
            bufferedEndSec = bufferedEnd,
            rawStartSec = rawStart,
            rawEndSec = rawEnd,
            matchedLine = joinedLine,
            beforeLine = segments.getOrNull(firstIdx - 1)?.text.orEmpty(),
            afterLine = segments.getOrNull(lastIdx + 1)?.text.orEmpty(),
            captionTypeLabel = transcript.captionTypeLabel,
            fromCache = transcript.fromDiskOrMemoryCache
        )
    }

    private fun loadOrFetchEnglishTranscript(videoId: String, cacheDir: File?): CachedTranscript? {
        val now = System.currentTimeMillis()
        // 1. In-memory cache
        memoryTranscriptCache[videoId]?.let { cached ->
            if (now - cached.savedAtMs < CACHE_TTL_MS) {
                return cached.copy(fromDiskOrMemoryCache = true)
            }
        }
        // 2. Local disk JSON cache (Anti-HTTP 429)
        readDiskTranscriptCache(videoId, cacheDir, now)?.let { diskCached ->
            memoryTranscriptCache[videoId] = diskCached
            return diskCached
        }

        // If YouTube recently returned HTTP 429, avoid hammering the endpoint during cooldown
        if (now < rateLimitedUntilMs) return null

        // 3. Attempt 1: Manual English captions
        val manualRes = httpGetWithStatus("https://www.youtube.com/api/timedtext?v=$videoId&lang=en")
        if (manualRes.statusCode == 429) {
            rateLimitedUntilMs = now + RATE_LIMIT_COOLDOWN_MS
            return null
        }
        parseCaptionXml(manualRes.body)?.let { segs ->
            if (segs.isNotEmpty()) {
                val fresh = CachedTranscript(segs, "수동 EN", now, false)
                saveTranscriptCache(videoId, fresh, cacheDir)
                return fresh
            }
        }

        // 4. Attempt 2: Auto-generated ASR English captions (`a.en` / `kind=asr`)
        val asrRes = httpGetWithStatus("https://www.youtube.com/api/timedtext?v=$videoId&lang=en&kind=asr")
        if (asrRes.statusCode == 429) {
            rateLimitedUntilMs = now + RATE_LIMIT_COOLDOWN_MS
            return null
        }
        parseCaptionXml(asrRes.body)?.let { segs ->
            if (segs.isNotEmpty()) {
                val fresh = CachedTranscript(segs, "자동 a.en", now, false)
                saveTranscriptCache(videoId, fresh, cacheDir)
                return fresh
            }
        }

        // 5. Attempt 3: Watch page signed captionTracks fallback
        val watchRes = httpGetWithStatus(
            urlStr = "https://www.youtube.com/watch?v=$videoId&hl=en&bpctr=9999999999",
            extraHeaders = youtubeHeaders()
        )
        if (watchRes.statusCode == 429) {
            rateLimitedUntilMs = now + RATE_LIMIT_COOLDOWN_MS
            return null
        }
        val watchHtml = watchRes.body ?: return null
        val tracksBlock = captionTrackUrlRegex.find(watchHtml)?.groupValues?.getOrNull(1) ?: return null
        val trackUrls = baseUrlRegex.findAll(tracksBlock)
            .map { unescapeJsonString(it.groupValues[1]) }
            .filter { it.startsWith("http") }
            .toList()

        val preferredTrack = trackUrls.firstOrNull { it.contains("lang=en") && !it.contains("kind=asr") }
            ?: trackUrls.firstOrNull { it.contains("lang=en") }
            ?: trackUrls.firstOrNull()
        if (preferredTrack != null) {
            val xmlRes = httpGetWithStatus(preferredTrack, extraHeaders = youtubeHeaders())
            if (xmlRes.statusCode == 429) {
                rateLimitedUntilMs = now + RATE_LIMIT_COOLDOWN_MS
                return null
            }
            parseCaptionXml(xmlRes.body)?.let { segs ->
                if (segs.isNotEmpty()) {
                    val label = if (preferredTrack.contains("kind=asr")) "자동 a.en" else "수동 EN"
                    val fresh = CachedTranscript(segs, label, now, false)
                    saveTranscriptCache(videoId, fresh, cacheDir)
                    return fresh
                }
            }
        }
        return null
    }

    private fun readDiskTranscriptCache(videoId: String, cacheDir: File?, nowMs: Long): CachedTranscript? {
        if (cacheDir == null) return null
        val file = File(File(cacheDir, "yt_transcript_cache"), "$videoId.json")
        if (!file.exists()) return null
        return runCatching {
            val root = JSONObject(file.readText())
            val savedAt = root.optLong("savedAtMs", 0L)
            if (nowMs - savedAt > CACHE_TTL_MS) {
                file.delete()
                return null
            }
            val label = root.optString("captionType", "수동 EN")
            val arr = root.optJSONArray("segments") ?: return null
            val segs = ArrayList<CaptionSegment>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val text = obj.optString("t").trim()
                if (text.isNotBlank()) {
                    segs.add(
                        CaptionSegment(
                            startSec = obj.optDouble("s", 0.0),
                            durationSec = obj.optDouble("d", 2.0),
                            text = text
                        )
                    )
                }
            }
            if (segs.isEmpty()) null else CachedTranscript(segs, label, savedAt, true)
        }.getOrNull()
    }

    private fun saveTranscriptCache(videoId: String, transcript: CachedTranscript, cacheDir: File?) {
        memoryTranscriptCache[videoId] = transcript
        if (cacheDir == null) return
        runCatching {
            val dir = File(cacheDir, "yt_transcript_cache")
            if (!dir.exists()) dir.mkdirs()
            val arr = JSONArray()
            transcript.segments.forEach { seg ->
                arr.put(
                    JSONObject().apply {
                        put("s", seg.startSec)
                        put("d", seg.durationSec)
                        put("t", seg.text)
                    }
                )
            }
            val root = JSONObject().apply {
                put("savedAtMs", transcript.savedAtMs)
                put("captionType", transcript.captionTypeLabel)
                put("segments", arr)
            }
            File(dir, "$videoId.json").writeText(root.toString())

            // Simple LRU eviction when cache exceeds MAX_CACHE_FILES
            val files = dir.listFiles()
            if (files != null && files.size > MAX_CACHE_FILES) {
                files.sortedBy { it.lastModified() }
                    .take(files.size - MAX_CACHE_FILES)
                    .forEach { it.delete() }
            }
        }
    }

    private fun parseCaptionXml(xml: String?): List<CaptionSegment>? {
        if (xml.isNullOrBlank()) return null
        val list = mutableListOf<CaptionSegment>()

        // Classic timedtext format: <text start="12.34" dur="2.1">Hello world</text>
        timedTextTagRegex.findAll(xml).forEach { m ->
            val attrs = m.groupValues[1]
            val start = startAttrRegex.find(attrs)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
            val dur = durAttrRegex.find(attrs)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 2.2
            val text = cleanHtmlText(m.groupValues[2])
            if (text.isNotBlank()) {
                list.add(CaptionSegment(startSec = start, durationSec = dur, text = text))
            }
        }
        if (list.isNotEmpty()) return list

        // Newer srv3 format: <p t="12340" d="2100"><s>Hello</s><s> world</s></p>
        srv3TagRegex.findAll(xml).forEach { m ->
            val attrs = m.groupValues[1]
            val startMs = tAttrRegex.find(attrs)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return@forEach
            val durMs = dAttrRegex.find(attrs)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 2200L
            val text = cleanHtmlText(m.groupValues[2])
            if (text.isNotBlank()) {
                list.add(CaptionSegment(startSec = startMs / 1000.0, durationSec = durMs / 1000.0, text = text))
            }
        }
        return list.ifEmpty { null }
    }

    private fun fetchYouTubeSearchCandidates(
        searchQuery: String,
        subtitlesOnly: Boolean
    ): List<YouTubeVideoCandidate> {
        val encoded = URLEncoder.encode(searchQuery, "UTF-8")
        val spParam = if (subtitlesOnly) "&sp=EgIoAQ%253D%253D" else ""
        val url = "https://www.youtube.com/results?search_query=$encoded$spParam&hl=en&gl=US"
        val res = httpGetWithStatus(url, extraHeaders = youtubeHeaders())
        if (res.statusCode == 429) {
            rateLimitedUntilMs = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
            return emptyList()
        }
        val html = res.body ?: return emptyList()

        val matches = videoRendererIdRegex.findAll(html).toList()
        if (matches.isEmpty()) return emptyList()

        val seen = LinkedHashSet<String>()
        val results = mutableListOf<YouTubeVideoCandidate>()

        for (match in matches) {
            val videoId = match.groupValues[1]
            if (!seen.add(videoId)) continue

            val chunkStart = match.range.first
            val chunkEnd = (chunkStart + 2600).coerceAtMost(html.length)
            val chunk = html.substring(chunkStart, chunkEnd)

            val title = titleRunsRegex.find(chunk)?.groupValues?.getOrNull(1)
                ?.let { unescapeJsonString(it) }
                ?.takeIf { it.isNotBlank() }
                ?: "English Dialogue Clip"
            val channel = ownerRunsRegex.find(chunk)?.groupValues?.getOrNull(1)
                ?.let { unescapeJsonString(it) }
                ?.takeIf { it.isNotBlank() }
                ?: "YouTube English"
            val duration = lengthSimpleRegex.find(chunk)?.groupValues?.getOrNull(1)
                ?.takeIf { it.isNotBlank() }
                ?: ""

            results.add(
                YouTubeVideoCandidate(
                    videoId = videoId,
                    title = title,
                    channelName = channel,
                    durationLabel = duration
                )
            )
            if (results.size >= 8) break
        }
        return results
    }

    private fun fetchPipedSearchCandidates(searchQuery: String): List<YouTubeVideoCandidate> {
        val encoded = URLEncoder.encode(searchQuery, "UTF-8")
        val endpoints = listOf(
            "https://pipedapi.kavin.rocks/search?q=$encoded&filter=videos",
            "https://api.piped.private.coffee/search?q=$encoded&filter=videos"
        )
        for (ep in endpoints) {
            val body = runCatching { httpGet(ep, extraHeaders = mapOf("Accept" to "application/json")) }.getOrNull()
                ?: continue
            val items = runCatching { JSONObject(body).optJSONArray("items") }.getOrNull() ?: continue
            val out = parsePipedItems(items)
            if (out.isNotEmpty()) return out
        }
        return emptyList()
    }

    private fun parsePipedItems(items: JSONArray): List<YouTubeVideoCandidate> {
        val list = mutableListOf<YouTubeVideoCandidate>()
        for (i in 0 until items.length()) {
            val obj = items.optJSONObject(i) ?: continue
            val rawUrl = obj.optString("url")
            val videoId = rawUrl.substringAfter("v=", "").take(11)
            if (videoId.length != 11) continue
            val title = obj.optString("title").ifBlank { "English Dialogue Clip" }
            val uploader = obj.optString("uploaderName").ifBlank { "YouTube English" }
            val durationSec = obj.optInt("duration", 0)
            val durLabel = if (durationSec > 0) "%d:%02d".format(durationSec / 60, durationSec % 60) else ""
            list.add(YouTubeVideoCandidate(videoId, title, uploader, durLabel))
            if (list.size >= 8) break
        }
        return list
    }

    // ==================== DIRECT MOVIE/DRAMA MP4 CLIPS (GETYARN / PLAYPHRASE) ====================

    suspend fun fetchExactSceneClips(rawQuery: String): List<RealDialogueClip> = withContext(Dispatchers.IO) {
        val clean = sanitizeQuery(rawQuery)
        if (clean.isBlank()) return@withContext emptyList()

        val primary = runCatching { fetchFromGetYarn(clean) }.getOrDefault(emptyList())
        if (primary.isNotEmpty()) return@withContext primary

        val words = clean.split(" ").filter { it.length >= 4 && it.lowercase() !in stopWords }
        val coreWord = words.maxByOrNull { it.length }
        if (!coreWord.isNullOrBlank() && !coreWord.equals(clean, ignoreCase = true)) {
            val coreClips = runCatching { fetchFromGetYarn(coreWord) }.getOrDefault(emptyList())
            if (coreClips.isNotEmpty()) return@withContext coreClips
        }

        val playPhrase = runCatching { fetchFromPlayPhrase(coreWord ?: clean) }.getOrDefault(emptyList())
        if (playPhrase.isNotEmpty()) return@withContext playPhrase

        emptyList()
    }

    private fun fetchFromGetYarn(query: String): List<RealDialogueClip> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val html = httpGet("https://getyarn.io/yarn-find?text=$encoded") ?: return emptyList()

        val matches = uuidRegex.findAll(html).toList()
        if (matches.isEmpty()) return emptyList()

        val seen = LinkedHashSet<String>()
        val results = mutableListOf<RealDialogueClip>()

        for (match in matches) {
            val uuid = match.groupValues[1].lowercase()
            if (!seen.add(uuid)) continue

            val startIdx = (match.range.first - 120).coerceAtLeast(0)
            val endIdx = (match.range.last + 700).coerceAtMost(html.length)
            val snippet = html.substring(startIdx, endIdx)

            var dialogue = transcriptRegex.find(snippet)
                ?.groupValues?.getOrNull(1)
                ?.let { cleanHtmlText(it) }
                .orEmpty()

            var sourceTitle = "영화·미드 클립"

            if (dialogue.isBlank() && results.size < 3) {
                val clipHtml = runCatching { httpGet("https://getyarn.io/yarn-clip/$uuid") }.getOrNull()
                if (!clipHtml.isNullOrBlank()) {
                    val ogTitle = ogTitleRegex.find(clipHtml)?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) }.orEmpty()
                    val ogDesc = ogDescRegex.find(clipHtml)?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) }.orEmpty()
                    if (ogDesc.isNotBlank()) {
                        dialogue = ogDesc
                    } else if (ogTitle.isNotBlank()) {
                        dialogue = ogTitle
                    }
                    if (ogTitle.contains(" - ")) {
                        sourceTitle = ogTitle.substringBefore(" - ").take(36)
                    }
                }
            }

            if (dialogue.isBlank()) {
                dialogue = "\"...$query...\""
            }

            results.add(
                RealDialogueClip(
                    id = uuid,
                    mp4Url = "https://y.yarn.co/$uuid.mp4",
                    dialogueLineEn = dialogue,
                    sourceTitle = sourceTitle,
                    providerLabel = "GetYarn MP4"
                )
            )
            if (results.size >= 8) break
        }
        return results
    }

    private fun fetchFromPlayPhrase(query: String): List<RealDialogueClip> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val jsonStr = httpGet(
            urlStr = "https://www.playphrase.me/api/v1/phrases/search?q=$encoded&limit=6&platform=desktop+safari&skip=0",
            extraHeaders = mapOf(
                "Accept" to "application/json",
                "Referer" to "https://www.playphrase.me/",
                "X-Csrf-Token" to "cmf6"
            )
        ) ?: return emptyList()

        val root = JSONObject(jsonStr)
        val phrases = root.optJSONArray("phrases") ?: return emptyList()
        val list = mutableListOf<RealDialogueClip>()
        for (i in 0 until phrases.length()) {
            val item = phrases.optJSONObject(i) ?: continue
            val videoUrl = item.optString("video-url").trim()
            val text = item.optString("text").trim()
            val info = item.optJSONObject("video-info")?.optString("info").orEmpty()
            if (videoUrl.startsWith("http")) {
                list.add(
                    RealDialogueClip(
                        id = item.optString("id", "pp_$i"),
                        mp4Url = videoUrl,
                        dialogueLineEn = text.ifBlank { query },
                        sourceTitle = info.ifBlank { "PlayPhrase 클립" },
                        providerLabel = "PlayPhrase MP4"
                    )
                )
            }
        }
        return list
    }

    private fun youtubeHeaders(): Map<String, String> = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9",
        "Cookie" to "CONSENT=YES+cb.20240101-01-p0.en+FX+999; PREF=hl=en&gl=US",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    )

    private fun httpGet(
        urlStr: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): String? = httpGetWithStatus(urlStr, extraHeaders).body

    private fun httpGetWithStatus(
        urlStr: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): HttpResult {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6500
            readTimeout = 7500
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            val code = conn.responseCode
            if (code in 200..299) {
                HttpResult(conn.inputStream.bufferedReader().use { it.readText() }, code)
            } else {
                HttpResult(null, code)
            }
        } catch (_: Exception) {
            HttpResult(null, -1)
        } finally {
            conn.disconnect()
        }
    }

    private fun unescapeJsonString(raw: String): String {
        return raw
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("\\u003c", "<")
            .replace("\\u003e", ">")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\n", " ")
            .replace("\\\\", "\\")
            .trim()
    }

    private fun cleanHtmlText(raw: String): String {
        return raw
            .replace(htmlTagRegex, " ")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;#39;", "'")
            .replace("&amp;quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
