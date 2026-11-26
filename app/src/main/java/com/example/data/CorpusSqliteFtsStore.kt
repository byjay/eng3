package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.engine.ExamCorpusAnalyticsEngine
import com.example.model.CorpusDataProvenance
import com.example.model.ExamCorpusWordEntry
import com.example.model.ExamOccurrenceRecord
import com.example.model.ExamSection
import com.example.model.ExamType
import com.example.model.GrammarCategory
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

data class PagedOccurrenceResult(
    val items: List<ExamOccurrenceRecord>,
    val totalCount: Int,
    val hasMore: Boolean,
    val sourceLabelKo: String
)

data class ByodImportOutcome(
    val modeName: String,
    val wordCount: Int,
    val sentenceCount: Int,
    val occurrenceCount: Int,
    val messageKo: String
)

/**
 * Native Android SQLiteOpenHelper + External-Content FTS5 Storage Engine (Zero ORM bloat).
 *
 * Schema:
 *  - `words` (indexed by tier + priority_score DESC, lemma COLLATE NOCASE)
 *  - `sentences` (normalized master table; text stored once)
 *  - `occurrences` (indexed by word_id + sent_id, word_id + surface_form)
 *  - `sentences_fts` (External-Content FTS5 virtual table, tokenize='porter unicode61')
 *
 * Security & Compliance:
 *  - 100% parameterized SQL bindings (`?`); zero raw string interpolation of user input.
 *  - FTS5 queries always pass through `ExamCorpusAnalyticsEngine.sanitizeFtsQuery`.
 *  - Safe BYOD importer validates SQLite header magic, table schema, and strict byte/decompression caps
 *    (10 MB JSON / 40 MB decompressed SQLite) before promoting a staging file.
 */
class CorpusSqliteFtsStore private constructor(
    context: Context,
    dbName: String
) : SQLiteOpenHelper(context.applicationContext, dbName, null, DB_VERSION) {

    @Volatile
    private var fts5Available: Boolean = true

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS words (
              word_id TEXT PRIMARY KEY,
              lemma TEXT NOT NULL,
              tier TEXT NOT NULL,
              difficulty_rank INTEGER NOT NULL,
              difficulty_score REAL NOT NULL,
              priority_score REAL NOT NULL,
              total_freq INTEGER NOT NULL,
              csat_freq INTEGER NOT NULL,
              uniformity REAL NOT NULL,
              mean_gap_yr REAL,
              overdue_yr INTEGER,
              listening_ratio REAL NOT NULL,
              is_new_recent_3yr INTEGER NOT NULL DEFAULT 0,
              is_polysemous INTEGER NOT NULL DEFAULT 0,
              payload_json TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_tier_prio ON words(tier, priority_score DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_lemma ON words(lemma COLLATE NOCASE)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sentences (
              rowid INTEGER PRIMARY KEY AUTOINCREMENT,
              sent_id TEXT UNIQUE NOT NULL,
              year INTEGER NOT NULL,
              exam_type TEXT NOT NULL,
              grade INTEGER NOT NULL,
              month INTEGER NOT NULL,
              section TEXT NOT NULL,
              item_no INTEGER NOT NULL,
              item_type TEXT NOT NULL,
              text TEXT NOT NULL,
              translation_ko TEXT,
              grammar_tags_json TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS occurrences (
              occ_id TEXT PRIMARY KEY,
              word_id TEXT NOT NULL,
              sent_id TEXT NOT NULL,
              surface_form TEXT NOT NULL,
              pos TEXT NOT NULL,
              span_start INTEGER NOT NULL,
              span_end INTEGER NOT NULL,
              is_underlined INTEGER NOT NULL DEFAULT 0,
              is_answer_key INTEGER NOT NULL DEFAULT 0,
              FOREIGN KEY (word_id) REFERENCES words(word_id),
              FOREIGN KEY (sent_id) REFERENCES sentences(sent_id)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_occ_word_lookup ON occurrences(word_id, sent_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_occ_word_surface ON occurrences(word_id, surface_form)")

        fts5Available = runCatching {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS sentences_fts USING fts5(
                  text,
                  content='sentences',
                  content_rowid='rowid',
                  tokenize='porter unicode61'
                )
                """.trimIndent()
            )
            true
        }.getOrDefault(false)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS sentences_fts")
        db.execSQL("DROP TABLE IF EXISTS occurrences")
        db.execSQL("DROP TABLE IF EXISTS sentences")
        db.execSQL("DROP TABLE IF EXISTS words")
        onCreate(db)
    }

    /**
     * Populates normalized 3-table storage + rebuilds External-Content FTS5 index atomically.
     */
    fun seedFromEntries(entries: List<ExamCorpusWordEntry>) {
        val db = writableDatabase
        val existingCount = db.rawQuery("SELECT COUNT(*) FROM words", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        if (existingCount >= entries.size && entries.isNotEmpty()) return

        db.beginTransaction()
        try {
            db.delete("occurrences", null, null)
            db.delete("sentences", null, null)
            db.delete("words", null, null)

            val insertedSentences = hashSetOf<String>()
            entries.forEach { entry ->
                val a = entry.analytics
                val wCv = ContentValues().apply {
                    put("word_id", entry.wordId)
                    put("lemma", entry.lemma)
                    put("tier", a.difficultyTier.code)
                    put("difficulty_rank", a.difficultyTier.rank)
                    put("difficulty_score", a.difficultyScore.toDouble())
                    put("priority_score", a.priorityScore.toDouble())
                    put("total_freq", a.totalFrequency)
                    put("csat_freq", a.csatFrequency)
                    put("uniformity", a.griesDispersion.uniformity.toDouble())
                    a.recurrence.meanGapYr?.let { put("mean_gap_yr", it.toDouble()) } ?: putNull("mean_gap_yr")
                    a.recurrence.overdueYr?.let { put("overdue_yr", it) } ?: putNull("overdue_yr")
                    put("listening_ratio", a.listeningRatio.toDouble())
                    put("is_new_recent_3yr", if (a.flags.isNewRecent3Yr) 1 else 0)
                    put("is_polysemous", if (a.flags.isPolysemous) 1 else 0)
                    put("payload_json", "{}")
                }
                db.insertWithOnConflict("words", null, wCv, SQLiteDatabase.CONFLICT_REPLACE)

                entry.occurrences.forEachIndexed { idx, occ ->
                    val sentId = "sent_${occ.id}"
                    if (insertedSentences.add(sentId)) {
                        val tagsJson = JSONArray().apply {
                            occ.grammarCategories.forEach { put(it.udRuleCode) }
                        }.toString()
                        val sCv = ContentValues().apply {
                            put("sent_id", sentId)
                            put("year", occ.year)
                            put("exam_type", occ.examType.code)
                            put("grade", entry.grade)
                            put("month", occ.month)
                            put("section", occ.section.code)
                            put("item_no", occ.itemNumber)
                            put("item_type", occ.itemTypeKo)
                            put("text", occ.sentenceEn)
                            put("translation_ko", occ.sentenceKo)
                            put("grammar_tags_json", tagsJson)
                        }
                        db.insertWithOnConflict("sentences", null, sCv, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                    val span = if (occ.charSpanStart >= 0 && occ.charSpanEnd > occ.charSpanStart) {
                        occ.charSpanStart to occ.charSpanEnd
                    } else {
                        ExamCorpusAnalyticsEngine.resolveCharSpan(occ.sentenceEn, occ.surfaceForm)
                    }
                    val oCv = ContentValues().apply {
                        put("occ_id", "${entry.wordId}_${occ.id}_$idx")
                        put("word_id", entry.wordId)
                        put("sent_id", sentId)
                        put("surface_form", occ.surfaceForm)
                        put("pos", entry.pos)
                        put("span_start", span.first.coerceAtLeast(0))
                        put("span_end", span.second.coerceAtLeast(0))
                        put("is_underlined", if (occ.isUnderlined) 1 else 0)
                        put("is_answer_key", if (occ.isAnswerKeyToken) 1 else 0)
                    }
                    db.insertWithOnConflict("occurrences", null, oCv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }

            if (fts5Available) {
                runCatching {
                    db.execSQL("INSERT INTO sentences_fts(sentences_fts) VALUES('rebuild')")
                }.onFailure { fts5Available = false }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Lazy paginated occurrence query (`LIMIT ? OFFSET ?`) across normalized `occurrences` + `sentences`.
     * All filter parameters are strictly bound via `?` placeholders.
     */
    fun queryOccurrencesPaged(
        wordId: String,
        lemmaFallback: String = "",
        examTypeFilter: ExamType? = null,
        sectionFilter: ExamSection? = null,
        surfaceFormFilter: String? = null,
        onlyGrammarCore: Boolean = false,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0,
        provenance: CorpusDataProvenance = CorpusDataProvenance.CURATED_TRAINING_CORPUS
    ): PagedOccurrenceResult {
        val db = readableDatabase
        val effectiveWordId = resolveWordId(db, wordId, lemmaFallback) ?: return PagedOccurrenceResult(
            items = emptyList(),
            totalCount = 0,
            hasMore = false,
            sourceLabelKo = provenance.badgeKo
        )

        val whereClauses = mutableListOf("o.word_id = ?")
        val args = mutableListOf(effectiveWordId)

        if (examTypeFilter != null) {
            whereClauses += "s.exam_type = ?"
            args += examTypeFilter.code
        }
        if (sectionFilter != null) {
            whereClauses += "s.section = ?"
            args += sectionFilter.code
        }
        if (!surfaceFormFilter.isNullOrBlank()) {
            whereClauses += "LOWER(o.surface_form) = ?"
            args += surfaceFormFilter.trim().lowercase(Locale.US)
        }
        if (onlyGrammarCore) {
            whereClauses += "(s.grammar_tags_json IS NOT NULL AND s.grammar_tags_json != '[]' AND s.grammar_tags_json != '')"
        }

        val whereSql = whereClauses.joinToString(" AND ")
        val totalCount = db.rawQuery(
            """
            SELECT COUNT(*)
            FROM occurrences o
            JOIN sentences s ON s.sent_id = o.sent_id
            WHERE $whereSql
            """.trimIndent(),
            args.toTypedArray()
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val pageArgs = args + listOf(safeLimit.toString(), safeOffset.toString())

        val items = mutableListOf<ExamOccurrenceRecord>()
        db.rawQuery(
            """
            SELECT o.occ_id, s.year, s.month, s.exam_type, s.item_type, s.item_no,
                   o.surface_form, s.text, s.translation_ko, s.grammar_tags_json,
                   s.section, o.span_start, o.span_end, o.is_underlined, o.is_answer_key
            FROM occurrences o
            JOIN sentences s ON s.sent_id = o.sent_id
            WHERE $whereSql
            ORDER BY s.year DESC, s.month DESC, s.item_no ASC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            pageArgs.toTypedArray()
        ).use { c ->
            while (c.moveToNext()) {
                val grammarTags = parseGrammarTagJson(c.getString(9))
                val surface = c.getString(6).orEmpty()
                val textEn = c.getString(7).orEmpty()
                val spanStart = c.getInt(11)
                val spanEnd = c.getInt(12)
                items += ExamOccurrenceRecord(
                    id = c.getString(0),
                    year = c.getInt(1),
                    month = c.getInt(2),
                    examType = ExamType.fromCode(c.getString(3).orEmpty()),
                    itemTypeKo = c.getString(4).orEmpty().ifBlank { "독해 지문" },
                    itemNumber = c.getInt(5),
                    surfaceForm = surface,
                    sentenceEn = textEn,
                    sentenceKo = c.getString(8).orEmpty(),
                    structureNoteKo = if (grammarTags.isNotEmpty()) {
                        "활용 어형 '$surface' · ${grammarTags.first().labelKo} 핵심 구문"
                    } else {
                        "활용 어형 '$surface' 문맥 확인"
                    },
                    grammarCategories = grammarTags,
                    provenance = provenance,
                    section = ExamSection.fromCode(c.getString(10).orEmpty()),
                    charSpanStart = spanStart,
                    charSpanEnd = spanEnd,
                    isUnderlined = c.getInt(13) == 1,
                    isAnswerKeyToken = c.getInt(14) == 1
                )
            }
        }

        return PagedOccurrenceResult(
            items = items,
            totalCount = totalCount,
            hasMore = (safeOffset + items.size) < totalCount,
            sourceLabelKo = provenance.badgeKo
        )
    }

    /**
     * Full-text search via External-Content FTS5 (`MATCH ?`) with automatic sanitization
     * and safe parameterized fallback if FTS5 is absent on a device.
     */
    fun searchWordIdsByFts(rawQuery: String, limit: Int = 80): Set<String> {
        val clean = rawQuery.trim()
        if (clean.isBlank()) return emptySet()
        val db = readableDatabase
        val hits = linkedSetOf<String>()
        val safeLimit = limit.coerceIn(1, 200).toString()

        // 1) Direct lemma prefix match
        db.rawQuery(
            "SELECT word_id FROM words WHERE lemma LIKE ? COLLATE NOCASE LIMIT ?",
            arrayOf("${clean.lowercase(Locale.US)}%", safeLimit)
        ).use { c ->
            while (c.moveToNext()) hits += c.getString(0)
        }

        // 2) External-Content FTS5 full-text sentence match
        val ftsQuery = ExamCorpusAnalyticsEngine.sanitizeFtsQuery(clean)
        if (ftsQuery.isNotBlank() && fts5Available) {
            runCatching {
                db.rawQuery(
                    """
                    SELECT DISTINCT o.word_id
                    FROM sentences_fts f
                    JOIN sentences s ON s.rowid = f.rowid
                    JOIN occurrences o ON o.sent_id = s.sent_id
                    WHERE sentences_fts MATCH ?
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(ftsQuery, safeLimit)
                ).use { c ->
                    while (c.moveToNext()) hits += c.getString(0)
                }
            }.onFailure {
                // Safe parameterized LIKE fallback if imported DB has no FTS5 table
                fallbackSentenceLikeSearch(db, clean, safeLimit, hits)
            }
        } else if (clean.length >= 2) {
            fallbackSentenceLikeSearch(db, clean, safeLimit, hits)
        }
        return hits
    }

    private fun fallbackSentenceLikeSearch(
        db: SQLiteDatabase,
        clean: String,
        safeLimit: String,
        outHits: MutableSet<String>
    ) {
        db.rawQuery(
            """
            SELECT DISTINCT o.word_id
            FROM sentences s
            JOIN occurrences o ON o.sent_id = s.sent_id
            WHERE s.text LIKE ?
            LIMIT ?
            """.trimIndent(),
            arrayOf("%$clean%", safeLimit)
        ).use { c ->
            while (c.moveToNext()) outHits += c.getString(0)
        }
    }

    private fun resolveWordId(db: SQLiteDatabase, wordId: String, lemmaFallback: String): String? {
        db.rawQuery("SELECT word_id FROM words WHERE word_id = ? LIMIT 1", arrayOf(wordId)).use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        if (lemmaFallback.isNotBlank()) {
            db.rawQuery(
                "SELECT word_id FROM words WHERE lemma = ? COLLATE NOCASE LIMIT 1",
                arrayOf(lemmaFallback.trim())
            ).use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        }
        return null
    }

    private fun parseGrammarTagJson(raw: String?): List<GrammarCategory> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    GrammarCategory.fromCode(arr.optString(i))?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val DB_VERSION = 2
        private const val DEFAULT_DB_NAME = "csat_curated_corpus_v2.sqlite"
        private const val BYOD_DB_NAME = "csat_byod_local_v2.sqlite"
        private const val DEFAULT_PAGE_SIZE = 15

        private const val MAX_BYOD_JSON_BYTES = 10 * 1024 * 1024 // 10 MB cap
        private const val MAX_BYOD_SQLITE_BYTES = 40 * 1024 * 1024 // 40 MB decompressed cap (gzip bomb guard)
        private val SQLITE_HEADER_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        @Volatile
        private var curatedInstance: CorpusSqliteFtsStore? = null

        @Volatile
        private var byodInstance: CorpusSqliteFtsStore? = null

        fun getCurated(context: Context): CorpusSqliteFtsStore =
            curatedInstance ?: synchronized(this) {
                curatedInstance ?: CorpusSqliteFtsStore(context.applicationContext, DEFAULT_DB_NAME).also {
                    curatedInstance = it
                }
            }

        fun hasByodSqlite(context: Context): Boolean {
            val dbFile = context.applicationContext.getDatabasePath(BYOD_DB_NAME)
            return dbFile.exists() && dbFile.length() > SQLITE_HEADER_MAGIC.size
        }

        fun getActiveStore(context: Context): Pair<CorpusSqliteFtsStore, CorpusDataProvenance> {
            return if (hasByodSqlite(context)) {
                val store = byodInstance ?: synchronized(this) {
                    byodInstance ?: CorpusSqliteFtsStore(context.applicationContext, BYOD_DB_NAME).also {
                        byodInstance = it
                    }
                }
                store to CorpusDataProvenance.LOCAL_BYOD_SQLITE
            } else {
                getCurated(context) to CorpusDataProvenance.CURATED_TRAINING_CORPUS
            }
        }

        fun clearByodDatabase(context: Context) {
            synchronized(this) {
                byodInstance?.close()
                byodInstance = null
                context.applicationContext.deleteDatabase(BYOD_DB_NAME)
            }
        }

        /**
         * Safely imports a user-selected BYOD `.sqlite` or `.sqlite.gz` file from Android SAF.
         * Defends against gzip bombs, invalid binary formats, and missing required tables.
         */
        fun importByodSqliteStream(
            context: Context,
            displayName: String,
            inputStream: InputStream
        ): Result<ByodImportOutcome> = runCatching {
            val appCtx = context.applicationContext
            val isGzip = displayName.lowercase(Locale.US).endsWith(".gz")
            val stagingFile = File(appCtx.cacheDir, "byod_staging_${System.currentTimeMillis()}.sqlite")

            try {
                val sourceStream: InputStream = if (isGzip) GZIPInputStream(inputStream) else inputStream
                var totalWritten = 0L
                val buf = ByteArray(16 * 1024)
                FileOutputStream(stagingFile).use { out ->
                    while (true) {
                        val read = sourceStream.read(buf)
                        if (read <= 0) break
                        totalWritten += read
                        if (totalWritten > MAX_BYOD_SQLITE_BYTES) {
                            throw IllegalArgumentException("압축 해제 SQLite 용량이 40MB 상한을 초과했습니다.")
                        }
                        out.write(buf, 0, read)
                    }
                }

                // Validate SQLite 3 magic header
                if (stagingFile.length() < SQLITE_HEADER_MAGIC.size) {
                    throw IllegalArgumentException("유효한 SQLite 데이터베이스 파일이 아닙니다.")
                }
                val header = stagingFile.inputStream().use { inp ->
                    ByteArray(SQLITE_HEADER_MAGIC.size).also { inp.read(it) }
                }
                if (!header.contentEquals(SQLITE_HEADER_MAGIC)) {
                    throw IllegalArgumentException("SQLite3 헤더 시그니처가 일치하지 않습니다.")
                }

                // Validate required normalized 3 tables before promoting
                var wordCount = 0
                var sentenceCount = 0
                var occurrenceCount = 0
                SQLiteDatabase.openDatabase(
                    stagingFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                ).use { testDb ->
                    val existingTables = hashSetOf<String>()
                    testDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                        while (c.moveToNext()) existingTables += c.getString(0).lowercase(Locale.US)
                    }
                    val required = listOf("words", "sentences", "occurrences")
                    val missing = required.filterNot { it in existingTables }
                    if (missing.isNotEmpty()) {
                        throw IllegalArgumentException("필수 테이블이 누락되었습니다: ${missing.joinToString(", ")}")
                    }
                    wordCount = testDb.rawQuery("SELECT COUNT(*) FROM words", null).use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 0
                    }
                    sentenceCount = testDb.rawQuery("SELECT COUNT(*) FROM sentences", null).use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 0
                    }
                    occurrenceCount = testDb.rawQuery("SELECT COUNT(*) FROM occurrences", null).use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 0
                    }
                }

                // Atomically promote validated DB into app-private database path
                synchronized(this) {
                    byodInstance?.close()
                    byodInstance = null
                    val targetFile = appCtx.getDatabasePath(BYOD_DB_NAME)
                    targetFile.parentFile?.mkdirs()
                    stagingFile.copyTo(targetFile, overwrite = true)
                }

                ByodImportOutcome(
                    modeName = "LOCAL_BYOD_SQLITE",
                    wordCount = wordCount,
                    sentenceCount = sentenceCount,
                    occurrenceCount = occurrenceCount,
                    messageKo = "로컬 BYOD SQLite DB 연결 완료 (어휘 ${wordCount}개 · 문장 ${sentenceCount}개 · 용례 ${occurrenceCount}건)"
                )
            } finally {
                stagingFile.delete()
            }
        }

        /**
         * Reads a SAF JSON stream with strict 10 MB upper bound.
         */
        fun readBoundedUtf8Stream(inputStream: InputStream, maxBytes: Int = MAX_BYOD_JSON_BYTES): Result<String> =
            runCatching {
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = inputStream.read(buf)
                    if (read <= 0) break
                    total += read
                    if (total > maxBytes) {
                        throw IllegalArgumentException("JSON 파일 크기가 ${maxBytes / (1024 * 1024)}MB 제한을 초과했습니다.")
                    }
                    out.write(buf, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
    }
}
