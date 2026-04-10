package com.almostbrilliantideas.hexaddict.game

import androidx.compose.ui.graphics.Color

/**
 * Color palette for hex pieces.
 * Each color has a gradient from light to dark corner.
 */
object ColorPalette {

    data class PieceColor(
        val id: String,
        val light: Color,
        val dark: Color
    ) {
        val primary: Color get() = light  // Use light as the main color
    }

    val Purple = PieceColor(
        id = "purple",
        light = Color(0xFF8B84D4),
        dark = Color(0xFF5A52B0)
    )

    val Teal = PieceColor(
        id = "teal",
        light = Color(0xFF4DB893),
        dark = Color(0xFF268B68)
    )

    val Coral = PieceColor(
        id = "coral",
        light = Color(0xFFD88060),
        dark = Color(0xFFB05530)
    )

    val Blue = PieceColor(
        id = "blue",
        light = Color(0xFF55A0CE),
        dark = Color(0xFF2870A8)
    )

    val Mauve = PieceColor(
        id = "mauve",
        light = Color(0xFFA880C4),
        dark = Color(0xFF7A52A0)
    )

    val Amber = PieceColor(
        id = "amber",
        light = Color(0xFFD4AE45),
        dark = Color(0xFFA87E10)
    )

    /**
     * All 6 colors in the palette.
     */
    val allColors = listOf(Purple, Teal, Coral, Blue, Mauve, Amber)

    /**
     * Get active colors based on score.
     * - Score 0-999: 4 colors
     * - Score 1,000-2,999: 5 colors
     * - Score 3,000+: 6 colors
     */
    fun getActiveColors(score: Int): List<PieceColor> {
        val count = when {
            score < 1000 -> 4
            score < 3000 -> 5
            else -> 6
        }
        return allColors.take(count)
    }

    /**
     * Get a random color from the active palette.
     */
    fun randomColor(score: Int): PieceColor {
        return getActiveColors(score).random()
    }
}
