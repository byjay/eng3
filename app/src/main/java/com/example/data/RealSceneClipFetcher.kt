package com.example.data

import java.io.File
import java.net.URLEncoder

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
    val sourceBadge: String = "YouTube 공식 검색",
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

/**
 * Policy-Compliant External Video & Pronunciation Link Helper:
 * - Zero unofficial HTML scraping, zero `timedtext` scraping, and zero unauthorized MP4 extraction.
 * - Sanitizes user phrases and constructs official YouTube / YouGlish / Filmot study links only.
 */
object RealSceneClipFetcher {

    private val stopWords = setOf(
        "the", "and", "for", "with", "that", "this", "your", "have", "from", "just",
        "very", "into", "about", "there", "here", "what", "when", "where", "which",
        "will", "would", "could", "should", "they", "them", "their", "then", "than",
        "are", "was", "were", "been", "being", "does", "did", "done", "you", "our", "out"
    )

    fun isCurrentlyRateLimited(): Boolean = false

    fun buildOfficialYouTubeSearchUrl(rawQuery: String): String {
        val clean = sanitizeQuery(rawQuery).ifBlank { "hello" }
        val encoded = URLEncoder.encode("\"$clean\" english lesson conversation", "UTF-8")
        return "https://www.youtube.com/results?search_query=$encoded"
    }

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
     * Converts a long conversational sentence into a crisp 2~4 word English search phrase
     * while keeping short words/idioms untouched.
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
     * Unofficial YouTube HTML and `timedtext` scraping have been removed for ToS & copyright compliance.
     * The UI opens official YouTube search or embedded YouGlish / Filmot links directly instead.
     */
    suspend fun searchYouTubeDialogues(
        rawQuery: String,
        cacheDir: File? = null
    ): List<YouTubeDialogueMatch> = emptyList()

    /**
     * Unofficial third-party movie/drama MP4 scraping has been removed for copyright compliance.
     */
    suspend fun fetchExactSceneClips(rawQuery: String): List<RealDialogueClip> = emptyList()
}
