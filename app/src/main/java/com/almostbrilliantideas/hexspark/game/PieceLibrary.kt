package com.almostbrilliantideas.hexspark.game

import com.almostbrilliantideas.hexspark.model.AxialCoord
import com.almostbrilliantideas.hexspark.model.HexPiece
import kotlin.random.Random

/**
 * Complete library of 15 unique hex pieces.
 * All pieces are defined in axial coordinates relative to (0,0).
 * No rotation - each orientation is a separate piece.
 */
object PieceLibrary {

    /**
     * Piece categories for weighted selection.
     */
    enum class PieceCategory {
        SINGLE,      // 1-cell pieces (easiest)
        PAIR,        // 2-cell pieces (easy)
        LINE,        // 3-cell straight lines (medium)
        COMPACT      // 3-cell compact shapes (hardest)
    }

    // 1-cell pieces (1 total)
    private val single = HexPiece(
        id = "single",
        cells = listOf(AxialCoord(0, 0))
    )

    // 2-cell pieces (3 total)
    private val pairFlat = HexPiece(
        id = "pair_flat",
        cells = listOf(AxialCoord(0, 0), AxialCoord(1, 0))
    )

    private val pairDiagR = HexPiece(
        id = "pair_diag_r",
        cells = listOf(AxialCoord(0, 0), AxialCoord(0, 1))
    )

    private val pairDiagL = HexPiece(
        id = "pair_diag_l",
        cells = listOf(AxialCoord(0, 0), AxialCoord(-1, 1))
    )

    // 3-cell straight lines (3 total)
    private val lineHorizontal = HexPiece(
        id = "line_horizontal",
        cells = listOf(AxialCoord(0, 0), AxialCoord(1, 0), AxialCoord(2, 0))
    )

    private val lineDiagR = HexPiece(
        id = "line_diag_r",
        cells = listOf(AxialCoord(0, 0), AxialCoord(0, 1), AxialCoord(0, 2))
    )

    private val lineDiagL = HexPiece(
        id = "line_diag_l",
        cells = listOf(AxialCoord(0, 0), AxialCoord(-1, 1), AxialCoord(-2, 2))
    )

    // 3-cell compact shapes (8 total)
    private val c1 = HexPiece(
        id = "c1",
        cells = listOf(AxialCoord(0, 0), AxialCoord(0, 1), AxialCoord(1, 0))
    )

    private val c2 = HexPiece(
        id = "c2",
        cells = listOf(AxialCoord(0, 0), AxialCoord(0, 1), AxialCoord(1, 1))
    )

    private val c3 = HexPiece(
        id = "c3",
        cells = listOf(AxialCoord(0, 0), AxialCoord(1, 0), AxialCoord(1, 1))
    )

    private val c4 = HexPiece(
        id = "c4",
        cells = listOf(AxialCoord(0, 1), AxialCoord(0, 2), AxialCoord(1, 0))
    )

    private val c5 = HexPiece(
        id = "c5",
        cells = listOf(AxialCoord(0, 1), AxialCoord(1, 0), AxialCoord(1, 1))
    )

    private val c6 = HexPiece(
        id = "c6",
        cells = listOf(AxialCoord(0, 1), AxialCoord(1, 0), AxialCoord(2, 0))
    )

    private val c7 = HexPiece(
        id = "c7",
        cells = listOf(AxialCoord(0, 1), AxialCoord(1, 1), AxialCoord(2, 0))
    )

    private val c8 = HexPiece(
        id = "c8",
        cells = listOf(AxialCoord(0, 2), AxialCoord(1, 0), AxialCoord(1, 1))
    )

    /**
     * Pieces organized by category.
     */
    private val piecesByCategory = mapOf(
        PieceCategory.SINGLE to listOf(single),
        PieceCategory.PAIR to listOf(pairFlat, pairDiagR, pairDiagL),
        PieceCategory.LINE to listOf(lineHorizontal, lineDiagR, lineDiagL),
        PieceCategory.COMPACT to listOf(c1, c2, c3, c4, c5, c6, c7, c8)
    )

    /**
     * All 15 pieces in the game.
     */
    val allPieces: List<HexPiece> = listOf(
        // 1-cell
        single,
        // 2-cell
        pairFlat, pairDiagR, pairDiagL,
        // 3-cell lines
        lineHorizontal, lineDiagR, lineDiagL,
        // 3-cell compact
        c1, c2, c3, c4, c5, c6, c7, c8
    )

    /**
     * Weight profiles for different score ranges.
     * Values represent relative weights for each category.
     */
    private data class WeightProfile(
        val single: Float,
        val pair: Float,
        val line: Float,
        val compact: Float
    )

    // 0-999: Heavy small pieces, minimal compact
    private val earlyGameWeights = WeightProfile(
        single = 4.0f,
        pair = 3.5f,
        line = 1.5f,
        compact = 1.0f
    )

    // 1000-2999: Small pieces still common, compact stays low
    private val earlyMidWeights = WeightProfile(
        single = 2.5f,
        pair = 2.5f,
        line = 1.5f,
        compact = 1.0f
    )

    // 3000-4999: Fully balanced across all categories
    private val midGameWeights = WeightProfile(
        single = 1.5f,
        pair = 1.5f,
        line = 1.5f,
        compact = 1.5f
    )

    // 5000-9999: Compact shapes start to dominate
    private val lateGameWeights = WeightProfile(
        single = 0.5f,
        pair = 1.0f,
        line = 1.5f,
        compact = 3.0f
    )

    // 10000+: Compact shapes heavily dominate, singles nearly absent
    private val endGameWeights = WeightProfile(
        single = 0.1f,
        pair = 0.5f,
        line = 1.5f,
        compact = 5.0f
    )

    /**
     * Get category weights based on current score.
     * Uses smooth interpolation between weight profiles.
     *
     * Progression:
     * - 0-999: Heavy small pieces (single 4x, pair 3.5x, line 1.5x, compact 1x)
     * - 1000-2999: Small still common (single 2.5x, pair 2.5x, line 1.5x, compact 1x)
     * - 3000-4999: Balanced (single 1.5x, pair 1.5x, line 1.5x, compact 1.5x)
     * - 5000-9999: Compact pressure (single 0.5x, pair 1x, line 1.5x, compact 3x)
     * - 10000+: Compact dominates (single 0.1x, pair 0.5x, line 1.5x, compact 5x)
     */
    private fun getWeightsForScore(score: Int): Map<PieceCategory, Float> {
        val profile = when {
            score < 1000 -> earlyGameWeights
            score < 3000 -> {
                val t = (score - 1000) / 2000f
                interpolateProfiles(earlyGameWeights, earlyMidWeights, smoothStep(t))
            }
            score < 5000 -> {
                val t = (score - 3000) / 2000f
                interpolateProfiles(earlyMidWeights, midGameWeights, smoothStep(t))
            }
            score < 10000 -> {
                val t = (score - 5000) / 5000f
                interpolateProfiles(midGameWeights, lateGameWeights, smoothStep(t))
            }
            else -> endGameWeights
        }

        return mapOf(
            PieceCategory.SINGLE to profile.single,
            PieceCategory.PAIR to profile.pair,
            PieceCategory.LINE to profile.line,
            PieceCategory.COMPACT to profile.compact
        )
    }

    /**
     * Smooth step function for gradual transitions.
     * Maps [0,1] to [0,1] with ease-in-out curve.
     */
    private fun smoothStep(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return clamped * clamped * (3f - 2f * clamped)
    }

    /**
     * Interpolate between two weight profiles.
     */
    private fun interpolateProfiles(a: WeightProfile, b: WeightProfile, t: Float): WeightProfile {
        return WeightProfile(
            single = a.single + (b.single - a.single) * t,
            pair = a.pair + (b.pair - a.pair) * t,
            line = a.line + (b.line - a.line) * t,
            compact = a.compact + (b.compact - a.compact) * t
        )
    }

    /**
     * Generate a tray of pieces. Below score 10,000: no tray of all compact shapes —
     * always replace one with a 1- or 2-cell piece. At 10,000+ the floor is removed.
     */
    fun generateTray(score: Int, size: Int = 3): List<HexPiece> {
        val pieces = MutableList(size) { weightedRandomPiece(score) }

        if (score < 10000) {
            val allCompact = pieces.all { piece ->
                piecesByCategory[PieceCategory.COMPACT]?.contains(piece) == true
            }
            if (allCompact && size > 0) {
                val smallPieces = (piecesByCategory[PieceCategory.SINGLE] ?: emptyList()) +
                        (piecesByCategory[PieceCategory.PAIR] ?: emptyList())
                val replaceIndex = Random.nextInt(size)
                pieces[replaceIndex] = smallPieces.random()
            }
        }

        return pieces
    }

    /**
     * Get a weighted random piece based on current score.
     */
    fun weightedRandomPiece(score: Int): HexPiece {
        val weights = getWeightsForScore(score)

        // Build weighted list of categories
        val weightedCategories = mutableListOf<Pair<PieceCategory, Float>>()
        for ((category, weight) in weights) {
            // Multiply category weight by number of pieces in category
            // so each individual piece has equal chance within its category
            val piecesInCategory = piecesByCategory[category]?.size ?: 1
            weightedCategories.add(category to weight * piecesInCategory)
        }

        // Calculate total weight
        val totalWeight = weightedCategories.sumOf { it.second.toDouble() }.toFloat()

        // Pick a random value in [0, totalWeight)
        val randomValue = Random.nextFloat() * totalWeight

        // Find which category this falls into
        var cumulative = 0f
        var selectedCategory = PieceCategory.SINGLE
        for ((category, weight) in weightedCategories) {
            cumulative += weight
            if (randomValue < cumulative) {
                selectedCategory = category
                break
            }
        }

        // Pick a random piece from the selected category
        val piecesInCategory = piecesByCategory[selectedCategory] ?: listOf(single)
        return piecesInCategory.random()
    }

    /**
     * Get a random piece from the library (unweighted).
     * @deprecated Use weightedRandomPiece(score) instead for gameplay.
     */
    fun randomPiece(): HexPiece = allPieces.random()

    /**
     * Get a piece by its ID.
     */
    fun getPiece(id: String): HexPiece? = allPieces.find { it.id == id }
}
