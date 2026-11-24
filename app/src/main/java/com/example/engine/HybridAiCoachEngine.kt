package com.example.engine

import android.util.Log
import com.example.BuildConfig
import com.example.data.LlmSlotStore
import com.example.model.AiEngineMode
import com.example.model.AiHumanTutor
import com.example.model.LlmSlotConfig
import com.example.model.TravelTwist
import com.example.model.TutorChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "HybridAiCoachEngine"
private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
private const val GROQ_MODEL_OSS_120B = "openai/gpt-oss-120b"

data class SuggestedReplyOption(
    val english: String,
    val korean: String
)

data class AiCoachReply(
    val replyEn: String,
    val translationKo: String,
    val coachingTipKo: String,
    val naturalRephraseEn: String,
    val grammarFixKo: String = "",
    val suggestedReplies: List<SuggestedReplyOption> = emptyList(),
    val engineBadge: String,
    val thoughtKo: String = "",
    val goalProgressKo: String = "",
    val characterMood: String = "happy"
)

data class GroqKeyProbeResult(
    val keyIndex: Int,
    val success: Boolean,
    val httpCode: Int,
    val latencyMs: Long,
    val chunkCount: Int,
    val responsePreview: String,
    val errorMessage: String? = null
)

class HybridAiCoachEngine(
    private val slotStore: LlmSlotStore? = null,
    private val groqEndpointUrl: String = GROQ_ENDPOINT,
    private val customGroqKeysProvider: (() -> List<Pair<Int, String>>)? = null
) {
    private val moshi by lazy { Moshi.Builder().build() }
    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any?>>(type)
    }

    private fun logWarn(msg: String, tr: Throwable? = null) {
        runCatching {
            if (tr != null) Log.w(TAG, msg, tr) else Log.w(TAG, msg)
        }
    }

    private fun buildGroqOss120bPayloadJson(
        systemPrompt: String?,
        userPrompt: String
    ): String {
        val messages = buildList {
            if (!systemPrompt.isNullOrBlank()) {
                add(mapOf("role" to "system", "content" to systemPrompt))
            }
            add(mapOf("role" to "user", "content" to userPrompt))
        }
        val payload = linkedMapOf<String, Any?>(
            "model" to GROQ_MODEL_OSS_120B,
            "messages" to messages,
            "temperature" to 1.0,
            "max_completion_tokens" to 2048,
            "top_p" to 1.0,
            "reasoning_effort" to "medium",
            "stream" to true
        )
        return mapAdapter.toJson(payload)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractGroqDeltaContent(jsonData: String): String {
        val parsed = runCatching { mapAdapter.fromJson(jsonData) }.getOrNull() ?: return ""
        val choices = parsed["choices"] as? List<*> ?: return ""
        val first = choices.firstOrNull() as? Map<*, *> ?: return ""
        val delta = first["delta"] as? Map<*, *> ?: return ""
        return (delta["content"] as? String).orEmpty()
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractGroqMessageContent(jsonBody: String): String {
        val parsed = runCatching { mapAdapter.fromJson(jsonBody) }.getOrNull() ?: return ""
        val choices = parsed["choices"] as? List<*> ?: return ""
        val first = choices.firstOrNull() as? Map<*, *> ?: return ""
        val message = first["message"] as? Map<*, *> ?: return ""
        return (message["content"] as? String).orEmpty()
    }
    private val primaryGeminiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }
    private val fastFallbackGeminiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.5-flash-lite")
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(24, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val groqCursor = AtomicInteger(0)
    private val groqKeyCooldownUntil = ConcurrentHashMap<Int, Long>()
    private val roundRobinCursor = AtomicInteger(0)
    private val slotCooldownUntil = ConcurrentHashMap<Int, Long>()

    @Volatile
    private var geminiCooldownUntilMs: Long = 0L

    @Volatile
    private var pollinationsCooldownUntilMs: Long = 0L

    /**
     * Collects all valid Groq API keys injected from AI Studio Secrets Panel (.env -> BuildConfig).
     * Filters out empty strings or placeholder values automatically.
     */
    fun configuredGroqEnvKeys(): List<Pair<Int, String>> {
        customGroqKeysProvider?.let { return it() }
        val raw = listOf(
            1 to BuildConfig.GROQ_API_KEY_1,
            2 to BuildConfig.GROQ_API_KEY_2,
            3 to BuildConfig.GROQ_API_KEY_3,
            4 to BuildConfig.GROQ_API_KEY_4,
            5 to BuildConfig.GROQ_API_KEY_5
        )
        return raw.mapNotNull { (idx, value) ->
            val trimmed = value.trim()
            if (trimmed.isBlank() || trimmed.startsWith("YOUR_GROQ_API_KEY", ignoreCase = true)) {
                null
            } else {
                idx to trimmed
            }
        }
    }

    /**
     * Direct diagnostic probe for a single Groq API key against `openai/gpt-oss-120b` (SSE streaming).
     */
    suspend fun probeGroqKey(
        keyNum: Int,
        apiKey: String,
        userMessage: String = "Say hello in one short English sentence."
    ): GroqKeyProbeResult = withContext(Dispatchers.IO) {
        val startNs = System.nanoTime()
        var chunkCount = 0
        var lastHttpCode = -1
        try {
            val payloadJson = buildGroqOss120bPayloadJson(
                systemPrompt = null,
                userPrompt = userMessage
            )
            val req = Request.Builder()
                .url(groqEndpointUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .post(payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                lastHttpCode = resp.code
                val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty().take(240)
                    return@withContext GroqKeyProbeResult(
                        keyIndex = keyNum,
                        success = false,
                        httpCode = resp.code,
                        latencyMs = elapsedMs,
                        chunkCount = 0,
                        responsePreview = "",
                        errorMessage = "HTTP ${resp.code}: $errBody"
                    )
                }
                val body = resp.body ?: return@withContext GroqKeyProbeResult(
                    keyIndex = keyNum,
                    success = false,
                    httpCode = resp.code,
                    latencyMs = elapsedMs,
                    chunkCount = 0,
                    responsePreview = "",
                    errorMessage = "Empty response body"
                )
                val sb = StringBuilder()
                val contentType = resp.header("Content-Type").orEmpty()
                if (contentType.contains("text/event-stream", ignoreCase = true)) {
                    BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmed = line?.trim().orEmpty()
                            if (!trimmed.startsWith("data:")) continue
                            val data = trimmed.removePrefix("data:").trim()
                            if (data == "[DONE]" || data.isBlank()) continue
                            val deltaContent = extractGroqDeltaContent(data)
                            if (deltaContent.isNotEmpty()) {
                                chunkCount++
                                sb.append(deltaContent)
                            }
                        }
                    }
                } else {
                    val text = extractGroqMessageContent(body.string())
                    if (text.isNotEmpty()) {
                        chunkCount = 1
                        sb.append(text)
                    }
                }
                val totalMs = (System.nanoTime() - startNs) / 1_000_000L
                val resultText = sb.toString().trim()
                GroqKeyProbeResult(
                    keyIndex = keyNum,
                    success = resultText.isNotBlank(),
                    httpCode = resp.code,
                    latencyMs = totalMs,
                    chunkCount = chunkCount,
                    responsePreview = resultText.take(180),
                    errorMessage = if (resultText.isBlank()) "Stream completed with empty content" else null
                )
            }
        } catch (e: Exception) {
            val totalMs = (System.nanoTime() - startNs) / 1_000_000L
            GroqKeyProbeResult(
                keyIndex = keyNum,
                success = false,
                httpCode = lastHttpCode,
                latencyMs = totalMs,
                chunkCount = chunkCount,
                responsePreview = "",
                errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun isKoreanExpressionQuestion(text: String): Boolean {
        val hasHangul = text.any { it in '\uAC00'..'\uD7A3' }
        val asksEnglish = text.contains("영어") || text.contains("어떻게 말") ||
            text.contains("알려줘") || text.contains("표현") || text.contains("번역") ||
            text.contains("뭐라고") || text.contains("해줘")
        return hasHangul && (asksEnglish || text.length >= 4)
    }

    suspend fun generateFriendReply(
        tutor: AiHumanTutor,
        userSpokenText: String,
        history: List<TutorChatMessage>,
        mode: AiEngineMode,
        slots: List<LlmSlotConfig>,
        activeTopicTag: String = "수다",
        activeTwist: TravelTwist? = null,
        learnerName: String = "원삐",
        conversationGoalKo: String = "",
        onStreamChunk: ((String) -> Unit)? = null
    ): AiCoachReply {
        val resolvedGoal = conversationGoalKo.ifBlank { goalForTopic(activeTopicTag) }
        val prompt = buildTutorPrompt(
            tutor = tutor,
            userSpokenText = userSpokenText,
            history = history,
            activeTopicTag = activeTopicTag,
            activeTwist = activeTwist,
            learnerName = learnerName,
            conversationGoalKo = resolvedGoal
        )

        return when (mode) {
            AiEngineMode.OFFLINE_ONLY -> {
                LocalConversationSkillEngine.generateReply(
                    tutor, userSpokenText, history, activeTopicTag, activeTwist, learnerName, resolvedGoal
                )
            }

            AiEngineMode.FREE_OPEN_SOURCE_AUTO -> {
                tryGroq5KeyRotation(prompt, onStreamChunk)
                    ?: tryExternalSlotRotation(prompt, slots, keyedOnly = true)
                    ?: tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: LocalConversationSkillEngine.generateReply(
                        tutor, userSpokenText, history, activeTopicTag, activeTwist, learnerName, resolvedGoal
                    )
            }

            AiEngineMode.EXTERNAL_10_SLOT_ROTATOR -> {
                tryGroq5KeyRotation(prompt, onStreamChunk)
                    ?: tryExternalSlotRotation(prompt, slots, keyedOnly = false)
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: LocalConversationSkillEngine.generateReply(
                        tutor, userSpokenText, history, activeTopicTag, activeTwist, learnerName, resolvedGoal
                    )
            }

            AiEngineMode.BUILT_IN_GEMINI -> {
                tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: tryGroq5KeyRotation(prompt, onStreamChunk)
                    ?: tryExternalSlotRotation(prompt, slots, keyedOnly = false)
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: LocalConversationSkillEngine.generateReply(
                        tutor, userSpokenText, history, activeTopicTag, activeTwist, learnerName, resolvedGoal
                    )
            }
        }
    }

    /**
     * Proactive Thinking API Turn:
     * Actively calls the configured AI API (Groq 5-key / Gemini / external slots) so the character
     * thinks independently about the situational topic & goal, calls the learner by name,
     * and initiates or advances the conversation without waiting passively.
     */
    suspend fun generateProactiveTopicTurn(
        tutor: AiHumanTutor,
        learnerName: String,
        activeTopicTag: String,
        conversationGoalKo: String,
        history: List<TutorChatMessage>,
        mode: AiEngineMode,
        slots: List<LlmSlotConfig>,
        isOpening: Boolean,
        onStreamChunk: ((String) -> Unit)? = null
    ): AiCoachReply {
        val resolvedGoal = conversationGoalKo.ifBlank { goalForTopic(activeTopicTag) }
        val prompt = buildProactivePrompt(
            tutor = tutor,
            learnerName = learnerName,
            activeTopicTag = activeTopicTag,
            conversationGoalKo = resolvedGoal,
            history = history,
            isOpening = isOpening
        )

        return when (mode) {
            AiEngineMode.OFFLINE_ONLY -> {
                LocalConversationSkillEngine.generateProactiveOpener(
                    tutor = tutor,
                    learnerName = learnerName,
                    activeTopicTag = activeTopicTag,
                    conversationGoalKo = resolvedGoal,
                    turnCount = history.size,
                    isOpening = isOpening
                )
            }

            AiEngineMode.FREE_OPEN_SOURCE_AUTO -> {
                tryGroq5KeyRotation(prompt, onStreamChunk)
                    ?: tryExternalSlotRotation(prompt, slots, keyedOnly = true)
                    ?: tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: LocalConversationSkillEngine.generateProactiveOpener(
                        tutor, learnerName, activeTopicTag, resolvedGoal, history.size, isOpening
                    )
            }

            AiEngineMode.EXTERNAL_10_SLOT_ROTATOR -> {
                tryGroq5KeyRotation(prompt, onStreamChunk)
                    ?: tryExternalSlotRotation(prompt, slots, keyedOnly = false)
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: LocalConversationSkillEngine.generateProactiveOpener(
                        tutor, learnerName, activeTopicTag, resolvedGoal, history.size, isOpening
                    )
            }

            AiEngineMode.BUILT_IN_GEMINI -> {
                tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: tryGroq5KeyRotation(prompt, onStreamChunk)
                    ?: tryExternalSlotRotation(prompt, slots, keyedOnly = false)
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: LocalConversationSkillEngine.generateProactiveOpener(
                        tutor, learnerName, activeTopicTag, resolvedGoal, history.size, isOpening
                    )
            }
        }
    }

    /**
     * 5-Key Groq Round-Robin Rotator using `openai/gpt-oss-120b`:
     * - Rotates keys #1..#5 evenly on every turn via AtomicInteger
     * - Automatically fails over to the next key on HTTP 429 / 401 / 503 with 60s cooldown
     * - Streams SSE chunks (`stream = true`, `reasoning_effort = "medium"`, `max_completion_tokens = 2048`)
     */
    internal suspend fun tryGroq5KeyRotation(
        prompt: String,
        onStreamChunk: ((String) -> Unit)?
    ): AiCoachReply? = withContext(Dispatchers.IO) {
        val pool = configuredGroqEnvKeys()
        if (pool.isEmpty()) return@withContext null

        val now = System.currentTimeMillis()
        val startIdx = Math.floorMod(groqCursor.getAndIncrement(), pool.size)

        for (offset in pool.indices) {
            val (keyNum, apiKey) = pool[(startIdx + offset) % pool.size]
            val cooldown = groqKeyCooldownUntil[keyNum] ?: 0L
            if (cooldown > now) continue

            val raw = callGroqOss120bStreaming(apiKey, keyNum, prompt, onStreamChunk)
            if (raw != null) {
                return@withContext parseStructuredReply(raw, "Groq #$keyNum · gpt-oss-120b")
            } else {
                groqKeyCooldownUntil[keyNum] = System.currentTimeMillis() + 60_000L
            }
        }
        null
    }

    private fun callGroqOss120bStreaming(
        apiKey: String,
        keyNum: Int,
        prompt: String,
        onStreamChunk: ((String) -> Unit)?
    ): String? {
        return try {
            val payloadJson = buildGroqOss120bPayloadJson(
                systemPrompt = "You are a warm, elite native English video-call coach. Follow the requested 8-line format strictly.",
                userPrompt = prompt
            )

            val req = Request.Builder()
                .url(groqEndpointUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer $apiKey")
                .post(payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    logWarn("Groq Key #$keyNum HTTP ${resp.code}")
                    return null
                }
                val body = resp.body ?: return null
                val contentType = resp.header("Content-Type").orEmpty()

                if (contentType.contains("text/event-stream", ignoreCase = true)) {
                    val sb = StringBuilder()
                    BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmed = line?.trim().orEmpty()
                            if (!trimmed.startsWith("data:")) continue
                            val data = trimmed.removePrefix("data:").trim()
                            if (data == "[DONE]" || data.isBlank()) continue
                            val delta = extractGroqDeltaContent(data)
                            if (delta.isNotEmpty()) {
                                sb.append(delta)
                                val preview = extractLiveReplyPreview(sb.toString())
                                if (preview.isNotBlank()) {
                                    onStreamChunk?.invoke(preview)
                                }
                            }
                        }
                    }
                    sb.toString().trim().takeIf { it.isNotBlank() }
                } else {
                    extractGroqMessageContent(body.string()).trim().takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            logWarn("Groq Key #$keyNum stream error: ${e.message}")
            null
        }
    }

    private fun buildTutorPrompt(
        tutor: AiHumanTutor,
        userSpokenText: String,
        history: List<TutorChatMessage>,
        activeTopicTag: String,
        activeTwist: TravelTwist?,
        learnerName: String,
        conversationGoalKo: String
    ): String {
        val enName = formatLearnerEnglishName(learnerName)
        val koName = learnerName.trim().ifBlank { "원삐" }
        val recentLines = history.takeLast(6).joinToString("\n") { msg ->
            val speaker = if (msg.fromTutor) tutor.name else enName
            "$speaker: ${msg.textEn}"
        }
        val twistBlock = if (activeTwist != null) {
            """
            ACTIVE TRAVEL TWIST ROLEPLAY:
            Situation: ${activeTwist.titleKo}
            Staff just said: "${activeTwist.staffPromptEn}"
            Stay in character as a friendly real person responding smoothly to $enName.
            """.trimIndent()
        } else {
            """
            ACTIVE SITUATION / TOPIC: $activeTopicTag
            CONVERSATION GOAL (목표): $conversationGoalKo
            LEARNER NAME: "$enName" (Korean: "$koName")
            PROACTIVE CHARACTER DIRECTIVE:
            - You are an actively thinking character in this exact situation ($activeTopicTag), NOT a passive chatbot.
            - Call the learner warmly by their name ("$enName" in REPLY_EN and "${koName}님" or "${koName}" in TRANSLATION_KO) naturally.
            - Actively steer the scene toward the goal ($conversationGoalKo) by introducing vivid situational details, choices, or a fun follow-up question!
            """.trimIndent()
        }
        val koreanQuestionBlock = if (isKoreanExpressionQuestion(userSpokenText)) {
            """
            IMPORTANT: $enName asked in Korean how to say something in English during a live video call.
            - In REPLY_EN: Call $enName warmly, give the exact natural English sentence right away, and invite them to say it back in this $activeTopicTag scene!
            - In REPHRASE_EN: Put ONLY the exact clean target English expression for them to shadow and practice.
            - In GRAMMAR_KO: Explain in 1 crisp Korean sentence why natives prefer this phrasing.
            - In TRANSLATION_KO: Friendly Korean subtitle of REPLY_EN calling ${koName}님 naturally.
            - In TIP_KO: 1 crisp pronunciation/stress/linking coaching tip in Korean.
            """.trimIndent()
        } else {
            """
            $enName spoke English during a live situational video call ($activeTopicTag).
            - In REPLY_EN: React genuinely to what $enName said, call their name ("$enName") naturally, and proactively advance the $activeTopicTag situation toward the goal (1-2 short energetic spoken sentences max, ending with an engaging question).
            - In REPHRASE_EN: Upgrade $enName's sentence into smoother, more authentic native English.
            - In GRAMMAR_KO: 1 short Korean pinpoint tip comparing what they said vs. the upgraded native phrase.
            - In TIP_KO: 1 short Korean pronunciation/intonation/rhythm tip.
            """.trimIndent()
        }

        return """
            ${tutor.friendPersonaPrompt}
            $twistBlock
            $koreanQuestionBlock

            Recent video-call transcript:
            $recentLines

            $enName just said: "$userSpokenText"

            Respond in EXACTLY 11 lines with these exact prefixes (plain text only, no markdown, no bullets):
            THOUGHT_KO: <1 short Korean inner thought showing how you are proactively steering this $activeTopicTag conversation for ${koName}님>
            GOAL_KO: <Short Korean status of the active conversation goal (목표)>
            MOOD: <One word only: happy | curious | excited | proud | thinking>
            REPLY_EN: <Your natural spoken English response calling $enName by name, 1-2 short sentences>
            TRANSLATION_KO: <Warm natural Korean live subtitle of REPLY_EN>
            REPHRASE_EN: <Upgraded native English sentence for $enName to practice/shadow>
            GRAMMAR_KO: <1 short Korean explanation of the grammar/expression upgrade>
            TIP_KO: <1 short actionable Korean pronunciation/intonation coaching tip>
            HINT1: <Natural short English reply option 1 to your REPLY_EN> | <Korean meaning>
            HINT2: <Natural short English reply option 2 to your REPLY_EN> | <Korean meaning>
            HINT3: <Natural short English reply option 3 to your REPLY_EN> | <Korean meaning>
        """.trimIndent()
    }

    private fun buildProactivePrompt(
        tutor: AiHumanTutor,
        learnerName: String,
        activeTopicTag: String,
        conversationGoalKo: String,
        history: List<TutorChatMessage>,
        isOpening: Boolean
    ): String {
        val enName = formatLearnerEnglishName(learnerName)
        val koName = learnerName.trim().ifBlank { "원삐" }
        val recentLines = history.takeLast(6).joinToString("\n") { msg ->
            val speaker = if (msg.fromTutor) tutor.name else enName
            "$speaker: ${msg.textEn}"
        }.ifBlank { "(New scene just started)" }

        val stageInstruction = if (isOpening) {
            "You just walked into the '$activeTopicTag' scene with $enName! Proactively greet $enName BY NAME, set a vivid real-life vibe, and ask an inviting first question aligned with the goal: $conversationGoalKo."
        } else {
            "$enName paused quietly for a moment during the '$activeTopicTag' conversation. Proactively think of a helpful next cue, call $enName BY NAME gently, offer a concrete choice or fun situational prompt so they feel excited to speak, moving closer to the goal: $conversationGoalKo."
        }

        return """
            ${tutor.friendPersonaPrompt}
            ACTIVE SITUATION / TOPIC: $activeTopicTag
            CONVERSATION GOAL (목표): $conversationGoalKo
            LEARNER NAME: "$enName" (Korean: "$koName")

            PROACTIVE TASK:
            $stageInstruction

            Recent transcript:
            $recentLines

            Respond in EXACTLY 11 lines with these exact prefixes (plain text only, no markdown, no bullets):
            THOUGHT_KO: <1 short Korean inner thought about how you proactively lead ${koName}님 in this $activeTopicTag scene>
            GOAL_KO: <Short Korean label of the current goal (목표)>
            MOOD: <One word only: happy | curious | excited | proud | thinking>
            REPLY_EN: <Your natural spoken English line calling $enName by name, 1-2 short energetic sentences ending with an easy engaging question>
            TRANSLATION_KO: <Warm natural Korean subtitle of REPLY_EN>
            REPHRASE_EN: <One easy native English starter sentence $enName can say right now>
            GRAMMAR_KO: <1 short Korean tip on a key pattern useful for this $activeTopicTag goal>
            TIP_KO: <1 short Korean voice/pronunciation confidence tip>
            HINT1: <Easy natural English reply 1> | <Korean meaning>
            HINT2: <Easy natural English reply 2> | <Korean meaning>
            HINT3: <Easy natural English reply 3> | <Korean meaning>
        """.trimIndent()
    }

    private suspend fun tryGeminiBuiltIn(
        prompt: String,
        onStreamChunk: ((String) -> Unit)?
    ): AiCoachReply? {
        val now = System.currentTimeMillis()
        if (now < geminiCooldownUntilMs) {
            return null
        }
        val primary = streamFromModel(primaryGeminiModel, prompt, "Gemini 3.8", timeoutMs = 14_000L, onStreamChunk = onStreamChunk)
        if (primary != null) return primary
        Log.w(TAG, "Primary Gemini model unavailable, switching seamlessly to gemini-3.5-flash-lite")
        val fallback = streamFromModel(fastFallbackGeminiModel, prompt, "Gemini Lite", timeoutMs = 10_000L, onStreamChunk = onStreamChunk)
        if (fallback == null) {
            geminiCooldownUntilMs = System.currentTimeMillis() + 90_000L
        }
        return fallback
    }

    private suspend fun streamFromModel(
        model: com.google.firebase.ai.GenerativeModel,
        prompt: String,
        badge: String,
        timeoutMs: Long = 14_000L,
        onStreamChunk: ((String) -> Unit)?
    ): AiCoachReply? {
        return try {
            withTimeout(timeoutMs) {
                val sb = StringBuilder()
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        sb.append(piece)
                        val livePreview = extractLiveReplyPreview(sb.toString())
                        if (livePreview.isNotBlank()) {
                            onStreamChunk?.invoke(livePreview)
                        }
                    }
                }
                val raw = sb.toString().trim()
                if (raw.isBlank()) null else parseStructuredReply(raw, badge)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "$badge stream timed out after ${timeoutMs}ms", e)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$badge call failed gracefully: ${e.message}")
            null
        }
    }

    private fun isKeylessEndpoint(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("pollinations.ai") ||
            u.contains("localhost") ||
            u.contains("127.0.0.1") ||
            u.contains("10.0.2.2") ||
            u.contains("192.168.")
    }

    private suspend fun tryFreeKeylessOpenSourceLlm(prompt: String): AiCoachReply? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now < pollinationsCooldownUntilMs) {
            return@withContext null
        }
        val result = try {
            withTimeout(9_000L) {
                val freeSlot = LlmSlotConfig(
                    slotIndex = 0,
                    label = "Pollinations Free",
                    providerName = "오픈소스 LLM",
                    baseUrl = "https://text.pollinations.ai/openai",
                    modelId = "openai",
                    apiKey = "",
                    enabled = true
                )
                callOpenAiCompatibleSlot(freeSlot, prompt)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Keyless open-source LLM timed out or failed: ${e.message}")
            null
        }
        if (result == null) {
            pollinationsCooldownUntilMs = System.currentTimeMillis() + 60_000L
            null
        } else {
            parseStructuredReply(result, "오픈소스 LLM")
        }
    }

    private suspend fun tryExternalSlotRotation(
        prompt: String,
        slots: List<LlmSlotConfig>,
        keyedOnly: Boolean = false
    ): AiCoachReply? = withContext(Dispatchers.IO) {
        val activeSlots = slots.filter { slot ->
            slot.enabled && slot.baseUrl.isNotBlank() &&
                if (keyedOnly) slot.apiKey.isNotBlank()
                else (slot.apiKey.isNotBlank() || isKeylessEndpoint(slot.baseUrl))
        }
        if (activeSlots.isEmpty()) return@withContext null

        val now = System.currentTimeMillis()
        val startIdx = Math.floorMod(roundRobinCursor.getAndIncrement(), activeSlots.size)

        for (offset in activeSlots.indices) {
            val slot = activeSlots[(startIdx + offset) % activeSlots.size]
            val cooldown = slotCooldownUntil[slot.slotIndex] ?: 0L
            if (cooldown > now) continue

            val result = callOpenAiCompatibleSlot(slot, prompt)
            if (result != null) {
                slotStore?.incrementSlotRequestCount(slot.slotIndex)
                return@withContext parseStructuredReply(
                    result,
                    "슬롯#${slot.slotIndex + 1} ${slot.providerName}"
                )
            } else {
                slotCooldownUntil[slot.slotIndex] = System.currentTimeMillis() + 60_000L
            }
        }
        null
    }

    suspend fun testExternalSlot(slot: LlmSlotConfig): Result<String> = withContext(Dispatchers.IO) {
        if (slot.baseUrl.isBlank() || (slot.apiKey.isBlank() && !isKeylessEndpoint(slot.baseUrl))) {
            return@withContext Result.failure(IllegalArgumentException("API 키와 Base URL을 먼저 입력해 주세요."))
        }
        val response = callOpenAiCompatibleSlot(
            slot = slot,
            prompt = "Reply in one cheerful short English sentence confirming the voice connection is ready."
        )
        if (response != null) {
            slotStore?.incrementSlotRequestCount(slot.slotIndex)
            Result.success(response.trim())
        } else {
            Result.failure(IllegalStateException("응답 실패 (API 키·모델 ID·네트워크를 확인해 주세요)"))
        }
    }

    private fun callOpenAiCompatibleSlot(slot: LlmSlotConfig, prompt: String): String? {
        return try {
            val cleanBase = slot.baseUrl.trim().trimEnd('/')
            val endpoint = when {
                cleanBase.endsWith("/chat/completions") -> cleanBase
                cleanBase.contains("text.pollinations.ai/openai", ignoreCase = true) -> cleanBase
                else -> "$cleanBase/chat/completions"
            }

            val messagesArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a warm, elite native English video-call coach. Follow the requested 8-line format strictly.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            val modelName = slot.modelId.trim().ifBlank { GROQ_MODEL_OSS_120B }
            val isGroqOss = cleanBase.contains("groq.com", ignoreCase = true) && modelName.contains("gpt-oss", ignoreCase = true)
            val payload = JSONObject().apply {
                put("model", modelName)
                put("messages", messagesArr)
                if (isGroqOss) {
                    put("temperature", 1.0)
                    put("max_completion_tokens", 2048)
                    put("top_p", 1.0)
                    put("reasoning_effort", "medium")
                } else {
                    put("temperature", 0.7)
                    put("max_tokens", 360)
                }
            }

            val builder = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            if (slot.apiKey.isNotBlank()) {
                builder.header("Authorization", "Bearer ${slot.apiKey.trim()}")
            }
            if (cleanBase.contains("openrouter.ai", ignoreCase = true)) {
                builder.header("HTTP-Referer", "https://dadquest.app")
                builder.header("X-Title", "DadQuest English Tutor")
            }

            httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Slot #${slot.slotIndex + 1} HTTP ${resp.code}")
                    return null
                }
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val choices = json.optJSONArray("choices") ?: return null
                if (choices.length() == 0) return null
                choices.getJSONObject(0)
                    .optJSONObject("message")
                    ?.optString("content")
                    ?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Slot #${slot.slotIndex + 1} network error: ${e.message}")
            null
        }
    }

    private fun extractLiveReplyPreview(raw: String): String {
        val replyLine = raw.lines().firstOrNull { it.trimStart().startsWith("REPLY_EN:") }
        return if (replyLine != null) {
            replyLine.substringAfter("REPLY_EN:").trim()
        } else {
            val first = raw.lines().firstOrNull()?.trim().orEmpty()
            val isMetaLine = first.startsWith("THOUGHT", true) ||
                first.startsWith("GOAL", true) ||
                first.startsWith("MOOD", true) ||
                first.startsWith("TRANSLATION", true) ||
                first.startsWith("REPHRASE", true) ||
                first.startsWith("HINT", true)
            if (isMetaLine) "" else first
        }
    }

    private fun parseStructuredReply(raw: String, badge: String): AiCoachReply {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        fun grab(prefix: String): String? =
            lines.firstOrNull { it.startsWith(prefix, ignoreCase = true) }
                ?.substringAfter(":")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val thoughtKo = grab("THOUGHT_KO").orEmpty()
        val goalProgressKo = grab("GOAL_KO").orEmpty()
        val characterMood = grab("MOOD")
            ?.lowercase()
            ?.takeIf { it in setOf("happy", "curious", "excited", "proud", "thinking", "neutral") }
            ?: "happy"

        val replyEn = grab("REPLY_EN")
            ?: lines.firstOrNull {
                !it.startsWith("THOUGHT", true) &&
                    !it.startsWith("GOAL", true) &&
                    !it.startsWith("MOOD", true) &&
                    !it.startsWith("TRANSLATION", true) &&
                    !it.startsWith("REPHRASE", true) &&
                    !it.startsWith("GRAMMAR", true) &&
                    !it.startsWith("TIP", true) &&
                    !it.startsWith("HINT", true)
            }
            ?: "That sounds awesome! Tell me a little more about that!"
        val translationKo = grab("TRANSLATION_KO")
            ?: "정말 멋진 이야기야! 조금만 더 들려줄래?"
        val rephraseEn = grab("REPHRASE_EN") ?: replyEn
        val grammarFixKo = grab("GRAMMAR_KO").orEmpty()
        val tipKo = grab("TIP_KO")
            ?: "핵심 동사와 명사에 강세를 실어 한 호흡으로 이어 말하면 원어민 리듬이 살아나요."

        val parsedHints = listOfNotNull(grab("HINT1"), grab("HINT2"), grab("HINT3")).mapNotNull { line ->
            val parts = line.split("|")
            val en = parts.getOrNull(0)?.trim().orEmpty()
            val ko = parts.getOrNull(1)?.trim().orEmpty()
            if (en.isNotBlank()) SuggestedReplyOption(en, ko.ifBlank { "자연스럽게 이어 답하기" }) else null
        }

        return AiCoachReply(
            replyEn = replyEn,
            translationKo = translationKo,
            coachingTipKo = tipKo,
            naturalRephraseEn = rephraseEn,
            grammarFixKo = grammarFixKo,
            suggestedReplies = parsedHints,
            engineBadge = badge,
            thoughtKo = thoughtKo,
            goalProgressKo = goalProgressKo,
            characterMood = characterMood
        )
    }

    companion object {
        fun formatLearnerEnglishName(rawName: String): String {
            val cleaned = rawName.trim()
            if (cleaned.isBlank()) return "friend"
            return when (cleaned) {
                "원삐" -> "Wonppi"
                "투삐" -> "Tooppi"
                "손님" -> "friend"
                else -> cleaned
            }
        }

        fun goalForTopic(topicTag: String): String {
            return when {
                topicTag.contains("공항") -> "입국 목적·체류 기간·게이트 질문 자연스럽게 완주하기"
                topicTag.contains("카페") -> "음료 주문·얼음/당도 옵션 선택·결제까지 끝내기"
                topicTag.contains("식당") || topicTag.contains("레스토랑") -> "좌석 요청·추천 메뉴 주문·계산서 요청까지 해내기"
                topicTag.contains("호텔") -> "예약 체크인·조식 확인·객실 추가 요청하기"
                topicTag.contains("마트") -> "상품 문의·카드/현금 결제·봉투 요청 완주하기"
                topicTag.contains("길찾기") -> "목적지 길 묻기·도보 시간 확인·감사 인사하기"
                topicTag.contains("소개") -> "자기소개·취미 나누기·상대에게 질문 되묻기"
                else -> "오늘 있었던 일과 관심사를 주제로 자유롭게 3턴 이어가기"
            }
        }
        fun defaultSuggestedRepliesForTopic(topicTag: String): List<SuggestedReplyOption> {
            return when {
                topicTag.contains("공항") -> listOf(
                    SuggestedReplyOption("I'm here for sightseeing with my family.", "가족과 관광하러 왔어요."),
                    SuggestedReplyOption("I'll be staying here for five days.", "여기서 5일 머물 예정이에요."),
                    SuggestedReplyOption("Could you show me the way to gate 12?", "12번 게이트 가는 길 알려주시겠어요?")
                )
                topicTag.contains("카페") -> listOf(
                    SuggestedReplyOption("Can I get an iced Americano, please?", "아이스 아메리카노 한 잔 주세요."),
                    SuggestedReplyOption("I'd like it for here, please.", "매장에서 마시고 갈게요."),
                    SuggestedReplyOption("Do you have anything less sweet?", "덜 단 메뉴 추천해 주실래요?")
                )
                topicTag.contains("마트") -> listOf(
                    SuggestedReplyOption("I'll pay by card, please.", "카드로 결제할게요."),
                    SuggestedReplyOption("Could I get a shopping bag?", "쇼핑백 하나 주실 수 있나요?"),
                    SuggestedReplyOption("Could you take this one off? I'll pay cash.", "이거 빼고 현금으로 계산할게요.")
                )
                topicTag.contains("소개") -> listOf(
                    SuggestedReplyOption("Hi! I love learning English and traveling.", "안녕! 영어 공부랑 여행을 좋아해."),
                    SuggestedReplyOption("I live in Korea and enjoy listening to music.", "한국에 살고 음악 듣는 걸 좋아해."),
                    SuggestedReplyOption("What do you usually do on weekends?", "너는 주말에 보통 뭐 해?")
                )
                topicTag.contains("식당") || topicTag.contains("레스토랑") -> listOf(
                    SuggestedReplyOption("A table for two near the window, please.", "창가 쪽 2인석 부탁해요."),
                    SuggestedReplyOption("What's your most popular dish here?", "여기서 제일 인기 있는 메뉴가 뭐예요?"),
                    SuggestedReplyOption("Could we get the check when you're ready?", "준비되시면 계산서 부탁드릴게요.")
                )
                topicTag.contains("호텔") -> listOf(
                    SuggestedReplyOption("Hi, I'd like to check in under my name.", "안녕하세요, 제 이름으로 체크인할게요."),
                    SuggestedReplyOption("Is breakfast included with the room?", "객실에 조식이 포함되어 있나요?"),
                    SuggestedReplyOption("Could I get an extra towel and late checkout?", "수건 추가랑 레이트 체크아웃 될까요?")
                )
                topicTag.contains("길찾기") -> listOf(
                    SuggestedReplyOption("Excuse me, how do I get to the subway station?", "실례지만 지하철역까지 어떻게 가나요?"),
                    SuggestedReplyOption("Is it within walking distance from here?", "여기서 걸어갈 수 있는 거리인가요?"),
                    SuggestedReplyOption("Should I turn left at the next traffic light?", "다음 신호등에서 좌회전하면 되나요?")
                )
                else -> listOf(
                    SuggestedReplyOption("My day is going great! How about you?", "오늘 아주 잘 보내고 있어! 너는?"),
                    SuggestedReplyOption("Times Square looks so bright and exciting!", "타임스스퀘어 정말 밝고 멋지다!"),
                    SuggestedReplyOption("Let's practice useful travel English today!", "오늘 실전 여행 영어 같이 연습하자!")
                )
            }
        }
    }
}
