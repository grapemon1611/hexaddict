package com.almostbrilliantideas.hexspark.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.almostbrilliantideas.hexspark.ads.AdManager
import com.almostbrilliantideas.hexspark.audio.GameAudioController
import com.almostbrilliantideas.hexspark.game.HexUtils
import com.almostbrilliantideas.hexspark.model.HexPiece
import com.almostbrilliantideas.hexspark.ui.components.BackgroundScene
import com.almostbrilliantideas.hexspark.ui.components.DraggedPiece
import com.almostbrilliantideas.hexspark.ui.components.GameLogo
import com.almostbrilliantideas.hexspark.ui.components.HexBoard
import com.almostbrilliantideas.hexspark.ui.components.PieceTray
import kotlin.math.sqrt

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    adManager: AdManager? = null,
    activity: Activity? = null
) {
    val context = LocalContext.current
    val gameState by viewModel.gameState.collectAsState()
    val dimensions = rememberGameDimensions()
    val previewCells by viewModel.previewCells.collectAsState()
    val isValidPlacement by viewModel.isValidPlacement.collectAsState()
    val lineHighlights by viewModel.lineHighlights.collectAsState()
    val scorePreview by viewModel.scorePreview.collectAsState()

    // Audio controller for sound effects and haptics (nullable to handle init failures gracefully)
    val audioController = remember {
        try {
            GameAudioController(context)
        } catch (e: Exception) {
            null
        }
    }

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

    // Track previous game over state to detect transitions
    var wasGameOver by remember { mutableStateOf(false) }

    // Track whether the game over overlay should be shown (after ad completes or immediately if no ad)
    var showGameOverOverlay by remember { mutableStateOf(false) }

    // Settings screen state
    var showSettings by remember { mutableStateOf(false) }

    // Tutorial state - show on first launch or if enabled in settings
    var showTutorial by remember {
        mutableStateOf(audioController?.settingsManager?.shouldShowTutorial() ?: false)
    }

    // Trigger bonus text and audio when clearInfo changes
    LaunchedEffect(gameState.lastClearInfo) {
        val clearInfo = gameState.lastClearInfo
        if (clearInfo != null) {
            // Play audio feedback for the clear
            audioController?.onLineClear(clearInfo)

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

    // Trigger game over audio and ad when game ends
    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver && !wasGameOver) {
            audioController?.onGameOver()

            // Show ad if available, then show game over overlay
            if (adManager != null && activity != null) {
                adManager.showAdIfReady(activity) {
                    showGameOverOverlay = true
                }
            } else {
                // No ad manager, show game over immediately
                showGameOverOverlay = true
            }
        } else if (!gameState.isGameOver && wasGameOver) {
            // Game restarted, hide overlay
            showGameOverOverlay = false
        }
        wasGameOver = gameState.isGameOver
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
                .padding(horizontal = dimensions.screenPaddingHorizontal)
                .padding(top = dimensions.screenPaddingTop, bottom = dimensions.screenPaddingBottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo above score bar
            GameLogo(dimensions = dimensions)

            // Score display
            ScoreDisplay(
                score = gameState.score,
                bestScore = gameState.bestScore,
                dimensions = dimensions
            )

            Spacer(modifier = Modifier.height(dimensions.spacerAfterScoreBar))

            // Hex board - centered in available space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HexBoard(
                    cells = gameState.board,
                    lineHighlights = lineHighlights,
                    previewCells = previewCells,
                    clearingCells = gameState.clearingCells,
                    clearInfo = gameState.lastClearInfo,
                    invalidPreview = !isValidPlacement,
                    onCellTap = { coord ->
                        viewModel.onCellTap(coord)
                    },
                    dimensions = dimensions,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        boardPositionInRoot = coordinates.positionInRoot()
                        boardSize = coordinates.size
                    }
                )
            }

            Spacer(modifier = Modifier.height(dimensions.spacerAfterBoard))

            // Piece tray
            PieceTray(
                pieces = gameState.piecesTray,
                dimensions = dimensions,
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
                        val placed = viewModel.placePiece(draggedPieceIndex, piece, targetCoord)
                        if (placed) {
                            audioController?.onPiecePlaced()
                        }
                    }

                    draggedPiece = null
                    draggedPieceIndex = -1
                    viewModel.clearPreview()
                },
                modifier = Modifier.height(dimensions.pieceTrayHeight)
            )
        }

        // Dragged piece overlay - rendered at absolute screen position
        if (draggedPiece != null) {
            DraggedPiece(
                piece = draggedPiece!!,
                position = dragPosition,
                dimensions = dimensions
            )
        }

        // Game over overlay (shown after ad completes or immediately if no ad)
        if (showGameOverOverlay && !showSettings) {
            GameOverOverlay(
                score = gameState.score,
                bestScore = gameState.bestScore,
                dimensions = dimensions,
                onRestart = {
                    showGameOverOverlay = false
                    viewModel.restartGame()
                },
                onSettings = { showSettings = true }
            )
        }

        // Bonus text overlay (PAYDAY / JACKPOT)
        BonusTextOverlay(
            visible = showBonusText,
            text = bonusText,
            color = bonusColor,
            dimensions = dimensions
        )

        // Score preview during drag
        ScorePreviewOverlay(
            scorePreview = scorePreview,
            visible = draggedPiece != null && isValidPlacement,
            dimensions = dimensions
        )

        // Settings screen overlay
        if (showSettings) {
            audioController?.settingsManager?.let { settingsManager ->
                SettingsScreen(
                    settingsManager = settingsManager,
                    onClose = { showSettings = false }
                )
            }
        }

        // Gear icon for settings (top right, unobtrusive)
        if (!gameState.isGameOver && !showGameOverOverlay && !showSettings && !showTutorial) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = dimensions.screenPaddingTop, end = dimensions.screenPaddingHorizontal),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensions.settingsIconSize)
                        .clip(CircleShape)
                        .background(Color(0xFF252540).copy(alpha = 0.7f))
                        .clickable { showSettings = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2699", // Gear symbol
                        fontSize = dimensions.settingsIconFontSize,
                        color = Color(0xFFB8B4C4)
                    )
                }
            }
        }

        // Tutorial overlay (shows on first launch or if enabled in settings)
        if (showTutorial) {
            TutorialOverlay(
                onDismiss = {
                    showTutorial = false
                    // Mark tutorial as seen so it won't show again unless enabled in settings
                    audioController?.settingsManager?.tutorialSeen = true
                }
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
): com.almostbrilliantideas.hexspark.model.AxialCoord {
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
    bestScore: Int,
    dimensions: GameDimensions
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF252540).copy(alpha = 0.85f),
                shape = RoundedCornerShape(dimensions.scoreBarCornerRadius)
            )
            .padding(
                horizontal = dimensions.scoreBarPaddingHorizontal,
                vertical = dimensions.scoreBarPaddingVertical
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "SCORE",
                fontSize = dimensions.scoreLabelFontSize,
                color = Color(0xFFB8B4C4)
            )
            Text(
                text = score.toString(),
                fontSize = dimensions.scoreValueFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "BEST",
                fontSize = dimensions.scoreLabelFontSize,
                color = Color(0xFFB8B4C4)
            )
            Text(
                text = bestScore.toString(),
                fontSize = dimensions.scoreValueFontSize,
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
    dimensions: GameDimensions,
    onRestart: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF252540),
            shape = RoundedCornerShape(dimensions.boardCornerRadius)
        ) {
            Column(
                modifier = Modifier.padding(dimensions.overlayPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME OVER",
                    fontSize = dimensions.overlayTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(dimensions.spacerAfterBoard))

                Text(
                    text = "Score: $score",
                    fontSize = dimensions.overlaySubtitleFontSize,
                    color = Color.White
                )

                if (score >= bestScore && score > 0) {
                    Text(
                        text = "NEW BEST!",
                        fontSize = (dimensions.overlaySubtitleFontSize.value * 0.8f).sp,
                        color = Color(0xFF8B84D4),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(dimensions.spacerAfterBoard))

                // Play Again button
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B84D4)
                    )
                ) {
                    Text(
                        text = "Play Again",
                        fontSize = dimensions.buttonFontSize
                    )
                }

                Spacer(modifier = Modifier.height(dimensions.spacerAfterScoreBar))

                // Settings button
                Button(
                    onClick = onSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3A3A50)
                    )
                ) {
                    Text(
                        text = "\u2699",
                        fontSize = dimensions.buttonFontSize
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacerAfterScoreBar))
                    Text(
                        text = "Settings",
                        fontSize = dimensions.buttonFontSize
                    )
                }
            }
        }
    }
}

@Composable
private fun BonusTextOverlay(
    visible: Boolean,
    text: String,
    color: Color,
    dimensions: GameDimensions
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
                fontSize = dimensions.bonusTextFontSize,
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
    visible: Boolean,
    dimensions: GameDimensions
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
                    .padding(top = dimensions.pieceTrayHeight * 0.67f),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(dimensions.pieceSlotCornerRadius / 2)
                ) {
                    Text(
                        text = when {
                            preview.isPayday -> "PAYDAY ${preview.points} pts 3x"
                            preview.multiplier == 2 -> "${preview.points} pts 2x"
                            else -> "${preview.points} pts"
                        },
                        modifier = Modifier.padding(
                            horizontal = dimensions.scorePreviewPadding * 2,
                            vertical = dimensions.scorePreviewPadding
                        ),
                        fontSize = dimensions.scorePreviewFontSize,
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
