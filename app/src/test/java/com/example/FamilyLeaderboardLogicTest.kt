package com.example

import com.example.data.KakaoAuthAndFriendsService
import com.example.data.rankFamilyLeaderboardEntries
import com.example.model.FamilyStreakEntry
import com.example.model.KakaoFriend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyLeaderboardLogicTest {

    @Test
    fun `normalizeKakaoId strips unsafe characters and ensures single kakao prefix`() {
        assertEquals("kakao_34567890", KakaoAuthAndFriendsService.normalizeKakaoId("34567890"))
        assertEquals("kakao_34567890", KakaoAuthAndFriendsService.normalizeKakaoId("kakao_34567890"))
        assertEquals("kakao_abc_123-XYZ", KakaoAuthAndFriendsService.normalizeKakaoId("  abc_123-XYZ/../../ "))
        assertEquals("", KakaoAuthAndFriendsService.normalizeKakaoId("   "))
        assertEquals("", KakaoAuthAndFriendsService.normalizeKakaoId("///@@@"))
    }

    @Test
    fun `sanitizeHttpsUrl permits only strict https URLs without whitespace`() {
        assertEquals(
            "https://k.kakaocdn.net/dn/sample/avatar.jpg",
            KakaoAuthAndFriendsService.sanitizeHttpsUrl("https://k.kakaocdn.net/dn/sample/avatar.jpg")
        )
        assertEquals("", KakaoAuthAndFriendsService.sanitizeHttpsUrl("http://k.kakaocdn.net/a.jpg"))
        assertEquals("", KakaoAuthAndFriendsService.sanitizeHttpsUrl("javascript:alert(1)"))
        assertEquals("", KakaoAuthAndFriendsService.sanitizeHttpsUrl("https://bad url.com/a.png"))
        assertEquals("", KakaoAuthAndFriendsService.sanitizeHttpsUrl(""))
    }

    @Test
    fun `rankFamilyLeaderboardEntries sorts by streak desc then todayWords desc then totalXp desc and deduplicates owner`() {
        val entries = listOf(
            FamilyStreakEntry(
                id = "kakao_dad",
                userId = "uid_dad",
                kakaoUserId = "kakao_dad",
                displayName = "아빠 국왕",
                streakCount = 14,
                todayWordsCompleted = 20,
                totalXp = 900
            ),
            FamilyStreakEntry(
                id = "kakao_daughter1",
                userId = "uid_d1",
                kakaoUserId = "kakao_daughter1",
                displayName = "원삐",
                streakCount = 21,
                todayWordsCompleted = 30,
                totalXp = 1800
            ),
            FamilyStreakEntry(
                id = "kakao_mom",
                userId = "uid_mom",
                kakaoUserId = "kakao_mom",
                displayName = "엄마 왕비",
                streakCount = 14,
                todayWordsCompleted = 25,
                totalXp = 850
            ),
            // Duplicate fallback uid_ entry for dad with lower info should be deduplicated
            FamilyStreakEntry(
                id = "uid_uid_dad",
                userId = "uid_dad",
                kakaoUserId = "",
                displayName = "아빠 국왕",
                streakCount = 10,
                todayWordsCompleted = 5,
                totalXp = 200
            )
        )

        val friendMeta = mapOf(
            "kakao_mom" to KakaoFriend(
                uuid = "u_mom",
                nickname = "엄마 카톡닉",
                thumbnailUrl = "https://k.kakaocdn.net/dn/mom.jpg",
                kakaoId = "kakao_mom"
            )
        )

        val ranked = rankFamilyLeaderboardEntries(
            entries = entries,
            currentUserUid = "uid_dad",
            friendNicknamesByKakaoId = friendMeta
        )

        assertEquals(3, ranked.size)

        // 1st: 원삐 (streak 21)
        assertEquals(1, ranked[0].rank)
        assertEquals("원삐", ranked[0].displayName)
        assertEquals(21, ranked[0].streakCount)
        assertFalse(ranked[0].isCurrentUser)

        // 2nd: 엄마 왕비 (streak 14, todayWords 25 > 20)
        assertEquals(2, ranked[1].rank)
        assertEquals("엄마 왕비", ranked[1].displayName)
        assertEquals(14, ranked[1].streakCount)
        assertEquals("https://k.kakaocdn.net/dn/mom.jpg", ranked[1].avatarUrl)

        // 3rd: 아빠 국왕 (streak 14, todayWords 20, isCurrentUser = true)
        assertEquals(3, ranked[2].rank)
        assertEquals("아빠 국왕", ranked[2].displayName)
        assertEquals(14, ranked[2].streakCount)
        assertTrue(ranked[2].isCurrentUser)
        assertTrue(ranked[2].isKakaoLinked)
    }
}
