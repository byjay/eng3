package com.example.ui.components

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.NanoBananaMotionEngine
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 33 Motion-Photo Expressions & Gestures for the English Video-Call Friend Character.
 * Each gesture includes both smooth GPU kinetic parameters and an English Nano Banana 2.1
 * photorealistic image-editing prompt.
 */
enum class MotionPhotoGesture(
    val labelKo: String,
    val badgeIcon: String,
    val nanoBananaPromptEn: String,
    val tiltZ: Float = 0f,
    val rotY: Float = 0f,
    val rotX: Float = 0f,
    val scaleBoost: Float = 1f,
    val bounceY: Float = 0f,
    val winkLeft: Boolean = false
) {
    WARM_SMILE("미소", "SMILE", "warm genuine friendly smile looking directly at the camera", tiltZ = 1.0f, rotY = 1.5f, scaleBoost = 1.015f),
    FRIENDLY_WAVE("손인사", "WAVE", "cheerfully waving one hand hello toward the camera with a bright smile", tiltZ = -3.5f, rotY = -4f, bounceY = -6f),
    ENTHUSIASTIC_NOD("끄덕임", "NOD", "warmly nodding in enthusiastic agreement with sparkling eyes", rotX = 4.5f, bounceY = 7f, scaleBoost = 1.02f),
    DOUBLE_NOD("열정 끄덕", "YES!", "excitedly agreeing with an upbeat encouraging grin", rotX = 6f, bounceY = 9f),
    CURIOUS_TILT("고개 갸웃", "HUH?", "curiously tilting head slightly with raised friendly eyebrows", tiltZ = 5.5f, rotY = 4f, scaleBoost = 1.01f),
    EXCITED_BOUNCE("신남 점프", "YAY!", "super excited joyful celebration smile with bright eyes", bounceY = -12f, scaleBoost = 1.045f),
    PLAYFUL_WINK("윙크", "WINK", "playful charming wink with one eye closed and a cute smile", tiltZ = -3f, rotY = -3.5f, winkLeft = true),
    LAUGHING_JOY("하하 웃음", "HAHA", "laughing joyfully with natural crinkled happy eyes and open smile", tiltZ = 2.5f, rotX = -3f, bounceY = -8f),
    LISTEN_LEAN_IN("경청 집중", "LISTEN", "leaning slightly closer listening attentively with warm focused eyes", rotX = 3f, scaleBoost = 1.055f, rotY = 2.5f),
    PROUD_CHEER("뿌듯 응원", "PROUD", "proud encouraging expression cheering on a close friend", rotX = -4f, scaleBoost = 1.04f),
    SURPRISED_WOW("깜짝 놀람", "WOW!", "pleasantly surprised amazed expression with wide bright eyes", scaleBoost = 1.065f, rotX = -4.5f),
    THOUGHTFUL_MUSE("생각 중", "HMM", "thoughtfully looking slightly upward tapping chin while thinking", tiltZ = -4.5f, rotY = 5.5f, rotX = -2.5f),
    SHY_BLUSH("수줍 미소", "SHY", "gentle shy warm smile with soft rosy cheeks", tiltZ = 3.5f, rotY = -4f),
    CLAPPING_BRAVO("박수 갈채", "BRAVO", "clapping both hands happily celebrating great pronunciation", bounceY = -9f, scaleBoost = 1.035f),
    HIGH_FIVE_LEAN("하이파이브", "HIGH5", "raising an open palm forward for a friendly video-call high five", rotX = 5f, scaleBoost = 1.06f),
    THUMBS_UP_COOL("최고 엄지", "BEST", "giving an enthusiastic thumbs-up gesture with a proud grin", tiltZ = -2.5f, scaleBoost = 1.04f),
    COFFEE_CHILL("카페 여유", "CHILL", "relaxed friendly smile holding a warm coffee cup in a cozy mood", tiltZ = 2f, rotY = -3f, bounceY = 3f),
    OCEAN_BREEZE("바닷바람", "BREEZE", "refreshed carefree smile enjoying a sunny coastal ocean breeze", tiltZ = -2.2f, rotY = 5f),
    CITY_VIBE("뉴욕 바이브", "NYC", "stylish upbeat smile enjoying energetic New York City street vibes", rotY = -5.5f, scaleBoost = 1.03f),
    HEART_EYES("하트 뿅뿅", "LOVE", "making a cute finger heart or hand heart gesture with an affectionate smile", scaleBoost = 1.04f),
    AMAZED_SPARKLE("반짝 감탄", "STAR", "delighted expression with sparkling eyes admiring something awesome", rotX = -3.5f, scaleBoost = 1.045f),
    GENTLE_COMFORT("따뜻한 위로", "OKAY", "compassionate gentle reassuring smile comforting a friend", tiltZ = 3f, rotX = 2.5f),
    PLAYFUL_TEASE("장난꾸러기", "HEHE", "cheeky playful teasing smile with one eyebrow slightly raised", tiltZ = -5f, rotY = 4f, winkLeft = true),
    VICTORY_DANCE("승리 댄스", "WIN!", "celebrating victory with both fists raised happily", bounceY = -14f, tiltZ = 3.5f),
    CONFIDENT_SMIRK("자신만만", "COOL", "cool confident relaxed grin looking effortlessly stylish", rotY = -6f, rotX = -2.5f, scaleBoost = 1.03f),
    SPEAK_ANIMATED("활기찬 대화", "TALK", "animatedly speaking mid-sentence with a natural open conversational smile", rotX = 2f, bounceY = -4f, scaleBoost = 1.025f),
    DRAMATIC_GASP("헉 진짜?", "OMG!", "expressive playful gasp of disbelief with hands near cheeks", scaleBoost = 1.06f, tiltZ = -3.5f),
    PEACE_SIGN("브이 포즈", "PEACE", "flashing a cheerful peace V-sign with fingers near face", tiltZ = 4f, rotY = -3.5f),
    SALUTE_READY("경례 준비", "READY", "giving a cute playful two-finger salute ready for English adventure", rotX = -3f, scaleBoost = 1.03f),
    MUSIC_SWAY("리듬 흔들", "VIBE", "happy relaxed smile swaying gently to music", tiltZ = 4.5f, bounceY = -5f),
    RAINBOW_CHEER("무지개 응원", "CHEER", "radiant joyful cheering expression radiating positive energy", bounceY = -10f, scaleBoost = 1.045f),
    SUNSET_DREAM("노을 감성", "DREAM", "serene peaceful dreamy smile bathed in golden-hour sunlight", rotY = 5f, tiltZ = 1.8f),
    BYE_WAVE("또 만나요", "BYE!", "waving goodbye warmly with a sweet affectionate smile", tiltZ = -4f, bounceY = -7f);

    companion object {
        fun inferFromSpeech(text: String, isListening: Boolean, isSpeaking: Boolean): MotionPhotoGesture {
            val lower = text.lowercase()
            return when {
                isListening -> LISTEN_LEAN_IN
                lower.contains("wow") || lower.contains("amazing") || lower.contains("awesome") -> AMAZED_SPARKLE
                lower.contains("great") || lower.contains("perfect") || lower.contains("good job") -> CLAPPING_BRAVO
                lower.contains("haha") || lower.contains("lol") || lower.contains("funny") -> LAUGHING_JOY
                lower.contains("?") -> CURIOUS_TILT
                lower.contains("beach") || lower.contains("guam") || lower.contains("ocean") -> OCEAN_BREEZE
                lower.contains("new york") || lower.contains("nyc") || lower.contains("manhattan") -> CITY_VIBE
                lower.contains("coffee") || lower.contains("cafe") -> COFFEE_CHILL
                lower.contains("love") || lower.contains("sweet") -> HEART_EYES
                lower.contains("bye") || lower.contains("see you") -> BYE_WAVE
                lower.contains("hello") || lower.contains("hey") || lower.contains("hi ") -> FRIENDLY_WAVE
                isSpeaking -> SPEAK_ANIMATED
                else -> WARM_SMILE
            }
        }
    }
}

/**
 * Zero-Lag GPU Motion-Photo Rig for the English Video-Call Friend Character:
 * - Reads ALL animation states exclusively inside `Modifier.graphicsLayer { ... }` so ZERO
 *   Compose recompositions occur per frame (silky 60fps, fixes lag completely!).
 * - ZERO unnatural flat drawn eye boxes! Instead, crossfades smoothly to the real photorealistic
 *   closed-eye frame synthesized by `gemini-nano-banana-2.1` when available.
 * - Immediately reflects conversation topic atmosphere (Airport, Cafe, Supermarket, Park, Guam, NYC)
 *   and smoothly crossfades into the full Nano Banana 2.1 topic-edited scene portrait!
 */
@Composable
fun LiveMotionPhotoFriendView(
    @DrawableRes portraitRes: Int,
    contentDescription: String,
    activeGesture: MotionPhotoGesture,
    activeTopicTag: String,
    nanoBananaSceneBitmap: Bitmap?,
    nanoBananaBlinkBitmap: Bitmap?,
    isSpeaking: Boolean,
    isListening: Boolean,
    speechBeatIndex: Int = 0,
    userRmsDb: Float = 0f,
    extraCropZoom: Float = 1.0f,
    verticalOffsetFraction: Float = 0f,
    modifier: Modifier = Modifier
) {
    // 1. Natural randomized periodic blink state (used ONLY when photorealistic Nano Banana blink frame is ready)
    val blinkTarget = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(nanoBananaBlinkBitmap != null) {
        if (nanoBananaBlinkBitmap == null) {
            blinkTarget.floatValue = 0f
            return@LaunchedEffect
        }
        while (true) {
            delay(Random.nextLong(2400L, 4800L))
            blinkTarget.floatValue = 1f
            delay(125L)
            blinkTarget.floatValue = 0f
            if (Random.nextFloat() < 0.25f) {
                delay(155L)
                blinkTarget.floatValue = 1f
                delay(105L)
                blinkTarget.floatValue = 0f
            }
        }
    }
    val smoothBlinkState = animateFloatAsState(
        targetValue = blinkTarget.floatValue,
        animationSpec = tween(durationMillis = 75, easing = FastOutSlowInEasing),
        label = "nanoBlinkAlpha"
    )

    // 2. GPU-only continuous gentle breathing + handheld camera micro-sway + speaking cadence beat
    val infinite = rememberInfiniteTransition(label = "gpuMotionPhoto")
    val breathPhaseState = infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathPhase"
    )
    val swayPhaseState = infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cameraSwayPhase"
    )
    val talkPulseState = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "talkPulse"
    )

    // Organic word-synced speaking micro-nod that alternates gently on each spoken word!
    val cadenceNodTarget = if (isSpeaking) {
        when (speechBeatIndex % 4) {
            0 -> 1.0f
            1 -> -0.6f
            2 -> 0.8f
            else -> -0.3f
        }
    } else {
        0f
    }
    val cadenceNodState = animateFloatAsState(
        targetValue = cadenceNodTarget,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
        label = "cadenceNod"
    )

    // Attentive lean-in zoom when user speaks loudly into mic
    val voiceLeanState = animateFloatAsState(
        targetValue = if (isListening) (userRmsDb.coerceIn(0f, 10f) / 10f) * 0.022f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 180f),
        label = "voiceLeanZoom"
    )

    // 3. Spring-damped target values for the 33 gestures (read ONLY inside graphicsLayer!)
    val tiltState = animateFloatAsState(
        targetValue = activeGesture.tiltZ,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 200f),
        label = "gestureTilt"
    )
    val rotYState = animateFloatAsState(
        targetValue = activeGesture.rotY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 190f),
        label = "gestureRotY"
    )
    val rotXState = animateFloatAsState(
        targetValue = activeGesture.rotX,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 190f),
        label = "gestureRotX"
    )
    val scaleState = animateFloatAsState(
        targetValue = activeGesture.scaleBoost + if (isSpeaking) 0.015f else if (isListening) 0.028f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 210f),
        label = "gestureScale"
    )
    val bounceYState = animateFloatAsState(
        targetValue = activeGesture.bounceY,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 240f),
        label = "gestureBounce"
    )

    val sceneImageBitmap = remember(nanoBananaSceneBitmap) { nanoBananaSceneBitmap?.asImageBitmap() }
    val blinkImageBitmap = remember(nanoBananaBlinkBitmap) { nanoBananaBlinkBitmap?.asImageBitmap() }
    val effectiveCropZoom = if (sceneImageBitmap != null) 1.0f else extraCropZoom
    val effectiveVerticalOffset = if (sceneImageBitmap != null) 0f else verticalOffsetFraction

    // Topic ambient studio lighting grade (instant 0ms visual transformation whenever topic changes — zero quota error!)
    val topicKey = remember(activeTopicTag) { NanoBananaMotionEngine.normalizeTopicKey(activeTopicTag) }
    val topicAtmosphereBrush = remember(topicKey) {
        when (topicKey) {
            "airport" -> Brush.verticalGradient(
                listOf(Color(0xFF0284C7).copy(alpha = 0.24f), Color(0xFF38BDF8).copy(alpha = 0.05f), Color(0xFF0F172A).copy(alpha = 0.28f))
            )
            "cafe" -> Brush.verticalGradient(
                listOf(Color(0xFF9A3412).copy(alpha = 0.22f), Color(0xFFFF7A59).copy(alpha = 0.06f), Color(0xFF1C1917).copy(alpha = 0.26f))
            )
            "mart" -> Brush.verticalGradient(
                listOf(Color(0xFF2563EB).copy(alpha = 0.20f), Color(0xFF38BDF8).copy(alpha = 0.05f), Color(0xFF0F172A).copy(alpha = 0.24f))
            )
            "restaurant" -> Brush.verticalGradient(
                listOf(Color(0xFFC2410C).copy(alpha = 0.22f), Color(0xFFFB7185).copy(alpha = 0.06f), Color(0xFF1E1B2E).copy(alpha = 0.28f))
            )
            "hotel" -> Brush.verticalGradient(
                listOf(Color(0xFF6D28D9).copy(alpha = 0.20f), Color(0xFF8B7CFF).copy(alpha = 0.06f), Color(0xFF1E1B4B).copy(alpha = 0.28f))
            )
            "intro" -> Brush.verticalGradient(
                listOf(Color(0xFF4F6EF7).copy(alpha = 0.20f), Color(0xFF38BDF8).copy(alpha = 0.06f), Color(0xFF0D0F14).copy(alpha = 0.24f))
            )
            "guam_beach" -> Brush.verticalGradient(
                listOf(Color(0xFF0EA5E9).copy(alpha = 0.18f), Color.Transparent, Color(0xFF0C4A6E).copy(alpha = 0.18f))
            )
            else -> Brush.verticalGradient(
                listOf(Color(0xFF1E3A8A).copy(alpha = 0.10f), Color.Transparent, Color(0xFF0D0F14).copy(alpha = 0.16f))
            )
        }
    }
    val bokehOrbColor = remember(topicKey) {
        when (topicKey) {
            "airport" -> Color(0xFF7DD3FC)
            "cafe", "restaurant" -> Color(0xFFFFA891)
            "hotel" -> Color(0xFFC4B5FD)
            "mart", "intro" -> Color(0xFF93C5FD)
            "guam_beach" -> Color(0xFF67E8F9)
            else -> Color(0xFFA5B4FC)
        }
    }

    // Full-bleed natural front-camera framing: overscan >= 1.14f + strictly bounded micro-tilt
    // guarantees ZERO black triangular edges at screen corners while feeling like a live FaceTime call!
    val motionModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            val breath = breathPhaseState.value
            val sway = swayPhaseState.value
            val talk = if (isSpeaking) talkPulseState.value else 0f
            val nod = cadenceNodState.value
            val baseScale = 1.14f * effectiveCropZoom * (scaleState.value + voiceLeanState.value)
            val clampedTiltZ = (sin(sway * 0.7f) * 0.42f + tiltState.value * 0.15f + nod * 0.28f).coerceIn(-1.05f, 1.05f)
            val clampedRotY = (rotYState.value * 0.32f + sin(breath) * 0.75f + nod * 0.65f).coerceIn(-2.5f, 2.5f)
            val clampedRotX = (rotXState.value * 0.28f + talk * 0.95f + nod * 0.85f).coerceIn(-2.4f, 2.4f)
            val clampedTx = (sin(sway * 0.85f) * 4.0f + rotYState.value * 1.0f).coerceIn(-13f, 13f)
            val frameShiftY = size.height * effectiveVerticalOffset
            val clampedTy = (cos(breath) * 2.8f + bounceYState.value * 0.48f - talk * 2.8f + nod * 2.4f).coerceIn(-14f, 14f) + frameShiftY

            scaleX = baseScale
            scaleY = baseScale * (1f + talk * 0.007f)
            translationX = clampedTx
            translationY = clampedTy
            rotationZ = clampedTiltZ
            rotationY = clampedRotY
            rotationX = clampedRotX
            cameraDistance = 24f * density
            transformOrigin = TransformOrigin(0.5f, 0.42f)
        }

    // Instant photorealistic scenic background matching the selected conversation theme!
    @DrawableRes val topicBackdropRes: Int? = remember(topicKey) {
        when (topicKey) {
            "airport" -> R.drawable.img_scene_airport_terminal_1795687513640
            "cafe", "restaurant" -> R.drawable.img_scene_paris_cafe_1795687515140
            "hotel" -> R.drawable.img_scene_luxury_hotel_1795687515906
            "mart" -> R.drawable.img_scene_london_shopping_1795687513464
            "directions" -> R.drawable.img_scene_tokyo_shibuya_1795687516377
            "guam_beach" -> R.drawable.img_friend_guam_beach_no_mic_1795422199110
            "intro" -> R.drawable.img_friend_nyc_no_mic_1795422201093
            else -> null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Crossfade smoothly between base real-human portrait, topic scenic stage, and AI-edited scene portrait
        Crossfade(
            targetState = Triple(sceneImageBitmap, topicBackdropRes, portraitRes),
            animationSpec = tween(220),
            label = "topicSceneCrossfade"
        ) { (aiBitmap, backdropRes, fallbackRes) ->
            when {
                aiBitmap != null -> {
                    Image(
                        bitmap = aiBitmap,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = motionModifier
                    )
                }
                backdropRes != null -> {
                    // Full-bleed themed real-world scene + crisp framed live tutor video portrait
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(backdropRes),
                            contentDescription = activeTopicTag,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF0F172A).copy(alpha = 0.25f),
                                            Color.Transparent,
                                            Color(0xFF0F172A).copy(alpha = 0.72f)
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(bottom = 128.dp)
                                .width(236.dp)
                                .height(308.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .border(
                                    width = if (isSpeaking) 3.dp else 2.dp,
                                    color = if (isSpeaking) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(32.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(fallbackRes),
                                contentDescription = contentDescription,
                                contentScale = ContentScale.Crop,
                                modifier = motionModifier
                            )
                        }
                    }
                }
                else -> {
                    Image(
                        painter = painterResource(fallbackRes),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = motionModifier
                    )
                }
            }
        }

        // Real photorealistic closed-eye blink layer from Nano Banana 2.1 (GPU alpha only, ZERO fake boxes!)
        if (blinkImageBitmap != null) {
            Image(
                bitmap = blinkImageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val breath = breathPhaseState.value
                        val sway = swayPhaseState.value
                        val talk = if (isSpeaking) talkPulseState.value else 0f
                        val nod = cadenceNodState.value
                        val baseScale = 1.14f * effectiveCropZoom * (scaleState.value + voiceLeanState.value)
                        val clampedTiltZ = (sin(sway * 0.7f) * 0.42f + tiltState.value * 0.15f + nod * 0.28f).coerceIn(-1.05f, 1.05f)
                        val clampedRotY = (rotYState.value * 0.32f + sin(breath) * 0.75f + nod * 0.65f).coerceIn(-2.5f, 2.5f)
                        val clampedRotX = (rotXState.value * 0.28f + talk * 0.95f + nod * 0.85f).coerceIn(-2.4f, 2.4f)
                        val clampedTx = (sin(sway * 0.85f) * 4.0f + rotYState.value * 1.0f).coerceIn(-13f, 13f)
                        val frameShiftY = size.height * effectiveVerticalOffset
                        val clampedTy = (cos(breath) * 2.8f + bounceYState.value * 0.48f - talk * 2.8f + nod * 2.4f).coerceIn(-14f, 14f) + frameShiftY

                        alpha = smoothBlinkState.value
                        scaleX = baseScale
                        scaleY = baseScale * (1f + talk * 0.007f)
                        translationX = clampedTx
                        translationY = clampedTy
                        rotationZ = clampedTiltZ
                        rotationY = clampedRotY
                        rotationX = clampedRotX
                        cameraDistance = 24f * density
                        transformOrigin = TransformOrigin(0.5f, 0.42f)
                    }
            )
        }

        // Subtle topic-matched ambient color grade + gentle cinema bokeh depth lights
        if (sceneImageBitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(topicAtmosphereBrush)
            )
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val p = swayPhaseState.value
                val speakGlow = if (isSpeaking) 0.11f else 0.06f
                // Soft upper-left & upper-right out-of-focus cinematic ambient light bokeh (never blocks face!)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bokehOrbColor.copy(alpha = speakGlow), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(
                            size.width * (0.14f + sin(p) * 0.03f),
                            size.height * (0.16f + cos(p * 0.8f) * 0.02f)
                        ),
                        radius = size.width * 0.34f
                    ),
                    radius = size.width * 0.34f,
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * (0.14f + sin(p) * 0.03f),
                        size.height * (0.16f + cos(p * 0.8f) * 0.02f)
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bokehOrbColor.copy(alpha = speakGlow * 0.85f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(
                            size.width * (0.86f + cos(p * 0.9f) * 0.03f),
                            size.height * (0.24f + sin(p * 0.7f) * 0.025f)
                        ),
                        radius = size.width * 0.30f
                    ),
                    radius = size.width * 0.30f,
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * (0.86f + cos(p * 0.9f) * 0.03f),
                        size.height * (0.24f + sin(p * 0.7f) * 0.025f)
                    )
                )
            }
        }
    }
}
