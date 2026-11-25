package com.example

import com.example.engine.HybridAiCoachEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class GroqRotationAndStreamingTest {

    private val moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    private data class RecordedRequest(
        val authHeader: String,
        val bodyMap: Map<String, Any?>
    )

    private fun startMockGroqServer(
        handler: (RecordedRequest) -> Pair<Int, Pair<String, String>>
    ): Pair<ServerSocket, List<RecordedRequest>> {
        val server = ServerSocket(0)
        val recorded = CopyOnWriteArrayList<RecordedRequest>()
        val running = AtomicBoolean(true)

        thread(isDaemon = true, name = "mock-groq-server") {
            while (running.get() && !server.isClosed) {
                try {
                    val socket = server.accept()
                    socket.use { s ->
                        val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                        reader.readLine() ?: return@use
                        var authHeader = ""
                        var contentLength = 0
                        while (true) {
                            val headerLine = reader.readLine() ?: break
                            if (headerLine.isEmpty()) break
                            val lower = headerLine.lowercase()
                            if (lower.startsWith("authorization:")) {
                                authHeader = headerLine.substringAfter(":").trim()
                            } else if (lower.startsWith("content-length:")) {
                                contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                            }
                        }
                        val bodyChars = CharArray(contentLength)
                        var readTotal = 0
                        while (readTotal < contentLength) {
                            val r = reader.read(bodyChars, readTotal, contentLength - readTotal)
                            if (r <= 0) break
                            readTotal += r
                        }
                        val bodyStr = String(bodyChars, 0, readTotal)
                        val bodyMap = runCatching { mapAdapter.fromJson(bodyStr) }.getOrNull().orEmpty()
                        val req = RecordedRequest(
                            authHeader = authHeader,
                            bodyMap = bodyMap
                        )
                        recorded.add(req)

                        val (statusCode, contentTypeAndBody) = handler(req)
                        val (contentType, responseBody) = contentTypeAndBody
                        val bodyBytes = responseBody.toByteArray(Charsets.UTF_8)
                        val statusText = when (statusCode) {
                            200 -> "OK"
                            429 -> "Too Many Requests"
                            else -> "Error"
                        }
                        val out = s.getOutputStream()
                        val headers = buildString {
                            append("HTTP/1.1 $statusCode $statusText\r\n")
                            append("Content-Type: $contentType\r\n")
                            append("Content-Length: ${bodyBytes.size}\r\n")
                            append("Connection: close\r\n\r\n")
                        }
                        out.write(headers.toByteArray(Charsets.UTF_8))
                        out.write(bodyBytes)
                        out.flush()
                    }
                } catch (_: Exception) {
                    if (server.isClosed) running.set(false)
                }
            }
        }
        return server to recorded
    }

    @Test
    fun `groq oss 120b SSE streaming ignores null reasoning chunks and parses structured reply`() = runBlocking {
        val ssePayload = buildString {
            // Simulate openai/gpt-oss-120b reasoning chunks where content is JSON null
            append("data: {\"choices\":[{\"delta\":{\"content\":null,\"reasoning\":\"Thinking about a warm reply...\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"reasoning\":\"Formatting 8 lines.\"}}]}\n\n")
            // Stream actual content in small incremental deltas
            append("data: {\"choices\":[{\"delta\":{\"content\":\"REPLY_EN: Hey! Awesome job today. \"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"What did you enjoy most?\\n\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"TRANSLATION_KO: 안녕! 오늘 정말 잘했어. 뭐가 제일 재밌었어?\\n\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"REPHRASE_EN: I had a blast practicing English today.\\n\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"GRAMMAR_KO: have a blast는 원어민이 신나게 즐겼을 때 자주 쓰는 표현이에요.\\n\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"TIP_KO: blast의 모음을 길고 시원하게 발음해 보세요.\\n\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"HINT1: I loved the cafe roleplay! | 카페 롤플레이가 제일 재밌었어!\\n\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"HINT2: Practicing pronunciation was super helpful. | 발음 연습이 정말 도움됐어.\\n\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"HINT3: Let's try airport English next! | 다음엔 공항 영어 해보자!\"}}]}\n\n")
            append("data: [DONE]\n\n")
        }

        val (server, recorded) = startMockGroqServer {
            200 to ("text/event-stream; charset=utf-8" to ssePayload)
        }

        try {
            val engine = HybridAiCoachEngine(
                slotStore = null,
                groqEndpointUrl = "http://127.0.0.1:${server.localPort}/openai/v1/chat/completions",
                customGroqKeysProvider = { listOf(1 to "gsk_test_key_1") }
            )

            val streamedPreviews = mutableListOf<String>()
            val reply = engine.tryGroq5KeyRotation(
                prompt = "Test prompt",
                onStreamChunk = { preview -> streamedPreviews.add(preview) }
            )

            assertNotNull("Expected non-null AiCoachReply from Groq SSE stream", reply)
            val parsed = reply!!

            // Verify exact Groq openai/gpt-oss-120b request parameters
            assertEquals(1, recorded.size)
            val sent = recorded.first().bodyMap
            assertEquals("Bearer gsk_test_key_1", recorded.first().authHeader)
            assertEquals("openai/gpt-oss-120b", sent["model"])
            assertEquals(1.0, (sent["temperature"] as Number).toDouble(), 0.001)
            assertEquals(2048, (sent["max_completion_tokens"] as Number).toInt())
            assertEquals(1.0, (sent["top_p"] as Number).toDouble(), 0.001)
            assertEquals("medium", sent["reasoning_effort"])
            assertEquals(true, sent["stream"])

            // Verify no literal "null" leaked from reasoning chunks
            assertFalse("Reply must not contain literal 'null'", parsed.replyEn.contains("null"))
            assertEquals("Hey! Awesome job today. What did you enjoy most?", parsed.replyEn)
            assertEquals("안녕! 오늘 정말 잘했어. 뭐가 제일 재밌었어?", parsed.translationKo)
            assertEquals("I had a blast practicing English today.", parsed.naturalRephraseEn)
            assertEquals(3, parsed.suggestedReplies.size)
            assertEquals("Groq #1 · gpt-oss-120b", parsed.engineBadge)
            assertTrue("Expected live streaming previews to fire", streamedPreviews.isNotEmpty())
            println("[TEST 1 PASS] SSE streaming + null reasoning filtering + 8-line parsing verified (${streamedPreviews.size} live chunks)")
        } finally {
            server.close()
        }
    }

    @Test
    fun `groq 5-key round robin rotates 1 to 5 and fails over with 60s cooldown on HTTP 429`() = runBlocking {
        val failKey2 = AtomicBoolean(false)
        val (server, recorded) = startMockGroqServer { req ->
            if (failKey2.get() && req.authHeader == "Bearer gsk_fake_2") {
                429 to ("application/json" to """{"error":{"message":"Rate limit reached"}}""")
            } else {
                val sse = "data: {\"choices\":[{\"delta\":{\"content\":\"REPLY_EN: OK from ${req.authHeader}\\nTRANSLATION_KO: 정상 응답\"}}]}\n\ndata: [DONE]\n\n"
                200 to ("text/event-stream; charset=utf-8" to sse)
            }
        }

        try {
            val fiveKeys = (1..5).map { idx -> idx to "gsk_fake_$idx" }
            val engine = HybridAiCoachEngine(
                slotStore = null,
                groqEndpointUrl = "http://127.0.0.1:${server.localPort}/openai/v1/chat/completions",
                customGroqKeysProvider = { fiveKeys }
            )

            // 1) Verify clean 1 -> 2 -> 3 -> 4 -> 5 -> 1 rotation across 6 turns
            val badges = (1..6).map {
                engine.tryGroq5KeyRotation("Turn $it", null)?.engineBadge
            }
            assertEquals(
                listOf(
                    "Groq #1 · gpt-oss-120b",
                    "Groq #2 · gpt-oss-120b",
                    "Groq #3 · gpt-oss-120b",
                    "Groq #4 · gpt-oss-120b",
                    "Groq #5 · gpt-oss-120b",
                    "Groq #1 · gpt-oss-120b"
                ),
                badges
            )

            // 2) Next cursor is Key #2 -> simulate HTTP 429 on Key #2, expect immediate failover to Key #3
            failKey2.set(true)
            val failoverReply = engine.tryGroq5KeyRotation("Turn 7 failover", null)
            assertNotNull(failoverReply)
            assertEquals("Groq #3 · gpt-oss-120b", failoverReply!!.engineBadge)

            // 3) Verify Key #2 is now in 60s cooldown and skipped across the next 4 turns
            val postCooldownBadges = (1..4).map {
                engine.tryGroq5KeyRotation("Post-cooldown $it", null)?.engineBadge
            }
            assertFalse("Key #2 must stay in cooldown", postCooldownBadges.any { it == "Groq #2 · gpt-oss-120b" })
            assertEquals(
                listOf(
                    "Groq #3 · gpt-oss-120b",
                    "Groq #4 · gpt-oss-120b",
                    "Groq #5 · gpt-oss-120b",
                    "Groq #1 · gpt-oss-120b"
                ),
                postCooldownBadges
            )
            assertTrue(recorded.size >= 11)
            println("[TEST 2 PASS] 5-Key Round-Robin (1->2->3->4->5->1) + HTTP 429 Failover (#2 -> #3) + 60s Cooldown verified")
        } finally {
            server.close()
        }
    }

    @Test
    fun `production BuildConfig does not bundle any client-side Groq API keys`() {
        val engine = HybridAiCoachEngine(slotStore = null)
        val bundledKeys = engine.configuredGroqEnvKeys()
        assertTrue(
            "APK BuildConfig must never bundle Groq API keys; use Firebase AI Logic instead",
            bundledKeys.isEmpty()
        )
    }
}
