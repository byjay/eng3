package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.NightBackground
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import org.json.JSONArray
import org.json.JSONObject

/**
 * 4 Emotion Categories grouping all 33 #humanRig CSS motion classes (g-01 .. g-33).
 */
enum class RigEmotionCategory(val labelKo: String, val aliases: List<String>) {
    JOY("즐거움", listOf("즐거움", "기쁨", "happy", "joy", "cheer", "laugh", "warm", "excited", "fun")),
    THINKING("생각함", listOf("생각함", "호기심", "차분함", "curious", "thinking", "calm", "neutral", "ponder", "wonder", "question")),
    SURPRISE("놀람", listOf("놀람", "surprised", "surprise", "wow", "amazed", "gasp", "shock", "eureka")),
    ENCOURAGE("응원", listOf("응원", "proud", "encourage", "bravo", "confident", "support", "comfort", "great"))
}

data class HumanRigGestureSpec(
    val id: String,               // "g-01" .. "g-33"
    val category: RigEmotionCategory,
    val labelKo: String,
    val emotionTags: List<String>,
    val durationMs: Int
)

data class QueuedGestureStep(
    val gestureId: String,
    val categoryKo: String,
    val emotionTagKo: String,
    val durationMs: Int
)

/**
 * Structured 33-motion CSS dataset (g-01 .. g-33) grouped by emotion category
 * + deterministic sentence-segment sequence queue planner for #humanRig.
 */
object HumanRigGestureCatalog {

    val gestures: List<HumanRigGestureSpec> = listOf(
        // === 즐거움 (Joy / 기쁨: g-01 .. g-09) ===
        HumanRigGestureSpec("g-01", RigEmotionCategory.JOY, "기쁨 바운스", listOf("기쁨", "즐거움", "happy", "joy"), 1450),
        HumanRigGestureSpec("g-02", RigEmotionCategory.JOY, "경쾌한 스웨이", listOf("기쁨", "즐거움", "happy", "cheer"), 1550),
        HumanRigGestureSpec("g-03", RigEmotionCategory.JOY, "미소 끄덕임", listOf("기쁨", "warm", "smile"), 1400),
        HumanRigGestureSpec("g-04", RigEmotionCategory.JOY, "환한 고개 기울임", listOf("즐거움", "playful", "fun"), 1500),
        HumanRigGestureSpec("g-05", RigEmotionCategory.JOY, "신나는 점프 리듬", listOf("기쁨", "excited", "yay"), 1350),
        HumanRigGestureSpec("g-06", RigEmotionCategory.JOY, "친근한 윙크 린", listOf("즐거움", "wink", "playful"), 1500),
        HumanRigGestureSpec("g-07", RigEmotionCategory.JOY, "반가운 손인사 틸트", listOf("기쁨", "greet", "welcome"), 1600),
        HumanRigGestureSpec("g-08", RigEmotionCategory.JOY, "햇살 글로우 끄덕", listOf("즐거움", "sunny", "happy"), 1500),
        HumanRigGestureSpec("g-09", RigEmotionCategory.JOY, "웃음 파동", listOf("기쁨", "laugh", "haha"), 1400),

        // === 생각함 (Thinking / 호기심 / 차분함: g-10 .. g-18) ===
        HumanRigGestureSpec("g-10", RigEmotionCategory.THINKING, "호기심 고개 갸웃", listOf("호기심", "curious", "question"), 1650),
        HumanRigGestureSpec("g-11", RigEmotionCategory.THINKING, "깊은 사색", listOf("생각함", "thinking", "ponder"), 1800),
        HumanRigGestureSpec("g-12", RigEmotionCategory.THINKING, "차분한 끄덕임", listOf("차분함", "calm", "neutral"), 1750),
        HumanRigGestureSpec("g-13", RigEmotionCategory.THINKING, "시선 상향 추론", listOf("생각함", "호기심", "wonder"), 1700),
        HumanRigGestureSpec("g-14", RigEmotionCategory.THINKING, "분석적 경청 린", listOf("호기심", "생각함", "listen"), 1600),
        HumanRigGestureSpec("g-15", RigEmotionCategory.THINKING, "차분한 호흡 스웨이", listOf("차분함", "calm", "gentle"), 1850),
        HumanRigGestureSpec("g-16", RigEmotionCategory.THINKING, "질문 유도 틸트", listOf("호기심", "question", "curious"), 1550),
        HumanRigGestureSpec("g-17", RigEmotionCategory.THINKING, "아이디어 부유", listOf("생각함", "idea", "muse"), 1700),
        HumanRigGestureSpec("g-18", RigEmotionCategory.THINKING, "안정적 균형 자세", listOf("차분함", "composed", "neutral"), 1800),

        // === 놀람 (Surprise / 놀람: g-19 .. g-25) ===
        HumanRigGestureSpec("g-19", RigEmotionCategory.SURPRISE, "깜짝 팝업", listOf("놀람", "surprised", "wow"), 1250),
        HumanRigGestureSpec("g-20", RigEmotionCategory.SURPRISE, "살짝 뒤로 젖힘", listOf("놀람", "shock", "recoil"), 1300),
        HumanRigGestureSpec("g-21", RigEmotionCategory.SURPRISE, "반짝 감탄 리프트", listOf("놀람", "amazed", "sparkle"), 1400),
        HumanRigGestureSpec("g-22", RigEmotionCategory.SURPRISE, "와우 탄성 바운스", listOf("놀람", "wow", "excited"), 1350),
        HumanRigGestureSpec("g-23", RigEmotionCategory.SURPRISE, "아하 유레카 점프", listOf("놀람", "eureka", "aha"), 1400),
        HumanRigGestureSpec("g-24", RigEmotionCategory.SURPRISE, "흥미진진 반동", listOf("놀람", "호기심", "intrigued"), 1350),
        HumanRigGestureSpec("g-25", RigEmotionCategory.SURPRISE, "기쁜 놀람 아크", listOf("놀람", "기쁨", "delighted"), 1450),

        // === 응원 (Encourage / 응원: g-26 .. g-33) ===
        HumanRigGestureSpec("g-26", RigEmotionCategory.ENCOURAGE, "자신감 더블 끄덕", listOf("응원", "confident", "yes"), 1450),
        HumanRigGestureSpec("g-27", RigEmotionCategory.ENCOURAGE, "박수 갈채 스웨이", listOf("응원", "bravo", "clap"), 1500),
        HumanRigGestureSpec("g-28", RigEmotionCategory.ENCOURAGE, "뿌듯한 리프트", listOf("응원", "proud", "great"), 1550),
        HumanRigGestureSpec("g-29", RigEmotionCategory.ENCOURAGE, "하이파이브 전진", listOf("응원", "highfive", "cheer"), 1450),
        HumanRigGestureSpec("g-30", RigEmotionCategory.ENCOURAGE, "따뜻한 격려 끄덕", listOf("응원", "차분함", "support"), 1650),
        HumanRigGestureSpec("g-31", RigEmotionCategory.ENCOURAGE, "최고 엄지 바운스", listOf("응원", "기쁨", "best"), 1400),
        HumanRigGestureSpec("g-32", RigEmotionCategory.ENCOURAGE, "승리 펄스", listOf("응원", "victory", "proud"), 1500),
        HumanRigGestureSpec("g-33", RigEmotionCategory.ENCOURAGE, "부드러운 위로 스웨이", listOf("응원", "차분함", "comfort"), 1750)
    )

    val emotionCategoryMap: Map<RigEmotionCategory, List<HumanRigGestureSpec>> =
        gestures.groupBy { it.category }

    fun categoryForEmotionTag(rawTag: String): RigEmotionCategory {
        val norm = rawTag.trim().lowercase()
        if (norm.isEmpty()) return RigEmotionCategory.JOY
        return RigEmotionCategory.entries.firstOrNull { cat ->
            cat.aliases.any { alias -> norm.contains(alias) }
        } ?: RigEmotionCategory.JOY
    }

    fun selectBestGestureForEmotion(rawEmotionTag: String, seed: Int = 0): HumanRigGestureSpec {
        val norm = rawEmotionTag.trim().lowercase()
        // 1. Direct exact emotion tag match (e.g., "기쁨" -> g-01, "호기심" -> g-10, "차분함" -> g-12, "놀람" -> g-22)
        val exact = gestures.filter { g -> g.emotionTags.any { it.equals(norm, ignoreCase = true) } }
        if (exact.isNotEmpty()) {
            return exact[Math.floorMod(seed, exact.size)]
        }
        // 2. Category group fallback
        val category = categoryForEmotionTag(norm)
        val bucket = emotionCategoryMap[category].orEmpty().ifEmpty { gestures }
        return bucket[Math.floorMod(seed, bucket.size)]
    }

    /**
     * Splits a multi-sentence utterance into sequential gesture queue items so shifting emotions
     * (e.g. surprise -> curiosity -> encouragement) animate smoothly on #humanRig!
     */
    fun buildGestureSequenceQueue(
        utterance: String,
        defaultEmotionTag: String,
        speechRate: Float = 1.0f
    ): List<QueuedGestureStep> {
        val safeRate = speechRate.coerceIn(0.6f, 1.5f)
        val cleaned = utterance.trim()
        if (cleaned.isEmpty()) {
            val g = selectBestGestureForEmotion(defaultEmotionTag, 0)
            return listOf(
                QueuedGestureStep(
                    gestureId = g.id,
                    categoryKo = g.category.labelKo,
                    emotionTagKo = emotionLabelKoFor(defaultEmotionTag, g.category),
                    durationMs = (g.durationMs / safeRate).toInt().coerceIn(900, 2600)
                )
            )
        }

        val segments = cleaned
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(5)
            .ifEmpty { listOf(cleaned) }

        return segments.mapIndexed { idx, seg ->
            val detectedTag = inferEmotionTagFromSegment(seg, defaultEmotionTag)
            val chosen = selectBestGestureForEmotion(detectedTag, seed = idx + seg.length)
            val estimatedMs = ((seg.length * 58L).coerceIn(1150L, 2800L) / safeRate).toInt()
            QueuedGestureStep(
                gestureId = chosen.id,
                categoryKo = chosen.category.labelKo,
                emotionTagKo = emotionLabelKoFor(detectedTag, chosen.category),
                durationMs = estimatedMs
            )
        }
    }

    private fun inferEmotionTagFromSegment(segment: String, fallbackTag: String): String {
        val lower = segment.lowercase()
        return when {
            lower.contains("wow") || lower.contains("amazing") || lower.contains("really!") ||
                lower.contains("unbelievable") || lower.contains("놀라") || lower.contains("와우") -> "놀람"
            segment.endsWith("?") || lower.startsWith("what") || lower.startsWith("how") ||
                lower.startsWith("why") || lower.contains("wonder") || lower.contains("curious") -> "호기심"
            lower.contains("great job") || lower.contains("proud") || lower.contains("you can") ||
                lower.contains("bravo") || lower.contains("well done") || lower.contains("keep going") -> "응원"
            lower.contains("calm") || lower.contains("slowly") || lower.contains("breathe") ||
                lower.contains("step by step") || lower.contains("okay") || lower.contains("no worries") -> "차분함"
            segment.endsWith("!") || lower.contains("awesome") || lower.contains("love") ||
                lower.contains("happy") || lower.contains("welcome") -> "기쁨"
            else -> fallbackTag.ifBlank { "차분함" }
        }
    }

    private fun emotionLabelKoFor(rawTag: String, category: RigEmotionCategory): String {
        val norm = rawTag.trim().lowercase()
        return when {
            norm in setOf("기쁨", "happy", "joy", "excited") -> "기쁨"
            norm in setOf("호기심", "curious", "question") -> "호기심"
            norm in setOf("차분함", "calm", "neutral", "gentle") -> "차분함"
            norm in setOf("놀람", "wow", "surprised", "amazed") -> "놀람"
            norm in setOf("응원", "proud", "bravo", "encourage") -> "응원"
            else -> category.labelKo
        }
    }
}

/**
 * Upgraded SVG + CSS Character Rig (#humanRig):
 * - 33 CSS motion classes (g-01..g-33) grouped by emotion category (즐거움 · 생각함 · 놀람 · 응원)
 * - Array-based Gesture Sequence Queue playing sequential segment motions during speech
 * - Smooth transition back to `idleBreath` when utterance ends
 * - Pauses `idleBreath` on voice input (`isListening`) and overlays live CSS lip-sync
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SvgCssLipSyncRigView(
    tutorId: String,
    tutorName: String,
    learnerName: String,
    activeTopicTag: String,
    activeGesture: MotionPhotoGesture,
    aiMood: String,
    liveThoughtKo: String,
    isSpeaking: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
    speechRate: Float,
    speechBeatIndex: Int,
    userRmsDb: Float,
    spokenSentence: String = "",
    transparentBackdrop: Boolean = false,
    compactOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }

    val rigStateName = when {
        isListening -> "listening"
        isSpeaking -> "speaking"
        isThinking -> "thinking"
        else -> "idle"
    }

    val normalizedVoiceLevel = remember(isListening, isSpeaking, userRmsDb, speechBeatIndex) {
        when {
            isListening -> ((userRmsDb + 2f) / 11f).coerceIn(0.18f, 1.0f)
            isSpeaking -> {
                val pattern = floatArrayOf(0.82f, 0.45f, 0.96f, 0.60f, 0.88f, 0.35f, 0.92f)
                pattern[Math.floorMod(speechBeatIndex, pattern.size)]
            }
            else -> 0f
        }
    }

    val resolvedEmotionTag = remember(aiMood, activeGesture, isThinking) {
        when {
            isThinking || activeGesture == MotionPhotoGesture.THOUGHTFUL_MUSE -> "생각함"
            aiMood.isNotBlank() -> aiMood.lowercase()
            activeGesture == MotionPhotoGesture.CURIOUS_TILT -> "호기심"
            activeGesture == MotionPhotoGesture.SURPRISED_WOW || activeGesture == MotionPhotoGesture.AMAZED_SPARKLE -> "놀람"
            activeGesture == MotionPhotoGesture.CLAPPING_BRAVO ||
                activeGesture == MotionPhotoGesture.PROUD_CHEER ||
                activeGesture == MotionPhotoGesture.THUMBS_UP_COOL -> "응원"
            activeGesture == MotionPhotoGesture.GENTLE_COMFORT || activeGesture == MotionPhotoGesture.COFFEE_CHILL -> "차분함"
            else -> "기쁨"
        }
    }

    val gestureSequence = remember(spokenSentence, resolvedEmotionTag, speechRate) {
        HumanRigGestureCatalog.buildGestureSequenceQueue(
            utterance = spokenSentence,
            defaultEmotionTag = resolvedEmotionTag,
            speechRate = speechRate
        )
    }

    val primaryGestureStep = gestureSequence.firstOrNull()
    val htmlPayload = remember(tutorId, transparentBackdrop, compactOverlay) {
        buildSvgCssRigHtml(tutorId, transparentBackdrop, compactOverlay)
    }

    // Push real-time Rig state, voice level, lip-sync cadence, and gesture sequence queue into #humanRig
    LaunchedEffect(pageReady, rigStateName, normalizedVoiceLevel, speechRate, resolvedEmotionTag, speechBeatIndex, gestureSequence) {
        if (!pageReady) return@LaunchedEffect
        val queueJson = JSONArray().apply {
            gestureSequence.forEach { step ->
                put(
                    JSONObject().apply {
                        put("gestureId", step.gestureId)
                        put("categoryKo", step.categoryKo)
                        put("emotionTagKo", step.emotionTagKo)
                        put("durationMs", step.durationMs)
                    }
                )
            }
        }
        val js = buildString {
            append("window.updateSvgRig && window.updateSvgRig(")
            append(JSONObject.quote(rigStateName)).append(", ")
            append(String.format(java.util.Locale.US, "%.3f", normalizedVoiceLevel)).append(", ")
            append(String.format(java.util.Locale.US, "%.2f", speechRate.coerceIn(0.6f, 1.5f))).append(", ")
            append(JSONObject.quote(resolvedEmotionTag)).append(", ")
            append(speechBeatIndex).append(", ")
            append(queueJson.toString())
            append(");")
        }
        webViewRef?.evaluateJavascript(js, null)
    }

    val reloadKey = "$tutorId|$transparentBackdrop|$compactOverlay"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (transparentBackdrop) Color.Transparent else NightBackground)
            .testTag("svg_css_lipsync_rig_stage")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageReady = true
                        }
                    }
                    webViewRef = this
                    tag = reloadKey
                    loadDataWithBaseURL("https://svgrig.local/", htmlPayload, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webViewRef = webView
                if (webView.tag != reloadKey) {
                    pageReady = false
                    webView.tag = reloadKey
                    webView.loadDataWithBaseURL("https://svgrig.local/", htmlPayload, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Compact Rig State & Active Gesture Queue Floating Capsule
        AnimatedVisibility(
            visible = !compactOverlay && (isListening || isSpeaking || isThinking || liveThoughtKo.isNotBlank()),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 152.dp, start = 20.dp, end = 20.dp)
        ) {
            val pillAccent = when {
                isListening -> StudioCoral
                isSpeaking -> StudioCobalt
                isThinking -> StudioIris
                else -> GlacierCyan
            }
            val pillText = when {
                isListening -> "${learnerName}님 음성 감지 · idleBreath 일시정지 & 립싱크"
                isThinking -> "$tutorName 능동 사고 중 · ${activeTopicTag} 제스처 구상"
                isSpeaking -> {
                    val queueTag = if (gestureSequence.size > 1) {
                        gestureSequence.joinToString("→") { it.gestureId }
                    } else {
                        primaryGestureStep?.gestureId ?: "g-01"
                    }
                    val catTag = primaryGestureStep?.emotionTagKo ?: "기쁨"
                    "$tutorName 발화 중 · $catTag ($queueTag)"
                }
                liveThoughtKo.isNotBlank() -> liveThoughtKo
                else -> ""
            }
            if (pillText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xCC0D0F14),
                    border = BorderStroke(1.dp, pillAccent.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("svg_rig_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(pillAccent)
                        )
                        Icon(
                            imageVector = if (isThinking || (!isListening && !isSpeaking)) {
                                Icons.Outlined.Psychology
                            } else {
                                Icons.Outlined.GraphicEq
                            },
                            contentDescription = null,
                            tint = pillAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = pillText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class SvgTutorPalette(
    val hairMain: String,
    val hairShadow: String,
    val skinLight: String,
    val skinShade: String,
    val outfitMain: String,
    val outfitAccent: String,
    val irisColor: String,
    val lipColor: String,
    val glowPrimary: String,
    val glowSecondary: String,
    val isShortHair: Boolean
)

private fun paletteForTutor(tutorId: String): SvgTutorPalette {
    return when {
        tutorId.contains("liam", ignoreCase = true) || tutorId.contains("noah", ignoreCase = true) -> SvgTutorPalette(
            hairMain = "#262B38",
            hairShadow = "#151924",
            skinLight = "#F6D3BC",
            skinShade = "#E3B295",
            outfitMain = "#1E2A4A",
            outfitAccent = "#335CFF",
            irisColor = "#3B5998",
            lipColor = "#C9756B",
            glowPrimary = "#335CFF",
            glowSecondary = "#00A8E8",
            isShortHair = true
        )
        tutorId.contains("sophia", ignoreCase = true) || tutorId.contains("chloe", ignoreCase = true) -> SvgTutorPalette(
            hairMain = "#5B3A29",
            hairShadow = "#3B2216",
            skinLight = "#F9DAC4",
            skinShade = "#E7B79B",
            outfitMain = "#2B2242",
            outfitAccent = "#6B5BFF",
            irisColor = "#5165A8",
            lipColor = "#D96A73",
            glowPrimary = "#6B5BFF",
            glowSecondary = "#FF6242",
            isShortHair = false
        )
        else -> SvgTutorPalette(
            hairMain = "#3A2622",
            hairShadow = "#211412",
            skinLight = "#FBDDC8",
            skinShade = "#E8B99C",
            outfitMain = "#1B2438",
            outfitAccent = "#335CFF",
            irisColor = "#3F5AA6",
            lipColor = "#E06D75",
            glowPrimary = "#335CFF",
            glowSecondary = "#FF6242",
            isShortHair = false
        )
    }
}

private fun build33MotionCssRules(): String = buildString {
    // 33 GPU-composited motion classes (g-01 .. g-33) applied to #humanRig
    for (i in 1..33) {
        val id = "g-%02d".format(i)
        val (dur, kf) = when (i) {
            // 즐거움 (g-01 .. g-09)
            1 -> "1.35s" to "0%,100%{transform:translateY(0) scale(1)} 35%{transform:translateY(-8px) rotate(1.6deg) scale(1.02)} 70%{transform:translateY(-3px) rotate(-1.2deg)}"
            2 -> "1.50s" to "0%,100%{transform:rotate(0deg)} 30%{transform:rotate(2.8deg) translateX(3px)} 70%{transform:rotate(-2.6deg) translateX(-3px)}"
            3 -> "1.30s" to "0%,100%{transform:translateY(0)} 25%{transform:translateY(5px) scaleY(0.99)} 60%{transform:translateY(-4px)}"
            4 -> "1.45s" to "0%,100%{transform:rotate(0deg)} 45%{transform:rotate(-3.2deg) translateY(-3px)}"
            5 -> "1.20s" to "0%,100%{transform:translateY(0)} 28%{transform:translateY(-11px) scale(1.03)} 55%{transform:translateY(1px)} 78%{transform:translateY(-6px)}"
            6 -> "1.45s" to "0%,100%{transform:rotate(0deg) scale(1)} 40%{transform:rotate(2.6deg) translateY(-2px) scale(1.025)}"
            7 -> "1.55s" to "0%,100%{transform:rotate(0deg)} 25%{transform:rotate(-3.0deg) translateX(-4px)} 65%{transform:rotate(2.2deg) translateX(3px)}"
            8 -> "1.40s" to "0%,100%{transform:translateY(0) scale(1)} 50%{transform:translateY(-5px) scale(1.025)}"
            9 -> "1.15s" to "0%,100%{transform:translateY(0)} 20%{transform:translateY(-5px)} 40%{transform:translateY(1px)} 60%{transform:translateY(-6px)} 80%{transform:translateY(0)}"
            // 생각함 / 호기심 / 차분함 (g-10 .. g-18)
            10 -> "1.65s" to "0%,100%{transform:rotate(0deg)} 45%{transform:rotate(4.2deg) translateX(3px) translateY(-2px)}"
            11 -> "1.80s" to "0%,100%{transform:rotate(0deg)} 50%{transform:rotate(-2.4deg) translateY(3px) scale(0.992)}"
            12 -> "1.75s" to "0%,100%{transform:translateY(0)} 45%{transform:translateY(3.5px) scaleY(0.994)}"
            13 -> "1.70s" to "0%,100%{transform:translateY(0) rotate(0deg)} 50%{transform:translateY(-4px) rotate(-2.8deg)}"
            14 -> "1.60s" to "0%,100%{transform:scale(1)} 45%{transform:scale(1.03) translateY(2px) rotate(1.4deg)}"
            15 -> "1.90s" to "0%,100%{transform:translateY(0) rotate(0deg)} 50%{transform:translateY(-3px) rotate(1.1deg)}"
            16 -> "1.55s" to "0%,100%{transform:rotate(0deg)} 40%{transform:rotate(-4.0deg) translateX(-3px)}"
            17 -> "1.75s" to "0%,100%{transform:translate(0,0)} 50%{transform:translate(2.5px,-4px) rotate(1.6deg)}"
            18 -> "1.85s" to "0%,100%{transform:translateY(0) scale(1)} 50%{transform:translateY(-2px) scale(1.008)}"
            // 놀람 (g-19 .. g-25)
            19 -> "1.15s" to "0%,100%{transform:scale(1)} 25%{transform:translateY(-9px) scale(1.055)} 60%{transform:translateY(-3px) scale(1.02)}"
            20 -> "1.25s" to "0%,100%{transform:translateY(0) scale(1)} 30%{transform:translateY(-6px) scale(0.97) rotate(-2.2deg)}"
            21 -> "1.35s" to "0%,100%{transform:translateY(0) scale(1)} 40%{transform:translateY(-10px) scale(1.045)}"
            22 -> "1.25s" to "0%,100%{transform:translateY(0) scale(1)} 22%{transform:translateY(-12px) scale(1.06)} 55%{transform:translateY(-2px) scale(1.015)}"
            23 -> "1.30s" to "0%,100%{transform:translateY(0)} 30%{transform:translateY(-11px) rotate(2.4deg) scale(1.04)}"
            24 -> "1.30s" to "0%,100%{transform:rotate(0deg) scale(1)} 35%{transform:rotate(3.5deg) translateY(-7px) scale(1.035)}"
            25 -> "1.40s" to "0%,100%{transform:translateY(0)} 35%{transform:translateY(-8px) rotate(-2.5deg) scale(1.035)} 70%{transform:translateY(-3px) rotate(1.8deg)}"
            // 응원 (g-26 .. g-33)
            26 -> "1.35s" to "0%,100%{transform:translateY(0)} 22%{transform:translateY(6px)} 45%{transform:translateY(-3px)} 68%{transform:translateY(5px)}"
            27 -> "1.40s" to "0%,100%{transform:rotate(0deg)} 25%{transform:rotate(2.4deg) translateY(-5px)} 55%{transform:rotate(-2.4deg) translateY(-5px)}"
            28 -> "1.50s" to "0%,100%{transform:translateY(0) scale(1)} 45%{transform:translateY(-7px) scale(1.035)}"
            29 -> "1.40s" to "0%,100%{transform:scale(1)} 40%{transform:scale(1.05) translateY(-4px) rotate(-1.5deg)}"
            30 -> "1.60s" to "0%,100%{transform:translateY(0)} 40%{transform:translateY(4.5px) rotate(1.2deg)}"
            31 -> "1.35s" to "0%,100%{transform:translateY(0) scale(1)} 35%{transform:translateY(-8px) scale(1.03) rotate(2deg)}"
            32 -> "1.40s" to "0%,100%{transform:scale(1)} 30%{transform:scale(1.045) translateY(-6px)} 65%{transform:scale(1.02) translateY(-2px)}"
            else -> "1.70s" to "0%,100%{transform:rotate(0deg)} 35%{transform:rotate(1.8deg) translateY(2px)} 70%{transform:rotate(-1.8deg) translateY(1px)}"
        }
        append("#humanRig.$id { animation: kf-$id $dur cubic-bezier(0.22, 1, 0.36, 1) infinite; }\n")
        append("@keyframes kf-$id { $kf }\n")
    }
}

private fun buildSvgCssRigHtml(
    tutorId: String,
    transparentBackdrop: Boolean = false,
    compactOverlay: Boolean = false
): String {
    val p = paletteForTutor(tutorId)
    val bodyBgCss = if (transparentBackdrop) {
        "transparent"
    } else {
        "radial-gradient(circle at 50% 34%, #1A2238 0%, #111520 52%, #0D0F14 100%)"
    }
    val stageSizeCss = if (compactOverlay) {
        "width: 96%; height: 96%; margin-bottom: 0;"
    } else {
        "width: min(92vw, 390px); height: min(92vw, 390px); margin-bottom: 138px;"
    }
    val backHairSvg = if (p.isShortHair) {
        """<path d="M 118 170 C 115 100, 285 100, 282 170 L 274 218 C 250 198, 150 198, 126 218 Z" fill="${p.hairShadow}" />"""
    } else {
        """<path d="M 102 180 C 96 86, 304 86, 298 180 C 314 275, 312 360, 286 410 C 245 426, 155 426, 114 410 C 88 360, 86 275, 102 180 Z" fill="${p.hairShadow}" />"""
    }
    val bangsSvg = if (p.isShortHair) {
        """
        <path d="M 122 175 C 124 106, 276 106, 278 175 C 258 148, 224 140, 198 148 C 174 138, 144 148, 122 175 Z" fill="${p.hairMain}" />
        <path d="M 134 150 C 160 120, 222 114, 260 144" stroke="rgba(255,255,255,0.14)" stroke-width="4" stroke-linecap="round" fill="none" />
        """.trimIndent()
    } else {
        """
        <path d="M 116 184 C 116 100, 284 100, 284 184 C 266 154, 240 138, 210 146 C 186 156, 150 142, 116 184 Z" fill="${p.hairMain}" />
        <path d="M 112 182 C 102 240, 108 302, 128 345 C 122 292, 126 238, 134 198 Z" fill="${p.hairMain}" />
        <path d="M 288 182 C 298 240, 292 302, 272 345 C 278 292, 274 238, 266 198 Z" fill="${p.hairMain}" />
        <path d="M 142 142 C 178 112, 232 114, 264 146" stroke="rgba(255,255,255,0.16)" stroke-width="5" stroke-linecap="round" fill="none" />
        """.trimIndent()
    }

    val gesturesJson = JSONArray().apply {
        HumanRigGestureCatalog.gestures.forEach { g ->
            put(
                JSONObject().apply {
                    put("id", g.id)
                    put("category", g.category.labelKo)
                    put("labelKo", g.labelKo)
                    put("emotionTags", JSONArray(g.emotionTags))
                    put("durationMs", g.durationMs)
                }
            )
        }
    }.toString()

    val motionCssRules = build33MotionCssRules()

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            :root {
              --voice-level: 0;
              --lip-speed: 0.34s;
              --glow-primary: ${p.glowPrimary};
              --glow-secondary: ${p.glowSecondary};
            }

            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              overflow: hidden;
              background: $bodyBgCss;
              display: flex;
              align-items: center;
              justify-content: center;
            }

            .rig-stage {
              position: relative;
              $stageSizeCss
              display: flex;
              align-items: center;
              justify-content: center;
            }

            .ambient-ring {
              position: absolute;
              width: 84%;
              height: 84%;
              border-radius: 50%;
              background: radial-gradient(circle, rgba(51,92,255,0.22) 0%, rgba(0,168,232,0.06) 58%, transparent 74%);
              transition: transform 0.22s ease, opacity 0.25s ease, box-shadow 0.25s ease;
              pointer-events: none;
            }

            .voice-ripple {
              position: absolute;
              width: 78%;
              height: 78%;
              border-radius: 50%;
              border: 2px solid rgba(255, 98, 66, 0.0);
              transform: scale(0.96);
              opacity: 0;
              pointer-events: none;
              transition: opacity 0.18s ease;
            }

            svg#tutor-rig {
              width: 100%;
              height: 100%;
              overflow: visible;
              filter: drop-shadow(0 18px 34px rgba(0, 0, 0, 0.42));
            }

            /* === #humanRig BASE & SMOOTH RETURN TO idleBreath === */
            #humanRig {
              transform-origin: 200px 330px;
              transition: transform 0.52s cubic-bezier(0.22, 1, 0.36, 1);
              will-change: transform;
            }

            #humanRig.idleBreath {
              animation: idleBreath 4.4s ease-in-out infinite;
            }

            #humanRig.returning-idle {
              animation: none !important;
              transform: translate(0px, 0px) rotate(0deg) scale(1) !important;
            }

            @keyframes idleBreath {
              0%, 100% { transform: translateY(0px) scaleY(1); }
              50% { transform: translateY(-4.5px) scaleY(1.013); }
            }

            /* === 33 EMOTION-MAPPED MOTION CLASSES (g-01 .. g-33) === */
            $motionCssRules

            #rig-head-group {
              transform-origin: 200px 260px;
              animation: rig-idle-sway 6.2s ease-in-out infinite;
              transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
            }

            #rig-hair-back {
              transform-origin: 200px 180px;
              animation: rig-hair-drift 5.6s ease-in-out infinite;
            }

            #rig-eyelids {
              transform-origin: 200px 206px;
              animation: rig-eye-blink 4.8s ease-in-out infinite;
            }

            @keyframes rig-idle-sway {
              0%, 100% { transform: rotate(0deg) translateX(0px); }
              30% { transform: rotate(1.4deg) translateX(1.5px); }
              70% { transform: rotate(-1.3deg) translateX(-1.5px); }
            }

            @keyframes rig-hair-drift {
              0%, 100% { transform: rotate(0deg); }
              50% { transform: rotate(1.1deg); }
            }

            @keyframes rig-eye-blink {
              0%, 44%, 48%, 94%, 98%, 100% { transform: scaleY(0.02); opacity: 0; }
              46%, 96% { transform: scaleY(1); opacity: 1; }
            }

            /* === PAUSE idleBreath ON VOICE INPUT (LISTENING) === */
            .rig-stage.state-listening #humanRig,
            .rig-stage.state-listening #rig-head-group,
            .rig-stage.state-listening #rig-hair-back,
            .rig-stage.state-speaking #rig-head-group {
              animation-play-state: paused !important;
            }

            .rig-stage.state-listening #rig-head-active-offset {
              transform: translateY(3px) scale(1.03);
            }

            .rig-stage.state-listening .voice-ripple {
              opacity: 1;
              border-color: rgba(255, 98, 66, 0.58);
              animation: voice-pulse-ring 1.15s ease-out infinite;
            }

            .rig-stage.state-speaking .ambient-ring {
              box-shadow: 0 0 42px rgba(51, 92, 255, 0.38);
              transform: scale(calc(1.0 + var(--voice-level) * 0.08));
            }

            @keyframes voice-pulse-ring {
              0% { transform: scale(0.94); opacity: 0.72; }
              100% { transform: scale(1.14); opacity: 0; }
            }

            /* === THINKING STATE === */
            .rig-stage.state-thinking #rig-pupils { transform: translate(5px, -4px); }
            .rig-stage.state-thinking #rig-brows { transform: translateY(-3px) rotate(-2deg); }
            .rig-stage.state-thinking .ambient-ring {
              background: radial-gradient(circle, rgba(107,91,255,0.34) 0%, rgba(51,92,255,0.10) 58%, transparent 74%);
              animation: think-glow 1.3s ease-in-out infinite alternate;
            }
            @keyframes think-glow {
              from { transform: scale(0.97); opacity: 0.75; }
              to { transform: scale(1.06); opacity: 1; }
            }

            /* === NATURAL CSS LIP-SYNC OVERLAY === */
            #mouth-idle-rest {
              transition: opacity 0.14s ease, transform 0.14s ease;
              transform-origin: 200px 264px;
            }
            #mouth-lipsync-overlay {
              opacity: 0;
              transform-origin: 200px 264px;
              transition: opacity 0.12s ease;
              pointer-events: none;
            }
            .rig-stage.state-listening #mouth-idle-rest,
            .rig-stage.state-speaking #mouth-idle-rest { opacity: 0; }
            .rig-stage.state-listening #mouth-lipsync-overlay,
            .rig-stage.state-speaking #mouth-lipsync-overlay { opacity: 1; }

            #lipsync-cavity { transform-origin: 200px 262px; }
            #lipsync-upper-teeth { transform-origin: 200px 258px; }
            #lipsync-tongue { transform-origin: 200px 270px; }
            #lipsync-outer-lip { transform-origin: 200px 263px; }

            .rig-stage.state-listening #lipsync-cavity,
            .rig-stage.state-speaking #lipsync-cavity {
              animation: css-lipsync-cavity var(--lip-speed) cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
            }
            .rig-stage.state-listening #lipsync-outer-lip,
            .rig-stage.state-speaking #lipsync-outer-lip {
              animation: css-lipsync-outer var(--lip-speed) cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
            }
            .rig-stage.state-listening #lipsync-tongue,
            .rig-stage.state-speaking #lipsync-tongue {
              animation: css-lipsync-tongue var(--lip-speed) ease-in-out infinite;
            }
            .rig-stage.state-listening #lipsync-upper-teeth,
            .rig-stage.state-speaking #lipsync-upper-teeth {
              animation: css-lipsync-teeth var(--lip-speed) ease-in-out infinite;
            }

            #mouth-amplitude-wrap {
              transform-origin: 200px 263px;
              transition: transform 0.07s linear;
            }

            @keyframes css-lipsync-cavity {
              0%   { transform: scaleX(0.74) scaleY(0.24); }
              18%  { transform: scaleX(1.08) scaleY(0.95); }
              36%  { transform: scaleX(0.68) scaleY(0.76); }
              54%  { transform: scaleX(1.16) scaleY(0.54); }
              72%  { transform: scaleX(0.84) scaleY(1.06); }
              88%  { transform: scaleX(0.95) scaleY(0.36); }
              100% { transform: scaleX(0.74) scaleY(0.24); }
            }
            @keyframes css-lipsync-outer {
              0%   { transform: scaleX(0.88) scaleY(0.55); }
              18%  { transform: scaleX(1.06) scaleY(1.02); }
              36%  { transform: scaleX(0.78) scaleY(0.92); }
              54%  { transform: scaleX(1.14) scaleY(0.72); }
              72%  { transform: scaleX(0.90) scaleY(1.08); }
              88%  { transform: scaleX(0.96) scaleY(0.62); }
              100% { transform: scaleX(0.88) scaleY(0.55); }
            }
            @keyframes css-lipsync-tongue {
              0%, 100% { transform: translateY(2px) scaleY(0.55); opacity: 0.45; }
              35%      { transform: translateY(-1.5px) scaleY(1.05); opacity: 0.95; }
              72%      { transform: translateY(0px) scaleY(0.9); opacity: 0.85; }
            }
            @keyframes css-lipsync-teeth {
              0%, 100% { opacity: 0.35; transform: translateY(0px); }
              20%, 60% { opacity: 0.98; transform: translateY(-0.5px); }
            }

            /* === FACIAL EMOTION MODIFIERS === */
            #rig-head-active-offset, #rig-pupils, #rig-brows, #rig-blush {
              transition: transform 0.25s ease, opacity 0.25s ease;
              transform-origin: 200px 210px;
            }
            .rig-stage.mood-joy #rig-brows { transform: translateY(-3px); }
            .rig-stage.mood-joy #rig-blush { opacity: 0.82; }
            .rig-stage.mood-surprise #rig-brows { transform: translateY(-6px) scaleY(1.1); }
            .rig-stage.mood-thinking #rig-head-active-offset { transform: rotate(3.5deg); }
            .rig-stage.mood-thinking #rig-brows { transform: translateY(-2px) rotate(2.5deg); }
            .rig-stage.mood-encourage #rig-blush { opacity: 0.78; }

            #thinking-orbit { opacity: 0; transition: opacity 0.22s ease; }
            .rig-stage.state-thinking #thinking-orbit { opacity: 1; }
            .think-dot { animation: think-dot-bounce 0.95s ease-in-out infinite; }
            .think-dot.d2 { animation-delay: 0.18s; }
            .think-dot.d3 { animation-delay: 0.36s; }
            @keyframes think-dot-bounce {
              0%, 100% { transform: translateY(0px); opacity: 0.45; }
              50% { transform: translateY(-6px); opacity: 1; }
            }
          </style>
        </head>
        <body>
          <div id="rig-stage" class="rig-stage state-idle mood-joy">
            <div class="ambient-ring"></div>
            <div class="voice-ripple"></div>

            <svg id="tutor-rig" viewBox="0 0 400 440" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="skinGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="${p.skinLight}" />
                  <stop offset="100%" stop-color="${p.skinShade}" />
                </linearGradient>
                <linearGradient id="outfitGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="${p.outfitMain}" />
                  <stop offset="100%" stop-color="#0F1422" />
                </linearGradient>
                <radialGradient id="blushGrad" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#FF7A7A" stop-opacity="0.48" />
                  <stop offset="100%" stop-color="#FF7A7A" stop-opacity="0" />
                </radialGradient>
                <clipPath id="mouthClip">
                  <ellipse cx="200" cy="264" rx="22" ry="15" />
                </clipPath>
              </defs>

              <!-- Canonical #humanRig root controlled by 33-motion gesture queue & idleBreath -->
              <g id="humanRig" class="idleBreath">
                <g id="rig-hair-back">$backHairSvg</g>

                <g id="rig-torso">
                  <path d="M 88 430 C 94 352, 138 326, 200 326 C 262 326, 306 352, 312 430 Z" fill="url(#outfitGrad)" />
                  <path d="M 164 328 L 200 374 L 236 328" fill="none" stroke="${p.outfitAccent}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M 182 328 L 200 356 L 218 328" fill="rgba(255,255,255,0.10)" />
                  <rect x="178" y="284" width="44" height="52" rx="20" fill="${p.skinShade}" />
                  <path d="M 178 296 Q 200 314 222 296 L 222 284 L 178 284 Z" fill="rgba(120,62,48,0.16)" />
                </g>

                <g id="rig-head-group">
                  <g id="rig-head-active-offset">
                    <ellipse cx="122" cy="216" rx="11" ry="18" fill="${p.skinShade}" />
                    <ellipse cx="278" cy="216" rx="11" ry="18" fill="${p.skinShade}" />
                    <path d="M 126 196 C 126 132, 274 132, 274 196 C 274 260, 244 302, 200 304 C 156 302, 126 260, 126 196 Z" fill="url(#skinGrad)" />

                    <g id="rig-blush" opacity="0.52">
                      <ellipse cx="156" cy="238" rx="20" ry="11" fill="url(#blushGrad)" />
                      <ellipse cx="244" cy="238" rx="20" ry="11" fill="url(#blushGrad)" />
                    </g>

                    <g id="rig-brows">
                      <path d="M 148 184 Q 165 175 182 182" stroke="${p.hairMain}" stroke-width="4.2" stroke-linecap="round" fill="none" />
                      <path d="M 218 182 Q 235 175 252 184" stroke="${p.hairMain}" stroke-width="4.2" stroke-linecap="round" fill="none" />
                    </g>

                    <g id="rig-eyes">
                      <ellipse cx="166" cy="208" rx="15" ry="12" fill="#FFFFFF" />
                      <ellipse cx="234" cy="208" rx="15" ry="12" fill="#FFFFFF" />
                      <g id="rig-pupils">
                        <circle cx="167" cy="208" r="8.2" fill="${p.irisColor}" />
                        <circle cx="167" cy="208" r="4.2" fill="#111622" />
                        <circle cx="170" cy="204.5" r="2.3" fill="#FFFFFF" />
                        <circle cx="164" cy="211" r="1.1" fill="rgba(255,255,255,0.65)" />
                        <circle cx="233" cy="208" r="8.2" fill="${p.irisColor}" />
                        <circle cx="233" cy="208" r="4.2" fill="#111622" />
                        <circle cx="236" cy="204.5" r="2.3" fill="#FFFFFF" />
                        <circle cx="230" cy="211" r="1.1" fill="rgba(255,255,255,0.65)" />
                      </g>
                      <path d="M 150 205 Q 166 193 182 205" stroke="#1E1A22" stroke-width="3.2" stroke-linecap="round" fill="none" />
                      <path d="M 218 205 Q 234 193 250 205" stroke="#1E1A22" stroke-width="3.2" stroke-linecap="round" fill="none" />
                    </g>

                    <g id="rig-eyelids">
                      <ellipse cx="166" cy="208" rx="16" ry="13" fill="${p.skinShade}" />
                      <ellipse cx="234" cy="208" rx="16" ry="13" fill="${p.skinShade}" />
                      <path d="M 151 209 Q 166 216 181 209" stroke="#1E1A22" stroke-width="2.6" stroke-linecap="round" fill="none" />
                      <path d="M 219 209 Q 234 216 249 209" stroke="#1E1A22" stroke-width="2.6" stroke-linecap="round" fill="none" />
                    </g>

                    <path d="M 198 220 Q 194 233 200 236 Q 206 236 205 231" stroke="rgba(145,86,68,0.45)" stroke-width="2.5" stroke-linecap="round" fill="none" />

                    <g id="mouth-idle-rest">
                      <path d="M 183 262 Q 200 273 217 262" stroke="${p.lipColor}" stroke-width="3.8" stroke-linecap="round" fill="none" />
                      <path d="M 189 266 Q 200 271 211 266" stroke="rgba(255,255,255,0.35)" stroke-width="1.4" stroke-linecap="round" fill="none" />
                    </g>

                    <g id="mouth-lipsync-overlay">
                      <g id="mouth-amplitude-wrap">
                        <ellipse id="lipsync-outer-lip" cx="200" cy="264" rx="20" ry="12.5" fill="${p.lipColor}" />
                        <ellipse id="lipsync-cavity" cx="200" cy="264" rx="16" ry="10.2" fill="#521C25" />
                        <rect id="lipsync-upper-teeth" x="188" y="255.5" width="24" height="4.8" rx="2.4" fill="#FFFDF9" clip-path="url(#mouthClip)" />
                        <ellipse id="lipsync-tongue" cx="200" cy="271.5" rx="10.5" ry="5.5" fill="#E36E7B" clip-path="url(#mouthClip)" />
                      </g>
                    </g>

                    <g id="rig-hair-front">$bangsSvg</g>
                  </g>
                </g>

                <g id="thinking-orbit">
                  <rect x="272" y="102" width="66" height="30" rx="15" fill="rgba(13,15,20,0.82)" stroke="#6B5BFF" stroke-width="1.6" />
                  <circle class="think-dot d1" cx="290" cy="117" r="4" fill="#6B5BFF" />
                  <circle class="think-dot d2" cx="305" cy="117" r="4" fill="#335CFF" />
                  <circle class="think-dot d3" cx="320" cy="117" r="4" fill="#00A8E8" />
                </g>
              </g>
            </svg>
          </div>

          <script>
            const GESTURES = $gesturesJson;
            const EMOTION_GESTURE_MAP = {
              "즐거움": GESTURES.filter(g => g.category === "즐거움").map(g => g.id),
              "생각함": GESTURES.filter(g => g.category === "생각함").map(g => g.id),
              "놀람":   GESTURES.filter(g => g.category === "놀람").map(g => g.id),
              "응원":   GESTURES.filter(g => g.category === "응원").map(g => g.id)
            };

            const stageEl = document.getElementById("rig-stage");
            const humanRigEl = document.getElementById("humanRig");
            const ampWrap = document.getElementById("mouth-amplitude-wrap");

            let gestureQueue = [];
            let queueTimerId = null;
            let returnIdleTimerId = null;
            let lastRigState = "idle";
            let lastQueueSignature = "";

            function clearMotionTimers() {
              if (queueTimerId) { clearTimeout(queueTimerId); queueTimerId = null; }
              if (returnIdleTimerId) { clearTimeout(returnIdleTimerId); returnIdleTimerId = null; }
            }

            function moodClassForCategory(catKo) {
              if (catKo === "놀람") return "mood-surprise";
              if (catKo === "생각함" || catKo === "호기심" || catKo === "차분함") return "mood-thinking";
              if (catKo === "응원") return "mood-encourage";
              return "mood-joy";
            }

            function pickBestGestureId(emotionTag) {
              const norm = String(emotionTag || "").toLowerCase();
              const exact = GESTURES.find(g => g.emotionTags.some(t => t.toLowerCase() === norm));
              if (exact) return exact.id;
              if (norm.includes("놀") || norm.includes("wow") || norm.includes("surpris")) return EMOTION_GESTURE_MAP["놀람"][0] || "g-22";
              if (norm.includes("호기") || norm.includes("생각") || norm.includes("차분") || norm.includes("curio") || norm.includes("calm") || norm.includes("think")) return EMOTION_GESTURE_MAP["생각함"][0] || "g-10";
              if (norm.includes("응원") || norm.includes("proud") || norm.includes("bravo")) return EMOTION_GESTURE_MAP["응원"][0] || "g-26";
              return EMOTION_GESTURE_MAP["즐거움"][0] || "g-01";
            }

            function applyRigMotionClass(gestureId, categoryKo) {
              if (!humanRigEl) return;
              humanRigEl.classList.remove("idleBreath", "returning-idle");
              for (let i = 1; i <= 33; i++) {
                humanRigEl.classList.remove("g-" + String(i).padStart(2, "0"));
              }
              humanRigEl.classList.add(gestureId);
              if (stageEl) {
                stageEl.classList.remove("mood-joy", "mood-thinking", "mood-surprise", "mood-encourage");
                stageEl.classList.add(moodClassForCategory(categoryKo));
              }
            }

            function playNextQueuedGesture() {
              if (!gestureQueue.length || lastRigState !== "speaking") return;
              const current = gestureQueue.shift();
              const gid = current.gestureId || pickBestGestureId(current.emotionTagKo);
              applyRigMotionClass(gid, current.categoryKo || current.emotionTagKo);
              if (gestureQueue.length > 0) {
                const dur = Math.max(850, Math.min(3000, Number(current.durationMs) || 1500));
                queueTimerId = setTimeout(playNextQueuedGesture, dur);
              }
            }

            function smoothReturnToIdleBreath() {
              clearMotionTimers();
              gestureQueue = [];
              if (!humanRigEl) return;
              for (let i = 1; i <= 33; i++) {
                humanRigEl.classList.remove("g-" + String(i).padStart(2, "0"));
              }
              humanRigEl.classList.remove("idleBreath");
              humanRigEl.classList.add("returning-idle");
              returnIdleTimerId = setTimeout(() => {
                humanRigEl.classList.remove("returning-idle");
                humanRigEl.classList.add("idleBreath");
              }, 380);
            }

            window.updateSvgRig = function(stateName, voiceLevel, speechRate, emotionTag, beatIdx, queueArray) {
              if (!stageEl || !humanRigEl) return;
              const safeState = ["idle", "listening", "speaking", "thinking"].includes(stateName) ? stateName : "idle";
              const prevState = lastRigState;
              lastRigState = safeState;

              stageEl.classList.remove("state-idle", "state-listening", "state-speaking", "state-thinking");
              stageEl.classList.add("state-" + safeState);

              const clampedLevel = Math.max(0, Math.min(1, Number(voiceLevel) || 0));
              const safeRate = Math.max(0.6, Math.min(1.5, Number(speechRate) || 1.0));
              const lipPeriodSec = (0.38 / safeRate).toFixed(2) + "s";

              document.documentElement.style.setProperty("--voice-level", clampedLevel.toFixed(3));
              document.documentElement.style.setProperty("--lip-speed", lipPeriodSec);

              if (safeState === "speaking") {
                const incoming = Array.isArray(queueArray) && queueArray.length > 0
                  ? queueArray
                  : [{ gestureId: pickBestGestureId(emotionTag), categoryKo: emotionTag, emotionTagKo: emotionTag, durationMs: 1500 }];
                const sig = incoming.map(x => x.gestureId).join(",");
                if (prevState !== "speaking" || sig !== lastQueueSignature) {
                  lastQueueSignature = sig;
                  clearMotionTimers();
                  gestureQueue = incoming.slice();
                  playNextQueuedGesture();
                }
              } else if (safeState === "thinking") {
                if (prevState !== "thinking") {
                  clearMotionTimers();
                  gestureQueue = [];
                  applyRigMotionClass("g-11", "생각함");
                }
              } else if (safeState === "listening") {
                clearMotionTimers();
                gestureQueue = [];
                if (!humanRigEl.classList.contains("idleBreath")) {
                  humanRigEl.classList.add("idleBreath");
                }
              } else if (safeState === "idle" && prevState !== "idle") {
                lastQueueSignature = "";
                smoothReturnToIdleBreath();
              }

              if (ampWrap) {
                if (safeState === "listening" || safeState === "speaking") {
                  const scaleY = 0.62 + (clampedLevel * 0.64);
                  const scaleX = 0.90 + (clampedLevel * 0.22);
                  ampWrap.style.transform = "scale(" + scaleX.toFixed(3) + ", " + scaleY.toFixed(3) + ")";
                } else {
                  ampWrap.style.transform = "scale(1, 1)";
                }
              }
            };
          </script>
        </body>
        </html>
    """.trimIndent()
}
