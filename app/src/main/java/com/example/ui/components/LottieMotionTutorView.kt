package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.R
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightSurface
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import kotlinx.coroutines.delay

/**
 * Apache-2.0 Open-Source Lottie Motion-Graphics Tutor Avatar & Expression Reaction Stage:
 * - Zero-network 60fps local Lottie JSON avatar synced with speech speed, listening volume, & mood
 * - One-shot celebration sparkle Lottie overlay on positive feedback / high pronunciation scores
 * - Also serves as instant fail-safe fallback when WebGL 3D TalkingHead is unavailable or times out
 */
@Composable
fun LottieMotionTutorView(
    tutorName: String,
    activeGesture: MotionPhotoGesture,
    isSpeaking: Boolean,
    isListening: Boolean,
    speechRate: Float,
    userRmsDb: Float,
    latestPronunciationScore: Int?,
    modifier: Modifier = Modifier
) {
    val avatarComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_tutor_avatar))
    val waveComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_voice_wave))
    val cheerComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_reaction_cheer))

    // Sync animation playback speed directly with speech cadence
    val avatarSpeed = when {
        isSpeaking -> (1.35f * speechRate).coerceIn(0.9f, 2.0f)
        isListening -> 0.65f
        else -> 0.42f
    }
    val avatarProgress by animateLottieCompositionAsState(
        composition = avatarComposition,
        iterations = LottieConstants.IterateForever,
        speed = avatarSpeed
    )

    val waveSpeed = if (isListening) {
        (1.0f + (userRmsDb.coerceIn(0f, 10f) / 10f)).coerceIn(1.0f, 2.0f)
    } else if (isSpeaking) {
        (1.2f * speechRate).coerceIn(0.9f, 1.8f)
    } else {
        0.5f
    }
    val waveProgress by animateLottieCompositionAsState(
        composition = waveComposition,
        iterations = LottieConstants.IterateForever,
        speed = waveSpeed
    )

    // Trigger one-shot sparkle cheer reaction on celebratory gestures or passing score
    var cheerBurstCount by remember { mutableIntStateOf(0) }
    var showCheerOverlay by remember { mutableStateOf(false) }

    val isPositiveGesture = activeGesture == MotionPhotoGesture.CLAPPING_BRAVO ||
        activeGesture == MotionPhotoGesture.AMAZED_SPARKLE ||
        activeGesture == MotionPhotoGesture.EXCITED_BOUNCE ||
        activeGesture == MotionPhotoGesture.HEART_EYES ||
        activeGesture == MotionPhotoGesture.THUMBS_UP_COOL

    LaunchedEffect(activeGesture, latestPronunciationScore) {
        if (isPositiveGesture || (latestPronunciationScore != null && latestPronunciationScore >= 80)) {
            cheerBurstCount++
            showCheerOverlay = true
            delay(1500L)
            showCheerOverlay = false
        }
    }

    val cheerProgress by animateLottieCompositionAsState(
        composition = cheerComposition,
        isPlaying = showCheerOverlay,
        iterations = 1,
        restartOnPlay = true
    )

    val avatarScale by animateFloatAsState(
        targetValue = when {
            isSpeaking -> 1.05f
            isListening -> 1.02f + (userRmsDb.coerceIn(0f, 10f) * 0.005f)
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f),
        label = "lottieAvatarScale"
    )

    val accentColor = when {
        isListening -> StudioCoral
        isSpeaking -> StudioCobalt
        isPositiveGesture -> GlacierCyan
        else -> StudioIris
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.28f),
                        NightSurface,
                        NightBackground
                    )
                )
            )
            .testTag("lottie_motion_tutor_stage"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 140.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(270.dp)
            ) {
                LottieAnimation(
                    composition = avatarComposition,
                    progress = { avatarProgress },
                    modifier = Modifier
                        .size(260.dp)
                        .scale(avatarScale)
                )

                // Celebratory Reaction Burst Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showCheerOverlay,
                    enter = fadeIn() + scaleIn(initialScale = 0.7f),
                    exit = fadeOut() + scaleOut(targetScale = 1.15f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    LottieAnimation(
                        composition = cheerComposition,
                        progress = { cheerProgress },
                        modifier = Modifier.size(110.dp)
                    )
                }
            }

            // Expression & Live Lottie Sound Wave Capsule
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.48f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = "$tutorName · ${activeGesture.labelKo}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = Color.White
                    )
                    if (isSpeaking || isListening) {
                        LottieAnimation(
                            composition = waveComposition,
                            progress = { waveProgress },
                            modifier = Modifier
                                .width(44.dp)
                                .height(20.dp)
                        )
                    }
                }
            }
        }
    }
}
