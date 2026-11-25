package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import kotlin.math.PI
import kotlin.math.sin

/**
 * 4 Emotion Categories grouping all 33 #humanRig motion classes (g-01 .. g-33).
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
 * Structured 33-motion dataset (g-01 .. g-33) grouped by emotion category
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
        val exact = gestures.filter { g -> g.emotionTags.any { it.equals(norm, ignoreCase = true) } }
        if (exact.isNotEmpty()) {
            return exact[Math.floorMod(seed, exact.size)]
        }
        val category = categoryForEmotionTag(norm)
        val bucket = emotionCategoryMap[category].orEmpty().ifEmpty { gestures }
        return bucket[Math.floorMod(seed, bucket.size)]
    }

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

@DrawableRes
private fun illustratedCharacterPortraitRes(tutorId: String, compactOverlay: Boolean): Int {
    return when {
        tutorId.contains("liam", ignoreCase = true) ||
            tutorId.contains("lucas", ignoreCase = true) ||
            tutorId.contains("noah", ignoreCase = true) -> R.drawable.img_tutor_liam_1795412460241
        tutorId.contains("sophia", ignoreCase = true) ||
            tutorId.contains("chloe", ignoreCase = true) -> R.drawable.img_tutor_sophia_1795412462611
        tutorId.contains("maya", ignoreCase = true) || compactOverlay -> R.drawable.img_tutor_emma_1795412460310
        else -> R.drawable.img_friend_nyc_no_mic_1795422201093
    }
}

/**
 * High-Resolution 3D Illustrated Character Stage (#humanRig):
 * - Uses the original bundled Pixar-grade 3D tutor portraits (Emma/Liam/Sophia) with silky 60fps GPU
 *   breathing, handheld camera drift, speaking cadence nod, attentive listening lean-in, and 33 gesture kinetics.
 * - Supports both full-bleed Conversation Stage (`compactOverlay = false`) and transparent Street View PiP card
 *   (`compactOverlay = true`, `transparentBackdrop = true`).
 */
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
    val portraitRes = remember(tutorId, compactOverlay) {
        illustratedCharacterPortraitRes(tutorId, compactOverlay)
    }

    val normalizedVoiceLevel = remember(isListening, isSpeaking, userRmsDb, speechBeatIndex) {
        when {
            isListening -> ((userRmsDb + 2f) / 11f).coerceIn(0.22f, 1.0f)
            isSpeaking -> {
                val pattern = floatArrayOf(0.84f, 0.44f, 0.96f, 0.58f, 0.88f, 0.36f, 0.92f)
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

    val emotionCategory = remember(resolvedEmotionTag) {
        HumanRigGestureCatalog.categoryForEmotionTag(resolvedEmotionTag)
    }

    val gestureSequence = remember(spokenSentence, resolvedEmotionTag, speechRate) {
        HumanRigGestureCatalog.buildGestureSequenceQueue(
            utterance = spokenSentence,
            defaultEmotionTag = resolvedEmotionTag,
            speechRate = speechRate
        )
    }
    val primaryGestureStep = gestureSequence.firstOrNull()

    val infinite = rememberInfiniteTransition(label = "character_portrait_motion")
    val breathPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath_phase"
    )
    val driftPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_phase"
    )
    val ripplePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )

    val smoothVoiceLevel by animateFloatAsState(
        targetValue = normalizedVoiceLevel,
        animationSpec = tween(durationMillis = 100),
        label = "smooth_voice_level"
    )
    val speakNodTarget = if (isSpeaking) {
        if (speechBeatIndex % 2 == 0) 1f else -0.65f
    } else {
        0f
    }
    val smoothSpeakNod by animateFloatAsState(
        targetValue = speakNodTarget,
        animationSpec = tween(
            durationMillis = (220f / speechRate.coerceIn(0.7f, 1.4f)).toInt().coerceIn(130, 320),
            easing = FastOutSlowInEasing
        ),
        label = "speak_nod"
    )
    val listenZoom by animateFloatAsState(
        targetValue = if (isListening) 0.045f + smoothVoiceLevel * 0.025f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "listen_zoom"
    )

    val breathSin = if (isListening) 0f else sin(breathPhase)
    val swaySin = sin(driftPhase)
    val gestureTiltDeg = when {
        isListening -> 1.4f
        isThinking -> -2.2f
        emotionCategory == RigEmotionCategory.SURPRISE -> -1.3f
        emotionCategory == RigEmotionCategory.ENCOURAGE -> swaySin * 1.9f
        else -> swaySin * 1.15f
    }
    val gestureLiftPx = when {
        isSpeaking -> smoothSpeakNod * 5.5f
        emotionCategory == RigEmotionCategory.SURPRISE -> -7f
        else -> -breathSin * 4.5f
    }
    val portraitScale = (if (compactOverlay) 1.07f else 1.12f) +
        breathSin * 0.012f +
        listenZoom +
        (if (isSpeaking) smoothVoiceLevel * 0.014f else 0f)

    val accentColor = when {
        isListening -> StudioCoral
        isSpeaking -> GlacierCyan
        isThinking -> StudioIris
        else -> StudioCobalt
    }

    val topicOverlayTint = remember(activeTopicTag) {
        when {
            activeTopicTag.contains("카페") -> Color(0x22FFB86C)
            activeTopicTag.contains("공항") -> Color(0x2038BDF8)
            activeTopicTag.contains("식당") -> Color(0x22FB7185)
            activeTopicTag.contains("호텔") -> Color(0x20A78BFA)
            activeTopicTag.contains("마트") -> Color(0x1E34D399)
            else -> Color(0x1A60A5FA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (transparentBackdrop) Color.Transparent else Color(0xFF070C16))
            .testTag("svg_css_lipsync_rig_stage")
    ) {
        if (compactOverlay) {
            // Compact floating glass portrait card for Street View travel overlay
            val cardShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .fillMaxSize()
            ) {
                Surface(
                    shape = cardShape,
                    color = Color(0xFF0E1626),
                    border = BorderStroke(
                        width = if (isListening || isSpeaking) 2.2.dp else 1.4.dp,
                        color = accentColor.copy(alpha = if (isListening || isSpeaking) 0.92f else 0.55f)
                    ),
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = portraitRes),
                            contentDescription = "$tutorName 3D 캐릭터 튜터",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = portraitScale
                                    scaleY = portraitScale
                                    translationX = swaySin * 3.5f
                                    translationY = gestureLiftPx
                                    rotationZ = gestureTiltDeg
                                }
                        )
                        // Subtle bottom gradient for clean framing
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.68f to Color.Transparent,
                                        1.0f to Color.Black.copy(alpha = 0.52f)
                                    )
                                )
                        )
                        // Subtle live voice ring pulse when active
                        if (isListening || isSpeaking) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val alpha = (1f - ripplePhase) * 0.45f
                                drawRoundRect(
                                    color = accentColor.copy(alpha = alpha),
                                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                                    style = Stroke(width = (2f + ripplePhase * 4f).dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Full-bleed Pixar 3D character live video call stage
            Image(
                painter = painterResource(id = portraitRes),
                contentDescription = "$tutorName 3D 캐릭터 화상 회화",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = portraitScale
                        scaleY = portraitScale
                        translationX = swaySin * 7f
                        translationY = gestureLiftPx
                        rotationZ = gestureTiltDeg
                    }
            )

            // Topic ambient lighting wash + subtle live audio halo
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to topicOverlayTint,
                            0.45f to Color.Transparent,
                            1.0f to Color(0x33070B14)
                        )
                    )
                )
                if (isListening || isSpeaking) {
                    val glowRadius = w * (0.42f + smoothVoiceLevel * 0.14f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.18f * (1f - ripplePhase * 0.45f)),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.42f),
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = Offset(w * 0.5f, h * 0.42f)
                    )
                }
            }

            // Compact Live Emotion / Status Capsule (non-intrusive, positioned cleanly below center)
            AnimatedVisibility(
                visible = isListening || isSpeaking || isThinking || liveThoughtKo.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 86.dp, start = 20.dp, end = 20.dp)
            ) {
                val pillAccent = when {
                    isListening -> StudioCoral
                    isSpeaking -> StudioCobalt
                    isThinking -> StudioIris
                    else -> GlacierCyan
                }
                val pillText = when {
                    isListening -> "${learnerName}님 음성 감지 · 실시간 경청 중"
                    isThinking -> "$tutorName 생각 중 · ${activeTopicTag} 답변 구상"
                    isSpeaking -> {
                        val catTag = primaryGestureStep?.emotionTagKo ?: "기쁨"
                        "$tutorName 말하는 중 · $catTag"
                    }
                    liveThoughtKo.isNotBlank() -> liveThoughtKo
                    else -> ""
                }
                if (pillText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xBB0D0F14),
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
                                color = Color.White.copy(alpha = 0.94f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

