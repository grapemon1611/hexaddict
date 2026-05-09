package com.almostbrilliantideas.hexspark.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val BANNER_AD_UNIT_ID = "ca-app-pub-8853683185264753/5485580796"

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val adView = remember {
        AdView(context).also {
            it.setAdSize(AdSize.BANNER)
            it.adUnitId = BANNER_AD_UNIT_ID
        }
    }

    LaunchedEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier
    )
}
