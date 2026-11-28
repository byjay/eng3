package com.example.cloudflare

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.model.FamilyStreakEntry
import com.example.model.ScenarioProgress
import com.example.model.UserProgress
import com.example.model.WordMastery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

private const val TAG = "CloudflareApiClient"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

/**
 * Unified HTTP client for Cloudflare Workers + Cloudflare D1 + Cloudflare Workers AI.
 * Replaces all Firebase Firestore & Firebase AI Logic SDK network calls.
 */
object CloudflareApiClient {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun Request.Builder.withAuth(user: CloudflareUser?): Request.Builder {
        if (user != null) {
            if (user.sessionToken.isNotBlank()) {
                header("Authorization", "Bearer ${user.sessionToken}")
            }
            header("X-SpeakQuest-Uid", user.uid)
        }
        return this
    }

    suspend fun exchangeGoogleIdToken(googleIdToken: String): Result<CloudflareUser> = withContext(Dispatchers.IO) {
        val baseUrl = CloudflareConfig.resolveWorkerUrl()
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare Worker URL not configured"))
        }
        runCatching {
            val payload = JSONObject().put("idToken", googleIdToken)
            val req = Request.Builder()
                .url("$baseUrl/api/auth/google")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Cloudflare Auth HTTP ${resp.code}: ${raw.take(120)}")
                }
                val json = JSONObject(raw)
                CloudflareUser(
                    uid = json.getString("uid"),
                    displayName = json.optString("displayName", "Explorer"),
                    email = json.optString("email", ""),
                    sessionToken = json.optString("sessionToken", googleIdToken),
                    provider = "google"
                )
            }
        }
    }

    /**
     * Calls Cloudflare Workers AI (`POST /api/ai/chat`) with optional SSE streaming.
     */
    suspend fun chatWithWorkersAi(
        prompt: String,
        model: String = "@cf/meta/llama-3.3-70b-instruct-fp8-fast",
        user: CloudflareUser? = CloudflareAuthManager.currentUser,
        onStreamText: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = CloudflareConfig.resolveWorkerUrl()
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare Worker URL not configured"))
        }
        runCatching {
            val payload = JSONObject().apply {
                put("model", model)
                put("prompt", prompt)
                put("stream", onStreamText != null)
            }
            val req = Request.Builder()
                .url("$baseUrl/api/ai/chat")
                .header("Content-Type", "application/json")
                .header("Accept", if (onStreamText != null) "text/event-stream, application/json" else "application/json")
                .withAuth(user)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty().take(180)
                    throw IllegalStateException("Cloudflare Workers AI HTTP ${resp.code}: $err")
                }
                val body = resp.body ?: throw IllegalStateException("Empty Cloudflare Workers AI response")
                val contentType = resp.header("Content-Type").orEmpty()
                if (contentType.contains("text/event-stream", ignoreCase = true)) {
                    val sb = StringBuilder()
                    BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmed = line?.trim().orEmpty()
                            if (!trimmed.startsWith("data:")) continue
                            val data = trimmed.removePrefix("data:").trim()
                            if (data == "[DONE]" || data.isBlank()) continue
                            val chunkText = runCatching {
                                val obj = JSONObject(data)
                                obj.optString("response").ifBlank {
                                    obj.optJSONArray("choices")
                                        ?.optJSONObject(0)
                                        ?.optJSONObject("delta")
                                        ?.optString("content")
                                        .orEmpty()
                                }
                            }.getOrDefault("")
                            if (chunkText.isNotEmpty()) {
                                sb.append(chunkText)
                                onStreamText?.invoke(sb.toString())
                            }
                        }
                    }
                    sb.toString().trim()
                } else {
                    val raw = body.string()
                    val obj = JSONObject(raw)
                    obj.optString("text")
                        .ifBlank { obj.optString("response") }
                        .ifBlank { obj.optJSONObject("result")?.optString("response").orEmpty() }
                        .trim()
                }
            }
        }
    }

    /**
     * Calls Cloudflare Workers AI Image (`POST /api/ai/image`) and decodes PNG/JPEG/base64 response into a Bitmap.
     */
    suspend fun generateImageWithWorkersAi(
        prompt: String,
        user: CloudflareUser? = CloudflareAuthManager.currentUser
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val baseUrl = CloudflareConfig.resolveWorkerUrl()
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare Worker URL not configured"))
        }
        runCatching {
            val payload = JSONObject().put("prompt", prompt)
            val req = Request.Builder()
                .url("$baseUrl/api/ai/image")
                .header("Content-Type", "application/json")
                .withAuth(user)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty().take(140)
                    throw IllegalStateException("Cloudflare Image AI HTTP ${resp.code}: $err")
                }
                val contentType = resp.header("Content-Type").orEmpty()
                val bytes = if (contentType.startsWith("image/", ignoreCase = true)) {
                    resp.body?.bytes() ?: throw IllegalStateException("Empty image payload")
                } else {
                    val raw = resp.body?.string().orEmpty()
                    val b64 = JSONObject(raw).optString("imageBase64")
                        .ifBlank { JSONObject(raw).optString("image") }
                    if (b64.isBlank()) throw IllegalStateException("Missing imageBase64")
                    Base64.decode(b64, Base64.DEFAULT)
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("Failed to decode Cloudflare AI image bitmap")
            }
        }
    }

    suspend fun fetchUserState(user: CloudflareUser): Result<Triple<UserProgress?, List<WordMastery>, List<ScenarioProgress>>> =
        withContext(Dispatchers.IO) {
            val baseUrl = CloudflareConfig.resolveWorkerUrl()
            if (baseUrl.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Cloudflare Worker URL not configured"))
            }
            runCatching {
                val req = Request.Builder()
                    .url("$baseUrl/api/user/progress")
                    .withAuth(user)
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Cloudflare D1 fetch HTTP ${resp.code}: ${raw.take(120)}")
                    }
                    val root = JSONObject(raw)
                    val progress = root.optJSONObject("progress")?.let { p ->
                        UserProgress(
                            id = p.optString("userId", user.uid),
                            userId = p.optString("userId", user.uid),
                            displayName = p.optString("displayName", user.displayName),
                            selectedGrade = p.optInt("selectedGrade", 1),
                            selectedTeacherId = p.optString("selectedTeacherId", "emma_nyc"),
                            dailyGoal = p.optInt("dailyGoal", 30),
                            streakCount = p.optInt("streakCount", 1),
                            totalXp = p.optInt("totalXp", 0),
                            coins = p.optInt("coins", 150),
                            hearts = p.optInt("hearts", 5),
                            todayWordsCompleted = p.optInt("todayWordsCompleted", 0),
                            lastStudyDate = p.optString("lastStudyDate", ""),
                            unlockedItems = p.optJSONArray("unlockedItems").toStringList().ifEmpty { listOf("badge_starter") },
                            kakaoUserId = p.optString("kakaoUserId", ""),
                            connectedKakaoIds = p.optJSONArray("connectedKakaoIds").toStringList(),
                            createdAt = p.optLong("createdAt", 0L),
                            updatedAt = p.optLong("updatedAt", 0L)
                        )
                    }
                    val masteries = mutableListOf<WordMastery>()
                    root.optJSONArray("wordMastery")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val wid = item.optString("wordId", "")
                            if (wid.isNotBlank()) {
                                masteries.add(
                                    WordMastery(
                                        id = wid,
                                        wordId = wid,
                                        grade = item.optInt("grade", 1),
                                        correctCount = item.optInt("correctCount", 0),
                                        missCount = item.optInt("missCount", 0),
                                        speakingBestScore = item.optInt("speakingBestScore", 0),
                                        mastered = item.optBoolean("mastered", false),
                                        createdAt = item.optLong("createdAt", 0L),
                                        updatedAt = item.optLong("updatedAt", 0L)
                                    )
                                )
                            }
                        }
                    }
                    val scenarios = mutableListOf<ScenarioProgress>()
                    root.optJSONArray("scenarioProgress")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val sid = item.optString("scenarioId", "")
                            if (sid.isNotBlank()) {
                                scenarios.add(
                                    ScenarioProgress(
                                        id = sid,
                                        scenarioId = sid,
                                        zoneId = item.optInt("zoneId", 1),
                                        completedSentencesCount = item.optInt("completedSentencesCount", 0),
                                        twistCleared = item.optBoolean("twistCleared", false),
                                        bestFluencyScore = item.optInt("bestFluencyScore", 0),
                                        createdAt = item.optLong("createdAt", 0L),
                                        updatedAt = item.optLong("updatedAt", 0L)
                                    )
                                )
                            }
                        }
                    }
                    Triple(progress, masteries, scenarios)
                }
            }
        }

    suspend fun upsertUserProgress(user: CloudflareUser, progress: UserProgress): Result<Unit> =
        postJson("/api/user/progress", user, JSONObject().apply {
            put("userId", user.uid)
            put("displayName", progress.displayName.take(80))
            put("selectedGrade", progress.selectedGrade.coerceIn(1, 3))
            put("selectedTeacherId", progress.selectedTeacherId.take(40))
            put("dailyGoal", progress.dailyGoal.coerceIn(30, 120))
            put("streakCount", progress.streakCount.coerceIn(0, 10000))
            put("totalXp", progress.totalXp.coerceIn(0, 10_000_000))
            put("coins", progress.coins.coerceIn(0, 10_000_000))
            put("hearts", progress.hearts.coerceIn(0, 5))
            put("todayWordsCompleted", progress.todayWordsCompleted.coerceIn(0, 500))
            put("lastStudyDate", progress.lastStudyDate.take(20))
            put("unlockedItems", JSONArray(progress.unlockedItems))
            put("kakaoUserId", progress.kakaoUserId)
            put("connectedKakaoIds", JSONArray(progress.connectedKakaoIds))
        })

    suspend fun upsertWordMastery(user: CloudflareUser, mastery: WordMastery): Result<Unit> =
        postJson("/api/user/word-mastery", user, JSONObject().apply {
            put("wordId", mastery.wordId)
            put("grade", mastery.grade.coerceIn(1, 3))
            put("correctCount", mastery.correctCount.coerceAtLeast(0))
            put("missCount", mastery.missCount.coerceAtLeast(0))
            put("speakingBestScore", mastery.speakingBestScore.coerceIn(0, 100))
            put("mastered", mastery.mastered)
        })

    suspend fun upsertScenarioProgress(user: CloudflareUser, scenario: ScenarioProgress): Result<Unit> =
        postJson("/api/user/scenario-progress", user, JSONObject().apply {
            put("scenarioId", scenario.scenarioId)
            put("zoneId", scenario.zoneId.coerceIn(1, 10))
            put("completedSentencesCount", scenario.completedSentencesCount.coerceIn(0, 30))
            put("twistCleared", scenario.twistCleared)
            put("bestFluencyScore", scenario.bestFluencyScore.coerceIn(0, 100))
        })

    suspend fun logStudySession(
        user: CloudflareUser,
        mode: String,
        teacherId: String,
        accuracyPercent: Int,
        wpm: Int,
        xpEarned: Int,
        coinsEarned: Int
    ): Result<Unit> = postJson("/api/user/sessions", user, JSONObject().apply {
        put("mode", mode)
        put("teacherId", teacherId.take(40))
        put("accuracyPercent", accuracyPercent.coerceIn(0, 100))
        put("wpm", wpm.coerceIn(0, 400))
        put("xpEarned", xpEarned.coerceIn(0, 10000))
        put("coinsEarned", coinsEarned.coerceIn(0, 10000))
    })

    suspend fun syncFamilyStreak(
        user: CloudflareUser,
        entry: FamilyStreakEntry,
        connectedKakaoIds: List<String>
    ): Result<Unit> = postJson("/api/leaderboard/family/sync", user, JSONObject().apply {
        put("entryId", entry.id)
        put("userId", user.uid)
        put("kakaoUserId", entry.kakaoUserId)
        put("displayName", entry.displayName.take(40))
        put("avatarUrl", entry.avatarUrl)
        put("streakCount", entry.streakCount.coerceIn(0, 10000))
        put("todayWordsCompleted", entry.todayWordsCompleted.coerceIn(0, 500))
        put("totalXp", entry.totalXp.coerceIn(0, 10_000_000))
        put("lastStudyDate", entry.lastStudyDate.take(20))
        put("shareWithFriends", entry.shareWithFriends)
        put("connectedKakaoIds", JSONArray(connectedKakaoIds))
    })

    suspend fun fetchFamilyLeaderboard(
        user: CloudflareUser,
        ownEntryId: String,
        friendKakaoIds: List<String>
    ): Result<List<FamilyStreakEntry>> = withContext(Dispatchers.IO) {
        val baseUrl = CloudflareConfig.resolveWorkerUrl()
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare Worker URL not configured"))
        }
        runCatching {
            val payload = JSONObject().apply {
                put("ownEntryId", ownEntryId)
                put("friendKakaoIds", JSONArray(friendKakaoIds))
            }
            val req = Request.Builder()
                .url("$baseUrl/api/leaderboard/family/query")
                .header("Content-Type", "application/json")
                .withAuth(user)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Cloudflare Leaderboard HTTP ${resp.code}: ${raw.take(120)}")
                }
                val root = JSONObject(raw)
                val entriesArr = root.optJSONArray("entries") ?: JSONArray()
                buildList {
                    for (i in 0 until entriesArr.length()) {
                        val obj = entriesArr.optJSONObject(i) ?: continue
                        add(
                            FamilyStreakEntry(
                                id = obj.optString("id", obj.optString("entryId", "")),
                                userId = obj.optString("userId", ""),
                                kakaoUserId = obj.optString("kakaoUserId", ""),
                                displayName = obj.optString("displayName", "가족 멤버"),
                                avatarUrl = obj.optString("avatarUrl", ""),
                                streakCount = obj.optInt("streakCount", 0),
                                todayWordsCompleted = obj.optInt("todayWordsCompleted", 0),
                                totalXp = obj.optInt("totalXp", 0),
                                lastStudyDate = obj.optString("lastStudyDate", ""),
                                shareWithFriends = obj.optBoolean("shareWithFriends", true),
                                createdAt = obj.optLong("createdAt", 0L),
                                updatedAt = obj.optLong("updatedAt", 0L)
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun deleteAllUserData(user: CloudflareUser): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = CloudflareConfig.resolveWorkerUrl()
        if (baseUrl.isBlank()) return@withContext Result.success(Unit)
        runCatching {
            val req = Request.Builder()
                .url("$baseUrl/api/user/data")
                .withAuth(user)
                .delete()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty().take(120)
                    Log.w(TAG, "Cloudflare delete returned HTTP ${resp.code}: $err")
                }
            }
        }
    }

    private suspend fun postJson(
        path: String,
        user: CloudflareUser,
        payload: JSONObject
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = CloudflareConfig.resolveWorkerUrl()
        if (baseUrl.isBlank()) return@withContext Result.success(Unit)
        runCatching {
            val req = Request.Builder()
                .url("$baseUrl$path")
                .header("Content-Type", "application/json")
                .withAuth(user)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty().take(120)
                    throw IllegalStateException("Cloudflare D1 HTTP ${resp.code}: $err")
                }
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val item = optString(i, "")
                if (item.isNotBlank()) add(item)
            }
        }
    }
}
