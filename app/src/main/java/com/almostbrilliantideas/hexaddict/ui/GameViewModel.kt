package com.almostbrilliantideas.hexaddict.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almostbrilliantideas.hexaddict.game.ColorPalette
import com.almostbrilliantideas.hexaddict.game.HexUtils
import com.almostbrilliantideas.hexaddict.game.LineDefinitions
import com.almostbrilliantideas.hexaddict.game.PieceLibrary
import com.almostbrilliantideas.hexaddict.model.AxialCoord
import com.almostbrilliantideas.hexaddict.model.ClearInfo
import com.almostbrilliantideas.hexaddict.model.GameState
import com.almostbrilliantideas.hexaddict.model.HexCell
import com.almostbrilliantideas.hexaddict.model.HexPiece
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _previewCells = MutableStateFlow<Map<AxialCoord, Color>>(emptyMap())
    val previewCells: StateFlow<Map<AxialCoord, Color>> = _previewCells.asStateFlow()

    private val _isValidPlacement = MutableStateFlow(true)
    val isValidPlacement: StateFlow<Boolean> = _isValidPlacement.asStateFlow()

    private val boardCoords = HexUtils.getAllBoardCoords()

    companion object {
        private const val POINTS_PER_CELL_PLACED = 1
        private const val POINTS_PER_CELL_CLEARED = 10
        private const val CLEAR_ANIMATION_DURATION_MS = 300L
    }

    private fun createInitialState(): GameState {
        val board = HexUtils.generateBoard()
        val pieces = generateNewPieces(0)
        return GameState(
            board = board,
            piecesTray = pieces,
            score = 0,
            bestScore = 0,
            isGameOver = false
        )
    }

    /**
     * Generate 3 new random pieces with colors and shapes based on current score.
     * Piece complexity increases gradually as score rises.
     * Uses PieceLibrary.generateTray which ensures no all-compact trays.
     */
    private fun generateNewPieces(score: Int): List<HexPiece> {
        return PieceLibrary.generateTray(score, GameState.TRAY_SIZE).map { piece ->
            val color = ColorPalette.randomColor(score)
            piece.copy(color = color.primary)
        }
    }

    /**
     * Update preview when dragging a piece over the board.
     */
    fun updatePreview(piece: HexPiece, targetCoord: AxialCoord) {
        val cellsToPlace = piece.cellsAt(targetCoord)
        val currentState = _gameState.value
        val currentBoard = currentState.board

        // Check if all cells are valid - must be on board, empty, and not clearing
        val isValid = cellsToPlace.all { coord ->
            coord in boardCoords &&
            currentBoard[coord]?.isEmpty == true &&
            coord !in currentState.clearingCells
        }

        _isValidPlacement.value = isValid

        if (cellsToPlace.any { it in boardCoords }) {
            _previewCells.value = cellsToPlace
                .filter { it in boardCoords }
                .associateWith { piece.color }
        } else {
            _previewCells.value = emptyMap()
        }
    }

    /**
     * Clear the preview when drag ends or is cancelled.
     */
    fun clearPreview() {
        _previewCells.value = emptyMap()
        _isValidPlacement.value = true
    }

    /**
     * Attempt to place a piece at the given position.
     * Returns true if placement was successful.
     */
    fun placePiece(pieceIndex: Int, piece: HexPiece, targetCoord: AxialCoord): Boolean {
        val cellsToPlace = piece.cellsAt(targetCoord)
        val currentState = _gameState.value
        val currentBoard = currentState.board

        // Validate placement - cell must be on board, empty, and not currently clearing
        val isValid = cellsToPlace.all { coord ->
            coord in boardCoords &&
            currentBoard[coord]?.isEmpty == true &&
            coord !in currentState.clearingCells
        }

        if (!isValid) {
            clearPreview()
            return false
        }

        // Place the piece
        val newBoard = currentBoard.toMutableMap()
        for (coord in cellsToPlace) {
            val existingCell = newBoard[coord]!!
            newBoard[coord] = existingCell.copy(color = piece.color)
        }

        // Calculate placement score
        val placementScore = cellsToPlace.size * POINTS_PER_CELL_PLACED

        // Check for completed lines
        val completedLines = findCompletedLines(newBoard, cellsToPlace)

        // Remove piece from tray
        val newTray = currentState.piecesTray.toMutableList()
        newTray[pieceIndex] = null

        if (completedLines.isNotEmpty()) {
            // Handle line clearing with animation
            handleLineClear(
                board = newBoard,
                completedLines = completedLines,
                placementScore = placementScore,
                currentScore = currentState.score,
                currentBest = currentState.bestScore,
                tray = newTray
            )
        } else {
            // No lines cleared - just update state
            val newScore = currentState.score + placementScore

            // Check if tray is empty - refill it
            val finalTray = if (newTray.all { it == null }) {
                generateNewPieces(newScore)
            } else {
                newTray
            }

            // Check for game over
            val isGameOver = checkGameOver(newBoard, finalTray)

            _gameState.update { state ->
                state.copy(
                    board = newBoard,
                    piecesTray = finalTray,
                    score = newScore,
                    bestScore = maxOf(state.bestScore, newScore),
                    isGameOver = isGameOver,
                    lastClearInfo = null
                )
            }
        }

        clearPreview()
        return true
    }

    /**
     * Find all completed lines after placing cells.
     */
    private fun findCompletedLines(
        board: Map<AxialCoord, HexCell>,
        placedCells: List<AxialCoord>
    ): List<LineDefinitions.Line> {
        // Get all lines that might be affected by the placed cells
        val linesToCheck = LineDefinitions.getLinesContainingCells(placedCells)

        // Find which lines are complete (all cells occupied)
        return linesToCheck.filter { line ->
            line.cells.all { coord ->
                board[coord]?.isOccupied == true
            }
        }
    }

    /**
     * Handle line clearing with animation and scoring.
     */
    private fun handleLineClear(
        board: MutableMap<AxialCoord, HexCell>,
        completedLines: List<LineDefinitions.Line>,
        placementScore: Int,
        currentScore: Int,
        currentBest: Int,
        tray: MutableList<HexPiece?>
    ) {
        // Collect all cells to clear (may overlap between lines)
        val cellsToClear = completedLines.flatMap { it.cells }.toSet()

        // Check for same-color lines
        val sameColorLines = completedLines.filter { line ->
            val colors = line.cells.mapNotNull { board[it]?.color }.toSet()
            colors.size == 1  // All cells same color
        }

        val hasSameColorLine = sameColorLines.isNotEmpty()
        val hasMultipleSameColorLines = sameColorLines.size >= 2

        // Calculate multiplier
        val multiplier = when {
            hasMultipleSameColorLines -> 3
            hasSameColorLine -> 2
            else -> 1
        }

        // Calculate clear score
        val clearScore = cellsToClear.size * POINTS_PER_CELL_CLEARED
        val totalMoveScore = (placementScore + clearScore) * multiplier
        val newScore = currentScore + totalMoveScore

        // Create clear info for display
        val clearInfo = ClearInfo(
            linesCleared = completedLines.size,
            cellsCleared = cellsToClear.size,
            hadSameColorLine = hasSameColorLine,
            multipleSameColorLines = hasMultipleSameColorLines,
            scoreGained = totalMoveScore,
            multiplier = multiplier
        )

        // Start clearing animation
        _gameState.update { state ->
            state.copy(
                board = board.toMap(),
                piecesTray = tray,
                clearingCells = cellsToClear,
                lastClearInfo = clearInfo,
                score = newScore,
                bestScore = maxOf(currentBest, newScore)
            )
        }

        // After animation, remove cleared cells
        viewModelScope.launch {
            delay(CLEAR_ANIMATION_DURATION_MS)
            completeClear(cellsToClear, newScore, tray)
        }
    }

    /**
     * Complete the clear by removing cells and checking game state.
     */
    private fun completeClear(
        cellsToClear: Set<AxialCoord>,
        newScore: Int,
        tray: MutableList<HexPiece?>
    ) {
        _gameState.update { state ->
            val clearedBoard = state.board.toMutableMap()
            for (coord in cellsToClear) {
                val cell = clearedBoard[coord]
                if (cell != null) {
                    clearedBoard[coord] = cell.copy(color = null)
                }
            }

            // Check if tray is empty - refill it
            val finalTray = if (tray.all { it == null }) {
                generateNewPieces(newScore)
            } else {
                tray.toList()
            }

            // Check for game over
            val isGameOver = checkGameOver(clearedBoard, finalTray)

            state.copy(
                board = clearedBoard,
                piecesTray = finalTray,
                clearingCells = emptySet(),
                isGameOver = isGameOver
            )
        }
    }

    /**
     * Check if any remaining piece can be placed anywhere on the board.
     */
    private fun checkGameOver(board: Map<AxialCoord, HexCell>, tray: List<HexPiece?>): Boolean {
        val emptyCoords = board.filter { it.value.isEmpty }.keys

        for (piece in tray.filterNotNull()) {
            for (coord in emptyCoords) {
                val cellsNeeded = piece.cellsAt(coord)
                val canPlace = cellsNeeded.all { c ->
                    c in boardCoords && board[c]?.isEmpty == true
                }
                if (canPlace) {
                    return false  // Found a valid placement
                }
            }
        }

        return true  // No valid placements found
    }

    /**
     * Restart the game.
     */
    fun restartGame() {
        val currentBest = _gameState.value.bestScore
        _gameState.value = createInitialState().copy(bestScore = currentBest)
        clearPreview()
    }

    /**
     * Handle a tap on a board cell (for debugging/testing).
     */
    fun onCellTap(coord: AxialCoord) {
        // For now, just log the tap - useful for verifying coordinates
        println("Tapped cell: $coord")
    }
}
