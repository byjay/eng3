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
        val raw = prefs[engineModeKey] ?: AiEngineMode.FREE_OPEN_SOURCE_AUTO.name
        AiEngineMode.entries.firstOrNull { it.name == raw } ?: AiEngineMode.FREE_OPEN_SOURCE_AUTO
    }

    val speechRateFlow: Flow<Float> = context.llmSettingsDataStore.data.map { prefs ->
        (prefs[speechRateKey] ?: 1.0f).coerceIn(0.6f, 1.3f)
    }

    val handsFreeFlow: Flow<Boolean> = context.llmSettingsDataStore.data.map { prefs ->
        prefs[handsFreeKey] ?: true
    }

    val slotsFlow: Flow<List<LlmSlotConfig>> = context.llmSettingsDataStore.data.map { prefs ->
        (0 until 10).map { i ->
            val defaultProvider = when (i) {
                0 -> "Pollinations Free (키 불필요)"
                1, 2 -> "Groq Free (카드 불필요 · 300tok/s)"
                3, 4 -> "OpenRouter Free (무료 오픈소스 풀)"
                5 -> "Cerebras Free (초고속 Llama)"
                6 -> "HuggingFace Free (Qwen2.5)"
                7 -> "SambaNova Free (Llama 3.3)"
                8 -> "Ollama Local (PC 무료 로컬)"
                else -> "Custom OpenAI-Compatible"
            }
            val defaultUrl = when (i) {
                0 -> "https://text.pollinations.ai/openai"
                1, 2 -> "https://api.groq.com/openai/v1"
                3, 4 -> "https://openrouter.ai/api/v1"
                5 -> "https://api.cerebras.ai/v1"
                6 -> "https://router.huggingface.co/v1"
                7 -> "https://api.sambanova.ai/v1"
                8 -> "http://10.0.2.2:11434/v1"
                else -> "https://api.groq.com/openai/v1"
            }
            val defaultModel = when (i) {
                0 -> "openai"
                1 -> "llama-3.3-70b-versatile"
                2 -> "llama-3.1-8b-instant"
                3 -> "openrouter/free"
                4 -> "meta-llama/llama-3.3-70b-instruct:free"
                5 -> "llama-3.3-70b"
                6 -> "Qwen/Qwen2.5-72B-Instruct"
                7 -> "Meta-Llama-3.3-70B-Instruct"
                8 -> "llama3.2"
                else -> "llama-3.3-70b-versatile"
            }
            val savedUrl = prefs[slotUrlKey(i)] ?: defaultUrl
            val savedKey = prefs[slotApiKeyKey(i)] ?: ""
            val isKeyless = savedUrl.contains("pollinations.ai", ignoreCase = true) ||
                savedUrl.contains("10.0.2.2") ||
                savedUrl.contains("127.0.0.1") ||
                savedUrl.contains("localhost") ||
                savedUrl.contains("192.168.")
            LlmSlotConfig(
                slotIndex = i,
                label = prefs[slotLabelKey(i)] ?: "LLM 슬롯 #${i + 1}",
                providerName = prefs[slotProviderKey(i)] ?: defaultProvider,
                baseUrl = savedUrl,
                modelId = prefs[slotModelKey(i)] ?: defaultModel,
                apiKey = savedKey,
                enabled = prefs[slotEnabledKey(i)] ?: (i == 0),
                requestCount = prefs[slotCountKey(i)] ?: 0,
                lastStatus = when {
                    savedKey.isNotBlank() -> "무료 키 등록됨"
                    isKeyless -> "키 불필요 · 즉시 사용 가능"
                    else -> "무료 API 키 미입력"
                }
            )
        }
    }

    suspend fun setEngineMode(mode: AiEngineMode) {
        context.llmSettingsDataStore.edit { it[engineModeKey] = mode.name }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.llmSettingsDataStore.edit { it[speechRateKey] = rate.coerceIn(0.6f, 1.3f) }
    }

    suspend fun setHandsFree(enabled: Boolean) {
        context.llmSettingsDataStore.edit { it[handsFreeKey] = enabled }
    }

    suspend fun saveSlot(slot: LlmSlotConfig) {
        val i = slot.slotIndex.coerceIn(0, 9)
        context.llmSettingsDataStore.edit { prefs ->
            prefs[slotLabelKey(i)] = slot.label.trim().ifBlank { "LLM 슬롯 #${i + 1}" }
            prefs[slotProviderKey(i)] = slot.providerName.trim().ifBlank { "Groq Free" }
            prefs[slotUrlKey(i)] = slot.baseUrl.trim().trimEnd('/')
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
}
