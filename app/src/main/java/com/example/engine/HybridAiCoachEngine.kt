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

data class AiCoachReply(
    val replyEn: String,
    val translationKo: String,
    val coachingTipKo: String,
    val naturalRephraseEn: String,
    val engineBadge: String
)

class HybridAiCoachEngine(
    private val slotStore: LlmSlotStore
) {
    // Lazy initialization per firebase-ai-logic-android requirements
    private val geminiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
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
        activeTwist: TravelTwist? = null,
        onStreamChunk: ((String) -> Unit)? = null
    ): AiCoachReply {
        val prompt = buildTutorPrompt(tutor, userSpokenText, history, activeTwist)

        return when (mode) {
            AiEngineMode.OFFLINE_ONLY -> {
                buildOfflineCoachReply(tutor, userSpokenText, activeTwist)
            }

            AiEngineMode.EXTERNAL_10_SLOT_ROTATOR -> {
                val external = tryExternalSlotRotation(prompt, slots)
                if (external != null) {
                    external
                } else {
                    tryGeminiBuiltIn(prompt, onStreamChunk)
                        ?: buildOfflineCoachReply(tutor, userSpokenText, activeTwist)
                }
            }

            AiEngineMode.BUILT_IN_GEMINI -> {
                tryGeminiBuiltIn(prompt, onStreamChunk)
                    ?: tryExternalSlotRotation(prompt, slots)
                    ?: buildOfflineCoachReply(tutor, userSpokenText, activeTwist)
            }
        }
    }

    private fun buildTutorPrompt(
        tutor: AiHumanTutor,
        userSpokenText: String,
        history: List<TutorChatMessage>,
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
            ""
        }
        val koreanQuestionBlock = if (isKoreanExpressionQuestion(userSpokenText)) {
            """
            IMPORTANT: The learner just asked in Korean how to say something in English (like asking a live tutor on a video call, e.g., "마트에서 이거 빼고 현금으로 할게요 영어로 알려줘").
            - In REPLY_EN: Speak warmly like a real native video-call friend giving the exact natural English sentence right away and inviting them to repeat it! Example: "Oh, super easy! Just say: 'Could you take this one off? I'll pay in cash.' Try saying it with me!"
            - In REPHRASE_EN: Put ONLY the exact clean target English expression for them to shadow and practice (e.g., "Could you take this one off? I'll pay in cash.").
            - In TRANSLATION_KO: Give a friendly Korean subtitle explaining what you just said and the exact nuance.
            - In TIP_KO: Give 1 crisp pronunciation/stress coaching tip in Korean (what word to stress or how to link sounds).
            """.trimIndent()
        } else {
            """
            The learner spoke English during a live video call.
            - Reply naturally like a close native friend (2 short spoken sentences max) and keep the conversation flowing.
            - Immediately spot any awkward grammar or phrasing and provide a smoother native version in REPHRASE_EN, plus a friendly 1-sentence Korean correction & pronunciation coaching tip in TIP_KO.
            """.trimIndent()
        }

        return """
            ${tutor.friendPersonaPrompt}
            $twistBlock
            $koreanQuestionBlock

            Recent video-call transcript:
            $recentLines

            Learner just said: "$userSpokenText"

            Respond in EXACTLY 4 lines with these exact prefixes (no markdown, no extra text):
            REPLY_EN: <Your natural spoken English response on the live video call, 1-2 short sentences>
            TRANSLATION_KO: <Warm natural Korean live subtitle of REPLY_EN>
            REPHRASE_EN: <Exact clean target English sentence for the learner to practice/shadow>
            TIP_KO: <1 short actionable Korean speaking/pronunciation correction comment>
        """.trimIndent()
    }

    private suspend fun tryGeminiBuiltIn(
        prompt: String,
        onStreamChunk: ((String) -> Unit)?
    ): AiCoachReply? {
        return try {
            withTimeout(45_000L) {
                val sb = StringBuilder()
                geminiModel.generateContentStream(prompt).collect { chunk ->
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
                if (raw.isBlank()) null else parseStructuredReply(raw, "실제 Gemini 영상통화 AI")
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Gemini stream timed out", e)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini call failed, falling back gracefully: ${e.message}")
            null
        }
    }

    private suspend fun tryExternalSlotRotation(
        prompt: String,
        slots: List<LlmSlotConfig>
    ): AiCoachReply? = withContext(Dispatchers.IO) {
        val activeSlots = slots.filter { it.enabled && it.apiKey.isNotBlank() && it.baseUrl.isNotBlank() }
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
        if (slot.apiKey.isBlank() || slot.baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API 키와 Base URL을 먼저 입력해 주세요."))
        }
        val response = callOpenAiCompatibleSlot(
            slot = slot,
            prompt = "Reply in one cheerful short English sentence confirming the voice connection is ready."
        )
        if (response != null) {
            slotStore.incrementSlotRequestCount(slot.slotIndex)
            Result.success(response.trim())
        } else {
            Result.failure(IllegalStateException("응답 실패 (API 키·모델 ID·할당량을 확인해 주세요)"))
        }
    }

    private fun callOpenAiCompatibleSlot(slot: LlmSlotConfig, prompt: String): String? {
        return try {
            val cleanBase = slot.baseUrl.trim().trimEnd('/')
            val endpoint = if (cleanBase.endsWith("/chat/completions")) cleanBase else "$cleanBase/chat/completions"

            val messagesArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a warm, friendly native English video-call coach. Follow the requested 4-line format strictly.")
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
                put("max_tokens", 280)
            }

            val request = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer ${slot.apiKey.trim()}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
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
            raw.lines().firstOrNull()?.trim().orEmpty()
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
            ?: lines.firstOrNull { !it.startsWith("TRANSLATION") && !it.startsWith("REPHRASE") && !it.startsWith("TIP") }
            ?: "That sounds awesome! Tell me a little more about that!"
        val translationKo = grab("TRANSLATION_KO")
            ?: "정말 멋진 이야기야! 조금만 더 들려줄래?"
        val rephraseEn = grab("REPHRASE_EN") ?: replyEn
        val tipKo = grab("TIP_KO")
            ?: "문장 끝 억양을 자연스럽게 이어 말하면 원어민 느낌이 확 살아나요!"

        return AiCoachReply(
            replyEn = replyEn,
            translationKo = translationKo,
            coachingTipKo = tipKo,
            naturalRephraseEn = rephraseEn,
            engineBadge = badge
        )
    }

    private fun buildOfflineCoachReply(
        tutor: AiHumanTutor,
        userText: String,
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
                    engineBadge = "오프라인 초경량 코치"
                )
            } else {
                AiCoachReply(
                    replyEn = "I hear you! To solve this even faster, try saying: \"$suggested\"",
                    translationKo = "무슨 말씀인지 알겠어요! 훨씬 빠르게 해결하려면 이렇게 말해보세요!",
                    coachingTipKo = "팁: ${activeTwist.strategyKo}",
                    naturalRephraseEn = suggested,
                    engineBadge = "오프라인 초경량 코치"
                )
            }
        }

        // Smart offline Korean-to-English real-life situational phrasing!
        val offlinePhraseMatch: Triple<String, String, String>? = when {
            (lower.contains("마트") || lower.contains("빼고")) && lower.contains("현금") -> Triple(
                "Could you take this one off? I'll pay in cash.",
                "마트 계산대에서 딱 이렇게 말하면 완벽해! 'Could you take this one off? I'll pay in cash.' 한번 따라 해볼래?",
                "'take this one off'에서 take와 off에 강세를 주고, 'in cash'를 한 호흡으로 이어 말하세요!"
            )
            lower.contains("카드") || lower.contains("결제") -> Triple(
                "Can I pay by credit card, please?",
                "카드 결제할 땐 'Can I pay by credit card, please?'가 가장 자연스러워! 소리 내어 말해봐!",
                "'pay by card'에서 p 파열음과 b 진동을 또렷하게 구분해 보세요."
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
            lower.contains("할인") || lower.contains("깎아") -> Triple(
                "Is there any discount available on this item?",
                "할인을 물어볼 땐 'Is there any discount available on this item?'이 정중하고 세련돼!",
                "'discount'는 첫 음절 '디스'에 강세를 꼭 주세요."
            )
            else -> null
        }

        if (offlinePhraseMatch != null) {
            return AiCoachReply(
                replyEn = "Oh, great question! Just say: \"${offlinePhraseMatch.first}\" Try repeating it right now!",
                translationKo = offlinePhraseMatch.second,
                coachingTipKo = offlinePhraseMatch.third,
                naturalRephraseEn = offlinePhraseMatch.first,
                engineBadge = "오프라인 회화 코치"
            )
        }

        val cleaned = userText.trim().replaceFirstChar { it.uppercase() }
        return AiCoachReply(
            replyEn = "Haha, I love how you said that! Tell me more—what's your favorite part about it?",
            translationKo = "하하, 방금 표현 진짜 좋았어! 거기서 제일 좋아하는 부분이 뭐야?",
            coachingTipKo = "말하기 코칭: 문장 속 동사와 핵심 명사에 강세를 주면 ${tutor.accentLabel} 리듬이 살아납니다!",
            naturalRephraseEn = if (cleaned.any { it in 'a'..'z' || it in 'A'..'Z' }) cleaned else "Could you help me practice that sentence?",
            engineBadge = "오프라인 초경량 코치"
        )
    }
}
