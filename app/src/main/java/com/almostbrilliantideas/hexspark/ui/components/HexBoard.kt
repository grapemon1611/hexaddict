package com.almostbrilliantideas.hexspark.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.almostbrilliantideas.hexspark.game.HexUtils
import com.almostbrilliantideas.hexspark.model.AxialCoord
import com.almostbrilliantideas.hexspark.model.ClearInfo
import com.almostbrilliantideas.hexspark.model.HexCell
import com.almostbrilliantideas.hexspark.ui.LineHighlight
import com.almostbrilliantideas.hexspark.ui.effects.ClearTier
import com.almostbrilliantideas.hexspark.ui.effects.SparkEffectState
import com.almostbrilliantideas.hexspark.ui.effects.SparkParticle
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * The main hex game board displaying 77 hexagonal cells.
 */
@Composable
fun HexBoard(
    cells: Map<AxialCoord, HexCell>,
    lineHighlights: Map<AxialCoord, LineHighlight> = emptyMap(),
    highlightedCells: Set<AxialCoord> = emptySet(),
    previewCells: Map<AxialCoord, Color> = emptyMap(),
    clearingCells: Set<AxialCoord> = emptySet(),
    clearInfo: ClearInfo? = null,
    invalidPreview: Boolean = false,
    onCellTap: (AxialCoord) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val boardCoords = remember { HexUtils.getAllBoardCoords() }
    val density = LocalDensity.current

    // Spark effect state for line clearing animation
    val sparkEffect = remember { SparkEffectState() }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isAnimating by remember { mutableStateOf(false) }

    // Store hex size for particle calculations
    var cachedHexSize by remember { mutableStateOf(0f) }
    var cachedBoardOffset by remember { mutableStateOf(Offset.Zero) }

    // Track if we need to start a spark effect (deferred until hex size is known)
    var pendingClearInfo by remember { mutableStateOf<ClearInfo?>(null) }

    // When clearInfo changes, mark it as pending
    LaunchedEffect(clearInfo) {
        if (clearInfo != null && clearInfo.completedLines.isNotEmpty()) {
            pendingClearInfo = clearInfo
        }
    }

    // Start spark effect once we have valid hex size
    LaunchedEffect(pendingClearInfo, cachedHexSize) {
        val info = pendingClearInfo
        if (info != null && cachedHexSize > 0f) {
            val tier = SparkEffectState.tierFromClearInfo(
                info.linesCleared,
                info.sameColorLineCount,
                info.isPayday,
                info.isJackpot
            )

            // Calculate slow motion scale based on clear type
            val slowMotionScale = SparkEffectState.slowMotionScaleFromClearInfo(
                linesCleared = info.linesCleared,
                isPayday = info.isPayday,
                isJackpot = info.isJackpot
            )

            // Extra particles for PAYDAY
            val particleMultiplier = SparkEffectState.particleMultiplierFromClearInfo(info.isPayday)

            // Function to convert axial coords to pixel positions
            val cellToPixel: (AxialCoord) -> Offset = { coord ->
                val pixelPos = HexUtils.axialToPixel(coord, cachedHexSize)
                Offset(
                    pixelPos.x + cachedBoardOffset.x,
                    pixelPos.y + cachedBoardOffset.y
                )
            }

            if (info.isJackpot) {
                sparkEffect.startJackpotEffect(
                    completedLines = info.completedLines,
                    allOccupiedCells = info.cellColors,
                    hexSize = cachedHexSize,
                    cellToPixel = cellToPixel,
                    slowMotionScale = slowMotionScale
                )
            } else {
                sparkEffect.startEffect(
                    completedLines = info.completedLines,
                    cellColors = info.cellColors,
                    tier = tier,
                    sameColorLines = info.sameColorLines,
                    hexSize = cachedHexSize,
                    cellToPixel = cellToPixel,
                    slowMotionScale = slowMotionScale,
                    extraParticles = particleMultiplier
                )
            }
            isAnimating = true
            pendingClearInfo = null // Clear pending so we don't restart
        }
    }

    // Animation loop for spark particles
    LaunchedEffect(isAnimating) {
        while (isAnimating) {
            currentTime = System.currentTimeMillis()
            val tier = clearInfo?.let {
                SparkEffectState.tierFromClearInfo(
                    it.linesCleared,
                    it.sameColorLineCount,
                    it.isPayday,
                    it.isJackpot
                )
            } ?: ClearTier.SINGLE_LINE

            val cellToPixel: (AxialCoord) -> Offset = { coord ->
                val pixelPos = HexUtils.axialToPixel(coord, cachedHexSize)
                Offset(
                    pixelPos.x + cachedBoardOffset.x,
                    pixelPos.y + cachedBoardOffset.y
                )
            }

            isAnimating = sparkEffect.update(currentTime, tier, cellToPixel)
            delay(16) // ~60fps
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)  // Portrait ratio for hex board
            .background(
                color = Color(0xFF252540).copy(alpha = 0.85f),  // Frosted effect
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Calculate hex size based on canvas size
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val hexSize = calculateHexSize(canvasWidth, canvasHeight)
                        val boardOffset = calculateBoardOffset(canvasWidth, canvasHeight, hexSize)

                        // Convert tap position to axial coordinates
                        val adjustedTap = Offset(
                            tapOffset.x - boardOffset.x,
                            tapOffset.y - boardOffset.y
                        )
                        val axialCoord = HexUtils.pixelToAxial(adjustedTap, hexSize)

                        // Only trigger if tap is on a valid board cell
                        if (axialCoord in boardCoords) {
                            onCellTap(axialCoord)
                        }
                    }
                }
        ) {
            val hexSize = calculateHexSize(size.width, size.height)
            val boardOffset = calculateBoardOffset(size.width, size.height, hexSize)

            // Cache these for particle calculations
            cachedHexSize = hexSize
            cachedBoardOffset = boardOffset

            // Get spark effect states
            val flashStates = sparkEffect.getCellFlashStates(currentTime)
            val fadeStates = sparkEffect.getCellFadeStates(currentTime)
            val particles = sparkEffect.getParticles()

            // Draw all cells
            for ((coord, cell) in cells) {
                val pixelPos = HexUtils.axialToPixel(coord, hexSize)
                val center = Offset(
                    pixelPos.x + boardOffset.x,
                    pixelPos.y + boardOffset.y
                )

                // Determine cell appearance
                val isHighlighted = coord in highlightedCells
                val previewColor = previewCells[coord]
                val isPreview = previewColor != null
                val isClearing = coord in clearingCells

                // Check if this cell is part of spark animation
                val flashIntensity = flashStates[coord]
                val fadeAlpha = fadeStates[coord]

                when {
                    isClearing && cell.isOccupied && (flashIntensity != null || fadeAlpha != null) -> {
                        // New spark-based clearing animation
                        val pieceColor = cell.color!!
                        val alpha = fadeAlpha ?: 1f
                        val flash = flashIntensity ?: 0f

                        // Blend to white based on flash intensity
                        val blendedColor = Color(
                            red = pieceColor.red + (1f - pieceColor.red) * flash,
                            green = pieceColor.green + (1f - pieceColor.green) * flash,
                            blue = pieceColor.blue + (1f - pieceColor.blue) * flash,
                            alpha = alpha
                        )

                        if (alpha > 0.01f) {
                            drawHexagon(
                                center = center,
                                size = hexSize * 0.95f,
                                fillColor = blendedColor,
                                strokeColor = Color.White.copy(alpha = alpha * 0.5f),
                                strokeWidth = 2f
                            )
                        }
                    }
                    isClearing && cell.isOccupied -> {
                        // Cell is clearing but spark effect hasn't started yet - show normally
                        val pieceColor = cell.color!!
                        drawHexagon(
                            center = center,
                            size = hexSize * 0.95f,
                            fillColor = pieceColor,
                            strokeColor = Color.White.copy(alpha = 0.3f),
                            strokeWidth = 1.5f
                        )
                    }
                    isPreview && invalidPreview -> {
                        // Invalid placement preview - red tint
                        drawHexagon(
                            center = center,
                            size = hexSize * 0.95f,
                            fillColor = Color(0x80FF4444),
                            strokeColor = Color(0xFFFF4444),
                            strokeWidth = 2f
                        )
                    }
                    isPreview -> {
                        // Valid placement preview
                        drawHexagon(
                            center = center,
                            size = hexSize * 0.95f,
                            fillColor = previewColor!!.copy(alpha = 0.7f),
                            strokeColor = Color.White.copy(alpha = 0.5f),
                            strokeWidth = 2f
                        )
                    }
                    cell.isOccupied -> {
                        // Occupied cell with piece color
                        val pieceColor = cell.color!!

                        // Draw ambient glow if this cell is part of a same-color line
                        val highlight = lineHighlights[coord]
                        if (highlight != null) {
                            // Outer glow - larger hexagon with low alpha
                            val glowAlpha = 0.15f + (highlight.intensity * 0.20f)  // 0.15 to 0.35
                            val glowSize = hexSize * (1.0f + highlight.intensity * 0.15f)  // Slight size increase
                            drawHexagon(
                                center = center,
                                size = glowSize,
                                fillColor = highlight.color.copy(alpha = glowAlpha),
                                strokeColor = Color.Transparent,
                                strokeWidth = 0f
                            )
                        }

                        // Normal cell rendering
                        drawHexagon(
                            center = center,
                            size = hexSize * 0.95f,
                            fillColor = pieceColor,
                            strokeColor = Color.White.copy(alpha = 0.3f),
                            strokeWidth = 1.5f
                        )
                    }
                    isHighlighted -> {
                        // Highlighted empty cell
                        drawHexagon(
                            center = center,
                            size = hexSize * 0.95f,
                            fillColor = Color(0xFF3D3A5C),
                            strokeColor = Color(0xFF8B84D4),
                            strokeWidth = 2f
                        )
                    }
                    else -> {
                        // Empty cell
                        drawHexagon(
                            center = center,
                            size = hexSize * 0.95f,
                            fillColor = Color(0xFF2D2D4A),
                            strokeColor = Color(0xFF4A4A6A),
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // Draw spark particles on top of ALL cells - this MUST be last
            drawSparkParticles(particles, currentTime, density)
        }
    }
}

/**
 * Draw all spark particles.
 * DEBUG: Significantly increased visibility for testing.
 */
private fun DrawScope.drawSparkParticles(
    particles: List<SparkParticle>,
    currentTime: Long,
    density: Density
) {
    // Convert dp to pixels using density
    val dpToPx = with(density) { 1.dp.toPx() }

    for (particle in particles) {
        val pos = particle.positionAt(currentTime)
        val alpha = particle.alphaAt(currentTime)

        if (alpha > 0.01f) {
            // DEBUG: particle.size is now in dp, convert to pixels
            val radiusPx = particle.size * dpToPx

            // Draw outer glow for more visibility
            drawCircle(
                color = particle.color.copy(alpha = alpha * 0.3f),
                radius = radiusPx * 1.5f,
                center = pos
            )

            // Draw main particle as a circle
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = radiusPx,
                center = pos
            )

            // Draw bright center
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.8f),
                radius = radiusPx * 0.4f,
                center = pos
            )

            // Draw trail for all particles (DEBUG: always show trail)
            if (particle.hasTrail && alpha > 0.2f) {
                val trailLength = 4
                for (i in 1..trailLength) {
                    val trailTime = currentTime - (i * 25L)
                    val trailPos = particle.positionAt(trailTime)
                    val trailAlpha = (alpha * (1f - i.toFloat() / (trailLength + 1))) * 0.6f
                    drawCircle(
                        color = particle.color.copy(alpha = trailAlpha),
                        radius = radiusPx * (1f - i * 0.12f),
                        center = trailPos
                    )
                }
            }
        }
    }
}

/**
 * Calculate appropriate hex size for the canvas.
 */
private fun calculateHexSize(canvasWidth: Float, canvasHeight: Float): Float {
    val sqrt3 = sqrt(3.0).toFloat()

    // Board spans roughly:
    // - Horizontal: ~9 hexes wide (considering offset rows)
    // - Vertical: 9 rows

    // Width needed for 9 hexes: size * sqrt(3) * 9 + some offset
    // Height needed for 9 rows: size * 1.5 * 8 + size * 2 (first and last full height)

    val maxHexWidth = canvasWidth / (sqrt3 * 9.5f)
    val maxHexHeight = canvasHeight / (1.5f * 8f + 2f)

    return minOf(maxHexWidth, maxHexHeight) * 0.95f
}

/**
 * Calculate offset to center the board in the canvas.
 */
private fun calculateBoardOffset(canvasWidth: Float, canvasHeight: Float, hexSize: Float): Offset {
    val sqrt3 = sqrt(3.0).toFloat()

    // Calculate actual board dimensions
    val boardWidth = hexSize * sqrt3 * 9f  // Approximate
    val boardHeight = hexSize * 1.5f * 8f + hexSize * 2f

    // Center the board
    val offsetX = (canvasWidth - boardWidth) / 2f + hexSize * sqrt3 * 0.5f
    val offsetY = (canvasHeight - boardHeight) / 2f + hexSize

    return Offset(offsetX, offsetY)
}
