package com.example.engine

import com.example.model.CefrLevel
import com.example.model.CorpusSummaryMetrics
import com.example.model.CorpusWordAnalytics
import com.example.model.CurriculumGroup2022
import com.example.model.ExamCorpusWordEntry
import com.example.model.ExamOccurrenceRecord
import com.example.model.ExamPriorityTier
import com.example.model.ExamSection
import com.example.model.ExamType
import com.example.model.GrammarCategory
import com.example.model.GriesDispersionMetrics
import com.example.model.RecurrenceMetrics
import com.example.model.VocabDifficultyTier
import com.example.model.WordAnalyticsFlags
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Deterministic 10-Year Corpus Analytics, Gries DP_norm Dispersion, 5-Tier Classifier,
 * FTS5 Query Sanitizer, Lemmatization & High-Speed Search Engine (v2.0).
 */
object ExamCorpusAnalyticsEngine {

    const val DEFAULT_START_YEAR = 2017
    const val DEFAULT_END_YEAR = 2026
    const val LATEST_CSAT_ACADEMIC_YEAR = 2027

    private val FTS_SPECIAL_REGEX = Regex("""["'*():^~-]+""")

    private val irregularLemmaMap: Map<String, String> = mapOf(
        "children" to "child",
        "people" to "person",
        "went" to "go",
        "gone" to "go",
        "brought" to "bring",
        "bought" to "buy",
        "thought" to "think",
        "taught" to "teach",
        "caught" to "catch",
        "found" to "find",
        "kept" to "keep",
        "led" to "lead",
        "meant" to "mean",
        "met" to "meet",
        "paid" to "pay",
        "said" to "say",
        "seen" to "see",
        "saw" to "see",
        "spoken" to "speak",
        "spoke" to "speak",
        "taken" to "take",
        "took" to "take",
        "written" to "write",
        "wrote" to "write",
        "better" to "good",
        "best" to "good",
        "worse" to "bad",
        "worst" to "bad",
        "conversations" to "conversation",
        "crucially" to "crucial",
        "consistently" to "consistent",
        "accurately" to "accurate",
        "adequately" to "adequate",
        "appropriately" to "appropriate",
        "inevitably" to "inevitable",
        "ambiguously" to "ambiguous",
        "coherently" to "coherent",
        "deliberately" to "deliberate",
        "pragmatically" to "pragmatic",
        "empirically" to "empirical",
        "resilience" to "resilient",
        "ubiquity" to "ubiquitous",
        "awareness" to "aware",
        "availability" to "available",
        "consistency" to "consistent",
        "accuracy" to "accurate",
        "capability" to "capable",
        "commitment" to "commit",
        "comparison" to "compare",
        "analysis" to "analyze",
        "adaptation" to "adapt",
        "accomplishment" to "accomplish",
        "concentration" to "concentrate",
        "confirmation" to "confirm",
        "combination" to "combine",
        "assumption" to "assume",
        "enhancement" to "enhance",
        "emphasis" to "emphasize",
        "distinction" to "distinguish",
        "demonstration" to "demonstrate",
        "elimination" to "eliminate",
        "emergence" to "emerge",
        "compensation" to "compensate",
        "acknowledgment" to "acknowledge",
        "attribution" to "attribute",
        "collaboration" to "collaborate",
        "confrontation" to "confront",
        "cultivation" to "cultivate",
        "accumulation" to "accumulate",
        "allocation" to "allocate",
        "comprehension" to "comprehend",
        "conception" to "conceive",
        "confinement" to "confine",
        "contradiction" to "contradict",
        "deprivation" to "deprive",
        "derivation" to "derive",
        "elaboration" to "elaborate",
        "mitigation" to "mitigate",
        "fluctuation" to "fluctuate",
        "alleviation" to "alleviate",
        "articulation" to "articulate"
    )

    /**
     * Sanitizes raw user search input before passing to SQLite FTS5 `MATCH ?`
     * to prevent `fts5: syntax error near "-"` and operator injection crashes.
     */
    fun sanitizeFtsQuery(raw: String): String {
        val tokens = FTS_SPECIAL_REGEX
            .replace(raw.trim(), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" AND ") { "\"$it\"*" }
    }

    fun normalizeToken(raw: String): String =
        raw.trim()
            .lowercase(Locale.US)
            .replace(Regex("^[^a-z0-9가-힣]+|[^a-z0-9가-힣]+$"), "")

    /**
     * Resolves exact [start, end) character span of `surfaceForm` inside `sentence`
     * for highlighted display and Cloze blank masking.
     */
    fun resolveCharSpan(sentence: String, surfaceForm: String): Pair<Int, Int> {
        if (sentence.isBlank() || surfaceForm.isBlank()) return -1 to -1
        val escaped = Regex.escape(surfaceForm.trim())
        val wordBoundaryRegex = Regex("\\b$escaped\\b", RegexOption.IGNORE_CASE)
        val match = wordBoundaryRegex.find(sentence)
            ?: Regex(escaped, RegexOption.IGNORE_CASE).find(sentence)
        return if (match != null) match.range.first to (match.range.last + 1) else -1 to -1
    }

    /**
     * Computes Gries's Deviation of Proportions (DP), normalized DP_norm, and uniformity (1 - DP_norm).
     */
    fun computeGriesDp(
        countsPerPart: List<Int>,
        tokensPerPart: List<Int>
    ): GriesDispersionMetrics {
        if (countsPerPart.isEmpty() || countsPerPart.size != tokensPerPart.size) {
            return GriesDispersionMetrics(dp = 1f, dpNorm = 1f, uniformity = 0f)
        }
        val totalAf = countsPerPart.sumOf { it.coerceAtLeast(0) }.toDouble()
        val totalTokens = tokensPerPart.sumOf { it.coerceAtLeast(0) }.toDouble()
        if (totalAf <= 0.0 || totalTokens <= 0.0) {
            return GriesDispersionMetrics(dp = 1f, dpNorm = 1f, uniformity = 0f)
        }
        val shares = tokensPerPart.map { it.coerceAtLeast(0).toDouble() / totalTokens }
        val observed = countsPerPart.map { it.coerceAtLeast(0).toDouble() / totalAf }
        var sumAbsDiff = 0.0
        for (i in shares.indices) {
            sumAbsDiff += abs(observed[i] - shares[i])
        }
        val dp = 0.5 * sumAbsDiff
        val minShare = shares.minOrNull() ?: 0.0
        val denom = max(1e-9, 1.0 - minShare)
        val dpNorm = min(1.0, max(0.0, dp / denom))
        val uniformity = min(1.0, max(0.0, 1.0 - dpNorm))
        return GriesDispersionMetrics(
            dp = round4(dp),
            dpNorm = round4(dpNorm),
            uniformity = round4(uniformity)
        )
    }

    /**
     * Computes CSAT recurrence gaps (`meanGapYr`, `medianGapYr`, `maxGapYr`, `lastCsatYear`, `overdueYr`).
     */
    fun computeRecurrenceMetrics(
        csatYears: List<Int>,
        latestYear: Int = LATEST_CSAT_ACADEMIC_YEAR
    ): RecurrenceMetrics {
        val sorted = csatYears.distinct().sorted()
        if (sorted.isEmpty()) return RecurrenceMetrics()
        val lastYr = sorted.last()
        val overdue = max(0, latestYear - lastYr)
        if (sorted.size < 2) {
            return RecurrenceMetrics(lastCsatYear = lastYr, overdueYr = overdue)
        }
        val gaps = sorted.zipWithNext { a, b -> (b - a).coerceAtLeast(1) }
        val sortedGaps = gaps.sorted()
        val mid = sortedGaps.size / 2
        val median = if (sortedGaps.size % 2 == 1) {
            sortedGaps[mid].toFloat()
        } else {
            (sortedGaps[mid - 1] + sortedGaps[mid]) / 2f
        }
        val mean = (gaps.average() * 100.0).roundToInt() / 100f
        return RecurrenceMetrics(
            meanGapYr = mean,
            medianGapYr = (median * 100f).roundToInt() / 100f,
            maxGapYr = sortedGaps.last(),
            lastCsatYear = lastYr,
            overdueYr = overdue
        )
    }

    /**
     * 5-Tier Difficulty Classifier matching `pipeline/src/04_export_tiered_json.py`.
     * Guarantees zero contamination of footnote/excluded words into Tier S or Tier A.
     */
    fun classifyDifficultyTier(
        uniformity: Float,
        csatYearsCount: Int,
        meanGapYr: Float?,
        mockToCsatTransferHits: Int,
        curriculumGroup: CurriculumGroup2022,
        flags: WordAnalyticsFlags,
        keynessRecent3Yr: Float = 0f
    ): VocabDifficultyTier {
        if (flags.isFootnoteEver || flags.isExcludedEntity) {
            return VocabDifficultyTier.TIER_D_FOOTNOTE
        }
        if (uniformity >= 0.70f ||
            ((curriculumGroup == CurriculumGroup2022.G1_ELEM_800 ||
                curriculumGroup == CurriculumGroup2022.G2_MID_H1_1200) &&
                csatYearsCount >= 7 &&
                meanGapYr != null && meanGapYr <= 1.5f)
        ) {
            return VocabDifficultyTier.TIER_S_CORE
        }
        if ((uniformity in 0.40f..0.6999f) ||
            (csatYearsCount in 3..6) ||
            (mockToCsatTransferHits >= 2 && curriculumGroup != CurriculumGroup2022.OOV)
        ) {
            return VocabDifficultyTier.TIER_A_CYCLICAL
        }
        if ((curriculumGroup == CurriculumGroup2022.OOV && uniformity >= 0.15f) ||
            flags.isNewRecent3Yr ||
            keynessRecent3Yr >= 6.63f
        ) {
            return VocabDifficultyTier.TIER_B_ADVANCED_NEW
        }
        return VocabDifficultyTier.TIER_C_BURSTY
    }

    fun computeDifficultyScore(
        curriculumGroup: CurriculumGroup2022,
        uniformity: Float,
        isPolysemous: Boolean
    ): Float {
        val raw = curriculumGroup.baseDifficulty + (1f - uniformity) * 18f + (if (isPolysemous) 5f else 0f)
        return ((min(100f, max(0f, raw)) * 10f).roundToInt() / 10f)
    }

    fun lemmatize(
        rawToken: String,
        knownInflectionIndex: Map<String, String> = emptyMap(),
        knownLemmas: Set<String> = emptySet()
    ): String {
        val token = normalizeToken(rawToken)
        if (token.isBlank()) return ""
        knownInflectionIndex[token]?.let { return it }
        if (token in knownLemmas) return token
        irregularLemmaMap[token]?.let { return it }

        if (token.length <= 3) return token

        val candidates = buildList {
            if (token.endsWith("ies") && token.length > 4) add(token.dropLast(3) + "y")
            if (token.endsWith("ied") && token.length > 4) add(token.dropLast(3) + "y")
            if (token.endsWith("ing") && token.length > 5) {
                val stem = token.dropLast(3)
                add(stem)
                add(stem + "e")
                if (stem.length >= 2 && stem.last() == stem[stem.length - 2]) {
                    add(stem.dropLast(1))
                }
            }
            if (token.endsWith("ed") && token.length > 4) {
                val stem = token.dropLast(2)
                add(stem)
                add(stem + "e")
                if (stem.length >= 2 && stem.last() == stem[stem.length - 2]) {
                    add(stem.dropLast(1))
                }
            }
            if (token.endsWith("es") && token.length > 4) {
                add(token.dropLast(2))
                add(token.dropLast(1))
            }
            if (token.endsWith("s") && !token.endsWith("ss") && token.length > 3) {
                add(token.dropLast(1))
            }
            if (token.endsWith("ly") && token.length > 5) {
                val stem = token.dropLast(2)
                add(stem)
                if (stem.endsWith("i")) add(stem.dropLast(1) + "y")
            }
        }
        if (knownLemmas.isNotEmpty()) {
            candidates.firstOrNull { it in knownLemmas }?.let { return it }
        }
        return candidates.firstOrNull() ?: token
    }

    fun generateInflections(lemma: String, pos: String): List<String> {
        val base = lemma.trim().lowercase(Locale.US)
        if (base.isBlank()) return emptyList()
        val forms = linkedSetOf<String>()
        val isVerb = pos.contains("v.")
        val isNoun = pos.contains("n.")
        val isAdj = pos.contains("adj.")

        if (isVerb) {
            forms += when {
                base.endsWith("y") && base.length > 2 && base[base.length - 2] !in "aeiou" ->
                    base.dropLast(1) + "ies"
                base.endsWith("s") || base.endsWith("sh") || base.endsWith("ch") || base.endsWith("x") || base.endsWith("z") ->
                    base + "es"
                else -> base + "s"
            }
            forms += when {
                base.endsWith("e") -> base + "d"
                base.endsWith("y") && base.length > 2 && base[base.length - 2] !in "aeiou" ->
                    base.dropLast(1) + "ied"
                base.endsWith("t") && base.length <= 6 && base.endsWith("mit") ->
                    base + "ted"
                else -> base + "ed"
            }
            forms += when {
                base.endsWith("e") && !base.endsWith("ee") -> base.dropLast(1) + "ing"
                base.endsWith("mit") -> base + "ting"
                else -> base + "ing"
            }
        }
        if (isNoun) {
            forms += when {
                base.endsWith("y") && base.length > 2 && base[base.length - 2] !in "aeiou" ->
                    base.dropLast(1) + "ies"
                base.endsWith("s") || base.endsWith("x") || base.endsWith("ch") || base.endsWith("sh") ->
                    base + "es"
                else -> base + "s"
            }
        }
        if (isAdj) {
            forms += when {
                base.endsWith("le") -> base.dropLast(1) + "y"
                base.endsWith("ic") -> base + "ally"
                base.endsWith("y") -> base.dropLast(1) + "ily"
                else -> base + "ly"
            }
        }
        irregularLemmaMap.entries
            .filter { it.value == base }
            .forEach { forms += it.key }

        return forms.filter { it != base }.take(6)
    }

    /**
     * Computes 10-year quantitative statistics, Gries DP_norm uniformity, recurrence metrics,
     * and both v1 ExamPriorityTier & v2 5-Tier VocabDifficultyTier.
     */
    fun computeWordAnalytics(
        occurrences: List<ExamOccurrenceRecord>,
        cefr: CefrLevel,
        isMoeBasic: Boolean,
        grade: Int,
        startYear: Int = DEFAULT_START_YEAR,
        endYear: Int = DEFAULT_END_YEAR,
        curriculumGroup: CurriculumGroup2022 = when {
            cefr == CefrLevel.A2 -> CurriculumGroup2022.G1_ELEM_800
            isMoeBasic && grade <= 2 -> CurriculumGroup2022.G2_MID_H1_1200
            cefr == CefrLevel.B2 || grade == 2 -> CurriculumGroup2022.G3_H2_H3_1000
            else -> CurriculumGroup2022.OOV
        },
        isFootnoteEver: Boolean = false,
        isExcludedEntity: Boolean = false,
        isPolysemous: Boolean = false,
        isAwl: Boolean = cefr == CefrLevel.B2 || cefr == CefrLevel.C1
    ): CorpusWordAnalytics {
        val total = occurrences.size
        val csatOccurrences = occurrences.filter { it.examType == ExamType.CSAT }
        val csatCount = csatOccurrences.size
        val mockCount = total - csatCount
        val hasCarryover = csatCount > 0 && mockCount > 0
        val carryoverRate = if (total == 0) 0f else {
            if (hasCarryover) (csatCount.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
        }

        val sortedYears = occurrences.map { it.year }.distinct().sorted()
        val spanYears = (endYear - startYear + 1).coerceAtLeast(1)
        val dispersion = (sortedYears.size.toFloat() / spanYears.toFloat()).coerceIn(0f, 1f)

        val avgGap = if (sortedYears.size >= 2) {
            val gaps = sortedYears.zipWithNext { a, b -> (b - a).coerceAtLeast(1) }
            gaps.average().toFloat()
        } else {
            spanYears.toFloat()
        }

        val firstYear = sortedYears.firstOrNull() ?: endYear
        val lastYear = sortedYears.lastOrNull() ?: endYear

        val isEvergreen = sortedYears.size >= 3 && avgGap <= 2.75f
        val isNewlyEmergent = sortedYears.isNotEmpty() && firstYear >= (endYear - 2) && csatCount <= 1

        // Compute Gries DP_norm over 10-year bins
        val yearBins = (startYear..endYear).toList()
        val countsPerYear = yearBins.map { yr -> occurrences.count { it.year == yr } }
        val tokensPerYear = List(yearBins.size) { 6150 } // ~6,150 tokens/year normalized bin weight
        val gries = computeGriesDp(countsPerYear, tokensPerYear)

        val csatYears = csatOccurrences.map { it.year }.distinct().sorted()
        val mockYears = occurrences.filter { it.examType != ExamType.CSAT }.map { it.year }.toSet()
        val transferHits = csatYears.count { it in mockYears }.coerceAtLeast(if (hasCarryover) 1 else 0)
        val recurrence = computeRecurrenceMetrics(
            csatYears = if (csatYears.isNotEmpty()) csatYears else sortedYears,
            latestYear = endYear
        )

        val listeningFreq = occurrences.count { it.section == ExamSection.LISTENING }
        val readingFreq = total - listeningFreq
        val listeningRatio = if (total == 0) 0f else ((listeningFreq.toFloat() / total.toFloat()) * 1000f).roundToInt() / 1000f

        val isOverdue = recurrence.meanGapYr != null && recurrence.overdueYr != null &&
            recurrence.overdueYr >= recurrence.meanGapYr

        val flags = WordAnalyticsFlags(
            isPolysemous = isPolysemous,
            isListeningHeavy = listeningRatio >= 0.45f,
            isNewRecent3Yr = isNewlyEmergent,
            isOverdue = isOverdue,
            isFootnoteEver = isFootnoteEver,
            isExcludedEntity = isExcludedEntity
        )

        val difficultyTier = classifyDifficultyTier(
            uniformity = gries.uniformity,
            csatYearsCount = csatYears.size,
            meanGapYr = recurrence.meanGapYr,
            mockToCsatTransferHits = transferHits,
            curriculumGroup = curriculumGroup,
            flags = flags
        )
        val difficultyScore = computeDifficultyScore(curriculumGroup, gries.uniformity, isPolysemous)

        // Composite priority score (0..100)
        val freqComponent = ((total.coerceAtMost(8) / 8f) * 35f)
        val dispersionComponent = (dispersion * 25f)
        val carryoverComponent = if (hasCarryover) 15f else if (csatCount > 0) 9f else 4f
        val cefrComponent = when (cefr) {
            CefrLevel.C1 -> 15f
            CefrLevel.B2 -> 13f
            CefrLevel.B1 -> 10f
            CefrLevel.A2 -> 8f
        }
        val compactRecurrenceBonus = when {
            isEvergreen -> 10f
            isMoeBasic -> 7f
            else -> 4f
        }
        val priorityScore = (freqComponent + dispersionComponent + carryoverComponent + cefrComponent + compactRecurrenceBonus)
            .roundToInt()
            .coerceIn(0, 100)

        val tier = when {
            isNewlyEmergent -> ExamPriorityTier.NEWLY_EMERGENT
            (cefr == CefrLevel.C1 || grade == 3) && !isMoeBasic && priorityScore >= 58 -> ExamPriorityTier.HIGH_DISCRIMINATION
            priorityScore >= 68 && hasCarryover && dispersion >= 0.30f -> ExamPriorityTier.MUST_KNOW_CORE
            isEvergreen -> ExamPriorityTier.EVERGREEN_REPEAT
            cefr == CefrLevel.C1 || grade == 3 -> ExamPriorityTier.HIGH_DISCRIMINATION
            else -> ExamPriorityTier.MUST_KNOW_CORE
        }

        return CorpusWordAnalytics(
            totalFrequency = total,
            csatFrequency = csatCount,
            mockFrequency = mockCount,
            hasMockToCsatCarryover = hasCarryover,
            mockToCsatCarryoverRate = carryoverRate,
            yearDispersion = dispersion,
            avgRecurrenceGapYears = avgGap,
            firstSeenYear = firstYear,
            lastSeenYear = lastYear,
            isEvergreenCore = isEvergreen,
            isNewlyEmergent = isNewlyEmergent,
            priorityScore = priorityScore,
            priorityTier = tier,
            difficultyTier = difficultyTier,
            difficultyScore = difficultyScore,
            griesDispersion = gries,
            recurrence = recurrence,
            readingFrequency = readingFreq,
            listeningFrequency = listeningFreq,
            listeningRatio = listeningRatio,
            mockToCsatTransferHits = transferHits,
            flags = flags,
            curriculumGroup = curriculumGroup,
            isAwl = isAwl
        )
    }

    fun computeCorpusSummary(
        entries: List<ExamCorpusWordEntry>,
        grammarSentenceCount: Int
    ): CorpusSummaryMetrics {
        if (entries.isEmpty()) {
            return CorpusSummaryMetrics(0, 0, 0f, 0, 0, 0, 0, 0, grammarSentenceCount)
        }
        val totalOccurrences = entries.sumOf { it.analytics.totalFrequency }
        val avgDispersion = entries.map { it.analytics.yearDispersion }.average().toFloat()
        val avgUniformity = entries.map { it.analytics.griesDispersion.uniformity }.average().toFloat()
        val mockAppearing = entries.count { it.analytics.mockFrequency > 0 }.coerceAtLeast(1)
        val carryoverCount = entries.count { it.analytics.hasMockToCsatCarryover }
        val carryoverPercent = ((carryoverCount.toFloat() / mockAppearing.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        val tierCounts = VocabDifficultyTier.entries.associateWith { tier ->
            entries.count { it.analytics.difficultyTier == tier }
        }

        return CorpusSummaryMetrics(
            totalLemmas = entries.size,
            totalOccurrences = totalOccurrences,
            avgDispersion = avgDispersion,
            mockToCsatCarryoverPercent = carryoverPercent,
            mustKnowCount = entries.count { it.analytics.priorityTier == ExamPriorityTier.MUST_KNOW_CORE },
            highDiscriminationCount = entries.count { it.analytics.priorityTier == ExamPriorityTier.HIGH_DISCRIMINATION },
            evergreenCount = entries.count { it.analytics.priorityTier == ExamPriorityTier.EVERGREEN_REPEAT },
            newlyEmergentCount = entries.count { it.analytics.priorityTier == ExamPriorityTier.NEWLY_EMERGENT },
            grammarSentenceCount = grammarSentenceCount,
            tierCounts = tierCounts,
            avgUniformity = avgUniformity
        )
    }

    private fun round4(v: Double): Float = ((v * 10000.0).roundToInt() / 10000.0).toFloat()
}

/**
 * O(1) Inverted Index over Lemmas, Inflections, Collocations, 5-Tiers, CEFR & Grammar Tags.
 */
class ExamCorpusSearchIndex(private val corpusEntries: List<ExamCorpusWordEntry>) {

    private val byWordId: Map<String, ExamCorpusWordEntry> = corpusEntries.associateBy { it.wordId }
    private val byLemma: Map<String, ExamCorpusWordEntry> =
        corpusEntries.associateBy { it.lemma.lowercase(Locale.US) }
    private val inflectionToLemma: Map<String, String> = buildMap {
        corpusEntries.forEach { entry ->
            val lowerLemma = entry.lemma.lowercase(Locale.US)
            put(lowerLemma, lowerLemma)
            entry.inflections.forEach { form ->
                put(form.lowercase(Locale.US), lowerLemma)
            }
            entry.surfaceVariants.forEach { sv ->
                put(sv.form.lowercase(Locale.US), lowerLemma)
            }
            entry.occurrences.forEach { occ ->
                val normSurface = ExamCorpusAnalyticsEngine.normalizeToken(occ.surfaceForm)
                if (normSurface.isNotBlank()) put(normSurface, lowerLemma)
            }
        }
    }
    private val knownLemmas: Set<String> = byLemma.keys

    fun findByWordId(wordId: String): ExamCorpusWordEntry? = byWordId[wordId]

    fun findByTokenOrLemma(rawToken: String): ExamCorpusWordEntry? {
        val lemma = ExamCorpusAnalyticsEngine.lemmatize(
            rawToken = rawToken,
            knownInflectionIndex = inflectionToLemma,
            knownLemmas = knownLemmas
        )
        return byLemma[lemma]
    }

    fun resolveNormalizedLemma(rawQuery: String): String? {
        val clean = ExamCorpusAnalyticsEngine.normalizeToken(rawQuery)
        if (clean.isBlank()) return null
        val lemma = ExamCorpusAnalyticsEngine.lemmatize(clean, inflectionToLemma, knownLemmas)
        return if (lemma in knownLemmas && lemma != clean) lemma else null
    }

    fun query(
        rawQuery: String = "",
        tierFilter: ExamPriorityTier? = null,
        difficultyTierFilter: VocabDifficultyTier? = null,
        includeFootnoteTierD: Boolean = false,
        cefrFilter: CefrLevel? = null,
        grammarFilter: GrammarCategory? = null,
        onlyMockToCsatCarryover: Boolean = false,
        onlyRecent3YrNew: Boolean = false,
        onlyListeningHeavy: Boolean = false,
        ftsMatchedWordIds: Set<String>? = null
    ): List<ExamCorpusWordEntry> {
        val q = rawQuery.trim()
        val normalizedLemma = if (q.isNotBlank()) {
            ExamCorpusAnalyticsEngine.lemmatize(q, inflectionToLemma, knownLemmas)
        } else {
            ""
        }
        val lowerQ = q.lowercase(Locale.US)

        return corpusEntries.asSequence()
            .filter { entry ->
                if (difficultyTierFilter != null) {
                    entry.analytics.difficultyTier == difficultyTierFilter
                } else {
                    includeFootnoteTierD || !entry.analytics.difficultyTier.hiddenInDefaultDeck || lowerQ.isNotBlank()
                }
            }
            .filter { entry -> tierFilter == null || entry.analytics.priorityTier == tierFilter }
            .filter { entry -> cefrFilter == null || entry.cefr == cefrFilter }
            .filter { entry -> grammarFilter == null || grammarFilter in entry.grammarTags }
            .filter { entry -> !onlyMockToCsatCarryover || entry.analytics.hasMockToCsatCarryover }
            .filter { entry -> !onlyRecent3YrNew || entry.analytics.flags.isNewRecent3Yr }
            .filter { entry -> !onlyListeningHeavy || entry.analytics.flags.isListeningHeavy }
            .filter { entry ->
                if (lowerQ.isBlank()) true
                else {
                    (ftsMatchedWordIds != null && entry.wordId in ftsMatchedWordIds) ||
                        entry.lemma.lowercase(Locale.US).contains(lowerQ) ||
                        entry.lemma.lowercase(Locale.US) == normalizedLemma ||
                        entry.meaningKo.contains(q, ignoreCase = true) ||
                        entry.inflections.any { it.lowercase(Locale.US).contains(lowerQ) } ||
                        entry.collocations.any { it.lowercase(Locale.US).contains(lowerQ) }
                }
            }
            .sortedWith(
                compareByDescending<ExamCorpusWordEntry> {
                    if (normalizedLemma.isNotBlank() && it.lemma.equals(normalizedLemma, ignoreCase = true)) 1 else 0
                }.thenByDescending { it.analytics.priorityScore }
                    .thenByDescending { it.analytics.totalFrequency }
            )
            .toList()
    }
}
