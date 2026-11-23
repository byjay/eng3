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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.HumanTutorRepository
import com.example.engine.VoiceEngineState
import com.example.model.AiEngineMode
import com.example.model.AiHumanTutor
import com.example.model.TutorChatMessage
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.PronunciationReportCard
import com.example.ui.components.VoiceWaveformOrb3D
import com.example.ui.components.YouTubeWordContextDialog
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow
import com.example.viewmodel.TutorCallState

private data class QuickQuestionChip(
    val labelKo: String,
    val promptText: String,
    val isVoiceCommand: Boolean = false
)

private val quickKoreanQuestions = listOf(
    QuickQuestionChip("마트: 이거 빼고 현금!", "마트에서 이거 빼고 현금으로 할게요 영어로 알려줘"),
    QuickQuestionChip("자기소개 3문장 코칭", "고등학생 자기소개 3문장 영어로 자연스럽게 알려줘"),
    QuickQuestionChip("음성명령: 천천히", "천천히 말해줘", isVoiceCommand = true),
    QuickQuestionChip("음성명령: 자막 토글", "자막 꺼줘", isVoiceCommand = true),
    QuickQuestionChip("카페 얼음 적게", "카페에서 얼음 적게 넣어주세요 영어로 어떻게 말해?"),
    QuickQuestionChip("길 물어보기", "지하철역으로 가는 가장 빠른 길 영어로 알려줘")
)

private data class QuickTopicChip(
    val labelKo: String,
    val starterEn: String
)

private val videoTopics = listOf(
    QuickTopicChip("오늘 하루 수다", "Hey! How's your day going so far? Anything fun happen at school today?"),
    QuickTopicChip("마트 계산대", "Hi there! Are you paying by card or cash today? Anything else I can help with?"),
    QuickTopicChip("해외 카페 주문", "Welcome in! What can I get started for you—iced latte or something sweet?"),
    QuickTopicChip("공항 입국 심사", "Good afternoon! What's the purpose of your visit, and how long will you stay?"),
    QuickTopicChip("자기소개 연습", "Nice to meet you! Tell me a little bit about yourself in one to three sentences!")
)

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
    var showTranscriptDrawer by remember { mutableStateOf(false) }
    var youtubeQuery by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartListening()
    }

    val tutor = state.selectedTutor
    val tutorAccent = Color(tutor.themeColorHex)

    val pulseTransition = rememberInfiniteTransition(label = "videoSpeakPulse")
    val liveScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portraitScale"
    )
    val liveDotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )

    val mins = state.callSeconds / 60
    val secs = state.callSeconds % 60
    val timerFormatted = "%02d:%02d".format(mins, secs)

    if (youtubeQuery != null) {
        YouTubeWordContextDialog(
            wordOrPhrase = youtubeQuery!!,
            onDismiss = { youtubeQuery = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scrollable Cinematic Video Call Stage + Real-Time Coaching Guide
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 10 Real Human Tutor Switcher Strip
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HumanTutorRepository.tutors, key = { it.id }) { item ->
                        val selected = item.id == tutor.id
                        val accent = Color(item.themeColorHex)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) accent else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) Color.White else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .testTag("tutor_chip_${item.id}")
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectTutor(item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = item.portraitRes),
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color.White, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.accentLabel.take(6),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (selected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. REAL FULL-SCREEN-STYLE VIDEO CALL STAGE WITH OVERLAID SUBTITLES
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(355.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(2.5.dp, if (voiceState.isSpeaking) AdventureEmerald else tutorAccent, RoundedCornerShape(26.dp))
                        .background(Color(0xFF0A141E))
                ) {
                    // Full-Bleed Tutor Video Frame
                    Image(
                        painter = painterResource(id = tutor.portraitRes),
                        contentDescription = "${tutor.name} 실시간 영상통화 화면",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(if (voiceState.isSpeaking) liveScale else 1.0f)
                    )

                    // Top & Bottom Cinematic Gradient Scrims for high-contrast subtitles
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Black.copy(alpha = 0.58f),
                                        0.30f to Color.Transparent,
                                        0.55f to Color.Black.copy(alpha = 0.35f),
                                        1.0f to Color.Black.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )

                    // TOP OVERLAY BAR: LIVE indicator + Timer + Engine Badge + Subtitle Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SpartaRed.copy(alpha = 0.92f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = liveDotAlpha))
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "LIVE $timerFormatted",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.55f)
                            ) {
                                Text(
                                    text = "${tutor.name} · ${engineMode.labelKo.take(10)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = if (state.showKoreanTranslations) AdventureEmerald else Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onToggleKoreanSubtitles)
                                    .testTag("video_toggle_subtitles")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = "한글 자막 켜기/끄기",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = SkyQuestBlue.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onReplayTutorText(state.liveSubtitleEn) }
                                    .testTag("video_replay_subtitle")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "방금 선생님 말 다시 듣기",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Speaking / Thinking Live Badge (Top-Right Below Header)
                    if (voiceState.isSpeaking || state.isThinking || voiceState.isListening) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                voiceState.isListening -> SpartaRed.copy(alpha = 0.9f)
                                state.isThinking -> TrophyGold.copy(alpha = 0.92f)
                                else -> AdventureEmerald.copy(alpha = 0.92f)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 56.dp, end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.isThinking) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF3E2C00),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = when {
                                        voiceState.isListening -> if (state.koreanVoiceInputMode) "한국어 질문 듣는 중..." else "영어 발음 듣는 중..."
                                        state.isThinking -> "선생님 생각 중..."
                                        else -> "${tutor.name} 말하는 중"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (state.isThinking) Color(0xFF3E2C00) else Color.White
                                )
                            }
                        }
                    }

                    // BOTTOM CINEMA LIVE SUBTITLE OVERLAY (English + Korean + Partial Speech)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (voiceState.isListening && voiceState.partialTranscript.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SpartaRed.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "내 말: \"${voiceState.partialTranscript}\"",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Cinema English Subtitle
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.68f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = state.liveSubtitleEn,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                if (state.showKoreanTranslations && state.liveSubtitleKo.isNotBlank()) {
                                    Text(
                                        text = state.liveSubtitleKo,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = TrophyGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Voice Command Confirmation Notice (if triggered by voice!)
            if (state.liveVoiceCommandNotice.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RoyalPurple.copy(alpha = 0.16f),
                        border = BorderStroke(1.5.dp, RoyalPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = RoyalPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = state.liveVoiceCommandNotice,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = RoyalPurple
                            )
                        }
                    }
                }
            }

            // 4. REAL-TIME TEACHER COACHING & INSTANT CORRECTION CARD ("말하기 지적 · 바로 잡기 코칭가이드")
            item {
                Elevated3DCard(accentShadow = SkyQuestBlueShadow.copy(alpha = 0.35f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = SkyQuestBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "선생님 실시간 코멘트 · 바로 잡기 코칭가이드",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = SkyQuestBlueShadow
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AdventureEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "턴 ${state.turnsInCall}회",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AdventureEmeraldShadow,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = state.liveCoachingCommentKo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (state.liveNaturalRephraseEn.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = TrophyGold.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, TrophyGoldShadow.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "원어민 바로 잡기 추천 문장 (따라 말해보세요!)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TrophyGoldShadow
                                    )
                                    Text(
                                        text = "\"${state.liveNaturalRephraseEn}\"",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SkyQuestBlue,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onReplayTutorText(state.liveNaturalRephraseEn) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "추천 문장 듣기",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "발음 듣기",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SpartaRed,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { youtubeQuery = state.liveNaturalRephraseEn }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircle,
                                                    contentDescription = "유튜브 회화 클립",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "유튜브 실전 클립",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Real-Time Phonetic Pronunciation Report Card (if available)
            state.latestPronunciation?.let { report ->
                item {
                    PronunciationReportCard(
                        report = report,
                        onCompareNativeClick = {
                            onReplayTutorText(
                                state.liveNaturalRephraseEn.ifBlank { state.liveSubtitleEn }
                            )
                        }
                    )
                }
            }

            // 6. Quick Korean Expression Questions & Voice Settings Control Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "한국어로 영어 질문 & 말로 설정 변경 (원탭 체험)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickKoreanQuestions) { chip ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (chip.isVoiceCommand) RoyalPurple.copy(alpha = 0.14f) else CoralOrange.copy(alpha = 0.14f),
                                border = BorderStroke(
                                    1.dp,
                                    if (chip.isVoiceCommand) RoyalPurple else CoralOrange
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSendTypedMessage(chip.promptText) }
                                    .testTag("quick_kr_chip_${chip.labelKo}")
                            ) {
                                Text(
                                    text = chip.labelKo,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (chip.isVoiceCommand) RoyalPurple else CoralOrange,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Quick Video Roleplay Topic Starters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(videoTopics) { topic ->
                        val active = state.activeTopicTag == topic.labelKo
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) TrophyGold else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectTopic(topic.labelKo, topic.starterEn) }
                        ) {
                            Text(
                                text = topic.labelKo,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (active) Color(0xFF3E2C00) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // 8. Speed & Hands-Free Call Controls Bar
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HeadsetMic,
                                contentDescription = "연속 친구 통화",
                                tint = AdventureEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "연속 통화",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.width(4.dp))
                            Switch(
                                checked = handsFreeMode,
                                onCheckedChange = onSetHandsFree,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AdventureEmerald
                                ),
                                modifier = Modifier.testTag("video_hands_free_switch")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.8f to "0.8x", 1.0f to "1.0x", 1.15f to "1.15x").forEach { (rate, label) ->
                                val selected = kotlin.math.abs(speechRate - rate) < 0.05f
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) SkyQuestBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSetSpeechRate(rate) }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 9. Collapsible Transcript History Drawer
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showTranscriptDrawer = !showTranscriptDrawer }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "전체 통화 자막 기록 (${state.messages.size}개)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Icon(
                                imageVector = if (showTranscriptDrawer) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "자막 기록 열기/닫기"
                            )
                        }

                        AnimatedVisibility(visible = showTranscriptDrawer) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.messages.takeLast(12).forEach { msg ->
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

        // BOTTOM LIVE VIDEO CALL DOCK (EN/KR Voice Mode + 3D Mic Orb + Instant Text Ask)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // EN Speaking vs KR Question/Voice-Control Toggle Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val krMode = state.koreanVoiceInputMode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!krMode) AdventureEmerald else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { if (krMode) onToggleKoreanVoiceMode() }
                            .testTag("video_mode_en_button")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                            Text(
                                text = "EN 영어 친구 통화",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (!krMode) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (krMode) CoralOrange else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { if (!krMode) onToggleKoreanVoiceMode() }
                            .testTag("video_mode_kr_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = if (krMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "KR 한국어 질문·설정",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (krMode) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Mic Orb + Input Field + Send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceWaveformOrb3D(
                        isListening = voiceState.isListening,
                        isSpeaking = voiceState.isSpeaking,
                        rmsDb = voiceState.rmsDb,
                        tag = "video_call_mic_orb",
                        modifier = Modifier.size(64.dp),
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

                    OutlinedTextField(
                        value = typedMessage,
                        onValueChange = { typedMessage = it },
                        placeholder = {
                            Text(
                                text = if (state.koreanVoiceInputMode) {
                                    "한국어로 표현 질문·음성설정..."
                                } else {
                                    "영어나 한국어로 질문 입력..."
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("video_call_text_input")
                    )

                    Surface(
                        shape = CircleShape,
                        color = AdventureEmerald,
                        modifier = Modifier
                            .size(48.dp)
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "메시지 전송",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
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
        color = if (msg.fromTutor) MaterialTheme.colorScheme.surfaceVariant else AdventureEmerald.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (msg.fromTutor) tutorName else "나 ($playerName)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (msg.fromTutor) SkyQuestBlueShadow else AdventureEmeraldShadow
                )
                if (msg.fromTutor && !msg.isStreaming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onReplay, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "다시 듣기",
                                tint = SkyQuestBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onYouTube, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "유튜브 클립",
                                tint = SpartaRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = msg.textEn,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!msg.translationKo.isNullOrBlank()) {
                Text(
                    text = msg.translationKo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
