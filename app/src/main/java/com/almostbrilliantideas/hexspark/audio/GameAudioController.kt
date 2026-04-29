package com.almostbrilliantideas.hexspark.audio

import android.content.Context
import android.util.Log
import com.almostbrilliantideas.hexspark.model.ClearInfo

/**
 * Unified controller for game audio (sound effects and haptic feedback).
 * Provides simple event-based methods that trigger both sound and haptics.
 * All operations are wrapped in try/catch to never crash the game.
 */
class GameAudioController(context: Context) {

    companion object {
        private const val TAG = "GameAudioController"
    }

    val settingsManager: SettingsManager = try {
        SettingsManager(context)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create SettingsManager", e)
        SettingsManager(context) // Retry once
    }

    private val soundManager: SoundManager? = try {
        SoundManager(context, settingsManager)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create SoundManager", e)
        null
    }

    private val hapticsManager: HapticsManager? = try {
        HapticsManager(context, settingsManager)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create HapticsManager", e)
        null
    }

    /**
     * Play feedback for piece placement.
     */
    fun onPiecePlaced() {
        try {
            soundManager?.playPlacement()
            hapticsManager?.playPlacement()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play placement feedback", e)
        }
    }

    /**
     * Play feedback for line clear based on ClearInfo.
     * This is the main entry point for clear events.
     */
    fun onLineClear(clearInfo: ClearInfo) {
        try {
            when {
                clearInfo.isJackpot -> {
                    soundManager?.playJackpot()
                    hapticsManager?.playJackpot()
                }
                clearInfo.isPayday -> {
                    soundManager?.playPayday()
                    hapticsManager?.playPayday()
                }
                clearInfo.sameColorLineCount == 1 -> {
                    // 2x color bonus
                    soundManager?.playColorBonus()
                    hapticsManager?.playColorBonus()
                }
                else -> {
                    // Regular clear - tier based on line count
                    soundManager?.playClear(clearInfo.linesCleared)
                    hapticsManager?.playClear(clearInfo.linesCleared)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play line clear feedback", e)
        }
    }

    /**
     * Play feedback for game over.
     */
    fun onGameOver() {
        try {
            soundManager?.playGameOver()
            hapticsManager?.playGameOver()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play game over feedback", e)
        }
    }
}
