package com.almostbrilliantideas.hexaddict.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.almostbrilliantideas.hexaddict.game.HexUtils
import com.almostbrilliantideas.hexaddict.model.HexPiece
import com.almostbrilliantideas.hexaddict.ui.components.BackgroundScene
import com.almostbrilliantideas.hexaddict.ui.components.DraggedPiece
import com.almostbrilliantideas.hexaddict.ui.components.GameLogo
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
    val lineHighlights by viewModel.lineHighlights.collectAsState()
    val scorePreview by viewModel.scorePreview.collectAsState()

    // Drag state - positions are in absolute screen coordinates
    var draggedPiece by remember { mutableStateOf<HexPiece?>(null) }
    var draggedPieceIndex by remember { mutableStateOf(-1) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }

    // Board position and size for coordinate conversion
    var boardPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }

    // Bonus text overlay state
    var showBonusText by remember { mutableStateOf(false) }
    var bonusText by remember { mutableStateOf("") }
    var bonusColor by remember { mutableStateOf(Color.White) }

    // Trigger bonus text when clearInfo changes
    LaunchedEffect(gameState.lastClearInfo) {
        val clearInfo = gameState.lastClearInfo
        if (clearInfo != null) {
            when {
                clearInfo.isJackpot -> {
                    bonusText = "JACKPOT!"
                    bonusColor = Color(0xFFFFD700)  // Gold
                    showBonusText = true
                    delay(1500)
                    showBonusText = false
                }
                clearInfo.isPayday -> {
                    bonusText = "PAYDAY!"
                    bonusColor = Color(0xFF4DB893)  // Teal
                    showBonusText = true
                    delay(1200)
                    showBonusText = false
                }
                clearInfo.sameColorLineCount == 1 -> {
                    bonusText = "2X"
                    bonusColor = Color(0xFF8B84D4)  // Purple
                    showBonusText = true
                    delay(800)
                    showBonusText = false
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Atmospheric background scene - renders behind everything
        // Sky zone: top ~10% (above score bar)
        // Ground zone: ~78-90% (between board and piece tray)
        BackgroundScene(
            modifier = Modifier.fillMaxSize(),
            skyZoneEndFraction = 0.10f,
            groundZoneStartFraction = 0.78f,
            groundZoneEndFraction = 0.90f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo above score bar
            GameLogo()

            // Score display
            ScoreDisplay(
                score = gameState.score,
                bestScore = gameState.bestScore
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    lineHighlights = lineHighlights,
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

        // Bonus text overlay (PAYDAY / JACKPOT)
        BonusTextOverlay(
            visible = showBonusText,
            text = bonusText,
            color = bonusColor
        )

        // Score preview during drag
        ScorePreviewOverlay(
            scorePreview = scorePreview,
            visible = draggedPiece != null && isValidPlacement
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF252540).copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
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

@Composable
private fun BonusTextOverlay(
    visible: Boolean,
    text: String,
    color: Color
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(200)
        ) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(
            targetScale = 1.2f,
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScorePreviewOverlay(
    scorePreview: ScorePreview?,
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible && scorePreview != null,
        enter = fadeIn(animationSpec = tween(100)),
        exit = fadeOut(animationSpec = tween(100))
    ) {
        scorePreview?.let { preview ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when {
                            preview.isPayday -> "PAYDAY ${preview.points} pts 3x"
                            preview.multiplier == 2 -> "${preview.points} pts 2x"
                            else -> "${preview.points} pts"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            preview.isPayday -> Color(0xFF4DB893)  // Teal
                            preview.multiplier == 2 -> Color(0xFF8B84D4)  // Purple
                            else -> Color.White
                        }
                    )
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
