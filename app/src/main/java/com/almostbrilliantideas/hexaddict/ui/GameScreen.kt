package com.almostbrilliantideas.hexaddict.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almostbrilliantideas.hexaddict.game.HexUtils
import com.almostbrilliantideas.hexaddict.model.HexPiece
import com.almostbrilliantideas.hexaddict.ui.components.DraggedPiece
import com.almostbrilliantideas.hexaddict.ui.components.HexBoard
import com.almostbrilliantideas.hexaddict.ui.components.PieceTray
import kotlin.math.sqrt

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val previewCells by viewModel.previewCells.collectAsState()
    val isValidPlacement by viewModel.isValidPlacement.collectAsState()

    // Drag state - positions are in absolute screen coordinates
    var draggedPiece by remember { mutableStateOf<HexPiece?>(null) }
    var draggedPieceIndex by remember { mutableStateOf(-1) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }

    // Board position and size for coordinate conversion
    var boardPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score display
            ScoreDisplay(
                score = gameState.score,
                bestScore = gameState.bestScore
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hex board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        boardPositionInRoot = coordinates.positionInRoot()
                        boardSize = coordinates.size
                    }
            ) {
                HexBoard(
                    cells = gameState.board,
                    previewCells = previewCells,
                    clearingCells = gameState.clearingCells,
                    invalidPreview = !isValidPlacement,
                    onCellTap = { coord ->
                        viewModel.onCellTap(coord)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Piece tray
            PieceTray(
                pieces = gameState.piecesTray,
                onPieceDragStart = { index, piece, absolutePosition ->
                    draggedPiece = piece
                    draggedPieceIndex = index
                    dragPosition = absolutePosition
                },
                onPieceDrag = { absolutePosition ->
                    dragPosition = absolutePosition

                    // Calculate board-relative position for preview
                    draggedPiece?.let { piece ->
                        val targetCoord = screenPositionToAxial(
                            screenPos = absolutePosition,
                            boardPosition = boardPositionInRoot,
                            boardWidth = boardSize.width.toFloat(),
                            boardHeight = boardSize.height.toFloat()
                        )
                        viewModel.updatePreview(piece, targetCoord)
                    }
                },
                onPieceDragEnd = {
                    draggedPiece?.let { piece ->
                        val targetCoord = screenPositionToAxial(
                            screenPos = dragPosition,
                            boardPosition = boardPositionInRoot,
                            boardWidth = boardSize.width.toFloat(),
                            boardHeight = boardSize.height.toFloat()
                        )
                        viewModel.placePiece(draggedPieceIndex, piece, targetCoord)
                    }

                    draggedPiece = null
                    draggedPieceIndex = -1
                    viewModel.clearPreview()
                },
                modifier = Modifier.height(120.dp)
            )
        }

        // Dragged piece overlay - rendered at absolute screen position
        if (draggedPiece != null) {
            DraggedPiece(
                piece = draggedPiece!!,
                position = dragPosition
            )
        }

        // Game over overlay
        if (gameState.isGameOver) {
            GameOverOverlay(
                score = gameState.score,
                bestScore = gameState.bestScore,
                onRestart = { viewModel.restartGame() }
            )
        }
    }
}

/**
 * Convert absolute screen position to axial coordinate on the board.
 */
private fun screenPositionToAxial(
    screenPos: Offset,
    boardPosition: Offset,
    boardWidth: Float,
    boardHeight: Float
): com.almostbrilliantideas.hexaddict.model.AxialCoord {
    // Convert screen position to board-relative position
    val boardRelativePos = Offset(
        screenPos.x - boardPosition.x,
        screenPos.y - boardPosition.y
    )

    // Calculate hex size and board offset (must match HexBoard calculations)
    val hexSize = calculateHexSize(boardWidth, boardHeight)
    val boardOffset = calculateBoardOffset(boardWidth, boardHeight, hexSize)

    // Adjust for board centering offset
    val adjustedPos = Offset(
        boardRelativePos.x - boardOffset.x,
        boardRelativePos.y - boardOffset.y
    )

    return HexUtils.pixelToAxial(adjustedPos, hexSize)
}

@Composable
private fun ScoreDisplay(
    score: Int,
    bestScore: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "SCORE",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFB8B4C4)
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "BEST",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFB8B4C4)
            )
            Text(
                text = bestScore.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B84D4)
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    bestScore: Int,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF252540),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME OVER",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                if (score >= bestScore && score > 0) {
                    Text(
                        text = "NEW BEST!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF8B84D4),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onRestart) {
                    Text("Play Again")
                }
            }
        }
    }
}

// Helper functions - must match HexBoard calculations exactly
private fun calculateHexSize(canvasWidth: Float, canvasHeight: Float): Float {
    val sqrt3 = sqrt(3.0).toFloat()
    val maxHexWidth = canvasWidth / (sqrt3 * 9.5f)
    val maxHexHeight = canvasHeight / (1.5f * 8f + 2f)
    return minOf(maxHexWidth, maxHexHeight) * 0.95f
}

private fun calculateBoardOffset(canvasWidth: Float, canvasHeight: Float, hexSize: Float): Offset {
    val sqrt3 = sqrt(3.0).toFloat()
    val boardWidth = hexSize * sqrt3 * 9f
    val boardHeight = hexSize * 1.5f * 8f + hexSize * 2f
    val offsetX = (canvasWidth - boardWidth) / 2f + hexSize * sqrt3 * 0.5f
    val offsetY = (canvasHeight - boardHeight) / 2f + hexSize
    return Offset(offsetX, offsetY)
}
