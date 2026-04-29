package com.almostbrilliantideas.hexspark.ui.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A spark particle for the splash screen dissolve effect.
 * Reuses the same visual language as the line clear spark particles.
 */
data class SplashSparkParticle(
    val startPos: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val lifetime: Float,
    val createdAt: Long,
    val hasTrail: Boolean = true
) {
    fun positionAt(currentTime: Long): Offset {
        val elapsed = (currentTime - createdAt).toFloat()
        val progress = (elapsed / lifetime).coerceIn(0f, 1f)
        // Deceleration - particles slow down as they travel
        val decel = 1f - progress * 0.4f
        return Offset(
            startPos.x + velocity.x * progress * decel,
            startPos.y + velocity.y * progress * decel
        )
    }

    fun alphaAt(currentTime: Long): Float {
        val elapsed = (currentTime - createdAt).toFloat()
        val progress = (elapsed / lifetime).coerceIn(0f, 1f)
        // Full opacity for first 20%, then fade out
        return if (progress < 0.2f) {
            1f
        } else {
            1f - ((progress - 0.2f) / 0.8f)
        }
    }

    fun isExpired(currentTime: Long): Boolean {
        return (currentTime - createdAt) > lifetime
    }
}

/**
 * Manages the spark dissolve effect for the splash screen.
 * Creates particles that scatter outward from icon and text bounds.
 */
class SplashSparkEffectState {
    private val particles = mutableListOf<SplashSparkParticle>()
    private var effectStartTime: Long = 0
    private var isActive = false
    private var totalDuration: Long = 0

    // Spark colors - matching the game's visual language
    private val sparkColors = listOf(
        Color(0xFFFFFFFF),  // Pure white
        Color(0xFFFFD700),  // Gold
        Color(0xFFFFE135),  // Banana yellow
        Color(0xFFFFFACD),  // Lemon chiffon
        Color(0xFF8B84D4),  // Purple (app primary)
        Color(0xFF4DB893)   // Teal (app secondary)
    )

    /**
     * Start the dissolve effect.
     * Creates particles from the icon and text bounds that scatter outward.
     *
     * @param iconBounds The bounds of the icon to dissolve
     * @param textBounds The bounds of the text to dissolve
     * @param screenCenter Center of the screen for directional scattering
     * @param duration Total duration of the dissolve in ms
     */
    fun startDissolve(
        iconBounds: Rect,
        textBounds: Rect,
        screenCenter: Offset,
        duration: Long = 800
    ) {
        clear()
        isActive = true
        effectStartTime = System.currentTimeMillis()
        totalDuration = duration

        // Create particles for the icon area
        spawnParticlesFromBounds(
            bounds = iconBounds,
            screenCenter = screenCenter,
            particleCount = 150,
            baseLifetime = duration.toFloat()
        )

        // Create particles for the text area
        spawnParticlesFromBounds(
            bounds = textBounds,
            screenCenter = screenCenter,
            particleCount = 80,
            baseLifetime = duration.toFloat()
        )
    }

    private fun spawnParticlesFromBounds(
        bounds: Rect,
        screenCenter: Offset,
        particleCount: Int,
        baseLifetime: Float
    ) {
        val centerX = bounds.center.x
        val centerY = bounds.center.y

        repeat(particleCount) { i ->
            // Distribute spawn points across the bounds with some randomization
            val spawnX = bounds.left + Random.nextFloat() * bounds.width
            val spawnY = bounds.top + Random.nextFloat() * bounds.height
            val spawnPos = Offset(spawnX, spawnY)

            // Calculate outward direction from screen center
            val dirX = spawnX - screenCenter.x
            val dirY = spawnY - screenCenter.y
            val dirLen = kotlin.math.sqrt(dirX * dirX + dirY * dirY)
            val normalizedDirX = if (dirLen > 0) dirX / dirLen else 0f
            val normalizedDirY = if (dirLen > 0) dirY / dirLen else 0f

            // Add randomness to direction
            val angleOffset = (Random.nextFloat() - 0.5f) * PI.toFloat() * 0.5f
            val cosAngle = cos(angleOffset)
            val sinAngle = sin(angleOffset)
            val finalDirX = normalizedDirX * cosAngle - normalizedDirY * sinAngle
            val finalDirY = normalizedDirX * sinAngle + normalizedDirY * cosAngle

            // Velocity - particles scatter outward with varying speeds
            val baseSpeed = 300f + Random.nextFloat() * 400f  // 300-700 px/s
            val velocity = Offset(
                finalDirX * baseSpeed,
                finalDirY * baseSpeed
            )

            // Stagger spawn times for cascading effect
            val spawnDelay = (i.toFloat() / particleCount * 200).toLong()
            val adjustedCreatedAt = effectStartTime + spawnDelay

            // Lifetime with variation
            val lifetime = baseLifetime * (0.7f + Random.nextFloat() * 0.5f)

            // Particle size
            val size = 2.5f + Random.nextFloat() * 3.5f  // 2.5-6 dp

            particles.add(
                SplashSparkParticle(
                    startPos = spawnPos,
                    velocity = velocity,
                    color = sparkColors.random(),
                    size = size,
                    lifetime = lifetime,
                    createdAt = adjustedCreatedAt,
                    hasTrail = Random.nextFloat() < 0.6f  // 60% chance of trail
                )
            )
        }
    }

    /**
     * Update effect state. Call this each frame.
     * @return True if effect is still active
     */
    fun update(currentTime: Long): Boolean {
        if (!isActive) return false

        // Remove expired particles
        particles.removeAll { it.isExpired(currentTime) }

        // Check if effect is complete
        val elapsed = currentTime - effectStartTime
        if (elapsed > totalDuration + 200 && particles.isEmpty()) {
            isActive = false
        }

        return isActive
    }

    /**
     * Get the progress of the dissolve (0-1).
     * Used to fade out the icon/text as particles spawn.
     */
    fun getDissolveProgress(currentTime: Long): Float {
        if (!isActive) return 0f
        val elapsed = currentTime - effectStartTime
        // Fade out quickly in the first 300ms
        return (elapsed / 300f).coerceIn(0f, 1f)
    }

    fun getParticles(): List<SplashSparkParticle> = particles.toList()

    fun isActive(): Boolean = isActive

    fun clear() {
        particles.clear()
        isActive = false
    }
}
