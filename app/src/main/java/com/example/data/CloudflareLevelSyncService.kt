package com.example.data

import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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

    /**
     * Registers progress to an optional user-configured Cloudflare Worker using
     * the signed-in Firebase user's short-lived ID token instead of any static APK admin secret.
     */
    suspend fun registerLevelToCloudflare(
        customWorkerUrl: String,
        payload: CloudflareLevelPayload
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = resolveWorkerUrl(customWorkerUrl)
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Cloudflare Worker URL을 입력해 주세요. (기본 클라우드 동기화는 Firebase Firestore를 사용합니다)")
            )
        }
        if (!baseUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(
                IllegalArgumentException("보안을 위해 HTTPS Cloudflare Worker URL만 허용됩니다.")
            )
        }
        val currentUser = Firebase.auth.currentUser
            ?: return@withContext Result.failure(
                IllegalStateException("Google 로그인 후 서버 동기화를 진행해 주세요.")
            )

        runCatching {
            val idToken = currentUser.getIdToken(false).await().token
                ?: throw IllegalStateException("인증 토큰을 가져올 수 없습니다. 다시 로그인해 주세요.")

            val endpoint = if (baseUrl.endsWith("/levels")) baseUrl else "$baseUrl/api/levels"
            val bodyJson = JSONObject().apply {
                put("accountId", currentUser.uid)
                put("daughterName", payload.daughterName.take(32))
                put("vocabGradeTier", payload.vocabGradeTier.coerceIn(1, 5))
                put("princessLevel", payload.princessLevel.coerceIn(1, 999))
                put("castleHpPercent", payload.castleHpPercent.coerceIn(0, 100))
                put("masteredWordCount", payload.masteredWordCount.coerceAtLeast(0))
                put("accumulatedApiWordCount", payload.accumulatedApiWordCount.coerceAtLeast(0))
                put("bossVictories", payload.bossVictories.coerceAtLeast(0))
                put("totalXp", payload.totalXp.coerceAtLeast(0))
                put("registeredAt", System.currentTimeMillis())
            }

            val req = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $idToken")
                .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Cloudflare 서버 오류 (HTTP ${resp.code}): ${raw.take(80)}")
                }
                "Cloudflare 등록 완료 (${payload.vocabGradeTier}등급 · Lv.${payload.princessLevel})"
            }
        }
    }
}
