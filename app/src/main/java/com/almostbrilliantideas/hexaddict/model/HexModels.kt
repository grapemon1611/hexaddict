package com.almostbrilliantideas.hexaddict.model

import androidx.compose.ui.graphics.Color

/**
 * Axial coordinate for hexagonal grid.
 * Uses the axial coordinate system where:
 * - q is the column-like axis
 * - r is the row-like axis
 */
data class AxialCoord(val q: Int, val r: Int) {
    operator fun plus(other: AxialCoord) = AxialCoord(q + other.q, r + other.r)
    operator fun minus(other: AxialCoord) = AxialCoord(q - other.q, r - other.r)

    companion object {
        val ZERO = AxialCoord(0, 0)

        // Six neighbor directions in axial coordinates
        val DIRECTIONS = listOf(
            AxialCoord(1, 0),   // East
            AxialCoord(-1, 0),  // West
            AxialCoord(0, 1),   // Southeast
            AxialCoord(0, -1),  // Northwest
            AxialCoord(1, -1),  // Northeast
            AxialCoord(-1, 1)   // Southwest
        )
    }

    fun neighbors(): List<AxialCoord> = DIRECTIONS.map { this + it }
}

/**
 * A cell on the hex board.
 */
data class HexCell(
    val coord: AxialCoord,
    val row: Int,
    val col: Int,
    val color: Color? = null  // null means empty
) {
    val isEmpty: Boolean get() = color == null
    val isOccupied: Boolean get() = color != null
}

/**
 * A piece definition with relative axial coordinates.
 * All coordinates are relative to (0,0).
 */
data class HexPiece(
    val id: String,
    val cells: List<AxialCoord>,
    val color: Color = Color.Gray
) {
    val size: Int get() = cells.size

    /**
     * Get the cells this piece would occupy if placed at the given board position.
     */
    fun cellsAt(position: AxialCoord): List<AxialCoord> {
        return cells.map { it + position }
    }
}

/**
 * Represents the current game state.
 */
data class GameState(
    val board: Map<AxialCoord, HexCell>,
    val piecesTray: List<HexPiece?>,  // 3 slots, null = used
    val score: Int = 0,
    val bestScore: Int = 0,
    val isGameOver: Boolean = false,
    val clearingCells: Set<AxialCoord> = emptySet(),  // Cells currently animating clear
    val lastClearInfo: ClearInfo? = null  // Info about the last clear for display
) {
    companion object {
        const val TRAY_SIZE = 3
    }
}

/**
 * Information about a line clear event.
 */
data class ClearInfo(
    val linesCleared: Int,
    val cellsCleared: Int,
    val hadSameColorLine: Boolean,
    val multipleSameColorLines: Boolean,
    val scoreGained: Int,
    val multiplier: Int
)

/**
 * Result of attempting to place a piece.
 */
sealed class PlacementResult {
    data class Success(
        val placedCells: List<AxialCoord>,
        val clearedLines: List<List<AxialCoord>>,
        val scoreGained: Int
    ) : PlacementResult()

    data object InvalidPosition : PlacementResult()
    data object OutOfBounds : PlacementResult()
    data object Occupied : PlacementResult()
}
