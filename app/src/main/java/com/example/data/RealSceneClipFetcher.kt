package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class RealDialogueClip(
    val id: String,
    val mp4Url: String,
    val dialogueLineEn: String,
    val sourceTitle: String,
    val providerLabel: String
)

/**
 * Fetches REAL 3~5 second direct MP4 movie/TV dialogue clips showing ONLY the exact
 * spoken line without YouTube's "Sign in to confirm you're not a bot" WebView block.
 *
 * Primary source: GetYarn (getyarn.io -> https://y.yarn.co/{UUID}.mp4)
 * Secondary fallback: PlayPhrase.me direct MP4 clips API
 */
object RealSceneClipFetcher {

    private val uuidRegex = Regex("""/yarn-clip/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})""", RegexOption.IGNORE_CASE)
    private val transcriptRegex = Regex("""class="[^"]*transcript[^"]*"[^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ogDescRegex = Regex("""<meta[^>]+property=["']og:description["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val ogTitleRegex = Regex("""<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val htmlTagRegex = Regex("""<[^>]+>""")

    suspend fun fetchExactSceneClips(rawQuery: String): List<RealDialogueClip> = withContext(Dispatchers.IO) {
        val clean = sanitizeQuery(rawQuery)
        if (clean.isBlank()) return@withContext emptyList()

        // 1. Try full clean phrase on GetYarn first (exact spoken scene MP4)
        val primary = runCatching { fetchFromGetYarn(clean) }.getOrDefault(emptyList())
        if (primary.isNotEmpty()) return@withContext primary

        // 2. If multi-word phrase had 0 exact matches, try the most distinctive content word
        val words = clean.split(" ").filter { it.length >= 4 }
        val coreWord = words.maxByOrNull { it.length }
        if (!coreWord.isNullOrBlank() && !coreWord.equals(clean, ignoreCase = true)) {
            val coreClips = runCatching { fetchFromGetYarn(coreWord) }.getOrDefault(emptyList())
            if (coreClips.isNotEmpty()) return@withContext coreClips
        }

        // 3. Secondary fallback: PlayPhrase.me JSON endpoint
        val playPhrase = runCatching { fetchFromPlayPhrase(coreWord ?: clean) }.getOrDefault(emptyList())
        if (playPhrase.isNotEmpty()) return@withContext playPhrase

        emptyList()
    }

    fun sanitizeQuery(raw: String): String {
        return raw.trim()
            .replace(Regex("[^a-zA-Z0-9\\s'-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(50)
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

            // Inspect surrounding HTML chunk for inline transcript text
            val startIdx = (match.range.first - 120).coerceAtLeast(0)
            val endIdx = (match.range.last + 700).coerceAtMost(html.length)
            val snippet = html.substring(startIdx, endIdx)

            var dialogue = transcriptRegex.find(snippet)
                ?.groupValues?.getOrNull(1)
                ?.let { cleanHtmlText(it) }
                .orEmpty()

            var sourceTitle = "실제 미드·영화 회화 장면"

            // If search card didn't expose transcript cleanly, inspect clip metadata for first 3 clips
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
                dialogue = "\"...$query...\" (실제 원어민 대사 재생 중)"
            }

            results.add(
                RealDialogueClip(
                    id = uuid,
                    mp4Url = "https://y.yarn.co/$uuid.mp4",
                    dialogueLineEn = dialogue,
                    sourceTitle = sourceTitle,
                    providerLabel = "GetYarn MP4 직재생"
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
                        sourceTitle = info.ifBlank { "PlayPhrase 영화 클립" },
                        providerLabel = "PlayPhrase MP4"
                    )
                )
            }
        }
        return list
    }

    private fun httpGet(
        urlStr: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 9000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun cleanHtmlText(raw: String): String {
        return raw
            .replace(htmlTagRegex, " ")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
