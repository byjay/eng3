package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.RealDialogueClip
import com.example.data.RealSceneClipFetcher
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.SpartaRedShadow
import com.example.ui.theme.TrophyGold
import java.net.URLEncoder

object YouTubeContextClipLauncher {

    fun sanitizeEnglishQuery(raw: String): String = RealSceneClipFetcher.sanitizeQuery(raw).ifBlank { "hello" }

    fun openYouTubeConversationSearch(context: Context, wordOrPhrase: String) {
        val clean = sanitizeEnglishQuery(wordOrPhrase)
        val query = URLEncoder.encode("\"$clean\" english conversation pronunciation in context", "UTF-8")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=$query")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun openPlayPhraseMovieClips(context: Context, wordOrPhrase: String) {
        val clean = sanitizeEnglishQuery(wordOrPhrase)
        val encoded = URLEncoder.encode(clean, "UTF-8")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.playphrase.me/#/search?q=$encoded")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }
}

/**
 * Clean Exact-Dialogue Video Clip Dialog:
 * - Tab 0 (Default): Direct native VideoView MP4 stream (GetYarn / PlayPhrase) showing
 *   ONLY the exact 3~5 second spoken movie/drama clip + highlighted subtitle line!
 *   Completely bypasses YouTube's "Sign in to confirm you're not a bot" WebView wall and hides search bars!
 * - Tab 1: Minimal YouGlish player (search bar stripped via data-components="72") + YouTube App launcher.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWordContextDialog(
    wordOrPhrase: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val safeQuery = remember(wordOrPhrase) { YouTubeContextClipLauncher.sanitizeEnglishQuery(wordOrPhrase) }

    var playerMode by remember { mutableIntStateOf(0) } // 0 = Direct Exact-Scene MP4, 1 = YouGlish Minimal
    var isLoadingClips by remember { mutableStateOf(true) }
    var clips by remember { mutableStateOf<List<RealDialogueClip>>(emptyList()) }
    var activeClipIndex by remember { mutableIntStateOf(0) }
    var replayTrigger by remember { mutableIntStateOf(0) }
    var videoBuffering by remember { mutableStateOf(true) }
    var videoError by remember { mutableStateOf<String?>(null) }
    var accent by remember { mutableStateOf("us") }

    LaunchedEffect(safeQuery) {
        isLoadingClips = true
        videoError = null
        activeClipIndex = 0
        clips = RealSceneClipFetcher.fetchExactSceneClips(safeQuery)
        isLoadingClips = false
    }

    val currentClip = clips.getOrNull(activeClipIndex)

    // Minimal YouGlish embed (data-components="72" removes search bar & accent bar; video + caption only)
    val minimalYouGlishHtml = remember(safeQuery, accent) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
          <style>
            * { box-sizing: border-box; }
            body { margin: 0; padding: 0; background: #000000; color: #FFFFFF; overflow: hidden; }
            #yg-widget-0 { width: 100% !important; }
          </style>
        </head>
        <body>
          <a id="yg-widget-0" class="youglish-widget"
             data-query="$safeQuery"
             data-lang="english"
             data-accent="$accent"
             data-components="72"
             data-auto-start="1"
             data-bkg-color="theme_dark"
             href="https://youglish.com">Loading "$safeQuery"...</a>
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
            color = Color(0xFF0B131E),
            border = BorderStroke(1.5.dp, TrophyGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = TrophyGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "실제 원어민 대사 장면 바로재생",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                            Text(
                                text = "\"$safeQuery\" · 해당 대사 구간만 자동 반복",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_youtube_clip_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
                    }
                }

                // Clean Mode Switcher: Direct Scene MP4 (No Bot Block!) vs YouGlish
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "영화·미드 대사 직재생 (무차단)",
                        1 to "유튜브 YouGlish"
                    ).forEach { (mode, label) ->
                        val active = playerMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) AdventureEmerald else Color(0xFF1A2838),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { playerMode = mode }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                if (playerMode == 0) {
                    // ==================== DIRECT EXACT-SCENE MP4 PLAYER (ONLY THE SCENE & DIALOGUE!) ====================
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
                                    CircularProgressIndicator(color = TrophyGold)
                                    Text(
                                        text = "\"$safeQuery\" 실제 원어민 대사 장면 찾는 중...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            currentClip != null -> {
                                // Native Android VideoView streaming direct HTTPS MP4 of ONLY that spoken line
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
                                                videoError = "이 장면 스트림을 불러오지 못해 다음 장면이나 유튜브 앱으로 열어주세요."
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

                                // Bottom gradient + ONLY the exact spoken dialogue line overlay (target word highlighted!)
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
                                            text = buildHighlightedDialogue(currentClip.dialogueLineEn, safeQuery),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 16.sp,
                                                lineHeight = 22.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${currentClip.sourceTitle} · 장면 ${activeClipIndex + 1}/${clips.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                    }
                                }

                                if (videoBuffering) {
                                    CircularProgressIndicator(
                                        color = TrophyGold,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                if (videoError != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
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
                                    Text(
                                        text = "\"$safeQuery\"에 일치하는 MP4 대사 클립을 찾지 못했어요.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "아래 [유튜브 앱 바로열기]를 누르면 로그인된 유튜브 앱에서 봇 차단 없이 바로 재생됩니다!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TrophyGold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Prev / Replay / Next Exact-Scene Controls
                    if (clips.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Tactile3DButton(
                                text = "이전 장면",
                                icon = Icons.AutoMirrored.Filled.NavigateBefore,
                                containerColor = Color(0xFF1F3247),
                                shadowColor = Color(0xFF0E1B29),
                                height = 42.dp,
                                enabled = clips.size > 1,
                                modifier = Modifier.weight(1f),
                                tag = "clip_prev_button",
                                onClick = {
                                    activeClipIndex = if (activeClipIndex > 0) activeClipIndex - 1 else clips.lastIndex
                                }
                            )
                            Surface(
                                shape = CircleShape,
                                color = TrophyGold,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { replayTrigger++ }
                                    .testTag("clip_replay_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = "대사 다시 재생",
                                        tint = Color(0xFF3E2C00),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Tactile3DButton(
                                text = "다음 장면 (${activeClipIndex + 1}/${clips.size})",
                                icon = Icons.AutoMirrored.Filled.NavigateNext,
                                containerColor = AdventureEmerald,
                                shadowColor = AdventureEmeraldShadow,
                                height = 42.dp,
                                enabled = clips.size > 1,
                                modifier = Modifier.weight(1.25f),
                                tag = "clip_next_button",
                                onClick = {
                                    activeClipIndex = (activeClipIndex + 1) % clips.size
                                }
                            )
                        }
                    }
                } else {
                    // ==================== MINIMAL YOUGLISH MODE (SEARCH BAR HIDDEN) ====================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("us" to "미국 US", "uk" to "영국 UK", "aus" to "호주 AUS").forEach { (code, label) ->
                            val active = accent == code
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) SpartaRed else Color(0xFF1A2838),
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
                            .height(275.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    webViewClient = WebViewClient()
                                    webChromeClient = WebChromeClient()
                                    loadDataWithBaseURL("https://youglish.com", minimalYouGlishHtml, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                val tagKey = "$safeQuery|$accent"
                                if (webView.tag != tagKey) {
                                    webView.tag = tagKey
                                    webView.loadDataWithBaseURL("https://youglish.com", minimalYouGlishHtml, "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = "참고: 유튜브 웹뷰에서 봇 확인이 뜨면 [영화·미드 대사 직재생] 탭이나 아래 [유튜브 앱 바로열기]를 이용하세요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // External Native App Launchers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tactile3DButton(
                        text = "유튜브 앱 바로열기",
                        icon = Icons.Default.PlayCircle,
                        containerColor = SpartaRed,
                        shadowColor = SpartaRedShadow,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                        tag = "open_youtube_external",
                        onClick = { YouTubeContextClipLauncher.openYouTubeConversationSearch(context, safeQuery) }
                    )
                    Tactile3DButton(
                        text = "PlayPhrase 웹",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        containerColor = SkyQuestBlue,
                        shadowColor = SkyQuestBlueShadow,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                        tag = "open_playphrase_external",
                        onClick = { YouTubeContextClipLauncher.openPlayPhraseMovieClips(context, safeQuery) }
                    )
                }
            }
        }
    }
}

private fun buildHighlightedDialogue(dialogue: String, query: String) = buildAnnotatedString {
    val tokens = query.split(" ").filter { it.length >= 2 }.map { it.lowercase() }
    val words = dialogue.split(" ")
    words.forEachIndexed { idx, token ->
        val cleanToken = token.lowercase().replace(Regex("[^a-z0-9']"), "")
        val isMatch = tokens.any { cleanToken.contains(it) || it.contains(cleanToken) && cleanToken.length >= 3 }
        if (isMatch) {
            withStyle(SpanStyle(color = TrophyGold, fontWeight = FontWeight.ExtraBold)) {
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
