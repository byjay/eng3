package com.example

import com.example.data.CloudflareLevelPayload
import com.example.data.CloudflareLevelSyncService
import com.example.data.DadKingdomLocalStore
import com.example.data.HumanTutorRepository
import com.example.data.RealSceneClipFetcher
import com.example.data.VocabRepository
import com.example.engine.HybridAiCoachEngine
import com.example.engine.ParentPinCrypto
import com.example.engine.PinVerificationResult
import com.example.model.FamilyStreakEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreLaunchSecurityAndComplianceTest {

    @Test
    fun `parent PIN crypto rejects weak pins and verifies salted SHA-256 hashes`() {
        assertTrue(ParentPinCrypto.isWeakPin("1234"))
        assertTrue(ParentPinCrypto.isWeakPin("0000"))
        assertTrue(ParentPinCrypto.isWeakPin("1111"))
        assertFalse(ParentPinCrypto.isWeakPin("4829"))

        assertFalse(ParentPinCrypto.isValidPinFormat("123"))
        assertFalse(ParentPinCrypto.isValidPinFormat("1234567"))
        assertFalse(ParentPinCrypto.isValidPinFormat("12ab"))
        assertTrue(ParentPinCrypto.isValidPinFormat("4829"))
        assertTrue(ParentPinCrypto.isValidPinFormat("482917"))

        val (salt1, hash1) = ParentPinCrypto.createSaltedHash("4829")
        val (salt2, hash2) = ParentPinCrypto.createSaltedHash("4829")

        assertNotEquals("Salts must be randomly generated per PIN", salt1, salt2)
        assertNotEquals("Salted hashes of identical PINs must differ", hash1, hash2)
        assertTrue(ParentPinCrypto.verifyPin("4829", salt1, hash1))
        assertTrue(ParentPinCrypto.verifyPin("4829", salt2, hash2))
        assertFalse(ParentPinCrypto.verifyPin("1234", salt1, hash1))
        assertFalse(ParentPinCrypto.verifyPin("4829", "", ""))
    }

    @Test
    fun `parent PIN lockout enforces cooldown after 5 consecutive failures and resets after expiry`() {
        val (salt, hash) = ParentPinCrypto.createSaltedHash("5913")
        var failCount = 0
        var lockUntil = 0L
        val baseTime = 1_000_000L

        repeat(4) { idx ->
            val update = ParentPinCrypto.evaluatePinAttempt(
                candidatePin = "9999",
                storedSaltHex = salt,
                storedHashHex = hash,
                currentFailCount = failCount,
                lockUntilMs = lockUntil,
                nowMs = baseTime + idx * 1000L
            )
            assertTrue(update.result is PinVerificationResult.Invalid)
            assertEquals(4 - idx, (update.result as PinVerificationResult.Invalid).remainingAttempts)
            failCount = update.nextFailCount
            lockUntil = update.nextLockUntilMs
        }

        // 5th consecutive failure triggers 60s lockout
        val fifth = ParentPinCrypto.evaluatePinAttempt(
            candidatePin = "9999",
            storedSaltHex = salt,
            storedHashHex = hash,
            currentFailCount = failCount,
            lockUntilMs = lockUntil,
            nowMs = baseTime + 5_000L
        )
        assertTrue(fifth.result is PinVerificationResult.LockedOut)
        assertEquals(60L, (fifth.result as PinVerificationResult.LockedOut).retryAfterSeconds)

        // Even the correct PIN is blocked during cooldown
        val duringCooldown = ParentPinCrypto.evaluatePinAttempt(
            candidatePin = "5913",
            storedSaltHex = salt,
            storedHashHex = hash,
            currentFailCount = fifth.nextFailCount,
            lockUntilMs = fifth.nextLockUntilMs,
            nowMs = baseTime + 20_000L
        )
        assertTrue(duringCooldown.result is PinVerificationResult.LockedOut)

        // After cooldown expires, correct PIN succeeds and resets counters
        val afterCooldown = ParentPinCrypto.evaluatePinAttempt(
            candidatePin = "5913",
            storedSaltHex = salt,
            storedHashHex = hash,
            currentFailCount = fifth.nextFailCount,
            lockUntilMs = fifth.nextLockUntilMs,
            nowMs = fifth.nextLockUntilMs + 1_000L
        )
        assertEquals(PinVerificationResult.Verified, afterCooldown.result)
        assertEquals(0, afterCooldown.nextFailCount)
        assertEquals(0L, afterCooldown.nextLockUntilMs)
    }

    @Test
    fun `anonymous public LLM endpoints such as Pollinations are strictly blocked`() {
        val engine = HybridAiCoachEngine()
        assertFalse(engine.isAllowedCustomEndpoint("https://text.pollinations.ai/openai"))
        assertFalse(engine.isAllowedCustomEndpoint("https://POLLINATIONS.AI/v1"))
        assertFalse(engine.isAllowedCustomEndpoint(""))
    }

    @Test
    fun `tutor catalog clearly discloses AI simulation without misleading live-human claims`() {
        assertTrue(HumanTutorRepository.tutors.isNotEmpty())
        HumanTutorRepository.tutors.forEach { tutor ->
            assertFalse(
                "Tutor ${tutor.id} must not claim live human friend call",
                tutor.inspiredBy.contains("라이브 친구 통화")
            )
            assertFalse(
                "Tutor ${tutor.id} must not claim real person",
                tutor.koreanNickname.contains("실제 사람")
            )
            assertTrue(
                "Tutor ${tutor.id} should disclose AI simulation in label or nickname",
                tutor.inspiredBy.contains("AI") || tutor.koreanNickname.contains("AI")
            )
        }
    }

    @Test
    fun `minor privacy defaults to opt-in and DataStore schema version is tracked`() {
        val defaultEntry = FamilyStreakEntry()
        assertFalse("Minor leaderboard sharing must default to false (opt-in)", defaultEntry.shareWithFriends)
        assertTrue(DadKingdomLocalStore.CURRENT_SCHEMA_VERSION >= 2)
    }

    @Test
    fun `no-login YouTube segment extractor parses URLs and captions without cookies while MP4 scraping stays disabled`() = runBlocking {
        assertTrue(RealSceneClipFetcher.fetchExactSceneClips("crucial").isEmpty())
        assertFalse(RealSceneClipFetcher.isCurrentlyRateLimited())

        val ytSearchUrl = RealSceneClipFetcher.buildOfficialYouTubeSearchUrl("Hello! How are you?")
        assertTrue(ytSearchUrl.startsWith("https://www.youtube.com/results?search_query="))

        // Login-free SRT/transcript extraction works completely offline with zero OAuth/cookies
        val manualSrt = """
            1
            00:00:08,000 --> 00:00:11,000
            Good morning everyone.

            2
            00:00:11,500 --> 00:00:15,000
            This crucial decision will shape our entire project.

            3
            00:00:15,500 --> 00:00:18,000
            Let's review the details together.
        """.trimIndent()
        val extracted = RealSceneClipFetcher.extractFromManualTranscript(
            youtubeUrlOrId = "https://youtu.be/dQw4w9WgXcQ?t=10",
            transcriptText = manualSrt,
            keywordFilter = "crucial"
        )
        assertEquals(1, extracted.size)
        assertEquals("dQw4w9WgXcQ", extracted.first().videoId)
        assertTrue(extracted.first().matchedDialogueEn.contains("crucial", ignoreCase = true))
        assertEquals(10, extracted.first().startSeconds)
        assertTrue(extracted.first().endSeconds >= 16)
    }

    @Test
    fun `cloudflare level sync enforces HTTPS worker URLs`() = runBlocking {
        val payload = CloudflareLevelPayload(
            daughterName = "학습자",
            vocabGradeTier = 2,
            princessLevel = 5,
            castleHpPercent = 80,
            masteredWordCount = 24,
            accumulatedApiWordCount = 0,
            bossVictories = 1,
            totalXp = 500
        )
        val httpResult = CloudflareLevelSyncService.registerLevelToCloudflare(
            customWorkerUrl = "http://insecure.example.com",
            payload = payload
        )
        assertTrue("HTTP (non-HTTPS) worker URL must be rejected", httpResult.isFailure)

        val emptyResult = CloudflareLevelSyncService.registerLevelToCloudflare(
            customWorkerUrl = "",
            payload = payload
        )
        assertTrue("Empty worker URL must fail gracefully", emptyResult.isFailure)
    }

    @Test
    fun `curated vocabulary repository reports exact bundled word count accurately`() {
        assertEquals(105, VocabRepository.allWords.size)
        assertEquals(35, VocabRepository.allWords.count { it.grade == 1 })
        assertEquals(35, VocabRepository.allWords.count { it.grade == 2 })
        assertEquals(35, VocabRepository.allWords.count { it.grade == 3 })
    }
}
