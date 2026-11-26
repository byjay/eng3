package com.example.data

import android.content.Context
import com.example.engine.WordRun2027Engine
import com.example.model.WordRunChallengeState
import com.example.model.WordRunDayProgress
import com.example.model.WordRunEntry
import com.example.model.WordRunExamRecord
import com.example.model.WordRunUserContext
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File

/**
 * Repository & Atomic Local Persistence Store for WORD RUN 2027 (500 CSAT Words · 10-Day Challenge).
 *
 * - Loads bundled copyright-clean 500-word JSON asset (`assets/corpus/word_run_2027_500.json`)
 * - Persists user challenge progress atomically (write temp -> rename) inside app-private filesDir
 * - Pure Moshi serialization so unit tests run cleanly on JVM without Android stubs
 */
object WordRun2027Repository {

    private const val ASSET_PATH = "corpus/word_run_2027_500.json"
    private const val STATE_FILE_NAME = "word_run_2027_state_v1.json"

    private val moshi by lazy { Moshi.Builder().build() }
    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any?>>(type)
    }

    @Volatile
    private var cachedWords: List<WordRunEntry>? = null

    fun loadAllWords(context: Context): List<WordRunEntry> {
        cachedWords?.let { return it }
        val raw = runCatching {
            context.applicationContext.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return emptyList()
        val parsed = parseWordsJson(raw)
        if (parsed.isNotEmpty()) {
            cachedWords = parsed
        }
        return parsed
    }

    fun parseWordsJson(rawJson: String): List<WordRunEntry> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            val root = mapAdapter.fromJson(rawJson) ?: return emptyList()
            val items = root["items"] as? List<*> ?: return emptyList()
            buildList {
                items.forEach { rawItem ->
                    val obj = rawItem as? Map<*, *> ?: return@forEach
                    val id = (obj["id"] as? Number)?.toInt() ?: return@forEach
                    val day = ((obj["day"] as? Number)?.toInt() ?: 1).coerceIn(1, WordRun2027Engine.TOTAL_DAYS)
                    val word = (obj["w"] as? String).orEmpty().trim()
                    val meaningKo = (obj["k"] as? String).orEmpty().trim()
                    if (word.isBlank() || meaningKo.isBlank()) return@forEach
                    add(
                        WordRunEntry(
                            id = id,
                            day = day,
                            word = word,
                            meaningKo = meaningKo,
                            questionNum = ((obj["q"] as? Number)?.toInt() ?: 21).coerceIn(18, 45),
                            difficulty = ((obj["d"] as? Number)?.toInt() ?: 1).coerceIn(1, 3),
                            pos = (obj["p"] as? String).orEmpty().trim().ifBlank { "n" },
                            exampleEn = (obj["exampleEn"] as? String).orEmpty().trim(),
                            exampleKo = (obj["exampleKo"] as? String).orEmpty().trim()
                        )
                    )
                }
            }.sortedBy { it.id }
        }.getOrDefault(emptyList())
    }

    fun loadState(context: Context): WordRunChallengeState {
        val file = File(context.applicationContext.filesDir, STATE_FILE_NAME)
        if (!file.exists()) return WordRunChallengeState()
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return WordRunChallengeState()
        return parseStateJson(raw)
    }

    fun saveState(context: Context, state: WordRunChallengeState): Boolean {
        return runCatching {
            val dir = context.applicationContext.filesDir
            val target = File(dir, STATE_FILE_NAME)
            val tmp = File(dir, "$STATE_FILE_NAME.tmp")
            val json = serializeStateJson(state)
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
            true
        }.getOrDefault(false)
    }

    fun serializeStateJson(state: WordRunChallengeState): String {
        val daysMap = state.dayProgress.entries.associate { (day, prog) ->
            day.toString() to mapOf(
                "day" to prog.day,
                "reviewCompleted" to prog.reviewCompleted,
                "learnedIds" to prog.learnedIds.sorted(),
                "practicedIds" to prog.practicedIds.sorted(),
                "passedDateKst" to prog.passedDateKst,
                "wrongWordIds" to prog.wrongWordIds
            )
        }
        val againMap = state.studyAgainCounts.entries.associate { (id, cnt) -> id.toString() to cnt }
        val ctxMap = state.userContexts.entries.associate { (id, ctx) ->
            id.toString() to mapOf("en" to ctx.en, "ko" to ctx.ko, "label" to ctx.label)
        }
        val historyList = state.examHistory.take(40).map { rec ->
            mapOf(
                "id" to rec.id,
                "day" to rec.day,
                "score" to rec.score,
                "total" to rec.total,
                "answered" to rec.answered,
                "passed" to rec.passed,
                "elapsedSec" to rec.elapsedSec,
                "timestampIso" to rec.timestampIso,
                "wrongWordIds" to rec.wrongWordIds
            )
        }
        val root = mapOf(
            "selectedDay" to state.selectedDay,
            "freePracticeUnlocked" to state.freePracticeUnlocked,
            "dayProgress" to daysMap,
            "studyAgainCounts" to againMap,
            "userContexts" to ctxMap,
            "examHistory" to historyList
        )
        return mapAdapter.toJson(root)
    }

    fun parseStateJson(rawJson: String): WordRunChallengeState {
        if (rawJson.isBlank()) return WordRunChallengeState()
        return runCatching {
            val root = mapAdapter.fromJson(rawJson) ?: return WordRunChallengeState()
            val selectedDay = ((root["selectedDay"] as? Number)?.toInt() ?: 1).coerceIn(1, WordRun2027Engine.TOTAL_DAYS)
            val freePractice = (root["freePracticeUnlocked"] as? Boolean) == true

            val dayProgress = linkedMapOf<Int, WordRunDayProgress>()
            (root["dayProgress"] as? Map<*, *>)?.forEach { (k, v) ->
                val day = (k as? String)?.toIntOrNull() ?: return@forEach
                val obj = v as? Map<*, *> ?: return@forEach
                val learned = (obj["learnedIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet().orEmpty()
                val practiced = (obj["practicedIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet().orEmpty()
                val wrong = (obj["wrongWordIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }.orEmpty()
                dayProgress[day] = WordRunDayProgress(
                    day = day,
                    reviewCompleted = (obj["reviewCompleted"] as? Boolean) == true,
                    learnedIds = learned,
                    practicedIds = practiced,
                    passedDateKst = (obj["passedDateKst"] as? String).orEmpty(),
                    wrongWordIds = wrong
                )
            }

            val againCounts = linkedMapOf<Int, Int>()
            (root["studyAgainCounts"] as? Map<*, *>)?.forEach { (k, v) ->
                val id = (k as? String)?.toIntOrNull() ?: return@forEach
                val cnt = (v as? Number)?.toInt() ?: return@forEach
                if (cnt > 0) againCounts[id] = cnt
            }

            val userContexts = linkedMapOf<Int, WordRunUserContext>()
            (root["userContexts"] as? Map<*, *>)?.forEach { (k, v) ->
                val id = (k as? String)?.toIntOrNull() ?: return@forEach
                val obj = v as? Map<*, *> ?: return@forEach
                val en = (obj["en"] as? String).orEmpty().take(WordRun2027Engine.MAX_CONTEXT_LENGTH)
                val ko = (obj["ko"] as? String).orEmpty().take(WordRun2027Engine.MAX_CONTEXT_LENGTH)
                if (en.isNotBlank()) {
                    userContexts[id] = WordRunUserContext(
                        en = en,
                        ko = ko,
                        label = (obj["label"] as? String).orEmpty().ifBlank { "직접 등록 · 학습 문맥" }
                    )
                }
            }

            val history = buildList {
                (root["examHistory"] as? List<*>)?.forEach { item ->
                    val obj = item as? Map<*, *> ?: return@forEach
                    add(
                        WordRunExamRecord(
                            id = (obj["id"] as? String).orEmpty().ifBlank { "exam_${size}" },
                            day = ((obj["day"] as? Number)?.toInt() ?: 1).coerceIn(1, WordRun2027Engine.TOTAL_DAYS),
                            score = (obj["score"] as? Number)?.toInt() ?: 0,
                            total = (obj["total"] as? Number)?.toInt() ?: WordRun2027Engine.WORDS_PER_DAY,
                            answered = (obj["answered"] as? Number)?.toInt() ?: 0,
                            passed = (obj["passed"] as? Boolean) == true,
                            elapsedSec = (obj["elapsedSec"] as? Number)?.toInt() ?: 0,
                            timestampIso = (obj["timestampIso"] as? String).orEmpty(),
                            wrongWordIds = (obj["wrongWordIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }.orEmpty()
                        )
                    )
                }
            }

            WordRunChallengeState(
                selectedDay = selectedDay,
                freePracticeUnlocked = freePractice,
                dayProgress = dayProgress,
                studyAgainCounts = againCounts,
                userContexts = userContexts,
                examHistory = history
            )
        }.getOrDefault(WordRunChallengeState())
    }
}
