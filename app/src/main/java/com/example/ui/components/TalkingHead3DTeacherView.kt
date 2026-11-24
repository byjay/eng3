package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import kotlinx.coroutines.delay
import org.json.JSONObject

private enum class TalkingHeadLoadState {
    LOADING,
    READY,
    FALLBACK_TIMEOUT,
    FALLBACK_WEBGL_LOST
}

/**
 * WebView-Hosted 3D English AI Teacher powered by Vanilla JS (ESM) + Three.js 0.160.0 + @met4citizen/talkinghead:
 * - Loads Ready Player Me `.glb?morphTargets=ARKit,Oculus%20Visemes` in upper-body framing (`cameraView: 'upper'`)
 * - Real-time Viseme lip-sync + mood switching (`happy` | `neutral` | `thinking`) + natural eye blink & idle breath
 * - Speech Queue prevents overlapping audio/lip-sync on rapid utterances
 * - Mandatory Defensive Design:
 *   1. `webglcontextlost` listener -> instant fallback to Lottie Motion Tutor
 *   2. Explicit `AudioContext.resume()` on user interaction ("수업 시작" tap)
 *   3. 10-second `.glb` load timeout -> error banner + retry button + seamless Lottie fallback
 */
@SuppressLint("SetJavaScriptEnabled")
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
    var loadState by remember { mutableStateOf(TalkingHeadLoadState.LOADING) }
    var audioUnlocked by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastQueuedUtterance by remember { mutableStateOf("") }

    // Map tutorId to Ready Player Me `.glb` with ARKit + Oculus Visemes morphTargets
    val avatarGlbUrl = remember(tutorId) {
        when (tutorId) {
            "tutor_liam" -> "https://models.readyplayer.me/64bfa15f0e72c63d7c3934a6.glb?morphTargets=ARKit,Oculus%20Visemes"
            "tutor_sophia" -> "https://models.readyplayer.me/64614d3f7b2c70090a6e87f8.glb?morphTargets=ARKit,Oculus%20Visemes"
            else -> "https://models.readyplayer.me/64614d3f7b2c70090a6e87f8.glb?morphTargets=ARKit,Oculus%20Visemes"
        }
    }

    val moodName = remember(activeGesture, isThinking) {
        when {
            isThinking || activeGesture == MotionPhotoGesture.THOUGHTFUL_MUSE -> "thinking"
            activeGesture == MotionPhotoGesture.WARM_SMILE ||
                activeGesture == MotionPhotoGesture.CLAPPING_BRAVO ||
                activeGesture == MotionPhotoGesture.EXCITED_BOUNCE ||
                activeGesture == MotionPhotoGesture.AMAZED_SPARKLE ||
                activeGesture == MotionPhotoGesture.HEART_EYES ||
                activeGesture == MotionPhotoGesture.THUMBS_UP_COOL -> "happy"
            else -> "neutral"
        }
    }

    // Defensive 10-second load timeout on Compose side as well as JS side
    LaunchedEffect(reloadKey, avatarGlbUrl) {
        loadState = TalkingHeadLoadState.LOADING
        delay(10_000L)
        if (loadState == TalkingHeadLoadState.LOADING) {
            loadState = TalkingHeadLoadState.FALLBACK_TIMEOUT
        }
    }

    // Push mood transitions live
    LaunchedEffect(loadState, moodName) {
        if (loadState == TalkingHeadLoadState.READY) {
            webViewRef?.evaluateJavascript("window.setTeacherMood && window.setTeacherMood('$moodName');", null)
        }
    }

    // Push live speech cue into the 3D TalkingHead speech queue & lip-sync driver
    LaunchedEffect(loadState, spokenTextEn, isSpeaking) {
        if (loadState == TalkingHeadLoadState.READY) {
            if (isSpeaking && spokenTextEn.isNotBlank() && spokenTextEn != lastQueuedUtterance) {
                lastQueuedUtterance = spokenTextEn
                val quotedText = JSONObject.quote(spokenTextEn)
                webViewRef?.evaluateJavascript(
                    "window.enqueueTeacherSpeech && window.enqueueTeacherSpeech($quotedText, '$moodName', $speechRate);",
                    null
                )
            } else if (!isSpeaking) {
                webViewRef?.evaluateJavascript("window.setTeacherSpeakingState && window.setTeacherSpeakingState(false);", null)
            }
        }
    }

    val htmlPayload = remember(avatarGlbUrl) { buildTalkingHeadHtml(avatarGlbUrl) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
            .testTag("talkinghead_3d_stage")
    ) {
        // Underlying Lottie Motion Tutor serves as zero-flicker loading backdrop & instant WebGL/timeout fallback
        if (loadState != TalkingHeadLoadState.READY) {
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
        }

        // Hardware-Accelerated WebGL TalkingHead 3D Canvas
        if (loadState == TalkingHeadLoadState.LOADING || loadState == TalkingHeadLoadState.READY) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowContentAccess = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
                        }
                        webViewClient = WebViewClient()
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onAvatarReady() {
                                    post { loadState = TalkingHeadLoadState.READY }
                                }

                                @JavascriptInterface
                                fun onWebGlLost() {
                                    post { loadState = TalkingHeadLoadState.FALLBACK_WEBGL_LOST }
                                }

                                @JavascriptInterface
                                fun onLoadError() {
                                    post { loadState = TalkingHeadLoadState.FALLBACK_TIMEOUT }
                                }
                            },
                            "Android3DTeacher"
                        )
                        webViewRef = this
                        loadDataWithBaseURL("https://talkinghead.local/", htmlPayload, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    val desiredTag = "$avatarGlbUrl#$reloadKey"
                    if (webView.tag != desiredTag) {
                        webView.tag = desiredTag
                        webView.loadDataWithBaseURL("https://talkinghead.local/", htmlPayload, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading indicator chip (top-center, non-blocking)
        AnimatedVisibility(
            visible = loadState == TalkingHeadLoadState.LOADING,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 120.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = NightSurface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, StudioCobalt.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = StudioCobalt,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "3D 아바타 불러오는 중",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Explicit AudioContext.resume() user interaction pill when 3D is ready
        AnimatedVisibility(
            visible = loadState == TalkingHeadLoadState.READY && !audioUnlocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = StudioCobalt.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable {
                        audioUnlocked = true
                        webViewRef?.evaluateJavascript("window.unlockAudioContext && window.unlockAudioContext();", null)
                    }
                    .testTag("start_3d_lesson_audio_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = "수업 시작", tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "수업 시작",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                }
            }
        }

        // Defensive Timeout / WebGL Fallback Retry Pill (Lottie tutor keeps running smoothly behind!)
        AnimatedVisibility(
            visible = loadState == TalkingHeadLoadState.FALLBACK_TIMEOUT || loadState == TalkingHeadLoadState.FALLBACK_WEBGL_LOST,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 132.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NightSurface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, NightBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (loadState == TalkingHeadLoadState.FALLBACK_WEBGL_LOST) {
                            "WebGL 전환 · 모션 모드"
                        } else {
                            "3D 로드 지연 · 모션 모드"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioCoral,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { reloadKey++ }
                            .testTag("retry_3d_avatar_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "3D 재시도", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "재시도",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildTalkingHeadHtml(avatarUrl: String): String {
    val safeUrl = JSONObject.quote(avatarUrl)
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              overflow: hidden;
              background: radial-gradient(circle at 50% 30%, #1F2640 0%, #121622 52%, #0D0F14 100%);
            }
            #avatar {
              width: 100vw;
              height: 100vh;
            }
          </style>
          <script type="importmap">
            {
              "imports": {
                "three": "https://unpkg.com/three@0.160.0/build/three.module.js",
                "three/addons/": "https://unpkg.com/three@0.160.0/examples/jsm/",
                "talkinghead": "https://unpkg.com/@met4citizen/talkinghead@latest/dist/talkinghead.module.js"
              }
            }
          </script>
        </head>
        <body>
          <div id="avatar"></div>
          <script type="module">
            import { TalkingHead } from "talkinghead";

            const container = document.getElementById("avatar");
            const speechQueue = [];
            let isProcessingQueue = false;
            let head = null;
            let audioCtx = null;
            let loadFinished = false;

            const notifyReady = () => {
              if (!loadFinished) {
                loadFinished = true;
                if (window.Android3DTeacher) window.Android3DTeacher.onAvatarReady();
              }
            };
            const notifyError = () => {
              if (!loadFinished) {
                loadFinished = true;
                if (window.Android3DTeacher) window.Android3DTeacher.onLoadError();
              }
            };

            // 10-second model load timeout guard
            const loadTimeout = setTimeout(() => {
              if (!loadFinished) notifyError();
            }, 10000);

            window.unlockAudioContext = async () => {
              try {
                const AudioContextCls = window.AudioContext || window.webkitAudioContext;
                if (AudioContextCls) {
                  if (!audioCtx) audioCtx = new AudioContextCls();
                  if (audioCtx.state === "suspended") await audioCtx.resume();
                }
                if (head && head.audioCtx && head.audioCtx.state === "suspended") {
                  await head.audioCtx.resume();
                }
              } catch (_) {}
            };

            window.setTeacherMood = (mood) => {
              try {
                if (head && typeof head.setMood === "function") {
                  head.setMood(mood || "neutral");
                }
              } catch (_) {}
            };

            window.setTeacherSpeakingState = (speaking) => {
              if (!speaking && head && typeof head.stopSpeaking === "function") {
                try { head.stopSpeaking(); } catch (_) {}
              }
            };

            window.enqueueTeacherSpeech = (text, mood, rate) => {
              if (!text) return;
              speechQueue.push({ text, mood: mood || "neutral", rate: rate || 1.0 });
              processSpeechQueue();
            };

            async function processSpeechQueue() {
              if (isProcessingQueue || speechQueue.length === 0 || !head) return;
              isProcessingQueue = true;
              const cue = speechQueue.shift();
              try {
                if (typeof head.setMood === "function") head.setMood(cue.mood);
                if (typeof head.speakText === "function") {
                  head.speakText(cue.text);
                }
                const estimatedMs = Math.min(8000, Math.max(1200, (cue.text.split(/\s+/).length * 420) / (cue.rate || 1.0)));
                await new Promise(r => setTimeout(r, estimatedMs));
              } catch (_) {
              } finally {
                isProcessingQueue = false;
                if (speechQueue.length > 0) processSpeechQueue();
              }
            }

            async function init() {
              try {
                head = new TalkingHead(container, {
                  cameraView: "upper",
                  cameraRotateEnable: false,
                  cameraPanEnable: false,
                  cameraZoomEnable: false,
                  lipsyncLang: "en",
                  lipsyncModules: ["en"]
                });

                // Defensive WebGL context-lost handler
                const canvas = container.querySelector("canvas");
                if (canvas) {
                  canvas.addEventListener("webglcontextlost", (e) => {
                    e.preventDefault();
                    if (window.Android3DTeacher) window.Android3DTeacher.onWebGlLost();
                  }, false);
                }

                await head.showAvatar({
                  url: $safeUrl,
                  body: "F",
                  avatarMood: "happy",
                  lipsyncLang: "en"
                });

                clearTimeout(loadTimeout);
                notifyReady();
              } catch (_) {
                clearTimeout(loadTimeout);
                notifyError();
              }
            }

            init();
          </script>
        </body>
        </html>
    """.trimIndent()
}
