package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CloudflareLevelPayload(
    val daughterName: String,
    val vocabGradeTier: Int, // 1..5 (1등급 최고 ~ 5등급 기초)
    val princessLevel: Int,
    val castleHpPercent: Int,
    val masteredWordCount: Int,
    val accumulatedApiWordCount: Int,
    val bossVictories: Int,
    val totalXp: Int
)

object CloudflareLevelSyncService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun resolveWorkerUrl(customUrl: String): String {
        val candidate = customUrl.trim().ifBlank { BuildConfig.CLOUDFLARE_WORKER_URL.trim() }
        return if (candidate.isBlank() || candidate.contains("your-worker.your-subdomain")) {
            ""
        } else {
            candidate.trimEnd('/')
        }
    }

    suspend fun registerLevelToCloudflare(
        customWorkerUrl: String,
        payload: CloudflareLevelPayload
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = resolveWorkerUrl(customWorkerUrl)
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Cloudflare Worker URL을 입력하거나 AI Studio Secrets에 CLOUDFLARE_WORKER_URL을 등록해 주세요.")
            )
        }
        runCatching {
            val endpoint = if (baseUrl.endsWith("/levels")) baseUrl else "$baseUrl/api/levels"
            val bodyJson = JSONObject().apply {
                put("daughterName", payload.daughterName)
                put("vocabGradeTier", payload.vocabGradeTier)
                put("princessLevel", payload.princessLevel)
                put("castleHpPercent", payload.castleHpPercent)
                put("masteredWordCount", payload.masteredWordCount)
                put("accumulatedApiWordCount", payload.accumulatedApiWordCount)
                put("bossVictories", payload.bossVictories)
                put("totalXp", payload.totalXp)
                put("registeredAt", System.currentTimeMillis())
            }

            val reqBuilder = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

            val token = BuildConfig.CLOUDFLARE_API_TOKEN.trim()
            if (token.isNotBlank() && token != "YOUR_CLOUDFLARE_API_TOKEN") {
                reqBuilder.header("Authorization", "Bearer $token")
            }

            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Cloudflare 서버 오류 (HTTP ${resp.code}): ${raw.take(80)}")
                }
                "Cloudflare 등록 완료 (${payload.vocabGradeTier}등급 · Lv.${payload.princessLevel})"
            }
        }
    }
}
