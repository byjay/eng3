package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightSurface
import com.example.ui.theme.StudioCobalt

/**
 * Self-contained Native Studio Character Stage:
 * Replaced the discontinued external Ready Player Me `.glb` / third-party CDN WebView loader
 * with an always-available, zero-latency local Lottie + Compose animated tutor stage.
 */
@Composable
fun TalkingHead3DTeacherView(
    tutorId: String,
    tutorName: String,
    spokenTextEn: String,
    activeGesture: MotionPhotoGesture,
    isSpeaking: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
    speechRate: Float,
    userRmsDb: Float,
    latestPronunciationScore: Int?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
            .testTag("talkinghead_3d_stage")
    ) {
        LottieMotionTutorView(
            tutorName = tutorName,
            activeGesture = activeGesture,
            isSpeaking = isSpeaking,
            isListening = isListening,
            speechRate = speechRate,
            userRmsDb = userRmsDb,
            latestPronunciationScore = latestPronunciationScore,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = NightSurface.copy(alpha = 0.84f),
            border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.55f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 148.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = StudioCobalt,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "로컬 스튜디오 모션 AI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
