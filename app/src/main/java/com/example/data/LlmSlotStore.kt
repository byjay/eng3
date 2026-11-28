package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AiEngineMode
import com.example.model.LlmSlotConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.llmSettingsDataStore by preferencesDataStore(name = "speakquest_llm_slots")

class LlmSlotStore(private val context: Context) {

    private val engineModeKey = stringPreferencesKey("ai_engine_mode")
    private val speechRateKey = floatPreferencesKey("speech_rate_multiplier")
    private val handsFreeKey = booleanPreferencesKey("hands_free_friend_mode")
    private val rotationCursorKey = intPreferencesKey("rotation_cursor")

    private fun slotLabelKey(i: Int) = stringPreferencesKey("slot_${i}_label")
    private fun slotProviderKey(i: Int) = stringPreferencesKey("slot_${i}_provider")
    private fun slotUrlKey(i: Int) = stringPreferencesKey("slot_${i}_url")
    private fun slotModelKey(i: Int) = stringPreferencesKey("slot_${i}_model")
    private fun slotApiKeyKey(i: Int) = stringPreferencesKey("slot_${i}_apikey")
    private fun slotEnabledKey(i: Int) = booleanPreferencesKey("slot_${i}_enabled")
    private fun slotCountKey(i: Int) = intPreferencesKey("slot_${i}_count")

    val engineModeFlow: Flow<AiEngineMode> = context.llmSettingsDataStore.data.map { prefs ->
        val raw = prefs[engineModeKey] ?: AiEngineMode.BUILT_IN_GEMINI.name
        AiEngineMode.entries.firstOrNull { it.name == raw } ?: AiEngineMode.BUILT_IN_GEMINI
    }

    val speechRateFlow: Flow<Float> = context.llmSettingsDataStore.data.map { prefs ->
        (prefs[speechRateKey] ?: 1.0f).coerceIn(0.6f, 1.3f)
    }

    val handsFreeFlow: Flow<Boolean> = context.llmSettingsDataStore.data.map { prefs ->
        prefs[handsFreeKey] ?: true
    }

    val slotsFlow: Flow<List<LlmSlotConfig>> = context.llmSettingsDataStore.data.map { prefs ->
        if (!com.example.BuildConfig.DEBUG) {
            return@map emptyList()
        }
        (0 until 10).map { i ->
            val defaultProvider = when (i) {
                0, 1 -> "Groq (Debug Only)"
                2, 3 -> "OpenRouter (Debug Only)"
                4 -> "Cerebras (Debug Only)"
                5 -> "HuggingFace (Debug Only)"
                6 -> "SambaNova (Debug Only)"
                7 -> "Ollama Local (Debug Only)"
                else -> "Custom OpenAI-Compatible (Debug)"
            }
            val defaultUrl = when (i) {
                0, 1 -> "https://api.groq.com/openai/v1"
                2, 3 -> "https://openrouter.ai/api/v1"
                4 -> "https://api.cerebras.ai/v1"
                5 -> "https://router.huggingface.co/v1"
                6 -> "https://api.sambanova.ai/v1"
                7 -> "http://10.0.2.2:11434/v1"
                else -> "https://api.groq.com/openai/v1"
            }
            val defaultModel = when (i) {
                0 -> "llama-3.3-70b-versatile"
                1 -> "llama-3.1-8b-instant"
                2 -> "openrouter/free"
                3 -> "meta-llama/llama-3.3-70b-instruct:free"
                4 -> "llama-3.3-70b"
                5 -> "Qwen/Qwen2.5-72B-Instruct"
                6 -> "Meta-Llama-3.3-70B-Instruct"
                7 -> "llama3.2"
                else -> "llama-3.3-70b-versatile"
            }
            val rawSavedUrl = prefs[slotUrlKey(i)] ?: defaultUrl
            val savedUrl = if (rawSavedUrl.contains("pollinations.ai", ignoreCase = true)) defaultUrl else rawSavedUrl
            val savedKey = prefs[slotApiKeyKey(i)] ?: ""
            val isLocalDebug = savedUrl.startsWith("http://10.0.2.2:") ||
                savedUrl.startsWith("http://127.0.0.1:") ||
                savedUrl.startsWith("http://localhost:")
            LlmSlotConfig(
                slotIndex = i,
                label = prefs[slotLabelKey(i)] ?: "디버그 슬롯 #${i + 1}",
                providerName = prefs[slotProviderKey(i)] ?: defaultProvider,
                baseUrl = savedUrl,
                modelId = prefs[slotModelKey(i)] ?: defaultModel,
                apiKey = savedKey,
                enabled = prefs[slotEnabledKey(i)] ?: false,
                requestCount = prefs[slotCountKey(i)] ?: 0,
                lastStatus = when {
                    savedKey.isNotBlank() -> "디버그 키 설정됨"
                    isLocalDebug -> "로컬 디버그 엔드포인트"
                    else -> "미설정 (기본 Cloudflare Workers AI 사용)"
                }
            )
        }
    }

    suspend fun setEngineMode(mode: AiEngineMode) {
        val safeMode = if (!com.example.BuildConfig.DEBUG && mode == AiEngineMode.EXTERNAL_10_SLOT_ROTATOR) {
            AiEngineMode.BUILT_IN_GEMINI
        } else {
            mode
        }
        context.llmSettingsDataStore.edit { it[engineModeKey] = safeMode.name }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.llmSettingsDataStore.edit { it[speechRateKey] = rate.coerceIn(0.6f, 1.3f) }
    }

    suspend fun setHandsFree(enabled: Boolean) {
        context.llmSettingsDataStore.edit { it[handsFreeKey] = enabled }
    }

    suspend fun saveSlot(slot: LlmSlotConfig) {
        if (!com.example.BuildConfig.DEBUG) return
        val cleanUrl = slot.baseUrl.trim().trimEnd('/')
        if (cleanUrl.contains("pollinations.ai", ignoreCase = true)) return
        val i = slot.slotIndex.coerceIn(0, 9)
        context.llmSettingsDataStore.edit { prefs ->
            prefs[slotLabelKey(i)] = slot.label.trim().ifBlank { "디버그 슬롯 #${i + 1}" }
            prefs[slotProviderKey(i)] = slot.providerName.trim().ifBlank { "Custom Debug" }
            prefs[slotUrlKey(i)] = cleanUrl
            prefs[slotModelKey(i)] = slot.modelId.trim()
            prefs[slotApiKeyKey(i)] = slot.apiKey.trim()
            prefs[slotEnabledKey(i)] = slot.enabled
        }
    }

    suspend fun incrementSlotRequestCount(slotIndex: Int) {
        val i = slotIndex.coerceIn(0, 9)
        context.llmSettingsDataStore.edit { prefs ->
            val current = prefs[slotCountKey(i)] ?: 0
            prefs[slotCountKey(i)] = current + 1
            prefs[rotationCursorKey] = (i + 1) % 10
        }
    }

    suspend fun clearAll() {
        context.llmSettingsDataStore.edit { it.clear() }
    }
}
