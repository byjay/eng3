package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.TokenStatus
import com.example.model.TutorChatMessage
import com.example.ui.components.LiveMotionPhotoFriendView
import com.example.ui.components.LottieMotionTutorView
import com.example.ui.components.MotionPhotoGesture
import com.example.ui.components.PronunciationReportCard
import com.example.ui.components.SvgCssLipSyncRigView
import com.example.ui.components.TalkingHead3DTeacherView
import com.example.ui.components.VoiceWaveformOrb3D
import com.example.ui.components.YouTubeWordContextDialog
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.TutorCallState
import kotlinx.coroutines.delay

private enum class TutorStageMode(val labelKo: String) {
    PHOTO("실사"),
    SVG_RIG("캐릭터"),
    THREE_D("3D"),
    LOTTIE("모션")
}

private data class IconQuickAction(
    val icon: ImageVector,
    val labelKo: String,
    val prompt: String,
    val isQuestion: Boolean = false
)

private val faceTalkQuickChips = listOf(
    IconQuickAction(Icons.Outlined.ChatBubbleOutline, "수다", "Hey! How's your day going? Tell me anything fun that happened today!"),
    IconQuickAction(Icons.Outlined.LocalCafe, "카페", "Welcome to the cafe! What coffee or drink can I get started for you today?"),
    IconQuickAction(Icons.Outlined.Flight, "공항", "Good afternoon! Welcome to the airport gate. What's the purpose of your visit?"),
    IconQuickAction(Icons.Outlined.Restaurant, "식당", "Welcome in! How many people are in your party today, and would you like a window seat?"),
    IconQuickAction(Icons.Outlined.Hotel, "호텔", "Welcome to our hotel! Do you have a reservation under your name today?"),
    IconQuickAction(Icons.Outlined.ShoppingCart, "마트", "Hi there! Are you paying by card or cash at the supermarket today?"),
    IconQuickAction(Icons.Outlined.Directions, "길찾기", "Hey, you look like you're searching for a place! Where are you trying to get to?"),
    IconQuickAction(Icons.Outlined.WavingHand, "소개", "Nice to meet you here at the park! Tell me a little about yourself in English!")
)

private val koreanQuickHelpPrompts = listOf(
    "방금 문장 쉽게 설명해줘",
    "더 천천히 말해줘",
    "내 문장 더 자연스럽게 고쳐줘",
    "이 상황에서 쓸 원어민 표현 하나 알려줘"
)

private fun roleplayMissionsForTopic(topicTag: String): List<String> {
    return when {
        topicTag.contains("공항") -> listOf("방문 목적 말하기", "체류 기간 답하기", "게이트 질문하기")
        topicTag.contains("카페") -> listOf("음료 주문하기", "옵션 선택하기", "결제·감사 인사")
        topicTag.contains("식당") -> listOf("인원·좌석 요청", "추천 메뉴 주문", "계산서 요청하기")
        topicTag.contains("호텔") -> listOf("예약 체크인하기", "조식·시설 묻기", "추가 요청하기")
        topicTag.contains("마트") -> listOf("결제 수단 말하기", "봉투 요청하기", "영수증 받기")
        topicTag.contains("길찾기") -> listOf("목적지 묻기", "거리·시간 확인", "방향 감사 인사")
        topicTag.contains("소개") -> listOf("인사·취미 말하기", "질문 되묻기", "응원 주고받기")
        else -> listOf("안부 인사하기", "오늘 일정 나누기", "자연스럽게 답하기")
    }
}

/**
 * Kakao FaceTalk-inspired Full-Screen Conversation Learning ("회화학습"):
 * - Zero-lag GPU motion-photo stage powered by Gemini Nano Banana 2.1
 * - Automatic background switching matching the active conversation topic (Airport, Cafe, Mart, Park, NYC, Guam)
 * - Photorealistic Nano Banana closed-eye blinking + 33 expressions & gestures
 */
@OptIn(ExperimentalLayoutApi::class)
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
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onToggleKoreanSubtitles: () -> Unit,
    onToggleKoreanVoiceMode: () -> Unit,
    onSetSpeechRate: (Float) -> Unit,
    onSetHandsFree: (Boolean) -> Unit,
    onStartListening: () -> Unit,
    onStopVoice: () -> Unit,
    onSendTypedMessage: (String) -> Unit,
    onReplayTutorText: (String) -> Unit,
    onSpeakSlowWord: (String) -> Unit,
    onStartShadowingTarget: (String) -> Unit,
    onCancelShadowingTarget: () -> Unit,
    onToggleProactiveMode: () -> Unit = {},
    onTriggerProactiveCheckIn: () -> Unit = {},
    onAutoProactiveCheckIn: () -> Unit = {},
    onUpdateLearnerName: (String) -> Unit = {},
    onUpdateConversationGoal: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var typedMessage by remember { mutableStateOf("") }
    var showTutorPicker by remember { mutableStateOf(false) }
    var showKeyboardDock by remember { mutableStateOf(false) }
    var showHistoryOverlay by remember { mutableStateOf(false) }
    var showScorecardOverlay by remember { mutableStateOf(false) }
    var showEnginePicker by remember { mutableStateOf(false) }
    var showPronunciationDetail by remember { mutableStateOf(false) }
    var showGestureBar by remember { mutableStateOf(false) }
    var showStudioTray by remember { mutableStateOf(false) }
    var showMissionGoalTray by remember { mutableStateOf(false) }
    var showGuidedHints by remember { mutableStateOf(false) }
    var showCoachCard by remember { mutableStateOf(false) }
    var showTopicStrip by remember { mutableStateOf(false) }
    var showNameGoalDialog by remember { mutableStateOf(false) }
    var showReportAiDialog by remember { mutableStateOf(false) }
    var reportSubmittedNotice by remember { mutableStateOf(false) }
    var manualGesture by remember { mutableStateOf<MotionPhotoGesture?>(null) }
    var youtubeQuery by remember { mutableStateOf<String?>(null) }
    var tappedWord by remember { mutableStateOf<String?>(null) }
    var stageMode by remember { mutableStateOf(TutorStageMode.PHOTO) }

    val hasAnyOverlayOpen = showHistoryOverlay || showScorecardOverlay || showTutorPicker ||
        showGestureBar || showEnginePicker || showStudioTray || showMissionGoalTray ||
        showPronunciationDetail || showNameGoalDialog || showReportAiDialog
    BackHandler(enabled = hasAnyOverlayOpen) {
        showHistoryOverlay = false
        showScorecardOverlay = false
        showTutorPicker = false
        showGestureBar = false
        showEnginePicker = false
        showStudioTray = false
        showMissionGoalTray = false
        showPronunciationDetail = false
        showNameGoalDialog = false
        showReportAiDialog = false
    }

    // Gentle Proactive API Check-In when user stays silent for 18s during an active topic conversation
    LaunchedEffect(
        state.proactiveModeEnabled,
        state.isThinking,
        state.turnsInCall,
        state.messages.size,
        voiceState.isListening,
        voiceState.isSpeaking,
        hasAnyOverlayOpen,
        showKeyboardDock
    ) {
        if (state.proactiveModeEnabled &&
            !state.isThinking &&
            !voiceState.isListening &&
            !voiceState.isSpeaking &&
            !hasAnyOverlayOpen &&
            !showKeyboardDock
        ) {
            delay(18_000L)
            onAutoProactiveCheckIn()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartListening()
        } else {
            showKeyboardDock = true
        }
    }

    // System Google Speech Dialog Fallback (works even when inline SpeechRecognizer service fails!)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotBlank()) {
                onSendTypedMessage(spoken)
            }
        }
    }

    fun launchSystemSpeechFallback() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langTag = if (state.koreanVoiceInputMode) "ko-KR" else state.selectedTutor.locale.toLanguageTag().ifBlank { "en-US" }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (state.koreanVoiceInputMode) "한국어로 질문하거나 표현을 물어보세요!" else "Speak naturally in English!"
            )
        }
        try {
            systemSpeechLauncher.launch(intent)
        } catch (_: Exception) {
            showKeyboardDock = true
        }
    }

    // Automatically open keyboard input bar if SpeechRecognizer reports an error so user is never stuck!
    LaunchedEffect(voiceState.errorTimestampMs) {
        if (voiceState.errorTimestampMs > 0L && voiceState.errorNotice != null) {
            showKeyboardDock = true
        }
    }

    val tutor = state.selectedTutor
    val activeGesture = manualGesture ?: MotionPhotoGesture.inferFromSpeech(
        text = state.liveSubtitleEn,
        isListening = voiceState.isListening,
        isSpeaking = voiceState.isSpeaking
    )
    // Dynamic AI / Zero-LLM Skill hints refreshed every turn!
    val guidedHints = state.dynamicSuggestedReplies
    val roleplayMissions = remember(state.activeTopicTag) { roleplayMissionsForTopic(state.activeTopicTag) }
    val isEmmaRealPortrait = tutor.portraitRes == R.drawable.img_real_tutor_emma_1795478674413
    val timerText = "%02d:%02d".format(state.callSeconds / 60, state.callSeconds % 60)
    val previousMsg = state.messages.dropLast(1).lastOrNull()
    val subtitleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.9f),
        offset = Offset(0f, 3f),
        blurRadius = 10f
    )
    // Track character span of each word in liveSubtitleEn so TTS onRangeStart highlights it live!
    val subtitleWordSpans = remember(state.liveSubtitleEn) {
        Regex("\\S+").findAll(state.liveSubtitleEn).map { m ->
            Triple(m.value, m.range.first, m.range.last + 1)
        }.toList()
    }
    val isKaraokeActiveForSubtitle = voiceState.isSpeaking &&
        voiceState.spokenCharStart >= 0 &&
        voiceState.activeSpokenText.trim().equals(state.liveSubtitleEn.trim(), ignoreCase = true)

    if (youtubeQuery != null) {
        YouTubeWordContextDialog(
            wordOrPhrase = youtubeQuery!!,
            onDismiss = { youtubeQuery = null }
        )
    }

    if (showReportAiDialog) {
        AlertDialog(
            onDismissRequest = { showReportAiDialog = false },
            title = { Text("부적절한 AI 응답 신고", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AI 튜터가 부적절하거나 부정확한 표현을 생성했나요? 신고하시면 현재 대화를 즉시 초기화하고 안전한 새 주제로 전환합니다.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.liveSubtitleEn.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "최근 AI 응답: \"${state.liveSubtitleEn.take(140)}\"",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    if (reportSubmittedNotice) {
                        Text(
                            text = "신고가 접수되었으며 안전 필터가 적용된 새 대화로 전환되었습니다.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AdventureEmerald
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportSubmittedNotice = true
                        onSelectTopic("수다", "뉴욕 타임스스퀘어")
                        android.widget.Toast.makeText(
                            context,
                            "부적절한 AI 응답 신고가 접수되어 안전한 새 대화로 초기화되었습니다.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        showReportAiDialog = false
                    },
                    modifier = Modifier.testTag("confirm_report_ai_response_button")
                ) {
                    Text("신고 후 대화 초기화")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportAiDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
    ) {
        // 1. 4-MODE AI TEACHER STAGE (SVG+CSS Lip-Sync Rig · 3D TalkingHead · Lottie · Real Photo)
        when (stageMode) {
            TutorStageMode.SVG_RIG -> {
                SvgCssLipSyncRigView(
                    tutorId = tutor.id,
                    tutorName = tutor.name,
                    learnerName = activePlayerName,
                    activeTopicTag = state.activeTopicTag,
                    activeGesture = activeGesture,
                    aiMood = state.aiMood,
                    liveThoughtKo = state.liveThoughtKo,
                    spokenSentence = state.liveSubtitleEn,
                    isSpeaking = voiceState.isSpeaking,
                    isListening = voiceState.isListening,
                    isThinking = state.isThinking,
                    speechRate = speechRate,
                    speechBeatIndex = voiceState.speechBeatIndex,
                    userRmsDb = voiceState.rmsDb,
                    modifier = Modifier.fillMaxSize()
                )
            }
            TutorStageMode.THREE_D -> {
                TalkingHead3DTeacherView(
                    tutorId = tutor.id,
                    tutorName = tutor.name,
                    spokenTextEn = state.liveSubtitleEn,
                    activeGesture = activeGesture,
                    isSpeaking = voiceState.isSpeaking,
                    isListening = voiceState.isListening,
                    isThinking = state.isThinking,
                    speechRate = speechRate,
                    userRmsDb = voiceState.rmsDb,
                    latestPronunciationScore = state.latestPronunciation?.overallScore,
                    modifier = Modifier.fillMaxSize()
                )
            }
            TutorStageMode.LOTTIE -> {
                LottieMotionTutorView(
                    tutorName = tutor.name,
                    activeGesture = activeGesture,
                    isSpeaking = voiceState.isSpeaking,
                    isListening = voiceState.isListening,
                    speechRate = speechRate,
                    userRmsDb = voiceState.rmsDb,
                    latestPronunciationScore = state.latestPronunciation?.overallScore,
                    modifier = Modifier.fillMaxSize()
                )
            }
            TutorStageMode.PHOTO -> {
                LiveMotionPhotoFriendView(
                    portraitRes = tutor.portraitRes,
                    contentDescription = "${tutor.name} 라이브 회화학습",
                    activeGesture = activeGesture,
                    activeTopicTag = state.activeTopicTag,
                    nanoBananaSceneBitmap = state.nanoBananaSceneBitmap,
                    nanoBananaBlinkBitmap = state.nanoBananaBlinkBitmap,
                    isSpeaking = voiceState.isSpeaking,
                    isListening = voiceState.isListening,
                    speechBeatIndex = voiceState.speechBeatIndex,
                    userRmsDb = voiceState.rmsDb,
                    extraCropZoom = if (isEmmaRealPortrait) 1.24f else 1.0f,
                    verticalOffsetFraction = if (isEmmaRealPortrait) 0.055f else 0f,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

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
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.weight(1f, fill = false)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "${tutor.name} · $timerText",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = StudioCobalt.copy(alpha = 0.85f),
                                    modifier = Modifier.testTag("ai_tutor_disclosure_badge")
                                ) {
                                    Text(
                                        text = "AI",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                if (voiceState.isSpeaking) {
                                    LiveMiniAudioEqualizer(
                                        beatIndex = voiceState.speechBeatIndex,
                                        color = AdventureEmerald
                                    )
                                }
                            }
                            Text(
                                text = state.sceneLocationLabelKo,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TrophyGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Clean 4-Button Top Action Bar (Zero right-edge overflow!)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FaceTalkIconBtn(
                        icon = Icons.Outlined.SwitchAccount,
                        desc = "선생님 변경",
                        active = showTutorPicker,
                        activeColor = StudioCobalt,
                        tag = "facetalk_switch_tutor",
                        onClick = {
                            showTutorPicker = !showTutorPicker
                            if (showTutorPicker) {
                                showStudioTray = false
                                showGestureBar = false
                            }
                        }
                    )
                    FaceTalkIconBtn(
                        icon = Icons.Outlined.ClosedCaption,
                        desc = "자막",
                        active = state.showKoreanTranslations,
                        activeColor = StudioCobalt,
                        tag = "video_toggle_subtitles",
                        onClick = onToggleKoreanSubtitles
                    )
                    FaceTalkIconBtn(
                        icon = Icons.Outlined.HeadsetMic,
                        desc = "연속회화",
                        active = handsFreeMode,
                        activeColor = SkyQuestBlue,
                        tag = "video_hands_free_switch",
                        onClick = { onSetHandsFree(!handsFreeMode) }
                    )
                    FaceTalkIconBtn(
                        icon = Icons.Outlined.Tune,
                        desc = "대화 설정",
                        active = showStudioTray || showGestureBar,
                        activeColor = StudioIris,
                        tag = "facetalk_studio_settings_btn",
                        onClick = {
                            showStudioTray = !showStudioTray
                            if (showStudioTray) {
                                showTutorPicker = false
                                showMissionGoalTray = false
                            } else {
                                showEnginePicker = false
                            }
                        }
                    )
                }
            }

            // Ultra-Slim 1-Row Compact Status Strip (Minimizes portrait occlusion!)
            val completedMissionCount = state.turnsInCall.coerceIn(0, roleplayMissions.size)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Compact Mission & Goal Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (showMissionGoalTray) StudioCobalt.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.44f),
                        border = BorderStroke(1.dp, if (showMissionGoalTray) GlacierCyan else Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                showMissionGoalTray = !showMissionGoalTray
                                if (showMissionGoalTray) showStudioTray = false
                            }
                            .testTag("toggle_mission_goal_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "미션 및 목표",
                                tint = GlacierCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "미션 $completedMissionCount/${roleplayMissions.size} · ${state.activeTopicTag}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Live Listening / Thinking / Scene Generating Status Pill (Only visible when active!)
                    if (state.isGeneratingScene) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = StudioCobalt.copy(alpha = 0.88f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 1.8.dp, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "배경 생성",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    } else if (voiceState.isListening || state.isThinking) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (voiceState.isListening) SpartaRed.copy(alpha = 0.90f) else StudioCoral.copy(alpha = 0.90f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (state.isThinking) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 1.8.dp, modifier = Modifier.size(11.dp))
                                } else {
                                    Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    text = if (state.isThinking) "응답 중" else if (state.koreanVoiceInputMode) "KR 듣는 중" else "EN 듣는 중",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        state.latestPronunciation?.let { report ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = AdventureEmerald.copy(alpha = 0.86f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { showPronunciationDetail = !showPronunciationDetail }
                            ) {
                                Text(
                                    text = "발음 ${report.overallScore}점",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Sleek Compact Learner Profile Pill (Replaces bulky 76x96dp PiP box!)
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.46f),
                    border = BorderStroke(
                        1.dp,
                        if (voiceState.isListening) SpartaRed else Color.White.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { showNameGoalDialog = true }
                        .testTag("edit_learner_name_pip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = "내 이름 설정",
                            tint = if (voiceState.isListening) SpartaRed else GlacierCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$activePlayerName · ${if (state.koreanVoiceInputMode) "KR" else "EN"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            if (state.liveVoiceCommandNotice.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoyalPurple.copy(alpha = 0.82f)
                ) {
                    Text(
                        text = state.liveVoiceCommandNotice.removePrefix("음성 설정 반영: "),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Expandable 3-Step Mission & Conversation Goal Tray (Hidden by default!)
            AnimatedVisibility(visible = showMissionGoalTray) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.68f),
                    border = BorderStroke(1.dp, GlacierCyan.copy(alpha = 0.42f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roleplay_mission_strip")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            roleplayMissions.forEachIndexed { idx, stepLabel ->
                                val done = state.turnsInCall > idx
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = if (done) GlacierCyan else Color.White.copy(alpha = 0.36f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${idx + 1}.$stepLabel",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (done) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (done) GlacierCyan else Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showNameGoalDialog = true }
                                .testTag("conversation_goal_pill"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "목표: ${state.conversationGoalKo}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "편집",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = GlacierCyan,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
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

            // Expandable Call Studio Controls Tray (Hidden by default so portrait stage stays clean!)
            AnimatedVisibility(visible = showStudioTray) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xE80D111A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("facetalk_studio_control_tray")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Stage Mode (실사 · 캐릭터 · 3D · 모션) + Speed Cycle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.testTag("tutor_stage_mode_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    TutorStageMode.entries.forEach { mode ->
                                        val active = stageMode == mode
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (active) StudioCobalt else Color.Transparent,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9.dp))
                                                .clickable { stageMode = mode }
                                                .testTag("tutor_stage_mode_${mode.name}")
                                        ) {
                                            Text(
                                                text = mode.labelKo,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (active) Color.White else Color.White.copy(alpha = 0.72f),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
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
                                Text(
                                    text = "속도 ${speechRate}x",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Row 2: AI Engine + Proactive Mode + First-Speak
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.70f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showEnginePicker = !showEnginePicker }
                                    .testTag("facetalk_engine_badge_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = "AI 엔진 전환",
                                        tint = GlacierCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${state.activeEngineBadgeKo} ▾",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFFBAE6FD),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (state.proactiveModeEnabled) StudioIris.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.10f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleProactiveMode() }
                                    .testTag("proactive_mode_toggle_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.Psychology, contentDescription = "능동 사고", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (state.proactiveModeEnabled) "능동 ON" else "능동 OFF",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioCobalt.copy(alpha = 0.88f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !state.isThinking && !voiceState.isListening) {
                                        showStudioTray = false
                                        onTriggerProactiveCheckIn()
                                    }
                                    .testTag("proactive_speak_now_btn")
                            ) {
                                Text(
                                    text = "먼저 말걸기",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = showEnginePicker) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                AiEngineMode.entries.forEach { mode ->
                                    val selected = mode == engineMode
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (selected) StudioCobalt else Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable {
                                                onSelectEngineMode(mode)
                                                showEnginePicker = false
                                            }
                                            .testTag("call_engine_mode_${mode.name}")
                                    ) {
                                        Text(
                                            text = if (selected) "✓ ${mode.labelKo}" else mode.labelKo,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Row 3: Quick Utility Tools (표정, 성적표, 대화기록, 신고)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FaceTalkIconBtn(
                                icon = Icons.Outlined.Mood,
                                desc = "표정",
                                active = showGestureBar,
                                activeColor = CoralOrange,
                                tag = "facetalk_motion_gestures",
                                onClick = {
                                    showGestureBar = !showGestureBar
                                    if (showGestureBar) showStudioTray = false
                                }
                            )
                            FaceTalkIconBtn(
                                icon = Icons.Outlined.EmojiEvents,
                                desc = "성적표",
                                active = showScorecardOverlay,
                                activeColor = StudioCoral,
                                tag = "facetalk_scorecard_btn",
                                onClick = {
                                    showScorecardOverlay = !showScorecardOverlay
                                    if (showScorecardOverlay) {
                                        showHistoryOverlay = false
                                        showStudioTray = false
                                    }
                                }
                            )
                            FaceTalkIconBtn(
                                icon = Icons.Outlined.History,
                                desc = "기록",
                                active = showHistoryOverlay,
                                activeColor = RoyalPurple,
                                tag = "facetalk_history_btn",
                                onClick = {
                                    showHistoryOverlay = !showHistoryOverlay
                                    if (showHistoryOverlay) {
                                        showScorecardOverlay = false
                                        showStudioTray = false
                                    }
                                }
                            )
                            FaceTalkIconBtn(
                                icon = Icons.Outlined.Flag,
                                desc = "AI 응답 신고",
                                active = showReportAiDialog,
                                activeColor = SpartaRed,
                                tag = "report_ai_response_button",
                                onClick = {
                                    reportSubmittedNotice = false
                                    showStudioTray = false
                                    showReportAiDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showNameGoalDialog) {
            var draftName by remember(activePlayerName) { mutableStateOf(activePlayerName) }
            var draftGoal by remember(state.conversationGoalKo) { mutableStateOf(state.conversationGoalKo) }
            AlertDialog(
                onDismissRequest = { showNameGoalDialog = false },
                title = {
                    Text(
                        text = "내 이름 · 대화 목표 설정",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "AI 캐릭터가 대화 중 이름을 부르고, 설정한 목표를 향해 능동적으로 대화를 이끌어 줍니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = draftName,
                            onValueChange = { draftName = it },
                            label = { Text("불러줄 이름 (예: 지민, Alex, 원삐)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("learner_name_input")
                        )
                        OutlinedTextField(
                            value = draftGoal,
                            onValueChange = { draftGoal = it },
                            label = { Text("상황별 대화 목표") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conversation_goal_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (draftName.isNotBlank() && draftName.trim() != activePlayerName) {
                                onUpdateLearnerName(draftName.trim())
                            }
                            if (draftGoal.isNotBlank() && draftGoal.trim() != state.conversationGoalKo) {
                                onUpdateConversationGoal(draftGoal.trim())
                            }
                            showNameGoalDialog = false
                        },
                        modifier = Modifier.testTag("save_name_goal_btn")
                    ) {
                        Text("적용")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNameGoalDialog = false }) {
                        Text("취소")
                    }
                }
            )
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
                        Icon(Icons.Outlined.Close, contentDescription = "닫기")
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

            // Actionable Voice Mic Error / Fallback Banner (never leaves user guessing why mic didn't respond!)
            val voiceErrorText = voiceState.errorNotice
            if (!voiceErrorText.isNullOrBlank() && !voiceState.isListening) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xE81E293B),
                    border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_error_fallback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = voiceErrorText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFFED7AA),
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SkyQuestBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { launchSystemSpeechFallback() }
                                .testTag("voice_system_dialog_fallback_btn")
                        ) {
                            Text(
                                text = "음성창",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Floating Tapped-Word Slow Shadowing Chip (Praktika / Cake Instant Tap-Word Learning!)
            if (tappedWord != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xE0111827),
                    border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.75f)),
                    modifier = Modifier.testTag("tapped_word_coach_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "\"$tappedWord\"",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = TrophyGold
                        )
                        Text(
                            text = "0.8x 천천히 듣기",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF93C5FD),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSpeakSlowWord(tappedWord!!) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "실전 예문 ▸",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { youtubeQuery = tappedWord }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "닫기",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { tappedWord = null }
                        )
                    }
                }
            }

            // Active 1-Tap Shadowing Drill Banner
            state.shadowingTargetEn?.let { shadowTarget ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xEC161922),
                    border = BorderStroke(1.5.dp, StudioCobalt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_shadowing_drill_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MicNone,
                            contentDescription = null,
                            tint = GlacierCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "따라 말하기 측정 중",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GlacierCyan
                            )
                            Text(
                                text = "\"$shadowTarget\"",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onCancelShadowingTarget,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "섀도잉 종료", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // MAIN INTERACTIVE KARAOKE + TAP-ANY-WORD TRANSPARENT CINEMA SUBTITLES
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("interactive_tap_word_subtitle")
                ) {
                    subtitleWordSpans.forEach { (rawWord, startIdx, endIdx) ->
                        val clean = rawWord.replace(Regex("[^A-Za-z'-]"), "")
                        val isSelected = clean.isNotBlank() && clean.equals(tappedWord, ignoreCase = true)
                        val isCurrentlySpoken = isKaraokeActiveForSubtitle &&
                            voiceState.spokenCharStart < endIdx &&
                            voiceState.spokenCharEnd > startIdx
                        Text(
                            text = rawWord,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = if (isCurrentlySpoken) 20.sp else 19.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = subtitleShadow
                            ),
                            color = when {
                                isCurrentlySpoken -> GlacierCyan
                                isSelected -> StudioCoral
                                else -> Color.White
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isCurrentlySpoken -> StudioCobalt.copy(alpha = 0.36f)
                                        isSelected -> StudioCoral.copy(alpha = 0.24f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    if (clean.isNotBlank()) {
                                        tappedWord = clean
                                        onSpeakSlowWord(clean)
                                    } else {
                                        onReplayTutorText(state.liveSubtitleEn)
                                    }
                                }
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
                if (state.showKoreanTranslations && state.liveSubtitleKo.isNotBlank()) {
                    Text(
                        text = state.liveSubtitleKo,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            shadow = subtitleShadow
                        ),
                        color = Color(0xFFBAE6FD),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { onReplayTutorText(state.liveSubtitleEn) }
                    )
                }
            }

            // Word-by-Word Pronunciation Heatmap Strip
            state.latestPronunciation?.let { report ->
                if (report.tokens.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("elsa_pronunciation_heatmap")
                    ) {
                        items(report.tokens) { wf ->
                            val (bg, txt) = when (wf.status) {
                                TokenStatus.EXACT -> StudioCobalt.copy(alpha = 0.36f) to GlacierCyan
                                TokenStatus.CLOSE -> StudioCoral.copy(alpha = 0.32f) to StudioCoral
                                TokenStatus.MISSED -> SpartaRed.copy(alpha = 0.42f) to Color(0xFFFCA5A5)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = bg,
                                border = BorderStroke(1.dp, txt.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSpeakSlowWord(wf.targetWord) }
                            ) {
                                Text(
                                    text = wf.targetWord,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = txt,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Sleek 1-Row Quick Utility Toggle Strip (Collapsed by default so portrait stays crystal clear!)
            val hasCoachFeedback = state.liveNaturalRephraseEn.isNotBlank() ||
                state.liveGrammarFixKo.isNotBlank() ||
                state.liveCoachingCommentKo.isNotBlank()
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item(key = "util_hints") {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (showGuidedHints) StudioCoral.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.46f),
                        border = BorderStroke(1.dp, if (showGuidedHints) StudioCoral else StudioCoral.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                showGuidedHints = !showGuidedHints
                                if (showGuidedHints) {
                                    showCoachCard = false
                                    showTopicStrip = false
                                }
                            }
                            .testTag("toggle_guided_hints_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Lightbulb, contentDescription = "추천 답변", tint = StudioCoral, modifier = Modifier.size(13.dp))
                            Text(
                                text = if (showGuidedHints) "추천 답변 ▾" else "추천 답변",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = Color.White
                            )
                        }
                    }
                }
                if (hasCoachFeedback) {
                    item(key = "util_coach") {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (showCoachCard) StudioCobalt.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.46f),
                            border = BorderStroke(
                                1.dp,
                                if (state.liveNaturalRephraseEn.isNotBlank()) TrophyGold.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    showCoachCard = !showCoachCard
                                    if (showCoachCard) {
                                        showGuidedHints = false
                                        showTopicStrip = false
                                    }
                                }
                                .testTag("toggle_coach_card_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.AutoFixHigh, contentDescription = "코칭·따라말하기", tint = TrophyGold, modifier = Modifier.size(13.dp))
                                Text(
                                    text = if (showCoachCard) "코칭·따라말하기 ▾" else "코칭·따라말하기",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                item(key = "util_topic") {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (showTopicStrip) AdventureEmerald.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.46f),
                        border = BorderStroke(1.dp, if (showTopicStrip) AdventureEmerald else Color.White.copy(alpha = 0.26f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                showTopicStrip = !showTopicStrip
                                if (showTopicStrip) {
                                    showGuidedHints = false
                                    showCoachCard = false
                                }
                            }
                            .testTag("toggle_topic_strip_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "상황 변경", tint = AdventureEmerald, modifier = Modifier.size(13.dp))
                            Text(
                                text = if (showTopicStrip) "상황 · ${state.activeTopicTag} ▾" else "상황 · ${state.activeTopicTag}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showGuidedHints) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guided_reply_hints_row")
                ) {
                    items(guidedHints, key = { it.english }) { hint ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.64f),
                            border = BorderStroke(1.dp, AdventureEmerald.copy(alpha = 0.60f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !state.isThinking) {
                                    showGuidedHints = false
                                    onSendTypedMessage(hint.english)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = hint.english,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = hint.korean,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFF93C5FD),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Inline Native Expression Upgrade + 1-Tap Shadowing Card
            AnimatedVisibility(visible = showCoachCard && hasCoachFeedback) {
                val shadowSentence = state.liveNaturalRephraseEn.ifBlank { state.liveSubtitleEn }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.64f),
                    border = BorderStroke(1.dp, if (state.liveNaturalRephraseEn.isNotBlank()) TrophyGold.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.24f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoFixHigh,
                            contentDescription = "코칭",
                            tint = StudioCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (state.liveNaturalRephraseEn.isNotBlank() && state.lastUserSpokenEn.isNotBlank()) {
                                Text(
                                    text = "내 말: ${state.lastUserSpokenEn}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.68f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = if (state.liveNaturalRephraseEn.isNotBlank()) {
                                    "원어민: ${state.liveNaturalRephraseEn}"
                                } else {
                                    state.liveCoachingCommentKo
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (state.liveNaturalRephraseEn.isNotBlank()) GlacierCyan else Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.liveGrammarFixKo.isNotBlank()) {
                                Text(
                                    text = "교정: ${state.liveGrammarFixKo}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = Color(0xFF93C5FD),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioCobalt.copy(alpha = 0.92f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        onStartShadowingTarget(shadowSentence)
                                    } else {
                                        onStartShadowingTarget(shadowSentence)
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                                .testTag("shadowing_target_btn")
                        ) {
                            Text(
                                text = "따라말하기",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                            )
                        }
                        IconButton(
                            onClick = { onReplayTutorText(shadowSentence) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "다시 듣기",
                                tint = SkyQuestBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                youtubeQuery = com.example.data.RealSceneClipFetcher.extractBestSearchPhrase(shadowSentence)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = "유튜브 실제 대사 찾기",
                                tint = SpartaRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // KR Mode Instant 1-Tap Korean Question Helpers
            AnimatedVisibility(visible = state.koreanVoiceInputMode) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("kr_quick_helper_row")
                ) {
                    items(koreanQuickHelpPrompts, key = { it }) { krPrompt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CoralOrange.copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, CoralOrange.copy(alpha = 0.72f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !state.isThinking) {
                                    onSendTypedMessage(krPrompt)
                                }
                        ) {
                            Text(
                                text = "KR · $krPrompt",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Expandable 8 Realistic Live Roleplay Scenario Chips
            AnimatedVisibility(visible = showTopicStrip) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roleplay_scenario_strip"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(faceTalkQuickChips, key = { it.labelKo }) { chip ->
                        val isSelectedScene = state.activeTopicTag.contains(chip.labelKo)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelectedScene) AdventureEmerald.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.56f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelectedScene) Color.White else Color.White.copy(alpha = 0.26f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showTopicStrip = false
                                    if (chip.isQuestion) onSendTypedMessage(chip.prompt)
                                    else onSelectTopic(chip.labelKo, chip.prompt)
                                }
                                .testTag("quick_kr_chip_${chip.labelKo}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = chip.icon,
                                    contentDescription = chip.labelKo,
                                    tint = if (isSelectedScene) Color.White else TrophyGold,
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
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "전송", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // FLOATING BOTTOM ACTION ROW (Minimalist Outlined Icons)
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
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = "한영 전환",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (state.koreanVoiceInputMode) "KR" else "EN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // Replay Audio Button
                FaceTalkBigCircleBtn(
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    desc = "다시 듣기",
                    bg = Color.Black.copy(alpha = 0.52f),
                    tag = "video_replay_subtitle",
                    onClick = { onReplayTutorText(state.liveSubtitleEn) }
                )

                // Center Large Mic Orb
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

                // Real YouTube Dialogue Search Button
                FaceTalkBigCircleBtn(
                    icon = Icons.Outlined.PlayCircle,
                    desc = "유튜브 대사 찾기",
                    bg = SpartaRed.copy(alpha = 0.86f),
                    tag = "facetalk_clip_btn",
                    onClick = {
                        val raw = state.liveNaturalRephraseEn.ifBlank { state.liveSubtitleEn }
                        youtubeQuery = com.example.data.RealSceneClipFetcher.extractBestSearchPhrase(raw)
                    }
                )

                // Keyboard Toggle Button
                FaceTalkBigCircleBtn(
                    icon = Icons.Outlined.Keyboard,
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
                            Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = Color.White)
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

        // Paid-App Live Call Performance Scorecard Overlay (Speak / Ringle / ELSA Session Report!)
        AnimatedVisibility(
            visible = showScorecardOverlay,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            val avgScore = state.pronunciationScoresInCall.average().takeIf { !it.isNaN() }?.toInt() ?: 0
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xF20B131E),
                border = BorderStroke(1.5.dp, TrophyGold.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .testTag("live_call_scorecard_overlay")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "실시간 회화 성적표 · $activePlayerName",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TrophyGold
                            )
                            Text(
                                text = "엔진: ${state.activeEngineBadgeKo} · 주제: ${state.sceneLocationLabelKo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        IconButton(onClick = { showScorecardOverlay = false }) {
                            Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("통화 시간", timerText, SkyQuestBlue),
                            Triple("발화 턴", "${state.turnsInCall}회", AdventureEmerald),
                            Triple("평균 발음", if (avgScore > 0) "${avgScore}점" else "측정 대기", TrophyGold)
                        ).forEach { (label, value, color) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = color.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                                    Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = color)
                                }
                            }
                        }
                    }

                    Text(
                        text = "오늘 수집한 원어민 업그레이드 표현 (${state.collectedUpgrades.size}개)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    if (state.collectedUpgrades.isEmpty()) {
                        Text(
                            text = "마이크로 한 문장만 말하거나 추천 힌트를 탭하면 원어민 업그레이드 표현이 자동 기록됩니다!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.collectedUpgrades) { (spoken, upgraded) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "내 말: $spoken",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "원어민: $upgraded",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = GlacierCyan
                                            )
                                        }
                                        IconButton(onClick = { onReplayTutorText(upgraded) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "듣기", tint = SkyQuestBlue, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
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
                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "듣기", tint = SkyQuestBlue, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onYouTube, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.PlayCircle, contentDescription = "클립", tint = SpartaRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveMiniAudioEqualizer(
    beatIndex: Int,
    color: Color
) {
    val infinite = rememberInfiniteTransition(label = "liveEq")
    val bar1 by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(240, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val bar2 by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(190, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val bar3 by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(270, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )
    val beatBoost = if (beatIndex % 2 == 0) 1.0f else 0.82f
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(12.dp)
    ) {
        listOf(bar1 * beatBoost, bar2, bar3 * beatBoost).forEach { level ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((12f * level.coerceIn(0.25f, 1f)).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
