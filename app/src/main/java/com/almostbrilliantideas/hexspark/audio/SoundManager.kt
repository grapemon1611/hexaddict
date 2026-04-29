package com.almostbrilliantideas.hexspark.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Manages synthesized game sounds.
 * All sounds are generated programmatically - no audio asset files.
 * Respects system volume and silent mode.
 * All operations are wrapped in try/catch to never crash the game.
 */
class SoundManager(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val audioManager: AudioManager? = try {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    } catch (e: Exception) {
        Log.w(TAG, "Failed to get AudioManager", e)
        null
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TAG = "SoundManager"
        private const val SAMPLE_RATE = 44100
    }

    /**
     * Check if sounds should play based on settings and system state.
     */
    private fun canPlaySound(): Boolean {
        if (!settingsManager.soundEnabled) return false

        // Check if media volume is muted (games use media stream, not ringer)
        return try {
            val mediaVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            mediaVolume > 0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check media volume", e)
            true // Default to allowing sound if we can't check
        }
    }

    /**
     * Play piece placement sound - soft click, subtle.
     */
    fun playPlacement() {
        if (!canPlaySound()) return
        scope.launch {
            safePlayTone(
                frequencies = listOf(800.0),
                durationMs = 50,
                volume = 0.4f,
                attack = 0.01,
                decay = 0.8
            )
        }
    }

    /**
     * Play single line clear - small crisp spark crack.
     */
    fun playClearSingle() {
        if (!canPlaySound()) return
        scope.launch {
            safePlaySparkSound(
                durationMs = 120,
                volume = 0.55f,
                crackCount = 2,
                sizzleIntensity = 0.3f
            )
        }
    }

    /**
     * Play two line clear - slightly fuller spark burst.
     */
    fun playClearDouble() {
        if (!canPlaySound()) return
        scope.launch {
            safePlaySparkSound(
                durationMs = 180,
                volume = 0.6f,
                crackCount = 4,
                sizzleIntensity = 0.5f
            )
        }
    }

    /**
     * Play three line clear - substantial spark burst.
     */
    fun playClearTriple() {
        if (!canPlaySound()) return
        scope.launch {
            safePlaySparkSound(
                durationMs = 250,
                volume = 0.65f,
                crackCount = 6,
                sizzleIntensity = 0.7f
            )
        }
    }

    /**
     * Play 2x color bonus - colored spark with brighter sizzle.
     */
    fun playColorBonus() {
        if (!canPlaySound()) return
        scope.launch {
            safePlaySparkSound(
                durationMs = 200,
                volume = 0.6f,
                crackCount = 5,
                sizzleIntensity = 0.6f,
                pitchShift = 1.2f  // Slightly higher pitched for color bonus
            )
        }
    }

    /**
     * Play PAYDAY (3x) - full sizzle burst with multiple sparks.
     */
    fun playPayday() {
        if (!canPlaySound()) return
        scope.launch {
            safePlaySparkSound(
                durationMs = 400,
                volume = 0.7f,
                crackCount = 10,
                sizzleIntensity = 0.85f,
                pitchShift = 1.0f
            )
        }
    }

    /**
     * Play JACKPOT - dramatic sustained spark storm.
     */
    fun playJackpot() {
        if (!canPlaySound()) return
        scope.launch {
            // Opening crack
            safePlaySparkSound(
                durationMs = 150,
                volume = 0.75f,
                crackCount = 4,
                sizzleIntensity = 0.9f
            )
            // Main spark storm - sustained intense sizzle
            safePlaySparkSound(
                durationMs = 600,
                volume = 0.8f,
                crackCount = 20,
                sizzleIntensity = 1.0f,
                pitchShift = 0.9f
            )
            // Trailing sparkles
            safePlaySparkSound(
                durationMs = 300,
                volume = 0.5f,
                crackCount = 8,
                sizzleIntensity = 0.4f,
                pitchShift = 1.3f
            )
        }
    }

    /**
     * Play game over - gentle descending tone, not punishing.
     */
    fun playGameOver() {
        if (!canPlaySound()) return
        scope.launch {
            // Gentle descending minor feel
            safePlayTone(listOf(440.0), 180, 0.45f, 0.05, 0.4) // A4
            safePlayTone(listOf(392.0), 180, 0.4f, 0.05, 0.4) // G4
            safePlayTone(listOf(329.63), 220, 0.35f, 0.05, 0.3) // E4
            safePlayTone(listOf(293.66), 350, 0.3f, 0.05, 0.2) // D4
        }
    }

    /**
     * Play clear sound based on number of lines cleared.
     */
    fun playClear(linesCleared: Int) {
        when {
            linesCleared >= 3 -> playClearTriple()
            linesCleared == 2 -> playClearDouble()
            linesCleared == 1 -> playClearSingle()
        }
    }

    /**
     * Safely play a spark sound, catching any exceptions.
     */
    private fun safePlaySparkSound(
        durationMs: Int,
        volume: Float,
        crackCount: Int,
        sizzleIntensity: Float,
        pitchShift: Float = 1.0f
    ) {
        try {
            playSparkSound(durationMs, volume, crackCount, sizzleIntensity, pitchShift)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play spark sound", e)
        }
    }

    /**
     * Generate and play a synthesized electric spark/sizzle sound.
     * Uses filtered white noise with transient cracks.
     */
    private fun playSparkSound(
        durationMs: Int,
        volume: Float,
        crackCount: Int,
        sizzleIntensity: Float,
        pitchShift: Float = 1.0f
    ) {
        val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt()
        if (numSamples <= 0) return

        val samples = ShortArray(numSamples)
        val random = java.util.Random()

        // Generate crack timing - positioned throughout the duration
        val crackTimes = (0 until crackCount).map {
            (random.nextFloat() * numSamples * 0.8f).toInt()
        }.sorted()

        for (i in 0 until numSamples) {
            val tNorm = i.toDouble() / numSamples

            // Base envelope - quick attack, slow decay
            val envelope = if (tNorm < 0.05) {
                tNorm / 0.05
            } else {
                exp(-2.0 * (tNorm - 0.05))
            }

            // White noise base (sizzle)
            var sample = (random.nextFloat() * 2f - 1f) * sizzleIntensity

            // Add high-frequency emphasis for electric sound
            val highFreqNoise = sin(2.0 * PI * (4000 * pitchShift) * i / SAMPLE_RATE).toFloat() *
                    (random.nextFloat() * 0.3f) * sizzleIntensity
            sample += highFreqNoise

            // Add crackle transients
            for (crackTime in crackTimes) {
                val distFromCrack = kotlin.math.abs(i - crackTime)
                if (distFromCrack < SAMPLE_RATE / 200) { // ~5ms crack duration
                    val crackEnvelope = 1.0f - (distFromCrack.toFloat() / (SAMPLE_RATE / 200f))
                    val crackNoise = (random.nextFloat() * 2f - 1f) * crackEnvelope * 1.5f
                    sample += crackNoise
                }
            }

            // Apply simple low-pass filter for smoother sound
            // (moving average with previous sample)
            if (i > 0) {
                val prevSample = samples[i - 1].toFloat() / Short.MAX_VALUE
                sample = sample * 0.6f + prevSample * 0.4f
            }

            // Apply envelope and volume
            val finalSample = (sample * envelope * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

            samples[i] = finalSample.toShort()
        }

        playPcmData(samples)
    }

    /**
     * Safely play a tone, catching any exceptions.
     */
    private fun safePlayTone(
        frequencies: List<Double>,
        durationMs: Int,
        volume: Float,
        attack: Double,
        decay: Double
    ) {
        try {
            playTone(frequencies, durationMs, volume, attack, decay)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play tone", e)
        }
    }

    /**
     * Generate and play a synthesized tone.
     */
    private fun playTone(
        frequencies: List<Double>,
        durationMs: Int,
        volume: Float,
        attack: Double,
        decay: Double
    ) {
        val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt()
        if (numSamples <= 0) return

        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val tNorm = i.toDouble() / numSamples

            // ADSR-like envelope: attack then exponential decay
            val envelope = if (tNorm < attack) {
                tNorm / attack
            } else {
                exp(-decay * (tNorm - attack) * 10)
            }

            // Sum all frequencies (for chords)
            var sample = 0.0
            for (freq in frequencies) {
                sample += sin(2.0 * PI * freq * t)
            }
            sample /= frequencies.size // Normalize

            // Apply envelope and volume
            val finalSample = (sample * envelope * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

            samples[i] = finalSample.toShort()
        }

        playPcmData(samples)
    }

    /**
     * Play raw PCM data using AudioTrack.
     */
    private fun playPcmData(samples: ShortArray) {
        if (samples.isEmpty()) return

        var audioTrack: AudioTrack? = null
        try {
            // Get minimum buffer size for this configuration
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (minBufferSize == AudioTrack.ERROR || minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                Log.w(TAG, "Invalid buffer size from getMinBufferSize")
                return
            }

            // Use the larger of our data size or minimum buffer size
            val bufferSize = maxOf(samples.size * 2, minBufferSize)

            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                Log.w(TAG, "AudioTrack failed to initialize, state: ${audioTrack.state}")
                audioTrack.release()
                return
            }

            // Start playback first for MODE_STREAM
            audioTrack.play()

            // Write samples
            val written = audioTrack.write(samples, 0, samples.size)
            if (written < 0) {
                Log.w(TAG, "AudioTrack write failed: $written")
            }

            // Wait for playback to complete
            val durationMs = (samples.size * 1000L) / SAMPLE_RATE
            Thread.sleep(durationMs + 100)

            audioTrack.stop()

        } catch (e: Exception) {
            Log.w(TAG, "Failed to play PCM data", e)
        } finally {
            try {
                audioTrack?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release AudioTrack", e)
            }
        }
    }
}
