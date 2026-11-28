package com.example.data

import android.content.Context
import com.example.engine.KoreanTranslationRefinerEngine
import com.example.model.VocabExample
import com.example.model.VocabWord
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class WordTranslationOverride(
    val wordId: String,
    val word: String,
    val meaningKo: String,
    val exampleEn: String,
    val exampleKo: String,
    val pronunciationTipKo: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

/**
 * Persistent local store for user-edited and engine-polished word translations.
 *
 * Uses atomic temp-file writes (`word_translation_overrides_v1.json`) inside app-private filesDir
 * so edits immediately propagate across vocabulary flashcards, quizzes, corpus inspection,
 * and AI video-call learned-word questions.
 */
object WordTranslationOverrideStore {

    private const val STORE_FILE_NAME = "word_translation_overrides_v1.json"

    private val moshi by lazy { Moshi.Builder().build() }
    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any?>>(type)
    }

    private val _overrides = MutableStateFlow<Map<String, WordTranslationOverride>>(emptyMap())
    val overrides: StateFlow<Map<String, WordTranslationOverride>> = _overrides.asStateFlow()

    @Volatile
    private var loadedFromDisk = false

    fun ensureLoaded(context: Context): Map<String, WordTranslationOverride> {
        if (loadedFromDisk) return _overrides.value
        synchronized(this) {
            if (loadedFromDisk) return _overrides.value
            val file = File(context.applicationContext.filesDir, STORE_FILE_NAME)
            if (file.exists()) {
                val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
                val parsed = parseOverridesJson(raw)
                _overrides.value = parsed
            }
            loadedFromDisk = true
            return _overrides.value
        }
    }

    fun saveOverride(
        context: Context?,
        wordId: String,
        word: String,
        meaningKo: String,
        exampleEn: String,
        exampleKo: String,
        pronunciationTipKo: String = ""
    ): WordTranslationOverride {
        val cleanMeaning = KoreanTranslationRefinerEngine.normalizeMeaningKo(meaningKo)
        val cleanExampleKo = KoreanTranslationRefinerEngine.polishKorean(exampleKo)
        val cleanTip = KoreanTranslationRefinerEngine.polishKorean(pronunciationTipKo)
        val item = WordTranslationOverride(
            wordId = wordId.trim(),
            word = word.trim(),
            meaningKo = cleanMeaning,
            exampleEn = exampleEn.trim(),
            exampleKo = cleanExampleKo,
            pronunciationTipKo = cleanTip,
            updatedAtMs = System.currentTimeMillis()
        )
        val updated = LinkedHashMap(_overrides.value).apply {
            put(item.wordId, item)
        }
        _overrides.value = updated
        if (context != null) {
            persistAtomically(context, updated)
        }
        return item
    }

    fun removeOverride(context: Context?, wordId: String) {
        val updated = LinkedHashMap(_overrides.value).apply {
            remove(wordId)
        }
        _overrides.value = updated
        if (context != null) {
            persistAtomically(context, updated)
        }
    }

    fun clearAll(context: Context?) {
        _overrides.value = emptyMap()
        if (context != null) {
            runCatching {
                File(context.applicationContext.filesDir, STORE_FILE_NAME).delete()
            }
        }
    }

    fun applyTo(word: VocabWord, map: Map<String, WordTranslationOverride> = _overrides.value): VocabWord {
        val ov = map[word.id] ?: return word
        val newMeaning = ov.meaningKo.ifBlank { word.meaningKo }
        val newExEn = ov.exampleEn.ifBlank { word.exampleEn }
        val newExKo = ov.exampleKo.ifBlank { word.exampleKo }
        val newTip = ov.pronunciationTipKo.ifBlank { word.pronunciationTipKo }
        val updatedExamples = if (word.examples.isEmpty()) {
            listOf(VocabExample("수정된 핵심 예문", newExEn, newExKo, word.ipa))
        } else {
            word.examples.mapIndexed { idx, ex ->
                if (idx == 0) ex.copy(english = newExEn, korean = newExKo) else ex
            }
        }
        return word.copy(
            meaningKo = newMeaning,
            exampleEn = newExEn,
            exampleKo = newExKo,
            examples = updatedExamples,
            pronunciationTipKo = newTip
        )
    }

    fun applyAll(words: List<VocabWord>, map: Map<String, WordTranslationOverride> = _overrides.value): List<VocabWord> {
        if (map.isEmpty()) return words
        return words.map { applyTo(it, map) }
    }

    fun serializeOverridesJson(map: Map<String, WordTranslationOverride>): String {
        val items = map.values.map { ov ->
            mapOf(
                "wordId" to ov.wordId,
                "word" to ov.word,
                "meaningKo" to ov.meaningKo,
                "exampleEn" to ov.exampleEn,
                "exampleKo" to ov.exampleKo,
                "pronunciationTipKo" to ov.pronunciationTipKo,
                "updatedAtMs" to ov.updatedAtMs
            )
        }
        return mapAdapter.toJson(mapOf("schemaVersion" to 1, "items" to items))
    }

    fun parseOverridesJson(rawJson: String): Map<String, WordTranslationOverride> {
        if (rawJson.isBlank()) return emptyMap()
        return runCatching {
            val root = mapAdapter.fromJson(rawJson) ?: return emptyMap()
            val items = root["items"] as? List<*> ?: return emptyMap()
            val out = LinkedHashMap<String, WordTranslationOverride>()
            items.forEach { raw ->
                val obj = raw as? Map<*, *> ?: return@forEach
                val wordId = (obj["wordId"] as? String).orEmpty().trim()
                if (wordId.isBlank()) return@forEach
                out[wordId] = WordTranslationOverride(
                    wordId = wordId,
                    word = (obj["word"] as? String).orEmpty().trim(),
                    meaningKo = (obj["meaningKo"] as? String).orEmpty().trim(),
                    exampleEn = (obj["exampleEn"] as? String).orEmpty().trim(),
                    exampleKo = (obj["exampleKo"] as? String).orEmpty().trim(),
                    pronunciationTipKo = (obj["pronunciationTipKo"] as? String).orEmpty().trim(),
                    updatedAtMs = (obj["updatedAtMs"] as? Number)?.toLong() ?: 0L
                )
            }
            out
        }.getOrDefault(emptyMap())
    }

    private fun persistAtomically(context: Context, map: Map<String, WordTranslationOverride>): Boolean {
        return runCatching {
            val dir = context.applicationContext.filesDir
            val target = File(dir, STORE_FILE_NAME)
            val tmp = File(dir, "$STORE_FILE_NAME.tmp")
            val json = serializeOverridesJson(map)
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
            true
        }.getOrDefault(false)
    }
}
