package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.example.model.KakaoFriend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class KakaoAuthResult(
    val accessToken: String,
    val kakaoUserId: String,
    val nickname: String,
    val profileImageUrl: String,
    val friends: List<KakaoFriend>
)

object KakaoAuthAndFriendsService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun normalizeKakaoId(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return ""
        val withoutPrefix = trimmed.removePrefix("kakao_")
        val sanitized = withoutPrefix
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(64)
        return if (sanitized.isBlank()) "" else "kakao_$sanitized"
    }

    fun sanitizeHttpsUrl(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return ""
        if (!trimmed.startsWith("https://")) return ""
        if (trimmed.any { it.isWhitespace() }) return ""
        return trimmed.take(500)
    }

    fun isKakaoKeyConfigured(): Boolean {
        val restKey = BuildConfig.KAKAO_REST_API_KEY
        return restKey.isNotBlank() &&
            restKey != "YOUR_KAKAO_REST_API_KEY" &&
            !restKey.contains("PLACEHOLDER", ignoreCase = true)
    }

    fun buildAuthorizeUrl(): String {
        val clientId = URLEncoder.encode(BuildConfig.KAKAO_REST_API_KEY, "UTF-8")
        val redirectUri = URLEncoder.encode(BuildConfig.KAKAO_REDIRECT_URI, "UTF-8")
        val scope = URLEncoder.encode("profile_nickname,profile_image,friends,talk_message", "UTF-8")
        return "https://kauth.kakao.com/oauth/authorize" +
            "?response_type=code" +
            "&client_id=$clientId" +
            "&redirect_uri=$redirectUri" +
            "&scope=$scope"
    }

    fun launchKakaoOAuthLogin(context: Context): Result<Unit> {
        if (!isKakaoKeyConfigured()) {
            return Result.failure(
                IllegalStateException("AI Studio Secrets 패널에 KAKAO_REST_API_KEY와 KAKAO_NATIVE_APP_KEY를 등록하면 카카오톡 실로그인이 즉시 활성화됩니다.")
            )
        }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buildAuthorizeUrl())).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    suspend fun exchangeCodeAndLoadProfileAndFriends(authCode: String): Result<KakaoAuthResult> =
        withContext(Dispatchers.IO) {
            if (!isKakaoKeyConfigured()) {
                return@withContext Result.failure(IllegalStateException("KAKAO_REST_API_KEY가 설정되지 않았습니다."))
            }
            runCatching {
                val formBody = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("client_id", BuildConfig.KAKAO_REST_API_KEY)
                    .add("redirect_uri", BuildConfig.KAKAO_REDIRECT_URI)
                    .add("code", authCode)
                    .build()

                val tokenReq = Request.Builder()
                    .url("https://kauth.kakao.com/oauth/token")
                    .post(formBody)
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .build()

                val accessToken = httpClient.newCall(tokenReq).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("카카오 토큰 발급 실패 (${resp.code})")
                    }
                    JSONObject(raw).getString("access_token")
                }

                // 1. Fetch Kakao User Profile (/v2/user/me)
                val meReq = Request.Builder()
                    .url("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                var kakaoUserId = ""
                var nickname = "카카오 수호기사"
                var profileUrl = ""
                httpClient.newCall(meReq).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && raw.isNotBlank()) {
                        val root = JSONObject(raw)
                        kakaoUserId = normalizeKakaoId(root.opt("id")?.toString())
                        val profile = root.optJSONObject("kakao_account")?.optJSONObject("profile")
                        nickname = profile?.optString("nickname").orEmpty().ifBlank { nickname }
                        profileUrl = sanitizeHttpsUrl(profile?.optString("thumbnail_image_url"))
                    }
                }

                // 2. Fetch KakaoTalk Friends (/v1/api/talk/friends)
                val friends = fetchKakaoFriendsInternal(accessToken)

                KakaoAuthResult(
                    accessToken = accessToken,
                    kakaoUserId = kakaoUserId,
                    nickname = nickname,
                    profileImageUrl = profileUrl,
                    friends = friends
                )
            }
        }

    suspend fun refreshFriends(accessToken: String): Result<List<KakaoFriend>> =
        withContext(Dispatchers.IO) {
            if (accessToken.isBlank()) {
                return@withContext Result.failure(IllegalStateException("먼저 카카오톡 로그인을 진행해 주세요."))
            }
            runCatching { fetchKakaoFriendsInternal(accessToken) }
        }

    private fun fetchKakaoFriendsInternal(accessToken: String): List<KakaoFriend> {
        val friendsReq = Request.Builder()
            .url("https://kapi.kakao.com/v1/api/talk/friends?limit=30")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return httpClient.newCall(friendsReq).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful || raw.isBlank()) {
                return@use emptyList()
            }
            val arr = JSONObject(raw).optJSONArray("elements") ?: return@use emptyList()
            (0 until arr.length()).map { i ->
                val item = arr.getJSONObject(i)
                val uuid = item.optString("uuid")
                val rawId = item.opt("id")?.toString().orEmpty().ifBlank { uuid }
                KakaoFriend(
                    uuid = uuid,
                    nickname = item.optString("profile_nickname", "카톡 친구"),
                    thumbnailUrl = sanitizeHttpsUrl(item.optString("profile_thumbnail_image")),
                    favorite = item.optBoolean("favorite", false),
                    kakaoId = normalizeKakaoId(rawId)
                )
            }
        }
    }

    fun shareKingdomReportViaKakaoTalk(context: Context, messageText: String) {
        val kakaoIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, messageText)
            setPackage("com.kakao.talk")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val canOpenKakao = kakaoIntent.resolveActivity(context.packageManager) != null
        if (canOpenKakao) {
            context.startActivity(kakaoIntent)
        } else {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
            }
            val chooser = Intent.createChooser(fallback, "카카오톡 / 가족에게 학습 보고서 공유").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
