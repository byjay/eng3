package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.R
import com.example.model.AiHumanTutor
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris

data class TravelLocalHost(
    val id: String,
    val name: String,
    val roleKo: String,
    @DrawableRes val portraitRes: Int,
    val accentColorHex: Long
)

data class TravelStreetLocation(
    val zoneId: Int,
    val lat: Double,
    val lng: Double,
    val heading: Int,
    val pitch: Int,
    val placeNameKo: String,
    val cityBadgeEn: String,
    @DrawableRes val sceneBgRes: Int,
    val defaultHost: TravelLocalHost
)

object TravelStreetDirectory {

    val diverseHosts: List<TravelLocalHost> = listOf(
        TravelLocalHost("host_maya", "Maya", "국제공항 안내관", R.drawable.img_real_tutor_emma_1795478674413, 0xFF0EA5E9),
        TravelLocalHost("host_liam", "Liam", "뉴욕 시티 가이드", R.drawable.img_real_tutor_liam_clean_1795478701897, 0xFF2563EB),
        TravelLocalHost("host_sophia", "Sophia", "5성급 호텔 컨시어지", R.drawable.img_real_tutor_sophia_clean_1795478702156, 0xFF7C3AED),
        TravelLocalHost("host_noah", "Noah", "소호 카페 바리스타", R.drawable.img_real_tutor_liam_1795478674528, 0xFFEA580C),
        TravelLocalHost("host_chloe", "Chloe", "런던 스타일리스트", R.drawable.img_real_tutor_sophia_1795478674364, 0xFFE11D48),
        TravelLocalHost("host_lucas", "Lucas", "파리 현지 큐레이터", R.drawable.img_tutor_liam_1795412460241, 0xFF0D9488),
        TravelLocalHost("host_olivia", "Dr. Olivia", "현지 클리닉 전문의", R.drawable.img_tutor_sophia_1795412462611, 0xFF16A34A),
        TravelLocalHost("host_emma", "Emma", "여행 회화 파트너", R.drawable.img_tutor_emma_1795412460310, 0xFF4F6EF7)
    )

    val locations: List<TravelStreetLocation> = listOf(
        TravelStreetLocation(
            zoneId = 1,
            lat = 40.6438, lng = -73.7820, heading = 120, pitch = 4,
            placeNameKo = "뉴욕 JFK 국제공항 터미널 4",
            cityBadgeEn = "JFK Terminal 4, NY",
            sceneBgRes = R.drawable.img_scene_airport_terminal_1795687513640,
            defaultHost = diverseHosts[0] // Maya · 국제공항 안내관
        ),
        TravelStreetLocation(
            zoneId = 2,
            lat = 51.4713, lng = -0.4880, heading = 95, pitch = 2,
            placeNameKo = "런던 히드로 공항 · 국제선 기내",
            cityBadgeEn = "Heathrow T5, London",
            sceneBgRes = R.drawable.img_mnemonic_world_travel_1795492661880,
            defaultHost = diverseHosts[4].copy(roleKo = "국제선 수석 승무원") // Chloe
        ),
        TravelStreetLocation(
            zoneId = 3,
            lat = 40.7580, lng = -73.9855, heading = 195, pitch = 8,
            placeNameKo = "뉴욕 타임스스퀘어 · 대중교통 허브",
            cityBadgeEn = "Times Square, NYC",
            sceneBgRes = R.drawable.img_friend_nyc_no_mic_1795422201093,
            defaultHost = diverseHosts[1] // Liam · 뉴욕 시티 가이드
        ),
        TravelStreetLocation(
            zoneId = 4,
            lat = 40.7644, lng = -73.9745, heading = 240, pitch = 6,
            placeNameKo = "뉴욕 5번가 럭셔리 호텔 로비",
            cityBadgeEn = "5th Ave Plaza, NYC",
            sceneBgRes = R.drawable.img_scene_luxury_hotel_1795687515906,
            defaultHost = diverseHosts[2] // Sophia · 5성급 호텔 컨시어지
        ),
        TravelStreetLocation(
            zoneId = 5,
            lat = 40.7233, lng = -74.0005, heading = 65, pitch = 3,
            placeNameKo = "소호 프린스 스트리트 카페 · 비스트로",
            cityBadgeEn = "SoHo Prince St, NYC",
            sceneBgRes = R.drawable.img_scene_paris_cafe_1795687515140,
            defaultHost = diverseHosts[3] // Noah · 소호 카페 바리스타
        ),
        TravelStreetLocation(
            zoneId = 6,
            lat = 51.5152, lng = -0.1419, heading = 80, pitch = 4,
            placeNameKo = "런던 옥스퍼드 스트리트 쇼핑 거리",
            cityBadgeEn = "Oxford St, London",
            sceneBgRes = R.drawable.img_scene_london_shopping_1795687513464,
            defaultHost = diverseHosts[7].copy(roleKo = "옥스퍼드 스타일리스트") // Emma
        ),
        TravelStreetLocation(
            zoneId = 7,
            lat = 48.8708, lng = 2.3048, heading = 300, pitch = 5,
            placeNameKo = "파리 샹젤리제 · 노천 테라스 거리",
            cityBadgeEn = "Champs-Élysées, Paris",
            sceneBgRes = R.drawable.img_scene_paris_cafe_1795687515140,
            defaultHost = diverseHosts[5] // Lucas · 파리 현지 큐레이터
        ),
        TravelStreetLocation(
            zoneId = 8,
            lat = 37.7897, lng = -122.4014, heading = 215, pitch = 4,
            placeNameKo = "샌프란시스코 마켓 스트리트 · 응급 센터",
            cityBadgeEn = "Market St, San Francisco",
            sceneBgRes = R.drawable.img_mnemonic_emotion_scene_1795492662342,
            defaultHost = diverseHosts[6] // Dr. Olivia · 현지 클리닉 전문의
        ),
        TravelStreetLocation(
            zoneId = 9,
            lat = 35.6595, lng = 139.7004, heading = 15, pitch = 6,
            placeNameKo = "도쿄 시부야 스크램블 교차로",
            cityBadgeEn = "Shibuya Crossing, Tokyo",
            sceneBgRes = R.drawable.img_scene_tokyo_shibuya_1795687516377,
            defaultHost = diverseHosts[1].copy(name = "Kenji", roleKo = "시부야 로컬 가이드")
        ),
        TravelStreetLocation(
            zoneId = 10,
            lat = 1.2834, lng = 103.8607, heading = 145, pitch = 5,
            placeNameKo = "해변 리조트 · 마리나 베이 산책로",
            cityBadgeEn = "Marina Bay, Singapore",
            sceneBgRes = R.drawable.img_friend_guam_beach_no_mic_1795422199110,
            defaultHost = diverseHosts[4].copy(name = "Hannah", roleKo = "리조트 액티비티 호스트")
        )
    )

    fun locationForZone(zoneId: Int): TravelStreetLocation =
        locations.firstOrNull { it.zoneId == zoneId } ?: locations.first()
}

/**
 * High-speed 60fps native Compose Travel Scene Stage:
 * - Zero WebViews on default load for instant (<16ms) zone switching and snappy clicks.
 * - Theme-matched photorealistic scenic background + diverse local character persona per zone.
 * - 1-tap character switcher ("인물 변경") and optional live 360° Street View launch.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleStreetViewStage(
    zoneId: Int,
    scenarioTitleKo: String,
    activePlayerName: String,
    tutor: AiHumanTutor,
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    rmsDb: Float,
    speechRate: Float,
    speechBeatIndex: Int,
    proactiveLineEn: String,
    proactiveLineKo: String,
    proactiveThoughtKo: String,
    proactiveMood: String,
    proactiveEngineBadge: String,
    showKorean: Boolean,
    onTriggerProactiveSpeak: () -> Unit,
    onReplayProactiveLine: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val location = remember(zoneId) { TravelStreetDirectory.locationForZone(zoneId) }
    var hostOffset by remember(zoneId) { mutableIntStateOf(0) }
    var showLiveWebStreetView by remember { mutableStateOf(false) }

    val activeHost = remember(location, hostOffset) {
        if (hostOffset == 0) {
            location.defaultHost
        } else {
            val all = TravelStreetDirectory.diverseHosts
            all[(zoneId + hostOffset) % all.size]
        }
    }

    val rawMapsKey = BuildConfig.GOOGLE_MAPS_API_KEY.trim()
    val hasValidMapsKey = rawMapsKey.isNotBlank() &&
        !rawMapsKey.equals("YOUR_GOOGLE_MAPS_API_KEY", ignoreCase = true)

    val hostAccent = remember(activeHost.accentColorHex) { Color(activeHost.accentColorHex) }

    // Lightweight GPU-only speaking/listening pulse (zero recomposition lag)
    val pulseTransition = rememberInfiniteTransition(label = "hostVoicePulse")
    val speakPulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking || isListening) 1.04f else 1.008f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 420 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hostScale"
    )

    val htmlContent = remember(location, hasValidMapsKey, showLiveWebStreetView) {
        if (hasValidMapsKey && showLiveWebStreetView) {
            buildGoogleMapsStreetViewHtml(
                apiKey = rawMapsKey,
                location = location,
                streetViewMode = true
            )
        } else {
            ""
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, hostAccent.copy(alpha = 0.28f)),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("travel_google_street_stage")
    ) {
        Column {
            // 1. Instant Native GPU Scenic Backdrop + Diverse Local Host Portrait Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(276.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF0F172A))
            ) {
                if (showLiveWebStreetView && hasValidMapsKey) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                webViewClient = WebViewClient()
                            }
                        },
                        update = { webView ->
                            val tagKey = "gmaps_${location.zoneId}"
                            if (webView.tag != tagKey) {
                                webView.tag = tagKey
                                webView.loadDataWithBaseURL(
                                    "https://maps.googleapis.com/",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        }
                    )
                } else {
                    Crossfade(
                        targetState = location.sceneBgRes,
                        animationSpec = tween(durationMillis = 180),
                        label = "travelSceneBackdrop"
                    ) { bgRes ->
                        Image(
                            painter = painterResource(bgRes),
                            contentDescription = location.placeNameKo,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Soft atmospheric top & bottom readability scrims
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F172A).copy(alpha = 0.56f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.80f))
                            )
                        )
                )

                // Top Row: Location Chip + Character Switch + Optional 360° Street View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, hostAccent.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Public,
                                contentDescription = "위치",
                                tint = hostAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = location.placeNameKo,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        // Instant 1-tap local host diversity switcher
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = hostAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { hostOffset += 1 }
                                .testTag("travel_cycle_host_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SwitchAccount,
                                    contentDescription = "인물 변경",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "인물 변경",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        if (hasValidMapsKey) {
                            Surface(
                                shape = CircleShape,
                                color = if (showLiveWebStreetView) StudioCobalt else Color.White.copy(alpha = 0.92f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { showLiveWebStreetView = !showLiveWebStreetView }
                                    .testTag("travel_toggle_street_map")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (showLiveWebStreetView) Icons.Outlined.PhotoCamera else Icons.Outlined.Explore,
                                        contentDescription = "360° 거리뷰 전환",
                                        tint = if (showLiveWebStreetView) Color.White else StudioCobalt,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val panoUri = Uri.parse(
                                        "https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${location.lat},${location.lng}&heading=${location.heading}&pitch=${location.pitch}"
                                    )
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, panoUri))
                                    }
                                }
                                .testTag("travel_open_external_streetview")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "구글 지도 거리뷰 열기",
                                    tint = StudioCobalt,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom-Right: Instant Native GPU Character Portrait Card (Zero WebView!)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .width(126.dp)
                        .height(172.dp)
                        .graphicsLayer {
                            scaleX = speakPulse
                            scaleY = speakPulse
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = if (isSpeaking || isListening) 2.5.dp else 2.dp,
                            color = when {
                                isListening -> StudioCoral
                                isSpeaking -> GlacierCyan
                                else -> Color.White.copy(alpha = 0.92f)
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { hostOffset += 1 }
                        .testTag("travel_host_portrait_card")
                ) {
                    Crossfade(
                        targetState = activeHost.portraitRes,
                        animationSpec = tween(durationMillis = 160),
                        label = "hostPortraitCrossfade"
                    ) { portraitRes ->
                        Image(
                            painter = painterResource(portraitRes),
                            contentDescription = "${activeHost.name} ${activeHost.roleKo}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.88f))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(
                                text = activeHost.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeHost.roleKo,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Left-Bottom: Live Status Badge + Quick Proactive & Mic Buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp, end = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isListening -> StudioCoral
                            isSpeaking -> GlacierCyan
                            isThinking -> StudioIris
                            else -> Color(0xFF0F172A).copy(alpha = 0.78f)
                        }
                    ) {
                        Text(
                            text = when {
                                isListening -> "듣는 중 · 발음 분석"
                                isSpeaking -> "${activeHost.name} 말하는 중"
                                isThinking -> "현지 대화 생성 중..."
                                else -> location.cityBadgeEn
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioCobalt,
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isThinking, onClick = onTriggerProactiveSpeak)
                                .testTag("travel_proactive_speak_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isThinking) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = "먼저 말걸기",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "먼저 말걸기",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isListening) RoseCrimson else StudioCoral,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onMicClick)
                                .testTag("travel_street_stage_mic")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isListening) Icons.Outlined.Stop else Icons.Outlined.MicNone,
                                    contentDescription = if (isListening) "듣기 중지" else "거리에서 말하기",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Proactive Companion Dialogue Strip (personalized with diverse local host name!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = hostAccent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${activeHost.name} (${activeHost.roleKo}) · ${activePlayerName}님",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = hostAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (proactiveEngineBadge.isNotBlank()) {
                            Text(
                                text = proactiveEngineBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = GlacierCyan.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onReplayProactiveLine)
                            .testTag("travel_proactive_replay")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "다시 듣기",
                                tint = GlacierCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = proactiveLineEn.ifBlank {
                        "Hi $activePlayerName! I'm ${activeHost.name} here at ${location.cityBadgeEn}. Ready to practice '$scenarioTitleKo'?"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (showKorean) {
                    Text(
                        text = proactiveLineKo.ifBlank {
                            "${activePlayerName}님, ${location.placeNameKo}의 ${activeHost.name}(${activeHost.roleKo})예요! '$scenarioTitleKo' 상황을 함께 연습해 볼까요?"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (proactiveThoughtKo.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioIris.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "현지 코치 팁: $proactiveThoughtKo",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioIris,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun buildGoogleMapsStreetViewHtml(
    apiKey: String,
    location: TravelStreetLocation,
    streetViewMode: Boolean
): String {
    val safeKey = Uri.encode(apiKey)
    val initScript = if (streetViewMode) {
        """
        const pos = { lat: ${location.lat}, lng: ${location.lng} };
        const svService = new google.maps.StreetViewService();
        const pano = new google.maps.StreetViewPanorama(document.getElementById('stage'), {
          position: pos,
          pov: { heading: ${location.heading}, pitch: ${location.pitch} },
          zoom: 1,
          addressControl: false,
          fullscreenControl: false,
          motionTracking: false,
          linksControl: true,
          panControl: false
        });
        svService.getPanorama({ location: pos, radius: 120 }, (data, status) => {
          if (status === 'OK' && data && data.location && data.location.latLng) {
            pano.setPosition(data.location.latLng);
          }
        });
        """.trimIndent()
    } else {
        """
        const pos = { lat: ${location.lat}, lng: ${location.lng} };
        const map = new google.maps.Map(document.getElementById('stage'), {
          center: pos,
          zoom: 17,
          mapTypeControl: false,
          fullscreenControl: false,
          streetViewControl: true
        });
        new google.maps.Marker({ position: pos, map: map, title: '${location.cityBadgeEn.replace("'", "\\'")}' });
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
          <style>
            html, body, #stage { width: 100%; height: 100%; margin: 0; padding: 0; overflow: hidden; background: #0F172A; }
          </style>
        </head>
        <body>
          <div id="stage"></div>
          <script>
            function initTravelStage() {
              $initScript
            }
          </script>
          <script async defer src="https://maps.googleapis.com/maps/api/js?key=$safeKey&callback=initTravelStage"></script>
        </body>
        </html>
    """.trimIndent()
}
