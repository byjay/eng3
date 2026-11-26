package com.example.data

import android.content.Context
import android.net.Uri
import com.example.engine.ExamCorpusAnalyticsEngine
import com.example.engine.ExamCorpusSearchIndex
import com.example.engine.GrammarSpanEngine
import com.example.model.CefrLevel
import com.example.model.CorpusDataProvenance
import com.example.model.CorpusRuntimeMode
import com.example.model.CorpusSummaryMetrics
import com.example.model.CurriculumGroup2022
import com.example.model.ExamCorpusWordEntry
import com.example.model.ExamOccurrenceRecord
import com.example.model.ExamSection
import com.example.model.ExamType
import com.example.model.GrammarCategory
import com.example.model.GrammarFocusSentence
import com.example.model.SurfaceVariantCount
import com.example.model.VocabDifficultyTier
import com.example.model.VocabWord
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.util.Locale

data class ExamCorpusSnapshot(
    val entries: List<ExamCorpusWordEntry>,
    val grammarSentences: List<GrammarFocusSentence>,
    val searchIndex: ExamCorpusSearchIndex,
    val summary: CorpusSummaryMetrics,
    val provenanceNoticeKo: String,
    val runtimeMode: CorpusRuntimeMode = CorpusRuntimeMode.CURATED_ORIGINAL
)

private data class TierOverrideMeta(
    val tier: VocabDifficultyTier,
    val curriculumGroup: CurriculumGroup2022,
    val isAwl: Boolean,
    val uniformityOverride: Float?,
    val listeningRatioOverride: Float?,
    val isPolysemous: Boolean,
    val isNewRecent3Yr: Boolean,
    val isFootnoteEver: Boolean
)

/**
 * Partitioned 5-Tier JSON + Native Android SQLite/FTS5 + Hybrid BYOD Repository (v2.0).
 *
 * - Loads v2.0 `assets/corpus/manifest.json` and 5 split tier JSON files (`words/tier_*.json`)
 * - Seeds native Android `CorpusSqliteFtsStore` (`words`, `sentences`, `occurrences`, `sentences_fts`)
 * - Supports copyright-safe Hybrid BYOD mode: users can import their own local `.sqlite`,
 *   `.sqlite.gz`, or `.json` file via Android SAF without broad storage permissions, and restore
 *   the built-in copyright-safe curated mode at any time.
 */
object ExamCorpusRepository {

    private const val MANIFEST_V2_PATH = "corpus/manifest.json"
    private const val MANIFEST_V1_PATH = "corpus/corpus_manifest.json"
    private const val GRAMMAR_ASSET_PATH = "corpus/grammar_focus_patterns.json"
    private const val BYOD_JSON_CACHE_NAME = "byod_occurrences_override.json"
    private const val MAX_IMPORT_JSON_BYTES = 10 * 1024 * 1024 // 10MB defensive guard

    private val moshi by lazy { Moshi.Builder().build() }
    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any?>>(type)
    }

    @Volatile
    private var cachedSnapshot: ExamCorpusSnapshot? = null

    @Volatile
    private var byodJsonOverlay: Map<String, List<ExamOccurrenceRecord>> = emptyMap()

    fun invalidateCache() {
        cachedSnapshot = null
    }

    fun resolveRuntimeMode(context: Context?): CorpusRuntimeMode {
        if (context != null && CorpusSqliteFtsStore.hasByodSqlite(context)) {
            return CorpusRuntimeMode.LOCAL_BYOD_SQLITE
        }
        if (byodJsonOverlay.isNotEmpty()) {
            return CorpusRuntimeMode.LOCAL_BYOD_JSON
        }
        if (context != null) {
            val savedJson = File(context.applicationContext.filesDir, BYOD_JSON_CACHE_NAME)
            if (savedJson.exists() && savedJson.length() > 2L) {
                return CorpusRuntimeMode.LOCAL_BYOD_JSON
            }
        }
        return CorpusRuntimeMode.CURATED_ORIGINAL
    }

    fun loadSnapshot(
        context: Context? = null,
        extraWords: List<VocabWord> = emptyList()
    ): ExamCorpusSnapshot {
        if (extraWords.isEmpty()) {
            cachedSnapshot?.let { return it }
        }

        // Load persisted BYOD JSON overlay if present on device
        if (context != null && byodJsonOverlay.isEmpty()) {
            val savedJson = File(context.applicationContext.filesDir, BYOD_JSON_CACHE_NAME)
            if (savedJson.exists() && savedJson.length() in 3L..MAX_IMPORT_JSON_BYTES.toLong()) {
                runCatching {
                    parseExternalOccurrencesJson(savedJson.readText(Charsets.UTF_8)).getOrNull()
                }.getOrNull()?.let { parsed ->
                    if (parsed.isNotEmpty()) byodJsonOverlay = parsed
                }
            }
        }

        val manifestNotice = (readAssetText(context, MANIFEST_V2_PATH) ?: readAssetText(context, MANIFEST_V1_PATH))
            ?.let { raw -> runCatching { mapAdapter.fromJson(raw)?.get("provenanceNoticeKo") as? String }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
            ?: "저작권 안전 창작·통계 모드(대법원 2021다272001 준수): 5-Tier 정량 통계와 자체 창작 예문을 제공하며 개인 로컬 BYOD 임포트를 지원합니다."

        val tierOverrides = loadTierAssetOverrides(context)

        val grammarJsonText = readAssetText(context, GRAMMAR_ASSET_PATH)
        val grammarSentences = if (!grammarJsonText.isNullOrBlank()) {
            parseGrammarPatternsJson(grammarJsonText).ifEmpty { builtInFallbackGrammarSentences() }
        } else {
            builtInFallbackGrammarSentences()
        }

        val grammarByLemma: Map<String, List<GrammarFocusSentence>> = buildMap {
            grammarSentences.forEach { sentence ->
                sentence.linkedLemmas.forEach { rawLemma ->
                    val key = rawLemma.trim().lowercase(Locale.US)
                    val list = (get(key) ?: emptyList()) + sentence
                    put(key, list)
                }
            }
        }

        val combinedWords = (VocabRepository.allWords + extraWords).distinctBy { it.id }
        val entries = combinedWords.mapIndexed { idx, vocabWord ->
            val key = vocabWord.word.trim().lowercase(Locale.US)
            buildWordEntry(
                word = vocabWord,
                index = idx,
                linkedGrammar = grammarByLemma[key].orEmpty(),
                tierOverride = tierOverrides[key],
                byodExtraOccurrences = byodJsonOverlay[key].orEmpty()
            )
        }

        // Seed Android native SQLite+FTS5 store for curated/hybrid local search & paging
        if (context != null) {
            runCatching {
                CorpusSqliteFtsStore.getCurated(context).seedFromEntries(entries)
            }
        }

        val searchIndex = ExamCorpusSearchIndex(entries)
        val summary = ExamCorpusAnalyticsEngine.computeCorpusSummary(entries, grammarSentences.size)
        val runtimeMode = resolveRuntimeMode(context)
        val snapshot = ExamCorpusSnapshot(
            entries = entries,
            grammarSentences = grammarSentences,
            searchIndex = searchIndex,
            summary = summary,
            provenanceNoticeKo = when (runtimeMode) {
                CorpusRuntimeMode.LOCAL_BYOD_SQLITE ->
                    "로컬 BYOD SQLite+FTS5 모드 활성: 기기 내부로 가져온 개인 기출 DB를 밀리초 단위로 조회합니다."
                CorpusRuntimeMode.LOCAL_BYOD_JSON ->
                    "로컬 BYOD JSON 모드 활성: 기기 내부로 가져온 개인 기출 JSON 용례(${byodJsonOverlay.values.sumOf { it.size }}건)가 결합되었습니다."
                CorpusRuntimeMode.CURATED_ORIGINAL -> manifestNotice
            },
            runtimeMode = runtimeMode
        )
        if (extraWords.isEmpty()) {
            cachedSnapshot = snapshot
        }
        return snapshot
    }

    /**
     * Lazy paginated occurrence query (`LIMIT 15 OFFSET ?`) routed to active Android SQLite+FTS5 store
     * with seamless in-memory fallback for JVM tests or words not yet present in an external BYOD DB.
     */
    fun queryOccurrencesPaged(
        context: Context?,
        entry: ExamCorpusWordEntry,
        examTypeFilter: ExamType? = null,
        sectionFilter: ExamSection? = null,
        surfaceFormFilter: String? = null,
        onlyGrammarCore: Boolean = false,
        limit: Int = 15,
        offset: Int = 0
    ): PagedOccurrenceResult {
        if (context != null) {
            val (store, provenance) = CorpusSqliteFtsStore.getActiveStore(context)
            val dbResult = runCatching {
                store.queryOccurrencesPaged(
                    wordId = entry.wordId,
                    lemmaFallback = entry.lemma,
                    examTypeFilter = examTypeFilter,
                    sectionFilter = sectionFilter,
                    surfaceFormFilter = surfaceFormFilter,
                    onlyGrammarCore = onlyGrammarCore,
                    limit = limit,
                    offset = offset,
                    provenance = provenance
                )
            }.getOrNull()
            if (dbResult != null && (dbResult.totalCount > 0 || provenance == CorpusDataProvenance.LOCAL_BYOD_SQLITE)) {
                return dbResult
            }
        }

        val filtered = entry.occurrences.asSequence()
            .filter { examTypeFilter == null || it.examType == examTypeFilter }
            .filter { sectionFilter == null || it.section == sectionFilter }
            .filter { surfaceFormFilter.isNullOrBlank() || it.surfaceForm.equals(surfaceFormFilter, ignoreCase = true) }
            .filter { !onlyGrammarCore || it.grammarCategories.isNotEmpty() }
            .toList()
        val page = filtered.drop(offset.coerceAtLeast(0)).take(limit.coerceIn(1, 50))
        return PagedOccurrenceResult(
            items = page,
            totalCount = filtered.size,
            hasMore = (offset + page.size) < filtered.size,
            sourceLabelKo = CorpusDataProvenance.CURATED_TRAINING_CORPUS.badgeKo
        )
    }

    /**
     * Executes sanitized External-Content FTS5 full-text search on Android.
     */
    fun searchWordIdsByFts(context: Context?, rawQuery: String): Set<String>? {
        if (context == null || rawQuery.trim().length < 2) return null
        val (store, _) = CorpusSqliteFtsStore.getActiveStore(context)
        return runCatching { store.searchWordIdsByFts(rawQuery) }.getOrNull()
    }

    /**
     * Imports a user-selected BYOD `.sqlite`, `.sqlite.gz`, or `.json` file via Android SAF Uri.
     * Never requests broad storage permissions; validates bounds & schema before applying.
     */
    fun importByodFromUri(
        context: Context,
        uri: Uri,
        displayName: String
    ): Result<ByodImportOutcome> {
        val lowerName = displayName.lowercase(Locale.US)
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Result.failure(IllegalStateException("선택한 파일을 열 수 없습니다."))

        return inputStream.use { stream ->
            when {
                lowerName.endsWith(".sqlite") || lowerName.endsWith(".db") || lowerName.endsWith(".gz") -> {
                    CorpusSqliteFtsStore.importByodSqliteStream(context, displayName, stream).onSuccess {
                        invalidateCache()
                    }
                }
                lowerName.endsWith(".json") -> {
                    CorpusSqliteFtsStore.readBoundedUtf8Stream(stream, MAX_IMPORT_JSON_BYTES).mapCatching { rawJson ->
                        val parsed = parseExternalOccurrencesJson(rawJson).getOrThrow()
                        val totalOccs = parsed.values.sumOf { it.size }
                        if (totalOccs == 0) {
                            throw IllegalArgumentException("가져올 수 있는 유효한 문장 용례가 없습니다.")
                        }
                        val target = File(context.applicationContext.filesDir, BYOD_JSON_CACHE_NAME)
                        target.writeText(rawJson, Charsets.UTF_8)
                        byodJsonOverlay = parsed
                        invalidateCache()
                        ByodImportOutcome(
                            modeName = "LOCAL_BYOD_JSON",
                            wordCount = parsed.size,
                            sentenceCount = totalOccs,
                            occurrenceCount = totalOccs,
                            messageKo = "로컬 BYOD JSON 결합 완료 (표제어 ${parsed.size}개 · 용례 ${totalOccs}건)"
                        )
                    }
                }
                else -> Result.failure(
                    IllegalArgumentException("지원 형식은 .sqlite, .sqlite.gz, .json 입니다.")
                )
            }
        }
    }

    /**
     * Restores the default copyright-safe curated mode by deleting local BYOD files.
     */
    fun restoreCuratedMode(context: Context) {
        CorpusSqliteFtsStore.clearByodDatabase(context)
        File(context.applicationContext.filesDir, BYOD_JSON_CACHE_NAME).delete()
        byodJsonOverlay = emptyMap()
        invalidateCache()
    }

    /**
     * Validates and parses an externally imported JSON shard conforming to Schema v1.0 or v2.0.
     * Never throws on malformed input; returns Result.failure with clear diagnostic message.
     */
    @Suppress("UNCHECKED_CAST")
    fun parseExternalOccurrencesJson(rawJson: String): Result<Map<String, List<ExamOccurrenceRecord>>> {
        if (rawJson.isBlank()) {
            return Result.failure(IllegalArgumentException("JSON 내용이 비어 있습니다."))
        }
        if (rawJson.toByteArray(Charsets.UTF_8).size > MAX_IMPORT_JSON_BYTES) {
            return Result.failure(IllegalArgumentException("JSON 파일 크기가 10MB 제한을 초과했습니다."))
        }
        return runCatching {
            val root = mapAdapter.fromJson(rawJson)
                ?: throw IllegalArgumentException("JSON 객체를 파싱할 수 없습니다.")
            val entriesList = (root["entries"] as? List<*>) ?: (root["items"] as? List<*>)
                ?: throw IllegalArgumentException("'entries' 또는 'items' 배열 필드가 필요합니다.")
            val map = linkedMapOf<String, MutableList<ExamOccurrenceRecord>>()
            entriesList.forEach { item ->
                val obj = item as? Map<*, *> ?: return@forEach
                val lemma = ExamCorpusAnalyticsEngine.normalizeToken(obj["lemma"] as? String ?: "")
                if (lemma.isBlank()) return@forEach
                val occList = obj["occurrences"] as? List<*> ?: emptyList<Any>()
                val parsedOccs = mutableListOf<ExamOccurrenceRecord>()
                occList.forEachIndexed { j, rawOcc ->
                    val o = rawOcc as? Map<*, *> ?: return@forEachIndexed
                    val year = ((o["year"] as? Number)?.toInt() ?: 2024).coerceIn(2010, 2035)
                    val month = ((o["month"] as? Number)?.toInt() ?: 11).coerceIn(1, 12)
                    val sentenceEn = ((o["sentenceEn"] as? String) ?: (o["text"] as? String)).orEmpty().trim().take(500)
                    if (sentenceEn.isBlank()) return@forEachIndexed
                    val grammarRaw = (o["grammarTags"] as? List<*>) ?: (o["grammar_tags"] as? List<*>)
                    val grammarList = buildList {
                        grammarRaw?.forEach { g ->
                            GrammarCategory.fromCode(g as? String ?: "")?.let { add(it) }
                        }
                    }
                    val surface = ((o["surfaceForm"] as? String) ?: (o["surface_form"] as? String))
                        .orEmpty().ifBlank { lemma }.take(64)
                    val span = ExamCorpusAnalyticsEngine.resolveCharSpan(sentenceEn, surface)
                    val rawSpansList = (o["grammar_spans"] as? List<*>) ?: (o["grammarSpans"] as? List<*>)
                    val parsedGrammarSpans = if (rawSpansList != null) {
                        val spansJson = runCatching {
                            val listType = Types.newParameterizedType(List::class.java, Any::class.java)
                            moshi.adapter<List<Any?>>(listType).toJson(rawSpansList)
                        }.getOrNull()
                        GrammarSpanEngine.resolveEffectiveSpans(
                            sentenceEn,
                            GrammarSpanEngine.parseGrammarSpansJson(spansJson, sentenceEn.length)
                        )
                    } else {
                        GrammarSpanEngine.detectGrammarSpans(sentenceEn)
                    }
                    parsedOccs += ExamOccurrenceRecord(
                        id = (o["id"] as? String).orEmpty().ifBlank { "${lemma}_ext_${year}_${month}_$j" },
                        year = year,
                        month = month,
                        examType = ExamType.fromCode((o["examType"] as? String) ?: (o["exam_type"] as? String) ?: "CSAT"),
                        itemTypeKo = ((o["itemTypeKo"] as? String) ?: (o["item_type"] as? String))
                            .orEmpty().ifBlank { "독해 지문" }.take(40),
                        itemNumber = (((o["itemNumber"] as? Number) ?: (o["item_no"] as? Number))?.toInt() ?: 31).coerceIn(1, 45),
                        surfaceForm = surface,
                        sentenceEn = sentenceEn,
                        sentenceKo = ((o["sentenceKo"] as? String) ?: (o["translation_ko"] as? String)).orEmpty().trim().take(500),
                        structureNoteKo = (o["structureNoteKo"] as? String).orEmpty().trim().take(240),
                        grammarCategories = grammarList,
                        provenance = CorpusDataProvenance.fromCode(o["provenance"] as? String ?: "OFFICIAL_IMPORT"),
                        section = ExamSection.fromCode(o["section"] as? String ?: "reading"),
                        charSpanStart = span.first,
                        charSpanEnd = span.second,
                        isUnderlined = (o["is_underlined"] as? Boolean) == true,
                        grammarSpans = parsedGrammarSpans
                    )
                }
                if (parsedOccs.isNotEmpty()) {
                    map.getOrPut(lemma) { mutableListOf() }.addAll(parsedOccs)
                }
            }
            map
        }
    }

    fun parseGrammarPatternsJson(rawJson: String): List<GrammarFocusSentence> {
        return runCatching {
            val root = mapAdapter.fromJson(rawJson) ?: return emptyList()
            val arr = root["patterns"] as? List<*> ?: return emptyList()
            val provenance = CorpusDataProvenance.fromCode(root["provenance"] as? String ?: "CURATED_TRAINING")
            buildList {
                arr.forEach { rawObj ->
                    val obj = rawObj as? Map<*, *> ?: return@forEach
                    val category = GrammarCategory.fromCode(obj["category"] as? String ?: "") ?: return@forEach
                    val id = (obj["id"] as? String).orEmpty().trim()
                    val sentenceWithBlank = (obj["sentenceWithBlankEn"] as? String).orEmpty().trim()
                    val completeSentence = (obj["completeSentenceEn"] as? String).orEmpty().trim()
                    if (id.isBlank() || sentenceWithBlank.isBlank() || completeSentence.isBlank()) return@forEach
                    val linkedRaw = obj["linkedLemmas"] as? List<*>
                    val linked = buildList {
                        linkedRaw?.forEach { item ->
                            val lemma = (item as? String).orEmpty().trim()
                            if (lemma.isNotBlank()) add(lemma)
                        }
                    }
                    add(
                        GrammarFocusSentence(
                            id = id,
                            category = category,
                            titleKo = (obj["titleKo"] as? String).orEmpty().trim(),
                            patternFormula = (obj["patternFormula"] as? String).orEmpty().trim(),
                            sentenceWithBlankEn = sentenceWithBlank,
                            completeSentenceEn = completeSentence,
                            sentenceKo = (obj["sentenceKo"] as? String).orEmpty().trim(),
                            targetSegment = (obj["targetSegment"] as? String).orEmpty().trim(),
                            distractorSegment = (obj["distractorSegment"] as? String).orEmpty().trim(),
                            explanationKo = (obj["explanationKo"] as? String).orEmpty().trim(),
                            trapTipKo = (obj["trapTipKo"] as? String).orEmpty().trim(),
                            linkedLemmas = linked,
                            difficultyTier = ((obj["difficultyTier"] as? Number)?.toInt() ?: 2).coerceIn(1, 3),
                            provenance = provenance
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadTierAssetOverrides(context: Context?): Map<String, TierOverrideMeta> {
        if (context == null) return emptyMap()
        val result = linkedMapOf<String, TierOverrideMeta>()
        VocabDifficultyTier.entries.forEach { tier ->
            val raw = readAssetText(context, "corpus/words/${tier.fileName}") ?: return@forEach
            runCatching {
                val root = mapAdapter.fromJson(raw) ?: return@forEach
                val items = root["items"] as? List<*> ?: return@forEach
                items.forEach { item ->
                    val obj = item as? Map<*, *> ?: return@forEach
                    val lemma = (obj["lemma"] as? String).orEmpty().trim().lowercase(Locale.US)
                    if (lemma.isBlank()) return@forEach
                    result[lemma] = TierOverrideMeta(
                        tier = tier,
                        curriculumGroup = CurriculumGroup2022.fromCode(obj["kr_2022_group"] as? String ?: "G2_MID_H1_1200"),
                        isAwl = (obj["is_awl"] as? Boolean) == true,
                        uniformityOverride = (obj["uniformity"] as? Number)?.toFloat(),
                        listeningRatioOverride = (obj["listening_ratio"] as? Number)?.toFloat(),
                        isPolysemous = (obj["is_polysemous"] as? Boolean) == true,
                        isNewRecent3Yr = (obj["is_new_recent_3yr"] as? Boolean) == true,
                        isFootnoteEver = (obj["is_footnote_ever"] as? Boolean) == true
                    )
                }
            }
        }
        return result
    }

    private fun readAssetText(context: Context?, path: String): String? {
        if (context == null) return null
        return runCatching {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }

    private fun buildWordEntry(
        word: VocabWord,
        index: Int,
        linkedGrammar: List<GrammarFocusSentence>,
        tierOverride: TierOverrideMeta?,
        byodExtraOccurrences: List<ExamOccurrenceRecord>
    ): ExamCorpusWordEntry {
        val cefr = when {
            word.grade >= 3 || word.csatDifficulty >= 3 -> CefrLevel.C1
            word.grade == 2 || word.csatDifficulty == 2 -> CefrLevel.B2
            else -> CefrLevel.B1
        }
        val isMoeBasic = word.grade <= 2 && word.csatDifficulty <= 2
        val inflections = ExamCorpusAnalyticsEngine.generateInflections(word.word, word.partOfSpeech)
        val collocations = buildCollocations(word)

        val baseOccurrences = buildStructuredOccurrences(word, index, inflections, linkedGrammar)
        val occurrences = (byodExtraOccurrences + baseOccurrences)
            .distinctBy { it.sentenceEn.lowercase(Locale.US) }
            .sortedByDescending { it.year * 100 + it.month }

        val grammarTags = (occurrences.flatMap { it.grammarCategories } + linkedGrammar.map { it.category })
            .distinct()

        // Ensure all 5 difficulty tiers are cleanly represented even without external assets
        val isFootnoteHeuristic = tierOverride?.isFootnoteEver ?: (index % 23 == 22 && word.grade == 3)
        val curriculumGroup = tierOverride?.curriculumGroup ?: when {
            word.grade == 1 && word.csatDifficulty == 1 -> CurriculumGroup2022.G1_ELEM_800
            word.grade <= 2 && isMoeBasic -> CurriculumGroup2022.G2_MID_H1_1200
            word.grade == 2 || word.csatDifficulty == 2 -> CurriculumGroup2022.G3_H2_H3_1000
            else -> CurriculumGroup2022.OOV
        }

        val baseAnalytics = ExamCorpusAnalyticsEngine.computeWordAnalytics(
            occurrences = occurrences,
            cefr = cefr,
            isMoeBasic = isMoeBasic,
            grade = word.grade,
            curriculumGroup = curriculumGroup,
            isFootnoteEver = isFootnoteHeuristic,
            isPolysemous = tierOverride?.isPolysemous ?: (word.meaningKo.contains(",") && index % 3 == 0),
            isAwl = tierOverride?.isAwl ?: (cefr == CefrLevel.B2 || cefr == CefrLevel.C1)
        )

        val adjustedAnalytics = if (tierOverride != null) {
            val unif = tierOverride.uniformityOverride ?: baseAnalytics.griesDispersion.uniformity
            val listRatio = tierOverride.listeningRatioOverride ?: baseAnalytics.listeningRatio
            val listFreq = (baseAnalytics.totalFrequency * listRatio).toInt()
            baseAnalytics.copy(
                difficultyTier = tierOverride.tier,
                griesDispersion = baseAnalytics.griesDispersion.copy(
                    dpNorm = (1f - unif).coerceIn(0f, 1f),
                    uniformity = unif.coerceIn(0f, 1f)
                ),
                listeningRatio = listRatio,
                listeningFrequency = listFreq,
                readingFrequency = (baseAnalytics.totalFrequency - listFreq).coerceAtLeast(0),
                flags = baseAnalytics.flags.copy(
                    isListeningHeavy = listRatio >= 0.45f,
                    isNewRecent3Yr = tierOverride.isNewRecent3Yr || baseAnalytics.flags.isNewRecent3Yr,
                    isFootnoteEver = tierOverride.isFootnoteEver
                )
            )
        } else {
            // Map deterministic 5-tier spread across curated 105 words when override JSON is absent
            val calibratedTier = when {
                isFootnoteHeuristic -> VocabDifficultyTier.TIER_D_FOOTNOTE
                baseAnalytics.isNewlyEmergent || (curriculumGroup == CurriculumGroup2022.OOV && index % 4 != 3) ->
                    VocabDifficultyTier.TIER_B_ADVANCED_NEW
                word.csatFrequency >= 5 && baseAnalytics.hasMockToCsatCarryover -> VocabDifficultyTier.TIER_S_CORE
                index % 11 == 10 -> VocabDifficultyTier.TIER_C_BURSTY
                else -> VocabDifficultyTier.TIER_A_CYCLICAL
            }
            baseAnalytics.copy(difficultyTier = calibratedTier)
        }

        val variantCounts = occurrences
            .groupingBy { it.surfaceForm.lowercase(Locale.US) }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { SurfaceVariantCount(it.key, it.value) }

        return ExamCorpusWordEntry(
            wordId = word.id,
            lemma = word.word,
            ipa = word.ipa,
            pos = word.partOfSpeech,
            meaningKo = word.meaningKo,
            grade = word.grade,
            cefr = cefr,
            isMoeBasic = isMoeBasic,
            inflections = inflections,
            collocations = collocations,
            occurrences = occurrences,
            grammarTags = grammarTags,
            analytics = adjustedAnalytics,
            sourceWord = word,
            surfaceVariants = variantCounts,
            wordFamilyHead = word.word
        )
    }

    private fun buildStructuredOccurrences(
        word: VocabWord,
        index: Int,
        inflections: List<String>,
        linkedGrammar: List<GrammarFocusSentence>
    ): List<ExamOccurrenceRecord> {
        val baseExamples = word.examples.ifEmpty {
            listOf(
                com.example.model.VocabExample(
                    labelKo = "핵심 용례",
                    english = word.exampleEn,
                    korean = word.exampleKo
                )
            )
        }
        val isEmergentPattern = (index % 9 == 8)
        val yearSchedule = when {
            isEmergentPattern -> listOf(2025, 2026)
            word.csatFrequency >= 5 -> listOf(2018, 2020, 2022, 2024, 2026)
            word.csatFrequency == 4 -> listOf(2019, 2021, 2023, 2025)
            else -> listOf(2020, 2023, 2026)
        }
        val examTypeSchedule = when (word.grade) {
            1 -> listOf(ExamType.MOCK_G1, ExamType.MOCK_G2, ExamType.KICE_MOCK, ExamType.CSAT)
            2 -> listOf(ExamType.MOCK_G2, ExamType.KICE_MOCK, ExamType.CSAT, ExamType.MOCK_G3)
            else -> listOf(ExamType.KICE_MOCK, ExamType.CSAT, ExamType.MOCK_G3, ExamType.CSAT)
        }
        val itemTypes = listOf("빈칸 추론", "주제·요지", "어법·구문", "문맥 어휘", "순서·삽입")

        val records = mutableListOf<ExamOccurrenceRecord>()
        val targetCount = yearSchedule.size.coerceAtMost(baseExamples.size + linkedGrammar.size)
        for (i in 0 until targetCount) {
            val year = yearSchedule[i]
            val examType = if (isEmergentPattern && i == 0) ExamType.KICE_MOCK
            else examTypeSchedule[i % examTypeSchedule.size]
            // Dynamic schedule reflecting 2024+ month shifts (G3 April->May, G1/G2 Nov->Oct)
            val month = when (examType) {
                ExamType.CSAT -> 11
                ExamType.KICE_MOCK -> if (i % 2 == 0) 6 else 9
                ExamType.MOCK_G3 -> if (year >= 2024) 5 else 4
                ExamType.MOCK_G1, ExamType.MOCK_G2 -> if (i % 2 == 0) 3 else (if (year >= 2024) 10 else 11)
            }
            val ex = baseExamples[i % baseExamples.size]
            val surface = detectSurfaceFormInSentence(ex.english, word.word, inflections)
            val inferredGrammar = inferGrammarCategories(ex.english, linkedGrammar)
            val span = ExamCorpusAnalyticsEngine.resolveCharSpan(ex.english, surface)
            val isListening = (word.grade == 1 && i == 0) || ((index + i) % 5 == 0)
            records += ExamOccurrenceRecord(
                id = "${word.id}_occ_$i",
                year = year,
                month = month,
                examType = examType,
                itemTypeKo = if (isListening) "듣기 대화" else itemTypes[(index + i) % itemTypes.size],
                itemNumber = if (isListening) 3 + ((index + i) % 12) else 21 + ((index * 3 + i * 4) % 20),
                surfaceForm = surface,
                sentenceEn = ex.english,
                sentenceKo = ex.korean,
                structureNoteKo = buildStructureNote(surface, word.partOfSpeech, inferredGrammar),
                grammarCategories = inferredGrammar,
                provenance = CorpusDataProvenance.CURATED_TRAINING_CORPUS,
                section = if (isListening) ExamSection.LISTENING else ExamSection.READING,
                charSpanStart = span.first,
                charSpanEnd = span.second,
                isUnderlined = ((index + i) % 4 == 0),
                grammarSpans = GrammarSpanEngine.detectGrammarSpans(ex.english)
            )
        }

        linkedGrammar.forEachIndexed { gIdx, gs ->
            if (records.none { it.sentenceEn.equals(gs.completeSentenceEn, ignoreCase = true) }) {
                val surface = detectSurfaceFormInSentence(gs.completeSentenceEn, word.word, inflections)
                val span = ExamCorpusAnalyticsEngine.resolveCharSpan(gs.completeSentenceEn, surface)
                records += ExamOccurrenceRecord(
                    id = "${word.id}_gram_$gIdx",
                    year = 2025 + (gIdx % 2),
                    month = 9,
                    examType = ExamType.KICE_MOCK,
                    itemTypeKo = "핵심 어법(${gs.category.shortLabelKo})",
                    itemNumber = 29,
                    surfaceForm = surface,
                    sentenceEn = gs.completeSentenceEn,
                    sentenceKo = gs.sentenceKo,
                    structureNoteKo = gs.patternFormula,
                    grammarCategories = listOf(gs.category),
                    provenance = gs.provenance,
                    section = ExamSection.READING,
                    charSpanStart = span.first,
                    charSpanEnd = span.second,
                    isUnderlined = true,
                    grammarSpans = GrammarSpanEngine.detectGrammarSpans(gs.completeSentenceEn)
                )
            }
        }
        return records.sortedByDescending { it.year * 100 + it.month }
    }

    private fun detectSurfaceFormInSentence(
        sentence: String,
        lemma: String,
        inflections: List<String>
    ): String {
        val tokens = sentence.split(Regex("[^A-Za-z']+")).filter { it.isNotBlank() }
        val candidates = (listOf(lemma) + inflections).map { it.lowercase(Locale.US) }
        return tokens.firstOrNull { tok -> tok.lowercase(Locale.US) in candidates }
            ?: tokens.firstOrNull { tok -> tok.lowercase(Locale.US).startsWith(lemma.take(5).lowercase(Locale.US)) }
            ?: lemma
    }

    private fun inferGrammarCategories(
        sentence: String,
        linkedGrammar: List<GrammarFocusSentence>
    ): List<GrammarCategory> {
        val lower = sentence.lowercase(Locale.US)
        val tags = linkedSetOf<GrammarCategory>()
        linkedGrammar.firstOrNull()?.let { tags += it.category }
        if (lower.contains(" that ") || lower.contains(" which ") || lower.contains(" where ") || lower.contains(" what ")) {
            tags += GrammarCategory.RELATIVES_CONJUNCTIONS
        }
        if (lower.contains(" and ") || lower.contains(" than ") || lower.contains(" both ")) {
            tags += GrammarCategory.PARALLELISM_COMPARISON
        }
        if (lower.contains("ing ") || lower.contains(" to ")) {
            tags += GrammarCategory.VERBALS_PARTICIPLES
        }
        if (lower.contains(" is ") || lower.contains(" are ") || lower.contains(" has ") || lower.contains(" have ")) {
            tags += GrammarCategory.SUBJECT_VERB_AGREEMENT
        }
        if (tags.isEmpty()) {
            tags += GrammarCategory.VOICE_TENSE
        }
        return tags.take(2)
    }

    private fun buildStructureNote(
        surfaceForm: String,
        pos: String,
        categories: List<GrammarCategory>
    ): String {
        val catLabel = categories.firstOrNull()?.labelKo ?: "핵심 구문"
        return "활용 어형 '$surfaceForm' ($pos) · $catLabel 포인트 집중 확인"
    }

    private fun buildCollocations(word: VocabWord): List<String> {
        val w = word.word
        return when {
            word.partOfSpeech.contains("v.") -> listOf("$w + 목적어", "help ~ $w")
            word.partOfSpeech.contains("adj.") -> listOf("$w role / step", "be $w to")
            else -> listOf("a key $w", "$w of ~")
        }
    }

    private fun builtInFallbackGrammarSentences(): List<GrammarFocusSentence> = listOf(
        GrammarFocusSentence(
            id = "gram_sva_01",
            category = GrammarCategory.SUBJECT_VERB_AGREEMENT,
            titleKo = "전치사구 수식어 뒤 단수 주어 수일치",
            patternFormula = "[단수 주어 + 전치사구] + 단수 동사",
            sentenceWithBlankEn = "The consistent effort of dedicated students ____ remarkable progress.",
            completeSentenceEn = "The consistent effort of dedicated students produces remarkable progress.",
            sentenceKo = "헌신적인 학생들의 꾸준한 노력은 놀라운 발전을 만들어낸다.",
            targetSegment = "produces",
            distractorSegment = "produce",
            explanationKo = "핵심 주어가 단수 명사 effort이므로 단수 동사 produces가 맞습니다.",
            trapTipKo = "동사 앞 복수 명사(students)에 현혹되지 마세요.",
            linkedLemmas = listOf("consistent", "accomplish"),
            difficultyTier = 1
        ),
        GrammarFocusSentence(
            id = "gram_rel_01",
            category = GrammarCategory.RELATIVES_CONJUNCTIONS,
            titleKo = "관계대명사 what vs 접속사 that",
            patternFormula = "선행사 없음 + 불완전한 절 → what",
            sentenceWithBlankEn = "His actions contradict ____ he promised earlier.",
            completeSentenceEn = "His actions contradict what he promised earlier.",
            sentenceKo = "그의 행동은 앞서 약속했던 것과 모순된다.",
            targetSegment = "what",
            distractorSegment = "that",
            explanationKo = "promised의 목적어가 빠진 불완전한 절이므로 what이 필요합니다.",
            trapTipKo = "뒤 절의 완전성 여부를 먼저 체크하세요.",
            linkedLemmas = listOf("contradict"),
            difficultyTier = 2
        ),
        GrammarFocusSentence(
            id = "gram_vrb_02",
            category = GrammarCategory.VERBALS_PARTICIPLES,
            titleKo = "분사구문 능동/수동 판단",
            patternFormula = "주절 주어와의 수동 관계 → 과거분사(p.p.)",
            sentenceWithBlankEn = "____ by sudden delays, travelers sought an alternative route.",
            completeSentenceEn = "Confronted by sudden delays, travelers sought an alternative route.",
            sentenceKo = "갑작스러운 지연에 직면해, 여행자들은 대안 경로를 찾았다.",
            targetSegment = "Confronted",
            distractorSegment = "Confronting",
            explanationKo = "지연에 직면하게 된 수동 관계이므로 과거분사 Confronted가 맞습니다.",
            trapTipKo = "주절 주어를 분사 앞에 넣어 능동/수동을 판단하세요.",
            linkedLemmas = listOf("confront", "alternative"),
            difficultyTier = 2
        ),
        GrammarFocusSentence(
            id = "gram_vt_01",
            category = GrammarCategory.VOICE_TENSE,
            titleKo = "자동사 수동태 불가",
            patternFormula = "자동사(emerge, occur) → 능동태만 사용",
            sentenceWithBlankEn = "New opportunities ____ whenever learners practice consistently.",
            completeSentenceEn = "New opportunities emerge whenever learners practice consistently.",
            sentenceKo = "학습자가 꾸준히 연습할 때마다 새로운 기회가 나타난다.",
            targetSegment = "emerge",
            distractorSegment = "are emerged",
            explanationKo = "emerge는 목적어가 없는 자동사이므로 수동태가 불가능합니다.",
            trapTipKo = "자동사 목록(emerge, occur, consist)을 함께 암기하세요.",
            linkedLemmas = listOf("emerge"),
            difficultyTier = 2
        ),
        GrammarFocusSentence(
            id = "gram_inv_01",
            category = GrammarCategory.INVERSION_EMPHASIS,
            titleKo = "부정어구 문두 도치",
            patternFormula = "Rarely/Never + 조동사 + 주어 + 동사원형",
            sentenceWithBlankEn = "Rarely ____ a single habit transform confidence so rapidly.",
            completeSentenceEn = "Rarely does a single habit transform confidence so rapidly.",
            sentenceKo = "단 하나의 습관이 자신감을 이토록 빠르게 바꾸는 일은 드물다.",
            targetSegment = "does a single habit",
            distractorSegment = "a single habit does",
            explanationKo = "부정어 Rarely가 문두에 오면 의문문 어순으로 도치됩니다.",
            trapTipKo = "문두 부정어를 보면 즉시 조동사 도치를 확인하세요.",
            linkedLemmas = listOf("consistent", "enhance"),
            difficultyTier = 3
        ),
        GrammarFocusSentence(
            id = "gram_par_01",
            category = GrammarCategory.PARALLELISM_COMPARISON,
            titleKo = "등위접속사 병렬 구조",
            patternFormula = "V1, V2, and V3 (형태 일치)",
            sentenceWithBlankEn = "Good notes help learners organize ideas, spot patterns, and ____ errors.",
            completeSentenceEn = "Good notes help learners organize ideas, spot patterns, and eliminate errors.",
            sentenceKo = "좋은 필기는 학습자가 아이디어를 정리하고 패턴을 찾으며 오류를 없애도록 돕는다.",
            targetSegment = "eliminate",
            distractorSegment = "eliminating",
            explanationKo = "앞의 organize, spot과 병렬 구조를 이루므로 동사원형 eliminate가 맞습니다.",
            trapTipKo = "and 앞뒤에 나열된 요소의 첫 형태를 비교하세요.",
            linkedLemmas = listOf("eliminate"),
            difficultyTier = 1
        )
    )
}
