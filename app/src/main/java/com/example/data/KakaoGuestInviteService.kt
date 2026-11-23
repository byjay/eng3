package com.example.data

import android.content.Context
import android.content.Intent
import com.example.BuildConfig
import java.net.URLEncoder
import kotlin.random.Random

/**
 * Generates guest invite codes & KakaoTalk-escape standalone app launch links.
 *
 * Why KakaoTalk normally opens links inside its in-app browser:
 * Plain https:// links stay inside KakaoTalk's WebView unless redirected with an Android
 * `intent://...#Intent;scheme=dadquest;package=<appId>;end` URI or custom scheme `dadquest://invite`,
 * combined with `android:launchMode="singleTask"` + dedicated `taskAffinity` on MainActivity!
 */
object KakaoGuestInviteService {

    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateInviteCode(): String {
        val suffix = (1..6).map { CODE_ALPHABET[Random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
        return "WP-$suffix"
    }

    fun normalizeCode(raw: String): String {
        return raw.trim().uppercase().replace(Regex("[^A-Z0-9-]"), "").take(12)
    }

    /** Direct custom-scheme deep link that forces Android OS to launch this standalone app. */
    fun buildStandaloneDeepLink(code: String, hostNickname: String): String {
        val encCode = URLEncoder.encode(normalizeCode(code), "UTF-8")
        val encHost = URLEncoder.encode(hostNickname, "UTF-8")
        return "dadquest://invite?code=$encCode&host=$encHost"
    }

    /**
     * Android Intent URI specifically crafted to break out of KakaoTalk's in-app browser
     * and open this app in its own independent task window.
     */
    fun buildKakaoEscapeIntentUri(code: String, hostNickname: String): String {
        val encCode = URLEncoder.encode(normalizeCode(code), "UTF-8")
        val encHost = URLEncoder.encode(hostNickname, "UTF-8")
        val pkg = BuildConfig.APPLICATION_ID
        val fallback = URLEncoder.encode("https://dadquest.app/invite?code=$encCode&host=$encHost", "UTF-8")
        return "intent://invite?code=$encCode&host=$encHost#Intent;scheme=dadquest;package=$pkg;action=android.intent.action.VIEW;category=android.intent.category.BROWSABLE;S.browser_fallback_url=$fallback;end"
    }

    /** Smart cross-platform URL: iPhone/iPad launches PWA immediately; Android launches app or APK direct install. */
    fun buildSmartShareUrl(code: String, hostNickname: String, cloudflareWorkerUrl: String): String {
        val encCode = URLEncoder.encode(normalizeCode(code), "UTF-8")
        val encHost = URLEncoder.encode(hostNickname, "UTF-8")
        val base = cloudflareWorkerUrl.trim().trimEnd('/')
        return if (base.startsWith("https://")) {
            "$base/?code=$encCode&host=$encHost"
        } else {
            "https://dadquest.app/?code=$encCode&host=$encHost"
        }
    }

    fun shareGuestInviteViaKakaoTalk(
        context: Context,
        code: String,
        hostNickname: String,
        cloudflareWorkerUrl: String
    ) {
        val cleanCode = normalizeCode(code)
        val deepLink = buildStandaloneDeepLink(cleanCode, hostNickname)
        val smartPwaUrl = buildSmartShareUrl(cleanCode, hostNickname, cloudflareWorkerUrl)
        val msg = buildString {
            appendLine("[아빠가딸들을 위한 학습도구 — 초대장]")
            appendLine("초대자: $hostNickname · 초대코드: $cleanCode")
            appendLine()
            appendLine("1) 아이폰/아이패드 (Apple PWA · 설치 없이 3초 홈화면 추가):")
            appendLine(smartPwaUrl)
            appendLine("   -> Safari로 열기 -> 공유(↑) -> '홈 화면에 추가'")
            appendLine()
            appendLine("2) 안드로이드 (앱 바로 실행 · 미설치 시 APK 바로 설치):")
            appendLine(deepLink)
            appendLine("   스마트 링크: $smartPwaUrl")
            appendLine()
            append("별도 로그인 없이 초대코드 [$cleanCode]만 입력하면 바로 시작됩니다!")
        }
        KakaoAuthAndFriendsService.shareKingdomReportViaKakaoTalk(context, msg)
    }
}
