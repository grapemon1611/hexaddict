package com.almostbrilliantideas.hexspark.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.almostbrilliantideas.hexspark.audio.SettingsManager

/**
 * Manages interstitial ad loading and display for game over screen.
 *
 * Critical rules:
 * - Never blocks the game over screen waiting for an ad to load
 * - If offline or ad fails to load, game over screen shows normally
 * - Respects ad-free purchase state
 * - Preloads ads in the background during gameplay
 */
class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private val settingsManager = SettingsManager(context)

    /**
     * Whether an ad is ready to be shown.
     */
    val isAdReady: Boolean
        get() = interstitialAd != null

    /**
     * Whether ads should be shown (user has not purchased ad-free).
     */
    val shouldShowAds: Boolean
        get() = !settingsManager.adFreeUnlocked

    /**
     * Preload an interstitial ad in the background.
     * Safe to call multiple times - will not load if already loading or loaded.
     */
    fun preloadAd() {
        // Skip if user has purchased ad-free
        if (!shouldShowAds) {
            Log.d(TAG, "Ad-free mode enabled, skipping ad preload")
            return
        }

        // Skip if already loading or already have an ad ready
        if (isLoading || interstitialAd != null) {
            Log.d(TAG, "Ad already loading or ready, skipping preload")
            return
        }

        isLoading = true
        Log.d(TAG, "Starting ad preload")

        val adRequest = AdRequest.Builder()
            // Uncomment and add your device ID if you see real ads during testing:
            // .addTestDeviceIds(listOf("YOUR_DEVICE_ID_FROM_LOGCAT"))
            .build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Ad failed to load: ${adError.message}")
                    interstitialAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    interstitialAd = ad
                    isLoading = false
                }
            }
        )
    }

    /**
     * Show the interstitial ad if ready, then invoke callback.
     * If ad is not ready, callback is invoked immediately with no delay.
     *
     * @param activity The activity context for showing the ad
     * @param onAdComplete Callback invoked after ad is dismissed or if no ad was shown
     */
    fun showAdIfReady(activity: Activity, onAdComplete: () -> Unit) {
        // Skip if user has purchased ad-free
        if (!shouldShowAds) {
            Log.d(TAG, "Ad-free mode enabled, skipping ad show")
            onAdComplete()
            return
        }

        val ad = interstitialAd

        // If no ad is ready, continue immediately
        if (ad == null) {
            Log.d(TAG, "No ad ready, continuing to game over screen")
            onAdComplete()
            // Try to preload for next time
            preloadAd()
            return
        }

        // Set up callbacks for the ad
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                interstitialAd = null
                onAdComplete()
                // Start preloading the next ad immediately
                preloadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "Ad failed to show: ${adError.message}")
                interstitialAd = null
                onAdComplete()
                // Try to preload for next time
                preloadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showing")
                // Clear reference since it's now shown
                interstitialAd = null
            }
        }

        // Show the ad
        Log.d(TAG, "Showing ad")
        ad.show(activity)
    }
}
