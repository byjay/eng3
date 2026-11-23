package com.example.engine

import android.util.Log
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "HybridAiCoachEngine"

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
    val engineBadge: String
)

class HybridAiCoachEngine(
    private val slotStore: LlmSlotStore
) {
    // Lazy initialization per firebase-ai-logic-android requirements (dual-model resilience!)
    private val primaryGeminiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }
    private val fastFallbackGeminiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.5-flash-lite")
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val roundRobinCursor = AtomicInteger(0)
    private val slotCooldownUntil = ConcurrentHashMap<Int, Long>()

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
        onStreamChunk: ((String) -> Unit)? = null
    ): AiCoachReply {
        val prompt = buildTutorPrompt(tutor, userSpokenText, history, activeTopicTag, activeTwist)

        return when (mode) {
            AiEngineMode.OFFLINE_ONLY -> {
                LocalConversationSkillEngine.generateReply(tutor, userSpokenText, history, activeTopicTag, activeTwist)
            }

            AiEngineMode.FREE_OPEN_SOURCE_AUTO -> {
                // 1. User-configured free slots (Groq/OpenRouter/Cerebras/HuggingFace/Ollama) ->
                // 2. Built-in Gemini dual failover ->
                // 3. Keyless Pollinations Free Open-Source LLM ->
                // 4. 0ms Zero-LLM LocalConversationSkillEngine (never fails!)
                val keyedExternal = tryExternalSlotRotation(prompt, slots, keyedOnly = true)
                keyedExternal
                    ?: tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: LocalConversationSkillEngine.generateReply(tutor, userSpokenText, history, activeTopicTag, activeTwist)
            }

            AiEngineMode.EXTERNAL_10_SLOT_ROTATOR -> {
                val external = tryExternalSlotRotation(prompt, slots, keyedOnly = false)
                external
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: LocalConversationSkillEngine.generateReply(tutor, userSpokenText, history, activeTopicTag, activeTwist)
            }

            AiEngineMode.BUILT_IN_GEMINI -> {
                tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: tryExternalSlotRotation(prompt, slots, keyedOnly = false)
                    ?: tryFreeKeylessOpenSourceLlm(prompt)
                    ?: LocalConversationSkillEngine.generateReply(tutor, userSpokenText, history, activeTopicTag, activeTwist)
            }
        }
    }

    private fun buildTutorPrompt(
        tutor: AiHumanTutor,
        userSpokenText: String,
        history: List<TutorChatMessage>,
        activeTopicTag: String,
        activeTwist: TravelTwist?
    ): String {
        val recentLines = history.takeLast(6).joinToString("\n") { msg ->
            val speaker = if (msg.fromTutor) tutor.name else "Learner"
            "$speaker: ${msg.textEn}"
        }
        val twistBlock = if (activeTwist != null) {
            """
            ACTIVE TRAVEL TWIST ROLEPLAY:
            Situation: ${activeTwist.titleKo}
            Staff just said: "${activeTwist.staffPromptEn}"
            Stay in character as a friendly real person responding smoothly to the traveler.
            """.trimIndent()
        } else {
            "ACTIVE CONVERSATION SCENE / TOPIC: $activeTopicTag"
        }
        val koreanQuestionBlock = if (isKoreanExpressionQuestion(userSpokenText)) {
            """
            IMPORTANT: The learner asked in Korean how to say something in English during a live video call.
            - In REPLY_EN: Speak warmly like a real native video-call friend giving the exact natural English sentence right away and inviting them to repeat it!
            - In REPHRASE_EN: Put ONLY the exact clean target English expression for them to shadow and practice.
            - In GRAMMAR_KO: Explain in 1 crisp Korean sentence why natives prefer this phrasing.
            - In TRANSLATION_KO: Friendly Korean subtitle of REPLY_EN.
            - In TIP_KO: 1 crisp pronunciation/stress/linking coaching tip in Korean.
            """.trimIndent()
        } else {
            """
            The learner spoke English during a live video call.
            - In REPLY_EN: Reply naturally like a real native friend (1-2 short energetic spoken sentences max, ending with an engaging follow-up question so conversation never stalls).
            - In REPHRASE_EN: Upgrade the learner's sentence into smoother, more authentic native English (even if grammatically okay, offer a C1 natural phrasing).
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

            Learner just said: "$userSpokenText"

            Respond in EXACTLY 8 lines with these exact prefixes (plain text only, no markdown, no bullets):
            REPLY_EN: <Your natural spoken English response on the live video call, 1-2 short sentences>
            TRANSLATION_KO: <Warm natural Korean live subtitle of REPLY_EN>
            REPHRASE_EN: <Upgraded native English sentence for the learner to practice/shadow>
            GRAMMAR_KO: <1 short Korean explanation of the grammar/expression upgrade>
            TIP_KO: <1 short actionable Korean pronunciation/intonation coaching tip>
            HINT1: <Natural short English reply option 1 to your REPLY_EN> | <Korean meaning>
            HINT2: <Natural short English reply option 2 to your REPLY_EN> | <Korean meaning>
            HINT3: <Natural short English reply option 3 to your REPLY_EN> | <Korean meaning>
        """.trimIndent()
    }

    private suspend fun tryGeminiBuiltIn(
        prompt: String,
        onStreamChunk: ((String) -> Unit)?
    ): AiCoachReply? {
        val primary = streamFromModel(primaryGeminiModel, prompt, "Gemini 3.8 Pro 코치", onStreamChunk)
        if (primary != null) return primary
        Log.w(TAG, "Primary Gemini model unavailable, switching seamlessly to gemini-3.5-flash-lite")
        return streamFromModel(fastFallbackGeminiModel, prompt, "Gemini Flash Lite 코치", onStreamChunk)
    }

    private suspend fun streamFromModel(
        model: com.google.firebase.ai.GenerativeModel,
        prompt: String,
        badge: String,
        onStreamChunk: ((String) -> Unit)?
    ): AiCoachReply? {
        return try {
            withTimeout(35_000L) {
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
            Log.w(TAG, "$badge stream timed out", e)
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
        val freeSlot = LlmSlotConfig(
            slotIndex = 0,
            label = "Pollinations Free",
            providerName = "무료 오픈소스 LLM",
            baseUrl = "https://text.pollinations.ai/openai",
            modelId = "openai",
            apiKey = "",
            enabled = true
        )
        val raw = callOpenAiCompatibleSlot(freeSlot, prompt)
        raw?.let { parseStructuredReply(it, "무료 오픈소스 LLM (키 불필요)") }
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
        val startIdx = roundRobinCursor.getAndIncrement() % activeSlots.size

        for (offset in activeSlots.indices) {
            val slot = activeSlots[(startIdx + offset) % activeSlots.size]
            val cooldown = slotCooldownUntil[slot.slotIndex] ?: 0L
            if (cooldown > now) continue

            val result = callOpenAiCompatibleSlot(slot, prompt)
            if (result != null) {
                slotStore.incrementSlotRequestCount(slot.slotIndex)
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
            return@withContext Result.failure(IllegalArgumentException("API 키와 Base URL을 먼저 입력해 주세요. (Pollinations·Ollama는 키 불필요!)"))
        }
        val response = callOpenAiCompatibleSlot(
            slot = slot,
            prompt = "Reply in one cheerful short English sentence confirming the voice connection is ready."
        )
        if (response != null) {
            slotStore.incrementSlotRequestCount(slot.slotIndex)
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
            val payload = JSONObject().apply {
                put("model", slot.modelId.trim().ifBlank { "llama-3.3-70b-versatile" })
                put("messages", messagesArr)
                put("temperature", 0.7)
                put("max_tokens", 360)
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
            if (first.startsWith("TRANSLATION") || first.startsWith("REPHRASE") || first.startsWith("HINT")) "" else first
        }
    }

    private fun parseStructuredReply(raw: String, badge: String): AiCoachReply {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        fun grab(prefix: String): String? =
            lines.firstOrNull { it.startsWith(prefix, ignoreCase = true) }
                ?.substringAfter(":")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val replyEn = grab("REPLY_EN")
            ?: lines.firstOrNull {
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
            ?: "핵심 동사와 명사에 강세를 실어 한 호흡으로 이어 말하면 원어민 리듬이 살아나요!"

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
            engineBadge = badge
        )
    }

    private fun buildOfflineCoachReply(
        tutor: AiHumanTutor,
        userText: String,
        activeTopicTag: String,
        activeTwist: TravelTwist?
    ): AiCoachReply {
        val lower = userText.lowercase()
        if (activeTwist != null) {
            val matchedKeyword = activeTwist.expectedKeywords.any { lower.contains(it.lowercase()) }
            val suggested = activeTwist.keyPhrases.firstOrNull()
                ?: "Could you please help me resolve this issue right away?"
            return if (matchedKeyword) {
                AiCoachReply(
                    replyEn = "Got it! Thank you for explaining that so clearly. I'll take care of it for you right away!",
                    translationKo = "알겠습니다! 아주 명확하게 설명해 주셔서 감사해요. 제가 지금 바로 처리해 드릴게요!",
                    coachingTipKo = "돌발 변수 핵심 키워드를 정확히 사용했어요! 다음엔 한 번 더 또렷하게 강세를 줘보세요.",
                    naturalRephraseEn = suggested,
                    grammarFixKo = "상황에 꼭 맞는 핵심 키워드와 정중한 요청 표현이 잘 어우러졌어요.",
                    suggestedReplies = listOf(
                        SuggestedReplyOption("Thank you so much for your help!", "도와주셔서 정말 감사합니다!"),
                        SuggestedReplyOption("How long will it take?", "얼마나 걸릴까요?"),
                        SuggestedReplyOption("I really appreciate your quick response.", "빠르게 대응해 주셔서 감사해요.")
                    ),
                    engineBadge = "스피킹 프로 코치"
                )
            } else {
                AiCoachReply(
                    replyEn = "I hear you! To solve this even faster, try saying: \"$suggested\"",
                    translationKo = "무슨 말씀인지 알겠어요! 훨씬 빠르게 해결하려면 이렇게 말해보세요!",
                    coachingTipKo = "팁: ${activeTwist.strategyKo}",
                    naturalRephraseEn = suggested,
                    grammarFixKo = "Could you please ~ 패턴을 쓰면 현지 직원이 훨씬 친절하게 도와줘요.",
                    suggestedReplies = listOf(
                        SuggestedReplyOption(suggested, "핵심 문장 그대로 따라 말하기"),
                        SuggestedReplyOption("Could you check that for me again?", "다시 한 번 확인해 주시겠어요?")
                    ),
                    engineBadge = "스피킹 프로 코치"
                )
            }
        }

        // Rich multi-situation Korean & English conversational coach
        val offlinePhraseMatch: Triple<String, String, String>? = when {
            (lower.contains("마트") || lower.contains("빼고")) && lower.contains("현금") -> Triple(
                "Could you take this one off? I'll pay in cash.",
                "마트 계산대에서 딱 이렇게 말하면 완벽해! 'Could you take this one off? I'll pay in cash.' 한번 따라 해볼래?",
                "'take this one off'에서 take와 off에 강세를 주고, 'in cash'를 한 호흡으로 이어 말하세요!"
            )
            lower.contains("카드") || lower.contains("결제") || lower.contains("card") -> Triple(
                "Can I pay by credit card, please?",
                "카드 결제할 땐 'Can I pay by credit card, please?'가 가장 자연스러워! 소리 내어 말해봐!",
                "'pay by card'에서 p 파열음과 b 진동을 또렷하게 구분해 보세요."
            )
            lower.contains("sightseeing") || lower.contains("vacation") || lower.contains("trip") || lower.contains("travel") -> Triple(
                "I'm here for sightseeing and I'm staying for five days.",
                "여행 목적 말하기 완벽해! 체류 기간까지 붙여서 말하면 입국심사가 한 번에 통과돼!",
                "'sightseeing'은 첫 음절 '사이트'에 강세를 실어주세요."
            )
            lower.contains("americano") || lower.contains("coffee") || lower.contains("latte") || lower.contains("iced") -> Triple(
                "Can I get a tall iced Americano for here, please?",
                "카페 주문 너무 자연스러워! 사이즈와 매장/포장(for here / to go)을 붙이면 원어민 100%!",
                "'Can I get a'를 '캐나이 게러'처럼 부드럽게 연음해 보세요."
            )
            lower.contains("화장실") -> Triple(
                "Excuse me, where is the restroom?",
                "화장실 물어볼 땐 'Excuse me, where is the restroom?'이라고 하면 아주 정중해!",
                "미국에선 restroom, 영국에선 toilet이나 loo도 많이 써요!"
            )
            lower.contains("얼마") || lower.contains("가격") -> Triple(
                "How much is this one altogether?",
                "가격을 물을 땐 'How much is this one altogether?'라고 해봐!",
                "'How much'를 붙여 '하우머치'처럼 리듬감 있게 연결하세요."
            )
            lower.contains("포장") || lower.contains("가져갈") -> Triple(
                "Could I get this to go, please?",
                "포장 요청할 땐 'Could I get this to go, please?'가 현지 찐 표현이야!",
                "'to go'를 짧고 경쾌하게 올려 발음해 보세요."
            )
            lower.contains("자기소개") || lower.contains("안녕") || lower.contains("이름") -> Triple(
                "Hi! I'm so happy to meet you today. I love learning English!",
                "자연스러운 첫인사는 'Hi! I'm so happy to meet you today.'로 웃으며 시작해 봐!",
                "happy와 meet에 밝은 억양을 실어주면 원어민 친구처럼 들려요."
            )
            else -> null
        }

        val defaultHints = defaultSuggestedRepliesForTopic(activeTopicTag)
        if (offlinePhraseMatch != null) {
            return AiCoachReply(
                replyEn = "Awesome! Try saying it smoothly like a native: \"${offlinePhraseMatch.first}\" Ready to give it another try?",
                translationKo = offlinePhraseMatch.second,
                coachingTipKo = offlinePhraseMatch.third,
                naturalRephraseEn = offlinePhraseMatch.first,
                grammarFixKo = "원어민이 실생활에서 가장 많이 쓰는 자연스러운 구어체 표현으로 업그레이드했어요.",
                suggestedReplies = listOf(
                    SuggestedReplyOption(offlinePhraseMatch.first, "추천 원어민 표현 바로 따라 말하기")
                ) + defaultHints.take(2),
                engineBadge = "스피킹 프로 코치"
            )
        }

        val cleaned = userText.trim().replaceFirstChar { it.uppercase() }
        return AiCoachReply(
            replyEn = "Nice! I love how clearly you expressed that. What would you like to say next in this situation?",
            translationKo = "좋아! 방금 의도가 아주 잘 전달됐어. 이 상황에서 다음엔 뭐라고 말해보고 싶어?",
            coachingTipKo = "말하기 코칭: 동사와 핵심 명사에 강세를 주면 ${tutor.accentLabel} 억양이 확 살아나요!",
            naturalRephraseEn = if (cleaned.any { it in 'a'..'z' || it in 'A'..'Z' }) cleaned else "Could you help me practice that sentence?",
            grammarFixKo = "문장 끝을 자연스럽게 내리며 여유 있는 속도로 말하면 더 유창하게 들려요.",
            suggestedReplies = defaultHints,
            engineBadge = "스피킹 프로 코치"
        )
    }

    companion object {
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
                else -> listOf(
                    SuggestedReplyOption("My day is going great! How about you?", "오늘 아주 잘 보내고 있어! 너는?"),
                    SuggestedReplyOption("Times Square looks so bright and exciting!", "타임스스퀘어 정말 밝고 멋지다!"),
                    SuggestedReplyOption("Let's practice useful travel English today!", "오늘 실전 여행 영어 같이 연습하자!")
                )
            }
        }
    }
}
