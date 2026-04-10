package com.almostbrilliantideas.hexaddict.game

import com.almostbrilliantideas.hexaddict.model.AxialCoord

/**
 * Precomputed line definitions for all three axes.
 *
 * Line axes:
 * - Horizontal: cells where r is fixed (9 lines)
 * - Diagonal-R: cells where q is fixed (11 lines)
 * - Diagonal-L: cells where q+r is fixed (11 lines)
 *
 * Total: 31 lines
 * 73 of 77 cells sit on all three axes.
 */
object LineDefinitions {

    /**
     * Minimum number of cells for a line to be clearable.
     * Lines shorter than this are ignored (prevents corner cells from
     * being cleared when tiny 1-2 cell "lines" are filled).
     */
    private const val MIN_LINE_SIZE = 5

    enum class LineAxis {
        HORIZONTAL,  // r fixed
        DIAGONAL_R,  // q fixed
        DIAGONAL_L   // q+r fixed (s in cube coordinates)
    }

    data class Line(
        val axis: LineAxis,
        val index: Int,  // The fixed value (r, q, or q+r)
        val cells: List<AxialCoord>
    ) {
        val size: Int get() = cells.size
    }

    /**
     * All lines on the board, precomputed from board geometry.
     */
    val allLines: List<Line> by lazy { computeAllLines() }

    /**
     * Lines grouped by axis.
     */
    val horizontalLines: List<Line> by lazy { allLines.filter { it.axis == LineAxis.HORIZONTAL } }
    val diagonalRLines: List<Line> by lazy { allLines.filter { it.axis == LineAxis.DIAGONAL_R } }
    val diagonalLLines: List<Line> by lazy { allLines.filter { it.axis == LineAxis.DIAGONAL_L } }

    /**
     * Map from cell coordinate to all lines containing that cell.
     * Used for efficient lookup after piece placement.
     */
    val cellToLines: Map<AxialCoord, List<Line>> by lazy { computeCellToLines() }

    private fun computeAllLines(): List<Line> {
        val boardCells = HexUtils.getAllBoardCoords()
        val lines = mutableListOf<Line>()

        // Horizontal lines (r fixed)
        val byR = boardCells.groupBy { it.r }
        for ((r, cells) in byR) {
            if (cells.size >= MIN_LINE_SIZE) {
                lines.add(Line(
                    axis = LineAxis.HORIZONTAL,
                    index = r,
                    cells = cells.sortedBy { it.q }
                ))
            }
        }

        // Diagonal-R lines (q fixed)
        val byQ = boardCells.groupBy { it.q }
        for ((q, cells) in byQ) {
            if (cells.size >= MIN_LINE_SIZE) {
                lines.add(Line(
                    axis = LineAxis.DIAGONAL_R,
                    index = q,
                    cells = cells.sortedBy { it.r }
                ))
            }
        }

        // Diagonal-L lines (q+r fixed, which is s in cube coordinates)
        val byS = boardCells.groupBy { it.q + it.r }
        for ((s, cells) in byS) {
            if (cells.size >= MIN_LINE_SIZE) {
                lines.add(Line(
                    axis = LineAxis.DIAGONAL_L,
                    index = s,
                    cells = cells.sortedBy { it.r }
                ))
            }
        }

        return lines
    }

    private fun computeCellToLines(): Map<AxialCoord, List<Line>> {
        val result = mutableMapOf<AxialCoord, MutableList<Line>>()

        for (line in allLines) {
            for (cell in line.cells) {
                result.getOrPut(cell) { mutableListOf() }.add(line)
            }
        }

        return result
    }

    /**
     * Get all lines that contain any of the given cells.
     * Used to check only relevant lines after piece placement.
     */
    fun getLinesContainingCells(cells: Collection<AxialCoord>): Set<Line> {
        val result = mutableSetOf<Line>()
        for (cell in cells) {
            cellToLines[cell]?.let { result.addAll(it) }
        }
        return result
    }
}
