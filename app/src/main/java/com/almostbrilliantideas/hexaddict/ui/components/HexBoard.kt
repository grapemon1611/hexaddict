package com.almostbrilliantideas.hexaddict.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.almostbrilliantideas.hexaddict.game.HexUtils
import com.almostbrilliantideas.hexaddict.model.AxialCoord
import com.almostbrilliantideas.hexaddict.model.HexCell
import kotlin.math.sqrt

/**
 * The main hex game board displaying 77 hexagonal cells.
 */
@Composable
fun HexBoard(
    cells: Map<AxialCoord, HexCell>,
    highlightedCells: Set<AxialCoord> = emptySet(),
    previewCells: Map<AxialCoord, Color> = emptyMap(),
    clearingCells: Set<AxialCoord> = emptySet(),
    invalidPreview: Boolean = false,
    onCellTap: (AxialCoord) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val boardCoords = remember { HexUtils.getAllBoardCoords() }

    // Animation for clearing cells - flash white then fade out
    val clearAnimation = remember { Animatable(0f) }

    LaunchedEffect(clearingCells) {
        if (clearingCells.isNotEmpty()) {
            // Flash to white then fade
            clearAnimation.snapTo(0f)
            clearAnimation.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)  // Portrait ratio for hex board
            .background(
                color = Color(0xFF252540),
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
            val clearProgress = clearAnimation.value

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

                when {
                    isClearing && cell.isOccupied -> {
                        // Clearing animation: flash white then fade out
                        val pieceColor = cell.color!!

                        // First half: blend to white, second half: fade out
                        val (blendColor, alpha) = if (clearProgress < 0.5f) {
                            // Flash to white (0 -> 0.5 maps to 0 -> 1 for white blend)
                            val whiteBlend = clearProgress * 2f
                            val blended = Color(
                                red = pieceColor.red + (1f - pieceColor.red) * whiteBlend,
                                green = pieceColor.green + (1f - pieceColor.green) * whiteBlend,
                                blue = pieceColor.blue + (1f - pieceColor.blue) * whiteBlend,
                                alpha = 1f
                            )
                            blended to 1f
                        } else {
                            // Fade out (0.5 -> 1 maps to 1 -> 0 for alpha)
                            val fadeOut = (clearProgress - 0.5f) * 2f
                            Color.White to (1f - fadeOut)
                        }

                        drawHexagon(
                            center = center,
                            size = hexSize * 0.95f,
                            fillColor = blendColor.copy(alpha = alpha),
                            strokeColor = Color.White.copy(alpha = alpha * 0.5f),
                            strokeWidth = 2f
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
