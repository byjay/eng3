package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.NightBackground
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import org.json.JSONObject

/**
 * Upgraded SVG + CSS Character Rig:
 * - Renders an editorial multi-layer SVG avatar customized per tutor
 * - Smooth CSS Idle animation (breathing, micro-sway, natural eye blinking, hair drift)
 * - AUTOMATIC IDLE PAUSE: When user voice input starts (`isListening`) or tutor speaks (`isSpeaking`),
 *   idle CSS animations immediately pause (`animation-play-state: paused`) and a multi-viseme
 *   CSS Lip-Sync overlay smoothly cross-fades over the mouth anchor
 * - Real-time JS bridge drives CSS custom properties (`--voice-level`, `--lip-speed`) from
 *   live microphone RMS dB (`userRmsDb`) and TTS cadence (`speechBeatIndex` / `speechRate`)
 * - Active thinking state (`isThinking`) shifts eye gaze & pulses a subtle neural halo
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SvgCssLipSyncRigView(
    tutorId: String,
    tutorName: String,
    learnerName: String,
    activeTopicTag: String,
    activeGesture: MotionPhotoGesture,
    aiMood: String,
    liveThoughtKo: String,
    isSpeaking: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
    speechRate: Float,
    speechBeatIndex: Int,
    userRmsDb: Float,
    transparentBackdrop: Boolean = false,
    compactOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }

    val rigStateName = when {
        isListening -> "listening"
        isSpeaking -> "speaking"
        isThinking -> "thinking"
        else -> "idle"
    }

    val normalizedVoiceLevel = remember(isListening, isSpeaking, userRmsDb, speechBeatIndex) {
        when {
            isListening -> ((userRmsDb + 2f) / 11f).coerceIn(0.18f, 1.0f)
            isSpeaking -> {
                val pattern = floatArrayOf(0.82f, 0.45f, 0.96f, 0.60f, 0.88f, 0.35f, 0.92f)
                pattern[Math.floorMod(speechBeatIndex, pattern.size)]
            }
            else -> 0f
        }
    }

    val resolvedMood = remember(aiMood, activeGesture, isThinking) {
        when {
            isThinking || activeGesture == MotionPhotoGesture.THOUGHTFUL_MUSE -> "thinking"
            aiMood.isNotBlank() -> aiMood.lowercase()
            activeGesture == MotionPhotoGesture.CURIOUS_TILT -> "curious"
            activeGesture == MotionPhotoGesture.CLAPPING_BRAVO ||
                activeGesture == MotionPhotoGesture.EXCITED_BOUNCE ||
                activeGesture == MotionPhotoGesture.AMAZED_SPARKLE -> "excited"
            else -> "happy"
        }
    }

    val htmlPayload = remember(tutorId, transparentBackdrop, compactOverlay) {
        buildSvgCssRigHtml(tutorId, transparentBackdrop, compactOverlay)
    }

    // Push real-time Rig state, voice level, lip-sync cadence, and mood into the SVG/CSS layer
    LaunchedEffect(pageReady, rigStateName, normalizedVoiceLevel, speechRate, resolvedMood, speechBeatIndex) {
        if (!pageReady) return@LaunchedEffect
        val js = buildString {
            append("window.updateSvgRig && window.updateSvgRig(")
            append(JSONObject.quote(rigStateName)).append(", ")
            append(String.format(java.util.Locale.US, "%.3f", normalizedVoiceLevel)).append(", ")
            append(String.format(java.util.Locale.US, "%.2f", speechRate.coerceIn(0.6f, 1.5f))).append(", ")
            append(JSONObject.quote(resolvedMood)).append(", ")
            append(speechBeatIndex)
            append(");")
        }
        webViewRef?.evaluateJavascript(js, null)
    }

    val reloadKey = "$tutorId|$transparentBackdrop|$compactOverlay"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (transparentBackdrop) Color.Transparent else NightBackground)
            .testTag("svg_css_lipsync_rig_stage")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageReady = true
                        }
                    }
                    webViewRef = this
                    tag = reloadKey
                    loadDataWithBaseURL("https://svgrig.local/", htmlPayload, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webViewRef = webView
                if (webView.tag != reloadKey) {
                    pageReady = false
                    webView.tag = reloadKey
                    webView.loadDataWithBaseURL("https://svgrig.local/", htmlPayload, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Compact Rig State & Active Thought Floating Capsule (non-blocking)
        AnimatedVisibility(
            visible = !compactOverlay && (isListening || isSpeaking || isThinking || liveThoughtKo.isNotBlank()),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 152.dp, start = 20.dp, end = 20.dp)
        ) {
            val pillAccent = when {
                isListening -> StudioCoral
                isSpeaking -> StudioCobalt
                isThinking -> StudioIris
                else -> GlacierCyan
            }
            val pillText = when {
                isListening -> "${learnerName}님 음성 감지 · Idle 일시정지 & 립싱크 오버레이"
                isThinking -> "$tutorName 능동 사고 중 · ${activeTopicTag} 대화 구상"
                isSpeaking -> "$tutorName 립싱크 발화 중"
                liveThoughtKo.isNotBlank() -> liveThoughtKo
                else -> ""
            }
            if (pillText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xCC0D0F14),
                    border = BorderStroke(1.dp, pillAccent.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("svg_rig_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(pillAccent)
                        )
                        Icon(
                            imageVector = if (isThinking || (!isListening && !isSpeaking)) {
                                Icons.Outlined.Psychology
                            } else {
                                Icons.Outlined.GraphicEq
                            },
                            contentDescription = null,
                            tint = pillAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = pillText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class SvgTutorPalette(
    val hairMain: String,
    val hairShadow: String,
    val skinLight: String,
    val skinShade: String,
    val outfitMain: String,
    val outfitAccent: String,
    val irisColor: String,
    val lipColor: String,
    val glowPrimary: String,
    val glowSecondary: String,
    val isShortHair: Boolean
)

private fun paletteForTutor(tutorId: String): SvgTutorPalette {
    return when {
        tutorId.contains("liam", ignoreCase = true) || tutorId.contains("noah", ignoreCase = true) -> SvgTutorPalette(
            hairMain = "#262B38",
            hairShadow = "#151924",
            skinLight = "#F6D3BC",
            skinShade = "#E3B295",
            outfitMain = "#1E2A4A",
            outfitAccent = "#4F6EF7",
            irisColor = "#3B5998",
            lipColor = "#C9756B",
            glowPrimary = "#4F6EF7",
            glowSecondary = "#0EA5E9",
            isShortHair = true
        )
        tutorId.contains("sophia", ignoreCase = true) || tutorId.contains("chloe", ignoreCase = true) -> SvgTutorPalette(
            hairMain = "#5B3A29",
            hairShadow = "#3B2216",
            skinLight = "#F9DAC4",
            skinShade = "#E7B79B",
            outfitMain = "#2B2242",
            outfitAccent = "#8B7CFF",
            irisColor = "#5165A8",
            lipColor = "#D96A73",
            glowPrimary = "#8B7CFF",
            glowSecondary = "#FF7A59",
            isShortHair = false
        )
        else -> SvgTutorPalette(
            hairMain = "#3A2622",
            hairShadow = "#211412",
            skinLight = "#FBDDC8",
            skinShade = "#E8B99C",
            outfitMain = "#1B2438",
            outfitAccent = "#4F6EF7",
            irisColor = "#3F5AA6",
            lipColor = "#E06D75",
            glowPrimary = "#4F6EF7",
            glowSecondary = "#FF7A59",
            isShortHair = false
        )
    }
}

private fun buildSvgCssRigHtml(
    tutorId: String,
    transparentBackdrop: Boolean = false,
    compactOverlay: Boolean = false
): String {
    val p = paletteForTutor(tutorId)
    val bodyBgCss = if (transparentBackdrop) {
        "transparent"
    } else {
        "radial-gradient(circle at 50% 34%, #1A2238 0%, #111520 52%, #0D0F14 100%)"
    }
    val stageSizeCss = if (compactOverlay) {
        "width: 96%; height: 96%; margin-bottom: 0;"
    } else {
        "width: min(92vw, 390px); height: min(92vw, 390px); margin-bottom: 138px;"
    }
    val backHairSvg = if (p.isShortHair) {
        """
        <path d="M 118 170 C 115 100, 285 100, 282 170 L 274 218 C 250 198, 150 198, 126 218 Z" fill="${p.hairShadow}" />
        """.trimIndent()
    } else {
        """
        <path d="M 102 180 C 96 86, 304 86, 298 180 C 314 275, 312 360, 286 410 C 245 426, 155 426, 114 410 C 88 360, 86 275, 102 180 Z" fill="${p.hairShadow}" />
        """.trimIndent()
    }
    val bangsSvg = if (p.isShortHair) {
        """
        <path d="M 122 175 C 124 106, 276 106, 278 175 C 258 148, 224 140, 198 148 C 174 138, 144 148, 122 175 Z" fill="${p.hairMain}" />
        <path d="M 134 150 C 160 120, 222 114, 260 144" stroke="rgba(255,255,255,0.14)" stroke-width="4" stroke-linecap="round" fill="none" />
        """.trimIndent()
    } else {
        """
        <path d="M 116 184 C 116 100, 284 100, 284 184 C 266 154, 240 138, 210 146 C 186 156, 150 142, 116 184 Z" fill="${p.hairMain}" />
        <path d="M 112 182 C 102 240, 108 302, 128 345 C 122 292, 126 238, 134 198 Z" fill="${p.hairMain}" />
        <path d="M 288 182 C 298 240, 292 302, 272 345 C 278 292, 274 238, 266 198 Z" fill="${p.hairMain}" />
        <path d="M 142 142 C 178 112, 232 114, 264 146" stroke="rgba(255,255,255,0.16)" stroke-width="5" stroke-linecap="round" fill="none" />
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            :root {
              --voice-level: 0;
              --lip-speed: 0.34s;
              --glow-primary: ${p.glowPrimary};
              --glow-secondary: ${p.glowSecondary};
            }

            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              overflow: hidden;
              background: $bodyBgCss;
              display: flex;
              align-items: center;
              justify-content: center;
            }

            .rig-stage {
              position: relative;
              $stageSizeCss
              display: flex;
              align-items: center;
              justify-content: center;
            }

            .ambient-ring {
              position: absolute;
              width: 84%;
              height: 84%;
              border-radius: 50%;
              background: radial-gradient(circle, rgba(79,110,247,0.22) 0%, rgba(14,165,233,0.06) 58%, transparent 74%);
              transition: transform 0.22s ease, opacity 0.25s ease, box-shadow 0.25s ease;
              pointer-events: none;
            }

            .voice-ripple {
              position: absolute;
              width: 78%;
              height: 78%;
              border-radius: 50%;
              border: 2px solid rgba(255, 122, 89, 0.0);
              transform: scale(0.96);
              opacity: 0;
              pointer-events: none;
              transition: opacity 0.18s ease;
            }

            svg#tutor-rig {
              width: 100%;
              height: 100%;
              overflow: visible;
              filter: drop-shadow(0 18px 34px rgba(0, 0, 0, 0.45));
            }

            /* === 1. IDLE ANIMATIONS (Paused automatically during voice input / speech!) === */
            #rig-idle-group {
              transform-origin: 200px 330px;
              animation: rig-idle-breathe 4.4s ease-in-out infinite;
            }

            #rig-head-group {
              transform-origin: 200px 260px;
              animation: rig-idle-sway 6.2s ease-in-out infinite;
              transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
            }

            #rig-hair-back {
              transform-origin: 200px 180px;
              animation: rig-hair-drift 5.6s ease-in-out infinite;
            }

            #rig-eyelids {
              transform-origin: 200px 206px;
              animation: rig-eye-blink 4.8s ease-in-out infinite;
            }

            @keyframes rig-idle-breathe {
              0%, 100% { transform: translateY(0px) scaleY(1); }
              50% { transform: translateY(-4.5px) scaleY(1.012); }
            }

            @keyframes rig-idle-sway {
              0%, 100% { transform: rotate(0deg) translateX(0px); }
              30% { transform: rotate(1.4deg) translateX(1.5px); }
              70% { transform: rotate(-1.3deg) translateX(-1.5px); }
            }

            @keyframes rig-hair-drift {
              0%, 100% { transform: rotate(0deg); }
              50% { transform: rotate(1.1deg); }
            }

            @keyframes rig-eye-blink {
              0%, 44%, 48%, 94%, 98%, 100% { transform: scaleY(0.02); opacity: 0; }
              46%, 96% { transform: scaleY(1); opacity: 1; }
            }

            /* === 2. PAUSE IDLE ON VOICE INPUT (LISTENING) OR SPEAKING === */
            .rig-stage.state-listening #rig-idle-group,
            .rig-stage.state-listening #rig-head-group,
            .rig-stage.state-listening #rig-hair-back,
            .rig-stage.state-speaking #rig-idle-group,
            .rig-stage.state-speaking #rig-head-group,
            .rig-stage.state-speaking #rig-hair-back {
              animation-play-state: paused !important;
            }

            /* Attentive forward lean on user voice input */
            .rig-stage.state-listening #rig-head-active-offset {
              transform: translateY(3px) scale(1.03);
            }

            .rig-stage.state-listening .voice-ripple {
              opacity: 1;
              border-color: rgba(255, 122, 89, 0.55);
              animation: voice-pulse-ring 1.15s ease-out infinite;
            }

            .rig-stage.state-speaking .ambient-ring {
              box-shadow: 0 0 42px rgba(79, 110, 247, 0.38);
              transform: scale(calc(1.0 + var(--voice-level) * 0.08));
            }

            @keyframes voice-pulse-ring {
              0% { transform: scale(0.94); opacity: 0.72; }
              100% { transform: scale(1.14); opacity: 0; }
            }

            /* === 3. PROACTIVE THINKING STATE === */
            .rig-stage.state-thinking #rig-idle-group {
              animation-play-state: paused !important;
            }
            .rig-stage.state-thinking #rig-pupils {
              transform: translate(5px, -4px);
            }
            .rig-stage.state-thinking #rig-brows {
              transform: translateY(-3px) rotate(-2deg);
            }
            .rig-stage.state-thinking .ambient-ring {
              background: radial-gradient(circle, rgba(139,124,255,0.34) 0%, rgba(79,110,247,0.10) 58%, transparent 74%);
              animation: think-glow 1.3s ease-in-out infinite alternate;
            }
            @keyframes think-glow {
              from { transform: scale(0.97); opacity: 0.75; }
              to { transform: scale(1.06); opacity: 1; }
            }

            /* === 4. NATURAL CSS LIP-SYNC OVERLAY === */
            #mouth-idle-rest {
              transition: opacity 0.14s ease, transform 0.14s ease;
              transform-origin: 200px 264px;
            }

            #mouth-lipsync-overlay {
              opacity: 0;
              transform-origin: 200px 264px;
              transition: opacity 0.12s ease;
              pointer-events: none;
            }

            /* When listening (user voice input) or speaking (tutor TTS), hide rest mouth & overlay CSS lip-sync */
            .rig-stage.state-listening #mouth-idle-rest,
            .rig-stage.state-speaking #mouth-idle-rest {
              opacity: 0;
            }

            .rig-stage.state-listening #mouth-lipsync-overlay,
            .rig-stage.state-speaking #mouth-lipsync-overlay {
              opacity: 1;
            }

            /* Layered CSS keyframe viseme morphing (cavity, teeth, tongue, outer lip contour) */
            #lipsync-cavity {
              transform-origin: 200px 262px;
            }
            #lipsync-upper-teeth {
              transform-origin: 200px 258px;
            }
            #lipsync-tongue {
              transform-origin: 200px 270px;
            }
            #lipsync-outer-lip {
              transform-origin: 200px 263px;
            }

            .rig-stage.state-listening #lipsync-cavity,
            .rig-stage.state-speaking #lipsync-cavity {
              animation: css-lipsync-cavity var(--lip-speed) cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
            }
            .rig-stage.state-listening #lipsync-outer-lip,
            .rig-stage.state-speaking #lipsync-outer-lip {
              animation: css-lipsync-outer var(--lip-speed) cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
            }
            .rig-stage.state-listening #lipsync-tongue,
            .rig-stage.state-speaking #lipsync-tongue {
              animation: css-lipsync-tongue var(--lip-speed) ease-in-out infinite;
            }
            .rig-stage.state-listening #lipsync-upper-teeth,
            .rig-stage.state-speaking #lipsync-upper-teeth {
              animation: css-lipsync-teeth var(--lip-speed) ease-in-out infinite;
            }

            /* Voice-reactive vertical mouth amplitude driven by live RMS / TTS beat */
            #mouth-amplitude-wrap {
              transform-origin: 200px 263px;
              transition: transform 0.07s linear;
            }

            @keyframes css-lipsync-cavity {
              0%   { transform: scaleX(0.74) scaleY(0.24); }
              18%  { transform: scaleX(1.08) scaleY(0.95); }
              36%  { transform: scaleX(0.68) scaleY(0.76); }
              54%  { transform: scaleX(1.16) scaleY(0.54); }
              72%  { transform: scaleX(0.84) scaleY(1.06); }
              88%  { transform: scaleX(0.95) scaleY(0.36); }
              100% { transform: scaleX(0.74) scaleY(0.24); }
            }

            @keyframes css-lipsync-outer {
              0%   { transform: scaleX(0.88) scaleY(0.55); }
              18%  { transform: scaleX(1.06) scaleY(1.02); }
              36%  { transform: scaleX(0.78) scaleY(0.92); }
              54%  { transform: scaleX(1.14) scaleY(0.72); }
              72%  { transform: scaleX(0.90) scaleY(1.08); }
              88%  { transform: scaleX(0.96) scaleY(0.62); }
              100% { transform: scaleX(0.88) scaleY(0.55); }
            }

            @keyframes css-lipsync-tongue {
              0%, 100% { transform: translateY(2px) scaleY(0.55); opacity: 0.45; }
              35%      { transform: translateY(-1.5px) scaleY(1.05); opacity: 0.95; }
              72%      { transform: translateY(0px) scaleY(0.9); opacity: 0.85; }
            }

            @keyframes css-lipsync-teeth {
              0%, 100% { opacity: 0.35; transform: translateY(0px); }
              20%, 60% { opacity: 0.98; transform: translateY(-0.5px); }
            }

            /* === 5. MOOD MODIFIERS === */
            #rig-head-active-offset, #rig-pupils, #rig-brows, #rig-blush {
              transition: transform 0.25s ease, opacity 0.25s ease;
              transform-origin: 200px 210px;
            }
            .rig-stage.mood-excited #rig-brows { transform: translateY(-4px); }
            .rig-stage.mood-excited #rig-blush { opacity: 0.85; }
            .rig-stage.mood-curious #rig-head-active-offset { transform: rotate(4deg); }
            .rig-stage.mood-curious #rig-brows { transform: translateY(-2px) rotate(3deg); }
            .rig-stage.mood-proud #rig-blush { opacity: 0.72; }
            .rig-stage.mood-thinking #rig-pupils { transform: translate(5px, -4px); }

            /* Proactive Thinking Orbit Dots */
            #thinking-orbit {
              opacity: 0;
              transition: opacity 0.22s ease;
            }
            .rig-stage.state-thinking #thinking-orbit {
              opacity: 1;
            }
            .think-dot {
              animation: think-dot-bounce 0.95s ease-in-out infinite;
            }
            .think-dot.d2 { animation-delay: 0.18s; }
            .think-dot.d3 { animation-delay: 0.36s; }
            @keyframes think-dot-bounce {
              0%, 100% { transform: translateY(0px); opacity: 0.45; }
              50% { transform: translateY(-6px); opacity: 1; }
            }
          </style>
        </head>
        <body>
          <div id="rig-stage" class="rig-stage state-idle mood-happy">
            <div class="ambient-ring"></div>
            <div class="voice-ripple"></div>

            <svg id="tutor-rig" viewBox="0 0 400 440" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="skinGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="${p.skinLight}" />
                  <stop offset="100%" stop-color="${p.skinShade}" />
                </linearGradient>
                <linearGradient id="outfitGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="${p.outfitMain}" />
                  <stop offset="100%" stop-color="#0F1422" />
                </linearGradient>
                <radialGradient id="blushGrad" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#FF7A7A" stop-opacity="0.48" />
                  <stop offset="100%" stop-color="#FF7A7A" stop-opacity="0" />
                </radialGradient>
                <clipPath id="mouthClip">
                  <ellipse cx="200" cy="264" rx="22" ry="15" />
                </clipPath>
              </defs>

              <g id="rig-idle-group">
                <!-- Back Hair Layer -->
                <g id="rig-hair-back">
                  $backHairSvg
                </g>

                <!-- Shoulders & Editorial Studio Outfit -->
                <g id="rig-torso">
                  <path d="M 88 430 C 94 352, 138 326, 200 326 C 262 326, 306 352, 312 430 Z" fill="url(#outfitGrad)" />
                  <path d="M 164 328 L 200 374 L 236 328" fill="none" stroke="${p.outfitAccent}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M 182 328 L 200 356 L 218 328" fill="rgba(255,255,255,0.10)" />
                  <!-- Neck -->
                  <rect x="178" y="284" width="44" height="52" rx="20" fill="${p.skinShade}" />
                  <path d="M 178 296 Q 200 314 222 296 L 222 284 L 178 284 Z" fill="rgba(120,62,48,0.16)" />
                </g>

                <!-- Head & Facial Rig -->
                <g id="rig-head-group">
                  <g id="rig-head-active-offset">
                    <!-- Ears -->
                    <ellipse cx="122" cy="216" rx="11" ry="18" fill="${p.skinShade}" />
                    <ellipse cx="278" cy="216" rx="11" ry="18" fill="${p.skinShade}" />

                    <!-- Face Contour -->
                    <path d="M 126 196 C 126 132, 274 132, 274 196 C 274 260, 244 302, 200 304 C 156 302, 126 260, 126 196 Z" fill="url(#skinGrad)" />

                    <!-- Soft Studio Blush -->
                    <g id="rig-blush" opacity="0.52">
                      <ellipse cx="156" cy="238" rx="20" ry="11" fill="url(#blushGrad)" />
                      <ellipse cx="244" cy="238" rx="20" ry="11" fill="url(#blushGrad)" />
                    </g>

                    <!-- Eyebrows -->
                    <g id="rig-brows">
                      <path d="M 148 184 Q 165 175 182 182" stroke="${p.hairMain}" stroke-width="4.2" stroke-linecap="round" fill="none" />
                      <path d="M 218 182 Q 235 175 252 184" stroke="${p.hairMain}" stroke-width="4.2" stroke-linecap="round" fill="none" />
                    </g>

                    <!-- Eyes (Sclera + Irises + Specular Highlights) -->
                    <g id="rig-eyes">
                      <ellipse cx="166" cy="208" rx="15" ry="12" fill="#FFFFFF" />
                      <ellipse cx="234" cy="208" rx="15" ry="12" fill="#FFFFFF" />
                      <g id="rig-pupils">
                        <circle cx="167" cy="208" r="8.2" fill="${p.irisColor}" />
                        <circle cx="167" cy="208" r="4.2" fill="#111622" />
                        <circle cx="170" cy="204.5" r="2.3" fill="#FFFFFF" />
                        <circle cx="164" cy="211" r="1.1" fill="rgba(255,255,255,0.65)" />

                        <circle cx="233" cy="208" r="8.2" fill="${p.irisColor}" />
                        <circle cx="233" cy="208" r="4.2" fill="#111622" />
                        <circle cx="236" cy="204.5" r="2.3" fill="#FFFFFF" />
                        <circle cx="230" cy="211" r="1.1" fill="rgba(255,255,255,0.65)" />
                      </g>
                      <!-- Upper Lash Accent -->
                      <path d="M 150 205 Q 166 193 182 205" stroke="#1E1A22" stroke-width="3.2" stroke-linecap="round" fill="none" />
                      <path d="M 218 205 Q 234 193 250 205" stroke="#1E1A22" stroke-width="3.2" stroke-linecap="round" fill="none" />
                    </g>

                    <!-- Natural Blink Eyelids -->
                    <g id="rig-eyelids">
                      <ellipse cx="166" cy="208" rx="16" ry="13" fill="${p.skinShade}" />
                      <ellipse cx="234" cy="208" rx="16" ry="13" fill="${p.skinShade}" />
                      <path d="M 151 209 Q 166 216 181 209" stroke="#1E1A22" stroke-width="2.6" stroke-linecap="round" fill="none" />
                      <path d="M 219 209 Q 234 216 249 209" stroke="#1E1A22" stroke-width="2.6" stroke-linecap="round" fill="none" />
                    </g>

                    <!-- Delicate Nose -->
                    <path d="M 198 220 Q 194 233 200 236 Q 206 236 205 231" stroke="rgba(145,86,68,0.45)" stroke-width="2.5" stroke-linecap="round" fill="none" />

                    <!-- REST MOUTH (Visible during Idle & Thinking) -->
                    <g id="mouth-idle-rest">
                      <path d="M 183 262 Q 200 273 217 262" stroke="${p.lipColor}" stroke-width="3.8" stroke-linecap="round" fill="none" />
                      <path d="M 189 266 Q 200 271 211 266" stroke="rgba(255,255,255,0.35)" stroke-width="1.4" stroke-linecap="round" fill="none" />
                    </g>

                    <!-- CSS LIP-SYNC OVERLAY (Cross-fades cleanly on Voice Input / Speaking while Idle is paused!) -->
                    <g id="mouth-lipsync-overlay">
                      <g id="mouth-amplitude-wrap">
                        <!-- Outer Lip Contour -->
                        <ellipse id="lipsync-outer-lip" cx="200" cy="264" rx="20" ry="12.5" fill="${p.lipColor}" />
                        <!-- Inner Mouth Cavity -->
                        <ellipse id="lipsync-cavity" cx="200" cy="264" rx="16" ry="10.2" fill="#521C25" />
                        <!-- Upper Teeth Highlight -->
                        <rect id="lipsync-upper-teeth" x="188" y="255.5" width="24" height="4.8" rx="2.4" fill="#FFFDF9" clip-path="url(#mouthClip)" />
                        <!-- Expressive Tongue -->
                        <ellipse id="lipsync-tongue" cx="200" cy="271.5" rx="10.5" ry="5.5" fill="#E36E7B" clip-path="url(#mouthClip)" />
                      </g>
                    </g>

                    <!-- Front Hair & Bangs -->
                    <g id="rig-hair-front">
                      $bangsSvg
                    </g>
                  </g>
                </g>

                <!-- Proactive Thinking Floating Dots (Top-Right of Head) -->
                <g id="thinking-orbit">
                  <rect x="272" y="102" width="66" height="30" rx="15" fill="rgba(13,15,20,0.82)" stroke="#8B7CFF" stroke-width="1.6" />
                  <circle class="think-dot d1" cx="290" cy="117" r="4" fill="#8B7CFF" />
                  <circle class="think-dot d2" cx="305" cy="117" r="4" fill="#4F6EF7" />
                  <circle class="think-dot d3" cx="320" cy="117" r="4" fill="#0EA5E9" />
                </g>
              </g>
            </svg>
          </div>

          <script>
            const stageEl = document.getElementById("rig-stage");
            const ampWrap = document.getElementById("mouth-amplitude-wrap");

            window.updateSvgRig = function(stateName, voiceLevel, speechRate, moodName, beatIdx) {
              if (!stageEl) return;
              const safeState = ["idle", "listening", "speaking", "thinking"].includes(stateName) ? stateName : "idle";
              const safeMood = ["happy", "curious", "excited", "proud", "thinking", "neutral"].includes(moodName) ? moodName : "happy";

              stageEl.className = "rig-stage state-" + safeState + " mood-" + safeMood;

              const clampedLevel = Math.max(0, Math.min(1, Number(voiceLevel) || 0));
              const safeRate = Math.max(0.6, Math.min(1.5, Number(speechRate) || 1.0));
              const lipPeriodSec = (0.38 / safeRate).toFixed(2) + "s";

              document.documentElement.style.setProperty("--voice-level", clampedLevel.toFixed(3));
              document.documentElement.style.setProperty("--lip-speed", lipPeriodSec);

              if (ampWrap) {
                if (safeState === "listening" || safeState === "speaking") {
                  // Modulate lip-sync overlay amplitude naturally by live voice RMS / speech beat
                  const scaleY = 0.62 + (clampedLevel * 0.64);
                  const scaleX = 0.90 + (clampedLevel * 0.22);
                  ampWrap.style.transform = "scale(" + scaleX.toFixed(3) + ", " + scaleY.toFixed(3) + ")";
                } else {
                  ampWrap.style.transform = "scale(1, 1)";
                }
              }
            };
          </script>
        </body>
        </html>
    """.trimIndent()
}
