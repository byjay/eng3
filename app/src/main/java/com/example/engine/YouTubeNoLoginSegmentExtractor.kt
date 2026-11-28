package com.example.engine

import android.util.Log
import com.example.data.YouTubeDialogueMatch
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor

data class CaptionCue(
    val startSec: Double,
    val endSec: Double,
    val text: String
)

data class ParsedYouTubeInput(
    val videoId: String,
    val startTimeSec: Int? = null
)

/**
 * Zero-Login YouTube Segment Dialogue Extractor.
 *
 * Extracts exact timestamped English dialogue segments (`[start-1s .. end+1.5s]`) from public
 * YouTube videos WITHOUT requiring Google/YouTube login, cookies, OAuth, or paid API keys:
 *  1. Direct YouTube URL / Video ID extraction (`youtube.com/watch`, `youtu.be`, `shorts`, `embed`)
 *  2. Keyword-to-video search using login-free public Innertube search (CC-filtered) + caption matching
 *  3. Multi-format caption parser: YouTube `json3`, XML `<text>` / `<p>`, and manual SRT / transcript paste
 *  4. 24-hour local disk JSON cache + strict HTTP 429 cooldown protection
 */
object YouTubeNoLoginSegmentExtractor {

    private const val TAG = "YouTubeNoLoginExtractor"
    private const val CACHE_DIR_NAME = "yt_nologin_seg_v2"
    private const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    private const val RATE_LIMIT_COOLDOWN_MS = 90_000L
    private const val MAX_CANDIDATE_VIDEOS = 5
    private const val MAX_RESULTS = 8

    @Volatile
    private var rateLimitedUntilMs: Long = 0L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(9, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val moshi by lazy { Moshi.Builder().build() }
    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any?>>(type)
    }

    private val videoIdPatterns = listOf(
        Regex("""(?:youtube\.com/watch\?[^#\s]*\bv=|youtu\.be/|youtube\.com/embed/|youtube\.com/shorts/|youtube\.com/live/)([A-Za-z0-9_-]{11})"""),
        Regex("""^\s*([A-Za-z0-9_-]{11})\s*$""")
    )

    private val timeParamRegex = Regex("""[?&](?:t|start)=([0-9hmsHMS]+)""")

    fun isRateLimited(nowMs: Long = System.currentTimeMillis()): Boolean = nowMs < rateLimitedUntilMs

    internal fun clearRateLimitForTest() {
        rateLimitedUntilMs = 0L
    }

    /**
     * Parses any YouTube URL or bare 11-character video ID without requiring login.
     */
    fun parseYouTubeInput(raw: String): ParsedYouTubeInput? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val vid = videoIdPatterns.firstNotNullOfOrNull { rx ->
            rx.find(trimmed)?.groupValues?.getOrNull(1)
        } ?: return null
        val tRaw = timeParamRegex.find(trimmed)?.groupValues?.getOrNull(1)
        val tSec = tRaw?.let { parseFlexibleTimestampToSeconds(it) }
        return ParsedYouTubeInput(videoId = vid, startTimeSec = tSec)
    }

    fun parseFlexibleTimestampToSeconds(raw: String): Int? {
        val clean = raw.trim().lowercase(Locale.US)
        if (clean.isBlank()) return null
        clean.toIntOrNull()?.let { return it.coerceAtLeast(0) }
        if (clean.endsWith("s") && !clean.contains("m") && !clean.contains("h")) {
            clean.dropLast(1).toIntOrNull()?.let { return it.coerceAtLeast(0) }
        }
        val hms = Regex("""(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s?)?""").matchEntire(clean)
        if (hms != null && (hms.groupValues[1].isNotEmpty() || hms.groupValues[2].isNotEmpty() || hms.groupValues[3].isNotEmpty())) {
            val h = hms.groupValues[1].toIntOrNull() ?: 0
            val m = hms.groupValues[2].toIntOrNull() ?: 0
            val s = hms.groupValues[3].toIntOrNull() ?: 0
            return (h * 3600 + m * 60 + s).coerceAtLeast(0)
        }
        val colonParts = clean.split(":").mapNotNull { it.substringBefore(".").substringBefore(",").toIntOrNull() }
        return when (colonParts.size) {
            2 -> colonParts[0] * 60 + colonParts[1]
            3 -> colonParts[0] * 3600 + colonParts[1] * 60 + colonParts[2]
            else -> null
        }
    }

    /**
     * Parses YouTube `fmt=json3` caption payloads into clean timestamped [CaptionCue] entries.
     */
    fun parseJson3Captions(rawJson: String): List<CaptionCue> {
        if (rawJson.isBlank() || !rawJson.trimStart().startsWith("{")) return emptyList()
        return runCatching {
            val root = mapAdapter.fromJson(rawJson) ?: return emptyList()
            val events = root["events"] as? List<*> ?: return emptyList()
            buildList {
                for (rawEv in events) {
                    val ev = rawEv as? Map<*, *> ?: continue
                    val tStartMs = (ev["tStartMs"] as? Number)?.toLong() ?: continue
                    if (tStartMs < 0L) continue
                    val dDurationMs = ((ev["dDurationMs"] as? Number)?.toLong() ?: 2500L).coerceAtLeast(500L)
                    val segs = ev["segs"] as? List<*> ?: continue
                    val sb = StringBuilder()
                    for (rawSeg in segs) {
                        val seg = rawSeg as? Map<*, *> ?: continue
                        val utf8 = seg["utf8"] as? String ?: ""
                        if (utf8.isNotEmpty()) sb.append(utf8)
                    }
                    val cleaned = cleanCaptionText(sb.toString())
                    if (cleaned.isNotBlank()) {
                        val startSec = tStartMs / 1000.0
                        val endSec = (tStartMs + dDurationMs) / 1000.0
                        add(CaptionCue(startSec = startSec, endSec = endSec, text = cleaned))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Parses YouTube XML captions (`<text start=".." dur="..">` or `<p t=".." d="..">`).
     */
    fun parseXmlCaptions(rawXml: String): List<CaptionCue> {
        if (rawXml.isBlank() || !rawXml.contains("<")) return emptyList()
        val cues = mutableListOf<CaptionCue>()
        val textTagRegex = Regex("""<text\b([^>]*)>(.*?)</text>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val attrStart = Regex("""start="([0-9.]+)"""")
        val attrDur = Regex("""dur="([0-9.]+)"""")
        textTagRegex.findAll(rawXml).forEach { m ->
            val attrs = m.groupValues[1]
            val body = cleanCaptionText(m.groupValues[2].replace(Regex("<[^>]+>"), " "))
            val start = attrStart.find(attrs)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
            val dur = attrDur.find(attrs)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.coerceAtLeast(0.6) ?: 2.5
            if (body.isNotBlank()) {
                cues += CaptionCue(startSec = start, endSec = start + dur, text = body)
            }
        }
        if (cues.isNotEmpty()) return cues

        val pTagRegex = Regex("""<p\b([^>]*)>(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val attrT = Regex("""\bt="(\d+)"""")
        val attrD = Regex("""\bd="(\d+)"""")
        pTagRegex.findAll(rawXml).forEach { m ->
            val attrs = m.groupValues[1]
            val body = cleanCaptionText(m.groupValues[2].replace(Regex("<[^>]+>"), " "))
            val tMs = attrT.find(attrs)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return@forEach
            val dMs = attrD.find(attrs)?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(600L) ?: 2500L
            if (body.isNotBlank()) {
                cues += CaptionCue(startSec = tMs / 1000.0, endSec = (tMs + dMs) / 1000.0, text = body)
            }
        }
        return cues
    }

    /**
     * Parses pasted SRT blocks (`00:01:02,000 --> 00:01:05,500`) or YouTube copy-pasted
     * transcript lines (`1:02\nHello there` or `01:02 Hello there`) into [CaptionCue]s.
     */
    fun parseManualTranscriptOrSrt(rawText: String): List<CaptionCue> {
        val text = rawText.trim()
        if (text.isBlank()) return emptyList()

        // 1. Try standard SRT format
        val srtRegex = Regex(
            """(\d{1,2}:\d{2}:\d{2}[,.]\d{1,3}|\d{1,2}:\d{2}[,.]\d{1,3})\s*-->\s*(\d{1,2}:\d{2}:\d{2}[,.]\d{1,3}|\d{1,2}:\d{2}[,.]\d{1,3})\s*\n(.*?)(?=\n\s*\n|\z)""",
            RegexOption.DOT_MATCHES_ALL
        )
        val srtMatches = srtRegex.findAll(text).toList()
        if (srtMatches.isNotEmpty()) {
            return srtMatches.mapNotNull { m ->
                val s = parseFlexibleTimestampToSeconds(m.groupValues[1])?.toDouble() ?: return@mapNotNull null
                val e = (parseFlexibleTimestampToSeconds(m.groupValues[2])?.toDouble() ?: (s + 3.0)).coerceAtLeast(s + 1.0)
                val line = cleanCaptionText(m.groupValues[3])
                if (line.isBlank()) null else CaptionCue(s, e, line)
            }
        }

        // 2. Try YouTube copy-pasted transcript (`0:15` on its own line OR `0:15 dialogue text`)
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val inlineTsRegex = Regex("""^(\d{1,2}:\d{2}(?::\d{2})?)\s+(.+)$""")
        val standaloneTsRegex = Regex("""^(\d{1,2}:\d{2}(?::\d{2})?)$""")
        val rawPairs = mutableListOf<Pair<Double, String>>()
        var pendingTs: Double? = null
        for (line in lines) {
            val inlineMatch = inlineTsRegex.matchEntire(line)
            if (inlineMatch != null) {
                val sec = parseFlexibleTimestampToSeconds(inlineMatch.groupValues[1])?.toDouble()
                val body = cleanCaptionText(inlineMatch.groupValues[2])
                if (sec != null && body.isNotBlank()) {
                    rawPairs += sec to body
                }
                pendingTs = null
                continue
            }
            val standalone = standaloneTsRegex.matchEntire(line)
            if (standalone != null) {
                pendingTs = parseFlexibleTimestampToSeconds(standalone.groupValues[1])?.toDouble()
                continue
            }
            if (pendingTs != null) {
                val body = cleanCaptionText(line)
                if (body.isNotBlank()) {
                    rawPairs += pendingTs to body
                }
                pendingTs = null
            }
        }
        return rawPairs.mapIndexed { idx, (start, body) ->
            val nextStart = rawPairs.getOrNull(idx + 1)?.first
            val end = when {
                nextStart != null && nextStart > start -> nextStart.coerceAtMost(start + 8.0)
                else -> start + 4.0
            }
            CaptionCue(startSec = start, endSec = end, text = body)
        }
    }

    /**
     * Matches [query] (supporting exact phrases and English inflections) against [cues]
     * and returns crisp A-B looped [YouTubeDialogueMatch] cards with before/after context.
     */
    fun extractDialogueMatchesFromCues(
        videoId: String,
        title: String,
        channelName: String,
        cues: List<CaptionCue>,
        query: String,
        captionTypeLabel: String = "영어 자막 (무로그인)",
        sourceBadge: String = "YouTube 구간 추출 · 무로그인",
        maxPerVideo: Int = 5
    ): List<YouTubeDialogueMatch> {
        if (videoId.isBlank() || cues.isEmpty()) return emptyList()
        val cleanQuery = query.trim().lowercase(Locale.US)
        val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        if (cleanQuery.isBlank()) {
            // If user provided a URL without a filter word, extract representative well-formed lines
            val step = (cues.size / maxPerVideo).coerceAtLeast(1)
            return cues.indices
                .filter { it % step == 0 && cues[it].text.split(" ").size >= 4 }
                .take(maxPerVideo)
                .map { idx ->
                    buildMatchFromCueIndex(
                        videoId = videoId,
                        title = title,
                        channelName = channelName,
                        thumbUrl = thumbUrl,
                        cues = cues,
                        idx = idx,
                        isExact = true,
                        captionTypeLabel = captionTypeLabel,
                        sourceBadge = sourceBadge
                    )
                }
        }

        val queryTokens = cleanQuery.split(Regex("\\s+")).filter { it.length >= 2 }
        val phraseRegex = buildFlexiblePhraseRegex(cleanQuery)
        val singleWordVariants = if (queryTokens.size == 1) {
            WordRun2027Engine.variants(queryTokens.first())
                .filter { it.isNotBlank() }
                .joinToString("|") { Regex.escape(it) }
                .let { if (it.isNotBlank()) Regex("\\b(?:$it)\\b", RegexOption.IGNORE_CASE) else null }
        } else null

        data class ScoredIdx(val idx: Int, val score: Int, val exact: Boolean)
        val scored = mutableListOf<ScoredIdx>()

        for (i in cues.indices) {
            val line = cues[i].text
            val combinedWindow = listOfNotNull(
                cues.getOrNull(i)?.text,
                cues.getOrNull(i + 1)?.text
            ).joinToString(" ")

            val exactInSingle = phraseRegex.containsMatchIn(line) ||
                (singleWordVariants?.containsMatchIn(line) == true)
            val exactAcrossTwo = !exactInSingle && phraseRegex.containsMatchIn(combinedWindow)

            if (exactInSingle) {
                scored += ScoredIdx(i, 100, exact = true)
            } else if (exactAcrossTwo) {
                scored += ScoredIdx(i, 85, exact = true)
            } else if (queryTokens.size >= 2) {
                val lowerLine = combinedWindow.lowercase(Locale.US)
                val hitCount = queryTokens.count { tok -> Regex("\\b${Regex.escape(tok)}").containsMatchIn(lowerLine) }
                if (hitCount >= (queryTokens.size * 0.7f).toInt().coerceAtLeast(2)) {
                    scored += ScoredIdx(i, 40 + hitCount * 10, exact = false)
                }
            }
        }

        val selected = mutableListOf<YouTubeDialogueMatch>()
        val usedTimeRanges = mutableListOf<IntRange>()

        for (cand in scored.sortedWith(compareByDescending<ScoredIdx> { it.score }.thenBy { it.idx })) {
            if (selected.size >= maxPerVideo) break
            val match = buildMatchFromCueIndex(
                videoId = videoId,
                title = title,
                channelName = channelName,
                thumbUrl = thumbUrl,
                cues = cues,
                idx = cand.idx,
                isExact = cand.exact,
                captionTypeLabel = captionTypeLabel,
                sourceBadge = sourceBadge
            )
            val range = match.startSeconds..match.endSeconds
            val overlapsExisting = usedTimeRanges.any { existing ->
                !(range.last < existing.first || range.first > existing.last)
            }
            if (!overlapsExisting) {
                usedTimeRanges += range
                selected += match
            }
        }
        return selected
    }

    private fun buildMatchFromCueIndex(
        videoId: String,
        title: String,
        channelName: String,
        thumbUrl: String,
        cues: List<CaptionCue>,
        idx: Int,
        isExact: Boolean,
        captionTypeLabel: String,
        sourceBadge: String
    ): YouTubeDialogueMatch {
        val cue = cues[idx]
        val before = cues.getOrNull(idx - 1)?.text.orEmpty()
        val after = cues.getOrNull(idx + 1)?.text.orEmpty()
        val rawStart = cue.startSec.coerceAtLeast(0.0)
        val rawEnd = cue.endSec.coerceAtLeast(rawStart + 1.5)
        val paddedStart = floor((rawStart - 1.0).coerceAtLeast(0.0)).toInt()
        val paddedEnd = ceil(rawEnd + 1.5).toInt().coerceAtLeast(paddedStart + 4)

        val totalEndSec = cues.lastOrNull()?.endSec?.toInt() ?: paddedEnd
        val durationLabel = "%02d:%02d".format(totalEndSec / 60, totalEndSec % 60)

        return YouTubeDialogueMatch(
            videoId = videoId,
            title = title.ifBlank { "YouTube English Dialogue ($videoId)" },
            channelName = channelName.ifBlank { "YouTube" },
            durationLabel = durationLabel,
            thumbnailUrl = thumbUrl,
            matchedDialogueEn = cue.text,
            contextBeforeEn = before,
            contextAfterEn = after,
            startSeconds = paddedStart,
            endSeconds = paddedEnd,
            rawStartSec = rawStart,
            rawEndSec = rawEnd,
            isExactCaptionMatch = isExact,
            fromCache = false,
            captionTypeLabel = captionTypeLabel,
            sourceBadge = sourceBadge
        )
    }

    /**
     * Main login-free entry point:
     * - If [rawQuery] contains a YouTube URL or Video ID, extracts dialogues directly from that video.
     * - Otherwise discovers CC-enabled English YouTube videos matching [rawQuery] via no-login
     *   Innertube search and extracts exact timestamped segments.
     */
    suspend fun searchNoLoginYouTubeSegments(
        rawQuery: String,
        cacheDir: File? = null
    ): List<YouTubeDialogueMatch> = withContext(Dispatchers.IO) {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // 1. Direct URL or Video ID pasted into search box
        val parsedUrl = parseYouTubeInput(trimmed)
        if (parsedUrl != null) {
            return@withContext extractFromVideoUrlOrId(
                urlOrId = trimmed,
                keywordQuery = "",
                cacheDir = cacheDir
            )
        }

        // 2. Check 24h local cache first (instant, zero network)
        val cacheKey = "search_${trimmed.lowercase(Locale.US)}"
        readFromCache(cacheDir, cacheKey)?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        if (isRateLimited()) return@withContext emptyList()

        // 3. Discover CC-enabled public YouTube videos (no login, no cookies)
        val candidates = discoverPublicCaptionedVideos(trimmed)
        if (candidates.isEmpty()) return@withContext emptyList()

        val collected = mutableListOf<YouTubeDialogueMatch>()
        for (cand in candidates.take(MAX_CANDIDATE_VIDEOS)) {
            if (collected.size >= MAX_RESULTS || isRateLimited()) break
            val (cues, trackLabel) = fetchNoLoginEnglishCaptions(cand.videoId)
            if (cues.isEmpty()) continue
            val hits = extractDialogueMatchesFromCues(
                videoId = cand.videoId,
                title = cand.title,
                channelName = cand.channelName,
                cues = cues,
                query = trimmed,
                captionTypeLabel = trackLabel,
                sourceBadge = "YouTube 구간 추출 · 무로그인",
                maxPerVideo = 2
            )
            collected.addAll(hits)
        }

        val finalMatches = collected
            .sortedByDescending { it.isExactCaptionMatch }
            .take(MAX_RESULTS)

        if (finalMatches.isNotEmpty()) {
            writeToCache(cacheDir, cacheKey, finalMatches)
        }
        finalMatches
    }

    /**
     * Direct login-free extraction from ANY pasted YouTube URL or Video ID.
     */
    suspend fun extractFromVideoUrlOrId(
        urlOrId: String,
        keywordQuery: String = "",
        cacheDir: File? = null
    ): List<YouTubeDialogueMatch> = withContext(Dispatchers.IO) {
        val parsed = parseYouTubeInput(urlOrId) ?: return@withContext emptyList()
        val videoId = parsed.videoId
        val cacheKey = "vid_${videoId}_${keywordQuery.trim().lowercase(Locale.US)}"
        readFromCache(cacheDir, cacheKey)?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val meta = fetchPublicOEmbedMeta(videoId)
        val (cues, trackLabel) = fetchNoLoginEnglishCaptions(videoId)
        val title = meta?.first.orEmpty().ifBlank { "YouTube 영상 ($videoId)" }
        val channel = meta?.second.orEmpty().ifBlank { "YouTube" }

        val matches = if (cues.isNotEmpty()) {
            extractDialogueMatchesFromCues(
                videoId = videoId,
                title = title,
                channelName = channel,
                cues = cues,
                query = keywordQuery,
                captionTypeLabel = trackLabel,
                sourceBadge = "직접 링크 구간 추출 · 무로그인",
                maxPerVideo = 8
            ).ifEmpty {
                // If keyword wasn't found in captions, still surface key segments around URL timestamp or opening
                val startHint = parsed.startTimeSec ?: 0
                val nearIdx = cues.indexOfFirst { it.startSec >= startHint }.coerceAtLeast(0)
                cues.indices.drop(nearIdx).take(5).map { idx ->
                    buildMatchFromCueIndex(
                        videoId = videoId,
                        title = title,
                        channelName = channel,
                        thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                        cues = cues,
                        idx = idx,
                        isExact = false,
                        captionTypeLabel = "$trackLabel · 전체 구간",
                        sourceBadge = "직접 링크 구간 추출 · 무로그인"
                    )
                }
            }
        } else {
            // Even if uploader disabled captions, return a playable A-B window at requested timestamp
            val start = (parsed.startTimeSec ?: 0).coerceAtLeast(0)
            listOf(
                YouTubeDialogueMatch(
                    videoId = videoId,
                    title = title,
                    channelName = channel,
                    durationLabel = "%02d:%02d".format(start / 60, start % 60),
                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    matchedDialogueEn = "Public English captions are disabled on this video — you can still play A-B loop here or paste transcript below.",
                    startSeconds = start,
                    endSeconds = start + 15,
                    rawStartSec = start.toDouble(),
                    rawEndSec = (start + 15).toDouble(),
                    isExactCaptionMatch = false,
                    captionTypeLabel = "자막 미제공 영상",
                    sourceBadge = "무로그인 IFrame 재생"
                )
            )
        }

        if (matches.isNotEmpty()) {
            writeToCache(cacheDir, cacheKey, matches)
        }
        matches
    }

    /**
     * Offline / manual transcript fallback: combines a YouTube video URL/ID with pasted
     * SRT or YouTube transcript text to generate instant clickable A-B dialogue segments.
     */
    fun extractFromManualTranscript(
        videoUrlOrId: String,
        transcriptText: String,
        keywordQuery: String = ""
    ): List<YouTubeDialogueMatch> {
        val parsed = parseYouTubeInput(videoUrlOrId) ?: return emptyList()
        val cues = parseManualTranscriptOrSrt(transcriptText)
        if (cues.isEmpty()) return emptyList()
        return extractDialogueMatchesFromCues(
            videoId = parsed.videoId,
            title = "직접 추출 영상 (${parsed.videoId})",
            channelName = "사용자 자막 연동",
            cues = cues,
            query = keywordQuery,
            captionTypeLabel = "직접 자막 파싱",
            sourceBadge = "SRT·스크립트 구간 추출",
            maxPerVideo = 12
        )
    }

    private data class CandidateVideo(val videoId: String, val title: String, val channelName: String)

    private fun discoverPublicCaptionedVideos(query: String): List<CandidateVideo> {
        return runCatching {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20250101.00.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("query", "\"$query\" english conversation OR ted talk OR interview")
                put("params", "EgIoAQ%3D%3D") // CC (subtitles/captions) filter
            }
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 429) {
                    rateLimitedUntilMs = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
                    return@use emptyList()
                }
                if (!resp.isSuccessful) return@use emptyList()
                val body = resp.body?.string().orEmpty()
                parseInnertubeSearchCandidates(body)
            }
        }.getOrElse { e ->
            runCatching { Log.w(TAG, "No-login video discovery failed: ${e.message}") }
            emptyList()
        }
    }

    private fun parseInnertubeSearchCandidates(rawJson: String): List<CandidateVideo> {
        if (rawJson.isBlank()) return emptyList()
        val found = LinkedHashMap<String, CandidateVideo>()
        val vrRegex = Regex(""""videoRenderer"\s*:\s*\{\s*"videoId"\s*:\s*"([A-Za-z0-9_-]{11})"""")
        vrRegex.findAll(rawJson).forEach { m ->
            val vid = m.groupValues[1]
            if (vid !in found && found.size < MAX_CANDIDATE_VIDEOS * 2) {
                val window = rawJson.substring(m.range.first, (m.range.first + 1800).coerceAtMost(rawJson.length))
                val title = Regex(""""title"\s*:\s*\{\s*"runs"\s*:\s*\[\s*\{\s*"text"\s*:\s*"([^"]+)"""")
                    .find(window)?.groupValues?.getOrNull(1)
                    ?.replace("\\\"", "\"")
                    .orEmpty()
                val channel = Regex(""""ownerText"\s*:\s*\{\s*"runs"\s*:\s*\[\s*\{\s*"text"\s*:\s*"([^"]+)"""")
                    .find(window)?.groupValues?.getOrNull(1)
                    ?.replace("\\\"", "\"")
                    .orEmpty()
                found[vid] = CandidateVideo(vid, title, channel)
            }
        }
        return found.values.toList()
    }

    private fun fetchPublicOEmbedMeta(videoId: String): Pair<String, String>? {
        return runCatching {
            val encoded = URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")
            val req = Request.Builder()
                .url("https://www.youtube.com/oembed?url=$encoded&format=json")
                .header("Accept", "application/json")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val json = JSONObject(resp.body?.string().orEmpty())
                val title = json.optString("title", "").trim()
                val author = json.optString("author_name", "").trim()
                if (title.isBlank() && author.isBlank()) null else (title to author)
            }
        }.getOrNull()
    }

    /**
     * Fetches public English captions without any login/cookies using Innertube player metadata
     * first, then public timedtext JSON3/XML fallback.
     */
    private fun fetchNoLoginEnglishCaptions(videoId: String): Pair<List<CaptionCue>, String> {
        if (isRateLimited()) return emptyList<CaptionCue>() to "제한됨"

        // Path A: No-login Innertube ANDROID client player response -> official captionTracks baseUrl
        val trackFromInnertube = discoverCaptionTrackViaInnertube(videoId)
        if (trackFromInnertube != null) {
            val (baseUrl, isAsr) = trackFromInnertube
            val json3Url = if (baseUrl.contains("fmt=")) baseUrl else "$baseUrl&fmt=json3"
            val cues = downloadAndParseCaptionUrl(json3Url)
            if (cues.isNotEmpty()) {
                return cues to (if (isAsr) "자동 영어 자막 (무로그인)" else "공식 영어 자막 (무로그인)")
            }
        }

        // Path B: Direct public timedtext JSON3/XML fallback (manual en -> auto asr en)
        val directCandidates = listOf(
            "https://www.youtube.com/api/timedtext?v=$videoId&lang=en&fmt=json3" to "공식 영어 자막 (무로그인)",
            "https://www.youtube.com/api/timedtext?v=$videoId&lang=en&kind=asr&fmt=json3" to "자동 영어 자막 (무로그인)",
            "https://www.youtube.com/api/timedtext?v=$videoId&lang=en" to "공식 영어 자막 (무로그인)"
        )
        for ((url, label) in directCandidates) {
            if (isRateLimited()) break
            val cues = downloadAndParseCaptionUrl(url)
            if (cues.isNotEmpty()) return cues to label
        }

        return emptyList<CaptionCue>() to "자막 없음"
    }

    private fun discoverCaptionTrackViaInnertube(videoId: String): Pair<String, Boolean>? {
        return runCatching {
            val payload = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID")
                        put("clientVersion", "19.29.37")
                        put("androidSdkVersion", 34)
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", "com.google.android.youtube/19.29.37 (Linux; U; Android 14) gzip")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 429) {
                    rateLimitedUntilMs = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
                    return@use null
                }
                if (!resp.isSuccessful) return@use null
                val root = JSONObject(resp.body?.string().orEmpty())
                val tracks = root.optJSONObject("captions")
                    ?.optJSONObject("playerCaptionsTracklistRenderer")
                    ?.optJSONArray("captionTracks")
                    ?: return@use null

                var bestManual: String? = null
                var bestAsr: String? = null
                for (i in 0 until tracks.length()) {
                    val tr = tracks.optJSONObject(i) ?: continue
                    val lang = tr.optString("languageCode", "").lowercase(Locale.US)
                    val baseUrl = tr.optString("baseUrl", "").trim()
                    if (!lang.startsWith("en") || baseUrl.isBlank()) continue
                    val kind = tr.optString("kind", "")
                    if (kind == "asr") {
                        if (bestAsr == null) bestAsr = baseUrl
                    } else {
                        if (bestManual == null || lang == "en" || lang == "en-us") {
                            bestManual = baseUrl
                        }
                    }
                }
                when {
                    bestManual != null -> bestManual to false
                    bestAsr != null -> bestAsr to true
                    else -> null
                }
            }
        }.getOrNull()
    }

    private fun downloadAndParseCaptionUrl(url: String): List<CaptionCue> {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json, text/xml, */*")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 429) {
                    rateLimitedUntilMs = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
                    return@use emptyList()
                }
                if (!resp.isSuccessful) return@use emptyList()
                val body = resp.body?.string().orEmpty().trim()
                if (body.isBlank()) return@use emptyList()
                if (body.startsWith("{")) parseJson3Captions(body) else parseXmlCaptions(body)
            }
        }.getOrDefault(emptyList())
    }

    private fun buildFlexiblePhraseRegex(cleanQuery: String): Regex {
        val tokens = cleanQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return Regex("$^")
        val body = tokens.joinToString("""[\s,.'"-]+""") { Regex.escape(it) }
        return Regex("""\b$body\b""", RegexOption.IGNORE_CASE)
    }

    internal fun cleanCaptionText(raw: String): String {
        return raw
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("""\[[^\]]*]"""), " ") // remove [Music], [Applause]
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun cacheFileFor(cacheDir: File?, key: String): File? {
        if (cacheDir == null) return null
        val dir = File(cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(24)
        return File(dir, "$hash.json")
    }

    private fun readFromCache(cacheDir: File?, key: String): List<YouTubeDialogueMatch>? {
        val file = cacheFileFor(cacheDir, key) ?: return null
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MS) return null
        return runCatching {
            val root = mapAdapter.fromJson(file.readText(Charsets.UTF_8)) ?: return null
            val items = root["items"] as? List<*> ?: return null
            items.mapNotNull { raw ->
                val o = raw as? Map<*, *> ?: return@mapNotNull null
                val videoId = (o["videoId"] as? String).orEmpty()
                if (videoId.isBlank()) return@mapNotNull null
                YouTubeDialogueMatch(
                    videoId = videoId,
                    title = (o["title"] as? String).orEmpty(),
                    channelName = (o["channelName"] as? String).orEmpty(),
                    durationLabel = (o["durationLabel"] as? String).orEmpty(),
                    thumbnailUrl = (o["thumbnailUrl"] as? String).orEmpty(),
                    matchedDialogueEn = (o["matchedDialogueEn"] as? String).orEmpty(),
                    contextBeforeEn = (o["contextBeforeEn"] as? String).orEmpty(),
                    contextAfterEn = (o["contextAfterEn"] as? String).orEmpty(),
                    startSeconds = (o["startSeconds"] as? Number)?.toInt() ?: 0,
                    endSeconds = (o["endSeconds"] as? Number)?.toInt() ?: 8,
                    rawStartSec = (o["rawStartSec"] as? Number)?.toDouble() ?: 0.0,
                    rawEndSec = (o["rawEndSec"] as? Number)?.toDouble() ?: 8.0,
                    isExactCaptionMatch = (o["isExactCaptionMatch"] as? Boolean) == true,
                    fromCache = true,
                    captionTypeLabel = (o["captionTypeLabel"] as? String).orEmpty().ifBlank { "캐시 자막" },
                    sourceBadge = (o["sourceBadge"] as? String).orEmpty().ifBlank { "캐시 · 무로그인" }
                )
            }
        }.getOrNull()
    }

    private fun writeToCache(cacheDir: File?, key: String, items: List<YouTubeDialogueMatch>) {
        val file = cacheFileFor(cacheDir, key) ?: return
        runCatching {
            val payload = mapOf(
                "savedAt" to System.currentTimeMillis(),
                "items" to items.map { m ->
                    mapOf(
                        "videoId" to m.videoId,
                        "title" to m.title,
                        "channelName" to m.channelName,
                        "durationLabel" to m.durationLabel,
                        "thumbnailUrl" to m.thumbnailUrl,
                        "matchedDialogueEn" to m.matchedDialogueEn,
                        "contextBeforeEn" to m.contextBeforeEn,
                        "contextAfterEn" to m.contextAfterEn,
                        "startSeconds" to m.startSeconds,
                        "endSeconds" to m.endSeconds,
                        "rawStartSec" to m.rawStartSec,
                        "rawEndSec" to m.rawEndSec,
                        "isExactCaptionMatch" to m.isExactCaptionMatch,
                        "captionTypeLabel" to m.captionTypeLabel,
                        "sourceBadge" to m.sourceBadge
                    )
                }
            )
            file.writeText(mapAdapter.toJson(payload), Charsets.UTF_8)
        }
    }
}
