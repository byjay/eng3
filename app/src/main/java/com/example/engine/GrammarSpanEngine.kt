package com.example.engine

import com.example.model.GrammarSpan
import com.example.model.GrammarSpanRole
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Atomic non-overlapping text segment produced by interval partitioning over
 * `grammar_spans` and optional target-word Cloze blank ranges.
 */
data class RenderedSentenceSegment(
    val start: Int,
    val end: Int,
    val text: String,
    val primaryGrammarRole: GrammarSpanRole? = null,
    val allGrammarRoles: List<GrammarSpanRole> = emptyList(),
    val isTargetToken: Boolean = false,
    val isClozeMasked: Boolean = false,
    val isClozeRevealed: Boolean = false
)

/**
 * Production-grade `grammar_spans` parser, deterministic fallback clause detector,
 * and conflict-free interval partitioning engine for Cloze + Grammar highlighting.
 */
object GrammarSpanEngine {

    private val moshi by lazy { Moshi.Builder().build() }
    private val listAnyAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, Any::class.java)
        moshi.adapter<List<Any?>>(type)
    }

    private val RELATIVE_CLAUSE_REGEX = Regex(
        """\b(which|that|who|whom|whose|what|where|when|whether|although|whereas|whenever|while)\b[^,.;:!?]{4,64}""",
        RegexOption.IGNORE_CASE
    )
    private val PARTICIPLE_REGEX = Regex(
        """(?:^|,\s*)([A-Z]?[a-z]{3,}(?:ing|ed)\s+(?:by|in|with|from|to|on|at|for|as|through)?\s*[A-Za-z][^,.;:!?]{2,42})"""
    )
    private val PARALLEL_REGEX = Regex(
        """\b([A-Za-z]+(?:ed|ing|ly|s)?\s*,\s*[A-Za-z]+(?:ed|ing|ly|s)?\s*,?\s+(?:and|or)\s+[A-Za-z]+(?:ed|ing|ly|s)?|not only\s+[^,.;:!?]{2,28}\s+but also\s+[^,.;:!?]{2,28}|both\s+[A-Za-z-]+\s+and\s+[A-Za-z-]+)\b""",
        RegexOption.IGNORE_CASE
    )
    private val PREP_MODIFIER_REGEX = Regex(
        """\b((?:of|in|for|with|through|during|between|among|beyond|across|without|regarding)\s+(?:the\s+|a\s+|an\s+|their\s+|this\s+|these\s+|our\s+)?[A-Za-z-]+(?:\s+[A-Za-z-]+){0,2})\b""",
        RegexOption.IGNORE_CASE
    )
    private val MAIN_VERB_REGEX = Regex(
        """\b((?:is|are|was|were|has|have|had|can|could|should|will|would|may|might|must|does|do|did)\s+(?:not\s+)?(?:be\s+|been\s+)?[a-z]{3,}(?:ed|ing|en|s)?|(?:produces|produce|reveals|revealed|shows|showed|requires|required|helps|helped|creates|created|leads|led|becomes|became|remains|remained|demonstrates|demonstrated|contradicts|contradict|emerges|emerge|transforms|transformed|connects|connected|adapts|adapt|explains|explained|provides|provided|determines|determined|suggests|suggested|improves|improved|generates|generated|protects|protected|enables|enabled|allows|allowed|fosters|fostered|reflects|reflected|maintains|maintained|supports|supported|influences|influenced|shapes|shaped|drives|driven|builds|built))\b""",
        RegexOption.IGNORE_CASE
    )

    // Visual priority when multiple grammar roles overlap on the same character range
    private val ROLE_PRIORITY: Map<GrammarSpanRole, Int> = mapOf(
        GrammarSpanRole.MAIN_VERB to 6,
        GrammarSpanRole.RELATIVE_CLAUSE to 5,
        GrammarSpanRole.PARTICIPLE_PHRASE to 4,
        GrammarSpanRole.PARALLEL_UNIT to 3,
        GrammarSpanRole.SUBJECT_CHUNK to 2,
        GrammarSpanRole.PREPOSITIONAL_MODIFIER to 1
    )

    /**
     * Parses `grammar_spans_json` safely (`[{"role":"MAIN_VERB","char_span":[10,18]}]` or `{"role":"...","start":10,"end":18}`).
     * Clamps every span strictly within [0, sentenceLength] and discards malformed/empty ranges.
     */
    fun parseGrammarSpansJson(rawJson: String?, sentenceLength: Int): List<GrammarSpan> {
        if (rawJson.isNullOrBlank() || sentenceLength <= 0) return emptyList()
        return runCatching {
            val arr = listAnyAdapter.fromJson(rawJson) ?: return emptyList()
            buildList {
                arr.forEach { item ->
                    val map = item as? Map<*, *> ?: return@forEach
                    val roleCode = (map["role"] as? String) ?: (map["tag"] as? String) ?: return@forEach
                    val role = GrammarSpanRole.fromCode(roleCode) ?: return@forEach

                    val charSpanList = (map["char_span"] as? List<*>) ?: (map["span"] as? List<*>)
                    val rawStart = if (charSpanList != null && charSpanList.size >= 2) {
                        (charSpanList[0] as? Number)?.toInt()
                    } else {
                        (map["start"] as? Number)?.toInt()
                    }
                    val rawEnd = if (charSpanList != null && charSpanList.size >= 2) {
                        (charSpanList[1] as? Number)?.toInt()
                    } else {
                        (map["end"] as? Number)?.toInt()
                    }

                    if (rawStart != null && rawEnd != null) {
                        val clampedStart = rawStart.coerceIn(0, sentenceLength)
                        val clampedEnd = rawEnd.coerceIn(0, sentenceLength)
                        if (clampedStart < clampedEnd) {
                            add(GrammarSpan(role = role, start = clampedStart, end = clampedEnd))
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun serializeGrammarSpansJson(spans: List<GrammarSpan>): String {
        if (spans.isEmpty()) return "[]"
        val payload = spans
            .filter { it.start >= 0 && it.end > it.start }
            .map { span ->
                mapOf(
                    "role" to span.role.code,
                    "char_span" to listOf(span.start, span.end)
                )
            }
        return runCatching { listAnyAdapter.toJson(payload) }.getOrDefault("[]")
    }

    /**
     * Lightweight, deterministic fallback detector so bundled training examples and user-imported
     * sentences always expose accurate `grammar_spans` even when upstream NLP spans are absent.
     */
    fun detectGrammarSpans(sentence: String): List<GrammarSpan> {
        val text = sentence
        if (text.length < 8) return emptyList()
        val found = mutableListOf<GrammarSpan>()

        // 1. Main verb
        val verbMatch = MAIN_VERB_REGEX.find(text)
        if (verbMatch != null) {
            val vStart = verbMatch.range.first
            val vEnd = verbMatch.range.last + 1
            found += GrammarSpan(GrammarSpanRole.MAIN_VERB, vStart, vEnd)

            // 2. Subject chunk precedes main verb (trimmed to clean token boundaries)
            val prefix = text.substring(0, vStart)
            val lastClauseBreak = maxOf(prefix.lastIndexOf(','), prefix.lastIndexOf(';'))
            val rawSubjStart = if (lastClauseBreak >= 0) lastClauseBreak + 1 else 0
            val subjSlice = text.substring(rawSubjStart, vStart)
            val leadSpaces = subjSlice.indexOfFirst { !it.isWhitespace() }
            val trimmedSubj = subjSlice.trim()
            if (leadSpaces >= 0 && trimmedSubj.length in 2..48) {
                val sStart = rawSubjStart + leadSpaces
                val sEnd = sStart + trimmedSubj.length
                if (sStart < sEnd) {
                    found += GrammarSpan(GrammarSpanRole.SUBJECT_CHUNK, sStart, sEnd)
                }
            }
        }

        // 3. Relative / subordinate clause
        RELATIVE_CLAUSE_REGEX.find(text)?.let { m ->
            val end = trimTrailingPunctuation(text, m.range.first, m.range.last + 1)
            if (end > m.range.first) {
                found += GrammarSpan(GrammarSpanRole.RELATIVE_CLAUSE, m.range.first, end)
            }
        }

        // 4. Participle phrase
        PARTICIPLE_REGEX.find(text)?.groups?.get(1)?.let { g ->
            val end = trimTrailingPunctuation(text, g.range.first, g.range.last + 1)
            if (end > g.range.first) {
                found += GrammarSpan(GrammarSpanRole.PARTICIPLE_PHRASE, g.range.first, end)
            }
        }

        // 5. Parallel unit
        PARALLEL_REGEX.find(text)?.let { m ->
            val end = trimTrailingPunctuation(text, m.range.first, m.range.last + 1)
            if (end > m.range.first) {
                found += GrammarSpan(GrammarSpanRole.PARALLEL_UNIT, m.range.first, end)
            }
        }

        // 6. Prepositional modifier (up to 2 non-overlapping)
        PREP_MODIFIER_REGEX.findAll(text).take(2).forEach { m ->
            val end = trimTrailingPunctuation(text, m.range.first, m.range.last + 1)
            if (end > m.range.first) {
                found += GrammarSpan(GrammarSpanRole.PREPOSITIONAL_MODIFIER, m.range.first, end)
            }
        }

        return found
            .mapNotNull { span ->
                val s = span.start.coerceIn(0, text.length)
                val e = span.end.coerceIn(0, text.length)
                if (s < e) span.copy(start = s, end = e) else null
            }
            .distinctBy { Triple(it.role, it.start, it.end) }
            .sortedWith(compareBy<GrammarSpan> { it.start }.thenBy { it.end })
    }

    /**
     * Resolves effective spans: uses stored `grammarSpans` when present, otherwise computes fallback spans.
     */
    fun resolveEffectiveSpans(sentence: String, storedSpans: List<GrammarSpan>): List<GrammarSpan> {
        val validStored = storedSpans.mapNotNull { span ->
            val s = span.start.coerceIn(0, sentence.length)
            val e = span.end.coerceIn(0, sentence.length)
            if (s < e) span.copy(start = s, end = e) else null
        }
        return validStored.ifEmpty { detectGrammarSpans(sentence) }
    }

    /**
     * Conflict-free Atomic Interval Partitioning:
     * Splits `sentence` along all clamped boundary points of active `grammarSpans` and `targetSpan`.
     * Guarantees:
     *  - Complete coverage of [0, sentence.length) with zero overlaps and zero gaps
     *  - Cloze target range is emitted as a single indivisible segment when `clozeEnabled && !clozeRevealed`
     *    so the blank chip never splits mid-word even if a grammar span partially overlaps it.
     */
    fun partitionSentenceSegments(
        sentence: String,
        grammarSpans: List<GrammarSpan>,
        activeRoles: Set<GrammarSpanRole>,
        targetSpan: Pair<Int, Int>,
        showGrammarSpans: Boolean,
        clozeEnabled: Boolean,
        clozeRevealed: Boolean
    ): List<RenderedSentenceSegment> {
        if (sentence.isEmpty()) return emptyList()
        val len = sentence.length

        val validTarget = if (targetSpan.first in 0 until len && targetSpan.second in (targetSpan.first + 1)..len) {
            targetSpan.first to targetSpan.second
        } else {
            null
        }

        val activeSpans = if (showGrammarSpans && activeRoles.isNotEmpty()) {
            grammarSpans.mapNotNull { span ->
                if (span.role !in activeRoles) return@mapNotNull null
                val s = span.start.coerceIn(0, len)
                val e = span.end.coerceIn(0, len)
                if (s < e) span.copy(start = s, end = e) else null
            }
        } else {
            emptyList()
        }

        val boundaries = sortedSetOf(0, len)
        validTarget?.let { (ts, te) ->
            boundaries += ts
            boundaries += te
        }
        activeSpans.forEach { span ->
            // Avoid splitting the interior of a masked Cloze blank token
            val isMaskedTarget = validTarget != null && clozeEnabled && !clozeRevealed
            if (!isMaskedTarget || span.start !in (validTarget.first + 1) until validTarget.second) {
                boundaries += span.start
            }
            if (!isMaskedTarget || span.end !in (validTarget.first + 1) until validTarget.second) {
                boundaries += span.end
            }
        }

        val pts = boundaries.toList()
        val segments = mutableListOf<RenderedSentenceSegment>()
        for (i in 0 until pts.size - 1) {
            val segStart = pts[i]
            val segEnd = pts[i + 1]
            if (segStart >= segEnd) continue

            val isTarget = validTarget != null && segStart >= validTarget.first && segEnd <= validTarget.second
            val overlappingRoles = activeSpans
                .filter { it.start < segEnd && it.end > segStart }
                .map { it.role }
                .distinct()
                .sortedByDescending { ROLE_PRIORITY[it] ?: 0 }

            segments += RenderedSentenceSegment(
                start = segStart,
                end = segEnd,
                text = sentence.substring(segStart, segEnd),
                primaryGrammarRole = overlappingRoles.firstOrNull(),
                allGrammarRoles = overlappingRoles,
                isTargetToken = isTarget,
                isClozeMasked = isTarget && clozeEnabled && !clozeRevealed,
                isClozeRevealed = isTarget && clozeEnabled && clozeRevealed
            )
        }
        return segments
    }

    private fun trimTrailingPunctuation(text: String, start: Int, end: Int): Int {
        var cur = end.coerceAtMost(text.length)
        while (cur > start && text[cur - 1] in charArrayOf(' ', '.', ',', ';', ':', '!', '?', '"', '\'')) {
            cur--
        }
        return cur
    }
}
