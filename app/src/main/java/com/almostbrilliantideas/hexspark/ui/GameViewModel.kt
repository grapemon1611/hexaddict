package com.almostbrilliantideas.hexspark.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almostbrilliantideas.hexspark.audio.SettingsManager
import com.almostbrilliantideas.hexspark.game.ColorPalette
import com.almostbrilliantideas.hexspark.game.HexUtils
import com.almostbrilliantideas.hexspark.game.LineDefinitions
import com.almostbrilliantideas.hexspark.game.PieceLibrary
import com.almostbrilliantideas.hexspark.model.AxialCoord
import com.almostbrilliantideas.hexspark.model.ClearInfo
import com.almostbrilliantideas.hexspark.model.GameState
import com.almostbrilliantideas.hexspark.model.HexCell
import com.almostbrilliantideas.hexspark.model.HexPiece
import com.almostbrilliantideas.hexspark.ui.effects.SparkEffectState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class for ambient line highlighting.
 * Cells that are part of same-color partial lines get a subtle glow.
 */
data class LineHighlight(
    val color: Color,
    val intensity: Float  // 0.0 to 1.0, scales with match count
)

/**
 * Data class for score preview during drag.
 */
data class ScorePreview(
    val points: Int,
    val multiplier: Int,  // 1, 2, or 3
    val isPayday: Boolean
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _previewCells = MutableStateFlow<Map<AxialCoord, Color>>(emptyMap())
    val previewCells: StateFlow<Map<AxialCoord, Color>> = _previewCells.asStateFlow()

    private val _isValidPlacement = MutableStateFlow(true)
    val isValidPlacement: StateFlow<Boolean> = _isValidPlacement.asStateFlow()

    private val _lineHighlights = MutableStateFlow<Map<AxialCoord, LineHighlight>>(emptyMap())
    val lineHighlights: StateFlow<Map<AxialCoord, LineHighlight>> = _lineHighlights.asStateFlow()

    private val _scorePreview = MutableStateFlow<ScorePreview?>(null)
    val scorePreview: StateFlow<ScorePreview?> = _scorePreview.asStateFlow()

    private val boardCoords = HexUtils.getAllBoardCoords()

    companion object {
        private const val POINTS_PER_CELL_PLACED = 1
        private const val POINTS_PER_CELL_CLEARED = 10
        private const val POINTS_PER_JACKPOT_CELL = 5
        // Animation duration is now calculated dynamically based on clear type
        // See SparkEffectState.animationDurationFromClearInfo()
    }

    /**
     * Calculate line highlights for cells that are part of same-color partial lines.
     * Scans all lines and highlights cells where 2+ cells share the same color.
     */
    private fun calculateLineHighlights(board: Map<AxialCoord, HexCell>): Map<AxialCoord, LineHighlight> {
        val highlights = mutableMapOf<AxialCoord, LineHighlight>()

        for (line in LineDefinitions.allLines) {
            // Group occupied cells by color
            val occupiedByColor = line.cells
                .mapNotNull { coord -> board[coord]?.color?.let { coord to it } }
                .groupBy { it.second }

            // Find dominant color with 2+ cells
            val entry = occupiedByColor
                .maxByOrNull { it.value.size }
                ?.takeIf { it.value.size >= 2 }
                ?: continue

            val dominantColor = entry.key
            val cells = entry.value

            // Calculate intensity: (matchCount - 1) / (lineSize - 1)
            // 2 matches = low intensity, all matches = max intensity
            val intensity = (cells.size - 1).toFloat() / (line.size - 1).toFloat()

            for ((coord, _) in cells) {
                // Take stronger highlight if cell already has one
                val existing = highlights[coord]
                if (existing == null || existing.intensity < intensity) {
                    highlights[coord] = LineHighlight(dominantColor, intensity)
                }
            }
        }
        return highlights
    }

    /**
     * Calculate score preview for a potential piece placement.
     */
    private fun calculateScorePreview(
        piece: HexPiece,
        targetCoord: AxialCoord,
        board: Map<AxialCoord, HexCell>
    ): ScorePreview? {
        val cellsToPlace = piece.cellsAt(targetCoord)

        // Verify all cells are valid for placement
        val isValid = cellsToPlace.all { coord ->
            coord in boardCoords && board[coord]?.isEmpty == true
        }
        if (!isValid) return null

        // Simulate placement
        val simulatedBoard = board.toMutableMap()
        for (coord in cellsToPlace) {
            val cell = simulatedBoard[coord] ?: return null
            simulatedBoard[coord] = cell.copy(color = piece.color)
        }

        // Check for completed lines
        val completedLines = findCompletedLines(simulatedBoard, cellsToPlace)

        if (completedLines.isEmpty()) {
            // Just placement points, no lines
            return ScorePreview(
                points = cellsToPlace.size * POINTS_PER_CELL_PLACED,
                multiplier = 1,
                isPayday = false
            )
        }

        // Count same-color lines
        val sameColorLines = completedLines.count { line ->
            val colors = line.cells.mapNotNull { simulatedBoard[it]?.color }.toSet()
            colors.size == 1
        }

        val multiplier = when {
            sameColorLines >= 2 -> 3
            sameColorLines == 1 -> 2
            else -> 1
        }

        val cellsToClear = completedLines.flatMap { it.cells }.toSet()
        val clearScore = cellsToClear.size * POINTS_PER_CELL_CLEARED
        val placementScore = cellsToPlace.size * POINTS_PER_CELL_PLACED
        val totalPoints = placementScore + (clearScore * multiplier)

        return ScorePreview(
            points = totalPoints,
            multiplier = multiplier,
            isPayday = sameColorLines >= 2
        )
    }

    private fun createInitialState(): GameState {
        val board = HexUtils.generateBoard()
        val pieces = generateNewPieces(0)
        // Load persisted best score
        val savedBestScore = settingsManager.bestScore
        return GameState(
            board = board,
            piecesTray = pieces,
            score = 0,
            bestScore = savedBestScore,
            isGameOver = false
        )
    }

    /**
     * Update best score in both state and persistent storage.
     */
    private fun updateBestScore(newScore: Int, currentBest: Int): Int {
        val newBest = maxOf(currentBest, newScore)
        if (newBest > currentBest) {
            settingsManager.bestScore = newBest
        }
        return newBest
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

            // Calculate score preview if valid
            if (isValid) {
                _scorePreview.value = calculateScorePreview(piece, targetCoord, currentBoard)
            } else {
                _scorePreview.value = null
            }
        } else {
            _previewCells.value = emptyMap()
            _scorePreview.value = null
        }
    }

    /**
     * Clear the preview when drag ends or is cancelled.
     */
    fun clearPreview() {
        _previewCells.value = emptyMap()
        _isValidPlacement.value = true
        _scorePreview.value = null
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
                    bestScore = updateBestScore(newScore, state.bestScore),
                    isGameOver = isGameOver,
                    lastClearInfo = null
                )
            }

            // Update line highlights for ambient glow
            _lineHighlights.value = calculateLineHighlights(newBoard)
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

        val sameColorLineCount = sameColorLines.size

        // Check for JACKPOT: all 3 axes cleared with at least 1 same-color line
        val axesCleared = completedLines.map { it.axis }.toSet()
        val isJackpot = axesCleared.size == 3 && sameColorLineCount >= 1

        // Calculate multiplier (applies to clear score only, not placement)
        val multiplier = when {
            sameColorLineCount >= 2 -> 3  // PAYDAY
            sameColorLineCount == 1 -> 2
            else -> 1
        }

        val isPayday = sameColorLineCount >= 2 && !isJackpot

        // Calculate scores
        val clearScore = cellsToClear.size * POINTS_PER_CELL_CLEARED
        val multipliedClearScore = clearScore * multiplier

        // For JACKPOT: clear entire board, 5 pts per remaining cell
        val jackpotBonusCells: Int
        val jackpotBonus: Int
        val finalCellsToClear: Set<AxialCoord>

        if (isJackpot) {
            // Find all remaining occupied cells (not already in cellsToClear)
            val remainingOccupied = board.filter { (coord, cell) ->
                cell.isOccupied && coord !in cellsToClear
            }.keys
            jackpotBonusCells = remainingOccupied.size
            jackpotBonus = jackpotBonusCells * POINTS_PER_JACKPOT_CELL
            finalCellsToClear = cellsToClear + remainingOccupied
        } else {
            jackpotBonusCells = 0
            jackpotBonus = 0
            finalCellsToClear = cellsToClear
        }

        val totalMoveScore = placementScore + multipliedClearScore + jackpotBonus
        val newScore = currentScore + totalMoveScore

        // Capture cell colors before clearing for spark effects
        val cellColorsCopy = finalCellsToClear.associateWith { coord ->
            board[coord]?.color ?: Color.Gray
        }

        // Create clear info for display and spark effects
        val clearInfo = ClearInfo(
            linesCleared = completedLines.size,
            cellsCleared = cellsToClear.size,
            sameColorLineCount = sameColorLineCount,
            scoreGained = totalMoveScore,
            multiplier = multiplier,
            isPayday = isPayday,
            isJackpot = isJackpot,
            jackpotBonusCells = jackpotBonusCells,
            completedLines = completedLines,
            sameColorLines = sameColorLines.toSet(),
            cellColors = cellColorsCopy
        )

        // Start clearing animation
        _gameState.update { state ->
            state.copy(
                board = board.toMap(),
                piecesTray = tray,
                clearingCells = finalCellsToClear,
                lastClearInfo = clearInfo,
                score = newScore,
                bestScore = updateBestScore(newScore, currentBest)
            )
        }

        // Update line highlights (will show glow on remaining cells)
        _lineHighlights.value = calculateLineHighlights(board)

        // Calculate animation duration based on clear type (slow motion for multi-line clears)
        val animationDuration = SparkEffectState.animationDurationFromClearInfo(
            linesCleared = completedLines.size,
            isPayday = isPayday,
            isJackpot = isJackpot
        )

        // After animation, remove cleared cells
        viewModelScope.launch {
            delay(animationDuration)
            completeClear(finalCellsToClear, newScore, tray)
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
        val clearedBoard = _gameState.value.board.toMutableMap()
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

        _gameState.update { state ->
            state.copy(
                board = clearedBoard,
                piecesTray = finalTray,
                clearingCells = emptySet(),
                isGameOver = isGameOver
            )
        }

        // Update line highlights for ambient glow
        _lineHighlights.value = calculateLineHighlights(clearedBoard)
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
        _lineHighlights.value = emptyMap()
    }

    /**
     * Handle a tap on a board cell (for debugging/testing).
     */
    fun onCellTap(coord: AxialCoord) {
        // For now, just log the tap - useful for verifying coordinates
        println("Tapped cell: $coord")
    }
}
