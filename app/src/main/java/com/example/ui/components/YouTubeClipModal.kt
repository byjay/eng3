package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.widget.VideoView
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.RealDialogueClip
import com.example.data.RealSceneClipFetcher
import com.example.data.YouTubeDialogueMatch
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.NightSurfaceElevated
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.RoseCrimsonDeep
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCobaltDeep
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import java.net.URLEncoder
import java.util.Locale

object YouTubeContextClipLauncher {

    fun sanitizeEnglishQuery(raw: String): String =
        RealSceneClipFetcher.sanitizeQuery(raw).ifBlank { "how are you" }

    fun openYouTubeVideoAtTimestamp(context: Context, videoId: String, startSeconds: Int) {
        val webUrl = if (startSeconds > 0) {
            "https://www.youtube.com/watch?v=$videoId&t=${startSeconds}s"
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(webIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(appIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "유튜브 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openYouTubeConversationSearch(context: Context, wordOrPhrase: String) {
        val clean = sanitizeEnglishQuery(wordOrPhrase)
        val query = URLEncoder.encode("\"$clean\" english lesson conversation", "UTF-8")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=$query&sp=EgIoAQ%253D%253D")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "유튜브 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFilmotExternal(context: Context, wordOrPhrase: String) {
        val url = RealSceneClipFetcher.buildFilmotSearchUrl(wordOrPhrase)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "브라우저를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * YouTube Lecture Segment Sync & Multi-Source Clip Modal:
 * - Tab 0: YouTube IFrame Player API A-B Auto-Loop [start-1s .. end+1.5s] + 0.75x/1.0x + Local JSON Cache
 * - Tab 1: Filmot Caption Search (in-app WebView)
 * - Tab 2: YouGlish Accent Player (US / UK / AUS)
 * - Tab 3: Direct Movie/Drama MP4 Clips (GetYarn / PlayPhrase)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWordContextDialog(
    wordOrPhrase: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialQuery = remember(wordOrPhrase) {
        RealSceneClipFetcher.extractBestSearchPhrase(wordOrPhrase)
    }

    var searchInput by remember(wordOrPhrase) { mutableStateOf(initialQuery) }
    var activeQuery by remember(wordOrPhrase) { mutableStateOf(initialQuery) }
    var playerMode by remember { mutableIntStateOf(2) } // 0 = Official YT Link, 1 = Filmot, 2 = YouGlish (Official Embed)

    // Tab 0: YouTube IFrame A-B Loop state
    var isLoadingYouTube by remember { mutableStateOf(true) }
    var ytMatches by remember { mutableStateOf<List<YouTubeDialogueMatch>>(emptyList()) }
    var activeYtIndex by remember { mutableIntStateOf(0) }
    var isAbLoopEnabled by remember { mutableStateOf(true) }
    var playbackRate by remember { mutableFloatStateOf(1.0f) }
    var ytWebViewRef by remember { mutableStateOf<WebView?>(null) }

    // Tab 3: Movie MP4 state
    var isLoadingClips by remember { mutableStateOf(false) }
    var clipsLoadedForQuery by remember { mutableStateOf("") }
    var clips by remember { mutableStateOf<List<RealDialogueClip>>(emptyList()) }
    var activeClipIndex by remember { mutableIntStateOf(0) }
    var replayTrigger by remember { mutableIntStateOf(0) }
    var videoBuffering by remember { mutableStateOf(true) }
    var videoError by remember { mutableStateOf<String?>(null) }

    // Tab 2: YouGlish accent
    var accent by remember { mutableStateOf("us") }

    // Built-in lightweight TTS for slow shadowing of matched lines
    var localTtsReady by remember { mutableStateOf(false) }
    val localTts = remember(context) {
        TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) localTtsReady = true
        }
    }
    DisposableEffect(localTts) {
        onDispose {
            try {
                localTts.stop()
                localTts.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    fun speakLineSlowly(line: String) {
        if (!localTtsReady || line.isBlank()) return
        try {
            localTts.language = Locale.US
            localTts.setSpeechRate(0.84f)
            localTts.speak(line, TextToSpeech.QUEUE_FLUSH, null, "yt_dialogue_tts")
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(activeQuery) {
        isLoadingYouTube = true
        activeYtIndex = 0
        ytMatches = RealSceneClipFetcher.searchYouTubeDialogues(activeQuery, context.cacheDir)
        isLoadingYouTube = false
    }

    // Sync loop / playback rate changes to YouTube IFrame Player API without reloading WebView!
    LaunchedEffect(isAbLoopEnabled) {
        ytWebViewRef?.evaluateJavascript("window.setLoopEnabled && window.setLoopEnabled($isAbLoopEnabled);", null)
    }
    LaunchedEffect(playbackRate) {
        ytWebViewRef?.evaluateJavascript("window.setPlaybackSpeed && window.setPlaybackSpeed($playbackRate);", null)
    }

    LaunchedEffect(playerMode, activeQuery) {
        if (playerMode == 3 && clipsLoadedForQuery != activeQuery) {
            isLoadingClips = true
            videoError = null
            activeClipIndex = 0
            clips = RealSceneClipFetcher.fetchExactSceneClips(activeQuery)
            clipsLoadedForQuery = activeQuery
            isLoadingClips = false
        }
    }

    val currentYt = ytMatches.getOrNull(activeYtIndex)
    val currentClip = clips.getOrNull(activeClipIndex)

    val quickKeywordChips = remember(wordOrPhrase, initialQuery) {
        val cleanAll = RealSceneClipFetcher.sanitizeQuery(wordOrPhrase)
        val distinctWords = cleanAll.split(" ")
            .filter { it.length >= 4 }
            .distinctBy { it.lowercase() }
            .sortedByDescending { it.length }
            .take(3)
        (listOf(initialQuery) + distinctWords).distinctBy { it.lowercase() }.take(4)
    }

    val filmotUrl = remember(activeQuery) { RealSceneClipFetcher.buildFilmotSearchUrl(activeQuery) }

    val minimalYouGlishHtml = remember(activeQuery, accent) {
        val safeAttr = activeQuery.replace("\"", "")
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
          <style>
            * { box-sizing: border-box; }
            body { margin: 0; padding: 0; background: #0D0F14; color: #F4F6FB; overflow: hidden; }
            #yg-widget-0 { width: 100% !important; }
          </style>
        </head>
        <body>
          <a id="yg-widget-0" class="youglish-widget"
             data-query="$safeAttr"
             data-lang="english"
             data-accent="$accent"
             data-components="72"
             data-auto-start="1"
             data-bkg-color="theme_dark"
             href="https://youglish.com">Loading "$safeAttr"...</a>
          <script async src="https://youglish.com/public/emb/widget.js" charset="utf-8"></script>
        </body>
        </html>
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NightBackground,
            border = BorderStroke(1.dp, NightBorder),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.91f)
                .padding(vertical = 8.dp)
                .testTag("youtube_dialogue_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // Clean Commercial Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = null,
                            tint = StudioCoral,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "유튜브 구간 학습",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_youtube_clip_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
                    }
                }

                // Compact Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("단어·표현 검색", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val next = YouTubeContextClipLauncher.sanitizeEnglishQuery(searchInput)
                                searchInput = next
                                activeQuery = next
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = NightSurface,
                            unfocusedContainerColor = NightSurface,
                            focusedBorderColor = StudioCobalt,
                            unfocusedBorderColor = NightBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("youtube_dialogue_search_input")
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioCobalt,
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                val next = YouTubeContextClipLauncher.sanitizeEnglishQuery(searchInput)
                                searchInput = next
                                activeQuery = next
                            }
                            .testTag("youtube_dialogue_search_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "검색", tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("검색", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                }

                // Quick keyword chips
                if (quickKeywordChips.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickKeywordChips) { chip ->
                            val selected = chip.equals(activeQuery, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) StudioCobalt.copy(alpha = 0.25f) else NightSurface,
                                border = BorderStroke(1.dp, if (selected) StudioCobalt else NightBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        searchInput = chip
                                        activeQuery = chip
                                    }
                            ) {
                                Text(
                                    text = chip,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // 4-Source Integrated Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        2 to "YouGlish",
                        1 to "Filmot",
                        0 to "공식 YouTube"
                    ).forEach { (mode, label) ->
                        val active = playerMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) StudioCobalt else NightSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { playerMode = mode }
                                .testTag("youtube_mode_tab_$mode")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (active) Color.White else Color.White.copy(alpha = 0.72f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Main Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (playerMode) {
                        0 -> {
                            // ==================== TAB 0: YOUTUBE IFRAME PLAYER API A-B AUTO-LOOP ====================
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // HTTP 429 Defensive Notice
                                if (RealSceneClipFetcher.isCurrentlyRateLimited()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = StudioCoral.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "자막 요청 제한 감지 · 캐시/Filmot 우선 사용",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Filmot ▸",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = StudioCoral,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { playerMode = 1 }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Embedded YouTube IFrame Player API Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(194.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isLoadingYouTube -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(color = StudioCobalt)
                                                Text(
                                                    text = "\"$activeQuery\" 자막 구간 검색 중",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                        currentYt != null -> {
                                            val embedHtml = remember(currentYt.videoId, currentYt.startSeconds, currentYt.endSeconds) {
                                                buildYouTubeIFrameLoopHtml(
                                                    videoId = currentYt.videoId,
                                                    startSeconds = currentYt.startSeconds,
                                                    endSeconds = currentYt.endSeconds.coerceAtLeast(currentYt.startSeconds + 6),
                                                    loopEnabled = isAbLoopEnabled,
                                                    playbackRate = playbackRate
                                                )
                                            }
                                            AndroidView(
                                                factory = { ctx ->
                                                    WebView(ctx).apply {
                                                        settings.javaScriptEnabled = true
                                                        settings.domStorageEnabled = true
                                                        settings.mediaPlaybackRequiresUserGesture = false
                                                        settings.loadWithOverviewMode = true
                                                        settings.useWideViewPort = true
                                                        settings.userAgentString =
                                                            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                                        CookieManager.getInstance().setAcceptCookie(true)
                                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                                        webChromeClient = WebChromeClient()
                                                        webViewClient = object : WebViewClient() {
                                                            override fun shouldOverrideUrlLoading(
                                                                view: WebView?,
                                                                request: WebResourceRequest?
                                                            ): Boolean {
                                                                val url = request?.url?.toString().orEmpty()
                                                                return !(url.contains("youtube") || url.contains("youtu.be") || url.contains("googlevideo"))
                                                            }
                                                        }
                                                        ytWebViewRef = this
                                                    }
                                                },
                                                update = { webView ->
                                                    ytWebViewRef = webView
                                                    val tagKey = "${currentYt.videoId}@${currentYt.startSeconds}-${currentYt.endSeconds}"
                                                    if (webView.tag != tagKey) {
                                                        webView.tag = tagKey
                                                        webView.loadDataWithBaseURL(
                                                            "https://www.youtube-nocookie.com",
                                                            embedHtml,
                                                            "text/html",
                                                            "UTF-8",
                                                            null
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        else -> {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "저작권·이용약관 준수를 위해 공식 YouTube 앱/웹에서 \"$activeQuery\" 영어 회화 영상을 바로 검색합니다.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White,
                                                    textAlign = TextAlign.Center
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = RoseCrimson,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                YouTubeContextClipLauncher.openYouTubeConversationSearch(context, activeQuery)
                                                            }
                                                            .testTag("open_official_youtube_search_btn")
                                                    ) {
                                                        Text(
                                                            text = "YouTube 공식 검색",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = StudioCobalt,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable { playerMode = 2 }
                                                    ) {
                                                        Text(
                                                            text = "YouGlish 듣기",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // A-B Loop Controls & Matched Caption Card
                                if (currentYt != null) {
                                    // Compact A-B Loop + Speed + Replay Strip
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isAbLoopEnabled) GlacierCyan.copy(alpha = 0.22f) else NightSurface,
                                            border = BorderStroke(1.dp, if (isAbLoopEnabled) GlacierCyan else NightBorder),
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { isAbLoopEnabled = !isAbLoopEnabled }
                                                .testTag("yt_ab_loop_toggle")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Loop,
                                                    contentDescription = "A-B 구간 반복",
                                                    tint = if (isAbLoopEnabled) GlacierCyan else Color.White.copy(alpha = 0.65f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = if (isAbLoopEnabled) "A-B 반복 ON" else "A-B 반복 OFF",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = NightSurface,
                                            border = BorderStroke(1.dp, if (playbackRate < 1f) StudioIris else NightBorder),
                                            modifier = Modifier
                                                .weight(0.75f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { playbackRate = if (playbackRate == 1.0f) 0.75f else 1.0f }
                                                .testTag("yt_speed_toggle")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Speed, contentDescription = "배속", tint = StudioIris, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = "${playbackRate}x",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = NightSurface,
                                            border = BorderStroke(1.dp, NightBorder),
                                            modifier = Modifier
                                                .weight(0.85f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    ytWebViewRef?.evaluateJavascript("window.replaySegment && window.replaySegment();", null)
                                                }
                                                .testTag("yt_replay_segment_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Replay, contentDescription = "구간 다시 재생", tint = StudioCoral, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = "다시재생",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    // Matched Dialogue Card
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = NightSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (currentYt.isExactCaptionMatch) GlacierCyan.copy(alpha = 0.65f) else NightBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(11.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = if (currentYt.isExactCaptionMatch) GlacierCyan else StudioCobalt
                                                    ) {
                                                        Text(
                                                            text = currentYt.formattedWindow,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = StudioIris.copy(alpha = 0.22f)
                                                    ) {
                                                        Text(
                                                            text = currentYt.sourceBadge,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = currentYt.channelName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(start = 8.dp)
                                                )
                                            }

                                            if (currentYt.contextBeforeEn.isNotBlank()) {
                                                Text(
                                                    text = currentYt.contextBeforeEn,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.50f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Text(
                                                text = buildHighlightedDialogue(currentYt.matchedDialogueEn, activeQuery),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    lineHeight = 20.sp
                                                ),
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (currentYt.contextAfterEn.isNotBlank()) {
                                                Text(
                                                    text = currentYt.contextAfterEn,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.50f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = StudioCobalt.copy(alpha = 0.22f),
                                                    border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.55f)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { speakLineSlowly(currentYt.matchedDialogueEn) }
                                                        .testTag("yt_dialogue_tts_btn")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "느리게 듣기", tint = GlacierCyan, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("느리게 듣기", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = StudioCoral.copy(alpha = 0.24f),
                                                    border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.65f)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            YouTubeContextClipLauncher.openYouTubeVideoAtTimestamp(
                                                                context,
                                                                currentYt.videoId,
                                                                currentYt.startSeconds
                                                            )
                                                        }
                                                        .testTag("yt_open_timestamp_btn")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.PlayCircle, contentDescription = "유튜브 앱 열기", tint = StudioCoral, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                            text = "유튜브 ${currentYt.formattedTimestamp}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Scrollable Matched Clips List
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(ytMatches, key = { idx, item -> "${item.videoId}_$idx" }) { idx, item ->
                                            val selected = idx == activeYtIndex
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (selected) StudioCobalt.copy(alpha = 0.22f) else NightSurface,
                                                border = BorderStroke(1.dp, if (selected) StudioCobalt else NightBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        if (activeYtIndex == idx) {
                                                            ytWebViewRef?.evaluateJavascript("window.replaySegment && window.replaySegment();", null)
                                                        } else {
                                                            activeYtIndex = idx
                                                        }
                                                    }
                                                    .testTag("yt_scene_item_$idx")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (item.isExactCaptionMatch) GlacierCyan else NightSurfaceElevated
                                                    ) {
                                                        Text(
                                                            text = if (item.isExactCaptionMatch) item.formattedTimestamp else "#${idx + 1}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                            text = item.matchedDialogueEn,
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = if (selected) StudioCoral else Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${item.title} · ${item.channelName}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = Color.White.copy(alpha = 0.6f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.PlayCircle,
                                                        contentDescription = "구간 재생",
                                                        tint = if (selected) StudioCoral else Color.White.copy(alpha = 0.55f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ==================== TAB 1: FILMOT CAPTION SEARCH ====================
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NightSurface)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                settings.loadWithOverviewMode = true
                                                settings.useWideViewPort = true
                                                settings.userAgentString =
                                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                                webViewClient = WebViewClient()
                                                webChromeClient = WebChromeClient()
                                                loadUrl(filmotUrl)
                                            }
                                        },
                                        update = { webView ->
                                            if (webView.tag != filmotUrl) {
                                                webView.tag = filmotUrl
                                                webView.loadUrl(filmotUrl)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        2 -> {
                            // ==================== TAB 2: YOUGLISH ACCENT PLAYER ====================
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("us" to "미국 US", "uk" to "영국 UK", "aus" to "호주 AUS").forEach { (code, label) ->
                                        val active = accent == code
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (active) StudioCoral else NightSurfaceElevated,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { accent = code }
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .heightIn(min = 240.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                settings.mediaPlaybackRequiresUserGesture = false
                                                settings.userAgentString =
                                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                                CookieManager.getInstance().setAcceptCookie(true)
                                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                                webViewClient = WebViewClient()
                                                webChromeClient = WebChromeClient()
                                                loadDataWithBaseURL("https://youglish.com", minimalYouGlishHtml, "text/html", "UTF-8", null)
                                            }
                                        },
                                        update = { webView ->
                                            val tagKey = "$activeQuery|$accent"
                                            if (webView.tag != tagKey) {
                                                webView.tag = tagKey
                                                webView.loadDataWithBaseURL("https://youglish.com", minimalYouGlishHtml, "text/html", "UTF-8", null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        else -> {
                            // ==================== TAB 3: DIRECT MOVIE MP4 CLIPS ====================
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(235.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isLoadingClips -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(color = StudioCobalt)
                                                Text(
                                                    text = "\"$activeQuery\" 클립 검색 중",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        currentClip != null -> {
                                            AndroidView(
                                                factory = { ctx ->
                                                    VideoView(ctx).apply {
                                                        setOnPreparedListener { mp ->
                                                            mp.isLooping = true
                                                            videoBuffering = false
                                                            videoError = null
                                                            start()
                                                        }
                                                        setOnErrorListener { _, _, _ ->
                                                            videoBuffering = false
                                                            videoError = "클립을 불러오지 못했습니다."
                                                            true
                                                        }
                                                    }
                                                },
                                                update = { vv ->
                                                    val desiredTag = "${currentClip.mp4Url}#$replayTrigger"
                                                    if (vv.tag != desiredTag) {
                                                        vv.tag = desiredTag
                                                        videoBuffering = true
                                                        videoError = null
                                                        vv.stopPlayback()
                                                        vv.setVideoURI(Uri.parse(currentClip.mp4Url))
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                                                        )
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Text(
                                                        text = buildHighlightedDialogue(currentClip.dialogueLineEn, activeQuery),
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontSize = 15.sp,
                                                            lineHeight = 21.sp,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "${currentClip.sourceTitle} · ${activeClipIndex + 1}/${clips.size}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }

                                            if (videoBuffering) {
                                                CircularProgressIndicator(color = StudioCoral, modifier = Modifier.size(34.dp))
                                            }
                                            if (videoError != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color.Black.copy(alpha = 0.82f),
                                                    modifier = Modifier.padding(16.dp)
                                                ) {
                                                    Text(
                                                        text = videoError!!,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            Column(
                                                modifier = Modifier.padding(18.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Movie, contentDescription = null, tint = StudioCoral, modifier = Modifier.size(32.dp))
                                                Text(
                                                    text = "일치하는 MP4 클립이 없습니다",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                if (clips.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Tactile3DButton(
                                            text = "이전",
                                            icon = Icons.AutoMirrored.Filled.NavigateBefore,
                                            containerColor = NightSurfaceElevated,
                                            shadowColor = NightBackground,
                                            height = 44.dp,
                                            enabled = clips.size > 1,
                                            modifier = Modifier.weight(1f),
                                            tag = "clip_prev_button",
                                            onClick = {
                                                activeClipIndex = if (activeClipIndex > 0) activeClipIndex - 1 else clips.lastIndex
                                            }
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = StudioCoral,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .clickable { replayTrigger++ }
                                                .testTag("clip_replay_button")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Replay,
                                                    contentDescription = "다시 재생",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Tactile3DButton(
                                            text = "다음 (${activeClipIndex + 1}/${clips.size})",
                                            icon = Icons.AutoMirrored.Filled.NavigateNext,
                                            containerColor = StudioCobalt,
                                            shadowColor = StudioCobaltDeep,
                                            height = 44.dp,
                                            enabled = clips.size > 1,
                                            modifier = Modifier.weight(1.25f),
                                            tag = "clip_next_button",
                                            onClick = {
                                                activeClipIndex = (activeClipIndex + 1) % clips.size
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom External Launchers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tactile3DButton(
                        text = "유튜브 앱 열기",
                        icon = Icons.Default.PlayCircle,
                        containerColor = RoseCrimson,
                        shadowColor = RoseCrimsonDeep,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                        tag = "open_youtube_external",
                        onClick = {
                            val yt = currentYt
                            if (yt != null) {
                                YouTubeContextClipLauncher.openYouTubeVideoAtTimestamp(context, yt.videoId, yt.startSeconds)
                            } else {
                                YouTubeContextClipLauncher.openYouTubeConversationSearch(context, activeQuery)
                            }
                        }
                    )
                    Tactile3DButton(
                        text = "Filmot 웹",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        containerColor = StudioCobalt,
                        shadowColor = StudioCobaltDeep,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                        tag = "open_filmot_external",
                        onClick = { YouTubeContextClipLauncher.openFilmotExternal(context, activeQuery) }
                    )
                }
            }
        }
    }
}

/**
 * Official YouTube IFrame Player API HTML wrapper implementing exact A-B segment auto-looping
 * and live JS bridge controls for loop toggle, playback rate (0.75x / 1.0x), and segment replay.
 */
private fun buildYouTubeIFrameLoopHtml(
    videoId: String,
    startSeconds: Int,
    endSeconds: Int,
    loopEnabled: Boolean,
    playbackRate: Float
): String {
    val safeStart = startSeconds.coerceAtLeast(0)
    val safeEnd = endSeconds.coerceAtLeast(safeStart + 4)
    val safeId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "")
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
          <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
            #player { width: 100%; height: 100%; }
          </style>
        </head>
        <body>
          <div id="player"></div>
          <script>
            var ytPlayer = null;
            var startSec = $safeStart;
            var endSec = $safeEnd;
            var loopEnabled = $loopEnabled;
            var desiredRate = $playbackRate;
            var loopTimer = null;

            var tag = document.createElement('script');
            tag.src = "https://www.youtube.com/iframe_api";
            var firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

            function onYouTubeIframeAPIReady() {
              ytPlayer = new YT.Player('player', {
                width: '100%',
                height: '100%',
                videoId: '$safeId',
                host: 'https://www.youtube-nocookie.com',
                playerVars: {
                  start: startSec,
                  end: endSec,
                  autoplay: 1,
                  playsinline: 1,
                  cc_load_policy: 1,
                  cc_lang_pref: 'en',
                  rel: 0,
                  modestbranding: 1
                },
                events: {
                  onReady: function(e) {
                    try {
                      e.target.setPlaybackRate(desiredRate);
                      e.target.seekTo(startSec, true);
                      e.target.playVideo();
                    } catch (_) {}
                    startLoopWatcher();
                  },
                  onStateChange: function(e) {
                    if (e.data === YT.PlayerState.ENDED && loopEnabled) {
                      try {
                        ytPlayer.seekTo(startSec, true);
                        ytPlayer.playVideo();
                      } catch (_) {}
                    }
                  }
                }
              });
            }

            function startLoopWatcher() {
              if (loopTimer) clearInterval(loopTimer);
              loopTimer = setInterval(function() {
                if (!ytPlayer || typeof ytPlayer.getCurrentTime !== 'function') return;
                try {
                  var cur = ytPlayer.getCurrentTime();
                  if (loopEnabled && cur >= endSec - 0.15) {
                    ytPlayer.seekTo(startSec, true);
                    ytPlayer.playVideo();
                  }
                } catch (_) {}
              }, 250);
            }

            window.setLoopEnabled = function(enabled) {
              loopEnabled = !!enabled;
            };
            window.setPlaybackSpeed = function(rate) {
              desiredRate = rate;
              if (ytPlayer && typeof ytPlayer.setPlaybackRate === 'function') {
                try { ytPlayer.setPlaybackRate(rate); } catch (_) {}
              }
            };
            window.replaySegment = function() {
              if (ytPlayer && typeof ytPlayer.seekTo === 'function') {
                try {
                  ytPlayer.seekTo(startSec, true);
                  ytPlayer.playVideo();
                } catch (_) {}
              }
            };
          </script>
        </body>
        </html>
    """.trimIndent()
}

private fun buildHighlightedDialogue(dialogue: String, query: String) = buildAnnotatedString {
    val tokens = query.split(" ").filter { it.length >= 2 }.map { it.lowercase() }
    val words = dialogue.split(" ")
    words.forEachIndexed { idx, token ->
        val cleanToken = token.lowercase().replace(Regex("[^a-z0-9']"), "")
        val isMatch = tokens.any {
            cleanToken.isNotEmpty() && (cleanToken.contains(it) || (it.contains(cleanToken) && cleanToken.length >= 3))
        }
        if (isMatch) {
            withStyle(SpanStyle(color = StudioCoral, fontWeight = FontWeight.ExtraBold)) {
                append(token)
            }
        } else {
            withStyle(SpanStyle(color = Color.White)) {
                append(token)
            }
        }
        if (idx < words.lastIndex) append(" ")
    }
}
