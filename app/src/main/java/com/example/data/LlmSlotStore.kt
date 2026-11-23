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
        (0 until 10).map { i ->
            val defaultProvider = when (i) {
                0, 1, 2 -> "Groq Free"
                3, 4 -> "OpenRouter Free"
                5, 6 -> "Cerebras / Together"
                else -> "Custom OpenAI-Compatible"
            }
            val defaultUrl = when (i) {
                0, 1, 2 -> "https://api.groq.com/openai/v1"
                3, 4 -> "https://openrouter.ai/api/v1"
                5, 6 -> "https://api.together.xyz/v1"
                else -> "https://api.groq.com/openai/v1"
            }
            val defaultModel = when (i) {
                0, 1, 2 -> "llama-3.3-70b-versatile"
                3, 4 -> "meta-llama/llama-3.3-70b-instruct:free"
                5, 6 -> "meta-llama/Llama-3.3-70B-Instruct-Turbo"
                else -> "llama-3.1-8b-instant"
            }
            LlmSlotConfig(
                slotIndex = i,
                label = prefs[slotLabelKey(i)] ?: "LLM 슬롯 #${i + 1}",
                providerName = prefs[slotProviderKey(i)] ?: defaultProvider,
                baseUrl = prefs[slotUrlKey(i)] ?: defaultUrl,
                modelId = prefs[slotModelKey(i)] ?: defaultModel,
                apiKey = prefs[slotApiKeyKey(i)] ?: "",
                enabled = prefs[slotEnabledKey(i)] ?: (i == 0),
                requestCount = prefs[slotCountKey(i)] ?: 0,
                lastStatus = if ((prefs[slotApiKeyKey(i)] ?: "").isNotBlank()) "키 등록됨" else "키 미입력"
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
