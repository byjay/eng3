package com.example.engine

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted SHA-256 Parent PIN security helper:
 * - Never stores plaintext PINs or defaults to weak guessable PINs like "1234"
 * - Uses per-PIN random 16-byte salt + iterated SHA-256 stretching
 * - Verifies with constant-time MessageDigest.isEqual to prevent timing leaks
 */
sealed class PinVerificationResult {
    data object Verified : PinVerificationResult()
    data object NotConfigured : PinVerificationResult()
    data class Invalid(val remainingAttempts: Int) : PinVerificationResult()
    data class LockedOut(val retryAfterSeconds: Long) : PinVerificationResult()
}

data class PinLockoutUpdate(
    val result: PinVerificationResult,
    val nextFailCount: Int,
    val nextLockUntilMs: Long
)

object ParentPinCrypto {

    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 60_000L

    private const val HASH_ITERATIONS = 10_000
    private const val SALT_BYTES = 16
    private const val DOMAIN_PREFIX = "speakquest-parent-pin-v1:"

    private val weakPins = setOf(
        "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
        "1234", "4321", "0123", "123456", "000000", "111111"
    )

    fun isValidPinFormat(rawPin: String): Boolean {
        val trimmed = rawPin.trim()
        return trimmed.length in 4..6 && trimmed.all { it.isDigit() }
    }

    fun isWeakPin(rawPin: String): Boolean {
        return rawPin.trim() in weakPins
    }

    fun generateSaltHex(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(rawPin: String, saltHex: String): String {
        val cleanPin = rawPin.trim()
        require(isValidPinFormat(cleanPin)) { "PIN must be 4 to 6 numeric digits." }
        require(saltHex.length >= 16) { "Salt must be at least 16 hex characters." }
        val md = MessageDigest.getInstance("SHA-256")
        var digest = md.digest("$DOMAIN_PREFIX$saltHex:$cleanPin".toByteArray(Charsets.UTF_8))
        repeat(HASH_ITERATIONS - 1) {
            md.reset()
            digest = md.digest(digest)
        }
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun createSaltedHash(rawPin: String): Pair<String, String> {
        val salt = generateSaltHex()
        val hash = hashPin(rawPin, salt)
        return salt to hash
    }

    fun verifyPin(candidatePin: String, storedSaltHex: String, storedHashHex: String): Boolean {
        if (storedSaltHex.isBlank() || storedHashHex.isBlank()) return false
        if (!isValidPinFormat(candidatePin)) return false
        val candidateHash = hashPin(candidatePin, storedSaltHex)
        return MessageDigest.isEqual(
            candidateHash.toByteArray(Charsets.UTF_8),
            storedHashHex.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Pure deterministic PIN attempt evaluator with brute-force lockout protection.
     * After [MAX_FAILED_ATTEMPTS] consecutive wrong attempts, locks verification for [LOCKOUT_DURATION_MS].
     */
    fun evaluatePinAttempt(
        candidatePin: String,
        storedSaltHex: String,
        storedHashHex: String,
        currentFailCount: Int,
        lockUntilMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): PinLockoutUpdate {
        if (storedSaltHex.isBlank() || storedHashHex.isBlank()) {
            return PinLockoutUpdate(PinVerificationResult.NotConfigured, 0, 0L)
        }
        if (lockUntilMs > nowMs) {
            val remainingSec = ((lockUntilMs - nowMs) + 999L) / 1_000L
            return PinLockoutUpdate(
                result = PinVerificationResult.LockedOut(remainingSec.coerceAtLeast(1L)),
                nextFailCount = currentFailCount,
                nextLockUntilMs = lockUntilMs
            )
        }
        val activeFailCount = if (lockUntilMs in 1L..nowMs) 0 else currentFailCount.coerceAtLeast(0)
        return if (verifyPin(candidatePin, storedSaltHex, storedHashHex)) {
            PinLockoutUpdate(PinVerificationResult.Verified, 0, 0L)
        } else {
            val nextFailures = activeFailCount + 1
            if (nextFailures >= MAX_FAILED_ATTEMPTS) {
                val until = nowMs + LOCKOUT_DURATION_MS
                PinLockoutUpdate(
                    result = PinVerificationResult.LockedOut(LOCKOUT_DURATION_MS / 1_000L),
                    nextFailCount = nextFailures,
                    nextLockUntilMs = until
                )
            } else {
                PinLockoutUpdate(
                    result = PinVerificationResult.Invalid(remainingAttempts = MAX_FAILED_ATTEMPTS - nextFailures),
                    nextFailCount = nextFailures,
                    nextLockUntilMs = 0L
                )
            }
        }
    }
}
