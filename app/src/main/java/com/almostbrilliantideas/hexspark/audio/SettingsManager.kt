package com.almostbrilliantideas.hexspark.audio

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent settings for sound and haptic feedback.
 * Both default to enabled (true).
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "hex_spark_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
        private const val KEY_SHOW_TUTORIAL_ON_STARTUP = "show_tutorial_on_startup"
        private const val KEY_AD_FREE_UNLOCKED = "ad_free_unlocked"
    }

    /**
     * Whether sound effects are enabled. Defaults to true.
     */
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    /**
     * Whether haptic feedback is enabled. Defaults to true.
     */
    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    /**
     * The player's best score. Persists across app restarts.
     */
    var bestScore: Int
        get() = prefs.getInt(KEY_BEST_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_BEST_SCORE, value).apply()

    /**
     * Whether the tutorial has been seen at least once.
     */
    var tutorialSeen: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_TUTORIAL_SEEN, value).apply()

    /**
     * Whether to show tutorial on each startup. Defaults to false after first view.
     */
    var showTutorialOnStartup: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TUTORIAL_ON_STARTUP, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TUTORIAL_ON_STARTUP, value).apply()

    /**
     * Check if tutorial should be shown this session.
     */
    fun shouldShowTutorial(): Boolean {
        return !tutorialSeen || showTutorialOnStartup
    }

    /**
     * Whether the user has purchased ad-free mode. Defaults to false.
     * When true, all ad logic is skipped entirely.
     */
    var adFreeUnlocked: Boolean
        get() = prefs.getBoolean(KEY_AD_FREE_UNLOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_AD_FREE_UNLOCKED, value).apply()
}
