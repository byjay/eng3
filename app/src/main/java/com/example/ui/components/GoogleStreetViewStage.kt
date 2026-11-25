package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.model.AiHumanTutor
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris

data class TravelStreetLocation(
    val zoneId: Int,
    val lat: Double,
    val lng: Double,
    val heading: Int,
    val pitch: Int,
    val placeNameKo: String,
    val cityBadgeEn: String
)

object TravelStreetDirectory {
    private val locations = listOf(
        TravelStreetLocation(1, 40.6438, -73.7820, 120, 4, "뉴욕 JFK 국제공항 터미널 4", "JFK Terminal 4, NY"),
        TravelStreetLocation(2, 51.4713, -0.4880, 95, 2, "런던 히드로 공항 터미널 5", "Heathrow T5, London"),
        TravelStreetLocation(3, 40.7580, -73.9855, 195, 8, "뉴욕 타임스스퀘어 브로드웨이", "Times Square, NYC"),
        TravelStreetLocation(4, 40.7644, -73.9745, 240, 6, "뉴욕 5번가 플라자 호텔 앞", "5th Ave Plaza, NYC"),
        TravelStreetLocation(5, 40.7233, -74.0005, 65, 3, "뉴욕 소호 프린스 스트리트 카페 거리", "SoHo Prince St, NYC"),
        TravelStreetLocation(6, 51.5152, -0.1419, 80, 4, "런던 옥스퍼드 스트리트 쇼핑 거리", "Oxford St, London"),
        TravelStreetLocation(7, 48.8708, 2.3048, 300, 5, "파리 샹젤리제 개선문 거리", "Champs-Élysées, Paris"),
        TravelStreetLocation(8, 37.7897, -122.4014, 215, 4, "샌프란시스코 마켓 스트리트", "Market St, San Francisco"),
        TravelStreetLocation(9, 35.6595, 139.7004, 15, 6, "도쿄 시부야 스크램블 교차로", "Shibuya Crossing, Tokyo"),
        TravelStreetLocation(10, 1.2834, 103.8607, 145, 5, "싱가포르 마리나 베이 산책로", "Marina Bay, Singapore")
    )

    fun locationForZone(zoneId: Int): TravelStreetLocation =
        locations.firstOrNull { it.zoneId == zoneId } ?: locations.first()
}

/**
 * Real Google Maps / 360° Street View backdrop layered with the transparent
 * SVG+CSS Lip-Sync Rig + proactive API-driven travel conversation companion.
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
    var useStreetViewMode by remember { mutableStateOf(true) }

    val rawMapsKey = BuildConfig.GOOGLE_MAPS_API_KEY.trim()
    val hasValidMapsKey = rawMapsKey.isNotBlank() &&
        !rawMapsKey.equals("YOUR_GOOGLE_MAPS_API_KEY", ignoreCase = true)

    val htmlContent = remember(location, useStreetViewMode, hasValidMapsKey) {
        if (hasValidMapsKey) {
            buildGoogleMapsStreetViewHtml(
                apiKey = rawMapsKey,
                location = location,
                streetViewMode = useStreetViewMode
            )
        } else {
            ""
        }
    }

    val fallbackEmbedUrl = remember(location, useStreetViewMode) {
        if (useStreetViewMode) {
            "https://maps.google.com/maps?q=${location.lat},${location.lng}&t=k&z=18&output=embed"
        } else {
            "https://maps.google.com/maps?q=${location.lat},${location.lng}&z=16&output=embed"
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, StudioCobalt.copy(alpha = 0.25f)),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("travel_google_street_stage")
    ) {
        Column {
            // 1. Live 360° Google Street View + Transparent SVG+CSS Lip-Sync Character Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(288.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFFDCEBFF))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.mediaPlaybackRequiresUserGesture = false
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        if (hasValidMapsKey) {
                            val tagKey = "gmaps_${location.zoneId}_$useStreetViewMode"
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
                        } else {
                            val tagKey = "embed_${location.zoneId}_$useStreetViewMode"
                            if (webView.tag != tagKey) {
                                webView.tag = tagKey
                                webView.loadUrl(fallbackEmbedUrl)
                            }
                        }
                    }
                )

                // Soft atmospheric bottom legibility gradient so text/controls stay crisp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF0F172A).copy(alpha = 0.72f)
                                )
                            )
                        )
                )

                // Top Row: Real Location Chip + StreetView/Map Toggle + Open Google Maps Street View
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
                        color = Color.White.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.25f)),
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
                                tint = StudioCobalt,
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
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (useStreetViewMode) StudioCobalt else Color.White.copy(alpha = 0.92f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { useStreetViewMode = !useStreetViewMode }
                                .testTag("travel_toggle_street_map")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (useStreetViewMode) Icons.Outlined.Explore else Icons.Outlined.Map,
                                    contentDescription = "거리뷰 전환",
                                    tint = if (useStreetViewMode) Color.White else StudioCobalt,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (useStreetViewMode) "360° 거리뷰" else "지도",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (useStreetViewMode) Color.White else Color(0xFF0F172A)
                                )
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
                                    contentDescription = "구글 지도 거리뷰 앱 열기",
                                    tint = StudioCobalt,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Transparent SVG+CSS Lip-Sync Rig Layered Directly Over Real Street Scene (Bottom-Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 0.dp)
                        .width(156.dp)
                        .fillMaxHeight(0.76f)
                ) {
                    SvgCssLipSyncRigView(
                        tutorId = tutor.id,
                        tutorName = tutor.name,
                        learnerName = activePlayerName,
                        activeTopicTag = scenarioTitleKo,
                        activeGesture = when {
                            isListening -> MotionPhotoGesture.LISTEN_LEAN_IN
                            isSpeaking -> MotionPhotoGesture.CITY_VIBE
                            isThinking -> MotionPhotoGesture.THOUGHTFUL_MUSE
                            else -> MotionPhotoGesture.FRIENDLY_WAVE
                        },
                        aiMood = proactiveMood,
                        liveThoughtKo = proactiveThoughtKo,
                        spokenSentence = proactiveLineEn,
                        isSpeaking = isSpeaking,
                        isListening = isListening,
                        isThinking = isThinking,
                        speechRate = speechRate,
                        speechBeatIndex = speechBeatIndex,
                        userRmsDb = rmsDb,
                        transparentBackdrop = true,
                        compactOverlay = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Left-Bottom Live Status & Street Coordinates Chip + Quick Proactive Button
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp, end = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isListening -> StudioCoral
                            isSpeaking -> GlacierCyan
                            isThinking -> StudioIris
                            else -> Color(0xFF0F172A).copy(alpha = 0.72f)
                        }
                    ) {
                        Text(
                            text = when {
                                isListening -> "듣는 중 · 립싱크 대기"
                                isSpeaking -> "${tutor.name} 말하는 중"
                                isThinking -> "현지 대화 생성 중..."
                                else -> location.cityBadgeEn
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioCobalt,
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isThinking, onClick = onTriggerProactiveSpeak)
                                .testTag("travel_proactive_speak_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onMicClick)
                                .testTag("travel_street_stage_mic")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isListening) Icons.Outlined.Stop else Icons.Outlined.MicNone,
                                    contentDescription = if (isListening) "듣기 중지" else "거리에서 말하기",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Proactive AI Companion Dialogue Strip (calls learner by name!)
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
                            color = StudioCobalt.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${tutor.name} · ${activePlayerName}님과 현지 실전",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioCobalt,
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
                            .size(34.dp)
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
                        "Hey $activePlayerName! Welcome to ${location.cityBadgeEn}. Ready to try '$scenarioTitleKo'?"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (showKorean) {
                    Text(
                        text = proactiveLineKo.ifBlank {
                            "${activePlayerName}님, ${location.placeNameKo}에 도착했어요! '$scenarioTitleKo' 상황을 바로 시작해 볼까요?"
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
                            text = "능동 사고: $proactiveThoughtKo",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioIris,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (!hasValidMapsKey) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioCoral.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Studio Secrets에 GOOGLE_MAPS_API_KEY를 등록하면 인앱 360° 실제 거리뷰 파노라마가 활성화됩니다. (우측 상단 아이콘으로 구글 지도 거리뷰 즉시 열기 가능)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            html, body, #stage { width: 100%; height: 100%; margin: 0; padding: 0; overflow: hidden; background: #DCEBFF; }
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
