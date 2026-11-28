package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.DrawableRes
import com.example.cloudflare.CloudflareApiClient
import com.example.cloudflare.CloudflareConfig
import com.example.ui.components.MotionPhotoGesture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "NanoBananaMotionEngine"

data class NanoBananaMotionResult(
    val sceneBitmap: Bitmap?,
    val blinkBitmap: Bitmap?,
    val errorMessage: String? = null
)

/**
 * Runtime Photorealistic Scene Synthesizer powered by Cloudflare Workers AI
 * (`@cf/black-forest-labs/flux-1-schnell` via `/api/ai/image`), with zero-latency fallback
 * to bundled HD themed scenic backgrounds when offline or unconfigured.
 */
class NanoBananaMotionEngine {

    private val sceneCache = ConcurrentHashMap<String, Bitmap>()
    private val blinkCache = ConcurrentHashMap<String, Bitmap>()

    fun getCachedScene(cacheKey: String): Bitmap? = sceneCache[cacheKey]
    fun getCachedBlink(cacheKey: String): Bitmap? = blinkCache[cacheKey]

    fun buildCacheKey(tutorId: String, topicTag: String, gesture: MotionPhotoGesture): String {
        return "${tutorId}_${normalizeTopicKey(topicTag)}_${gesture.name}"
    }

    suspend fun synthesizeTopicSceneAndBlink(
        context: Context,
        @DrawableRes basePortraitRes: Int,
        tutorId: String,
        tutorName: String,
        topicTag: String,
        gesture: MotionPhotoGesture,
        alsoSynthesizeBlink: Boolean = true
    ): NanoBananaMotionResult {
        val cacheKey = buildCacheKey(tutorId, topicTag, gesture)
        val cachedScene = sceneCache[cacheKey]
        val cachedBlink = blinkCache[cacheKey]
        if (cachedScene != null) {
            return NanoBananaMotionResult(cachedScene, cachedBlink)
        }

        if (!CloudflareConfig.isConfigured()) {
            return NanoBananaMotionResult(
                sceneBitmap = null,
                blinkBitmap = null,
                errorMessage = "기본 HD 테마 배경이 즉시 적용되었습니다. (Cloudflare Worker 연결 시 실시간 AI 배경 생성 지원)"
            )
        }

        val sceneLocationEn = topicToVisualScenePrompt(topicTag)
        val gesturePromptEn = gesture.nanoBananaPromptEn

        val prompt = buildString {
            append("Photorealistic candid front-facing smartphone video-call portrait of friendly English tutor $tutorName, ")
            append("natural lighting, 35mm f/1.8 depth of field, warm real human skin texture. ")
            append("Location background: $sceneLocationEn. ")
            append("Expression: $gesturePromptEn. ")
            append("No microphone, no headset, no text, no watermarks.")
        }

        return try {
            val bitmap = withTimeout(45_000L) {
                CloudflareApiClient.generateImageWithWorkersAi(prompt).getOrThrow()
            }
            sceneCache[cacheKey] = bitmap
            NanoBananaMotionResult(sceneBitmap = bitmap, blinkBitmap = null, errorMessage = null)
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Cloudflare Workers AI image timed out", e)
            NanoBananaMotionResult(null, null, "실사 배경 생성 시간이 초과되어 즉시 HD 테마 배경을 표시합니다.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cloudflare Workers AI image fallback: ${e.message}")
            NanoBananaMotionResult(null, null, "네트워크 상태로 인해 HD 테마 배경 모드로 표시합니다.")
        }
    }

    companion object {
        fun normalizeTopicKey(topicTag: String): String {
            val t = topicTag.lowercase()
            return when {
                t.contains("공항") || t.contains("airport") || t.contains("flight") || t.contains("비행") -> "airport"
                t.contains("카페") || t.contains("cafe") || t.contains("coffee") || t.contains("커피") -> "cafe"
                t.contains("마트") || t.contains("market") || t.contains("shop") || t.contains("쇼핑") -> "mart"
                t.contains("소개") || t.contains("intro") || t.contains("학교") || t.contains("school") -> "intro"
                t.contains("식당") || t.contains("레스토랑") || t.contains("restaurant") || t.contains("음식") -> "restaurant"
                t.contains("호텔") || t.contains("hotel") -> "hotel"
                t.contains("길찾기") || t.contains("direction") || t.contains("subway") || t.contains("지하철") -> "directions"
                t.contains("괌") || t.contains("바다") || t.contains("beach") || t.contains("ocean") -> "guam_beach"
                else -> "nyc_street"
            }
        }

        fun topicToDisplayLocationKo(topicTag: String): String {
            return when (normalizeTopicKey(topicTag)) {
                "airport" -> "국제공항 터미널"
                "cafe" -> "파리 · 맨해튼 감성 카페"
                "mart" -> "런던 쇼핑 · 현지 마트"
                "intro" -> "뉴욕 센트럴파크"
                "restaurant" -> "유럽 브런치 비스트로"
                "hotel" -> "5성급 럭셔리 호텔 로비"
                "directions" -> "도쿄 시부야 거리 길찾기"
                "guam_beach" -> "괌 투몬 바닷가"
                else -> "뉴욕 타임스스퀘어"
            }
        }

        fun topicToVisualScenePrompt(topicTag: String): String {
            return when (normalizeTopicKey(topicTag)) {
                "airport" -> "a bright modern international airport departure terminal with sunlit glass windows, airplanes outside, and softly blurred gate architecture in creamy bokeh"
                "cafe" -> "a warm cozy artisan coffee shop interior with espresso bar lights, wooden shelves, and sunlit window bokeh"
                "mart" -> "a bright modern London shopping street and lifestyle boutique with warm storefront lights in soft bokeh"
                "intro" -> "sunlit New York Central Park green lawn with golden-hour sunlight filtering through lush trees and city skyline softly blurred in the distance"
                "restaurant" -> "a lively warm European bistro restaurant table with soft fairy lights and cozy sunlit ambiance"
                "hotel" -> "an upscale sunlit 5-star modern hotel lobby with marble pillars, warm lamps, and tropical plants in soft bokeh"
                "directions" -> "vibrant Tokyo Shibuya twilight crossing with crisp warm bokeh street lights"
                "guam_beach" -> "tropical turquoise Tumon Beach in Guam with bright blue ocean waves, palm trees, and sunny white sand"
                else -> "vibrant sunlit New York City Times Square street with yellow cabs and colorful city lights softly blurred in bokeh"
            }
        }
    }
}
