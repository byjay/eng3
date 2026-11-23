package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.onGoogleSignInClicked
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.Tactile3DButton
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.TrophyGoldShadow

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFE3F6FF),
                        MaterialTheme.colorScheme.background,
                        Color(0xFFEDFFE0)
                    )
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 3D World Hero Banner with overlapping Mascot Medallion
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(R.drawable.img_world_map_hero_1795412460420),
                    contentDescription = "3D 영어 모험 월드맵",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .padding(bottom = 36.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(26.dp))
                )
                Image(
                    painter = painterResource(R.drawable.img_app_icon_mascot_1795412462209),
                    contentDescription = "SpeakQuest 3D 마스코트",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "SpeakQuest 3D",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "게임처럼 즐기는 고1~3 수능 30단어 스파르타 &\n100대 여행 회화 · 10인 원어민 AI 선생님 실시간 대화",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Elevated3DCard(accentShadow = Color(0xFFCDE3D2)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureBullet(
                        icon = Icons.Default.LocalFireDepartment,
                        tint = SpartaRed,
                        text = "고1~3 수능 영단어 매일 30단어+ 오답 무한 스파르타 & 발음 분석"
                    )
                    FeatureBullet(
                        icon = Icons.Default.FlightTakeoff,
                        tint = SkyQuestBlue,
                        text = "여행자 100가지 상황 × 30문장(3,000문장) & 돌발 변수 실전 대응"
                    )
                    FeatureBullet(
                        icon = Icons.Default.RecordVoiceOver,
                        tint = AdventureEmerald,
                        text = "실제 사람 캐릭터 AI 선생님 10명과 친구처럼 실시간 음성 대화"
                    )
                    FeatureBullet(
                        icon = Icons.Default.AutoAwesome,
                        tint = TrophyGoldShadow,
                        text = "내장 Gemini AI + Groq 등 10슬롯 API 순환 + 100% 오프라인 지원"
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            if (errorMessage != null) {
                Surface(
                    color = SpartaRed.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpartaRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(color = AdventureEmerald)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Google 계정과 학습 기록을 연결하고 있습니다...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Tactile3DButton(
                    text = "Google 로그인하고 모험 시작하기",
                    icon = Icons.Default.Login,
                    containerColor = AdventureEmerald,
                    shadowColor = AdventureEmeraldShadow,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "google_sign_in_button",
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "학습 진도, 스트릭, 코인, 수집 아이템은 클라우드에 안전하게 동기화됩니다.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
