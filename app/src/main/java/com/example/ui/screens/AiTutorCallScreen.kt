package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.HumanTutorRepository
import com.example.engine.VoiceEngineState
import com.example.model.AiEngineMode
import com.example.model.AiHumanTutor
import com.example.model.TutorChatMessage
import com.example.ui.components.LiveMotionPhotoFriendView
import com.example.ui.components.MotionPhotoGesture
import com.example.ui.components.PronunciationReportCard
import com.example.ui.components.VoiceWaveformOrb3D
import com.example.ui.components.YouTubeWordContextDialog
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.TutorCallState

private data class IconQuickAction(
    val icon: ImageVector,
    val labelKo: String,
    val prompt: String,
    val isQuestion: Boolean = false
)

private val faceTalkQuickChips = listOf(
    IconQuickAction(Icons.Default.ShoppingCart, "마트", "Hi there! Are you paying by card or cash at the supermarket today?"),
    IconQuickAction(Icons.Default.WavingHand, "소개", "Nice to meet you here at the park! Tell me a little about yourself in English!"),
    IconQuickAction(Icons.Default.LocalCafe, "카페", "Welcome to the cafe! What coffee or drink can I get started for you today?"),
    IconQuickAction(Icons.Default.Flight, "공항", "Good afternoon! Welcome to the airport gate. What's the purpose of your visit?")
)

/**
 * Kakao FaceTalk-inspired Full-Screen Conversation Learning ("회화학습"):
 * - Zero-lag GPU motion-photo stage powered by Gemini Nano Banana 2.1
 * - Automatic background switching matching the active conversation topic (Airport, Cafe, Mart, Park, NYC, Guam)
 * - Photorealistic Nano Banana closed-eye blinking + 33 expressions & gestures
 */
@Composable
fun AiTutorCallScreen(
    state: TutorCallState,
    activePlayerName: String,
    voiceState: VoiceEngineState,
    engineMode: AiEngineMode,
    speechRate: Float,
    handsFreeMode: Boolean,
    onSelectTutor: (AiHumanTutor) -> Unit,
    onSelectTopic: (String, String) -> Unit,
    onSynthesizeNanoBanana: (String, MotionPhotoGesture) -> Unit,
    onToggleKoreanSubtitles: () -> Unit,
    onToggleKoreanVoiceMode: () -> Unit,
    onSetSpeechRate: (Float) -> Unit,
    onSetHandsFree: (Boolean) -> Unit,
    onStartListening: () -> Unit,
    onStopVoice: () -> Unit,
    onSendTypedMessage: (String) -> Unit,
    onReplayTutorText: (String) -> Unit
) {
    val context = LocalContext.current
    var typedMessage by remember { mutableStateOf("") }
    var showTutorPicker by remember { mutableStateOf(false) }
    var showKeyboardDock by remember { mutableStateOf(false) }
    var showHistoryOverlay by remember { mutableStateOf(false) }
    var showPronunciationDetail by remember { mutableStateOf(false) }
    var showGestureBar by remember { mutableStateOf(false) }
    var manualGesture by remember { mutableStateOf<MotionPhotoGesture?>(null) }
    var youtubeQuery by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartListening()
    }

    val tutor = state.selectedTutor
    val activeGesture = manualGesture ?: MotionPhotoGesture.inferFromSpeech(
        text = state.liveSubtitleEn,
        isListening = voiceState.isListening,
        isSpeaking = voiceState.isSpeaking
    )
    val isGuamPortrait = tutor.portraitRes == R.drawable.img_friend_guam_beach_no_mic_1795422199110
    val timerText = "%02d:%02d".format(state.callSeconds / 60, state.callSeconds % 60)
    val previousMsg = state.messages.dropLast(1).lastOrNull()
    val subtitleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.9f),
        offset = Offset(0f, 3f),
        blurRadius = 10f
    )

    if (youtubeQuery != null) {
        YouTubeWordContextDialog(
            wordOrPhrase = youtubeQuery!!,
            onDismiss = { youtubeQuery = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
    ) {
        // 1. ZERO-LAG GPU MOTION-PHOTO FRIEND STAGE (Nano Banana 2.1 Topic Scenes + Photorealistic Blink!)
        LiveMotionPhotoFriendView(
            portraitRes = tutor.portraitRes,
            contentDescription = "${tutor.name} 라이브 회화학습",
            activeGesture = activeGesture,
            activeTopicTag = state.activeTopicTag,
            nanoBananaSceneBitmap = state.nanoBananaSceneBitmap,
            nanoBananaBlinkBitmap = state.nanoBananaBlinkBitmap,
            isSpeaking = voiceState.isSpeaking,
            isListening = voiceState.isListening,
            extraCropZoom = if (isGuamPortrait) 1.32f else 1.0f,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle Transparent Vignette for Crisp Floating Subtitles
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.50f),
                            0.22f to Color.Transparent,
                            0.52f to Color.Black.copy(alpha = 0.16f),
                            0.78f to Color.Black.copy(alpha = 0.62f),
                            1.0f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // 2. TOP FACETALK FLOATING HEADER (Minimal Icons + Timer + Active Topic Location)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LIVE Timer, Tutor & Active Topic Scene Location Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.48f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (voiceState.isListening) SpartaRed else AdventureEmerald)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${tutor.name} · $timerText",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = state.sceneLocationLabelKo,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TrophyGold
                            )
                        }
                    }
                }

                // Compact FaceTalk Circular Icon Controls
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FaceTalkIconBtn(
                        icon = Icons.Default.Mood,
                        desc = "33종 모션표정",
                        active = showGestureBar,
                        activeColor = CoralOrange,
                        tag = "facetalk_motion_gestures",
                        onClick = { showGestureBar = !showGestureBar }
                    )
                    FaceTalkIconBtn(
                        icon = Icons.Default.SwitchAccount,
                        desc = "친구·배경 변경",
                        active = showTutorPicker,
                        activeColor = TrophyGold,
                        tag = "facetalk_switch_tutor",
                        onClick = { showTutorPicker = !showTutorPicker }
                    )
                    FaceTalkIconBtn(
                        icon = Icons.Default.ClosedCaption,
                        desc = "자막",
                        active = state.showKoreanTranslations,
                        activeColor = AdventureEmerald,
                        tag = "video_toggle_subtitles",
                        onClick = onToggleKoreanSubtitles
                    )
                    FaceTalkIconBtn(
                        icon = Icons.Default.HeadsetMic,
                        desc = "연속회화",
                        active = handsFreeMode,
                        activeColor = SkyQuestBlue,
                        tag = "video_hands_free_switch",
                        onClick = { onSetHandsFree(!handsFreeMode) }
                    )
                    // 1-Tap Speed Cycle (0.8x -> 1.0x -> 1.15x)
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.48f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                val next = when {
                                    speechRate < 0.9f -> 1.0f
                                    speechRate < 1.1f -> 1.15f
                                    else -> 0.8f
                                }
                                onSetSpeechRate(next)
                            }
                            .testTag("facetalk_speed_cycle")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${speechRate}x",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }
                    FaceTalkIconBtn(
                        icon = Icons.Default.History,
                        desc = "기록",
                        active = showHistoryOverlay,
                        activeColor = RoyalPurple,
                        tag = "facetalk_history_btn",
                        onClick = { showHistoryOverlay = !showHistoryOverlay }
                    )
                }
            }

            // Expandable 33-Expression Motion-Photo Gesture Strip
            AnimatedVisibility(visible = showGestureBar) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.56f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "표정·제스처 (${MotionPhotoGesture.entries.size}종) · ${activeGesture.labelKo}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TrophyGold
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AdventureEmerald,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !state.isGeneratingScene) {
                                        onSynthesizeNanoBanana(state.activeTopicTag, activeGesture)
                                    }
                                    .testTag("nano_banana_synthesize_gesture_btn")
                            ) {
                                Text(
                                    text = if (state.isGeneratingScene) "나노바나나 생성 중..." else "나노바나나 실사 생성",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item(key = "gesture_auto") {
                                val isAuto = manualGesture == null
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isAuto) AdventureEmerald else Color.White.copy(alpha = 0.14f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isAuto) Color.White else Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { manualGesture = null }
                                        .testTag("motion_gesture_auto")
                                ) {
                                    Text(
                                        text = "자동표정",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(MotionPhotoGesture.entries, key = { it.name }) { gesture ->
                                val selected = manualGesture == gesture
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) CoralOrange else Color.White.copy(alpha = 0.14f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) TrophyGold else Color.White.copy(alpha = 0.24f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { manualGesture = gesture }
                                        .testTag("motion_gesture_${gesture.name}")
                                ) {
                                    Text(
                                        text = "${gesture.labelKo} · ${gesture.badgeIcon}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Friend & Location Avatar Strip (only when tapped!)
            AnimatedVisibility(visible = showTutorPicker) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(HumanTutorRepository.tutors, key = { it.id }) { item ->
                        val selected = item.id == tutor.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectTutor(item)
                                    showTutorPicker = false
                                }
                                .padding(2.dp)
                                .testTag("tutor_chip_${item.id}")
                        ) {
                            Image(
                                painter = painterResource(id = item.portraitRes),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(
                                        if (selected) 2.5.dp else 1.dp,
                                        if (selected) TrophyGold else Color.White.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, shadow = subtitleShadow),
                                color = if (selected) TrophyGold else Color.White
                            )
                        }
                    }
                }
            }

            // Top-Right Floating Kakao FaceTalk Self-Cam PiP Window + Score Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Live Voice Command / Nano Banana Scene Generation Status Pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.isGeneratingScene) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xCC148F68),
                            border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = TrophyGold,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "나노바나나 '${state.sceneLocationLabelKo}' 배경·눈깜빡임 생성 중...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    if (!state.sceneErrorKo.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SpartaRed.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = state.sceneErrorKo,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    if (voiceState.isListening || state.isThinking) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (voiceState.isListening) SpartaRed.copy(alpha = 0.85f) else TrophyGold.copy(alpha = 0.88f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.isThinking) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF3E2C00),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(13.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (state.isThinking) "응답 중" else if (state.koreanVoiceInputMode) "KR 듣는 중" else "EN 듣는 중",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (state.isThinking) Color(0xFF3E2C00) else Color.White
                                )
                            }
                        }
                    }
                    if (state.liveVoiceCommandNotice.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = RoyalPurple.copy(alpha = 0.82f)
                        ) {
                            Text(
                                text = state.liveVoiceCommandNotice.removePrefix("음성 설정 반영: "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    state.latestPronunciation?.let { report ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AdventureEmerald.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showPronunciationDetail = !showPronunciationDetail }
                        ) {
                            Text(
                                text = "발음 ${report.overallScore}점 (${report.fluencyGrade}) ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Floating FaceTalk Self-Cam PiP Mini Frame (원삐 / 투삐 / 손님)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.5.dp,
                        if (voiceState.isListening) SpartaRed else Color.White.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.size(width = 76.dp, height = 96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (voiceState.isListening) SpartaRed else TrophyGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = activePlayerName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = if (state.koreanVoiceInputMode) "KR" else "EN",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
            }
        }

        // Expandable Pronunciation Detail Overlay (only when user taps score chip)
        AnimatedVisibility(
            visible = showPronunciationDetail && state.latestPronunciation != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
        ) {
            state.latestPronunciation?.let { report ->
                Box {
                    PronunciationReportCard(
                        report = report,
                        onCompareNativeClick = {
                            onReplayTutorText(state.liveNaturalRephraseEn.ifBlank { state.liveSubtitleEn })
                        }
                    )
                    IconButton(
                        onClick = { showPronunciationDetail = false },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
            }
        }

        // 3. FLOATING TRANSPARENT CINEMA SUBTITLES + FACETALK BOTTOM DOCK
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rising Transparent Previous Line (Faded upward cinema subtitle feel!)
            if (previousMsg != null) {
                Text(
                    text = previousMsg.textEn,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        shadow = subtitleShadow
                    ),
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }

            // Live User Speech Partial Subtitle (while speaking)
            if (voiceState.isListening && voiceState.partialTranscript.isNotBlank()) {
                Text(
                    text = "\"${voiceState.partialTranscript}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = subtitleShadow
                    ),
                    color = Color(0xFFFFB4A9),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // MAIN TRANSPARENT FLOATING ENGLISH & KOREAN SUBTITLES (Zero heavy box!)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReplayTutorText(state.liveSubtitleEn) }
            ) {
                Text(
                    text = state.liveSubtitleEn,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = subtitleShadow
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (state.showKoreanTranslations && state.liveSubtitleKo.isNotBlank()) {
                    Text(
                        text = state.liveSubtitleKo,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            shadow = subtitleShadow
                        ),
                        color = TrophyGold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Ultra-Compact Floating Translucent Correction / Rephrase Pill (Icon-First!)
            if (state.liveNaturalRephraseEn.isNotBlank() || state.liveCoachingCommentKo.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.42f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "코칭",
                            tint = TrophyGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = state.liveNaturalRephraseEn.ifBlank { state.liveCoachingCommentKo },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onReplayTutorText(state.liveNaturalRephraseEn.ifBlank { state.liveSubtitleEn }) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "다시 듣기",
                                tint = SkyQuestBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { youtubeQuery = state.liveNaturalRephraseEn.ifBlank { state.liveSubtitleEn } },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "실제 대사 클립",
                                tint = SpartaRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Single Compact Icon Quick-Topic Strip (No wall of text!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                faceTalkQuickChips.forEach { chip ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.42f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (chip.isQuestion) onSendTypedMessage(chip.prompt)
                                else onSelectTopic(chip.labelKo, chip.prompt)
                            }
                            .testTag("quick_kr_chip_${chip.labelKo}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = chip.icon,
                                contentDescription = chip.labelKo,
                                tint = TrophyGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = chip.labelKo,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Expandable Translucent Keyboard Bar (only shown when keyboard icon tapped)
            AnimatedVisibility(visible = showKeyboardDock) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = typedMessage,
                        onValueChange = { typedMessage = it },
                        placeholder = { Text("말하거나 질문 입력...", color = Color.White.copy(alpha = 0.65f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Black.copy(alpha = 0.55f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.45f),
                            focusedBorderColor = TrophyGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("video_call_text_input")
                    )
                    Surface(
                        shape = CircleShape,
                        color = AdventureEmerald,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (typedMessage.isNotBlank()) {
                                    onSendTypedMessage(typedMessage)
                                    typedMessage = ""
                                }
                            }
                            .testTag("video_call_send_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // REAL KAKAO FACETALK FLOATING BOTTOM ACTION ROW (Icon-First!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // EN / KR Voice Mode Circular Button
                Surface(
                    shape = CircleShape,
                    color = if (state.koreanVoiceInputMode) CoralOrange else Color.Black.copy(alpha = 0.52f),
                    border = BorderStroke(1.5.dp, if (state.koreanVoiceInputMode) Color.White else Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleKoreanVoiceMode)
                        .testTag("video_mode_kr_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "한영 전환",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (state.koreanVoiceInputMode) "KR" else "EN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                    }
                }

                // Replay Audio Button
                FaceTalkBigCircleBtn(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    desc = "다시 듣기",
                    bg = Color.Black.copy(alpha = 0.52f),
                    tag = "video_replay_subtitle",
                    onClick = { onReplayTutorText(state.liveSubtitleEn) }
                )

                // Center Large FaceTalk 3D Mic Orb
                VoiceWaveformOrb3D(
                    isListening = voiceState.isListening,
                    isSpeaking = voiceState.isSpeaking,
                    rmsDb = voiceState.rmsDb,
                    tag = "video_call_mic_orb",
                    modifier = Modifier.size(78.dp),
                    onClick = {
                        if (voiceState.isListening) {
                            onStopVoice()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) onStartListening()
                            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

                // Real Dialogue Clip Button
                FaceTalkBigCircleBtn(
                    icon = Icons.Default.PlayCircle,
                    desc = "대사 클립",
                    bg = SpartaRed.copy(alpha = 0.82f),
                    tag = "facetalk_clip_btn",
                    onClick = { youtubeQuery = state.liveNaturalRephraseEn.ifBlank { state.liveSubtitleEn } }
                )

                // Keyboard Toggle Button
                FaceTalkBigCircleBtn(
                    icon = Icons.Default.Keyboard,
                    desc = "키보드 입력",
                    bg = if (showKeyboardDock) SkyQuestBlue else Color.Black.copy(alpha = 0.52f),
                    tag = "facetalk_keyboard_toggle",
                    onClick = { showKeyboardDock = !showKeyboardDock }
                )
            }
        }

        // Translucent Slide-Up History Overlay (only when History icon is tapped)
        AnimatedVisibility(
            visible = showHistoryOverlay,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xEB0B131E),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "회화 자막 기록 (${state.messages.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        IconButton(onClick = { showHistoryOverlay = false }) {
                            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.messages.takeLast(15), key = { it.id }) { msg ->
                            TranscriptRowItem(
                                msg = msg,
                                tutorName = tutor.name,
                                playerName = activePlayerName,
                                onReplay = { onReplayTutorText(msg.textEn) },
                                onYouTube = { youtubeQuery = msg.textEn }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceTalkIconBtn(
    icon: ImageVector,
    desc: String,
    active: Boolean,
    activeColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (active) activeColor.copy(alpha = 0.88f) else Color.Black.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun FaceTalkBigCircleBtn(
    icon: ImageVector,
    desc: String,
    bg: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TranscriptRowItem(
    msg: TutorChatMessage,
    tutorName: String,
    playerName: String,
    onReplay: () -> Unit,
    onYouTube: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (msg.fromTutor) Color.White.copy(alpha = 0.1f) else AdventureEmerald.copy(alpha = 0.22f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (msg.fromTutor) tutorName else playerName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (msg.fromTutor) TrophyGold else AdventureEmerald
                )
                Text(
                    text = msg.textEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                if (!msg.translationKo.isNullOrBlank()) {
                    Text(
                        text = msg.translationKo,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
            if (msg.fromTutor && !msg.isStreaming) {
                IconButton(onClick = onReplay, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "듣기", tint = SkyQuestBlue, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onYouTube, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.PlayCircle, contentDescription = "클립", tint = SpartaRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
