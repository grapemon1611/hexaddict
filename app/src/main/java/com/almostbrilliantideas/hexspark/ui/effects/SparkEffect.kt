package com.almostbrilliantideas.hexspark.ui.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.almostbrilliantideas.hexspark.model.AxialCoord
import com.almostbrilliantideas.hexspark.game.LineDefinitions
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A single spark particle.
 */
data class SparkParticle(
    val startPos: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val lifetime: Float,  // Total lifetime in ms
    val createdAt: Long,  // System time when created
    val hasTrail: Boolean = false
) {
    /**
     * Get current position based on elapsed time.
     * Particles slow down slightly over time.
     */
    fun positionAt(currentTime: Long): Offset {
        val elapsed = (currentTime - createdAt).toFloat()
        val progress = (elapsed / lifetime).coerceIn(0f, 1f)
        // Deceleration factor - particles slow down
        val decel = 1f - progress * 0.3f
        return Offset(
            startPos.x + velocity.x * progress * decel,
            startPos.y + velocity.y * progress * decel
        )
    }

    /**
     * Get current alpha based on elapsed time.
     * DEBUG: Start fully opaque, fade slowly.
     */
    fun alphaAt(currentTime: Long): Float {
        val elapsed = (currentTime - createdAt).toFloat()
        val progress = (elapsed / lifetime).coerceIn(0f, 1f)
        // DEBUG: Instant fade in, slow linear fade out starting at 30%
        return if (progress < 0.3f) {
            1f // Full opacity for first 30% of lifetime
        } else {
            // Fade from 100% to 0% over remaining 70%
            1f - ((progress - 0.3f) / 0.7f)
        }
    }

    /**
     * Check if particle has expired.
     */
    fun isExpired(currentTime: Long): Boolean {
        return (currentTime - createdAt) > lifetime
    }
}

/**
 * Animation state for a single cell during clearing.
 */
data class CellClearState(
    val coord: AxialCoord,
    val ignitionTime: Long,     // When this cell starts its animation
    val flashDuration: Float,   // Duration of white flash in ms
    val originalColor: Color,   // The cell's original color
    val sparkColor: Color,      // Color of sparks (may differ for same-color lines)
    val particleCount: Int,     // Number of particles to emit
    val isIntersection: Boolean // Whether this cell is at an intersection
)

/**
 * Clear effect tier determining visual intensity.
 * DEBUG: All values increased significantly for visibility testing.
 */
enum class ClearTier(
    val particlesPerCell: IntRange,
    val flashBrightness: Float,
    val particleLifetime: Float,  // ms
    val sparkSpeed: Float         // pixels per normalized time (will be scaled by dp)
) {
    // DEBUG: Minimum 20 particles, 500ms lifetime, high velocity for all tiers
    SINGLE_LINE(20..25, 0.9f, 500f, 400f),
    DOUBLE_LINE(22..28, 0.92f, 520f, 420f),
    TRIPLE_LINE(25..32, 0.95f, 550f, 450f),
    COLOR_BONUS(24..30, 0.95f, 540f, 440f),
    PAYDAY(28..36, 0.98f, 580f, 480f),
    JACKPOT(35..50, 1.0f, 620f, 550f)
}

/**
 * Manages the spark particle effect system for line clearing.
 */
class SparkEffectState {
    private val particles = mutableListOf<SparkParticle>()
    private val cellStates = mutableMapOf<AxialCoord, CellClearState>()
    private val spawnedCells = mutableSetOf<AxialCoord>() // Track which cells have spawned particles
    private var effectStartTime: Long = 0
    private var isActive = false

    // Time scale for slow motion effect (1.0 = normal, 2.0 = half speed, etc.)
    private var timeScale: Float = 1f
    // Extra particle multiplier for PAYDAY
    private var particleMultiplier: Float = 1f

    // Colors for standard sparks - DEBUG: Using bright, fully saturated colors
    private val standardSparkColors = listOf(
        Color(0xFFFFFFFF),  // Pure white
        Color(0xFFFFD700),  // Bright gold
        Color(0xFFFFFF00),  // Yellow
        Color(0xFFFFFACD),  // Lemon chiffon (bright)
        Color(0xFFFFE135)   // Banana yellow
    )

    /**
     * Start a new clear effect.
     *
     * @param completedLines The lines that were completed
     * @param cellColors Map of cell coordinates to their colors
     * @param tier The clear tier determining visual intensity
     * @param sameColorLines Lines that are same-color (for colored sparks)
     * @param hexSize Size of hexagons for particle positioning
     * @param cellToPixel Function to convert axial coords to pixel positions
     * @param slowMotionScale Time scale for slow motion (1.0 = normal, 2.0 = half speed)
     * @param extraParticles Particle multiplier for PAYDAY (1.0 = normal, 1.5 = 50% more)
     */
    fun startEffect(
        completedLines: List<LineDefinitions.Line>,
        cellColors: Map<AxialCoord, Color>,
        tier: ClearTier,
        sameColorLines: Set<LineDefinitions.Line>,
        hexSize: Float,
        cellToPixel: (AxialCoord) -> Offset,
        slowMotionScale: Float = 1f,
        extraParticles: Float = 1f
    ) {
        clear()
        isActive = true
        effectStartTime = System.currentTimeMillis()
        timeScale = slowMotionScale
        particleMultiplier = extraParticles

        // Count how many lines each cell belongs to (for intersection bonus)
        val cellLineCount = mutableMapOf<AxialCoord, Int>()
        completedLines.forEach { line ->
            line.cells.forEach { coord ->
                cellLineCount[coord] = (cellLineCount[coord] ?: 0) + 1
            }
        }

        // Calculate ignition timing for sequential travel along each line
        // Scale delays by timeScale for slow motion effect
        val sequentialDelayPerCell = 25f * timeScale // ms between cells in sequence
        val cellIgnitionTimes = mutableMapOf<AxialCoord, Long>()

        completedLines.forEachIndexed { lineIndex, line ->
            val lineStartDelay = (lineIndex * 15L * timeScale).toLong() // Stagger line starts slightly
            line.cells.forEachIndexed { cellIndex, coord ->
                val cellDelay = effectStartTime + lineStartDelay + (cellIndex * sequentialDelayPerCell).toLong()
                // Take earliest ignition time if cell is in multiple lines
                val existing = cellIgnitionTimes[coord]
                if (existing == null || cellDelay < existing) {
                    cellIgnitionTimes[coord] = cellDelay
                }
            }
        }

        // Determine spark color for each cell
        val cellSparkColors = mutableMapOf<AxialCoord, Color>()
        for (line in completedLines) {
            val isSameColor = line in sameColorLines
            for (coord in line.cells) {
                if (isSameColor) {
                    // Use the line's color for same-color lines
                    cellColors[coord]?.let { cellSparkColors[coord] = it }
                } else if (coord !in cellSparkColors) {
                    // Standard gold/white sparks for non-same-color lines
                    cellSparkColors[coord] = standardSparkColors.random()
                }
            }
        }

        // Create cell states
        val allCellsInLines = completedLines.flatMap { it.cells }.toSet()
        for (coord in allCellsInLines) {
            val isIntersection = (cellLineCount[coord] ?: 1) > 1
            val baseParticles = Random.nextInt(tier.particlesPerCell.first, tier.particlesPerCell.last + 1)
            // Apply particle multiplier and intersection bonus
            val particleCount = if (isIntersection) {
                (baseParticles * 1.5f * particleMultiplier).toInt() // 50% more at intersections + multiplier
            } else {
                (baseParticles * particleMultiplier).toInt()
            }

            cellStates[coord] = CellClearState(
                coord = coord,
                ignitionTime = cellIgnitionTimes[coord] ?: effectStartTime,
                flashDuration = (80f + Random.nextFloat() * 40f) * timeScale, // Scale flash duration
                originalColor = cellColors[coord] ?: Color.Gray,
                sparkColor = cellSparkColors[coord] ?: standardSparkColors.random(),
                particleCount = particleCount,
                isIntersection = isIntersection
            )
        }
    }

    /**
     * Start a JACKPOT effect with wave radiation from clearing lines outward.
     *
     * @param slowMotionScale Time scale for slow motion (1.0 = normal, 4.0 = quarter speed for JACKPOT)
     */
    fun startJackpotEffect(
        completedLines: List<LineDefinitions.Line>,
        allOccupiedCells: Map<AxialCoord, Color>,
        hexSize: Float,
        cellToPixel: (AxialCoord) -> Offset,
        slowMotionScale: Float = 1f
    ) {
        clear()
        isActive = true
        effectStartTime = System.currentTimeMillis()
        timeScale = slowMotionScale
        particleMultiplier = 1f  // JACKPOT already has high particle counts

        val tier = ClearTier.JACKPOT

        // Get all cells that were in completed lines (these ignite first)
        val lineCells = completedLines.flatMap { it.cells }.toSet()

        // Count intersections
        val cellLineCount = mutableMapOf<AxialCoord, Int>()
        completedLines.forEach { line ->
            line.cells.forEach { coord ->
                cellLineCount[coord] = (cellLineCount[coord] ?: 0) + 1
            }
        }

        // Calculate center of the clearing lines for wave propagation
        val lineCellPixels = lineCells.mapNotNull { coord ->
            allOccupiedCells[coord]?.let { coord to cellToPixel(coord) }
        }
        val centerX = lineCellPixels.map { it.second.x }.average().toFloat()
        val centerY = lineCellPixels.map { it.second.y }.average().toFloat()
        val center = Offset(centerX, centerY)

        // Calculate ignition times based on distance from center
        // Scale wave speed by timeScale for slow motion
        val waveSpeed = 0.15f * timeScale // ms per pixel distance
        val cellIgnitionTimes = mutableMapOf<AxialCoord, Long>()

        for ((coord, _) in allOccupiedCells) {
            val pixelPos = cellToPixel(coord)
            val distance = kotlin.math.sqrt(
                (pixelPos.x - center.x) * (pixelPos.x - center.x) +
                (pixelPos.y - center.y) * (pixelPos.y - center.y)
            )
            // Line cells ignite immediately, others based on wave distance
            val delay = if (coord in lineCells) {
                0L
            } else {
                (distance * waveSpeed).toLong()
            }
            cellIgnitionTimes[coord] = effectStartTime + delay
        }

        // Create cell states for ALL occupied cells
        for ((coord, color) in allOccupiedCells) {
            val isIntersection = (cellLineCount[coord] ?: 0) > 1
            val isLineCell = coord in lineCells
            val baseParticles = Random.nextInt(tier.particlesPerCell.first, tier.particlesPerCell.last + 1)
            val particleCount = when {
                isIntersection -> (baseParticles * 1.5f).toInt()
                isLineCell -> baseParticles
                else -> (baseParticles * 0.8f).toInt() // Slightly fewer for wave cells
            }

            cellStates[coord] = CellClearState(
                coord = coord,
                ignitionTime = cellIgnitionTimes[coord] ?: effectStartTime,
                flashDuration = (100f + Random.nextFloat() * 50f) * timeScale, // Scale flash duration
                originalColor = color,
                sparkColor = color, // JACKPOT uses cell's own color
                particleCount = particleCount,
                isIntersection = isIntersection
            )
        }
    }

    /**
     * Update effect state and spawn particles.
     * Call this each frame.
     *
     * @param currentTime Current system time
     * @param tier The clear tier for particle properties
     * @param cellToPixel Function to convert axial coords to pixel positions
     * @return True if effect is still active
     */
    fun update(
        currentTime: Long,
        tier: ClearTier,
        cellToPixel: (AxialCoord) -> Offset
    ): Boolean {
        if (!isActive) return false

        // Remove expired particles
        particles.removeAll { it.isExpired(currentTime) }

        // Check for cells that need to spawn particles
        // Spawn window scaled by timeScale for slow motion
        val spawnWindow = (200 * timeScale).toLong()
        for ((coord, state) in cellStates) {
            // Skip if already spawned
            if (coord in spawnedCells) continue

            val timeSinceIgnition = currentTime - state.ignitionTime
            // Spawn when ignition time has passed (with generous window)
            if (timeSinceIgnition >= 0 && timeSinceIgnition < spawnWindow) {
                spawnParticlesForCell(coord, state, tier, cellToPixel)
                spawnedCells.add(coord)
            }
        }

        // Check if effect is complete - scale end buffer by timeScale
        val latestIgnition = cellStates.values.maxOfOrNull { it.ignitionTime } ?: effectStartTime
        val effectEndTime = latestIgnition + (tier.particleLifetime * timeScale).toLong() + (300 * timeScale).toLong()
        if (currentTime > effectEndTime && particles.isEmpty()) {
            isActive = false
        }

        return isActive
    }

    /**
     * Spawn particles for a single cell.
     * Particle velocity is scaled down and lifetime is scaled up for slow motion.
     */
    private fun spawnParticlesForCell(
        coord: AxialCoord,
        state: CellClearState,
        tier: ClearTier,
        cellToPixel: (AxialCoord) -> Offset
    ) {
        val center = cellToPixel(coord)
        // Scale velocity down so particles move slower
        val baseSpeed = tier.sparkSpeed / timeScale
        // Scale lifetime up so particles last longer
        val lifetime = tier.particleLifetime * timeScale

        // Spawn particles in random directions
        repeat(state.particleCount) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            // DEBUG: Increased speed variation range
            val speedVariation = 0.8f + Random.nextFloat() * 0.6f // 80-140% of base speed
            val speed = baseSpeed * speedVariation

            val velocity = Offset(
                cos(angle) * speed,
                sin(angle) * speed
            )

            // DEBUG: Always use bright colors - mostly white/gold
            val sparkColor = if (Random.nextFloat() < 0.5f) {
                Color.White
            } else {
                state.sparkColor
            }

            // Particle sizes in dp (will be converted to pixels in renderer)
            val particleSize = if (state.isIntersection) {
                3.5f + Random.nextFloat() * 1.5f // 3.5-5 dp at intersections
            } else {
                2.5f + Random.nextFloat() * 2.5f // 2.5-5 dp normally
            }

            particles.add(
                SparkParticle(
                    startPos = center,
                    velocity = velocity,
                    color = sparkColor,
                    size = particleSize,
                    lifetime = lifetime * (0.9f + Random.nextFloat() * 0.3f), // 90-120% of base (minimum ~450ms)
                    createdAt = System.currentTimeMillis(),
                    hasTrail = true // DEBUG: Always show trail for visibility
                )
            )
        }
    }

    /**
     * Get current cell flash state for rendering.
     * Returns map of coord -> flash intensity (0-1, where 1 is full white flash)
     */
    fun getCellFlashStates(currentTime: Long): Map<AxialCoord, Float> {
        val result = mutableMapOf<AxialCoord, Float>()

        for ((coord, state) in cellStates) {
            val timeSinceIgnition = currentTime - state.ignitionTime
            if (timeSinceIgnition < 0) continue // Not yet ignited

            val flashProgress = timeSinceIgnition / state.flashDuration
            if (flashProgress > 1f) continue // Flash complete

            // Sharp flash that peaks quickly then fades
            val intensity = if (flashProgress < 0.3f) {
                flashProgress / 0.3f // Ramp up
            } else {
                1f - ((flashProgress - 0.3f) / 0.7f) // Fade out
            }
            result[coord] = intensity.coerceIn(0f, 1f)
        }

        return result
    }

    /**
     * Get current fade state for cells (for disappearing after flash).
     * Returns map of coord -> alpha (1 = fully visible, 0 = invisible)
     */
    fun getCellFadeStates(currentTime: Long): Map<AxialCoord, Float> {
        val result = mutableMapOf<AxialCoord, Float>()
        val fadeDuration = 150f * timeScale // ms to fade out after flash, scaled for slow motion

        for ((coord, state) in cellStates) {
            val timeSinceIgnition = currentTime - state.ignitionTime
            if (timeSinceIgnition < 0) {
                result[coord] = 1f // Not yet ignited, fully visible
                continue
            }

            if (timeSinceIgnition < state.flashDuration) {
                result[coord] = 1f // Still flashing, fully visible
                continue
            }

            val fadeProgress = (timeSinceIgnition - state.flashDuration) / fadeDuration
            result[coord] = (1f - fadeProgress).coerceIn(0f, 1f)
        }

        return result
    }

    /**
     * Get all active particles for rendering.
     */
    fun getParticles(): List<SparkParticle> = particles.toList()

    /**
     * Get all cell states.
     */
    fun getCellStates(): Map<AxialCoord, CellClearState> = cellStates.toMap()

    /**
     * Check if effect is currently active.
     */
    fun isActive(): Boolean = isActive

    /**
     * Clear all state.
     */
    fun clear() {
        particles.clear()
        cellStates.clear()
        spawnedCells.clear()
        isActive = false
    }

    companion object {
        // Base animation duration in ms (for single line)
        private const val BASE_ANIMATION_DURATION_MS = 300L

        /**
         * Determine clear tier from ClearInfo.
         */
        fun tierFromClearInfo(
            linesCleared: Int,
            sameColorLineCount: Int,
            isPayday: Boolean,
            isJackpot: Boolean
        ): ClearTier {
            return when {
                isJackpot -> ClearTier.JACKPOT
                isPayday -> ClearTier.PAYDAY
                sameColorLineCount >= 1 -> ClearTier.COLOR_BONUS
                linesCleared >= 3 -> ClearTier.TRIPLE_LINE
                linesCleared == 2 -> ClearTier.DOUBLE_LINE
                else -> ClearTier.SINGLE_LINE
            }
        }

        /**
         * Calculate slow motion time scale based on clear type.
         * - Single line: 1.0 (normal speed)
         * - Two lines: 2.0 (50% speed, animation takes twice as long)
         * - Three+ lines: 3.0 (33% speed, animation takes three times as long)
         * - PAYDAY: 3.0 (33% speed with extra particle density)
         * - JACKPOT: 4.0 (25% speed, maximum drama)
         */
        fun slowMotionScaleFromClearInfo(
            linesCleared: Int,
            isPayday: Boolean,
            isJackpot: Boolean
        ): Float {
            return when {
                isJackpot -> 4f   // 25% speed - maximum drama
                isPayday -> 3f   // 33% speed with extra particles
                linesCleared >= 3 -> 3f  // 33% speed
                linesCleared == 2 -> 2f  // 50% speed
                else -> 1f  // Normal speed
            }
        }

        /**
         * Calculate extra particle multiplier for PAYDAY clears.
         * Returns 1.5 for PAYDAY (50% more particles), 1.0 otherwise.
         */
        fun particleMultiplierFromClearInfo(isPayday: Boolean): Float {
            return if (isPayday) 1.5f else 1f
        }

        /**
         * Calculate animation duration in ms based on clear type.
         * Duration scales with slow motion.
         */
        fun animationDurationFromClearInfo(
            linesCleared: Int,
            isPayday: Boolean,
            isJackpot: Boolean
        ): Long {
            val scale = slowMotionScaleFromClearInfo(linesCleared, isPayday, isJackpot)
            return (BASE_ANIMATION_DURATION_MS * scale).toLong()
        }

        /**
         * Blend two colors together.
         */
        private fun blendColors(color1: Color, color2: Color, ratio: Float): Color {
            val r = ratio.coerceIn(0f, 1f)
            return Color(
                red = color1.red * (1 - r) + color2.red * r,
                green = color1.green * (1 - r) + color2.green * r,
                blue = color1.blue * (1 - r) + color2.blue * r,
                alpha = color1.alpha * (1 - r) + color2.alpha * r
            )
        }
    }
}
