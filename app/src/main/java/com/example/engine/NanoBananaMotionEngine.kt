package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import com.example.ui.components.MotionPhotoGesture
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "NanoBananaMotionEngine"

data class NanoBananaMotionResult(
    val sceneBitmap: Bitmap?,
    val blinkBitmap: Bitmap?,
    val errorMessage: String? = null
)

/**
 * Runtime Photorealistic Scene & Motion-Photo Frame Synthesizer powered by
 * Firebase AI Logic `gemini-nano-banana-2.1` (Image-to-Image editing).
 *
 * - Keeps the EXACT same English video-call friend's face, hairstyle, and warm 3D style
 *   with ZERO microphones/headsets and ZERO text overlays.
 * - Seamlessly changes the background to match the active conversation topic
 *   (Airport, Cafe, Supermarket, Self-Intro Park, NYC Times Square, Guam Beach).
 * - Synthesizes a matching photorealistic closed-eye blink frame for 100% natural eye blinking.
 */
class NanoBananaMotionEngine {

    private val imageModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

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
        if (cachedScene != null && (!alsoSynthesizeBlink || cachedBlink != null)) {
            return NanoBananaMotionResult(cachedScene, cachedBlink)
        }

        val baseBitmap = withContext(Dispatchers.IO) {
            decodeScaledPortrait(context, basePortraitRes, maxDim = 768)
        } ?: return NanoBananaMotionResult(
            sceneBitmap = null,
            blinkBitmap = null,
            errorMessage = "기본 캐릭터 이미지를 불러오지 못했습니다."
        )

        val sceneLocationEn = topicToVisualScenePrompt(topicTag)
        val gesturePromptEn = gesture.nanoBananaPromptEn

        val sceneBitmap = if (cachedScene != null) {
            cachedScene
        } else {
            val editInstruction = buildString {
                append("Edit this portrait of $tutorName for a live smartphone video call. ")
                append("PRESERVE the exact same young person's face, eyes, hairstyle, skin tone, clothing, and warm Pixar-like 3D realism. ")
                append("Change the background seamlessly to: $sceneLocationEn. ")
                append("Adjust their natural pose and facial expression to: $gesturePromptEn. ")
                append("Natural eye-level front camera framing, matching ambient lighting on face and hair. ")
                append("CRITICAL RULES: NO microphone, NO headset, NO earphones, NO text, NO logos, NO UI bars, NO watermarks.")
            }
            val gen = runImageEdit(baseBitmap, editInstruction)
            if (gen.first == null) {
                return NanoBananaMotionResult(null, null, gen.second)
            }
            sceneCache[cacheKey] = gen.first!!
            gen.first!!
        }

        val blinkBitmap = if (!alsoSynthesizeBlink) {
            cachedBlink
        } else if (cachedBlink != null) {
            cachedBlink
        } else {
            val blinkPrompt =
                "Keep EVERYTHING in this exact image identical — exact same person, head angle, smile, hair, outfit, lighting, and background — " +
                    "EXCEPT gently close both of their eyes naturally mid-blink with soft natural eyelashes. " +
                    "Do not change anything else. No text, no UI."
            val blinkGen = runImageEdit(sceneBitmap, blinkPrompt)
            if (blinkGen.first != null) {
                blinkCache[cacheKey] = blinkGen.first!!
            }
            blinkGen.first
        }

        return NanoBananaMotionResult(
            sceneBitmap = sceneBitmap,
            blinkBitmap = blinkBitmap,
            errorMessage = null
        )
    }

    private suspend fun runImageEdit(
        inputBitmap: Bitmap,
        instruction: String
    ): Pair<Bitmap?, String?> {
        return try {
            val response = withTimeout(60_000L) {
                imageModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(instruction)
                    }
                )
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            val outBitmap = imagePart?.image
            if (outBitmap != null) {
                outBitmap to null
            } else {
                null to "나노바나나 모델이 이미지를 반환하지 않았습니다."
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Nano Banana image edit timed out", e)
            null to "나노바나나 배경·표정 생성 시간이 초과되었습니다 (60초)."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Nano Banana image edit failed", e)
            null to (e.localizedMessage ?: "나노바나나 이미지 생성에 실패했습니다.")
        }
    }

    private fun decodeScaledPortrait(
        context: Context,
        @DrawableRes resId: Int,
        maxDim: Int
    ): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, opts)
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
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
                t.contains("괌") || t.contains("바다") || t.contains("beach") || t.contains("ocean") -> "guam_beach"
                else -> "nyc_street"
            }
        }

        fun topicToDisplayLocationKo(topicTag: String): String {
            return when (normalizeTopicKey(topicTag)) {
                "airport" -> "국제공항 터미널"
                "cafe" -> "맨해튼 감성 카페"
                "mart" -> "미국 현지 마트"
                "intro" -> "뉴욕 센트럴파크"
                "restaurant" -> "뉴욕 브런치 식당"
                "hotel" -> "럭셔리 호텔 로비"
                "guam_beach" -> "괌 투몬 바닷가"
                else -> "뉴욕 타임스스퀘어"
            }
        }

        fun topicToVisualScenePrompt(topicTag: String): String {
            return when (normalizeTopicKey(topicTag)) {
                "airport" -> "a bright modern international airport departure terminal with sunlit glass windows, airplanes outside, and softly blurred gate architecture in creamy bokeh"
                "cafe" -> "a warm cozy Manhattan artisan coffee shop interior with espresso bar lights, wooden shelves, and sunlit window bokeh"
                "mart" -> "a bright modern American grocery supermarket aisle with colorful fresh fruits, produce displays, and clean warm overhead lights in soft bokeh"
                "intro" -> "sunlit New York Central Park green lawn with golden-hour sunlight filtering through lush trees and city skyline softly blurred in the distance"
                "restaurant" -> "a lively warm New York bistro restaurant table with soft fairy lights and cozy sunlit ambiance"
                "hotel" -> "an upscale sunlit modern hotel lobby with marble pillars, warm lamps, and tropical plants in soft bokeh"
                "guam_beach" -> "tropical turquoise Tumon Beach in Guam with bright blue ocean waves, palm trees, and sunny white sand"
                else -> "vibrant sunlit New York City Times Square street with yellow cabs and colorful city lights softly blurred in bokeh"
            }
        }
    }
}
