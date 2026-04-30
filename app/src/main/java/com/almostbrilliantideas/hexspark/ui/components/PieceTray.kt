package com.almostbrilliantideas.hexspark.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.almostbrilliantideas.hexspark.game.HexUtils
import com.almostbrilliantideas.hexspark.model.HexPiece
import com.almostbrilliantideas.hexspark.ui.GameDimensions

/**
 * Tray displaying 2 available pieces for placement.
 *
 * @param onPieceDragStart Called with (index, piece, absolutePosition) when drag starts
 * @param onPieceDrag Called with absolute screen position during drag
 * @param onPieceDragEnd Called when drag ends
 * @param dimensions Responsive layout dimensions for scaling
 */
@Composable
fun PieceTray(
    pieces: List<HexPiece?>,
    dimensions: GameDimensions? = null,
    onPieceDragStart: (Int, HexPiece, Offset) -> Unit,
    onPieceDrag: (Offset) -> Unit,
    onPieceDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paddingHorizontal = dimensions?.pieceTrayPaddingHorizontal ?: 16.dp
    val slotPadding = dimensions?.pieceSlotPadding ?: 8.dp
    val slotCornerRadius = dimensions?.pieceSlotCornerRadius ?: 12.dp
    val previewCanvasSize = dimensions?.piecePreviewCanvasSize ?: 80.dp
    val previewHexSize = dimensions?.piecePreviewHexSize ?: 15f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = paddingHorizontal),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        pieces.forEachIndexed { index, piece ->
            PieceSlot(
                piece = piece,
                slotPadding = slotPadding,
                slotCornerRadius = slotCornerRadius,
                previewCanvasSize = previewCanvasSize,
                previewHexSize = previewHexSize,
                onDragStart = { p, absolutePos ->
                    if (p != null) {
                        onPieceDragStart(index, p, absolutePos)
                    }
                },
                onDrag = onPieceDrag,
                onDragEnd = onPieceDragEnd,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A single slot in the piece tray.
 */
@Composable
private fun PieceSlot(
    piece: HexPiece?,
    slotPadding: Dp,
    slotCornerRadius: Dp,
    previewCanvasSize: Dp,
    previewHexSize: Float,
    onDragStart: (HexPiece?, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var slotPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var currentDragPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(slotPadding)
            .onGloballyPositioned { coordinates ->
                slotPositionInRoot = coordinates.positionInRoot()
            }
            .background(
                color = if (piece != null) Color(0xFF2D2D4A).copy(alpha = 0.85f)
                        else Color(0xFF1A1A2E).copy(alpha = 0.7f),
                shape = RoundedCornerShape(slotCornerRadius)
            )
            .graphicsLayer {
                alpha = if (isDragging) 0.3f else 1f
            }
            .pointerInput(piece) {
                if (piece != null) {
                    detectDragGestures(
                        onDragStart = { localOffset ->
                            isDragging = true
                            // Convert local offset to absolute screen position
                            val absolutePos = Offset(
                                slotPositionInRoot.x + localOffset.x,
                                slotPositionInRoot.y + localOffset.y
                            )
                            currentDragPosition = absolutePos
                            onDragStart(piece, absolutePos)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Update absolute position by adding delta
                            currentDragPosition = Offset(
                                currentDragPosition.x + dragAmount.x,
                                currentDragPosition.y + dragAmount.y
                            )
                            onDrag(currentDragPosition)
                        },
                        onDragEnd = {
                            isDragging = false
                            onDragEnd()
                        },
                        onDragCancel = {
                            isDragging = false
                            onDragEnd()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (piece != null) {
            PiecePreview(
                piece = piece,
                canvasSize = previewCanvasSize,
                hexSize = previewHexSize
            )
        }
    }
}

/**
 * Visual preview of a piece in the tray.
 */
@Composable
fun PiecePreview(
    piece: HexPiece,
    modifier: Modifier = Modifier,
    canvasSize: Dp = 80.dp,
    hexSize: Float = 15f
) {
    Canvas(
        modifier = modifier.size(canvasSize)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Find piece bounds to center it
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (cell in piece.cells) {
            val pos = HexUtils.axialToPixel(cell, hexSize)
            minX = minOf(minX, pos.x)
            maxX = maxOf(maxX, pos.x)
            minY = minOf(minY, pos.y)
            maxY = maxOf(maxY, pos.y)
        }

        val pieceWidth = maxX - minX
        val pieceHeight = maxY - minY
        val offsetX = centerX - pieceWidth / 2 - minX
        val offsetY = centerY - pieceHeight / 2 - minY

        // Draw each cell of the piece
        for (cell in piece.cells) {
            val pos = HexUtils.axialToPixel(cell, hexSize)
            val center = Offset(pos.x + offsetX, pos.y + offsetY)

            drawHexagon(
                center = center,
                size = hexSize * 0.9f,
                fillColor = piece.color,
                strokeColor = Color.White.copy(alpha = 0.3f),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * Floating piece being dragged - rendered at absolute screen position.
 */
@Composable
fun DraggedPiece(
    piece: HexPiece,
    position: Offset,
    dimensions: GameDimensions? = null,
    modifier: Modifier = Modifier
) {
    val pieceSize = dimensions?.draggedPieceCanvasSize ?: 120.dp
    val hexSize = dimensions?.draggedPieceHexSize ?: 20f

    Canvas(
        modifier = modifier
            .size(pieceSize)
            .graphicsLayer {
                // Position so piece is centered on finger
                translationX = position.x - (pieceSize.toPx() / 2)
                translationY = position.y - (pieceSize.toPx() / 2)
            }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Find piece bounds to center it within the canvas
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (cell in piece.cells) {
            val pos = HexUtils.axialToPixel(cell, hexSize)
            minX = minOf(minX, pos.x)
            maxX = maxOf(maxX, pos.x)
            minY = minOf(minY, pos.y)
            maxY = maxOf(maxY, pos.y)
        }

        val pieceWidth = maxX - minX
        val pieceHeight = maxY - minY
        val offsetX = centerX - pieceWidth / 2 - minX
        val offsetY = centerY - pieceHeight / 2 - minY

        for (cell in piece.cells) {
            val pos = HexUtils.axialToPixel(cell, hexSize)
            val center = Offset(pos.x + offsetX, pos.y + offsetY)

            drawHexagon(
                center = center,
                size = hexSize * 0.9f,
                fillColor = piece.color.copy(alpha = 0.85f),
                strokeColor = Color.White.copy(alpha = 0.6f),
                strokeWidth = 2f
            )
        }
    }
}
