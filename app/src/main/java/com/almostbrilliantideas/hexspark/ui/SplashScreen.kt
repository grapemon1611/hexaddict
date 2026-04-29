package com.almostbrilliantideas.hexspark.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almostbrilliantideas.hexspark.R
import com.almostbrilliantideas.hexspark.ui.effects.SplashSparkEffectState
import com.almostbrilliantideas.hexspark.ui.effects.SplashSparkParticle
import kotlinx.coroutines.delay

private val SplashBackground = Color(0xFF14142A)

// Google Fonts provider for Oxanium
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val oxaniumFont = GoogleFont("Oxanium")

private val OxaniumSemiBold = FontFamily(
    Font(
        googleFont = oxaniumFont,
        fontProvider = provider,
        weight = FontWeight.SemiBold
    )
)

// Fallback to bold if SemiBold isn't available via Google Fonts
private val OxaniumFallback = FontFamily(
    Font(R.font.oxanium_bold, FontWeight.SemiBold)
)

enum class SplashPhase {
    FadeIn,
    Hold,
    Dissolve,
    FadeToGame,
    Complete
}

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var phase by remember { mutableStateOf(SplashPhase.FadeIn) }
    val contentAlpha = remember { Animatable(0f) }
    val gameAlpha = remember { Animatable(0f) }

    var iconBounds by remember { mutableStateOf<Rect?>(null) }
    var textBounds by remember { mutableStateOf<Rect?>(null) }
    var screenCenter by remember { mutableStateOf(Offset.Zero) }

    val sparkEffect = remember { SplashSparkEffectState() }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val density = LocalDensity.current

    // Animation sequence
    LaunchedEffect(Unit) {
        // Phase 1: Fade in (300ms)
        contentAlpha.animateTo(1f, animationSpec = tween(300))
        phase = SplashPhase.Hold

        // Phase 2: Hold (1.5 seconds)
        delay(1500)

        // Phase 3: Dissolve (800ms)
        phase = SplashPhase.Dissolve
        iconBounds?.let { icon ->
            textBounds?.let { text ->
                sparkEffect.startDissolve(
                    iconBounds = icon,
                    textBounds = text,
                    screenCenter = screenCenter,
                    duration = 800
                )
            }
        }

        // Fade out content as dissolve starts
        contentAlpha.animateTo(0f, animationSpec = tween(300))

        // Wait for particles to mostly clear
        delay(500)

        // Phase 4: Fade to game
        phase = SplashPhase.FadeToGame
        gameAlpha.animateTo(1f, animationSpec = tween(300))

        // Complete
        phase = SplashPhase.Complete
        onSplashComplete()
    }

    // Animation loop for particles
    LaunchedEffect(phase) {
        if (phase == SplashPhase.Dissolve || phase == SplashPhase.FadeToGame) {
            while (sparkEffect.isActive()) {
                currentTime = System.currentTimeMillis()
                sparkEffect.update(currentTime)
                delay(16) // ~60fps
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground)
            .onGloballyPositioned { coordinates ->
                screenCenter = Offset(
                    coordinates.size.width / 2f,
                    coordinates.size.height / 2f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Splash content (icon and text)
        if (contentAlpha.value > 0f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // App icon
                Image(
                    painter = painterResource(id = R.drawable.hexspark_icon),
                    contentDescription = "HexSpark",
                    modifier = Modifier
                        .size(180.dp)
                        .onGloballyPositioned { coordinates ->
                            iconBounds = coordinates.boundsInRoot()
                        },
                    alpha = contentAlpha.value
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App name
                Text(
                    text = "HexSpark",
                    fontFamily = OxaniumSemiBold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 42.sp,
                    color = Color.White.copy(alpha = contentAlpha.value),
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        textBounds = coordinates.boundsInRoot()
                    }
                )

                Spacer(modifier = Modifier.weight(1.2f))
            }
        }

        // Particle overlay
        if (sparkEffect.isActive()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val particles = sparkEffect.getParticles()
                particles.forEach { particle ->
                    drawSplashParticle(particle, currentTime, density.density)
                }
            }
        }
    }
}

private fun DrawScope.drawSplashParticle(
    particle: SplashSparkParticle,
    currentTime: Long,
    density: Float
) {
    val pos = particle.positionAt(currentTime)
    val alpha = particle.alphaAt(currentTime)

    if (alpha <= 0f) return

    val sizePx = particle.size * density

    // Draw trail if enabled
    if (particle.hasTrail) {
        val trailCount = 4
        val trailInterval = 20L // ms between trail points

        for (i in 1..trailCount) {
            val trailTime = currentTime - (i * trailInterval)
            if (trailTime < particle.createdAt) continue

            val trailPos = particle.positionAt(trailTime)
            val trailAlpha = alpha * (1f - i.toFloat() / (trailCount + 1)) * 0.5f
            val trailSize = sizePx * (1f - i.toFloat() / (trailCount + 1) * 0.3f)

            drawCircle(
                color = particle.color.copy(alpha = trailAlpha),
                radius = trailSize,
                center = trailPos
            )
        }
    }

    // Draw main particle with glow
    // Outer glow
    drawCircle(
        color = particle.color.copy(alpha = alpha * 0.3f),
        radius = sizePx * 1.8f,
        center = pos
    )

    // Main body
    drawCircle(
        color = particle.color.copy(alpha = alpha),
        radius = sizePx,
        center = pos
    )

    // Bright core
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.8f),
        radius = sizePx * 0.4f,
        center = pos
    )
}
