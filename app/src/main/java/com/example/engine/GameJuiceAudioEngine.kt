package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Zero-asset procedural PCM sound effects synthesizer for Game-First tactile feedback.
 * Uses Android AudioTrack with clean ADSR envelopes and gracefully no-ops in headless JVM tests.
 */
object GameJuiceAudioEngine {
    private const val SAMPLE_RATE = 22050
    private val muted = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "GameJuiceAudioSynth").apply { isDaemon = true }
    }

    // Pentatonic ascending scale frequencies (C5 -> E6) for rewarding combo chimes
    private val comboScaleHz = doubleArrayOf(
        523.25, // C5
        587.33, // D5
        659.25, // E5
        783.99, // G5
        880.00, // A5
        1046.50, // C6
        1174.66, // D6
        1318.51  // E6
    )

    fun isMuted(): Boolean = muted.get()

    fun setMuted(value: Boolean) {
        muted.set(value)
    }

    fun toggleMuted(): Boolean {
        val next = !muted.get()
        muted.set(next)
        return next
    }

    /** Crisp rising-pitch pentatonic chime scaled to the active combo streak. */
    fun playComboHit(combo: Int) {
        if (muted.get()) return
        val idx = (combo.coerceAtLeast(1) - 1).coerceAtMost(comboScaleHz.lastIndex)
        val baseHz = comboScaleHz[idx]
        playSequence(
            listOf(
                ToneNote(freqHz = baseHz, durationMs = 75, gain = 0.24),
                ToneNote(freqHz = baseHz * 1.5, durationMs = 110, gain = 0.16)
            )
        )
    }

    /** Energetic ascending arpeggio when entering FEVER mode. */
    fun playFeverTrigger() {
        if (muted.get()) return
        playSequence(
            listOf(
                ToneNote(freqHz = 523.25, durationMs = 60, gain = 0.22),
                ToneNote(freqHz = 659.25, durationMs = 60, gain = 0.24),
                ToneNote(freqHz = 783.99, durationMs = 65, gain = 0.26),
                ToneNote(freqHz = 1046.50, durationMs = 150, gain = 0.28)
            )
        )
    }

    /** Soft low-pitch warm thud on miss — informative without harshness. */
    fun playWrongThud() {
        if (muted.get()) return
        playSequence(
            listOf(
                ToneNote(freqHz = 210.0, durationMs = 90, gain = 0.22),
                ToneNote(freqHz = 155.0, durationMs = 120, gain = 0.18)
            )
        )
    }

    /** Victory fanfare on Boss Defeat or Exam Pass. */
    fun playBossDefeated() {
        if (muted.get()) return
        playSequence(
            listOf(
                ToneNote(freqHz = 523.25, durationMs = 80, gain = 0.24),
                ToneNote(freqHz = 659.25, durationMs = 80, gain = 0.24),
                ToneNote(freqHz = 783.99, durationMs = 85, gain = 0.26),
                ToneNote(freqHz = 1046.50, durationMs = 220, gain = 0.30)
            )
        )
    }

    /** Sparkly reward chest reveal chime scaled by rarity tier (0..3). */
    fun playChestUnlock(rarityTier: Int) {
        if (muted.get()) return
        val boost = 1.0 + (rarityTier.coerceIn(0, 3) * 0.08)
        playSequence(
            listOf(
                ToneNote(freqHz = 659.25 * boost, durationMs = 65, gain = 0.20),
                ToneNote(freqHz = 880.00 * boost, durationMs = 70, gain = 0.23),
                ToneNote(freqHz = 1318.51 * boost, durationMs = 160, gain = 0.26)
            )
        )
    }

    internal data class ToneNote(
        val freqHz: Double,
        val durationMs: Int,
        val gain: Double
    )

    internal fun synthesizePcm16(notes: List<ToneNote>): ShortArray {
        val totalSamples = notes.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
        if (totalSamples <= 0) return ShortArray(0)
        val pcm = ShortArray(totalSamples)
        var offset = 0
        for (note in notes) {
            val count = (SAMPLE_RATE * note.durationMs) / 1000
            val attackSamples = (count * 0.08).toInt().coerceAtLeast(1)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE.toDouble()
                val attackEnv = if (i < attackSamples) i.toDouble() / attackSamples.toDouble() else 1.0
                val decayEnv = exp(-4.5 * (i.toDouble() / count.coerceAtLeast(1).toDouble()))
                val env = attackEnv * decayEnv
                // Blend fundamental + gentle 2nd harmonic for warm marimba-like bell tone
                val fundamental = sin(2.0 * PI * note.freqHz * t)
                val overtone = 0.25 * sin(2.0 * PI * (note.freqHz * 2.0) * t)
                val sample = ((fundamental + overtone) * note.gain * env * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[offset + i] = sample.toShort()
            }
            offset += count
        }
        return pcm
    }

    private fun playSequence(notes: List<ToneNote>) {
        runCatching {
            executor.execute {
                runCatching {
                    val samples = synthesizePcm16(notes)
                    if (samples.isEmpty()) return@runCatching
                    val bufferBytes = samples.size * 2
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(bufferBytes)
                        .build()
                    try {
                        track.write(samples, 0, samples.size)
                        track.play()
                        val totalDurationMs = notes.sumOf { it.durationMs }.toLong()
                        Thread.sleep((totalDurationMs + 35L).coerceAtMost(900L))
                    } finally {
                        runCatching { track.stop() }
                        runCatching { track.release() }
                    }
                }
            }
        }
    }
}
