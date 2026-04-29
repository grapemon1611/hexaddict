package com.almostbrilliantideas.hexspark.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Manages haptic feedback for game events.
 * Uses VibrationEffect for modern APIs (26+) with fallback for older devices.
 * All operations are wrapped in try/catch to never crash the game.
 */
class HapticsManager(
    context: Context,
    private val settingsManager: SettingsManager
) {
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to get Vibrator service", e)
        null
    }

    companion object {
        private const val TAG = "HapticsManager"
    }

    /**
     * Check if haptics should play based on settings and device capability.
     */
    private fun canVibrate(): Boolean {
        if (!settingsManager.hapticsEnabled) return false
        return try {
            vibrator?.hasVibrator() == true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check vibrator capability", e)
            false
        }
    }

    /**
     * Piece placement - very light tick.
     */
    fun playPlacement() {
        if (!canVibrate()) return
        safeVibrate(duration = 15, amplitude = 80)
    }

    /**
     * Single line clear - short light pulse.
     */
    fun playClearSingle() {
        if (!canVibrate()) return
        safeVibrate(duration = 30, amplitude = 120)
    }

    /**
     * Two line clear - medium pulse.
     */
    fun playClearDouble() {
        if (!canVibrate()) return
        safeVibrate(duration = 40, amplitude = 160)
    }

    /**
     * Three line clear - stronger pulse.
     */
    fun playClearTriple() {
        if (!canVibrate()) return
        safeVibrate(duration = 50, amplitude = 200)
    }

    /**
     * 2x color bonus - quick double tap.
     */
    fun playColorBonus() {
        if (!canVibrate()) return
        safeVibratePattern(
            timings = longArrayOf(0, 20, 40, 25),
            amplitudes = intArrayOf(0, 100, 0, 140)
        )
    }

    /**
     * PAYDAY (3x) - double pulse.
     */
    fun playPayday() {
        if (!canVibrate()) return
        safeVibratePattern(
            timings = longArrayOf(0, 30, 50, 30, 50, 40),
            amplitudes = intArrayOf(0, 120, 0, 150, 0, 180)
        )
    }

    /**
     * JACKPOT - long dramatic rumble with crescendo.
     */
    fun playJackpot() {
        if (!canVibrate()) return
        safeVibratePattern(
            timings = longArrayOf(0, 50, 30, 60, 30, 80, 30, 150),
            amplitudes = intArrayOf(0, 80, 0, 120, 0, 180, 0, 255)
        )
    }

    /**
     * Game over - single soft pulse.
     */
    fun playGameOver() {
        if (!canVibrate()) return
        safeVibrate(duration = 100, amplitude = 60)
    }

    /**
     * Play clear haptic based on number of lines cleared.
     */
    fun playClear(linesCleared: Int) {
        when {
            linesCleared >= 3 -> playClearTriple()
            linesCleared == 2 -> playClearDouble()
            linesCleared == 1 -> playClearSingle()
        }
    }

    /**
     * Safely vibrate, catching any exceptions.
     */
    private fun safeVibrate(duration: Long, amplitude: Int) {
        try {
            vibrate(duration, amplitude)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to vibrate", e)
        }
    }

    /**
     * Safely vibrate pattern, catching any exceptions.
     */
    private fun safeVibratePattern(timings: LongArray, amplitudes: IntArray) {
        try {
            vibratePattern(timings, amplitudes)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to vibrate pattern", e)
        }
    }

    /**
     * Simple single vibration.
     */
    private fun vibrate(duration: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateModern(v, duration, amplitude)
        } else {
            vibrateLegacy(v, duration)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateModern(v: Vibrator, duration: Long, amplitude: Int) {
        val clampedAmplitude = amplitude.coerceIn(1, 255)
        val effect = VibrationEffect.createOneShot(duration, clampedAmplitude)
        v.vibrate(effect)
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy(v: Vibrator, duration: Long) {
        v.vibrate(duration)
    }

    /**
     * Pattern vibration with varying amplitudes.
     */
    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibratePatternModern(v, timings, amplitudes)
        } else {
            vibratePatternLegacy(v, timings)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibratePatternModern(v: Vibrator, timings: LongArray, amplitudes: IntArray) {
        val clampedAmplitudes = amplitudes.map { it.coerceIn(0, 255) }.toIntArray()
        val effect = VibrationEffect.createWaveform(timings, clampedAmplitudes, -1)
        v.vibrate(effect)
    }

    @Suppress("DEPRECATION")
    private fun vibratePatternLegacy(v: Vibrator, timings: LongArray) {
        v.vibrate(timings, -1)
    }
}
