package com.almostbrilliantideas.hexaddict.game

import androidx.compose.ui.geometry.Offset
import com.almostbrilliantideas.hexaddict.model.AxialCoord
import com.almostbrilliantideas.hexaddict.model.HexCell
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Hex grid coordinate utilities.
 * Uses the verified coordinate system from the HTML test file.
 */
object HexUtils {

    const val BOARD_ROWS = 9
    val CELLS_PER_ROW = listOf(9, 8, 9, 8, 9, 8, 9, 8, 9)  // Alternating 9/8
    const val TOTAL_CELLS = 77

    private val SQRT3 = sqrt(3.0).toFloat()

    /**
     * Convert axial coordinates to pixel coordinates.
     * Verified formula: x = size * sqrt(3) * (q + r/2), y = size * 1.5 * r
     */
    fun axialToPixel(coord: AxialCoord, size: Float): Offset {
        val x = size * SQRT3 * (coord.q + coord.r / 2.0f)
        val y = size * 1.5f * coord.r
        return Offset(x, y)
    }

    /**
     * Convert board cell (row, col) to axial coordinates.
     * Verified formula: q = col - floor(row/2), r = row
     */
    fun boardCellToAxial(row: Int, col: Int): AxialCoord {
        val q = col - floor(row / 2.0).toInt()
        val r = row
        return AxialCoord(q, r)
    }

    /**
     * Convert pixel coordinates to the nearest axial coordinate.
     * Used for hit detection when tapping/dragging.
     */
    fun pixelToAxial(pixel: Offset, size: Float): AxialCoord {
        // Reverse the axialToPixel formula
        val q = (pixel.x * SQRT3 / 3.0f - pixel.y / 3.0f) / size
        val r = pixel.y * 2.0f / 3.0f / size

        // Round to nearest hex using cube coordinates
        return axialRound(q, r)
    }

    /**
     * Round fractional axial coordinates to the nearest hex.
     * Uses cube coordinate rounding for accuracy.
     */
    private fun axialRound(q: Float, r: Float): AxialCoord {
        // Convert to cube coordinates
        val x = q
        val z = r
        val y = -x - z

        // Round cube coordinates
        var rx = kotlin.math.round(x)
        var ry = kotlin.math.round(y)
        var rz = kotlin.math.round(z)

        // Fix rounding errors by resetting the component with largest diff
        val xDiff = kotlin.math.abs(rx - x)
        val yDiff = kotlin.math.abs(ry - y)
        val zDiff = kotlin.math.abs(rz - z)

        when {
            xDiff > yDiff && xDiff > zDiff -> rx = -ry - rz
            yDiff > zDiff -> ry = -rx - rz
            else -> rz = -rx - ry
        }

        return AxialCoord(rx.toInt(), rz.toInt())
    }

    /**
     * Generate all 77 board cells with their axial coordinates.
     */
    fun generateBoard(): Map<AxialCoord, HexCell> {
        val cells = mutableMapOf<AxialCoord, HexCell>()

        for (row in 0 until BOARD_ROWS) {
            val cellCount = CELLS_PER_ROW[row]
            for (col in 0 until cellCount) {
                val coord = boardCellToAxial(row, col)
                cells[coord] = HexCell(coord = coord, row = row, col = col)
            }
        }

        return cells
    }

    /**
     * Get all valid board coordinates.
     */
    fun getAllBoardCoords(): Set<AxialCoord> {
        return generateBoard().keys
    }

    /**
     * Check if an axial coordinate is within the board bounds.
     */
    fun isOnBoard(coord: AxialCoord, boardCoords: Set<AxialCoord>): Boolean {
        return coord in boardCoords
    }

    /**
     * Calculate the bounding box of the board in pixel coordinates.
     */
    fun getBoardBounds(size: Float): Pair<Offset, Offset> {
        val allCoords = getAllBoardCoords()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (coord in allCoords) {
            val pixel = axialToPixel(coord, size)
            minX = minOf(minX, pixel.x)
            minY = minOf(minY, pixel.y)
            maxX = maxOf(maxX, pixel.x)
            maxY = maxOf(maxY, pixel.y)
        }

        // Add hex radius to account for cell size
        val hexWidth = size * SQRT3
        val hexHeight = size * 2

        return Pair(
            Offset(minX - hexWidth / 2, minY - hexHeight / 2),
            Offset(maxX + hexWidth / 2, maxY + hexHeight / 2)
        )
    }
}
